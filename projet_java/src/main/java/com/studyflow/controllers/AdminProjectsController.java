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
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminProjectsController implements Initializable {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @FXML private Label adminUsersCountLabel;
    @FXML private Label adminProjectsCountLabel;
    @FXML private TextField adminSearchField;
    @FXML private VBox adminUsersProjectsList;

    private final ServiceUser userService = new ServiceUser();
    private final ProjectService projectService = new ProjectService();

    private List<User> users;
    private List<Project> projects;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        users = userService.getAll();
        projects = projectService.getAll();

        adminUsersCountLabel.setText(String.valueOf(users.size()));
        adminProjectsCountLabel.setText(String.valueOf(projects.size()));
        adminSearchField.textProperty().addListener((obs, oldValue, newValue) -> renderUsersProjects());
        renderUsersProjects();
    }

    private void renderUsersProjects() {
        adminUsersProjectsList.getChildren().clear();

        List<User> filteredUsers = users.stream()
                .filter(this::matchesSearch)
                .collect(Collectors.toList());

        if (filteredUsers.isEmpty()) {
            adminUsersProjectsList.getChildren().add(createEmptyCard("No users match the current search."));
            return;
        }

        for (User user : filteredUsers) {
            adminUsersProjectsList.getChildren().add(createUserProjectsCard(user));
        }
    }

    private VBox createUserProjectsCard(User user) {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("card");
        card.setPadding(new Insets(18));

        List<Project> userProjects = projects.stream()
                .filter(project -> project.getOwnerUserId() == user.getId())
                .collect(Collectors.toList());

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().addAll("stat-icon-box", "accent");
        iconBox.setPrefSize(42, 42);
        iconBox.getChildren().add(new FontIcon("fth-user"));

        VBox titles = new VBox(4);
        HBox.setHgrow(titles, Priority.ALWAYS);

        String name = user.getFullName().trim().isEmpty() ? user.getEmail() : user.getFullName().trim();
        Label title = new Label(name);
        title.getStyleClass().add("card-title");

        Label subtitle = new Label(user.getEmail() + " | " + userProjects.size() + " projects");
        subtitle.getStyleClass().add("card-subtitle");
        titles.getChildren().addAll(title, subtitle);

        header.getChildren().addAll(iconBox, titles);
        card.getChildren().add(header);

        if (userProjects.isEmpty()) {
            Label empty = new Label("This user has no projects yet.");
            empty.getStyleClass().add("text-muted");
            card.getChildren().add(empty);
            return card;
        }

        for (Project project : userProjects) {
            card.getChildren().add(createProjectRow(project));
        }

        return card;
    }

    private HBox createProjectRow(Project project) {
        HBox row = new HBox(12);
        row.getStyleClass().add("detail-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12));

        FontIcon icon = new FontIcon("fth-folder");
        icon.getStyleClass().add("detail-row-icon");

        VBox textBox = new VBox(4);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label title = new Label(project.getTitle());
        title.getStyleClass().add("item-title");

        Label meta = new Label(project.getStatus() + " | " + formatDate(project) + " | " + project.getAssignmentCount() + " assignments");
        meta.getStyleClass().add("item-meta");
        meta.setWrapText(true);
        textBox.getChildren().addAll(title, meta);

        row.getChildren().addAll(icon, textBox, createBadge(project.getStatus(), project.getStatusStyleClass()));
        return row;
    }

    private HBox createBadge(String text, String styleClass) {
        HBox badge = new HBox();
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.getStyleClass().addAll("badge", styleClass);
        badge.getChildren().add(new Label(text));
        return badge;
    }

    private VBox createEmptyCard(String message) {
        VBox box = new VBox(10);
        box.getStyleClass().addAll("card", "empty-state-card");
        box.setPadding(new Insets(28));
        box.setAlignment(Pos.CENTER);

        FontIcon icon = new FontIcon("fth-users");
        icon.getStyleClass().add("empty-state-icon");

        Label title = new Label("Nothing here yet");
        title.getStyleClass().add("item-title");

        Label text = new Label(message);
        text.getStyleClass().add("text-muted");
        text.setWrapText(true);

        box.getChildren().addAll(icon, title, text);
        return box;
    }

    private boolean matchesSearch(User user) {
        String query = adminSearchField.getText();
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String normalized = query.trim().toLowerCase();
        return safe(user.getFullName()).toLowerCase().contains(normalized)
                || safe(user.getEmail()).toLowerCase().contains(normalized)
                || projects.stream()
                .filter(project -> project.getOwnerUserId() == user.getId())
                .anyMatch(project -> safe(project.getTitle()).toLowerCase().contains(normalized));
    }

    private String formatDate(Project project) {
        if (project.getStartDate() == null || project.getEndDate() == null) {
            return "--";
        }
        return DATE_FORMATTER.format(project.getStartDate()) + " -> " + DATE_FORMATTER.format(project.getEndDate());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
