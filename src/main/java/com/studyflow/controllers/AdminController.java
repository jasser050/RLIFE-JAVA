package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.utils.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private TextField searchField;
    @FXML private FontIcon maximizeIcon;

    @FXML private Button btnAdminDash;
    @FXML private Button btnAdminUsers;
    @FXML private Button btnAdminAudit;

    private Button activeButton;
    private double xOffset = 0, yOffset = 0;
    private boolean isMaximized = false;
    private double savedX, savedY, savedWidth, savedHeight;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeButton = btnAdminDash;
        showDashboard();
    }

    // ── Navigation ──────────────────────────────────────────────────

    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlPath));
            Parent content = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button button) {
        if (activeButton != null) activeButton.getStyleClass().remove("active");
        activeButton = button;
        activeButton.getStyleClass().add("active");
    }

    @FXML private void showDashboard()  { setActiveButton(btnAdminDash);  loadContent("views/admin/AdminDashboard.fxml"); }
    @FXML private void showUsers()      { setActiveButton(btnAdminUsers); loadContent("views/admin/AdminUsers.fxml"); }
    @FXML private void showAuditLog()   { setActiveButton(btnAdminAudit); loadContent("views/admin/AdminAuditLog.fxml"); }

    @FXML
    private void handleLogout() {
        UserSession.getInstance().logout();
        try { App.setRoot("views/Landing"); } catch (IOException e) { e.printStackTrace(); }
    }

    // ── Window controls ─────────────────────────────────────────────

    @FXML private void onTitleBarPressed(MouseEvent e) {
        Stage s = App.getPrimaryStage(); xOffset = s.getX() - e.getScreenX(); yOffset = s.getY() - e.getScreenY();
    }
    @FXML private void onTitleBarDragged(MouseEvent e) {
        if (!isMaximized) { Stage s = App.getPrimaryStage(); s.setX(e.getScreenX() + xOffset); s.setY(e.getScreenY() + yOffset); }
    }
    @FXML private void minimizeWindow() { App.getPrimaryStage().setIconified(true); }
    @FXML private void maximizeWindow() {
        Stage stage = App.getPrimaryStage();
        if (isMaximized) {
            stage.setX(savedX); stage.setY(savedY); stage.setWidth(savedWidth); stage.setHeight(savedHeight);
            isMaximized = false; maximizeIcon.setIconLiteral("fth-square");
        } else {
            savedX = stage.getX(); savedY = stage.getY(); savedWidth = stage.getWidth(); savedHeight = stage.getHeight();
            javafx.geometry.Rectangle2D sb = javafx.stage.Screen.getPrimary().getVisualBounds();
            stage.setX(sb.getMinX()); stage.setY(sb.getMinY()); stage.setWidth(sb.getWidth()); stage.setHeight(sb.getHeight());
            isMaximized = true; maximizeIcon.setIconLiteral("fth-copy");
        }
    }
    @FXML private void closeWindow() { Platform.exit(); }
}
