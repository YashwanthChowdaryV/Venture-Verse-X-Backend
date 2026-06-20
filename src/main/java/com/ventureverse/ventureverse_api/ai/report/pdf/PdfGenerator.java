package com.ventureverse.ventureverse_api.ai.report.pdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.BorderRadius;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.ventureverse.ventureverse_api.entities.Startup;
import com.ventureverse.ventureverse_api.entities.StartupReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Slf4j
@Component
public class PdfGenerator {

    private final ObjectMapper objectMapper;

    private static final Color GOLD = new DeviceRgb(255, 191, 0);
    private static final Color ORANGE = new DeviceRgb(255, 121, 0);
    private static final Color DARK = new DeviceRgb(30, 30, 30);
    private static final Color GRAY = new DeviceRgb(92, 92, 92);
    private static final Color LIGHT_BG = new DeviceRgb(252, 250, 245);
    private static final Color GREEN = new DeviceRgb(16, 185, 129);
    private static final Color RED = new DeviceRgb(239, 68, 68);
    private static final Color BORDER_COLOR = new DeviceRgb(242, 207, 126);

    private PdfFont boldFont;
    private PdfFont regularFont;
    private PdfFont titleFont;

    public PdfGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] generate(StartupReport report) {
        if (report == null) {
            throw new IllegalArgumentException("Report cannot be null");
        }
        if (report.getStartup() == null) {
            throw new IllegalArgumentException("Startup cannot be null");
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(45, 45, 45, 45);

            boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
            regularFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
            titleFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);

            Startup startup = report.getStartup();
            String formattedDate = formatDate(report.getCreatedAt());

            log.info("Generating PDF for startup: {}", startup.getStartupName());

            // PAGE 1: COVER
            addCoverPage(document, startup, report, formattedDate);
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // PAGE 2: EXECUTIVE SUMMARY
            addExecutiveSummary(document, report);
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // PAGE 3: STARTUP SNAPSHOT
            addStartupSnapshot(document, startup);
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // PAGE 4: SCORE DASHBOARD
            addScoreDashboard(document, report);
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // PAGE 5-10: ANALYSIS SECTIONS
            addAnalysisSection(document, "Investor Analysis", report.getInvestorDetailsJson());
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            addAnalysisSection(document, "Competitor Intelligence", report.getCompetitorDetailsJson());
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            addAnalysisSection(document, "Financial Analysis", report.getFinanceDetailsJson());
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            addAnalysisSection(document, "Customer Intelligence", report.getCustomerDetailsJson());
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            addAnalysisSection(document, "Risk Assessment", report.getRiskDetailsJson());
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            addAnalysisSection(document, "Product Strategy", report.getProductStrategyDetailsJson());
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // PAGE 11: EXECUTION PLAN
            addExecutionPlan(document, report);
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // PAGE 12: INVESTMENT VERDICT
            addInvestmentVerdict(document, report);
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // PAGE 13: CERTIFICATION
            addCertificationPage(document, startup, report, formattedDate);

            document.close();
            log.info("PDF generated successfully for startup: {}", startup.getStartupName());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate PDF for report ID: {}", report.getId(), e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Safely format date from various possible types (Date, LocalDateTime, String,
     * etc.)
     */
    private String formatDate(Object dateObj) {
        if (dateObj == null) {
            return "N/A";
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

            if (dateObj instanceof LocalDateTime) {
                return ((LocalDateTime) dateObj).format(formatter);
            }

            if (dateObj instanceof Date) {
                LocalDateTime ldt = ((Date) dateObj).toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                return ldt.format(formatter);
            }

            // If it's already a string, try to parse and reformat
            if (dateObj instanceof String) {
                String dateStr = (String) dateObj;
                try {
                    LocalDateTime ldt = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    return ldt.format(formatter);
                } catch (Exception e) {
                    // If parsing fails, return as-is
                    return dateStr;
                }
            }

            // Last resort: use toString()
            return dateObj.toString();

        } catch (Exception e) {
            log.warn("Failed to format date: {}", dateObj, e);
            return "N/A";
        }
    }

    // ==================== HELPER METHODS ====================

    private void addPageHeader(Document document, String title, String subtitle) {
        Div accentLine = new Div()
                .setHeight(3)
                .setWidth(UnitValue.createPercentValue(15))
                .setBackgroundColor(GOLD)
                .setMarginBottom(10);
        document.add(accentLine);

        document.add(new Paragraph(title)
                .setFontSize(22)
                .setFontColor(DARK)
                .setFont(titleFont)
                .setMarginBottom(4));

        document.add(new Paragraph(subtitle)
                .setFontSize(10)
                .setFontColor(GRAY)
                .setFont(regularFont)
                .setMarginBottom(25));
    }

    private Cell createTableHeader(String text) {
        return new Cell()
                .setBackgroundColor(GOLD, 0.1f)
                .setBorder(Border.NO_BORDER)
                .setPadding(10)
                .add(new Paragraph(text)
                        .setFontSize(9)
                        .setFontColor(GOLD)
                        .setFont(boldFont));
    }

    private Cell createScoreCard(String label, int score) {
        Color scoreColor = score >= 80 ? GREEN : score >= 60 ? GOLD : score >= 40 ? ORANGE : RED;

        Cell cell = new Cell()
                .setBorder(new SolidBorder(BORDER_COLOR, 1))
                .setBorderRadius(new BorderRadius(8))
                .setPadding(15)
                .setBackgroundColor(LIGHT_BG);

        cell.add(new Paragraph(label).setFontSize(9).setFontColor(GRAY).setFont(boldFont).setMarginBottom(5));
        cell.add(new Paragraph(score + "/100").setFontSize(24).setFontColor(scoreColor).setFont(titleFont)
                .setMarginBottom(3));
        String rating = score >= 80 ? "Excellent" : score >= 60 ? "Good" : score >= 40 ? "Fair" : "Needs Work";
        cell.add(new Paragraph(rating).setFontSize(8).setFontColor(GRAY).setFont(regularFont));

        return cell;
    }

    private Cell createMetricCard(String label, String value, String subtitle) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(BORDER_COLOR, 1))
                .setBorderRadius(new BorderRadius(8))
                .setPadding(12)
                .setBackgroundColor(LIGHT_BG);

        cell.add(new Paragraph(label).setFontSize(8).setFontColor(GRAY).setFont(boldFont).setMarginBottom(4));
        cell.add(new Paragraph(value != null ? value : "N/A").setFontSize(16).setFontColor(DARK).setFont(titleFont));
        if (subtitle != null && !subtitle.isEmpty()) {
            cell.add(new Paragraph(subtitle).setFontSize(7).setFontColor(GRAY).setFont(regularFont).setMarginTop(3));
        }
        return cell;
    }

    private Cell createListCard(String title, String content) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(BORDER_COLOR, 1))
                .setBorderRadius(new BorderRadius(8))
                .setPadding(12)
                .setBackgroundColor(LIGHT_BG);

        cell.add(new Paragraph(title).setFontSize(9).setFontColor(GOLD).setFont(boldFont).setMarginBottom(6));

        if (content != null) {
            for (String line : content.split("\n")) {
                if (line.trim().length() > 0) {
                    cell.add(new Paragraph(line.trim()).setFontSize(8).setFontColor(DARK).setFont(regularFont)
                            .setMarginBottom(2));
                }
            }
        }
        return cell;
    }

    private void addMetaCell(Table table, String label, String value) {
        Cell cell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(8)
                .setTextAlignment(TextAlignment.CENTER);

        cell.add(new Paragraph(label).setFontSize(7).setFontColor(GRAY).setFont(boldFont).setMarginBottom(2));
        cell.add(new Paragraph(value != null ? value : "-").setFontSize(9).setFontColor(DARK).setFont(regularFont));
        table.addCell(cell);
    }

    private String[] generateKeyFindings(StartupReport report) {
        java.util.List<String> findings = new java.util.ArrayList<>();

        int overallScore = report.getOverallScore();
        if (overallScore >= 80) {
            findings.add("Strong overall score of " + overallScore + "/100 indicates high investment readiness");
        } else if (overallScore >= 60) {
            findings.add("Moderate overall score of " + overallScore + "/100 - potential with targeted improvements");
        } else {
            findings.add("Overall score of " + overallScore + "/100 suggests significant development needed");
        }

        if (report.getInvestmentScore() >= 80)
            findings.add("High investment attractiveness with strong market positioning");
        if (report.getCompetitionScore() < 60)
            findings.add("Competitive landscape requires strategic differentiation");
        if (report.getFinancialScore() >= 70)
            findings.add("Solid financial projections with promising unit economics");
        if (report.getProductStrategyScore() >= 80)
            findings.add("Well-defined product roadmap with clear execution milestones");

        while (findings.size() < 5) {
            findings.add("Additional due diligence recommended for comprehensive assessment");
        }
        return findings.subList(0, 5).toArray(new String[0]);
    }

    private String getInvestmentGrade(int score) {
        if (score >= 90)
            return "A+";
        if (score >= 80)
            return "A";
        if (score >= 70)
            return "B+";
        if (score >= 60)
            return "B";
        return "C";
    }

    private String getRecommendation(int score) {
        if (score >= 90)
            return "Strong Buy - Exceptional Investment Opportunity";
        if (score >= 80)
            return "Buy - Promising Investment with Strong Fundamentals";
        if (score >= 70)
            return "Hold - Proceed with Targeted Validation";
        if (score >= 60)
            return "Caution - Requires Further Due Diligence";
        return "High Risk - Not Recommended for Investment";
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    // ==================== PAGE 1: COVER ====================
    private void addCoverPage(Document document, Startup startup, StartupReport report, String formattedDate) {
        Div topLine = new Div().setHeight(4).setWidth(UnitValue.createPercentValue(100)).setBackgroundColor(GOLD);
        document.add(topLine);
        document.add(new Paragraph("").setHeight(40));

        document.add(new Paragraph("VentureVerseX")
                .setFontSize(24).setFontColor(GOLD).setFont(titleFont)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(8));

        document.add(new Paragraph("AI Startup Due Diligence Report")
                .setFontSize(12).setFontColor(GRAY).setFont(regularFont)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(40));

        document.add(new Paragraph(safeString(startup.getStartupName()))
                .setFontSize(30).setFontColor(DARK).setFont(titleFont)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));

        document.add(new Paragraph(safeString(startup.getIndustry()))
                .setFontSize(14).setFontColor(GRAY).setFont(regularFont)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(40));

        // Score Circle
        Table scoreCircle = new Table(1)
                .setWidth(UnitValue.createPercentValue(35))
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMarginBottom(30);

        Cell circleCell = new Cell()
                .setBorder(new SolidBorder(GOLD, 3))
                .setBorderRadius(new BorderRadius(100))
                .setPadding(25)
                .setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(LIGHT_BG);

        circleCell.add(new Paragraph(String.valueOf(report.getOverallScore()))
                .setFontSize(44).setFontColor(GOLD).setFont(titleFont)
                .setTextAlignment(TextAlignment.CENTER));

        circleCell.add(new Paragraph("OUT OF 100")
                .setFontSize(7).setFontColor(GRAY).setFont(regularFont)
                .setTextAlignment(TextAlignment.CENTER));

        scoreCircle.addCell(circleCell);
        document.add(scoreCircle);

        document.add(new Paragraph(safeString(report.getFinalVerdict()))
                .setFontSize(14).setFontColor(DARK).setFont(boldFont)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(30));

        // Meta info
        Table metaTable = new Table(4)
                .setWidth(UnitValue.createPercentValue(90))
                .setHorizontalAlignment(HorizontalAlignment.CENTER);

        addMetaCell(metaTable, "Report ID", "VVX-" + report.getId());
        addMetaCell(metaTable, "Generated", formattedDate);
        addMetaCell(metaTable, "Industry", safeString(startup.getIndustry()));
        addMetaCell(metaTable, "Stage", "Early Stage");
        document.add(metaTable);

        document.add(new Paragraph("").setHeight(50));
        document.add(new Paragraph("CONFIDENTIAL INVESTOR REPORT")
                .setFontSize(8).setFontColor(GRAY).setFont(boldFont)
                .setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("Generated by VentureVerseX AI Intelligence Platform")
                .setFontSize(7).setFontColor(GRAY).setFont(regularFont)
                .setTextAlignment(TextAlignment.CENTER));
    }

    // ==================== PAGE 2: EXECUTIVE SUMMARY ====================
    private void addExecutiveSummary(Document document, StartupReport report) {
        addPageHeader(document, "Executive Summary", "Investment Overview & Key Findings");

        int[][] scores = {
                { report.getInvestmentScore(), report.getCompetitionScore(), report.getFinancialScore() },
                { report.getCustomerScore(), report.getRiskScore(), report.getProductStrategyScore() }
        };
        String[] labels = { "Investment", "Competition", "Finance", "Customer", "Risk", "Product Strategy" };

        Table scoreGrid = new Table(3).setWidth(UnitValue.createPercentValue(100)).setMarginBottom(20);
        int idx = 0;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                scoreGrid.addCell(createScoreCard(labels[idx], scores[row][col]));
                idx++;
            }
        }
        document.add(scoreGrid);

        String recommendation = report.getFinalRecommendation();
        if (recommendation != null && !recommendation.isEmpty()) {
            Div recBox = new Div()
                    .setBackgroundColor(GOLD, 0.08f)
                    .setBorder(new SolidBorder(GOLD, 1))
                    .setBorderRadius(new BorderRadius(10))
                    .setPadding(18)
                    .setMarginBottom(20);
            recBox.add(new Paragraph("INVESTMENT RECOMMENDATION")
                    .setFontSize(8).setFontColor(GOLD).setFont(boldFont).setMarginBottom(6));
            recBox.add(new Paragraph(recommendation)
                    .setFontSize(11).setFontColor(DARK).setFont(regularFont));
            document.add(recBox);
        }

        document.add(new Paragraph("KEY FINDINGS")
                .setFontSize(10).setFontColor(GOLD).setFont(boldFont).setMarginBottom(10));

        String[] findings = generateKeyFindings(report);
        for (String finding : findings) {
            document.add(new Paragraph("\u25B8 " + finding)
                    .setFontSize(10).setFontColor(DARK).setFont(regularFont)
                    .setMarginBottom(5).setMarginLeft(8));
        }
    }

    // ==================== PAGE 3: STARTUP SNAPSHOT ====================
    private void addStartupSnapshot(Document document, Startup startup) {
        addPageHeader(document, "Startup Snapshot", "Company Profile & Overview");

        String[][] fields = {
                { "Startup Name", safeString(startup.getStartupName()) },
                { "Industry", safeString(startup.getIndustry()) },
                { "Target Market", startup.getTargetMarket() != null ? startup.getTargetMarket() : "Not specified" },
                { "Business Model", "SaaS / Enterprise" },
                { "Funding Stage", "Early Stage" },
                { "Required Capital", "Not specified" }
        };

        Table snapshotTable = new Table(2).setWidth(UnitValue.createPercentValue(100)).setMarginBottom(25);
        for (String[] field : fields) {
            Cell labelCell = new Cell().setBackgroundColor(GOLD, 0.06f).setBorder(Border.NO_BORDER).setPadding(12)
                    .add(new Paragraph(field[0]).setFontSize(9).setFontColor(GRAY).setFont(boldFont));
            Cell valueCell = new Cell().setBackgroundColor(LIGHT_BG).setBorder(Border.NO_BORDER).setPadding(12)
                    .add(new Paragraph(field[1] != null ? field[1] : "-").setFontSize(10).setFontColor(DARK)
                            .setFont(regularFont));
            snapshotTable.addCell(labelCell);
            snapshotTable.addCell(valueCell);
        }
        document.add(snapshotTable);

        String ideaDesc = startup.getIdeaDescription();
        if (ideaDesc != null && !ideaDesc.isEmpty()) {
            document.add(new Paragraph("Company Description")
                    .setFontSize(10).setFontColor(GOLD).setFont(boldFont).setMarginBottom(8));
            document.add(new Paragraph(ideaDesc)
                    .setFontSize(10).setFontColor(DARK).setFont(regularFont));
        }
    }

    // ==================== PAGE 4: SCORE DASHBOARD ====================
    private void addScoreDashboard(Document document, StartupReport report) {
        addPageHeader(document, "Score Dashboard", "Multi-Dimensional Assessment");

        String[][] scoreData = {
                { "Investor", String.valueOf(report.getInvestmentScore()) },
                { "Competition", String.valueOf(report.getCompetitionScore()) },
                { "Finance", String.valueOf(report.getFinancialScore()) },
                { "Customer", String.valueOf(report.getCustomerScore()) },
                { "Risk", String.valueOf(report.getRiskScore()) },
                { "Product Strategy", String.valueOf(report.getProductStrategyScore()) }
        };

        Table scoreTable = new Table(3).setWidth(UnitValue.createPercentValue(100)).setMarginBottom(25);
        scoreTable.addCell(createTableHeader("Category"));
        scoreTable.addCell(createTableHeader("Score"));
        scoreTable.addCell(createTableHeader("Rating"));

        for (String[] row : scoreData) {
            int score = Integer.parseInt(row[1]);
            String rating = score >= 80 ? "Excellent" : score >= 60 ? "Good" : score >= 40 ? "Fair" : "Needs Work";
            Color scoreColor = score >= 80 ? GREEN : score >= 60 ? GOLD : score >= 40 ? ORANGE : RED;

            scoreTable.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(8)
                    .add(new Paragraph(row[0]).setFontSize(10).setFontColor(DARK).setFont(regularFont)));
            scoreTable.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(8)
                    .add(new Paragraph(row[1] + "/100").setFontSize(10).setFontColor(scoreColor).setFont(boldFont)));
            scoreTable.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(8)
                    .add(new Paragraph(rating).setFontSize(9).setFontColor(GRAY).setFont(regularFont)));
        }
        document.add(scoreTable);
    }

    // ==================== ANALYSIS SECTIONS (PAGES 5-10) ====================
    private void addAnalysisSection(Document document, String title, String jsonData) {
        addPageHeader(document, title, "Detailed Analysis");

        if (jsonData == null || jsonData.isEmpty() || jsonData.equals("{}")) {
            document.add(new Paragraph("No data available for this analysis.")
                    .setFontSize(11).setFontColor(GRAY).setFont(regularFont));
            return;
        }

        try {
            JsonNode data = objectMapper.readTree(jsonData);

            if (data.has("score")) {
                document.add(new Paragraph("Score: " + data.get("score").asText() + "/100")
                        .setFontSize(14).setFontColor(GOLD).setFont(boldFont).setMarginBottom(5));
            }

            if (data.has("verdict")) {
                document.add(new Paragraph("Verdict: " + data.get("verdict").asText())
                        .setFontSize(12).setFontColor(DARK).setFont(boldFont).setMarginBottom(15));
            }

            if (data.has("analysis")) {
                document.add(new Paragraph("ANALYSIS")
                        .setFontSize(10).setFontColor(GOLD).setFont(boldFont).setMarginBottom(8));
                document.add(new Paragraph(data.get("analysis").asText())
                        .setFontSize(10).setFontColor(DARK).setFont(regularFont).setMarginBottom(15));
            }

            java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = data.fields();
            while (fields.hasNext()) {
                java.util.Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                JsonNode value = field.getValue();

                if (key.equals("score") || key.equals("verdict") || key.equals("analysis")) {
                    continue;
                }

                if (value.isArray()) {
                    document.add(new Paragraph(formatFieldName(key))
                            .setFontSize(9).setFontColor(GOLD).setFont(boldFont)
                            .setMarginTop(10).setMarginBottom(5));

                    ArrayNode array = (ArrayNode) value;
                    for (JsonNode item : array) {
                        document.add(new Paragraph("  \u2022 " + item.asText())
                                .setFontSize(9).setFontColor(DARK).setFont(regularFont)
                                .setMarginBottom(3));
                    }
                } else if (!value.isObject()) {
                    document.add(new Paragraph(formatFieldName(key) + ": " + value.asText())
                            .setFontSize(9).setFontColor(DARK).setFont(regularFont)
                            .setMarginBottom(4));
                }
            }

        } catch (Exception e) {
            log.warn("Error parsing JSON for section: {}", title, e);
            document.add(new Paragraph(jsonData)
                    .setFontSize(9).setFontColor(GRAY).setFont(regularFont));
        }
    }

    // ==================== PAGE 11: EXECUTION PLAN ====================
    private void addExecutionPlan(Document document, StartupReport report) {
        addPageHeader(document, "Next 90 Day Execution Plan", "Immediate Priorities");

        String jsonData = report.getProductStrategyDetailsJson();
        if (jsonData == null || jsonData.isEmpty()) {
            document.add(new Paragraph("No execution plan data available.")
                    .setFontSize(10).setFontColor(GRAY).setFont(regularFont));
            return;
        }

        try {
            JsonNode data = objectMapper.readTree(jsonData);

            if (data.has("next90Days")) {
                ArrayNode priorities = (ArrayNode) data.get("next90Days");

                document.add(new Paragraph("IMMEDIATE PRIORITIES")
                        .setFontSize(10).setFontColor(GOLD).setFont(boldFont).setMarginBottom(12));

                for (int i = 0; i < priorities.size(); i++) {
                    String priority = priorities.get(i).asText();

                    Div priorityCard = new Div()
                            .setBackgroundColor(LIGHT_BG)
                            .setBorder(new SolidBorder(BORDER_COLOR, 1))
                            .setBorderRadius(new BorderRadius(8))
                            .setPadding(12)
                            .setMarginBottom(8);

                    priorityCard.add(new Paragraph("PRIORITY " + (i + 1))
                            .setFontSize(7).setFontColor(GOLD).setFont(boldFont).setMarginBottom(4));
                    priorityCard.add(new Paragraph(priority)
                            .setFontSize(10).setFontColor(DARK).setFont(regularFont));
                    document.add(priorityCard);
                }
            }

        } catch (Exception e) {
            log.warn("Error parsing execution plan", e);
        }
    }

    // ==================== PAGE 12: INVESTMENT VERDICT ====================
    private void addInvestmentVerdict(Document document, StartupReport report) {
        document.add(new Paragraph("").setHeight(80));

        document.add(new Paragraph("INVESTMENT VERDICT")
                .setFontSize(10).setFontColor(GOLD).setFont(boldFont)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(30));

        document.add(new Paragraph(String.valueOf(report.getOverallScore()))
                .setFontSize(72).setFontColor(GOLD).setFont(titleFont)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));

        document.add(new Paragraph("OUT OF 100")
                .setFontSize(10).setFontColor(GRAY).setFont(regularFont)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

        String grade = getInvestmentGrade(report.getOverallScore());
        document.add(new Paragraph("Investment Grade: " + grade)
                .setFontSize(18).setFontColor(DARK).setFont(titleFont)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(15));

        String recommendation = getRecommendation(report.getOverallScore());
        document.add(new Paragraph(recommendation)
                .setFontSize(14).setFontColor(GOLD).setFont(boldFont)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(40));

        String verdict = report.getFinalVerdict();
        if (verdict != null && !verdict.isEmpty()) {
            document.add(new Paragraph(verdict)
                    .setFontSize(12).setFontColor(DARK).setFont(regularFont)
                    .setTextAlignment(TextAlignment.CENTER));
        }
    }

    // ==================== PAGE 13: CERTIFICATION ====================
    private void addCertificationPage(Document document, Startup startup, StartupReport report, String formattedDate) {
        document.add(new Paragraph("").setHeight(100));

        document.add(new Paragraph("CERTIFICATION")
                .setFontSize(16).setFontColor(GOLD).setFont(titleFont)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(30));

        document.add(new Paragraph("This due diligence report has been generated by")
                .setFontSize(11).setFontColor(GRAY).setFont(regularFont)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));

        document.add(new Paragraph("VentureVerseX")
                .setFontSize(24).setFontColor(GOLD).setFont(titleFont)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(8));

        document.add(new Paragraph("AI-Powered Startup Intelligence Platform")
                .setFontSize(11).setFontColor(GRAY).setFont(regularFont)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(50));

        Table certTable = new Table(2)
                .setWidth(UnitValue.createPercentValue(70))
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMarginBottom(50);

        String[][] certFields = {
                { "Report ID", "VVX-" + report.getId() },
                { "Startup", safeString(startup.getStartupName()) },
                { "Industry", safeString(startup.getIndustry()) },
                { "Generated Date", formattedDate },
                { "Overall Score", report.getOverallScore() + "/100" },
                { "Investment Grade", getInvestmentGrade(report.getOverallScore()) }
        };

        for (String[] field : certFields) {
            certTable.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(6)
                    .add(new Paragraph(field[0]).setFontSize(8).setFontColor(GRAY).setFont(boldFont)));
            certTable.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(6)
                    .add(new Paragraph(field[1]).setFontSize(9).setFontColor(DARK).setFont(regularFont)));
        }
        document.add(certTable);

        document.add(new Paragraph("").setHeight(60));
        document.add(new Paragraph("CONFIDENTIAL INVESTOR DOCUMENT")
                .setFontSize(10).setFontColor(GOLD).setFont(boldFont)
                .setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("This report contains proprietary analysis generated by VentureVerseX AI.")
                .setFontSize(8).setFontColor(GRAY).setFont(regularFont)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(5));
        document.add(new Paragraph("For authorized recipients only. Do not distribute without permission.")
                .setFontSize(8).setFontColor(GRAY).setFont(regularFont)
                .setTextAlignment(TextAlignment.CENTER));
    }

    private String formatFieldName(String camelCase) {
        if (camelCase == null || camelCase.isEmpty())
            return "";
        String formatted = camelCase.replaceAll("([A-Z])", " $1");
        formatted = formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
        return formatted.trim();
    }
}