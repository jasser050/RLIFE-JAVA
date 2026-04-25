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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
    @FXML private ImageView sidebarLogo;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarUserSub;
    @FXML private Label sidebarAvatar;
    @FXML private HBox userProfileRow;
    @FXML private VBox userInfoBox;

    @FXML private Button btnAdminDash;
    @FXML private Button btnCourses;
    @FXML private Button btnPlanning;
    @FXML private Button btnRevisions;
    @FXML private Button btnProjects;
    @FXML private Button btnNotes;
    @FXML private Button btnStats;
    @FXML private Button btnAdminUsers;
    @FXML private Button btnAdminAudit;
    @FXML private Button btnAdminAI;
    @FXML private Button btnStressQuestions;
    @FXML private Button btnRecommendations;

    private Button activeButton;
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isMaximized = false;
    private double savedX;
    private double savedY;
    private double savedWidth;
    private double savedHeight;

    private final ServiceUser serviceUser = new ServiceUser();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadSidebarProfile();

        activeButton = btnAdminDash;
        showDashboard();

        if (userProfileRow != null) {
            userProfileRow.setOnMouseClicked(event -> showProfile());
        }
        if (userInfoBox != null) {
            userInfoBox.setOnMouseClicked(event -> showProfile());
        }

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal.trim();
            if (query.isEmpty()) {
                reloadActiveView();
                return;
            }
            if (isAdminSearchView()) {
                showSearchResults(query);
            }
        });
    }

    private void loadSidebarProfile() {
        if (sidebarLogo != null) {
            sidebarLogo.setImage(new Image(getClass().getResourceAsStream("/com/studyflow/images/logo.png")));
        }

        User user = UserSession.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        String fullName = user.getFullName() == null ? "" : user.getFullName().trim();
        String displayName = fullName.isEmpty() ? user.getUsername() : fullName;
        String initials = user.getInitials().isEmpty() ? "AD" : user.getInitials();

        if (sidebarUserName != null) sidebarUserName.setText(displayName);
        if (sidebarUserSub != null) sidebarUserSub.setText(user.getEmail());
        if (sidebarAvatar != null) sidebarAvatar.setText(initials);
    }

    private void loadContent(String fxmlPath) {
        try {
            String absolutePath = "/com/studyflow/" + fxmlPath;
            URL resource = getClass().getResource(absolutePath);
            if (resource == null) {
                System.err.println("FXML not found: " + absolutePath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent content = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);
        } catch (IOException e) {
            System.err.println("Failed to load: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private void showView(Button button, String fxmlPath) {
        setActiveButton(button);
        loadContent(fxmlPath);
    }

    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        activeButton = button;
        if (activeButton != null && !activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
        }
    }

    private void reloadActiveView() {
        if (activeButton == btnAdminDash) showDashboard();
        else if (activeButton == btnCourses) showCourses();
        else if (activeButton == btnPlanning) showPlanning();
        else if (activeButton == btnRevisions) showRevisions();
        else if (activeButton == btnProjects) showProjects();
        else if (activeButton == btnNotes) showNotes();
        else if (activeButton == btnStats) showStats();
        else if (activeButton == btnAdminUsers) showUsers();
        else if (activeButton == btnAdminAudit) showAuditLog();
        else if (activeButton == btnStressQuestions) showStressQuestions();
        else if (activeButton == btnRecommendations) showRecommendations();
        else if (activeButton == btnAdminAI) showAI();
        else showDashboard();
    }

    private boolean isAdminSearchView() {
        return activeButton == btnAdminDash
                || activeButton == btnAdminUsers
                || activeButton == btnAdminAudit
                || activeButton == btnStressQuestions
                || activeButton == btnRecommendations
                || activeButton == btnAdminAI;
    }

    @FXML private void showDashboard() { showView(btnAdminDash, "views/admin/AdminDashboard.fxml"); }
    @FXML private void showCourses() { showView(btnCourses, "views/admin/AdminMatieres.fxml"); }
    @FXML private void showPlanning() { showView(btnPlanning, "views/Planning.fxml"); }
    @FXML private void showRevisions() { showView(btnRevisions, "views/Revisions.fxml"); }
    @FXML private void showProjects() { showView(btnProjects, "views/admin/AdminProjects.fxml"); }
    @FXML private void showNotes() { showView(btnNotes, "views/Notes.fxml"); }
    @FXML private void showStats() { showView(btnStats, "views/Statistics.fxml"); }
    @FXML private void showUsers() { showView(btnAdminUsers, "views/admin/AdminUsers.fxml"); }
    @FXML private void showAuditLog() { showView(btnAdminAudit, "views/admin/AdminAuditLog.fxml"); }
    @FXML private void showStressQuestions() { showView(btnStressQuestions, "views/admin/AdminStressQuestions.fxml"); }
    @FXML private void showRecommendations() { showView(btnRecommendations, "views/admin/AdminRecommendations.fxml");
    }@FXML private void showAI() { showView(btnAdminAI, "views/admin/AdminAI.fxml"); }
    @FXML private void showProfile() {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        activeButton = null;
        loadContent("views/Profile.fxml");
    }

    @FXML
    private void onSearchSubmit() {
        String query = searchField.getText().trim();
        if (!query.isEmpty() && isAdminSearchView()) {
            showSearchResults(query);
        }
    }

    private void showSearchResults(String query) {
        List<User> results = serviceUser.searchUsers(query);

        VBox container = new VBox(16);
        container.setPadding(new Insets(24));

        Label title = new Label("Search Results - \"" + query + "\" (" + results.size() + " found)");
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

        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        contentArea.getChildren().clear();
        contentArea.getChildren().add(container);
    }

    private HBox buildSearchUserCard(User user) {
        HBox card = new HBox(16);
        card.getStyleClass().add("admin-user-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 20, 14, 20));

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("admin-user-avatar");
        if (user.isBanned()) {
            avatar.getStyleClass().add("admin-user-avatar-banned");
        }

        String initials = user.getInitials().isEmpty() ? "??" : user.getInitials();
        Label avatarLabel = new Label(initials);
        avatarLabel.getStyleClass().add("admin-user-avatar-text");
        avatar.getChildren().add(avatarLabel);

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(user.getFullName().trim());
        nameLabel.getStyleClass().add("admin-user-name");

        HBox details = new HBox(10);
        details.setAlignment(Pos.CENTER_LEFT);
        details.getChildren().add(buildPill("fth-mail", user.getEmail()));

        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            details.getChildren().add(buildPill("fth-at-sign", user.getUsername()));
        }

        if (user.getUniversity() != null && !user.getUniversity().isEmpty()) {
            details.getChildren().add(buildPill("fth-book-open", user.getUniversity()));
        }

        info.getChildren().addAll(nameLabel, details);

        Label statusBadge = new Label(user.isBanned() ? "Banned" : "Active");
        statusBadge.getStyleClass().add("admin-status-badge");
        statusBadge.getStyleClass().add(user.isBanned() ? "admin-status-banned" : "admin-status-active");

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
        if (!isMaximized) {
            Stage stage = App.getPrimaryStage();
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
