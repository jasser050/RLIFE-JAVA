package com.studyflow.services;

import com.studyflow.interfaces.IService;
import com.studyflow.models.Assignment;
import com.studyflow.models.AssignmentComment;
import com.studyflow.models.AssignmentDependency;
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
        ensureAssignmentAiPlanningSupport();
        ensureDependencySupport();
        normalizeAssignmentDateTime(assignment);
        String sql = "INSERT INTO assignment (user_id, project_id, titre, description, date_debut, date_fin, priorite, statut, estimated_min_days, estimated_max_days, complexity_level, ai_suggested_due_date, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
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
            setNullableInteger(statement, 9, assignment.getEstimatedMinDays());
            setNullableInteger(statement, 10, assignment.getEstimatedMaxDays());
            statement.setString(11, assignment.getComplexityLevel());
            setNullableDate(statement, 12, assignment.getAiSuggestedDueDate());
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
        ensureAssignmentAiPlanningSupport();
        ensureDependencySupport();
        applyStatusDueTimestampIfNeeded(assignment);
        normalizeAssignmentDateTime(assignment);
        int actorUserId = resolveActorUserId(assignment);
        String sql = "UPDATE assignment a " +
                "LEFT JOIN assignment_collaborator ac ON ac.assignment_id = a.id AND ac.user_id = ? " +
                "LEFT JOIN project_share ps ON ps.project_id = a.project_id AND ps.shared_with_user_id = ? " +
                "SET a.project_id = ?, a.titre = ?, a.description = ?, a.date_debut = ?, a.date_fin = ?, a.priorite = ?, a.statut = ?, " +
                "a.estimated_min_days = ?, a.estimated_max_days = ?, a.complexity_level = ?, a.ai_suggested_due_date = ?, " +
                "a.git_commit_message = ?, a.git_commit_pathspec = ?, a.git_last_commit_hash = ?, a.git_last_commit_at = ?, a.updated_at = NOW() " +
                "WHERE a.id = ? AND (a.user_id = ? OR (ac.user_id = ? AND ps.role = 'editor'))";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, actorUserId);
            statement.setInt(2, actorUserId);
            statement.setInt(3, assignment.getProjectId());
            statement.setString(4, assignment.getTitle());
            statement.setString(5, assignment.getDescription());
            statement.setTimestamp(6, Timestamp.valueOf(toDateTime(assignment.getStartDate(), assignment.getStartTime())));
            statement.setTimestamp(7, Timestamp.valueOf(toDateTime(assignment.getEndDate(), assignment.getEndTime())));
            statement.setString(8, assignment.getPriority());
            statement.setString(9, assignment.getStatus());
            setNullableInteger(statement, 10, assignment.getEstimatedMinDays());
            setNullableInteger(statement, 11, assignment.getEstimatedMaxDays());
            statement.setString(12, assignment.getComplexityLevel());
            setNullableDate(statement, 13, assignment.getAiSuggestedDueDate());
            setNullableString(statement, 14, assignment.getGitCommitMessage());
            setNullableString(statement, 15, assignment.getGitCommitPathspec());
            setNullableString(statement, 16, assignment.getGitLastCommitHash());
            setNullableDateTime(statement, 17, assignment.getGitLastCommitAt());
            statement.setInt(18, assignment.getId());
            statement.setInt(19, actorUserId);
            statement.setInt(20, actorUserId);
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

        String sql = "SELECT a.*, p.titre AS project_title, u.first_name, u.last_name, 1 AS owned_by_current_user, 1 AS editable_by_current_user " +
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
        ensureAssignmentAiPlanningSupport();
        ensureDependencySupport();

        String sql = "SELECT a.*, p.titre AS project_title, u.first_name, u.last_name, " +
                "CASE WHEN a.user_id = ? THEN 1 ELSE 0 END AS owned_by_current_user, " +
                "CASE WHEN a.user_id = ? THEN 1 WHEN ac.user_id = ? AND COALESCE(ps.role, 'viewer') = 'editor' THEN 1 ELSE 0 END AS editable_by_current_user " +
                "FROM assignment a " +
                "JOIN project p ON p.id = a.project_id " +
                "JOIN user u ON u.id = a.user_id " +
                "LEFT JOIN project_share ps ON ps.project_id = a.project_id AND ps.shared_with_user_id = ? " +
                "LEFT JOIN assignment_collaborator ac ON ac.assignment_id = a.id AND ac.user_id = ? " +
                "WHERE a.user_id = ? OR ac.user_id = ? " +
                "ORDER BY COALESCE(a.updated_at, a.created_at) DESC";

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
        ensureAssignmentAiPlanningSupport();
        ensureDependencySupport();

        String sql = "SELECT a.*, p.titre AS project_title, u.first_name, u.last_name, " +
                "CASE WHEN a.user_id = ? THEN 1 ELSE 0 END AS owned_by_current_user, " +
                "CASE WHEN a.user_id = ? THEN 1 WHEN ac.user_id = ? AND COALESCE(ps.role, 'viewer') = 'editor' THEN 1 ELSE 0 END AS editable_by_current_user " +
                "FROM assignment a " +
                "JOIN project p ON p.id = a.project_id " +
                "JOIN user u ON u.id = a.user_id " +
                "LEFT JOIN project_share ps ON ps.project_id = a.project_id AND ps.shared_with_user_id = ? " +
                "LEFT JOIN assignment_collaborator ac ON ac.assignment_id = a.id AND ac.user_id = ? " +
                "WHERE a.project_id = ? AND (a.user_id = ? OR ac.user_id = ?) " +
                "ORDER BY COALESCE(a.updated_at, a.created_at) DESC";

        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, userId);
            statement.setInt(2, userId);
            statement.setInt(3, userId);
            statement.setInt(4, userId);
            statement.setInt(5, userId);
            statement.setInt(6, projectId);
            statement.setInt(7, userId);
            statement.setInt(8, userId);
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

    public List<User> getSharedUsers(int assignmentId) {
        List<User> sharedUsers = new ArrayList<>();
        if (!ensureConnection("AssignmentService.getSharedUsers")) {
            return sharedUsers;
        }

        String sql = "SELECT u.id, u.email, u.first_name, u.last_name, u.username, u.phone_number, u.student_id " +
                "FROM assignment_collaborator ac " +
                "INNER JOIN `user` u ON u.id = ac.user_id " +
                "WHERE ac.assignment_id = ? " +
                "ORDER BY COALESCE(u.first_name, ''), COALESCE(u.last_name, ''), COALESCE(u.username, '')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, assignmentId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setEmail(rs.getString("email"));
                user.setFirstName(rs.getString("first_name"));
                user.setLastName(rs.getString("last_name"));
                user.setUsername(rs.getString("username"));
                user.setPhoneNumber(rs.getString("phone_number"));
                user.setStudentId(rs.getString("student_id"));
                sharedUsers.add(user);
            }
        } catch (SQLException e) {
            System.out.println("AssignmentService.getSharedUsers: " + e.getMessage());
        }
        return sharedUsers;
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
        ensureAssignmentAiPlanningSupport();
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

    public void updateAiPlanningMetadata(Assignment assignment) {
        if (!ensureConnection("AssignmentService.updateAiPlanningMetadata") || assignment == null || assignment.getId() <= 0) {
            return;
        }
        ensureAssignmentAiPlanningSupport();
        String sql = "UPDATE assignment SET estimated_min_days = ?, estimated_max_days = ?, complexity_level = ?, ai_suggested_due_date = ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableInteger(statement, 1, assignment.getEstimatedMinDays());
            setNullableInteger(statement, 2, assignment.getEstimatedMaxDays());
            statement.setString(3, assignment.getComplexityLevel());
            setNullableDate(statement, 4, assignment.getAiSuggestedDueDate());
            statement.setInt(5, assignment.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("AssignmentService.updateAiPlanningMetadata: " + e.getMessage());
        }
    }

    public List<AssignmentDependency> getDependenciesForProject(int projectId, int userId) {
        List<AssignmentDependency> dependencies = new ArrayList<>();
        if (!ensureConnection("AssignmentService.getDependenciesForProject")) {
            return dependencies;
        }
        ensureDependencySupport();
        if (!projectService.userHasProjectAccess(projectId, userId)) {
            return dependencies;
        }

        String sql = "SELECT ad.*, child.titre AS assignment_title, parent.titre AS depends_on_title " +
                "FROM assignment_dependency ad " +
                "JOIN assignment child ON child.id = ad.assignment_id " +
                "JOIN assignment parent ON parent.id = ad.depends_on_assignment_id " +
                "WHERE ad.project_id = ? ORDER BY child.titre ASC, parent.titre ASC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projectId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                AssignmentDependency dependency = new AssignmentDependency();
                dependency.setId(rs.getInt("id"));
                dependency.setProjectId(rs.getInt("project_id"));
                dependency.setAssignmentId(rs.getInt("assignment_id"));
                dependency.setDependsOnAssignmentId(rs.getInt("depends_on_assignment_id"));
                dependency.setAssignmentTitle(rs.getString("assignment_title"));
                dependency.setDependsOnTitle(rs.getString("depends_on_title"));
                dependency.setRationale(rs.getString("rationale"));
                dependency.setSource(rs.getString("source"));
                dependency.setCreatedAt(readDateTime(rs, "created_at"));
                dependencies.add(dependency);
            }
        } catch (SQLException e) {
            System.out.println("AssignmentService.getDependenciesForProject: " + e.getMessage());
        }
        return dependencies;
    }

    public int replaceProjectDependencies(int projectId, int userId, List<AssignmentDependency> dependencies) {
        if (!ensureConnection("AssignmentService.replaceProjectDependencies")) {
            return 0;
        }
        ensureDependencySupport();
        if (!projectService.userHasProjectAccess(projectId, userId)) {
            return 0;
        }

        String deleteSql = "DELETE FROM assignment_dependency WHERE project_id = ?";
        String insertSql = "INSERT INTO assignment_dependency (project_id, assignment_id, depends_on_assignment_id, rationale, source, created_at) VALUES (?, ?, ?, ?, ?, NOW())";
        int savedCount = 0;
        try (
                PreparedStatement deleteStatement = connection.prepareStatement(deleteSql);
                PreparedStatement insertStatement = connection.prepareStatement(insertSql)
        ) {
            deleteStatement.setInt(1, projectId);
            deleteStatement.executeUpdate();

            if (dependencies == null) {
                return 0;
            }

            for (AssignmentDependency dependency : dependencies) {
                if (dependency == null || dependency.getAssignmentId() <= 0 || dependency.getDependsOnAssignmentId() <= 0) {
                    continue;
                }
                insertStatement.setInt(1, projectId);
                insertStatement.setInt(2, dependency.getAssignmentId());
                insertStatement.setInt(3, dependency.getDependsOnAssignmentId());
                insertStatement.setString(4, dependency.getRationale());
                insertStatement.setString(5, dependency.getSource() == null ? "ai" : dependency.getSource());
                savedCount += insertStatement.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("AssignmentService.replaceProjectDependencies: " + e.getMessage());
        }
        return savedCount;
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
        assignment.setCurrentUserId(currentUserId);
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
        assignment.setEstimatedMinDays(readNullableInt(rs, "estimated_min_days"));
        assignment.setEstimatedMaxDays(readNullableInt(rs, "estimated_max_days"));
        assignment.setComplexityLevel(rs.getString("complexity_level"));
        assignment.setAiSuggestedDueDate(readDate(rs, "ai_suggested_due_date"));
        assignment.setOwnedByCurrentUser(readInt(rs, "owned_by_current_user") == 1 || rs.getInt("user_id") == currentUserId);
        assignment.setEditableByCurrentUser(readInt(rs, "editable_by_current_user") == 1 || assignment.isOwnedByCurrentUser());
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

    private Integer readNullableInt(ResultSet rs, String column) {
        try {
            int value = rs.getInt(column);
            return rs.wasNull() ? null : value;
        } catch (SQLException e) {
            return null;
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

    private void ensureAssignmentAiPlanningSupport() {
        if (!ensureConnection("AssignmentService.ensureAssignmentAiPlanningSupport")) {
            return;
        }
        ensureColumnExists("assignment", "estimated_min_days", "ALTER TABLE assignment ADD COLUMN estimated_min_days INT NULL");
        ensureColumnExists("assignment", "estimated_max_days", "ALTER TABLE assignment ADD COLUMN estimated_max_days INT NULL");
        ensureColumnExists("assignment", "complexity_level", "ALTER TABLE assignment ADD COLUMN complexity_level VARCHAR(32) NULL");
        ensureColumnExists("assignment", "ai_suggested_due_date", "ALTER TABLE assignment ADD COLUMN ai_suggested_due_date DATE NULL");
    }

    private void ensureAssignmentGitSupport() {
        if (!ensureConnection("AssignmentService.ensureAssignmentGitSupport")) {
            return;
        }
        ensureColumnExists("assignment", "git_commit_message", "ALTER TABLE assignment ADD COLUMN git_commit_message VARCHAR(500) NULL");
        ensureColumnExists("assignment", "git_commit_pathspec", "ALTER TABLE assignment ADD COLUMN git_commit_pathspec VARCHAR(500) NULL");
        ensureColumnExists("assignment", "git_last_commit_hash", "ALTER TABLE assignment ADD COLUMN git_last_commit_hash VARCHAR(80) NULL");
        ensureColumnExists("assignment", "git_last_commit_at", "ALTER TABLE assignment ADD COLUMN git_last_commit_at DATETIME NULL");
    }

    private void ensureDependencySupport() {
        if (!ensureConnection("AssignmentService.ensureDependencySupport")) {
            return;
        }
        String sql = "CREATE TABLE IF NOT EXISTS assignment_dependency (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "project_id INT NOT NULL, " +
                "assignment_id INT NOT NULL, " +
                "depends_on_assignment_id INT NOT NULL, " +
                "rationale VARCHAR(500) NULL, " +
                "source VARCHAR(32) NOT NULL DEFAULT 'ai', " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE KEY uq_assignment_dependency (assignment_id, depends_on_assignment_id), " +
                "INDEX idx_assignment_dependency_project (project_id), " +
                "CONSTRAINT fk_assignment_dependency_assignment FOREIGN KEY (assignment_id) REFERENCES assignment(id) ON DELETE CASCADE, " +
                "CONSTRAINT fk_assignment_dependency_parent FOREIGN KEY (depends_on_assignment_id) REFERENCES assignment(id) ON DELETE CASCADE " +
                ")";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("AssignmentService.ensureDependencySupport: " + e.getMessage());
        }
    }

    private void ensureColumnExists(String table, String column, String alterSql) {
        String checkSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
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
            System.out.println("AssignmentService.ensureColumnExists(" + table + "." + column + "): " + e.getMessage());
        }
    }

    private void setNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
            return;
        }
        statement.setInt(index, value);
    }

    private void setNullableDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DATE);
            return;
        }
        statement.setDate(index, Date.valueOf(value));
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

    public void updateGitMetadata(Assignment assignment) {
        if (!ensureConnection("AssignmentService.updateGitMetadata") || assignment == null || assignment.getId() <= 0) {
            return;
        }
        ensureAssignmentGitSupport();
        int actorUserId = resolveActorUserId(assignment);
        String sql = "UPDATE assignment a " +
                "LEFT JOIN assignment_collaborator ac ON ac.assignment_id = a.id AND ac.user_id = ? " +
                "LEFT JOIN project_share ps ON ps.project_id = a.project_id AND ps.shared_with_user_id = ? " +
                "SET a.git_commit_message = ?, a.git_commit_pathspec = ?, a.git_last_commit_hash = ?, a.git_last_commit_at = ?, a.updated_at = NOW() " +
                "WHERE a.id = ? AND (a.user_id = ? OR (ac.user_id = ? AND ps.role = 'editor'))";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, actorUserId);
            statement.setInt(2, actorUserId);
            setNullableString(statement, 3, assignment.getGitCommitMessage());
            setNullableString(statement, 4, assignment.getGitCommitPathspec());
            setNullableString(statement, 5, assignment.getGitLastCommitHash());
            setNullableDateTime(statement, 6, assignment.getGitLastCommitAt());
            statement.setInt(7, assignment.getId());
            statement.setInt(8, actorUserId);
            statement.setInt(9, actorUserId);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("AssignmentService.updateGitMetadata: " + e.getMessage());
        }
    }

    private int resolveActorUserId(Assignment assignment) {
        if (assignment == null) {
            return 0;
        }
        return assignment.getCurrentUserId() > 0 ? assignment.getCurrentUserId() : assignment.getUserId();
    }
}
