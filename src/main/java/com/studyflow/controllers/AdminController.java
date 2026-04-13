package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import com.studyflow.utils.UserSession;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

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

    private List<User> cachedUsers;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeButton = btnAdminDash;
        showDashboard();

        // Pre-load users for search
        cachedUsers = new ServiceUser().getAll();

        // Dynamic search — filter as the user types
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String q = newVal.trim().toLowerCase();
            if (q.isEmpty()) {
                // Restore whatever page was active
                if (activeButton == btnAdminDash) showDashboard();
                else if (activeButton == btnAdminUsers) showUsers();
                else if (activeButton == btnAdminAudit) showAuditLog();
                else showDashboard();
                return;
            }
            showSearchResults(q);
        });
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
    private void onSearchSubmit() {
        String q = searchField.getText().trim().toLowerCase();
        if (!q.isEmpty()) showSearchResults(q);
    }

    @SuppressWarnings("unchecked")
    private void showSearchResults(String q) {
        List<User> results = cachedUsers.stream()
                .filter(u -> u.getFullName().toLowerCase().contains(q)
                        || u.getEmail().toLowerCase().contains(q)
                        || u.getUsername().toLowerCase().contains(q))
                .collect(Collectors.toList());

        VBox container = new VBox(16);
        container.setStyle("-fx-padding: 24;");

        Label title = new Label("Search Results — \"" + q + "\" (" + results.size() + " found)");
        title.getStyleClass().add("text-heading");

        if (results.isEmpty()) {
            Label empty = new Label("No users matching your search.");
            empty.getStyleClass().add("text-muted");
            empty.setStyle("-fx-font-size: 15px; -fx-padding: 32 0;");
            container.getChildren().addAll(title, empty);
        } else {
            TableView<User> table = new TableView<>();
            table.getStyleClass().add("admin-table");
            table.setPrefHeight(Math.min(60 + results.size() * 36, 500));

            TableColumn<User, String> idCol = new TableColumn<>("ID");
            idCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
            idCol.setPrefWidth(50);

            TableColumn<User, String> nameCol = new TableColumn<>("Name");
            nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName().trim()));
            nameCol.setPrefWidth(180);

            TableColumn<User, String> emailCol = new TableColumn<>("Email");
            emailCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
            emailCol.setPrefWidth(240);

            TableColumn<User, String> genderCol = new TableColumn<>("Gender");
            genderCol.setCellValueFactory(c -> new SimpleStringProperty(
                    c.getValue().getGender() != null ? c.getValue().getGender() : "N/A"));
            genderCol.setPrefWidth(80);

            TableColumn<User, String> statusCol = new TableColumn<>("Status");
            statusCol.setCellValueFactory(c -> new SimpleStringProperty(
                    c.getValue().isBanned() ? "Banned" : "Active"));
            statusCol.setPrefWidth(100);
            statusCol.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); setStyle(""); return; }
                    setText(item);
                    setStyle("Banned".equals(item)
                        ? "-fx-text-fill: #F43F5E; -fx-font-weight: bold;"
                        : "-fx-text-fill: #34D399; -fx-font-weight: bold;");
                }
            });

            TableColumn<User, String> dateCol = new TableColumn<>("Joined");
            dateCol.setCellValueFactory(c -> new SimpleStringProperty(
                    c.getValue().getCreatedAt() != null
                        ? c.getValue().getCreatedAt().substring(0, Math.min(16, c.getValue().getCreatedAt().length()))
                        : "N/A"));
            dateCol.setPrefWidth(150);

            table.getColumns().addAll(idCol, nameCol, emailCol, genderCol, statusCol, dateCol);
            table.setItems(FXCollections.observableArrayList(results));

            container.getChildren().addAll(title, table);
        }

        if (activeButton != null) activeButton.getStyleClass().remove("active");

        contentArea.getChildren().clear();
        contentArea.getChildren().add(container);
    }

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
