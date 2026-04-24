package com.studyflow.controllers.admin;

import com.studyflow.App;
import com.studyflow.models.User;
import com.studyflow.utils.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminMainController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private FontIcon maximizeIcon;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarUserSub;
    @FXML private Label sidebarAvatar;
    @FXML private Label workspaceBadge;

    @FXML private Button btnDashboard;
    @FXML private Button btnCourses;
    @FXML private Button btnAssignments;
    @FXML private Button btnPlanning;
    @FXML private Button btnUsers;
    @FXML private Button btnMatieres;
    @FXML private Button btnProjects;
    @FXML private Button btnNotes;
    @FXML private Button btnStressQuestions;
    @FXML private Button btnRecommendations;
    @FXML private Button btnStats;
    @FXML private Button btnAuditLog;
    @FXML private Button btnAiAgent;
    @FXML private Button btnSettings;

    private Button activeButton;
    private double xOffset;
    private double yOffset;
    private boolean isMaximized;
    private double savedX;
    private double savedY;
    private double savedWidth;
    private double savedHeight;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        populateAdminIdentity();
        activeButton = btnDashboard;
        try {
            showDashboard();
        } catch (RuntimeException e) {
            showFallbackContent("Back office loaded, but the dashboard content failed to initialize.");
        }
    }

    private void populateAdminIdentity() {
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        String fullName = user.getFullName().trim();
        sidebarUserName.setText(fullName.isEmpty() ? user.getUsername() : fullName);
        sidebarUserSub.setText(user.getEmail());
        sidebarAvatar.setText(user.getInitials().isEmpty() ? "AD" : user.getInitials());
        workspaceBadge.setText(user.isAdmin() ? "Admin Workspace" : "Workspace");
    }

    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlPath));
            Parent content = loader.load();
            contentArea.getChildren().setAll(content);
        } catch (Exception e) {
            e.printStackTrace();
            String details = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            showFallbackContent("Unable to load " + fxmlPath + ": " + details);
        }
    }

    private void showFallbackContent(String message) {
        VBox box = new VBox(8);
        box.getStyleClass().addAll("card", "admin-panel");
        box.setStyle("-fx-padding: 24; -fx-background-color: #151F35; -fx-background-radius: 18;");
        box.setMaxWidth(760);
        Label title = new Label("Admin View");
        title.getStyleClass().add("card-title");
        Label body = new Label(message);
        body.getStyleClass().add("text-small");
        body.setWrapText(true);
        box.getChildren().setAll(title, body);
        contentArea.getChildren().setAll(box);
    }

    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        activeButton = button;
        if (!activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    @FXML
    private void showDashboard() {
        setActiveButton(btnDashboard);
        loadContent("views/admin/AdminDashboard.fxml");
    }

    @FXML
    private void showUsers() {
        setActiveButton(btnUsers);
        loadContent("views/admin/AdminUsers.fxml");
    }

    @FXML
    private void showCoursesAdmin() {
        setActiveButton(btnCourses);
        showFallbackContent("Courses module is managed from student workspace in this version.");
    }

    @FXML
    private void showAssignmentsAdmin() {
        setActiveButton(btnAssignments);
        showFallbackContent("Assignments module is managed from student workspace in this version.");
    }

    @FXML
    private void showPlanningAdmin() {
        setActiveButton(btnPlanning);
        showFallbackContent("Planning module is managed from student workspace in this version.");
    }

    @FXML
    private void showMatieres() {
        setActiveButton(btnMatieres);
        loadContent("views/admin/AdminMatieres.fxml");
    }

    @FXML
    private void showProjectsAdmin() {
        setActiveButton(btnProjects);
        showFallbackContent("Projects module is managed from student workspace in this version.");
    }

    @FXML
    private void showNotesAdmin() {
        setActiveButton(btnNotes);
        showFallbackContent("Notes module is managed from student workspace in this version.");
    }

    @FXML
    private void showStressQuestions() {
        setActiveButton(btnStressQuestions);
        loadContent("views/admin/AdminStressQuestions.fxml");
    }

    @FXML
    private void showRecommendations() {
        setActiveButton(btnRecommendations);
        loadContent("views/admin/AdminRecommendations.fxml");
    }

    @FXML
    private void showStats() {
        setActiveButton(btnStats);
        loadContent("views/admin/AdminStats.fxml");
    }

    @FXML
    private void showSettings() {
        setActiveButton(btnSettings);
        loadContent("views/admin/AdminSettings.fxml");
    }

    @FXML
    private void showAuditLog() {
        setActiveButton(btnAuditLog);
        showFallbackContent("Audit Log view is not configured yet.");
    }

    @FXML
    private void showAiAgent() {
        setActiveButton(btnAiAgent);
        showFallbackContent("AI Agent view is not configured yet.");
    }

    @FXML
    private void handleLogout() {
        UserSession.getInstance().logout();
        try {
            App.setRoot("views/Landing");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
            stage.setX(savedX);
            stage.setY(savedY);
            stage.setWidth(savedWidth);
            stage.setHeight(savedHeight);
            isMaximized = false;
            maximizeIcon.setIconLiteral("fth-square");
            return;
        }

        savedX = stage.getX();
        savedY = stage.getY();
        savedWidth = stage.getWidth();
        savedHeight = stage.getHeight();

        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());
        stage.setWidth(screenBounds.getWidth());
        stage.setHeight(screenBounds.getHeight());
        isMaximized = true;
        maximizeIcon.setIconLiteral("fth-copy");
    }

    @FXML
    private void closeWindow() {
        Platform.exit();
    }
}
