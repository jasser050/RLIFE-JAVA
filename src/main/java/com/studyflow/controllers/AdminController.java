package com.studyflow.controllers;

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
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private TextField searchField;
    @FXML private HBox titleBar;
    @FXML private FontIcon maximizeIcon;

    @FXML private Label sidebarUserName;
    @FXML private Label sidebarUserSub;
    @FXML private Label sidebarAvatar;

    @FXML private Button btnDashboard;
    @FXML private Button btnStats;
    @FXML private Button btnUsers;
    @FXML private Button btnCourses;
    @FXML private Button btnAssignments;
    @FXML private Button btnRevisions;
    @FXML private Button btnNotes;
    @FXML private Button btnProjects;
    @FXML private Button btnSettings;
    @FXML private Button btnLogs;

    private Button activeButton;

    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isMaximized = false;
    private double savedX, savedY, savedWidth, savedHeight;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeButton = btnDashboard;
        showDashboard();

        // Populate sidebar
        User user = UserSession.getInstance().getCurrentUser();
        if (user != null) {
            sidebarUserName.setText(user.getFullName().trim().isEmpty()
                    ? user.getUsername() : user.getFullName().trim());
            sidebarUserSub.setText("Administrator");
            sidebarAvatar.setText(user.getInitials().isEmpty() ? "AD" : user.getInitials());
        }
        if (searchField != null && searchField.getParent() != null) {
            searchField.getParent().setOnMouseClicked(event -> searchField.requestFocus());
        }
    }

    // ====================== WINDOW CONTROLS ======================
    @FXML private void onTitleBarPressed(MouseEvent event) {
        Stage stage = App.getPrimaryStage();
        xOffset = stage.getX() - event.getScreenX();
        yOffset = stage.getY() - event.getScreenY();
    }

    @FXML private void onTitleBarDragged(MouseEvent event) {
        Stage stage = App.getPrimaryStage();
        if (!isMaximized) {
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(event.getScreenY() + yOffset);
        }
    }

    @FXML private void minimizeWindow() { App.getPrimaryStage().setIconified(true); }

    @FXML private void maximizeWindow() {
        Stage stage = App.getPrimaryStage();
        if (isMaximized) {
            stage.setX(savedX); stage.setY(savedY);
            stage.setWidth(savedWidth); stage.setHeight(savedHeight);
            isMaximized = false;
            maximizeIcon.setIconLiteral("fth-square");
        } else {
            savedX = stage.getX(); savedY = stage.getY();
            savedWidth = stage.getWidth(); savedHeight = stage.getHeight();
            javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();
            stage.setX(screen.getMinX()); stage.setY(screen.getMinY());
            stage.setWidth(screen.getWidth()); stage.setHeight(screen.getHeight());
            isMaximized = true;
            maximizeIcon.setIconLiteral("fth-copy");
        }
    }

    @FXML private void closeWindow() { Platform.exit(); }

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
            System.err.println("❌ Failed to load FXML: " + fullPath);
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button button) {
        if (activeButton != null) activeButton.getStyleClass().remove("active");
        activeButton = button;
        activeButton.getStyleClass().add("active");
    }

    @FXML private void showDashboard()   { setActiveButton(btnDashboard);   loadContent("views/admin/AdminDashboard.fxml"); }
    @FXML private void showStats()       { setActiveButton(btnStats);        loadContent("views/admin/AdminStats.fxml"); }
    @FXML private void showUsers()       { setActiveButton(btnUsers);        loadContent("views/admin/AdminUsers.fxml"); }
    @FXML private void showCourses()     { setActiveButton(btnCourses);      loadContent("views/admin/AdminCourses.fxml"); }
    @FXML private void showAssignments() { setActiveButton(btnAssignments);  loadContent("views/admin/AdminAssignments.fxml"); }

    // ====================== DECKS (Admin) ======================
    @FXML
    private void showRevisions() {
        setActiveButton(btnRevisions);
        loadContent("views/Revisions.fxml");   // Admin voit la gestion complète des Decks
    }

    @FXML private void showNotes()       { setActiveButton(btnNotes);        loadContent("views/admin/AdminNotes.fxml"); }
    @FXML private void showProjects()    { setActiveButton(btnProjects);     loadContent("views/admin/AdminProjects.fxml"); }
    @FXML private void showSettings()    { setActiveButton(btnSettings);     loadContent("views/admin/AdminSettings.fxml"); }
    @FXML private void showLogs()        { setActiveButton(btnLogs);         loadContent("views/admin/AdminLogs.fxml"); }

    @FXML private void showProfile() {
        if (activeButton != null) activeButton.getStyleClass().remove("active");
        activeButton = null;
        loadContent("views/Profile.fxml");
    }

    @FXML private void handleAddUser() {
        showUsers();
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
