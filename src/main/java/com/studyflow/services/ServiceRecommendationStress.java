package com.studyflow.services;

import com.studyflow.models.RecommendationStress;
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

public class ServiceRecommendationStress {

    private Connection cnx;

    public ServiceRecommendationStress() {
        this.cnx = MyDataBase.getInstance().getConnection();
        if (this.cnx != null) {
            try {
                ensureTable();
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void ensureTable() {
        ensureConnectionAvailable();
        String sql = """
                CREATE TABLE IF NOT EXISTS recommendation_stress (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    content TEXT NOT NULL,
                    level VARCHAR(30) NOT NULL,
                    is_active TINYINT(1) NOT NULL DEFAULT 1,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NULL DEFAULT NULL
                )
                """;
        try (Statement stm = cnx.createStatement()) {
            stm.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to prepare recommendation_stress table", e);
        }
    }

    public List<RecommendationStress> findAll() {
        ensureConnectionAvailable();
        String sql = "SELECT id, title, content, level, is_active, created_at, updated_at FROM recommendation_stress ORDER BY id ASC";
        try (PreparedStatement pstm = cnx.prepareStatement(sql)) {
            ResultSet rs = pstm.executeQuery();
            List<RecommendationStress> items = new ArrayList<>();
            while (rs.next()) {
                items.add(map(rs));
            }
            return items;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch recommendations", e);
        }
    }

    public RecommendationStress findById(int id) {
        ensureConnectionAvailable();
        String sql = "SELECT id, title, content, level, is_active, created_at, updated_at FROM recommendation_stress WHERE id = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(sql)) {
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();
            return rs.next() ? map(rs) : null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch recommendation", e);
        }
    }

    public List<RecommendationStress> findByLevel(String level) {
        ensureConnectionAvailable();
        String sql = """
                SELECT id, title, content, level, is_active, created_at, updated_at
                FROM recommendation_stress
                WHERE LOWER(level) = LOWER(?) AND is_active = 1
                ORDER BY id ASC
                """;
        try (PreparedStatement pstm = cnx.prepareStatement(sql)) {
            pstm.setString(1, level == null ? "" : level);
            ResultSet rs = pstm.executeQuery();
            List<RecommendationStress> items = new ArrayList<>();
            while (rs.next()) {
                items.add(map(rs));
            }
            return items;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch recommendations by level", e);
        }
    }

    public void add(RecommendationStress item) {
        ensureConnectionAvailable();
        String sql = "INSERT INTO recommendation_stress (title, content, level, is_active, created_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setString(1, item.getTitle());
            pstm.setString(2, item.getContent());
            pstm.setString(3, item.getLevel());
            pstm.setBoolean(4, item.isActive());
            pstm.setTimestamp(5, Timestamp.valueOf(item.getCreatedAt() != null ? item.getCreatedAt() : LocalDateTime.now()));
            pstm.executeUpdate();
            ResultSet rs = pstm.getGeneratedKeys();
            if (rs.next()) {
                item.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create recommendation", e);
        }
    }

    public void update(RecommendationStress item) {
        ensureConnectionAvailable();
        String sql = "UPDATE recommendation_stress SET title = ?, content = ?, level = ?, is_active = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(sql)) {
            pstm.setString(1, item.getTitle());
            pstm.setString(2, item.getContent());
            pstm.setString(3, item.getLevel());
            pstm.setBoolean(4, item.isActive());
            pstm.setTimestamp(5, Timestamp.valueOf(item.getUpdatedAt() != null ? item.getUpdatedAt() : LocalDateTime.now()));
            pstm.setInt(6, item.getId());
            pstm.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update recommendation", e);
        }
    }

    public void delete(int id) {
        ensureConnectionAvailable();
        try (PreparedStatement pstm = cnx.prepareStatement("DELETE FROM recommendation_stress WHERE id = ?")) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete recommendation", e);
        }
    }

    private RecommendationStress map(ResultSet rs) throws SQLException {
        RecommendationStress item = new RecommendationStress();
        item.setId(rs.getInt("id"));
        item.setTitle(rs.getString("title"));
        item.setContent(rs.getString("content"));
        item.setLevel(rs.getString("level"));
        item.setActive(rs.getBoolean("is_active"));
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        item.setCreatedAt(created != null ? created.toLocalDateTime() : null);
        item.setUpdatedAt(updated != null ? updated.toLocalDateTime() : null);
        return item;
    }

    private void ensureConnectionAvailable() {
        cnx = MyDataBase.getInstance().getConnection();
        if (cnx == null) {
            throw new RuntimeException("Database connection is unavailable.");
        }
    }
}
