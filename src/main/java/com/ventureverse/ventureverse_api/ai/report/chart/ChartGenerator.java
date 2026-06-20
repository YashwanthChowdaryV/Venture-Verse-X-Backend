package com.ventureverse.ventureverse_api.ai.report.chart;

import com.ventureverse.ventureverse_api.entities.StartupReport;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.ByteArrayOutputStream;

@Component
public class ChartGenerator {

    public byte[] generateScoreChart(StartupReport report) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            dataset.addValue(report.getInvestmentScore(), "Score", "Investor");
            dataset.addValue(report.getCompetitionScore(), "Score", "Competitor");
            dataset.addValue(report.getFinancialScore(), "Score", "Finance");
            dataset.addValue(report.getCustomerScore(), "Score", "Customer");
            dataset.addValue(report.getRiskScore(), "Score", "Risk");
            dataset.addValue(report.getProductStrategyScore(), "Score", "Product");

            JFreeChart chart = ChartFactory.createBarChart(
                    "Startup Readiness Dashboard",
                    "Analysis Category",
                    "Score (0-100)",
                    dataset,
                    PlotOrientation.VERTICAL,
                    false,
                    true,
                    false);

            chart.setBackgroundPaint(Color.WHITE);
            chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 18));
            chart.getTitle().setPaint(new Color(30, 30, 30));

            CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(new Color(252, 250, 245));
            plot.setRangeGridlinePaint(new Color(242, 207, 126, 100));
            plot.setDomainGridlinesVisible(false);
            plot.setOutlineVisible(false);

            BarRenderer renderer = new BarRenderer() {
                @Override
                public Paint getItemPaint(int row, int col) {
                    int score = 0;
                    switch (col) {
                        case 0:
                            score = report.getInvestmentScore();
                            break;
                        case 1:
                            score = report.getCompetitionScore();
                            break;
                        case 2:
                            score = report.getFinancialScore();
                            break;
                        case 3:
                            score = report.getCustomerScore();
                            break;
                        case 4:
                            score = report.getRiskScore();
                            break;
                        case 5:
                            score = report.getProductStrategyScore();
                            break;
                    }

                    if (score >= 80)
                        return new Color(255, 191, 0);
                    if (score >= 60)
                        return new Color(242, 207, 126);
                    if (score >= 40)
                        return new Color(255, 230, 66);
                    return new Color(255, 121, 0);
                }
            };

            renderer.setMaximumBarWidth(0.15);
            renderer.setShadowVisible(false);
            renderer.setItemMargin(0.02);
            plot.setRenderer(renderer);

            plot.getDomainAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 11));
            plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.BOLD, 12));
            plot.getRangeAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 11));
            plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.BOLD, 12));
            plot.getRangeAxis().setRange(0, 100);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(baos, chart, 800, 450);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate chart", e);
        }
    }

    public byte[] generateRadarChart(StartupReport report) {
        // Placeholder for radar chart - can be implemented with custom drawing
        return generateScoreChart(report);
    }

    public byte[] generateRevenueChart(StartupReport report) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            // You'll need to parse finance JSON for these values
            dataset.addValue(2.5, "Revenue", "Year 1");
            dataset.addValue(6.0, "Revenue", "Year 2");
            dataset.addValue(15.0, "Revenue", "Year 3");

            JFreeChart chart = ChartFactory.createBarChart(
                    "Revenue Forecast",
                    "Year",
                    "Revenue ($M)",
                    dataset,
                    PlotOrientation.VERTICAL,
                    false,
                    true,
                    false);

            chart.setBackgroundPaint(Color.WHITE);
            chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 18));
            chart.getTitle().setPaint(new Color(30, 30, 30));

            CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(new Color(252, 250, 245));
            plot.setRangeGridlinePaint(new Color(242, 207, 126, 100));
            plot.setDomainGridlinesVisible(false);
            plot.setOutlineVisible(false);

            BarRenderer renderer = new BarRenderer();
            renderer.setSeriesPaint(0, new Color(255, 191, 0));
            renderer.setMaximumBarWidth(0.3);
            renderer.setShadowVisible(false);
            plot.setRenderer(renderer);

            plot.getDomainAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 11));
            plot.getRangeAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 11));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(baos, chart, 800, 450);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate revenue chart", e);
        }
    }

    public byte[] generateRiskHeatmap(StartupReport report) {
        // Placeholder for risk heatmap
        return generateScoreChart(report);
    }

    public byte[] generateUseOfFundsChart(StartupReport report) {
        // Placeholder for pie chart
        return generateScoreChart(report);
    }
}