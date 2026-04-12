package com.studyflow.services;

import com.studyflow.interfaces.IService;
import com.studyflow.models.Project;
import com.studyflow.utils.MyDataBase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProjectService implements IService<Project> {
    private final Connection connection;

    public ProjectService() {
        this.connection = MyDataBase.getInstance().getConnection();
    }

    public boolean isDatabaseAvailable() {
        return MyDataBase.getInstance().isConnected() && connection != null;
    }

    private boolean ensureConnection(String operation) {
        if (isDatabaseAvailable()) {
            return true;
        }
        System.out.println(operation + ": database connection unavailable.");
        return false;
    }

    @Override
    public void add(Project project) {
        if (!ensureConnection("ProjectService.add")) {
            return;
        }
        String sql = "INSERT INTO project (user_id, titre, description, date_debut, date_fin, statut, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setInt(1, project.getUserId());
            statement.setString(2, project.getTitle());
            statement.setString(3, project.getDescription());
            statement.setDate(4, Date.valueOf(project.getStartDate()));
            statement.setDate(5, Date.valueOf(project.getEndDate()));
            statement.setString(6, project.getStatus());
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                project.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            System.out.println("ProjectService.add: " + e.getMessage());
        }
    }

    @Override
    public void update(Project project) {
        if (!ensureConnection("ProjectService.update")) {
            return;
        }
        String sql = "UPDATE project SET titre = ?, description = ?, date_debut = ?, date_fin = ?, statut = ?, updated_at = NOW() WHERE id = ? AND user_id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, project.getTitle());
            statement.setString(2, project.getDescription());
            statement.setDate(3, Date.valueOf(project.getStartDate()));
            statement.setDate(4, Date.valueOf(project.getEndDate()));
            statement.setString(5, project.getStatus());
            statement.setInt(6, project.getId());
            statement.setInt(7, project.getUserId());
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ProjectService.update: " + e.getMessage());
        }
    }

    @Override
    public void delete(Project project) {
        if (!ensureConnection("ProjectService.delete")) {
            return;
        }
        String sql = "DELETE FROM project WHERE id = ? AND user_id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, project.getId());
            statement.setInt(2, project.getUserId());
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ProjectService.delete: " + e.getMessage());
        }
    }

    @Override
    public List<Project> getAll() {
        return new ArrayList<>();
    }

    public List<Project> getByUserId(int userId) {
        List<Project> projects = new ArrayList<>();
        if (!ensureConnection("ProjectService.getByUserId")) {
            return projects;
        }

        String sql = "SELECT p.*, COUNT(a.id) AS assignment_count " +
                "FROM project p " +
                "LEFT JOIN assignment a ON a.project_id = p.id " +
                "WHERE p.user_id = ? " +
                "GROUP BY p.id " +
                "ORDER BY p.updated_at DESC, p.created_at DESC";

        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                projects.add(mapProject(rs));
            }
        } catch (SQLException e) {
            System.out.println("ProjectService.getByUserId: " + e.getMessage());
        }
        return projects;
    }

    private Project mapProject(ResultSet rs) throws SQLException {
        Project project = new Project();
        project.setId(rs.getInt("id"));
        project.setUserId(rs.getInt("user_id"));
        project.setTitle(rs.getString("titre"));
        project.setDescription(rs.getString("description"));
        project.setStartDate(readDate(rs, "date_debut"));
        project.setEndDate(readDate(rs, "date_fin"));
        project.setStatus(normalizeStatus(rs.getString("statut")));
        project.setAssignmentCount(readInt(rs, "assignment_count"));
        project.setCreatedAt(readDateTime(rs, "created_at"));
        project.setUpdatedAt(readDateTime(rs, "updated_at"));
        return project;
    }

    private LocalDate readDate(ResultSet rs, String column) throws SQLException {
        Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private LocalDateTime readDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private int readInt(ResultSet rs, String column) {
        try {
            return rs.getInt(column);
        } catch (SQLException e) {
            return 0;
        }
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return "Planned";
        }
        String normalized = status.trim().toLowerCase();
        if (normalized.contains("cours")) {
            return "In Progress";
        }
        if (normalized.contains("pause")) {
            return "On Hold";
        }
        if (normalized.contains("attente") || normalized.contains("plan")) {
            return "Planned";
        }
        if (normalized.contains("termin") || normalized.contains("complete")) {
            return "Completed";
        }
        return status;
    }
}
