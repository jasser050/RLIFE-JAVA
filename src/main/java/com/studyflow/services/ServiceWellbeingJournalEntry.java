package com.studyflow.services;

import com.studyflow.models.WellbeingJournalEntry;
import com.studyflow.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceWellbeingJournalEntry {
    private final Connection cnx;

    public ServiceWellbeingJournalEntry() {
        this.cnx = MyDataBase.getInstance().getConnection();
        if (this.cnx != null) {
            ensureTable();
        }
    }

    public List<WellbeingJournalEntry> findByUser(Integer userId, int limit) {
        if (userId == null || userId <= 0) {
            return List.of();
        }
        if (this.cnx != null) {
            return List.of();
        }
        String sql = """
                SELECT id, content, language_code, input_mode, created_at, updated_at, user_id
                FROM wellbeing_journal_entry
                WHERE user_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """;
        List<WellbeingJournalEntry> out = new ArrayList<>();
        try (PreparedStatement pstm = cnx.prepareStatement(sql)) {
            pstm.setInt(1, userId);
            pstm.setInt(2, Math.max(1, limit));
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                out.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load journal entries.", e);
        }
        return out;
    }

    public void add(WellbeingJournalEntry item) {
        if (item.getUserId() == null || item.getUserId() <= 0) {
            throw new RuntimeException("User is required for journal entry.");
        }
        String sql = """
                INSERT INTO wellbeing_journal_entry
                (content, language_code, input_mode, created_at, updated_at, user_id)
                VALUES (?, ?, ?, ?, NULL, ?)
                """;
        LocalDateTime now = item.getCreatedAt() == null ? LocalDateTime.now() : item.getCreatedAt();
        try (PreparedStatement pstm = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setString(1, item.getContent());
            pstm.setString(2, item.getLanguageCode());
            pstm.setString(3, item.getInputMode());
            pstm.setTimestamp(4, Timestamp.valueOf(now));
            pstm.setInt(5, item.getUserId());
            pstm.executeUpdate();

            ResultSet rs = pstm.getGeneratedKeys();
            if (rs.next()) {
                item.setId(rs.getInt(1));
            }
            item.setCreatedAt(now);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add journal entry.", e);
        }
    }

    public void update(WellbeingJournalEntry item) {
        if (item.getId() <= 0 || item.getUserId() == null || item.getUserId() <= 0) {
            throw new RuntimeException("Invalid journal entry update request.");
        }
        String sql = """
                UPDATE wellbeing_journal_entry
                SET content = ?, language_code = ?, input_mode = ?, updated_at = ?
                WHERE id = ? AND user_id = ?
                """;
        LocalDateTime now = LocalDateTime.now();
        try (PreparedStatement pstm = cnx.prepareStatement(sql)) {
            pstm.setString(1, item.getContent());
            pstm.setString(2, item.getLanguageCode());
            pstm.setString(3, item.getInputMode());
            pstm.setTimestamp(4, Timestamp.valueOf(now));
            pstm.setInt(5, item.getId());
            pstm.setInt(6, item.getUserId());
            int affected = pstm.executeUpdate();
            if (affected == 0) {
                throw new RuntimeException("Journal entry not found.");
            }
            item.setUpdatedAt(now);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update journal entry.", e);
        }
    }

    public void delete(int entryId, Integer userId) {
        if (entryId <= 0 || userId == null || userId <= 0) {
            throw new RuntimeException("Invalid journal delete request.");
        }
        String sql = "DELETE FROM wellbeing_journal_entry WHERE id = ? AND user_id = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(sql)) {
            pstm.setInt(1, entryId);
            pstm.setInt(2, userId);
            int affected = pstm.executeUpdate();
            if (affected == 0) {
                throw new RuntimeException("Journal entry not found.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete journal entry.", e);
        }
    }

    private WellbeingJournalEntry map(ResultSet rs) throws SQLException {
        WellbeingJournalEntry item = new WellbeingJournalEntry();
        item.setId(rs.getInt("id"));
        item.setContent(rs.getString("content"));
        item.setLanguageCode(rs.getString("language_code"));
        item.setInputMode(rs.getString("input_mode"));
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        item.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);
        item.setUpdatedAt(updatedTs != null ? updatedTs.toLocalDateTime() : null);
        item.setUserId(rs.getInt("user_id"));
        return item;
    }

    private void ensureTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS wellbeing_journal_entry (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    content TEXT NOT NULL,
                    language_code VARCHAR(20) NULL,
                    input_mode VARCHAR(20) NOT NULL DEFAULT 'text',
                    created_at DATETIME NOT NULL,
                    updated_at DATETIME NULL,
                    user_id INT NOT NULL
                )
                """;
        try (PreparedStatement pstm = cnx.prepareStatement(sql)) {
            pstm.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure wellbeing_journal_entry table.", e);
        }
    }
}
