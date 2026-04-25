package com.studyflow.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.models.AdminNotification;
import com.studyflow.models.Deck;
import com.studyflow.models.Flashcard;
import com.studyflow.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminNotificationService {

    private static final DateTimeFormatter TITLE_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final ObjectMapper mapper = new ObjectMapper();
    private final DeckService deckService = new DeckService();
    private final FlashcardService flashcardService = new FlashcardService();
    private final AIFlashcardService aiFlashcardService = new AIFlashcardService();

    private Connection getConnection() throws SQLException {
        return MyDataBase.getInstance().getConnection();
    }

    public List<AdminNotification> getPendingNotificationsForDeck(String deckName) {
        ensureAdminNotificationsTable();

        List<AdminNotification> notifications = new ArrayList<>();
        String sql = """
                SELECT *
                  FROM admin_notifications
                 WHERE status = 'pending'
                 ORDER BY created_at DESC, id DESC
                """;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                AdminNotification notification = mapRow(rs);
                if (notification.matchesDeckName(deckName)) {
                    notifications.add(notification);
                }
            }
        } catch (SQLException e) {
            System.err.println("[AdminNotificationService] load notifications error: " + e.getMessage());
        }

        return notifications;
    }

    public GeneratedDeckResult generateSupportDeck(AdminNotification notification) {
        if (notification == null) {
            throw new IllegalArgumentException("Notification is required.");
        }
        if (!notification.isPending()) {
            throw new IllegalStateException("This alert has already been processed.");
        }

        Deck deck = buildSupportDeck(notification);
        deckService.add(deck);

        List<Flashcard> flashcards = generateFlashcards(notification, deck.getIdDeck(), notification.getStudentId());
        for (Flashcard flashcard : flashcards) {
            flashcardService.add(flashcard);
        }

        markResolved(notification.getId(), deck.getIdDeck(), deck.getTitre());
        return new GeneratedDeckResult(deck, flashcards.size());
    }

    public void ensureAdminNotificationsTable() {
        String createSql = """
                CREATE TABLE IF NOT EXISTS admin_notifications (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  student_id INT NOT NULL,
                  student_name VARCHAR(255) NOT NULL,
                  urgency_level VARCHAR(32) NOT NULL,
                  admin_message TEXT NOT NULL,
                  weak_decks_json LONGTEXT NULL,
                  deck_suggestion_json LONGTEXT NULL,
                  status VARCHAR(32) NOT NULL DEFAULT 'pending',
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  generated_deck_id INT NULL,
                  generated_deck_title VARCHAR(255) NULL,
                  processed_at TIMESTAMP NULL DEFAULT NULL
                )
                """;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createSql);
            ensureColumn(statement, "generated_deck_id", "ALTER TABLE admin_notifications ADD COLUMN generated_deck_id INT NULL");
            ensureColumn(statement, "generated_deck_title", "ALTER TABLE admin_notifications ADD COLUMN generated_deck_title VARCHAR(255) NULL");
            ensureColumn(statement, "processed_at", "ALTER TABLE admin_notifications ADD COLUMN processed_at TIMESTAMP NULL DEFAULT NULL");
        } catch (SQLException e) {
            System.err.println("[AdminNotificationService] ensure table error: " + e.getMessage());
        }
    }

    private void ensureColumn(Statement statement, String columnName, String ddl) throws SQLException {
        try {
            statement.execute(ddl);
        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
            if (!msg.contains("duplicate column") && !msg.contains("already exists")) {
                throw e;
            }
        }
    }

    private AdminNotification mapRow(ResultSet rs) throws SQLException {
        AdminNotification notification = new AdminNotification();
        notification.setId(rs.getInt("id"));
        notification.setStudentId(rs.getInt("student_id"));
        notification.setStudentName(rs.getString("student_name"));
        notification.setUrgencyLevel(rs.getString("urgency_level"));
        notification.setAdminMessage(rs.getString("admin_message"));
        notification.setWeakDecksJson(rs.getString("weak_decks_json"));
        notification.setDeckSuggestionJson(rs.getString("deck_suggestion_json"));
        notification.setStatus(rs.getString("status"));
        notification.setGeneratedDeckTitle(rs.getString("generated_deck_title"));

        int generatedDeckId = rs.getInt("generated_deck_id");
        if (!rs.wasNull()) {
            notification.setGeneratedDeckId(generatedDeckId);
        }

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            notification.setCreatedAt(created.toLocalDateTime());
        }

        Timestamp processed = rs.getTimestamp("processed_at");
        if (processed != null) {
            notification.setProcessedAt(processed.toLocalDateTime());
        }

        parseWeakDecks(notification);
        parseDeckSuggestion(notification);
        return notification;
    }

    private void parseWeakDecks(AdminNotification notification) {
        notification.getWeakDeckNames().clear();
        String raw = notification.getWeakDecksJson();
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            JsonNode array = mapper.readTree(raw);
            if (!array.isArray()) {
                return;
            }
            for (JsonNode node : array) {
                String deckName = node.path("deck_name").asText("").trim();
                if (!deckName.isEmpty() && !notification.getWeakDeckNames().contains(deckName)) {
                    notification.getWeakDeckNames().add(deckName);
                }
            }
        } catch (Exception e) {
            System.err.println("[AdminNotificationService] parse weak decks error: " + e.getMessage());
        }
    }

    private void parseDeckSuggestion(AdminNotification notification) {
        notification.getFocusTopics().clear();
        String raw = notification.getDeckSuggestionJson();
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            JsonNode node = mapper.readTree(raw);
            notification.setSuggestionTitle(node.path("title").asText("").trim());
            notification.setSuggestionSubject(node.path("subject").asText("").trim());
            notification.setSuggestionDifficulty(node.path("difficulty").asText("").trim());
            notification.setSuggestionReason(node.path("reason").asText("").trim());

            JsonNode focusTopics = node.path("focus_topics");
            if (focusTopics.isArray()) {
                for (JsonNode topic : focusTopics) {
                    String value = topic.asText("").trim();
                    if (!value.isEmpty()) {
                        notification.getFocusTopics().add(value);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[AdminNotificationService] parse suggestion error: " + e.getMessage());
        }
    }

    private Deck buildSupportDeck(AdminNotification notification) {
        String subject = firstNonBlank(notification.getSuggestionSubject(), firstWeakDeck(notification), "Personalized Support");
        String titleBase = firstNonBlank(notification.getSuggestionTitle(), "AI Support Deck");
        String finalTitle = titleBase + " - " + safe(notification.getStudentName(), "Student") + " - " + LocalDateTime.now().format(TITLE_SUFFIX);
        String level = mapDifficultyToDeckLevel(notification.getSuggestionDifficulty());
        String description = buildDeckDescription(notification);
        return new Deck(notification.getStudentId(), finalTitle, subject, level, description, null, null);
    }

    private String buildDeckDescription(AdminNotification notification) {
        StringBuilder description = new StringBuilder("Personalized support deck generated after low ratings.");
        String reason = safe(notification.getSuggestionReason(), "");
        if (!reason.isBlank()) {
            description.append(" Reason: ").append(reason).append('.');
        }
        if (!notification.getFocusTopics().isEmpty()) {
            description.append(" Focus: ").append(String.join(", ", notification.getFocusTopics())).append('.');
        }
        return description.toString();
    }

    private List<Flashcard> generateFlashcards(AdminNotification notification, int deckId, int createdBy) {
        List<Flashcard> generated = tryGenerateWithAI(notification, deckId, createdBy);
        if (!generated.isEmpty()) {
            return generated;
        }
        return buildFallbackFlashcards(notification, deckId, createdBy);
    }

    private List<Flashcard> tryGenerateWithAI(AdminNotification notification, int deckId, int createdBy) {
        try {
            String difficulty = mapDifficultyToAILevel(notification.getSuggestionDifficulty());
            String prompt = buildAIPrompt(notification);
            int count = Math.max(4, Math.min(8, notification.getFocusTopics().isEmpty() ? 6 : notification.getFocusTopics().size() * 2));
            List<Flashcard> cards = aiFlashcardService.generate(prompt, count, difficulty);
            for (Flashcard card : cards) {
                card.setIdDeck(deckId);
                card.setCreatedBy(createdBy);
                if (card.getNiveauDifficulte() < 1 || card.getNiveauDifficulte() > 3) {
                    card.setNiveauDifficulte(mapDifficultyLevel(notification.getSuggestionDifficulty()));
                }
            }
            return cards;
        } catch (Exception e) {
            System.err.println("[AdminNotificationService] AI flashcard generation fallback: " + e.getMessage());
            return List.of();
        }
    }

    private String buildAIPrompt(AdminNotification notification) {
        return """
                Generate a concise remediation flashcard deck for one struggling student.
                Subject: %s
                Focus topics: %s
                Teacher note: %s
                Keep the cards simple, practical, and beginner-friendly when needed.
                """.formatted(
                firstNonBlank(notification.getSuggestionSubject(), firstWeakDeck(notification), "Personalized Support"),
                notification.getFocusTopicsLabel(),
                firstNonBlank(notification.getSuggestionReason(), notification.getAdminMessage(), "Student needs simpler guided review")
        );
    }

    private List<Flashcard> buildFallbackFlashcards(AdminNotification notification, int deckId, int createdBy) {
        List<String> topics = notification.getFocusTopics().isEmpty()
                ? List.of(firstNonBlank(notification.getSuggestionSubject(), firstWeakDeck(notification), "Core concept"), "Worked example", "Common mistakes")
                : notification.getFocusTopics();

        List<Flashcard> cards = new ArrayList<>();
        int difficulty = mapDifficultyLevel(notification.getSuggestionDifficulty());
        for (String topic : topics) {
            cards.add(buildCard(deckId, createdBy, difficulty,
                    topic + " Basics",
                    "What is the main idea behind " + topic + "?",
                    topic + " is a key concept the student should be able to explain in simple words and identify in practice."));

            cards.add(buildCard(deckId, createdBy, difficulty,
                    topic + " Practice",
                    "How can you quickly review " + topic + " before solving exercises?",
                    "Review the definition, one simple example, one common mistake, then restate the method step by step."));
        }
        return cards.subList(0, Math.min(cards.size(), 8));
    }

    private Flashcard buildCard(int deckId, int createdBy, int difficulty, String title, String question, String answer) {
        Flashcard flashcard = new Flashcard();
        flashcard.setIdDeck(deckId);
        flashcard.setCreatedBy(createdBy);
        flashcard.setNiveauDifficulte(difficulty);
        flashcard.setEtat("new");
        flashcard.setTitre(title);
        flashcard.setQuestion(question);
        flashcard.setReponse(answer);
        return flashcard;
    }

    private void markResolved(int notificationId, int deckId, String deckTitle) {
        String sql = """
                UPDATE admin_notifications
                   SET status = 'resolved',
                       generated_deck_id = ?,
                       generated_deck_title = ?,
                       processed_at = NOW()
                 WHERE id = ?
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, deckId);
            statement.setString(2, deckTitle);
            statement.setInt(3, notificationId);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AdminNotificationService] resolve notification error: " + e.getMessage());
        }
    }

    private String mapDifficultyToDeckLevel(String difficulty) {
        String normalized = safe(difficulty, "").toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "advanced" -> "Advanced";
            case "intermediate" -> "Intermediate";
            default -> "Beginner";
        };
    }

    private String mapDifficultyToAILevel(String difficulty) {
        String normalized = safe(difficulty, "").toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "advanced" -> "Hard";
            case "intermediate" -> "Medium";
            default -> "Easy";
        };
    }

    private int mapDifficultyLevel(String difficulty) {
        String normalized = safe(difficulty, "").toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "advanced" -> 3;
            case "intermediate" -> 2;
            default -> 1;
        };
    }

    private String firstWeakDeck(AdminNotification notification) {
        return notification.getWeakDeckNames().isEmpty() ? "" : notification.getWeakDeckNames().get(0);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record GeneratedDeckResult(Deck deck, int flashcardCount) {}
}
