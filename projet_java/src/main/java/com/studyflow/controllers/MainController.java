package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.Assignment;
import com.studyflow.models.Notification;
import com.studyflow.models.Project;
import com.studyflow.models.User;
import com.studyflow.services.AssignmentService;
import com.studyflow.services.NotificationService;
import com.studyflow.services.ProjectService;
import com.studyflow.utils.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Main controller for the application layout
 * Handles navigation, content switching, and window controls
 */
public class MainController implements Initializable {
    private static MainController instance;

    @FXML private StackPane contentArea;
    @FXML private TextField searchField;
    @FXML private HBox titleBar;
    @FXML private FontIcon maximizeIcon;
    @FXML private Button themeToggleButton;
    @FXML private FontIcon themeToggleIcon;
    @FXML private Button notificationsButton;

    @FXML private Label sidebarUserName;
    @FXML private Label sidebarUserSub;
    @FXML private Label sidebarAvatar;

    @FXML private Button btnDashboard;
    @FXML private Button btnCourses;
    @FXML private Button btnAssignments;
    @FXML private Button btnPlanning;
    @FXML private Button btnRevisions;
    @FXML private Button btnProjects;
    @FXML private Button btnNotes;
    @FXML private Button btnWellbeing;
    @FXML private Button btnStats;

    private Button activeButton;
    private final ProjectService projectService = new ProjectService();
    private final AssignmentService assignmentService = new AssignmentService();
    private final NotificationService notificationService = new NotificationService();

    // Window dragging
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isMaximized = false;
    private double savedX, savedY, savedWidth, savedHeight;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        activeButton = btnDashboard;
        showDashboard();

