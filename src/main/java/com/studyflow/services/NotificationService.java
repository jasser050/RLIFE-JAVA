package com.studyflow.services;

import com.studyflow.models.Assignment;
import com.studyflow.models.Notification;
import com.studyflow.models.Project;
import com.studyflow.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final Connection connection;

    public NotificationService() {
        this.connection = MyDataBase.getInstance().getConnection();
    }

    public boolean isDatabaseAvailable() {
        return MyDataBase.getInstance().isConnected() && connection != null;
    }

    public void syncDueDateNotifications(int userId, List<Project> projects, List<Assignment> assignments) {
        if (!isDatabaseAvailable()) {
            return;
        }

        LocalDate today = LocalDate.now();
        for (Project project : projects) {
            if (project.getEndDate() == null || project.getEndDate().isAfter(today) || project.isCompleted()) {
                continue;
            }
            String link = "project:" + project.getId();
            if (!notificationExists(userId, "project_deadline", link)) {
                addNotification(
                        userId,
                        "Project deadline reached",
                        "Project \"" + safe(project.getTitle()) + "\" reached its end date on " + DATE_FORMATTER.format(project.getEndDate()) + ".",
                        "project_deadline",
                        link
                );
            }
        }

        for (Assignment assignment : assignments) {
            if (assignment.getEndDate() == null || assignment.getEndDate().isAfter(today) || assignment.isCompleted()) {
                continue;
            }
            String link = "assignment:" + assignment.getId();
            if (!notificationExists(userId, "assignment_deadline", link)) {
                addNotification(
                        userId,
                        "Assignment deadline reached",
                        "Assignment \"" + safe(assignment.getTitle()) + "\" reached its end date on " + DATE_FORMATTER.format(assignment.getEndDate()) + ".",
                        "assignment_deadline",
                        link
                );
            }
        }
    }

    public List<Notification> getRecentByUserId(int userId, int limit) {
        List<Notification> notifications = new ArrayList<>();
        if (!isDatabaseAvailable()) {
            return notifications;
        }

        String sql = "SELECT * FROM notification WHERE user_id = ? ORDER BY id DESC LIMIT ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, Math.max(1, limit));
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                notifications.add(mapNotification(rs));
            }
        } catch (SQLException e) {
            System.out.println("NotificationService.getRecentByUserId: " + e.getMessage());
        }
        return notifications;
    }

    public int countUnreadByUserId(int userId) {
        if (!isDatabaseAvailable()) {
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM notification WHERE user_id = ? AND is_read = 0";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("NotificationService.countUnreadByUserId: " + e.getMessage());
        }
        return 0;
    }

    public void markAllAsRead(int userId) {
        if (!isDatabaseAvailable()) {
            return;
        }

        String sql = "UPDATE notification SET is_read = 1 WHERE user_id = ? AND is_read = 0";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("NotificationService.markAllAsRead: " + e.getMessage());
        }
    }

    public void markAsRead(int notificationId, int userId) {
        if (!isDatabaseAvailable() || notificationId <= 0 || userId <= 0) {
            return;
        }

        String sql = "UPDATE notification SET is_read = 1 WHERE id = ? AND user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, notificationId);
            statement.setInt(2, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("NotificationService.markAsRead: " + e.getMessage());
        }
    }

    public void addNotificationForUser(int userId, String title, String message, String type, String link) {
        if (!isDatabaseAvailable()) {
            return;
        }
        addNotification(userId, title, message, type, link);
    }

    private void addNotification(int userId, String title, String message, String type, String link) {
        String sql = "INSERT INTO notification (title, message, created_at, is_read, user_id, type, link) VALUES (?, ?, ?, 0, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.setString(2, message);
            statement.setString(3, TIMESTAMP_FORMATTER.format(LocalDateTime.now()));
            statement.setInt(4, userId);
            statement.setString(5, type);
            statement.setString(6, link);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("NotificationService.addNotification: " + e.getMessage());
        }
    }

    private boolean notificationExists(int userId, String type, String link) {
        String sql = "SELECT id FROM notification WHERE user_id = ? AND type = ? AND link = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, type);
            statement.setString(3, link);
            ResultSet rs = statement.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("NotificationService.notificationExists: " + e.getMessage());
            return false;
        }
    }

    private Notification mapNotification(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        notification.setId(rs.getInt("id"));
        notification.setTitle(rs.getString("title"));
        notification.setMessage(rs.getString("message"));
        notification.setCreatedAt(rs.getString("created_at"));
        notification.setRead(rs.getBoolean("is_read"));
        notification.setUserId(rs.getInt("user_id"));
        notification.setType(rs.getString("type"));
        notification.setLink(rs.getString("link"));
        return notification;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
