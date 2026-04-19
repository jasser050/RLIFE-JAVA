package com.studyflow.services;

import com.studyflow.interfaces.IService;
import com.studyflow.models.Assignment;
import com.studyflow.models.AssignmentComment;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AssignmentService implements IService<Assignment> {
    private final Connection connection;
    private final ServiceUser userService;
    private final NotificationService notificationService;
    private final ProjectService projectService;

    public AssignmentService() {
        this.connection = MyDataBase.getInstance().getConnection();
        this.userService = new ServiceUser();
        this.notificationService = new NotificationService();
        this.projectService = new ProjectService();
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
        ensureAssignmentDateTimeSupport();
        normalizeAssignmentDateTime(assignment);
        String sql = "INSERT INTO assignment (user_id, project_id, titre, description, date_debut, date_fin, priorite, statut, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setInt(1, assignment.getUserId());
            statement.setInt(2, assignment.getProjectId());
            statement.setString(3, assignment.getTitle());
            statement.setString(4, assignment.getDescription());
            statement.setTimestamp(5, Timestamp.valueOf(toDateTime(assignment.getStartDate(), assignment.getStartTime())));
            statement.setTimestamp(6, Timestamp.valueOf(toDateTime(assignment.getEndDate(), assignment.getEndTime())));
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
        ensureAssignmentDateTimeSupport();
        applyStatusDueTimestampIfNeeded(assignment);
        normalizeAssignmentDateTime(assignment);
        String sql = "UPDATE assignment SET project_id = ?, titre = ?, description = ?, date_debut = ?, date_fin = ?, priorite = ?, statut = ?, updated_at = NOW() WHERE id = ? AND user_id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, assignment.getProjectId());
            statement.setString(2, assignment.getTitle());
            statement.setString(3, assignment.getDescription());
            statement.setTimestamp(4, Timestamp.valueOf(toDateTime(assignment.getStartDate(), assignment.getStartTime())));
            statement.setTimestamp(5, Timestamp.valueOf(toDateTime(assignment.getEndDate(), assignment.getEndTime())));
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
        List<Assignment> assignments = new ArrayList<>();
        if (!ensureConnection("AssignmentService.getAll")) {
            return assignments;
        }

        String sql = "SELECT a.*, p.titre AS project_title, u.first_name, u.last_name, 1 AS owned_by_current_user " +
                "FROM assignment a " +
                "JOIN project p ON p.id = a.project_id " +
                "JOIN user u ON u.id = a.user_id " +
                "ORDER BY COALESCE(a.updated_at, a.created_at) DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                assignments.add(mapAssignment(rs, rs.getInt("user_id")));
            }
        } catch (SQLException e) {
            System.out.println("AssignmentService.getAll: " + e.getMessage());
        }
        return assignments;
    }

    public List<Assignment> getByUserId(int userId) {
        List<Assignment> assignments = new ArrayList<>();
        if (!ensureConnection("AssignmentService.getByUserId")) {
            return assignments;
        }

        String sql = "SELECT a.*, p.titre AS project_title, u.first_name, u.last_name, " +
                "CASE WHEN a.user_id = ? THEN 1 ELSE 0 END AS owned_by_current_user " +
                "FROM assignment a " +
                "JOIN project p ON p.id = a.project_id " +
                "JOIN user u ON u.id = a.user_id " +
                "LEFT JOIN assignment_collaborator ac ON ac.assignment_id = a.id AND ac.user_id = ? " +
                "WHERE a.user_id = ? OR ac.user_id = ? " +
                "ORDER BY COALESCE(a.updated_at, a.created_at) DESC";

        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, userId);
            statement.setInt(2, userId);
            statement.setInt(3, userId);
            statement.setInt(4, userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                assignments.add(mapAssignment(rs, userId));
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

        String sql = "SELECT a.*, p.titre AS project_title, u.first_name, u.last_name, " +
                "CASE WHEN a.user_id = ? THEN 1 ELSE 0 END AS owned_by_current_user " +
                "FROM assignment a " +
                "JOIN project p ON p.id = a.project_id " +
                "JOIN user u ON u.id = a.user_id " +
                "LEFT JOIN assignment_collaborator ac ON ac.assignment_id = a.id AND ac.user_id = ? " +
                "WHERE a.project_id = ? AND (a.user_id = ? OR ac.user_id = ?) " +
                "ORDER BY COALESCE(a.updated_at, a.created_at) DESC";

        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, userId);
            statement.setInt(2, userId);
            statement.setInt(3, projectId);
            statement.setInt(4, userId);
            statement.setInt(5, userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                assignments.add(mapAssignment(rs, userId));
            }
        } catch (SQLException e) {
            System.out.println("AssignmentService.getByProjectId: " + e.getMessage());
        }
        return assignments;
    }

    public boolean shareAssignmentWithUser(int assignmentId, int ownerUserId, String recipientIdentifier) {
        if (!ensureConnection("AssignmentService.shareAssignmentWithUser")) {
            return false;
        }

        Assignment assignment = getOwnedAssignmentById(assignmentId, ownerUserId);
        if (assignment == null) {
            return false;
        }

        User recipient = userService.findByIdentifier(recipientIdentifier);
        if (recipient == null || recipient.getId() <= 0 || recipient.getId() == ownerUserId) {
            return false;
        }
        if (!projectService.userHasProjectAccess(assignment.getProjectId(), recipient.getId()) || isAssignmentAlreadyShared(assignmentId, recipient.getId())) {
            return false;
        }

        String sql = "INSERT INTO assignment_collaborator (assignment_id, user_id, assigned_by_user_id, created_at) VALUES (?, ?, ?, NOW())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, assignmentId);
            statement.setInt(2, recipient.getId());
            statement.setInt(3, ownerUserId);
            if (statement.executeUpdate() > 0) {
                notificationService.addNotificationForUser(
                        recipient.getId(),
                        "Assignment shared with you",
                        "You received assignment \"" + safe(assignment.getTitle()) + "\" in project \"" + safe(assignment.getProjectTitle()) + "\".",
                        "assignment_share",
                        "assignment:" + assignmentId
                );
                return true;
            }
        } catch (SQLException e) {
            System.out.println("AssignmentService.shareAssignmentWithUser: " + e.getMessage());
        }
        return false;
    }

    public boolean addComment(int assignmentId, int userId, String content) {
        if (!ensureConnection("AssignmentService.addComment") || content == null || content.trim().isEmpty()) {
            return false;
        }
        if (!userHasAssignmentAccess(assignmentId, userId)) {
            return false;
        }

        String sql = "INSERT INTO comment (assignment_id, user_id, content, created_at) VALUES (?, ?, ?, NOW())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, assignmentId);
            statement.setInt(2, userId);
            statement.setString(3, content.trim());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("AssignmentService.addComment: " + e.getMessage());
        }
        return false;
    }

    public List<AssignmentComment> getCommentsByAssignmentId(int assignmentId, int userId) {
        List<AssignmentComment> comments = new ArrayList<>();
        if (!ensureConnection("AssignmentService.getCommentsByAssignmentId") || !userHasAssignmentAccess(assignmentId, userId)) {
            return comments;
        }

        String sql = "SELECT c.*, u.first_name, u.last_name " +
                "FROM comment c " +
                "JOIN user u ON u.id = c.user_id " +
                "WHERE c.assignment_id = ? " +
                "ORDER BY c.created_at ASC";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, assignmentId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                AssignmentComment comment = new AssignmentComment();
                comment.setId(rs.getInt("id"));
                comment.setAssignmentId(rs.getInt("assignment_id"));
                comment.setUserId(rs.getInt("user_id"));
                comment.setContent(rs.getString("content"));
                comment.setAuthorName(buildName(rs.getString("first_name"), rs.getString("last_name")));
                comment.setCreatedAt(readDateTime(rs, "created_at"));
                comments.add(comment);
            }
        } catch (SQLException e) {
            System.out.println("AssignmentService.getCommentsByAssignmentId: " + e.getMessage());
        }
        return comments;
    }

    public Assignment getOwnedAssignmentById(int assignmentId, int ownerUserId) {
        if (!ensureConnection("AssignmentService.getOwnedAssignmentById")) {
            return null;
        }
        String sql = "SELECT a.*, p.titre AS project_title, u.first_name, u.last_name, 1 AS owned_by_current_user " +
                "FROM assignment a " +
                "JOIN project p ON p.id = a.project_id " +
                "JOIN user u ON u.id = a.user_id " +
                "WHERE a.id = ? AND a.user_id = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, assignmentId);
            statement.setInt(2, ownerUserId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return mapAssignment(rs, ownerUserId);
            }
        } catch (SQLException e) {
            System.out.println("AssignmentService.getOwnedAssignmentById: " + e.getMessage());
        }
        return null;
    }

    public boolean userHasAssignmentAccess(int assignmentId, int userId) {
        if (!ensureConnection("AssignmentService.userHasAssignmentAccess")) {
            return false;
        }
        String sql = "SELECT a.id " +
                "FROM assignment a " +
                "LEFT JOIN assignment_collaborator ac ON ac.assignment_id = a.id AND ac.user_id = ? " +
                "WHERE a.id = ? AND (a.user_id = ? OR ac.user_id = ?) " +
                "LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, assignmentId);
            statement.setInt(3, userId);
            statement.setInt(4, userId);
            ResultSet rs = statement.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("AssignmentService.userHasAssignmentAccess: " + e.getMessage());
        }
        return false;
    }

    private boolean isAssignmentAlreadyShared(int assignmentId, int recipientUserId) {
        String sql = "SELECT id FROM assignment_collaborator WHERE assignment_id = ? AND user_id = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, assignmentId);
            statement.setInt(2, recipientUserId);
            ResultSet rs = statement.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("AssignmentService.isAssignmentAlreadyShared: " + e.getMessage());
        }
        return false;
    }

    private Assignment mapAssignment(ResultSet rs, int currentUserId) throws SQLException {
        Assignment assignment = new Assignment();
        assignment.setId(rs.getInt("id"));
        assignment.setUserId(rs.getInt("user_id"));
        assignment.setOwnerUserId(rs.getInt("user_id"));
        assignment.setProjectId(rs.getInt("project_id"));
        assignment.setProjectTitle(rs.getString("project_title"));
        assignment.setTitle(rs.getString("titre"));
        assignment.setDescription(rs.getString("description"));
        LocalDateTime startDateTime = readFlexibleDateTime(rs, "date_debut");
        LocalDateTime endDateTime = readFlexibleDateTime(rs, "date_fin");
        assignment.setStartDate(startDateTime == null ? null : startDateTime.toLocalDate());
        assignment.setEndDate(endDateTime == null ? null : endDateTime.toLocalDate());
        assignment.setStartTime(startDateTime == null ? LocalTime.of(9, 0) : startDateTime.toLocalTime().withSecond(0).withNano(0));
        assignment.setEndTime(endDateTime == null ? LocalTime.of(13, 0) : endDateTime.toLocalTime().withSecond(0).withNano(0));
        assignment.setPriority(normalizePriority(rs.getString("priorite")));
        assignment.setStatus(normalizeStatus(rs.getString("statut")));
        assignment.setOwnedByCurrentUser(readInt(rs, "owned_by_current_user") == 1 || rs.getInt("user_id") == currentUserId);
        assignment.setOwnerName(buildName(rs.getString("first_name"), rs.getString("last_name")));
        assignment.setCreatedAt(readDateTime(rs, "created_at"));
        assignment.setUpdatedAt(readDateTime(rs, "updated_at"));
        return assignment;
    }

    private LocalDate readDate(ResultSet rs, String column) throws SQLException {
        Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private LocalDateTime readFlexibleDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        if (timestamp != null) {
            return timestamp.toLocalDateTime();
        }
        LocalDate date = readDate(rs, column);
        return date == null ? null : date.atStartOfDay();
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

    private String buildName(String firstName, String lastName) {
        String fullName = (firstName == null ? "" : firstName.trim()) + " " + (lastName == null ? "" : lastName.trim());
        return fullName.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void normalizeAssignmentDateTime(Assignment assignment) {
        if (assignment == null) {
            return;
        }
        if (assignment.getStartTime() == null) {
            assignment.setStartTime(LocalTime.of(9, 0));
        }
        if (assignment.getEndTime() == null) {
            assignment.setEndTime(LocalTime.of(13, 0));
        }
    }

    private LocalDateTime toDateTime(LocalDate date, LocalTime time) {
        return LocalDateTime.of(date, time == null ? LocalTime.MIDNIGHT : time);
    }

    private void applyStatusDueTimestampIfNeeded(Assignment assignment) {
        if (assignment == null || assignment.getId() <= 0 || assignment.getUserId() <= 0) {
            return;
        }
        Assignment existing = getOwnedAssignmentById(assignment.getId(), assignment.getUserId());
        if (existing == null) {
            return;
        }
        String previousStatus = existing.getStatus() == null ? "" : existing.getStatus().trim();
        String nextStatus = assignment.getStatus() == null ? "" : assignment.getStatus().trim();
        boolean movedToReview = !"Review".equalsIgnoreCase(previousStatus) && "Review".equalsIgnoreCase(nextStatus);
        boolean movedToCompleted = !"Completed".equalsIgnoreCase(previousStatus) && "Completed".equalsIgnoreCase(nextStatus);
        if (movedToReview || movedToCompleted) {
            LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
            assignment.setEndDate(now.toLocalDate());
            assignment.setEndTime(now.toLocalTime());
        }
    }

    private void ensureAssignmentDateTimeSupport() {
        if (!ensureConnection("AssignmentService.ensureAssignmentDateTimeSupport")) {
            return;
        }
        String checkSql = "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'assignment' AND COLUMN_NAME = ?";
        try (
                PreparedStatement checkStatement = connection.prepareStatement(checkSql);
                Statement alterStatement = connection.createStatement()
        ) {
            boolean requiresUpgrade = false;
            for (String column : List.of("date_debut", "date_fin")) {
                checkStatement.setString(1, column);
                try (ResultSet rs = checkStatement.executeQuery()) {
                    if (rs.next() && "date".equalsIgnoreCase(rs.getString("DATA_TYPE"))) {
                        requiresUpgrade = true;
                    }
                }
            }
            if (requiresUpgrade) {
                alterStatement.executeUpdate(
                        "ALTER TABLE assignment "
                                + "MODIFY date_debut DATETIME NOT NULL, "
                                + "MODIFY date_fin DATETIME NOT NULL"
                );
            }
        } catch (SQLException e) {
            System.out.println("AssignmentService.ensureAssignmentDateTimeSupport: " + e.getMessage());
        }
    }
}
