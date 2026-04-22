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
        ensureGitSupport();
        String sql = "INSERT INTO project (user_id, titre, description, date_debut, date_fin, statut, git_repo_path, git_remote_url, git_default_branch, git_username, git_access_token, git_last_status_summary, git_last_sync_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setInt(1, project.getUserId());
            statement.setString(2, project.getTitle());
            statement.setString(3, project.getDescription());
            statement.setDate(4, Date.valueOf(project.getStartDate()));
            statement.setDate(5, Date.valueOf(project.getEndDate()));
            statement.setString(6, project.getStatus());
            setNullableString(statement, 7, project.getGitRepoPath());
            setNullableString(statement, 8, project.getGitRemoteUrl());
            setNullableString(statement, 9, project.getGitDefaultBranch());
            setNullableString(statement, 10, project.getGitUsername());
            setNullableString(statement, 11, project.getGitAccessToken());
            setNullableString(statement, 12, project.getGitLastStatusSummary());
            setNullableDateTime(statement, 13, project.getGitLastSyncAt());
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
        ensureGitSupport();
        String sql = "UPDATE project SET titre = ?, description = ?, date_debut = ?, date_fin = ?, statut = ?, git_repo_path = ?, git_remote_url = ?, git_default_branch = ?, git_username = ?, git_access_token = ?, git_last_status_summary = ?, git_last_sync_at = ?, updated_at = NOW() WHERE id = ? AND user_id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, project.getTitle());
            statement.setString(2, project.getDescription());
            statement.setDate(3, Date.valueOf(project.getStartDate()));
            statement.setDate(4, Date.valueOf(project.getEndDate()));
            statement.setString(5, project.getStatus());
            setNullableString(statement, 6, project.getGitRepoPath());
            setNullableString(statement, 7, project.getGitRemoteUrl());
            setNullableString(statement, 8, project.getGitDefaultBranch());
            setNullableString(statement, 9, project.getGitUsername());
            setNullableString(statement, 10, project.getGitAccessToken());
            setNullableString(statement, 11, project.getGitLastStatusSummary());
            setNullableDateTime(statement, 12, project.getGitLastSyncAt());
            statement.setInt(13, project.getId());
            statement.setInt(14, project.getUserId());
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
        ensureGitSupport();

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
        ensureGitSupport();

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
        ensureGitSupport();
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
        project.setGitRepoPath(readString(rs, "git_repo_path"));
        project.setGitRemoteUrl(readString(rs, "git_remote_url"));
        project.setGitDefaultBranch(readString(rs, "git_default_branch"));
        project.setGitUsername(readString(rs, "git_username"));
        project.setGitAccessToken(readString(rs, "git_access_token"));
        project.setGitLastStatusSummary(readString(rs, "git_last_status_summary"));
        project.setCreatedAt(readDateTime(rs, "created_at"));
        project.setUpdatedAt(readDateTime(rs, "updated_at"));
        project.setGitLastSyncAt(readDateTime(rs, "git_last_sync_at"));
        return project;
    }

    public void updateGitSettings(Project project) {
        if (!ensureConnection("ProjectService.updateGitSettings") || project == null || project.getId() <= 0) {
            return;
        }
        ensureGitSupport();
        String sql = "UPDATE project SET git_repo_path = ?, git_remote_url = ?, git_default_branch = ?, git_username = ?, git_access_token = ?, git_last_status_summary = ?, git_last_sync_at = ?, updated_at = NOW() WHERE id = ? AND user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableString(statement, 1, project.getGitRepoPath());
            setNullableString(statement, 2, project.getGitRemoteUrl());
            setNullableString(statement, 3, project.getGitDefaultBranch());
            setNullableString(statement, 4, project.getGitUsername());
            setNullableString(statement, 5, project.getGitAccessToken());
            setNullableString(statement, 6, project.getGitLastStatusSummary());
            setNullableDateTime(statement, 7, project.getGitLastSyncAt());
            statement.setInt(8, project.getId());
            statement.setInt(9, project.getUserId());
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ProjectService.updateGitSettings: " + e.getMessage());
        }
    }

    private LocalDate readDate(ResultSet rs, String column) throws SQLException {
        Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private LocalDateTime readDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private String readString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            return null;
        }
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

    private void ensureGitSupport() {
        if (!ensureConnection("ProjectService.ensureGitSupport")) {
            return;
        }
        ensureColumnExists("project", "git_repo_path", "ALTER TABLE project ADD COLUMN git_repo_path VARCHAR(600) NULL");
        ensureColumnExists("project", "git_remote_url", "ALTER TABLE project ADD COLUMN git_remote_url VARCHAR(600) NULL");
        ensureColumnExists("project", "git_default_branch", "ALTER TABLE project ADD COLUMN git_default_branch VARCHAR(120) NULL");
        ensureColumnExists("project", "git_username", "ALTER TABLE project ADD COLUMN git_username VARCHAR(255) NULL");
        ensureColumnExists("project", "git_access_token", "ALTER TABLE project ADD COLUMN git_access_token VARCHAR(600) NULL");
        ensureColumnExists("project", "git_last_status_summary", "ALTER TABLE project ADD COLUMN git_last_status_summary VARCHAR(600) NULL");
        ensureColumnExists("project", "git_last_sync_at", "ALTER TABLE project ADD COLUMN git_last_sync_at DATETIME NULL");
    }

    private void ensureColumnExists(String table, String column, String alterSql) {
        String checkSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(checkSql)) {
            statement.setString(1, table);
            statement.setString(2, column);
            ResultSet rs = statement.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                try (Statement alterStatement = connection.createStatement()) {
                    alterStatement.executeUpdate(alterSql);
                }
            }
        } catch (SQLException e) {
            System.out.println("ProjectService.ensureColumnExists(" + table + "." + column + "): " + e.getMessage());
        }
    }

    private void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, java.sql.Types.VARCHAR);
            return;
        }
        statement.setString(index, value.trim());
    }

    private void setNullableDateTime(PreparedStatement statement, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.TIMESTAMP);
            return;
        }
        statement.setTimestamp(index, Timestamp.valueOf(value));
    }
}
