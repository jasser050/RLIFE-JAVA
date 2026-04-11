package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.User;
import com.studyflow.utils.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private StackPane contentArea;

    // === Labels de la sidebar (User Info) ===
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarUserSub;
    @FXML private Label sidebarAvatar;

    // === Boutons de navigation ===
    @FXML private Button btnDashboard;
    @FXML private Button btnRevisions;
    @FXML private Button btnCourses;
    @FXML private Button btnAssignments;
    @FXML private Button btnPlanning;
    @FXML private Button btnNotes;
    @FXML private Button btnProjects;
    @FXML private Button btnWellbeing;
    @FXML private Button btnStats;
    @FXML private Button btnThemeToggle;
    @FXML private FontIcon themeToggleIcon;

    private Button activeButton;
    private boolean lightThemeEnabled = false;

    private static final String DARK_THEME = "/com/studyflow/styles/dark-theme.css";
    private static final String LIGHT_THEME = "/com/studyflow/styles/light-theme.css";

    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isMaximized = false;
    private double savedX, savedY, savedWidth, savedHeight;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeButton = btnDashboard;
        showDashboard();

        // Mise à jour des informations utilisateur dans la sidebar
        User user = UserSession.getInstance().getCurrentUser();
        if (user != null) {
            String fullName = user.getFullName().trim();
            sidebarUserName.setText(fullName.isEmpty() ? (user.getUsername() != null ? user.getUsername() : "Student") : fullName);
            sidebarUserSub.setText("Student");
            sidebarAvatar.setText(user.getInitials().isEmpty() ? "??" : user.getInitials());
        }
        applyThemeOnScene();
    }

    // ====================== WINDOW CONTROLS ======================
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

    @FXML private void minimizeWindow() {
        App.getPrimaryStage().setIconified(true);
    }

    @FXML private void maximizeWindow() {
        Stage stage = App.getPrimaryStage();
        if (isMaximized) {
            stage.setX(savedX);
            stage.setY(savedY);
            stage.setWidth(savedWidth);
            stage.setHeight(savedHeight);
            isMaximized = false;
        } else {
            savedX = stage.getX();
            savedY = stage.getY();
            savedWidth = stage.getWidth();
            savedHeight = stage.getHeight();
            var screen = javafx.stage.Screen.getPrimary().getVisualBounds();
            stage.setX(screen.getMinX());
            stage.setY(screen.getMinY());
            stage.setWidth(screen.getWidth());
            stage.setHeight(screen.getHeight());
            isMaximized = true;
        }
    }

    @FXML private void closeWindow() {
        Platform.exit();
    }

    @FXML
    private void toggleTheme() {
        lightThemeEnabled = !lightThemeEnabled;
        applyThemeOnScene();
    }

    private void applyThemeOnScene() {
        Scene scene = App.getScene();
        if (scene == null) return;

        String darkUrl = App.class.getResource(DARK_THEME).toExternalForm();
        String lightUrl = App.class.getResource(LIGHT_THEME).toExternalForm();

        if (!scene.getStylesheets().contains(darkUrl)) {
            scene.getStylesheets().add(darkUrl);
        }

        scene.getStylesheets().remove(lightUrl);
        if (lightThemeEnabled) {
            scene.getStylesheets().add(lightUrl);
        }
        updateThemeToggleIcon();
    }

    private void updateThemeToggleIcon() {
        if (themeToggleIcon == null) return;
        themeToggleIcon.setIconLiteral(lightThemeEnabled ? "fth-sun" : "fth-moon");
    }

    // ====================== NAVIGATION ======================
    private void loadContent(String fxmlPath) {
        String fullPath = "/com/studyflow/" + fxmlPath;
        URL resource = App.class.getResource(fullPath);
        if (resource == null) {
            System.err.println("❌ FXML not found: " + fullPath);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Parent content = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);
        } catch (IOException e) {
            System.err.println("❌ Failed to load: " + fullPath);
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        activeButton = button;
        activeButton.getStyleClass().add("active");
    }

    @FXML private void showDashboard() {
        setActiveButton(btnDashboard);
        loadContent("views/Dashboard.fxml");
    }

    @FXML private void showRevisions() {
        setActiveButton(btnRevisions);
        loadContent("views/Flashcards.fxml");
    }

    @FXML private void showCourses() {
        setActiveButton(btnCourses);
        loadContent("views/Courses.fxml");
    }

    @FXML private void showAssignments() {
        setActiveButton(btnAssignments);
        loadContent("views/Assignments.fxml");
    }

    @FXML private void showPlanning() {
        setActiveButton(btnPlanning);
        loadContent("views/Planning.fxml");
    }

    @FXML private void showNotes() {
        setActiveButton(btnNotes);
        loadContent("views/Notes.fxml");
    }

    @FXML private void showProjects() {
        setActiveButton(btnProjects);
        loadContent("views/Projects.fxml");
    }

    @FXML private void showWellbeing() {
        setActiveButton(btnWellbeing);
        loadContent("views/Wellbeing.fxml");
    }

    @FXML private void showStats() {
        setActiveButton(btnStats);
        loadContent("views/Stats.fxml");
    }

    @FXML private void showProfile() {
        if (activeButton != null) activeButton.getStyleClass().remove("active");
        activeButton = null;
        loadContent("views/Profile.fxml");
    }

    @FXML private void handleLogout() {
        UserSession.getInstance().logout();
        try {
            App.setRoot("views/Landing");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
