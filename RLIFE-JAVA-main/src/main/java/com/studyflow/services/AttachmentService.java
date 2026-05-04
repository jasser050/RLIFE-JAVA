package com.studyflow.services;

import com.studyflow.models.AttachmentItem;
import com.studyflow.utils.MyDataBase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AttachmentService {
    private final Connection connection;
    private static final Path ROOT_DIR = Path.of(System.getProperty("user.home"), ".rlife", "attachments");

    public AttachmentService() {
        this.connection = MyDataBase.getInstance().getConnection();
        ensureAttachmentTable();
    }

    public AttachmentItem saveAttachment(String entityType, int entityId, int userId, Path sourceFile) {
        if (!isDatabaseAvailable() || entityId <= 0 || userId <= 0 || sourceFile == null || !Files.exists(sourceFile)) {
            return null;
        }
        try {
            Files.createDirectories(ROOT_DIR.resolve(safe(entityType)).resolve(String.valueOf(entityId)));
            String originalName = sourceFile.getFileName().toString();
            String storedName = System.currentTimeMillis() + "-" + sanitizeFileName(originalName);
            Path targetFile = ROOT_DIR.resolve(safe(entityType)).resolve(String.valueOf(entityId)).resolve(storedName);
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

            String sql = "INSERT INTO attachment_item (entity_type, entity_id, user_id, original_name, file_path, mime_type, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, safe(entityType));
                statement.setInt(2, entityId);
                statement.setInt(3, userId);
                statement.setString(4, originalName);
                statement.setString(5, targetFile.toString());
                statement.setString(6, safe(detectMimeType(sourceFile)));
                if (statement.executeUpdate() <= 0) {
                    return null;
                }
                AttachmentItem item = new AttachmentItem();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        item.setId(keys.getInt(1));
                    }
                }
                item.setEntityType(entityType);
                item.setEntityId(entityId);
                item.setUserId(userId);
                item.setOriginalName(originalName);
                item.setFilePath(targetFile.toString());
                item.setMimeType(detectMimeType(sourceFile));
                return item;
            }
        } catch (IOException | SQLException e) {
            System.out.println("AttachmentService.saveAttachment: " + e.getMessage());
        }
        return null;
    }

    public List<AttachmentItem> getAttachments(String entityType, int entityId) {
        List<AttachmentItem> items = new ArrayList<>();
        if (!isDatabaseAvailable() || entityId <= 0) {
            return items;
        }

        String sql = "SELECT ai.id, ai.entity_type, ai.entity_id, ai.user_id, ai.original_name, ai.file_path, ai.mime_type, ai.created_at, " +
                "CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')) AS uploader_name, u.username, u.email " +
                "FROM attachment_item ai " +
                "LEFT JOIN `user` u ON u.id = ai.user_id " +
                "WHERE ai.entity_type = ? AND ai.entity_id = ? ORDER BY ai.id DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, safe(entityType));
            statement.setInt(2, entityId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                AttachmentItem item = new AttachmentItem();
                item.setId(rs.getInt("id"));
                item.setEntityType(rs.getString("entity_type"));
                item.setEntityId(rs.getInt("entity_id"));
                item.setUserId(rs.getInt("user_id"));
                item.setOriginalName(rs.getString("original_name"));
                item.setFilePath(rs.getString("file_path"));
                item.setMimeType(rs.getString("mime_type"));
                item.setCreatedAt(rs.getString("created_at"));
                String uploader = safe(rs.getString("uploader_name")).trim();
                if (uploader.isEmpty()) {
                    uploader = safe(rs.getString("username"));
                }
                if (uploader.isEmpty()) {
                    uploader = safe(rs.getString("email"));
                }
                item.setUploadedByName(uploader.isEmpty() ? "Unknown user" : uploader);
                items.add(item);
            }
        } catch (SQLException e) {
            System.out.println("AttachmentService.getAttachments: " + e.getMessage());
        }
        return items;
    }

    private boolean isDatabaseAvailable() {
        return MyDataBase.getInstance().isConnected() && connection != null;
    }

    private void ensureAttachmentTable() {
        if (!isDatabaseAvailable()) {
            return;
        }
        String sql = "CREATE TABLE IF NOT EXISTS attachment_item (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "entity_type VARCHAR(32) NOT NULL, " +
                "entity_id INT NOT NULL, " +
                "user_id INT NOT NULL, " +
                "original_name VARCHAR(255) NOT NULL, " +
                "file_path VARCHAR(1000) NOT NULL, " +
                "mime_type VARCHAR(255) NULL, " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "INDEX idx_attachment_entity (entity_type, entity_id), " +
                "INDEX idx_attachment_user (user_id), " +
                "CONSTRAINT fk_attachment_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE" +
                ")";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("AttachmentService.ensureAttachmentTable: " + e.getMessage());
        }
    }

    private String detectMimeType(Path sourceFile) throws IOException {
        String mimeType = Files.probeContentType(sourceFile);
        return mimeType == null ? "application/octet-stream" : mimeType;
    }

    private String sanitizeFileName(String value) {
        return safe(value).replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
