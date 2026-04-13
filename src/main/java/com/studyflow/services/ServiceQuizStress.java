package com.studyflow.services;

import com.studyflow.models.QuizStress;
import com.studyflow.utils.MyDataBase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServiceQuizStress {

    private Connection cnx;

    public ServiceQuizStress() {
        cnx = MyDataBase.getInstance().getConnection();
        if (cnx != null) {
            ensureTable();
            ensureUserIdColumn();
        }
    }

    public void add(QuizStress quiz) {
        ensureConnectionAvailable();
        if (cnx == null) {
            throw new RuntimeException("Database connection is unavailable.");
        }

        String sql = """
                INSERT INTO quiz_stress
                (quiz_date_quiz, answers_quiz, total_score_quiz, stress_level_quiz, interpretation_quiz,
                 created_with_ai_quiz, ai_model_quiz, ai_prompt_version_quiz, created_at_quiz, updated_at_quiz, user_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement pstm = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setTimestamp(1, Timestamp.valueOf(
                    quiz.getQuizDate() != null ? quiz.getQuizDate() : LocalDateTime.now()));
            pstm.setString(2, toJson(quiz.getAnswers()));
            pstm.setInt(3, quiz.getTotalScore());
            pstm.setString(4, quiz.getStressLevel());
            pstm.setString(5, quiz.getInterpretation());
            pstm.setBoolean(6, quiz.isCreatedWithAi());
            pstm.setString(7, quiz.getAiModel());
            pstm.setString(8, quiz.getAiPromptVersion());
            pstm.setTimestamp(9, Timestamp.valueOf(
                    quiz.getCreatedAt() != null ? quiz.getCreatedAt() : LocalDateTime.now()));
            if (quiz.getUpdatedAt() != null) {
                pstm.setTimestamp(10, Timestamp.valueOf(quiz.getUpdatedAt()));
            } else {
                pstm.setNull(10, Types.TIMESTAMP);
            }
            if (quiz.getUserId() != null) {
                pstm.setInt(11, quiz.getUserId());
            } else {
                pstm.setNull(11, Types.INTEGER);
            }

            pstm.executeUpdate();

            try (ResultSet rs = pstm.getGeneratedKeys()) {
                if (rs.next()) {
                    quiz.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save quiz result", e);
        }
    }

    private String toJson(Map<Integer, Integer> answers) {
        if (answers == null || answers.isEmpty()) {
            return "{}";
        }
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<Integer, Integer> entry : answers.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            builder.append('"').append(entry.getKey()).append('"').append(':').append(entry.getValue());
            first = false;
        }
        builder.append('}');
        return builder.toString();
    }

    public Map<Integer, Integer> parseAnswers(String json) {
        Map<Integer, Integer> answers = new LinkedHashMap<>();
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            return answers;
        }

        String raw = json.trim();
        if (raw.startsWith("{")) {
            raw = raw.substring(1);
        }
        if (raw.endsWith("}")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        if (raw.isBlank()) {
            return answers;
        }

        String[] parts = raw.split(",");
        for (String part : parts) {
            String[] pair = part.split(":");
            if (pair.length != 2) {
                continue;
            }
            String key = pair[0].replace("\"", "").trim();
            String value = pair[1].replace("\"", "").trim();
            try {
                answers.put(Integer.parseInt(key), Integer.parseInt(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return answers;
    }

    private void ensureConnectionAvailable() {
        cnx = MyDataBase.getInstance().getConnection();
    }

    private void ensureTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS quiz_stress (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    quiz_date_quiz DATETIME NOT NULL,
                    answers_quiz JSON NOT NULL,
                    total_score_quiz INT NOT NULL,
                    stress_level_quiz VARCHAR(50) NOT NULL,
                    interpretation_quiz TEXT NOT NULL,
                    created_with_ai_quiz TINYINT(1) NOT NULL DEFAULT 0,
                    ai_model_quiz VARCHAR(255) NULL,
                    ai_prompt_version_quiz VARCHAR(255) NULL,
                    created_at_quiz DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at_quiz DATETIME NULL,
                    user_id INT NULL
                )
                """;
        try (Statement stm = cnx.createStatement()) {
            stm.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to prepare quiz_stress table", e);
        }
    }

    private void ensureUserIdColumn() {
        try {
            DatabaseMetaData meta = cnx.getMetaData();
            boolean hasUserId = false;
            try (ResultSet rs = meta.getColumns(cnx.getCatalog(), null, "quiz_stress", "user_id")) {
                hasUserId = rs.next();
            }
            if (!hasUserId) {
                try (Statement stm = cnx.createStatement()) {
                    stm.execute("ALTER TABLE quiz_stress ADD COLUMN user_id INT NULL");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure user_id column on quiz_stress", e);
        }
    }
}
