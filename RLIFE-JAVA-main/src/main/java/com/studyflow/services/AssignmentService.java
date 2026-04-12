package com.studyflow.services;

import com.studyflow.interfaces.IService;
import com.studyflow.models.Assignment;
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

public class AssignmentService implements IService<Assignment> {
    private final Connection connection;

    public AssignmentService() {
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
    public void add(Assignment assignment) {
        if (!ensureConnection("AssignmentService.add")) {
            return;
        }
        String sql = "INSERT INTO assignment (user_id, project_id, titre, description, date_debut, date_fin, priorite, statut, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setInt(1, assignment.getUserId());
            statement.setInt(2, assignment.getProjectId());
            statement.setString(3, assignment.getTitle());
            statement.setString(4, assignment.getDescription());
            statement.setDate(5, Date.valueOf(assignment.getStartDate()));
            statement.setDate(6, Date.valueOf(assignment.getEndDate()));
            statement.setString(7, assignment.getPriority());
            statement.setString(8, assignment.getStatus());
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                assignment.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            System.out.println("AssignmentService.add: " + e.getMessage());
        }
    }

    @Override
    public void update(Assignment assignment) {
        if (!ensureConnection("AssignmentService.update")) {
            return;
        }
        String sql = "UPDATE assignment SET project_id = ?, titre = ?, description = ?, date_debut = ?, date_fin = ?, priorite = ?, statut = ?, updated_at = NOW() WHERE id = ? AND user_id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, assignment.getProjectId());
            statement.setString(2, assignment.getTitle());
            statement.setString(3, assignment.getDescription());
            statement.setDate(4, Date.valueOf(assignment.getStartDate()));
            statement.setDate(5, Date.valueOf(assignment.getEndDate()));
            statement.setString(6, assignment.getPriority());
            statement.setString(7, assignment.getStatus());
            statement.setInt(8, assignment.getId());
            statement.setInt(9, assignment.getUserId());
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("AssignmentService.update: " + e.getMessage());
        }
    }

    @Override
    public void delete(Assignment assignment) {
        if (!ensureConnection("AssignmentService.delete")) {
            return;
        }
        String sql = "DELETE FROM assignment WHERE id = ? AND user_id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, assignment.getId());
            statement.setInt(2, assignment.getUserId());
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("AssignmentService.delete: " + e.getMessage());
        }
    }

    @Override
    public List<Assignment> getAll() {
        return new ArrayList<>();
    }

    public List<Assignment> getByUserId(int userId) {
        List<Assignment> assignments = new ArrayList<>();
        if (!ensureConnection("AssignmentService.getByUserId")) {
            return assignments;
        }

        String sql = "SELECT a.*, p.titre AS project_title FROM assignment a " +
                "JOIN project p ON p.id = a.project_id " +
                "WHERE a.user_id = ? " +
                "ORDER BY a.updated_at DESC, a.created_at DESC";

        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                assignments.add(mapAssignment(rs));
            }
        } catch (SQLException e) {
            System.out.println("AssignmentService.getByUserId: " + e.getMessage());
        }
        return assignments;
    }

    public List<Assignment> getByProjectId(int projectId, int userId) {
        List<Assignment> assignments = new ArrayList<>();
        if (!ensureConnection("AssignmentService.getByProjectId")) {
            return assignments;
        }

        String sql = "SELECT a.*, p.titre AS project_title FROM assignment a " +
                "JOIN project p ON p.id = a.project_id " +
                "WHERE a.project_id = ? AND a.user_id = ? " +
                "ORDER BY a.updated_at DESC, a.created_at DESC";

        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, projectId);
            statement.setInt(2, userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                assignments.add(mapAssignment(rs));
            }
        } catch (SQLException e) {
            System.out.println("AssignmentService.getByProjectId: " + e.getMessage());
        }
        return assignments;
    }

    private Assignment mapAssignment(ResultSet rs) throws SQLException {
        Assignment assignment = new Assignment();
        assignment.setId(rs.getInt("id"));
        assignment.setUserId(rs.getInt("user_id"));
        assignment.setProjectId(rs.getInt("project_id"));
        assignment.setProjectTitle(rs.getString("project_title"));
        assignment.setTitle(rs.getString("titre"));
        assignment.setDescription(rs.getString("description"));
        assignment.setStartDate(readDate(rs, "date_debut"));
        assignment.setEndDate(readDate(rs, "date_fin"));
        assignment.setPriority(normalizePriority(rs.getString("priorite")));
        assignment.setStatus(normalizeStatus(rs.getString("statut")));
        assignment.setCreatedAt(readDateTime(rs, "created_at"));
        assignment.setUpdatedAt(readDateTime(rs, "updated_at"));
        return assignment;
    }

    private LocalDate readDate(ResultSet rs, String column) throws SQLException {
        Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private LocalDateTime readDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private String normalizePriority(String priority) {
        if (priority == null) {
            return "Medium";
        }
        String normalized = priority.trim().toLowerCase();
        if (normalized.contains("haut") || normalized.equals("high")) {
            return "High";
        }
        if (normalized.contains("moy") || normalized.equals("medium")) {
            return "Medium";
        }
        if (normalized.contains("bas") || normalized.equals("low")) {
            return "Low";
        }
        return priority;
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return "To Do";
        }
        String normalized = status.trim().toLowerCase();
        if (normalized.contains("cours")) {
            return "In Progress";
        }
        if (normalized.contains("faire") || normalized.contains("todo") || normalized.contains("to do")) {
            return "To Do";
        }
        if (normalized.contains("termin") || normalized.contains("complete")) {
            return "Completed";
        }
        return status;
    }
}
