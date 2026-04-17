package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import com.studyflow.utils.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AdminController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private TextField searchField;
    @FXML private FontIcon maximizeIcon;

    @FXML private Button btnAdminDash;
    @FXML private Button btnAdminUsers;
    @FXML private Button btnAdminAudit;
    @FXML private Button btnAdminAI;

    private Button activeButton;
    private double xOffset = 0, yOffset = 0;
    private boolean isMaximized = false;
    private double savedX, savedY, savedWidth, savedHeight;

    private final ServiceUser serviceUser = new ServiceUser();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        activeButton = btnAdminDash;
        showDashboard();

        // Dynamic search — uses stream-based filtering in ServiceUser
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String q = newVal.trim();
            if (q.isEmpty()) {
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
    @FXML private void showAI()         { setActiveButton(btnAdminAI);    loadContent("views/admin/AdminAI.fxml"); }

    @FXML
    private void onSearchSubmit() {
        String q = searchField.getText().trim();
        if (!q.isEmpty()) showSearchResults(q);
    }

    private void showSearchResults(String query) {
        // Stream-based search in the service layer
        List<User> results = serviceUser.searchUsers(query);

        VBox container = new VBox(16);
        container.setPadding(new Insets(24));

        Label title = new Label("Search Results — \"" + query + "\" (" + results.size() + " found)");
        title.getStyleClass().add("text-heading");

        container.getChildren().add(title);

        if (results.isEmpty()) {
            Label empty = new Label("No users matching your search.");
            empty.getStyleClass().add("text-muted");
            empty.setStyle("-fx-font-size: 15px; -fx-padding: 32 0;");
            container.getChildren().add(empty);
        } else {
            VBox cardsBox = new VBox(10);
            cardsBox.setPadding(new Insets(4));
            for (User user : results) {
                cardsBox.getChildren().add(buildSearchUserCard(user));
            }
            ScrollPane scroll = new ScrollPane(cardsBox);
            scroll.setFitToWidth(true);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.getStyleClass().add("auth-scroll");
            VBox.setVgrow(scroll, Priority.ALWAYS);
            container.getChildren().add(scroll);
        }

        if (activeButton != null) activeButton.getStyleClass().remove("active");
        contentArea.getChildren().clear();
        contentArea.getChildren().add(container);
    }

    private HBox buildSearchUserCard(User user) {
        HBox card = new HBox(16);
        card.getStyleClass().add("admin-user-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 20, 14, 20));

        // Avatar
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("admin-user-avatar");
        if (user.isBanned()) avatar.getStyleClass().add("admin-user-avatar-banned");
        String initials = user.getInitials().isEmpty() ? "??" : user.getInitials();
        Label avatarLabel = new Label(initials);
        avatarLabel.getStyleClass().add("admin-user-avatar-text");
        avatar.getChildren().add(avatarLabel);

        // Info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(user.getFullName().trim());
        nameLabel.getStyleClass().add("admin-user-name");

        HBox details = new HBox(10);
        details.setAlignment(Pos.CENTER_LEFT);

        // Email pill
        HBox emailPill = buildPill("fth-mail", user.getEmail());
        details.getChildren().add(emailPill);

        // Username pill
        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            details.getChildren().add(buildPill("fth-at-sign", user.getUsername()));
        }

        // University pill
        if (user.getUniversity() != null && !user.getUniversity().isEmpty()) {
            details.getChildren().add(buildPill("fth-book-open", user.getUniversity()));
        }

        info.getChildren().addAll(nameLabel, details);

        // Status badge
        Label statusBadge = new Label(user.isBanned() ? "Banned" : "Active");
        statusBadge.getStyleClass().add("admin-status-badge");
        statusBadge.getStyleClass().add(user.isBanned() ? "admin-status-banned" : "admin-status-active");

        // Joined date
        VBox dateBox = new VBox(2);
        dateBox.setAlignment(Pos.CENTER_RIGHT);
        FontIcon clockIcon = new FontIcon("fth-clock");
        clockIcon.setIconSize(12);
        clockIcon.setIconColor(Color.web("#475569"));
        String dateStr = user.getCreatedAt() != null
                ? user.getCreatedAt().substring(0, Math.min(10, user.getCreatedAt().length()))
                : "N/A";
        Label dateLabel = new Label(dateStr);
        dateLabel.getStyleClass().add("admin-log-date");
        dateBox.getChildren().addAll(clockIcon, dateLabel);

        card.getChildren().addAll(avatar, info, statusBadge, dateBox);
        return card;
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
