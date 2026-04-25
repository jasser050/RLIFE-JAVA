package com.studyflow.services;

import com.studyflow.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class MoodCameraAnalysisService {
    private final Connection cnx;

    public MoodCameraAnalysisService() {
        this.cnx = MyDataBase.getInstance().getConnection();
        ensureTable();
    }

    public void addAnalysis(
            Integer userId,
            Integer copingSessionId,
            Integer checkinId,
            String mood,
            double confidence,
            String reason,
            String source,
            int sampleCount,
            String voteBreakdown,
            String model
    ) {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("User is required for mood camera analysis.");
        }
        LocalDateTime now = LocalDateTime.now();
        String sql = """
                INSERT INTO mood_camera_analysis
                (user_id, coping_session_id, wellbeing_checkin_id, detected_mood, confidence, reason_text,
                 source_label, sample_count, vote_breakdown, model_name, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement pstm = cnx.prepareStatement(sql)) {
            pstm.setInt(1, userId);
            if (copingSessionId == null || copingSessionId <= 0) {
                pstm.setNull(2, java.sql.Types.INTEGER);
            } else {
                pstm.setInt(2, copingSessionId);
            }
            if (checkinId == null || checkinId <= 0) {
                pstm.setNull(3, java.sql.Types.INTEGER);
            } else {
                pstm.setInt(3, checkinId);
            }
            pstm.setString(4, mood);
            pstm.setDouble(5, confidence);
            pstm.setString(6, reason);
            pstm.setString(7, source);
            pstm.setInt(8, Math.max(1, sampleCount));
            pstm.setString(9, voteBreakdown);
            pstm.setString(10, model);
            pstm.setTimestamp(11, Timestamp.valueOf(now));
            pstm.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save mood camera analysis.", e);
        }
    }

    private void ensureTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS mood_camera_analysis (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    coping_session_id INT NULL,
                    wellbeing_checkin_id INT NULL,
                    detected_mood VARCHAR(20) NOT NULL,
                    confidence DOUBLE NOT NULL,
                    reason_text VARCHAR(255) NULL,
                    source_label VARCHAR(40) NOT NULL,
                    sample_count INT NOT NULL DEFAULT 1,
                    vote_breakdown VARCHAR(255) NULL,
                    model_name VARCHAR(120) NULL,
                    created_at DATETIME NOT NULL
                )
                """;
        try (PreparedStatement pstm = cnx.prepareStatement(sql)) {
            pstm.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure mood_camera_analysis table.", e);
        }
    }
}
