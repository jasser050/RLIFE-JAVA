package com.studyflow.controllers;

import com.studyflow.models.Project;
import com.studyflow.models.User;
import com.studyflow.services.ProjectService;
import com.studyflow.services.ServiceUser;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminProjectsController implements Initializable {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    @FXML private VBox userProjectGroupsContainer;
    @FXML private Label userCountLabel;
    @FXML private Label projectCountLabel;

    private final ServiceUser serviceUser = new ServiceUser();
    private final ProjectService projectService = new ProjectService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadUserProjects();
    }

    private void loadUserProjects() {
        userProjectGroupsContainer.getChildren().clear();

        List<Project> projects = new ArrayList<>(projectService.getAll());
        Map<Integer, List<Project>> projectsByUser = projects.stream()
                .collect(Collectors.groupingBy(Project::getUserId));

        List<User> users = new ArrayList<>(serviceUser.getAll());
        users.removeIf(user -> isAdminUser(user) || !projectsByUser.containsKey(user.getId()));
        users.sort(Comparator.comparing(this::displayName, String.CASE_INSENSITIVE_ORDER));

        userCountLabel.setText(String.valueOf(users.size()));
        projectCountLabel.setText(String.valueOf(projects.size()));

        if (users.isEmpty()) {
            userProjectGroupsContainer.getChildren().add(buildEmptyState("No users found."));
            return;
        }

        for (User user : users) {
            List<Project> ownedProjects = new ArrayList<>(projectsByUser.getOrDefault(user.getId(), List.of()));
            ownedProjects.sort(Comparator.comparing(Project::getUpdatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            userProjectGroupsContainer.getChildren().add(buildUserProjectsSection(user, ownedProjects));
        }
    }

    private VBox buildUserProjectsSection(User user, List<Project> projects) {
        VBox section = new VBox(12);

        HBox headerCard = new HBox(16);
        headerCard.getStyleClass().add("admin-user-card");
        headerCard.setAlignment(Pos.CENTER_LEFT);
        headerCard.setPadding(new Insets(16, 20, 16, 20));

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("admin-user-avatar");
        if (user.isBanned()) {
            avatar.getStyleClass().add("admin-user-avatar-banned");
        }

        Label avatarLabel = new Label(user.getInitials().isEmpty() ? "??" : user.getInitials());
        avatarLabel.getStyleClass().add("admin-user-avatar-text");
        avatar.getChildren().add(avatarLabel);

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(displayName(user));
        nameLabel.getStyleClass().add("admin-user-name");

        HBox details = new HBox(10);
        details.setAlignment(Pos.CENTER_LEFT);
        details.getChildren().add(buildPill("fth-mail", user.getEmail()));
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            details.getChildren().add(buildPill("fth-at-sign", user.getUsername()));
        }
        if (user.getUniversity() != null && !user.getUniversity().isBlank()) {
            details.getChildren().add(buildPill("fth-book-open", user.getUniversity()));
        }
        info.getChildren().addAll(nameLabel, details);

        Label projectBadge = new Label(projects.size() + (projects.size() == 1 ? " Project" : " Projects"));
        projectBadge.getStyleClass().addAll("badge", projects.isEmpty() ? "warning" : "primary");

        Label statusBadge = new Label(user.isBanned() ? "Banned" : "Active");
        statusBadge.getStyleClass().add("admin-status-badge");
        statusBadge.getStyleClass().add(user.isBanned() ? "admin-status-banned" : "admin-status-active");

        headerCard.getChildren().addAll(avatar, info, projectBadge, statusBadge);

        VBox content = new VBox(10);
        if (projects.isEmpty()) {
            content.getChildren().add(buildEmptyState("This user has no projects yet."));
        } else {
            for (Project project : projects) {
                content.getChildren().add(buildProjectCard(project));
            }
        }

        section.getChildren().addAll(headerCard, content);
        return section;
    }

    private HBox buildProjectCard(Project project) {
        HBox card = new HBox(14);
        card.getStyleClass().addAll("detail-row", "project-assignment-row");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(16));

        FontIcon icon = new FontIcon("fth-folder");
        icon.getStyleClass().addAll("detail-row-icon", "project-assignment-icon");

        VBox body = new VBox(8);
        HBox.setHgrow(body, Priority.ALWAYS);

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(project.getTitle());
        title.getStyleClass().addAll("item-title", "project-assignment-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(project.getStatus());
        statusBadge.getStyleClass().addAll("badge", project.getStatusStyleClass());

        top.getChildren().addAll(title, spacer, statusBadge);

        Label description = new Label(project.getDescription() == null || project.getDescription().isBlank()
                ? "No description provided."
                : project.getDescription());
        description.setWrapText(true);
        description.getStyleClass().addAll("item-desc", "project-card-description");

        HBox meta = new HBox(10);
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.getChildren().add(buildProjectMeta("fth-calendar", formatDateRange(project)));
        meta.getChildren().add(buildProjectMeta("fth-check-square", project.getAssignmentCount() + " assignment" + (project.getAssignmentCount() == 1 ? "" : "s")));

        body.getChildren().addAll(top, description, meta);
        card.getChildren().addAll(icon, body);
        return card;
    }

    private HBox buildProjectMeta(String iconLiteral, String text) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(12);
        icon.setIconColor(Color.web("#64748B"));

        Label label = new Label(text);
        label.getStyleClass().add("item-meta");
        box.getChildren().addAll(icon, label);
        return box;
    }

    private HBox buildPill(String iconLiteral, String text) {
        HBox pill = new HBox(5);
        pill.setAlignment(Pos.CENTER_LEFT);
        pill.getStyleClass().add("admin-user-pill");

        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(11);
        icon.setIconColor(Color.web("#64748B"));

        Label label = new Label(text);
        label.getStyleClass().add("admin-user-detail");
        pill.getChildren().addAll(icon, label);
        return pill;
    }

    private VBox buildEmptyState(String message) {
        VBox box = new VBox(10);
        box.getStyleClass().addAll("card", "empty-state-card");
        box.setPadding(new Insets(24));
        box.setAlignment(Pos.CENTER);

        FontIcon icon = new FontIcon("fth-folder-minus");
        icon.getStyleClass().add("empty-state-icon");

        Label title = new Label("Nothing to show");
        title.getStyleClass().add("item-title");

        Label text = new Label(message);
        text.getStyleClass().add("text-muted");
        text.setWrapText(true);

        box.getChildren().addAll(icon, title, text);
        return box;
    }

    private String formatDateRange(Project project) {
        if (project.getStartDate() == null && project.getEndDate() == null) {
            return "No dates set";
        }
        if (project.getStartDate() == null) {
            return "Due " + DATE_FORMATTER.format(project.getEndDate());
        }
        if (project.getEndDate() == null) {
            return "Starts " + DATE_FORMATTER.format(project.getStartDate());
        }
        return DATE_FORMATTER.format(project.getStartDate()) + " - " + DATE_FORMATTER.format(project.getEndDate());
    }

    private String displayName(User user) {
        String fullName = user.getFullName() == null ? "" : user.getFullName().trim();
        return fullName.isEmpty() ? user.getUsername() : fullName;
    }

    private boolean isAdminUser(User user) {
        return user != null && "admin@rlife.com".equalsIgnoreCase(user.getEmail());
    }
}
