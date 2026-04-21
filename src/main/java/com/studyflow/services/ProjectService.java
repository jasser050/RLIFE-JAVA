package com.studyflow.services;

import com.studyflow.interfaces.IService;
import com.studyflow.models.Project;
import com.studyflow.models.User;
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
    private final ServiceUser userService;
    private final NotificationService notificationService;

    public ProjectService() {
        this.connection = MyDataBase.getInstance().getConnection();
        this.userService = new ServiceUser();
        this.notificationService = new NotificationService();
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
        List<Project> projects = new ArrayList<>();
        if (!ensureConnection("ProjectService.getAll")) {
            return projects;
        }

        String sql = "SELECT p.*, u.first_name, u.last_name, 1 AS owned_by_current_user, NULL AS shared_role, " +
                "(SELECT COUNT(*) FROM assignment a WHERE a.project_id = p.id) AS assignment_count " +
                "FROM project p " +
                "JOIN user u ON u.id = p.user_id " +
                "ORDER BY COALESCE(p.updated_at, p.created_at) DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                projects.add(mapProject(rs, rs.getInt("user_id")));
            }
        } catch (SQLException e) {
            System.out.println("ProjectService.getAll: " + e.getMessage());
        }
        return projects;
    }

    public int countProjectShares() {
        if (!ensureConnection("ProjectService.countProjectShares")) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM project_share";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("ProjectService.countProjectShares: " + e.getMessage());
        }
        return 0;
    }

    public List<Project> getByUserId(int userId) {
        List<Project> projects = new ArrayList<>();
        if (!ensureConnection("ProjectService.getByUserId")) {
            return projects;
        }

        String sql = "SELECT p.*, " +
                "u.first_name, u.last_name, " +
                "CASE WHEN p.user_id = ? THEN 1 ELSE 0 END AS owned_by_current_user, " +
                "ps.role AS shared_role, " +
                "COUNT(DISTINCT CASE " +
                "   WHEN p.user_id = ? THEN a.id " +
                "   WHEN ac.user_id = ? THEN a.id " +
                "END) AS assignment_count " +
                "FROM project p " +
                "JOIN user u ON u.id = p.user_id " +
                "LEFT JOIN project_share ps ON ps.project_id = p.id AND ps.shared_with_user_id = ? " +
                "LEFT JOIN assignment a ON a.project_id = p.id " +
                "LEFT JOIN assignment_collaborator ac ON ac.assignment_id = a.id AND ac.user_id = ? " +
                "WHERE p.user_id = ? OR ps.shared_with_user_id = ? " +
                "GROUP BY p.id, u.first_name, u.last_name, ps.role " +
                "ORDER BY COALESCE(p.updated_at, p.created_at) DESC";

        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, userId);
            statement.setInt(2, userId);
            statement.setInt(3, userId);
            statement.setInt(4, userId);
            statement.setInt(5, userId);
            statement.setInt(6, userId);
            statement.setInt(7, userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                projects.add(mapProject(rs, userId));
            }
        } catch (SQLException e) {
            System.out.println("ProjectService.getByUserId: " + e.getMessage());
        }
        return projects;
    }

    public boolean shareProjectWithUser(int projectId, int ownerUserId, String recipientIdentifier, String role) {
        if (!ensureConnection("ProjectService.shareProjectWithUser")) {
            return false;
        }

        User recipient = userService.findByIdentifier(recipientIdentifier);
        if (recipient == null || recipient.getId() <= 0 || recipient.getId() == ownerUserId) {
            return false;
        }
        if (!isOwnedByUser(projectId, ownerUserId) || isProjectAlreadyShared(projectId, recipient.getId())) {
            return false;
        }

        String sql = "INSERT INTO project_share (project_id, shared_with_user_id, shared_by_user_id, role, created_at) VALUES (?, ?, ?, ?, NOW())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projectId);
            statement.setInt(2, recipient.getId());
            statement.setInt(3, ownerUserId);
            statement.setString(4, normalizeRole(role));
            if (statement.executeUpdate() > 0) {
                Project project = getProjectById(projectId);
                if (project != null) {
                    notificationService.addNotificationForUser(
                            recipient.getId(),
                            "Project shared with you",
                            "You received access to project \"" + safe(project.getTitle()) + "\".",
                            "project_share",
                            "project:" + projectId
                    );
                }
                return true;
            }
        } catch (SQLException e) {
            System.out.println("ProjectService.shareProjectWithUser: " + e.getMessage());
        }
        return false;
    }

    public boolean isOwnedByUser(int projectId, int userId) {
        if (!ensureConnection("ProjectService.isOwnedByUser")) {
            return false;
        }
        String sql = "SELECT id FROM project WHERE id = ? AND user_id = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projectId);
            statement.setInt(2, userId);
            ResultSet rs = statement.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("ProjectService.isOwnedByUser: " + e.getMessage());
        }
        return false;
    }

    public boolean userHasProjectAccess(int projectId, int userId) {
        if (!ensureConnection("ProjectService.userHasProjectAccess")) {
            return false;
        }
        String sql = "SELECT p.id " +
                "FROM project p " +
                "LEFT JOIN project_share ps ON ps.project_id = p.id AND ps.shared_with_user_id = ? " +
                "WHERE p.id = ? AND (p.user_id = ? OR ps.shared_with_user_id = ?) " +
                "LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, projectId);
            statement.setInt(3, userId);
            statement.setInt(4, userId);
            ResultSet rs = statement.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("ProjectService.userHasProjectAccess: " + e.getMessage());
        }
        return false;
    }

    public Project getProjectById(int projectId) {
        if (!ensureConnection("ProjectService.getProjectById")) {
            return null;
        }
        String sql = "SELECT p.*, u.first_name, u.last_name, 1 AS owned_by_current_user, NULL AS shared_role, " +
                "(SELECT COUNT(*) FROM assignment a WHERE a.project_id = p.id) AS assignment_count " +
                "FROM project p JOIN user u ON u.id = p.user_id WHERE p.id = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projectId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return mapProject(rs, rs.getInt("user_id"));
            }
        } catch (SQLException e) {
            System.out.println("ProjectService.getProjectById: " + e.getMessage());
        }
        return null;
    }

    private boolean isProjectAlreadyShared(int projectId, int recipientUserId) {
        String sql = "SELECT id FROM project_share WHERE project_id = ? AND shared_with_user_id = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projectId);
            statement.setInt(2, recipientUserId);
            ResultSet rs = statement.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("ProjectService.isProjectAlreadyShared: " + e.getMessage());
        }
        return false;
    }

    private Project mapProject(ResultSet rs, int currentUserId) throws SQLException {
        Project project = new Project();
        project.setId(rs.getInt("id"));
        project.setUserId(rs.getInt("user_id"));
        project.setOwnerUserId(rs.getInt("user_id"));
        project.setTitle(rs.getString("titre"));
        project.setDescription(rs.getString("description"));
        project.setStartDate(readDate(rs, "date_debut"));
        project.setEndDate(readDate(rs, "date_fin"));
        project.setStatus(normalizeStatus(rs.getString("statut")));
        project.setAssignmentCount(readInt(rs, "assignment_count"));
        project.setOwnedByCurrentUser(readInt(rs, "owned_by_current_user") == 1 || rs.getInt("user_id") == currentUserId);
        project.setSharedRole(rs.getString("shared_role"));
        project.setOwnerName(buildName(rs.getString("first_name"), rs.getString("last_name")));
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

    private String normalizeRole(String role) {
        return "editor".equalsIgnoreCase(role) ? "editor" : "viewer";
    }

    private String buildName(String firstName, String lastName) {
        String fullName = (firstName == null ? "" : firstName.trim()) + " " + (lastName == null ? "" : lastName.trim());
        return fullName.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
