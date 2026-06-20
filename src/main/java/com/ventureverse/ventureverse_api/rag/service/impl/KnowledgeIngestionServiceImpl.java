package com.ventureverse.ventureverse_api.rag.service.impl;

import com.ventureverse.ventureverse_api.rag.chunking.SemanticChunker;
import com.ventureverse.ventureverse_api.rag.chunking.SemanticChunker.Chunk;
import com.ventureverse.ventureverse_api.rag.dto.IngestionResponse;
import com.ventureverse.ventureverse_api.rag.dto.KnowledgeUploadRequest;
import com.ventureverse.ventureverse_api.rag.service.EmbeddingService;
import com.ventureverse.ventureverse_api.rag.service.KnowledgeIngestionService;
import com.ventureverse.ventureverse_api.rag.service.QdrantRestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeIngestionServiceImpl implements KnowledgeIngestionService {

        private final EmbeddingService embeddingService;
        private final QdrantRestService qdrantRestService;
        private final SemanticChunker semanticChunker;

        // Metadata extraction patterns
        private static final Pattern TITLE_PATTERN = Pattern.compile("^TITLE:\\s*(.+)$", Pattern.MULTILINE);
        private static final Pattern CATEGORY_PATTERN = Pattern.compile("^CATEGORY:\\s*(.+)$", Pattern.MULTILINE);
        private static final Pattern SUBCATEGORY_PATTERN = Pattern.compile("^SUBCATEGORY:\\s*(.+)$", Pattern.MULTILINE);
        private static final Pattern TAGS_PATTERN = Pattern.compile("^TAGS:\\s*(.+)$", Pattern.MULTILINE);
        private static final Pattern SOURCE_PATTERN = Pattern.compile("^SOURCE:\\s*(.+)$", Pattern.MULTILINE);

        @Override
        public IngestionResponse ingestKnowledgeBase() {
                int filesProcessed = 0;
                int chunksCreated = 0;
                List<String> docIds = new ArrayList<>();
                Map<String, Integer> categories = new HashMap<>();

                try {
                        // Create collection if it doesn't exist
                        qdrantRestService.createCollection();

                        // Get all .txt files from knowledge directory
                        Path knowledgeDir = getKnowledgeDirectory();
                        List<Path> textFiles = getTextFiles(knowledgeDir);

                        log.info("Found {} text files in knowledge directory", textFiles.size());

                        for (Path filePath : textFiles) {
                                String fileName = filePath.getFileName().toString();
                                log.info("Processing file: {}", fileName);

                                try {
                                        String content = Files.readString(filePath);

                                        // Extract metadata from content
                                        Map<String, String> metadata = extractMetadata(content);

                                        // Build request with extracted metadata
                                        KnowledgeUploadRequest request = buildRequestFromMetadata(fileName, content,
                                                        metadata);

                                        IngestionResponse result = uploadDocument(request);
                                        chunksCreated += result.getChunksCreated();
                                        docIds.addAll(result.getDocumentIds());

                                        String cat = request.getCategory();
                                        categories.merge(cat, 1, Integer::sum);
                                        filesProcessed++;

                                } catch (Exception e) {
                                        log.error("Failed to process file: {}", fileName, e);
                                }
                        }

                        return IngestionResponse.builder()
                                        .filesProcessed(filesProcessed)
                                        .chunksCreated(chunksCreated)
                                        .documentsStored(docIds.size())
                                        .documentIds(docIds)
                                        .categoryDistribution(categories)
                                        .message(String.format(
                                                        "Knowledge base loaded with metadata - %d files processed, %d chunks created",
                                                        filesProcessed, chunksCreated))
                                        .build();

                } catch (Exception e) {
                        throw new RuntimeException("Ingestion failed: " + e.getMessage(), e);
                }
        }

        @Override
        public IngestionResponse uploadDocument(KnowledgeUploadRequest request) {
                int chunksCreated = 0;
                List<String> docIds = new ArrayList<>();

                try {
                        // Use semantic chunking instead of fixed-size
                        List<Chunk> chunks = semanticChunker.chunk(request.getContent());

                        log.info("Creating {} chunks for document: {}", chunks.size(), request.getTitle());

                        for (Chunk chunk : chunks) {
                                Map<String, Object> metadata = buildMetadata(request, chunk);

                                List<Float> embedding = embeddingService.createEmbedding(chunk.getText());

                                String chunkId = UUID.randomUUID().toString();
                                docIds.add(chunkId);

                                qdrantRestService.insertVectorWithMetadata(
                                                chunkId,
                                                request.getTitle(),
                                                chunk.getText(),
                                                embedding,
                                                metadata);

                                chunksCreated++;
                        }

                        return IngestionResponse.builder()
                                        .filesProcessed(1)
                                        .chunksCreated(chunksCreated)
                                        .documentsStored(chunksCreated)
                                        .documentIds(docIds)
                                        .message("Document ingested: " + request.getTitle())
                                        .build();

                } catch (Exception e) {
                        throw new RuntimeException("Document upload failed: " + e.getMessage(), e);
                }
        }

        @Override
        public IngestionResponse uploadDocuments(List<KnowledgeUploadRequest> requests) {
                int totalFiles = 0;
                int totalChunks = 0;
                List<String> allDocIds = new ArrayList<>();
                Map<String, Integer> categories = new HashMap<>();

                for (KnowledgeUploadRequest request : requests) {
                        IngestionResponse result = uploadDocument(request);
                        totalFiles++;
                        totalChunks += result.getChunksCreated();
                        allDocIds.addAll(result.getDocumentIds());
                        categories.merge(request.getCategory(), result.getChunksCreated(), Integer::sum);
                }

                return IngestionResponse.builder()
                                .filesProcessed(totalFiles)
                                .chunksCreated(totalChunks)
                                .documentsStored(allDocIds.size())
                                .documentIds(allDocIds)
                                .categoryDistribution(categories)
                                .message("Batch ingestion complete")
                                .build();
        }

        /**
         * Gets the knowledge directory path from classpath
         */
        private Path getKnowledgeDirectory() throws IOException {
                try {
                        // Try to get from classpath
                        ClassPathResource resource = new ClassPathResource("knowledge");
                        if (resource.exists() && resource.getFile().exists()) {
                                return resource.getFile().toPath();
                        }
                } catch (IOException e) {
                        log.warn("Could not load knowledge directory from classpath, trying filesystem path");
                }

                // Fallback to filesystem path
                Path fsPath = Paths.get("src/main/resources/knowledge");
                if (Files.exists(fsPath) && Files.isDirectory(fsPath)) {
                        return fsPath;
                }

                throw new IOException("Knowledge directory not found in classpath or filesystem");
        }

        /**
         * Gets all .txt files from the knowledge directory
         */
        private List<Path> getTextFiles(Path directory) throws IOException {
                List<Path> textFiles = new ArrayList<>();

                try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.txt")) {
                        for (Path entry : stream) {
                                if (Files.isRegularFile(entry)) {
                                        textFiles.add(entry);
                                }
                        }
                }

                // Sort files alphabetically for consistent ordering
                textFiles.sort(Comparator.comparing(path -> path.getFileName().toString()));
                return textFiles;
        }

        /**
         * Extracts metadata from document content using regex patterns
         */
        private Map<String, String> extractMetadata(String content) {
                Map<String, String> metadata = new HashMap<>();

                Matcher titleMatcher = TITLE_PATTERN.matcher(content);
                if (titleMatcher.find()) {
                        metadata.put("title", titleMatcher.group(1).trim());
                }

                Matcher categoryMatcher = CATEGORY_PATTERN.matcher(content);
                if (categoryMatcher.find()) {
                        metadata.put("category", categoryMatcher.group(1).trim());
                }

                Matcher subcategoryMatcher = SUBCATEGORY_PATTERN.matcher(content);
                if (subcategoryMatcher.find()) {
                        metadata.put("subcategory", subcategoryMatcher.group(1).trim());
                }

                Matcher tagsMatcher = TAGS_PATTERN.matcher(content);
                if (tagsMatcher.find()) {
                        metadata.put("tags", tagsMatcher.group(1).trim());
                }

                Matcher sourceMatcher = SOURCE_PATTERN.matcher(content);
                if (sourceMatcher.find()) {
                        metadata.put("source", sourceMatcher.group(1).trim());
                }

                return metadata;
        }

        /**
         * Builds a KnowledgeUploadRequest from filename, content, and extracted
         * metadata
         */
        private KnowledgeUploadRequest buildRequestFromMetadata(String fileName, String content,
                        Map<String, String> metadata) {
                // Extract title from metadata or fallback to filename
                String title = metadata.getOrDefault("title",
                                fileName.replace(".txt", "").replace("-", " "));

                // Extract category from metadata or detect from filename
                String category = metadata.getOrDefault("category", detectCategory(fileName));

                // Extract tags and convert to keywords
                List<String> keywords = new ArrayList<>();
                String tagsStr = metadata.get("tags");
                if (tagsStr != null && !tagsStr.isEmpty()) {
                        keywords = Arrays.stream(tagsStr.split(","))
                                        .map(String::trim)
                                        .collect(Collectors.toList());
                } else {
                        // Fallback to filename-based keywords
                        keywords = Arrays.asList(fileName.replace(".txt", "").split("-"));
                }

                // Build the request
                return KnowledgeUploadRequest.builder()
                                .title(title)
                                .content(content)
                                .category(category)
                                .topic(metadata.getOrDefault("subcategory", detectTopic(fileName)))
                                .keywords(keywords)
                                .difficulty(metadata.getOrDefault("difficulty", "intermediate"))
                                .persona(metadata.getOrDefault("persona", "founder"))
                                .sourceType(metadata.getOrDefault("source", "ventureverse_knowledge"))
                                .author(metadata.getOrDefault("author", "VentureVerse"))
                                .freshnessScore(1.0)
                                .authorityScore(1.0)
                                .customMetadata(new HashMap<>(metadata))
                                .build();
        }

        /**
         * Detects category from filename (fallback when metadata not available)
         */
        private String detectCategory(String filename) {
                if (filename.contains("fundraising") || filename.contains("venture") || filename.contains("capital"))
                        return "fundraising";
                if (filename.contains("metrics") || filename.contains("saas") || filename.contains("kpi"))
                        return "metrics";
                if (filename.contains("strategy") || filename.contains("competitive")
                                || filename.contains("go-to-market"))
                        return "strategy";
                if (filename.contains("startup") || filename.contains("lean") || filename.contains("yc")
                                || filename.contains("lifecycle"))
                        return "startup_fundamentals";
                if (filename.contains("product") || filename.contains("market-fit"))
                        return "product";
                if (filename.contains("failure") || filename.contains("risk"))
                        return "risk_management";
                if (filename.contains("scaling") || filename.contains("growth"))
                        return "scaling";
                if (filename.contains("team") || filename.contains("culture"))
                        return "team_culture";
                return "general";
        }

        /**
         * Detects topic from filename (fallback when metadata not available)
         */
        private String detectTopic(String filename) {
                if (filename.contains("fundraising"))
                        return "fundraising_process";
                if (filename.contains("venture") || filename.contains("capital"))
                        return "venture_capital";
                if (filename.contains("metrics") || filename.contains("saas") || filename.contains("kpi"))
                        return "startup_metrics";
                if (filename.contains("competitive"))
                        return "competitive_analysis";
                if (filename.contains("go-to-market") || filename.contains("gtm"))
                        return "gtm_strategy";
                if (filename.contains("lean"))
                        return "lean_methodology";
                if (filename.contains("product") || filename.contains("market-fit"))
                        return "product_market_fit";
                if (filename.contains("yc"))
                        return "ycombinator_advice";
                if (filename.contains("validation"))
                        return "startup_validation";
                if (filename.contains("failure"))
                        return "startup_risks";
                if (filename.contains("lifecycle"))
                        return "startup_lifecycle";
                if (filename.contains("scaling"))
                        return "startup_scaling";
                if (filename.contains("culture") || filename.contains("team"))
                        return "team_building";
                return "general";
        }

        /**
         * Builds metadata map for Qdrant storage
         */
        private Map<String, Object> buildMetadata(KnowledgeUploadRequest request, Chunk chunk) {
                Map<String, Object> metadata = new LinkedHashMap<>();

                // Core document metadata
                metadata.put("title", request.getTitle());
                metadata.put("category", request.getCategory());
                metadata.put("topic", request.getTopic());
                metadata.put("keywords", request.getKeywords());
                metadata.put("difficulty", request.getDifficulty());
                metadata.put("persona", request.getPersona());
                metadata.put("source_type", request.getSourceType());
                metadata.put("author", request.getAuthor());
                metadata.put("freshness_score", request.getFreshnessScore());
                metadata.put("authority_score", request.getAuthorityScore());

                // Chunk metadata
                metadata.put("chunk_number", chunk.getChunkNumber());
                metadata.put("total_chunks", chunk.getTotalChunks());
                metadata.put("section", chunk.getSection());
                metadata.put("ingested_at", java.time.LocalDateTime.now().toString());

                // Add any custom metadata from the request
                if (request.getCustomMetadata() != null) {
                        metadata.putAll(request.getCustomMetadata());
                }

                return metadata;
        }
}