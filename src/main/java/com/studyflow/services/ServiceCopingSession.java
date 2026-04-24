package com.studyflow.services;

import com.studyflow.models.CopingSession;
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

public class ServiceCopingSession {
    private final Connection cnx;

    public ServiceCopingSession() {
        this.cnx = MyDataBase.getInstance().getConnection();
        if (this.cnx != null) {
            ensureTable();
        }
    }

    public CopingSession startSession(Integer userId, String toolKey, String toolName, int durationSeconds) {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("User is required to start a coping session.");
        }
        LocalDateTime now = LocalDateTime.now();
        String sql = """
                INSERT INTO coping_session
                (tool_key, tool_name, duration_seconds, actual_seconds, status, started_at, finished_at, created_at, updated_at, user_id)
                VALUES (?, ?, ?, NULL, 'started', ?, NULL, ?, NULL, ?)
                """;
        try (PreparedStatement pstm = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setString(1, toolKey);
            pstm.setString(2, toolName);
            pstm.setInt(3, durationSeconds);
            pstm.setTimestamp(4, Timestamp.valueOf(now));
            pstm.setTimestamp(5, Timestamp.valueOf(now));
            pstm.setInt(6, userId);
            pstm.executeUpdate();

            CopingSession session = new CopingSession();
            session.setToolKey(toolKey);
            session.setToolName(toolName);
            session.setDurationSeconds(durationSeconds);
            session.setStatus("started");
            session.setStartedAt(now);
            session.setCreatedAt(now);
            session.setUserId(userId);

            ResultSet rs = pstm.getGeneratedKeys();
            if (rs.next()) {
                session.setId(rs.getInt(1));
            }
            return session;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to start coping session.", e);
        }
    }

    public void finishSession(int sessionId, Integer userId, String status, Integer actualSeconds) {
        if (sessionId <= 0 || userId == null || userId <= 0) {
            return;
        }
        String finalStatus = ("cancelled".equalsIgnoreCase(status)) ? "cancelled" : "finished";
        LocalDateTime finishAt = LocalDateTime.now();
        String sql = """
                UPDATE coping_session
                SET status = ?, actual_seconds = ?, finished_at = ?, updated_at = ?
                WHERE id = ? AND user_id = ? AND status = 'started'
                """;
        try (PreparedStatement pstm = cnx.prepareStatement(sql)) {
            pstm.setString(1, finalStatus);
            if (actualSeconds == null) {
                pstm.setNull(2, java.sql.Types.INTEGER);
            } else {
                pstm.setInt(2, Math.max(1, actualSeconds));
            }
            pstm.setTimestamp(3, Timestamp.valueOf(finishAt));
            pstm.setTimestamp(4, Timestamp.valueOf(finishAt));
            pstm.setInt(5, sessionId);
            pstm.setInt(6, userId);
            pstm.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to finish coping session.", e);
        }
    }

    public List<CopingSession> findRecentByUser(Integer userId, int limit) {
        if (userId == null || userId <= 0) {
            return List.of();
        }
        String sql = """
                SELECT id, tool_key, tool_name, duration_seconds, actual_seconds, status,
                       started_at, finished_at, created_at, updated_at, user_id
                FROM coping_session
                WHERE user_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """;
        List<CopingSession> out = new ArrayList<>();
        try (PreparedStatement pstm = cnx.prepareStatement(sql)) {
            pstm.setInt(1, userId);
            pstm.setInt(2, Math.max(1, limit));
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                out.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load coping sessions.", e);
        }
        return out;
    }

    private CopingSession map(ResultSet rs) throws SQLException {
        CopingSession item = new CopingSession();
        item.setId(rs.getInt("id"));
        item.setToolKey(rs.getString("tool_key"));
        item.setToolName(rs.getString("tool_name"));
        item.setDurationSeconds(rs.getInt("duration_seconds"));
        int actual = rs.getInt("actual_seconds");
        item.setActualSeconds(rs.wasNull() ? null : actual);
        item.setStatus(rs.getString("status"));

        Timestamp startedTs = rs.getTimestamp("started_at");
        Timestamp finishedTs = rs.getTimestamp("finished_at");
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        item.setStartedAt(startedTs != null ? startedTs.toLocalDateTime() : null);
        item.setFinishedAt(finishedTs != null ? finishedTs.toLocalDateTime() : null);
        item.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);
        item.setUpdatedAt(updatedTs != null ? updatedTs.toLocalDateTime() : null);
        item.setUserId(rs.getInt("user_id"));
        return item;
    }

    private void ensureTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS coping_session (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    tool_key VARCHAR(50) NOT NULL,
                    tool_name VARCHAR(120) NOT NULL,
                    duration_seconds INT NOT NULL DEFAULT 0,
                    actual_seconds INT NULL,
                    status VARCHAR(20) NOT NULL,
                    started_at DATETIME NOT NULL,
                    finished_at DATETIME NULL,
                    created_at DATETIME NOT NULL,
                    updated_at DATETIME NULL,
                    user_id INT NULL
                )
                """;
        try (PreparedStatement pstm = cnx.prepareStatement(sql)) {
            pstm.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure coping_session table.", e);
        }
    }
}
