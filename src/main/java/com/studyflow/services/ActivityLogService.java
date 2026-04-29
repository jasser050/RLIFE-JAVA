package com.studyflow.services;

import com.studyflow.models.ActivityLogEntry;
import com.studyflow.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogService {
    private final Connection connection;

    public ActivityLogService() {
        this.connection = MyDataBase.getInstance().getConnection();
        ensureActivityTable();
    }

    public void addActivity(String entityType, int entityId, int userId, String actionType, String description) {
        if (!isDatabaseAvailable() || entityId <= 0 || userId <= 0) {
            return;
        }
        String sql = "INSERT INTO activity_log (entity_type, entity_id, user_id, action_type, description, created_at) VALUES (?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, safe(entityType));
            statement.setInt(2, entityId);
            statement.setInt(3, userId);
            statement.setString(4, safe(actionType));
            statement.setString(5, safe(description));
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ActivityLogService.addActivity: " + e.getMessage());
        }
    }

    public List<ActivityLogEntry> getRecentActivity(String entityType, int entityId, int limit) {
        List<ActivityLogEntry> entries = new ArrayList<>();
        if (!isDatabaseAvailable() || entityId <= 0) {
            return entries;
        }

        String sql = "SELECT al.id, al.entity_type, al.entity_id, al.user_id, al.action_type, al.description, al.created_at, " +
                "CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')) AS actor_name, u.username, u.email " +
                "FROM activity_log al " +
                "LEFT JOIN `user` u ON u.id = al.user_id " +
                "WHERE al.entity_type = ? AND al.entity_id = ? " +
                "ORDER BY al.id DESC LIMIT ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, safe(entityType));
            statement.setInt(2, entityId);
            statement.setInt(3, Math.max(1, limit));
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                ActivityLogEntry entry = new ActivityLogEntry();
                entry.setId(rs.getInt("id"));
                entry.setEntityType(rs.getString("entity_type"));
                entry.setEntityId(rs.getInt("entity_id"));
                entry.setUserId(rs.getInt("user_id"));
                entry.setActionType(rs.getString("action_type"));
                entry.setDescription(rs.getString("description"));
                entry.setCreatedAt(rs.getString("created_at"));

                String actorName = safe(rs.getString("actor_name")).trim();
                if (actorName.isEmpty()) {
                    actorName = safe(rs.getString("username"));
                }
                if (actorName.isEmpty()) {
                    actorName = safe(rs.getString("email"));
                }
                entry.setActorName(actorName.isEmpty() ? "Unknown user" : actorName);
                entries.add(entry);
            }
        } catch (SQLException e) {
            System.out.println("ActivityLogService.getRecentActivity: " + e.getMessage());
        }
        return entries;
    }

    private boolean isDatabaseAvailable() {
        return MyDataBase.getInstance().isConnected() && connection != null;
    }

    private void ensureActivityTable() {
        if (!isDatabaseAvailable()) {
            return;
        }
        String sql = "CREATE TABLE IF NOT EXISTS activity_log (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "entity_type VARCHAR(32) NOT NULL, " +
                "entity_id INT NOT NULL, " +
                "user_id INT NOT NULL, " +
                "action_type VARCHAR(64) NOT NULL, " +
                "description VARCHAR(500) NOT NULL, " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "INDEX idx_activity_entity (entity_type, entity_id), " +
                "INDEX idx_activity_user (user_id), " +
                "CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE" +
                ")";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("ActivityLogService.ensureActivityTable: " + e.getMessage());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
