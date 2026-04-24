package com.studyflow.controllers;

import com.studyflow.models.Assignment;
import com.studyflow.models.AssignmentComment;
import com.studyflow.models.Project;
import com.studyflow.models.User;
import com.studyflow.services.AssignmentService;
import com.studyflow.utils.CrudViewContext;
import com.studyflow.utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class AssignmentDetailsController implements Initializable {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label feedbackLabel;
    @FXML private HBox headerBadgesBox;
    @FXML private Label projectValueLabel;
    @FXML private Label scheduleValueLabel;
    @FXML private Label ownerValueLabel;
    @FXML private Label statusValueLabel;
    @FXML private Label priorityValueLabel;
    @FXML private Label complexityValueLabel;
    @FXML private Label aiEstimateValueLabel;
    @FXML private Label aiDueValueLabel;
    @FXML private Label descriptionValueLabel;
    @FXML private TextArea descriptionEditorArea;
    @FXML private Button editDescriptionButton;
    @FXML private Button saveDescriptionButton;
    @FXML private Button editButton;
    @FXML private TextArea commentInputArea;
    @FXML private Button postCommentButton;
    @FXML private VBox activityList;

    private final AssignmentService assignmentService = new AssignmentService();
    private Assignment assignment;
    private List<Project> ownedProjects = new ArrayList<>();
    private boolean editingDescription;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assignment = CrudViewContext.consumeAssignment();
        ownedProjects = CrudViewContext.consumeOwnedProjects();
        if (assignment == null) {
            showFeedback("No assignment was selected.", true);
            disableInteractiveElements();
            return;
        }
        populateView();
    }

    @FXML
    private void handleBack() {
        CrudViewContext.rememberAssignmentSelection(assignment == null ? null : assignment.getId());
        MainController.loadContentInMainArea("views/Assignments.fxml");
    }

    @FXML
    private void handleEdit() {
        if (assignment == null || !assignment.canCurrentUserEdit()) {
            return;
        }
        CrudViewContext.setAssignmentContext(assignment, ownedProjects);
        CrudViewContext.rememberAssignmentSelection(assignment.getId());
        MainController.loadContentInMainArea("views/AssignmentEditDialog.fxml");
    }

    @FXML
    private void handleEditDescription() {
        if (assignment == null || !assignment.canCurrentUserEdit()) {
            return;
        }
        editingDescription = true;
        syncDescriptionMode();
    }

    @FXML
    private void handleSaveDescription() {
        if (assignment == null || !assignment.canCurrentUserEdit()) {
            return;
        }
        assignment.setCurrentUserId(getCurrentUser() == null ? 0 : getCurrentUser().getId());
        assignment.setDescription(commentSafe(descriptionEditorArea.getText()));
        assignmentService.update(assignment);
        editingDescription = false;
        populateView();
        showFeedback("Description updated.", false);
    }

    @FXML
    private void handlePostComment() {
        if (assignment == null) {
            showFeedback("No assignment selected.", true);
            return;
        }
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            showFeedback("No active user session.", true);
            return;
        }
        boolean added = assignmentService.addComment(assignment.getId(), currentUser.getId(), commentInputArea.getText());
        if (!added) {
            showFeedback("Unable to post comment.", true);
            return;
        }
        commentInputArea.clear();
        showFeedback("Comment added.", false);
        populateActivity();
    }

    private void populateView() {
        titleLabel.setText(safeText(assignment.getTitle(), "Untitled assignment"));
        subtitleLabel.setText(buildSubtitle());
        projectValueLabel.setText(safeText(assignment.getProjectTitle(), "No linked project"));
        scheduleValueLabel.setText(buildSchedule());
        ownerValueLabel.setText(assignment.isOwnedByCurrentUser()
                ? "You"
                : safeText(assignment.getOwnerName(), "Shared user"));
        statusValueLabel.setText(safeText(assignment.getStatus(), "Unknown"));
        priorityValueLabel.setText(safeText(assignment.getPriority(), "Unknown"));
        complexityValueLabel.setText(safeText(assignment.getComplexityLevel(), "Not estimated"));
        aiEstimateValueLabel.setText(buildAiEstimate());
        aiDueValueLabel.setText(formatDate(assignment.getAiSuggestedDueDate()));
        descriptionValueLabel.setText(safeText(assignment.getDescription(), "Add a more detailed description..."));
        descriptionEditorArea.setText(assignment.getDescription() == null ? "" : assignment.getDescription());
        populateHeaderBadges();
        populateActivity();
        editingDescription = false;
        syncDescriptionMode();
        if (editButton != null) {
            editButton.setDisable(!assignment.canCurrentUserEdit());
        }
        if (editDescriptionButton != null) {
            editDescriptionButton.setDisable(!assignment.canCurrentUserEdit());
        }
        if (saveDescriptionButton != null) {
            saveDescriptionButton.setDisable(!assignment.canCurrentUserEdit());
        }
    }

    private void populateHeaderBadges() {
        headerBadgesBox.getChildren().clear();
        headerBadgesBox.getChildren().add(createBadgeNode(safeText(assignment.getProjectTitle(), "Project"), "secondary"));
        headerBadgesBox.getChildren().add(createBadgeNode(safeText(assignment.getPriority(), "Priority"), assignment.getPriorityStyleClass()));
        headerBadgesBox.getChildren().add(createBadgeNode(safeText(assignment.getStatus(), "Status"), statusStyleClass()));
        if (assignment.isOverdue()) {
            headerBadgesBox.getChildren().add(createBadgeNode("Overdue", "danger"));
        }
        if (!assignment.isOwnedByCurrentUser()) {
            headerBadgesBox.getChildren().add(createBadgeNode("Shared", "accent"));
        }
    }

    private void populateActivity() {
        activityList.getChildren().clear();
        for (ActivityEntry entry : buildActivityEntries()) {
            activityList.getChildren().add(createActivityRow(entry));
        }
        if (activityList.getChildren().isEmpty()) {
            activityList.getChildren().add(createEmptyRow("No activity yet."));
        }
    }

    private List<ActivityEntry> buildActivityEntries() {
        List<ActivityEntry> entries = new ArrayList<>();
        List<AssignmentComment> comments = assignmentService.getCommentsByAssignmentId(
                assignment.getId(),
                getCurrentUser() == null ? -1 : getCurrentUser().getId()
        );

        if (assignment.getCreatedAt() != null) {
            entries.add(new ActivityEntry(
                    safeText(assignment.getOwnerName(), assignment.isOwnedByCurrentUser() ? "You" : "Owner"),
                    "created this assignment",
                    assignment.getCreatedAt(),
                    false
            ));
        }
        if (assignment.getUpdatedAt() != null && !assignment.getUpdatedAt().equals(assignment.getCreatedAt())) {
            entries.add(new ActivityEntry(
                    safeText(assignment.getOwnerName(), assignment.isOwnedByCurrentUser() ? "You" : "Owner"),
                    "updated this assignment",
                    assignment.getUpdatedAt(),
                    false
            ));
        }
        for (AssignmentComment comment : comments) {
            entries.add(new ActivityEntry(
                    safeText(comment.getAuthorName(), "User"),
                    commentSafe(comment.getContent()),
                    comment.getCreatedAt(),
                    true
            ));
        }
        entries.sort(Comparator.comparing(ActivityEntry::timestamp, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return entries;
    }

    private VBox createActivityRow(ActivityEntry entry) {
        VBox box = new VBox(4);
        box.getStyleClass().add("detail-row");
        box.setPadding(new Insets(12));

        HBox top = new HBox(10);
        top.setAlignment(Pos.TOP_LEFT);

        Label avatar = new Label(initialsFor(entry.author()));
        avatar.setPrefSize(34, 34);
        avatar.setMinSize(34, 34);
        avatar.setAlignment(Pos.CENTER);
        avatar.getStyleClass().add("board-avatar-chip");

        VBox body = new VBox(4);
        HBox.setHgrow(body, Priority.ALWAYS);

        Label title = new Label(entry.author() + (entry.comment() ? " commented" : " " + entry.message()));
        title.getStyleClass().add("card-title");
        title.setWrapText(true);

        if (entry.comment()) {
            Label comment = new Label(entry.message());
            comment.getStyleClass().add("item-desc");
            comment.setWrapText(true);
            body.getChildren().addAll(title, comment);
        } else {
            body.getChildren().add(title);
        }

        Label time = new Label(formatDateTime(entry.timestamp()));
        time.getStyleClass().add("text-muted");
        body.getChildren().add(time);

        top.getChildren().addAll(avatar, body);
        box.getChildren().add(top);
        return box;
    }

    private VBox createEmptyRow(String message) {
        VBox box = new VBox();
        box.getStyleClass().add("detail-row");
        box.setPadding(new Insets(12));
        Label label = new Label(message);
        label.getStyleClass().add("text-muted");
        box.getChildren().add(label);
        return box;
    }

    private HBox createBadgeNode(String text, String styleClass) {
        HBox badge = new HBox();
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.getStyleClass().addAll("badge", styleClass);
        badge.getChildren().add(new Label(text));
        return badge;
    }

    private void syncDescriptionMode() {
        boolean canEdit = assignment != null && assignment.canCurrentUserEdit();
        descriptionValueLabel.setVisible(!editingDescription);
        descriptionValueLabel.setManaged(!editingDescription);
        descriptionEditorArea.setVisible(editingDescription);
        descriptionEditorArea.setManaged(editingDescription);
        editDescriptionButton.setVisible(canEdit && !editingDescription);
        editDescriptionButton.setManaged(canEdit && !editingDescription);
        saveDescriptionButton.setVisible(canEdit && editingDescription);
        saveDescriptionButton.setManaged(canEdit && editingDescription);
    }

    private void disableInteractiveElements() {
        if (editButton != null) {
            editButton.setDisable(true);
        }
        if (editDescriptionButton != null) {
            editDescriptionButton.setDisable(true);
        }
        if (saveDescriptionButton != null) {
            saveDescriptionButton.setDisable(true);
        }
        if (postCommentButton != null) {
            postCommentButton.setDisable(true);
        }
        if (commentInputArea != null) {
            commentInputArea.setDisable(true);
        }
    }

    private void showFeedback(String message, boolean error) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("inline-alert-success", "inline-alert-error");
        feedbackLabel.getStyleClass().add(error ? "inline-alert-error" : "inline-alert-success");
        feedbackLabel.setVisible(true);
        feedbackLabel.setManaged(true);
    }

    private String buildSubtitle() {
        String project = safeText(assignment.getProjectTitle(), "No project");
        return "Card details for " + project + ".";
    }

    private String buildSchedule() {
        return formatDate(assignment.getStartDate()) + " " + formatTime(assignment.getStartTime())
                + " -> " + formatDate(assignment.getEndDate()) + " " + formatTime(assignment.getEndTime());
    }

    private String buildAiEstimate() {
        if (assignment.getEstimatedMinDays() == null && assignment.getEstimatedMaxDays() == null) {
            return "No AI estimate";
        }
        if (assignment.getEstimatedMinDays() != null && assignment.getEstimatedMaxDays() != null) {
            return assignment.getEstimatedMinDays() + " to " + assignment.getEstimatedMaxDays() + " days";
        }
        Integer value = assignment.getEstimatedMinDays() != null ? assignment.getEstimatedMinDays() : assignment.getEstimatedMaxDays();
        return value + " days";
    }

    private String statusStyleClass() {
        String status = safeText(assignment.getStatus(), "").toLowerCase();
        if (status.contains("progress")) {
            return "primary";
        }
        if (status.contains("review")) {
            return "warning";
        }
        if (status.contains("completed")) {
            return "success";
        }
        return "secondary";
    }

    private String initialsFor(String value) {
        String text = safeText(value, "?");
        String[] parts = text.split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
            if (initials.length() == 2) {
                break;
            }
        }
        return initials.isEmpty() ? "?" : initials.toString();
    }

    private String formatDate(LocalDate value) {
        return value == null ? "Not set" : value.format(DATE_FORMATTER);
    }

    private String formatTime(LocalTime value) {
        return value == null ? "--:--" : value.withSecond(0).withNano(0).format(TIME_FORMATTER);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "Unknown time" : value.withSecond(0).withNano(0).format(DATE_TIME_FORMATTER);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String commentSafe(String value) {
        return value == null ? "" : value.trim();
    }

    private User getCurrentUser() {
        return UserSession.getInstance().getCurrentUser();
    }

    private record ActivityEntry(String author, String message, LocalDateTime timestamp, boolean comment) {
    }
}
