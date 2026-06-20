package com.ventureverse.ventureverse_api.rag.knowledge.memory;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversationMemory {

    private static final int MAX_TURNS = 10;
    private static final int MAX_TOKENS = 2000;
    private static final long SESSION_TTL_MS = 3600_000; // 1 hour

    private final Map<String, ConversationSession> sessions = new LinkedHashMap<>();

    @Data
    @Builder
    public static class ConversationTurn {
        private String id;
        private String role; // user, assistant
        private String content;
        private long timestamp;
        private List<String> citations;
        private String intent;
    }

    @Data
    @Builder
    public static class ConversationSession {
        private String sessionId;
        private List<ConversationTurn> turns;
        private long createdAt;
        private long lastAccessedAt;
        private Map<String, String> metadata;
    }

    @Data
    @Builder
    public static class ConversationContext {
        private List<ConversationTurn> recentTurns;
        private String conversationSummary;
        private int totalTurns;
        private boolean isCompressed;
    }

    /**
     * Create new conversation session
     */
    public ConversationSession createSession(String sessionId) {
        ConversationSession session = ConversationSession.builder()
                .sessionId(sessionId)
                .turns(new ArrayList<>())
                .createdAt(System.currentTimeMillis())
                .lastAccessedAt(System.currentTimeMillis())
                .metadata(new LinkedHashMap<>())
                .build();
        sessions.put(sessionId, session);
        cleanExpiredSessions();
        return session;
    }

    /**
     * Add a turn to conversation
     */
    public void addTurn(String sessionId, String role, String content, List<String> citations, String intent) {
        ConversationSession session = sessions.computeIfAbsent(sessionId, this::createSession);

        ConversationTurn turn = ConversationTurn.builder()
                .id(UUID.randomUUID().toString())
                .role(role)
                .content(content)
                .timestamp(System.currentTimeMillis())
                .citations(citations != null ? citations : Collections.emptyList())
                .intent(intent)
                .build();

        session.getTurns().add(turn);
        session.setLastAccessedAt(System.currentTimeMillis());

        // Trim old turns if exceeding max
        if (session.getTurns().size() > MAX_TURNS * 2) {
            session.setTurns(session.getTurns().stream()
                    .skip(session.getTurns().size() - MAX_TURNS * 2)
                    .collect(Collectors.toList()));
        }
    }

    /**
     * Get conversation context for query enhancement
     */
    public ConversationContext getContext(String sessionId, String currentQuery) {
        ConversationSession session = sessions.get(sessionId);

        if (session == null) {
            return ConversationContext.builder()
                    .recentTurns(Collections.emptyList())
                    .conversationSummary("")
                    .totalTurns(0)
                    .isCompressed(false)
                    .build();
        }

        session.setLastAccessedAt(System.currentTimeMillis());

        // Get recent turns
        List<ConversationTurn> recentTurns = session.getTurns().stream()
                .skip(Math.max(0, session.getTurns().size() - MAX_TURNS))
                .collect(Collectors.toList());

        // Check if compression needed
        boolean needsCompression = estimateTokens(recentTurns) > MAX_TOKENS;
        String summary = needsCompression ? summarizeConversation(recentTurns) : "";

        return ConversationContext.builder()
                .recentTurns(recentTurns)
                .conversationSummary(summary)
                .totalTurns(session.getTurns().size())
                .isCompressed(needsCompression)
                .build();
    }

    /**
     * Enhance current query with conversation context
     */
    public String enhanceQueryWithContext(String sessionId, String currentQuery) {
        ConversationContext context = getContext(sessionId, currentQuery);

        if (context.getTotalTurns() == 0) {
            return currentQuery;
        }

        StringBuilder enhanced = new StringBuilder();

        // Add conversation summary if compressed
        if (context.isCompressed() && !context.getConversationSummary().isEmpty()) {
            enhanced.append("Previous conversation summary: ")
                    .append(context.getConversationSummary())
                    .append("\n\n");
        }

        // Add last 2 turns for immediate context
        List<ConversationTurn> recentTurns = context.getRecentTurns();
        if (!recentTurns.isEmpty()) {
            enhanced.append("Recent conversation:\n");
            int start = Math.max(0, recentTurns.size() - 4);
            for (int i = start; i < recentTurns.size(); i++) {
                ConversationTurn turn = recentTurns.get(i);
                enhanced.append(turn.getRole()).append(": ")
                        .append(truncate(turn.getContent(), 200))
                        .append("\n");
            }
            enhanced.append("\n");
        }

        enhanced.append("Current question: ").append(currentQuery);
        return enhanced.toString();
    }

    /**
     * Get recent topics from conversation
     */
    public List<String> getRecentTopics(String sessionId) {
        ConversationSession session = sessions.get(sessionId);
        if (session == null)
            return Collections.emptyList();

        return session.getTurns().stream()
                .filter(t -> t.getIntent() != null)
                .map(ConversationTurn::getIntent)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Delete a conversation session
     */
    public void deleteSession(String sessionId) {
        sessions.remove(sessionId);
    }

    /**
     * Get session statistics
     */
    public Map<String, Object> getSessionStats(String sessionId) {
        ConversationSession session = sessions.get(sessionId);
        if (session == null) {
            return Map.of("exists", false);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("exists", true);
        stats.put("sessionId", sessionId);
        stats.put("totalTurns", session.getTurns().size());
        stats.put("createdAt", Instant.ofEpochMilli(session.getCreatedAt()).toString());
        stats.put("lastAccessedAt", Instant.ofEpochMilli(session.getLastAccessedAt()).toString());
        stats.put("ageMinutes", (System.currentTimeMillis() - session.getCreatedAt()) / 60000);

        Map<String, Long> roleCount = session.getTurns().stream()
                .collect(Collectors.groupingBy(ConversationTurn::getRole, Collectors.counting()));
        stats.put("turnsByRole", roleCount);

        return stats;
    }

    /**
     * Get all active sessions count
     */
    public Map<String, Object> getGlobalStats() {
        cleanExpiredSessions();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("activeSessions", sessions.size());
        stats.put("totalTurns", sessions.values().stream()
                .mapToInt(s -> s.getTurns().size()).sum());
        stats.put("averageTurnsPerSession", sessions.isEmpty() ? 0
                : sessions.values().stream().mapToInt(s -> s.getTurns().size()).average().orElse(0));

        return stats;
    }

    // ==================== PRIVATE METHODS ====================

    private String summarizeConversation(List<ConversationTurn> turns) {
        if (turns.isEmpty())
            return "";

        StringBuilder summary = new StringBuilder();

        // Extract key topics from user questions
        List<String> userQuestions = turns.stream()
                .filter(t -> "user".equals(t.getRole()))
                .map(ConversationTurn::getContent)
                .collect(Collectors.toList());

        // Extract intents
        List<String> intents = turns.stream()
                .filter(t -> t.getIntent() != null)
                .map(ConversationTurn::getIntent)
                .distinct()
                .collect(Collectors.toList());

        summary.append("Topics discussed: ").append(String.join(", ", intents)).append(". ");
        summary.append("Total exchanges: ").append(turns.size() / 2).append(". ");

        if (!userQuestions.isEmpty()) {
            summary.append("Last question: ").append(truncate(userQuestions.get(userQuestions.size() - 1), 100));
        }

        return summary.toString();
    }

    private int estimateTokens(List<ConversationTurn> turns) {
        return turns.stream()
                .mapToInt(t -> (int) Math.ceil(t.getContent().length() / 4.0))
                .sum();
    }

    private String truncate(String text, int maxLength) {
        if (text == null)
            return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    private void cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> now - entry.getValue().getLastAccessedAt() > SESSION_TTL_MS);
    }
}