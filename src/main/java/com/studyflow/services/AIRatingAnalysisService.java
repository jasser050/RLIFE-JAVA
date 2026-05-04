package com.studyflow.services;

import com.studyflow.models.Rating;
import com.studyflow.utils.MyDataBase;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AIRatingAnalysisService {

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL = "mistralai/mistral-7b-instruct";
    private static final String APP_REFERER = "https://rlife-app.com";
    private static final String APP_TITLE = "RLife Flashcard App";

    private final String apiKey;

    public AIRatingAnalysisService() {
        String key = System.getenv("OPENROUTER_API_KEY");
        if (key == null || key.isBlank()) {
            key = System.getProperty("OPENROUTER_API_KEY", "");
        }
        this.apiKey = key;
    }

    public void analyzeRatings(int userId, String studentName, List<Rating> allRatings) {
        if (allRatings == null || allRatings.isEmpty()) {
            return;
        }

        List<Rating> weakRatings = allRatings.stream().filter(Rating::isWeak).toList();
        if (weakRatings.isEmpty()) {
            return;
        }

        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("[AIRating] OPENROUTER_API_KEY missing, using fallback analysis.");
            saveFallbackNotification(userId, studentName, weakRatings, "OPENROUTER_API_KEY missing");
            return;
        }

        try {
            String prompt = buildPrompt(studentName, allRatings, weakRatings);
            String requestBody = buildRequestBody(prompt);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENROUTER_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("HTTP-Referer", APP_REFERER)
                    .header("X-Title", APP_TITLE)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("[AIRating] OpenRouter status " + response.statusCode());
                saveFallbackNotification(userId, studentName, weakRatings, "OpenRouter status " + response.statusCode());
                return;
            }

            String content = extractContent(response.body());
            if (content == null || content.isBlank()) {
                saveFallbackNotification(userId, studentName, weakRatings, "OpenRouter returned empty content");
                return;
            }

            saveNotificationFromAIResponse(userId, studentName, content, weakRatings);
        } catch (Exception e) {
            System.err.println("[AIRating] analyzeRatings error: " + e.getMessage());
            saveFallbackNotification(userId, studentName, weakRatings, e.getMessage());
        }
    }

    private String buildPrompt(String studentName, List<Rating> allRatings, List<Rating> weakRatings) {
        StringBuilder allLines = new StringBuilder();
        StringBuilder weakLines = new StringBuilder();

        for (Rating rating : allRatings) {
            allLines.append("- ")
                    .append(rating.getDeckName() != null ? rating.getDeckName() : "Deck #" + rating.getDeckId())
                    .append(" (")
                    .append(rating.getDeckSubject() != null ? rating.getDeckSubject() : "?")
                    .append("): ")
                    .append(rating.getStars())
                    .append("/5 - ")
                    .append(rating.getStarsLabel())
                    .append('\n');
        }

        for (Rating rating : weakRatings) {
            weakLines.append("- ")
                    .append(rating.getDeckName() != null ? rating.getDeckName() : "Deck #" + rating.getDeckId())
                    .append(" (")
                    .append(rating.getDeckSubject() != null ? rating.getDeckSubject() : "?")
                    .append("): ")
                    .append(rating.getStars())
                    .append("/5 - ")
                    .append(rating.getStarsLabel())
                    .append('\n');
        }

        return """
                You are an expert educational assistant analyzing flashcard deck ratings from a student.

                Student name: %s

                All deck ratings:
                %s

                Weak decks (rated 3 or below):
                %s

                Return ONLY one valid JSON object:
                {
                  "urgency_level": "critical",
                  "subject_pattern": "short summary",
                  "admin_message": "French actionable admin message",
                  "deck_suggestion": {
                    "title": "Suggested deck title",
                    "subject": "Subject name",
                    "difficulty": "beginner",
                    "focus_topics": ["topic1", "topic2", "topic3"],
                    "reason": "Short reason"
                  }
                }

                Rules:
                - urgency_level must be critical or moderate when weak decks exist
                - admin_message must be in French
                - difficulty must be beginner, intermediate or advanced
                - focus_topics must contain 2 to 4 specific topics
                """.formatted(studentName, allLines, weakLines);
    }

    private String buildRequestBody(String userMessage) {
        String escaped = userMessage
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");

        return """
                {
                  "model": "%s",
                  "temperature": 0.3,
                  "max_tokens": 800,
                  "messages": [
                    {
                      "role": "system",
                      "content": "You are an educational AI. Return only valid JSON."
                    },
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ]
                }
                """.formatted(MODEL, escaped);
    }

    private String extractContent(String responseBody) {
        try {
            int contentIndex = responseBody.indexOf("\"content\":");
            if (contentIndex < 0) {
                return null;
            }
            int startQuote = responseBody.indexOf('"', contentIndex + 10);
            if (startQuote < 0) {
                return null;
            }
            String raw = responseBody.substring(startQuote + 1)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");

            int jsonStart = raw.indexOf('{');
            int jsonEnd = raw.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                return raw.substring(jsonStart, jsonEnd + 1).trim();
            }
        } catch (Exception e) {
            System.err.println("[AIRating] extractContent error: " + e.getMessage());
        }
        return null;
    }

    private void saveNotificationFromAIResponse(int userId, String studentName, String aiJson, List<Rating> weakRatings) {
        try {
            String urgency = extractJsonString(aiJson, "urgency_level").toLowerCase(Locale.ROOT);
            String adminMessage = extractJsonString(aiJson, "admin_message");
            String deckSuggestionJson = extractJsonBlock(aiJson, "deck_suggestion");

            if (!"critical".equals(urgency) && !"moderate".equals(urgency)) {
                saveFallbackNotification(userId, studentName, weakRatings, "AI returned unsupported urgency");
                return;
            }

            if (adminMessage == null || adminMessage.isBlank()) {
                saveFallbackNotification(userId, studentName, weakRatings, "AI returned empty admin_message");
                return;
            }

            saveAdminNotification(
                    userId,
                    studentName,
                    urgency,
                    adminMessage,
                    buildWeakDecksJson(weakRatings),
                    deckSuggestionJson == null || deckSuggestionJson.isBlank()
                            ? buildFallbackDeckSuggestionJson(weakRatings)
                            : deckSuggestionJson
            );
        } catch (Exception e) {
            System.err.println("[AIRating] saveNotificationFromAIResponse error: " + e.getMessage());
            saveFallbackNotification(userId, studentName, weakRatings, e.getMessage());
        }
    }

    private void saveFallbackNotification(int userId, String studentName, List<Rating> weakRatings, String reason) {
        String urgency = weakRatings.stream().anyMatch(Rating::isCritical) ? "critical" : "moderate";
        String subject = inferSubject(weakRatings);
        String focusTopics = inferFocusTopics(weakRatings);
        String weakestDecks = weakRatings.stream()
                .limit(3)
                .map(r -> safe(r.getDeckName(), "Deck #" + r.getDeckId()) + " (" + r.getStars() + "/5)")
                .reduce((a, b) -> a + ", " + b)
                .orElse("no deck");

        String adminMessage = "Alerte IA: l'etudiant " + safe(studentName, "Unknown student")
                + " a donne de mauvaises notes sur " + weakRatings.size() + " deck(s). "
                + "Sujet principal detecte: " + subject + ". "
                + "Decks faibles: " + weakestDecks + ". "
                + "Recommandation: creer un deck personnalise plus simple axe sur " + focusTopics + ".";

        saveAdminNotification(
                userId,
                studentName,
                urgency,
                adminMessage,
                buildWeakDecksJson(weakRatings),
                buildFallbackDeckSuggestionJson(weakRatings)
        );

        System.out.println("[AIRating] Fallback admin notification saved (" + reason + ")");
    }

    private String buildWeakDecksJson(List<Rating> weakRatings) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < weakRatings.size(); i++) {
            Rating rating = weakRatings.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"deck_name\":\"")
                    .append(escape(safe(rating.getDeckName(), "Deck #" + rating.getDeckId())))
                    .append("\",\"stars\":")
                    .append(rating.getStars())
                    .append(",\"label\":\"")
                    .append(escape(rating.getStarsLabel()))
                    .append("\"}");
        }
        json.append(']');
        return json.toString();
    }

    private String buildFallbackDeckSuggestionJson(List<Rating> weakRatings) {
        String subject = inferSubject(weakRatings);
        String difficulty = weakRatings.stream().anyMatch(Rating::isCritical) ? "beginner" : "intermediate";
        List<String> topics = new ArrayList<>(inferTopicsSet(weakRatings));

        StringBuilder json = new StringBuilder("{");
        json.append("\"title\":\"").append(escape("AI Support Deck - " + subject)).append("\",");
        json.append("\"subject\":\"").append(escape(subject)).append("\",");
        json.append("\"difficulty\":\"").append(difficulty).append("\",");
        json.append("\"focus_topics\":[");
        for (int i = 0; i < Math.min(4, topics.size()); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append("\"").append(escape(topics.get(i))).append("\"");
        }
        json.append("],");
        json.append("\"reason\":\"")
                .append(escape("Generated after repeated weak ratings on " + subject))
                .append("\"}");
        return json.toString();
    }

    private String inferSubject(List<Rating> weakRatings) {
        String fallback = "";
        for (Rating rating : weakRatings) {
            if (rating.getDeckSubject() != null && !rating.getDeckSubject().isBlank()) {
                return rating.getDeckSubject().trim();
            }
            if (fallback.isBlank() && rating.getDeckName() != null && !rating.getDeckName().isBlank()) {
                fallback = rating.getDeckName().trim();
            }
        }
        return fallback.isBlank() ? "Personalized Support" : fallback;
    }

    private String inferFocusTopics(List<Rating> weakRatings) {
        return String.join(", ", inferTopicsSet(weakRatings));
    }

    private Set<String> inferTopicsSet(List<Rating> weakRatings) {
        Set<String> topics = new LinkedHashSet<>();
        for (Rating rating : weakRatings) {
            if (rating.getDeckName() != null && !rating.getDeckName().isBlank()) {
                topics.add(rating.getDeckName().trim());
            } else if (rating.getDeckSubject() != null && !rating.getDeckSubject().isBlank()) {
                topics.add(rating.getDeckSubject().trim());
            }
            if (topics.size() == 4) {
                break;
            }
        }
        if (topics.isEmpty()) {
            topics.add("core concepts");
            topics.add("guided examples");
        }
        return topics;
    }

    private void saveAdminNotification(int studentId, String studentName, String urgencyLevel,
                                       String adminMessage, String weakDecksJson, String deckSuggestionJson) {
        ensureAdminNotificationsTable();
        if (!hasAdminNotificationsTable()) {
            System.err.println("[AIRating] admin_notifications table missing. Notification skipped.");
            return;
        }

        String sql = """
                INSERT INTO admin_notifications
                  (student_id, student_name, urgency_level, admin_message,
                   weak_decks_json, deck_suggestion_json, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, 'pending', NOW())
                """;

        try (Connection connection = MyDataBase.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.setString(2, safe(studentName, "Unknown student"));
            statement.setString(3, urgencyLevel.toLowerCase(Locale.ROOT));
            statement.setString(4, adminMessage);
            statement.setString(5, weakDecksJson);
            statement.setString(6, deckSuggestionJson);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AIRating] saveAdminNotification SQL error: " + e.getMessage());
        }
    }

    private boolean hasAdminNotificationsTable() {
        try (Connection connection = MyDataBase.getInstance().getConnection()) {
            return connection.getMetaData().getTables(null, null, "admin_notifications", null).next();
        } catch (SQLException e) {
            System.err.println("[AIRating] table check error: " + e.getMessage());
            return false;
        }
    }

    private void ensureAdminNotificationsTable() {
        if (hasAdminNotificationsTable()) {
            return;
        }

        String sql = """
                CREATE TABLE IF NOT EXISTS admin_notifications (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  student_id INT NOT NULL,
                  student_name VARCHAR(255) NOT NULL,
                  urgency_level VARCHAR(32) NOT NULL,
                  admin_message TEXT NOT NULL,
                  weak_decks_json LONGTEXT NULL,
                  deck_suggestion_json LONGTEXT NULL,
                  status VARCHAR(32) NOT NULL DEFAULT 'pending',
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try (Connection connection = MyDataBase.getInstance().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            System.err.println("[AIRating] create admin_notifications error: " + e.getMessage());
        }
    }

    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIndex = json.indexOf(search);
        if (keyIndex < 0) {
            return "";
        }
        int colon = json.indexOf(':', keyIndex + search.length());
        if (colon < 0) {
            return "";
        }
        int start = json.indexOf('"', colon + 1);
        if (start < 0) {
            return "";
        }
        start++;
        StringBuilder value = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char current = json.charAt(i);
            if (current == '\\' && i + 1 < json.length()) {
                i++;
                char next = json.charAt(i);
                switch (next) {
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    case 'n' -> value.append('\n');
                    case 't' -> value.append('\t');
                    default -> value.append(next);
                }
            } else if (current == '"') {
                break;
            } else {
                value.append(current);
            }
        }
        return value.toString().trim();
    }

    private String extractJsonBlock(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIndex = json.indexOf(search);
        if (keyIndex < 0) {
            return null;
        }
        int blockStart = json.indexOf('{', keyIndex);
        if (blockStart < 0) {
            return null;
        }

        int depth = 0;
        boolean inString = false;
        for (int i = blockStart; i < json.length(); i++) {
            char current = json.charAt(i);
            if (current == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return json.substring(blockStart, i + 1);
                }
            }
        }
        return null;
    }

    private String escape(String value) {
        return safe(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
