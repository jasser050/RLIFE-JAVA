package com.studyflow.controllers;

import com.studyflow.models.Assignment;
import com.studyflow.models.Project;
import com.studyflow.models.User;
import com.studyflow.services.AssignmentService;
import com.studyflow.services.ProjectService;
import com.studyflow.services.ServiceUser;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminDashboardController implements Initializable {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML private Label totalUsersLabel;
    @FXML private Label totalProjectsLabel;
    @FXML private Label totalAssignmentsLabel;
    @FXML private Label sharedProjectsLabel;
    @FXML private VBox recentProjectsList;
    @FXML private VBox highlightedUsersList;

    private final ServiceUser userService = new ServiceUser();
    private final ProjectService projectService = new ProjectService();
    private final AssignmentService assignmentService = new AssignmentService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<User> users = userService.getAll();
        List<Project> projects = projectService.getAll();
        List<Assignment> assignments = assignmentService.getAll();

        totalUsersLabel.setText(String.valueOf(users.size()));
        totalProjectsLabel.setText(String.valueOf(projects.size()));
        totalAssignmentsLabel.setText(String.valueOf(assignments.size()));
        sharedProjectsLabel.setText(String.valueOf(projectService.countProjectShares()));

        renderRecentProjects(projects);
        renderHighlightedUsers(users, projects);
    }

    private void renderRecentProjects(List<Project> projects) {
        recentProjectsList.getChildren().clear();
        List<Project> recent = projects.stream()
                .sorted(Comparator.comparing(Project::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .collect(Collectors.toList());

        if (recent.isEmpty()) {
            recentProjectsList.getChildren().add(createInfoRow("No projects in the database yet.", "fth-folder"));
            return;
        }

        for (Project project : recent) {
            String meta = safe(project.getOwnerName()) + " | " + project.getStatus() + " | "
                    + project.getAssignmentCount() + " assignments";
            recentProjectsList.getChildren().add(createItemRow(project.getTitle(), meta, "fth-folder"));
        }
    }

    private void renderHighlightedUsers(List<User> users, List<Project> projects) {
        highlightedUsersList.getChildren().clear();
        if (users.isEmpty()) {
            highlightedUsersList.getChildren().add(createInfoRow("No users found.", "fth-users"));
            return;
        }

        for (User user : users.stream().limit(6).collect(Collectors.toList())) {
            long userProjects = projects.stream().filter(project -> project.getOwnerUserId() == user.getId()).count();
            long completed = projects.stream().filter(project -> project.getOwnerUserId() == user.getId() && project.isCompleted()).count();
            String title = user.getFullName().trim().isEmpty() ? user.getEmail() : user.getFullName().trim();
            String meta = user.getEmail() + " | " + userProjects + " projects | " + completed + " completed";
            highlightedUsersList.getChildren().add(createItemRow(title, meta, "fth-user"));
        }
    }

    private HBox createItemRow(String title, String meta, String iconLiteral) {
        HBox row = new HBox(12);
        row.getStyleClass().add("detail-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14));

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().addAll("stat-icon-box", "primary");
        iconBox.setPrefSize(36, 36);
        iconBox.getChildren().add(new FontIcon(iconLiteral));

        VBox textBox = new VBox(4);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("item-title");

        Label metaLabel = new Label(meta);
        metaLabel.getStyleClass().add("item-meta");
        metaLabel.setWrapText(true);

        textBox.getChildren().addAll(titleLabel, metaLabel);
        row.getChildren().addAll(iconBox, textBox);
        return row;
    }

    private HBox createInfoRow(String text, String iconLiteral) {
        return createItemRow(text, "RLIFE admin overview", iconLiteral);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