        // Populate sidebar user info from session
        User user = UserSession.getInstance().getCurrentUser();
        if (user != null) {
            sidebarUserName.setText(user.getFullName().trim().isEmpty() ? user.getUsername() : user.getFullName().trim());
            sidebarUserSub.setText(isAdminUser()
                    ? "RLIFE Administrator"
                    : (user.getUniversity() != null && !user.getUniversity().isEmpty() ? user.getUniversity() : user.getEmail()));
            sidebarAvatar.setText(user.getInitials().isEmpty() ? "??" : user.getInitials());
        }
        if (isAdminUser()) {
            btnProjects.setText("User Projects");
            searchField.setPromptText("Search users, projects, assignments...");
        }
        syncNotifications();
        updateThemeButton();
    }

    // ============================================
    // WINDOW CONTROL METHODS
    // ============================================

    @FXML
    private void onTitleBarPressed(MouseEvent event) {
        Stage stage = App.getPrimaryStage();
        xOffset = stage.getX() - event.getScreenX();
        yOffset = stage.getY() - event.getScreenY();
    }

    @FXML
    private void onTitleBarDragged(MouseEvent event) {
        Stage stage = App.getPrimaryStage();
        if (!isMaximized) {
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(event.getScreenY() + yOffset);
        }
    }

    @FXML
    private void minimizeWindow() {
        App.getPrimaryStage().setIconified(true);
    }

    @FXML
    private void maximizeWindow() {
        Stage stage = App.getPrimaryStage();

        if (isMaximized) {
            // Restore to previous size
            stage.setX(savedX);
            stage.setY(savedY);
            stage.setWidth(savedWidth);
            stage.setHeight(savedHeight);
            isMaximized = false;
            maximizeIcon.setIconLiteral("fth-square");
        } else {
            // Save current size and position
            savedX = stage.getX();
            savedY = stage.getY();
            savedWidth = stage.getWidth();
            savedHeight = stage.getHeight();

            // Maximize to screen
            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());
            isMaximized = true;
            maximizeIcon.setIconLiteral("fth-copy");
        }
    }

    @FXML
    private void closeWindow() {
        Platform.exit();
    }

    @FXML
    private void toggleTheme() {
        App.toggleTheme();
        updateThemeButton();
    }

    @FXML
    private void showNotifications() {
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null || notificationsButton == null) {
            return;
        }

        syncNotifications();
        List<Notification> notifications = notificationService.getRecentByUserId(user.getId(), 8);
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("notifications-menu");

        if (notifications.isEmpty()) {
            Label emptyLabel = new Label("No notifications yet.");
            emptyLabel.getStyleClass().add("notification-empty-label");
            CustomMenuItem emptyItem = new CustomMenuItem(emptyLabel, false);
            menu.getItems().add(emptyItem);
        } else {
            for (Notification notification : notifications) {
                VBox box = new VBox(4);
                box.getStyleClass().add("notification-item");

                Label title = new Label(notification.getTitle());
                title.getStyleClass().add("notification-title");
                title.setWrapText(true);

                Label message = new Label(notification.getMessage());
                message.getStyleClass().add("notification-message");
                message.setWrapText(true);

                Label meta = new Label(notification.getCreatedAt());
                meta.getStyleClass().add("notification-meta");

                box.getChildren().addAll(title, message, meta);
                CustomMenuItem item = new CustomMenuItem(box, false);
                menu.getItems().add(item);
            }
            menu.getItems().add(new SeparatorMenuItem());
            Button markReadButton = new Button("Mark all as read");
            markReadButton.getStyleClass().add("btn-secondary");
            markReadButton.setMaxWidth(Double.MAX_VALUE);
            markReadButton.setOnAction(event -> {
                notificationService.markAllAsRead(user.getId());
                updateNotificationsButton();
                menu.hide();
            });
            VBox footer = new VBox(markReadButton);
            VBox.setVgrow(markReadButton, Priority.NEVER);
            footer.getStyleClass().add("notification-footer");
            menu.getItems().add(new CustomMenuItem(footer, false));
        }

        menu.show(notificationsButton, javafx.geometry.Side.BOTTOM, 0, 8);
    }

    // ============================================
    // NAVIGATION METHODS
    // ============================================

    /**
     * Load content into the main content area
     */
    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlPath));
            Parent content = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);
        } catch (Exception e) {
            e.printStackTrace();
            VBox errorBox = new VBox(12);
            errorBox.getStyleClass().addAll("page-section", "card");
            Label title = new Label("Unable to load this page");
            title.getStyleClass().add("text-heading");
            Label detail = new Label(e.getMessage() == null ? fxmlPath : e.getMessage());
            detail.getStyleClass().add("text-muted");
            detail.setWrapText(true);
            errorBox.getChildren().setAll(title, detail);
            contentArea.getChildren().clear();
            contentArea.getChildren().add(errorBox);
        }
    }

    public static void loadContentInMainArea(String fxmlPath) {
        if (instance != null) {
            instance.loadContent(fxmlPath);
        }
    }

    /**
     * Update active navigation button
     */
    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        activeButton = button;
        activeButton.getStyleClass().add("active");
    }

    @FXML
    private void showDashboard() {
        setActiveButton(btnDashboard);
        loadContent(isAdminUser() ? "views/AdminDashboard.fxml" : "views/Dashboard.fxml");
    }

    @FXML
    private void showCourses() {
        setActiveButton(btnCourses);
        loadContent("views/Courses.fxml");
    }

    @FXML
    private void showAssignments() {
        setActiveButton(btnAssignments);
        loadContent("views/Assignments.fxml");
    }

    @FXML
    private void showPlanning() {
        setActiveButton(btnPlanning);
        loadContent("views/Planning.fxml");
    }

    @FXML
    private void showRevisions() {
        setActiveButton(btnRevisions);
        loadContent("views/Revisions.fxml");
    }

    @FXML
    private void showProjects() {
        setActiveButton(btnProjects);
        loadContent(isAdminUser() ? "views/AdminProjects.fxml" : "views/Projects.fxml");
    }

    @FXML
    private void showNotes() {
        setActiveButton(btnNotes);
        loadContent("views/Notes.fxml");
    }

    @FXML
    private void showWellbeing() {
        setActiveButton(btnWellbeing);
        loadContent("views/Wellbeing.fxml");
    }

    @FXML
    private void showStats() {
        setActiveButton(btnStats);
        loadContent("views/Statistics.fxml");
    }

    @FXML
    private void showProfile() {
        if (activeButton != null) activeButton.getStyleClass().remove("active");
        activeButton = null;
        loadContent("views/Profile.fxml");
    }

    @FXML
    private void handleLogout() {
        UserSession.getInstance().logout();
        try {
            App.setRoot("views/Landing");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void updateThemeButton() {
        if (themeToggleIcon != null) {
            themeToggleIcon.setIconLiteral(App.isDarkTheme() ? "fth-sun" : "fth-moon");
        }
        if (themeToggleButton != null) {
            themeToggleButton.setText(App.isDarkTheme() ? "Light" : "Dark");
        }
        updateNotificationsButton();
    }

    private void syncNotifications() {
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null || !projectService.isDatabaseAvailable() || !assignmentService.isDatabaseAvailable() || !notificationService.isDatabaseAvailable()) {
            updateNotificationsButton();
            return;
        }

        List<Project> projects = projectService.getByUserId(user.getId());
        List<Assignment> assignments = assignmentService.getByUserId(user.getId());
        notificationService.syncDueDateNotifications(user.getId(), projects, assignments);
        updateNotificationsButton();
    }

    private void updateNotificationsButton() {
        if (notificationsButton == null) {
            return;
        }

        User user = UserSession.getInstance().getCurrentUser();
        int unread = user == null ? 0 : notificationService.countUnreadByUserId(user.getId());
        notificationsButton.setText(unread > 0 ? String.valueOf(unread) : "");
        notificationsButton.setAccessibleText(unread > 0 ? unread + " unread notifications" : "Notifications");
    }

    private boolean isAdminUser() {
        User user = UserSession.getInstance().getCurrentUser();
        return user != null && "admin@rlife.com".equalsIgnoreCase(user.getEmail());
    }
}
