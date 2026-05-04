package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Scene;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminUsersController implements Initializable {

    @FXML private VBox userCardsContainer;
    @FXML private Label userCountLabel;

    private final ServiceUser serviceUser = new ServiceUser();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadUsers();
    }

    private void loadUsers() {
        userCardsContainer.getChildren().clear();
        List<User> users = serviceUser.getAll();
        users.removeIf(u -> "admin@rlife.com".equalsIgnoreCase(u.getEmail()));
        userCountLabel.setText(users.size() + " users");

        for (User user : users) {
            userCardsContainer.getChildren().add(buildUserCard(user));
        }
    }

    private HBox buildUserCard(User user) {
        HBox card = new HBox(16);
        card.getStyleClass().add("admin-user-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 20));

        // Avatar circle
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("admin-user-avatar");
        String initials = user.getInitials().isEmpty() ? "??" : user.getInitials();
        Label avatarLabel = new Label(initials);
        avatarLabel.getStyleClass().add("admin-user-avatar-text");
        avatar.getChildren().add(avatarLabel);

        if (user.isBanned()) {
            avatar.getStyleClass().add("admin-user-avatar-banned");
        }

        // User info
        VBox info = new VBox(4);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(user.getFullName().trim());
        nameLabel.getStyleClass().add("admin-user-name");

        HBox details = new HBox(12);
        details.setAlignment(Pos.CENTER_LEFT);

        // Email pill
        HBox emailPill = new HBox(6);
        emailPill.setAlignment(Pos.CENTER_LEFT);
        emailPill.getStyleClass().add("admin-user-pill");
        FontIcon mailIcon = new FontIcon("fth-mail");
        mailIcon.setIconSize(12);
        mailIcon.setIconColor(Color.web("#64748B"));
        Label emailLabel = new Label(user.getEmail());
        emailLabel.getStyleClass().add("admin-user-detail");
        emailPill.getChildren().addAll(mailIcon, emailLabel);

        // Username pill
        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            HBox usernamePill = new HBox(6);
            usernamePill.setAlignment(Pos.CENTER_LEFT);
            usernamePill.getStyleClass().add("admin-user-pill");
            FontIcon userIcon = new FontIcon("fth-at-sign");
            userIcon.setIconSize(12);
            userIcon.setIconColor(Color.web("#64748B"));
            Label usernameLabel = new Label(user.getUsername());
            usernameLabel.getStyleClass().add("admin-user-detail");
            usernamePill.getChildren().addAll(userIcon, usernameLabel);
            details.getChildren().add(usernamePill);
        }
        details.getChildren().add(emailPill);

        // University pill
        if (user.getUniversity() != null && !user.getUniversity().isEmpty()) {
            HBox uniPill = new HBox(6);
            uniPill.setAlignment(Pos.CENTER_LEFT);
            uniPill.getStyleClass().add("admin-user-pill");
            FontIcon uniIcon = new FontIcon("fth-book-open");
            uniIcon.setIconSize(12);
            uniIcon.setIconColor(Color.web("#64748B"));
            Label uniLabel = new Label(user.getUniversity());
            uniLabel.getStyleClass().add("admin-user-detail");
            uniPill.getChildren().addAll(uniIcon, uniLabel);
            details.getChildren().add(uniPill);
        }

        info.getChildren().addAll(nameLabel, details);

        // Status badge
        Label statusBadge = new Label(user.isBanned() ? "Banned" : "Active");
        statusBadge.getStyleClass().add("admin-status-badge");
        statusBadge.getStyleClass().add(user.isBanned() ? "admin-status-banned" : "admin-status-active");

        // Ban reason tooltip
        if (user.isBanned() && user.getBanReason() != null && !user.getBanReason().isEmpty()) {
            Tooltip tooltip = new Tooltip("Reason: " + user.getBanReason());
            tooltip.setStyle("-fx-font-size: 12px;");
            Tooltip.install(statusBadge, tooltip);
        }

        // Actions
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        if (user.isBanned()) {
            Button unban = new Button("Unban");
            unban.getStyleClass().add("admin-action-btn");
            unban.getStyleClass().add("admin-action-unban");
            FontIcon icon = new FontIcon("fth-user-check");
            icon.setIconSize(14);
            icon.setIconColor(Color.web("#34D399"));
            unban.setGraphic(icon);
            unban.setOnAction(e -> handleUnban(user));
            actions.getChildren().add(unban);
        } else {
            Button ban = new Button("Ban");
            ban.getStyleClass().add("admin-action-btn");
            ban.getStyleClass().add("admin-action-ban");
            FontIcon icon = new FontIcon("fth-user-x");
            icon.setIconSize(14);
            icon.setIconColor(Color.web("#F43F5E"));
            ban.setGraphic(icon);
            ban.setOnAction(e -> showBanDialog(user));
            actions.getChildren().add(ban);
        }

        Button del = new Button();
        del.getStyleClass().add("admin-action-btn");
        del.getStyleClass().add("admin-action-delete");
        FontIcon trashIcon = new FontIcon("fth-trash-2");
        trashIcon.setIconSize(14);
        trashIcon.setIconColor(Color.web("#F43F5E"));
        del.setGraphic(trashIcon);
        del.setOnAction(e -> handleDelete(user));
        actions.getChildren().add(del);

        card.getChildren().addAll(avatar, info, statusBadge, actions);
        return card;
    }

    @FXML
    private void refreshUsers() { loadUsers(); }

    // ============================================
    // BAN DIALOG — Custom styled popup with reasons
    // ============================================

    private static final String[] BAN_REASONS = {
        "Spam or advertising",
        "Harassment or bullying",
        "Inappropriate content",
        "Impersonation",
        "Cheating or academic dishonesty",
        "Sharing copyrighted material",
        "Violating community guidelines",
        "Suspicious account activity",
        "Multiple account violations"
    };

    private void showBanDialog(User user) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setTitle("Ban User");

        VBox root = new VBox(0);
        root.getStyleClass().add("ban-dialog-root");
        root.setPrefWidth(480);
        root.setMaxWidth(480);

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("ban-dialog-header");
        header.setPadding(new Insets(20, 24, 16, 24));

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("ban-dialog-icon-box");
        FontIcon shieldIcon = new FontIcon("fth-shield-off");
        shieldIcon.setIconSize(22);
        shieldIcon.setIconColor(Color.WHITE);
        iconBox.getChildren().add(shieldIcon);

        VBox headerInfo = new VBox(4);
        Label title = new Label("Ban User");
        title.getStyleClass().add("ban-dialog-title");
        Label subtitle = new Label("Banning " + user.getFullName().trim() + " (" + user.getEmail() + ")");
        subtitle.getStyleClass().add("ban-dialog-subtitle");
        subtitle.setWrapText(true);
        headerInfo.getChildren().addAll(title, subtitle);
        HBox.setHgrow(headerInfo, Priority.ALWAYS);

        Button closeBtn = new Button();
        closeBtn.getStyleClass().add("ban-dialog-close");
        FontIcon closeIcon = new FontIcon("fth-x");
        closeIcon.setIconSize(16);
        closeIcon.setIconColor(Color.web("#64748B"));
        closeBtn.setGraphic(closeIcon);
        closeBtn.setOnAction(e -> dialog.close());

        header.getChildren().addAll(iconBox, headerInfo, closeBtn);

        // Body — reason selection
        VBox body = new VBox(10);
        body.setPadding(new Insets(8, 24, 16, 24));

        Label reasonLabel = new Label("Select a reason for banning:");
        reasonLabel.getStyleClass().add("ban-dialog-label");

        ToggleGroup reasonGroup = new ToggleGroup();
        VBox reasonsBox = new VBox(6);

        // Custom TextArea for "Other" reason
        TextArea customReasonField = new TextArea();
        customReasonField.setPromptText("Describe the reason for banning this user...");
        customReasonField.getStyleClass().add("ban-dialog-textarea");
        customReasonField.setPrefRowCount(3);
        customReasonField.setWrapText(true);
        customReasonField.setVisible(false);
        customReasonField.setManaged(false);

        for (String reason : BAN_REASONS) {
            RadioButton rb = new RadioButton(reason);
            rb.setToggleGroup(reasonGroup);
            rb.getStyleClass().add("ban-dialog-radio");
            rb.setOnAction(e -> {
                customReasonField.setVisible(false);
                customReasonField.setManaged(false);
            });
            reasonsBox.getChildren().add(rb);
        }

        // "Other" option
        RadioButton otherRb = new RadioButton("Other (specify below)");
        otherRb.setToggleGroup(reasonGroup);
        otherRb.getStyleClass().add("ban-dialog-radio");
        otherRb.setOnAction(e -> {
            customReasonField.setVisible(true);
            customReasonField.setManaged(true);
            customReasonField.requestFocus();
        });
        reasonsBox.getChildren().add(otherRb);

        // Select first by default
        ((RadioButton) reasonsBox.getChildren().get(0)).setSelected(true);

        ScrollPane scrollPane = new ScrollPane(reasonsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPrefHeight(240);
        scrollPane.getStyleClass().add("ban-dialog-scroll");

        body.getChildren().addAll(reasonLabel, scrollPane, customReasonField);

        // Footer
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 24, 20, 24));
        footer.getStyleClass().add("ban-dialog-footer");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("ban-dialog-cancel-btn");
        cancelBtn.setOnAction(e -> dialog.close());

        Button banBtn = new Button("Ban User");
        banBtn.getStyleClass().add("ban-dialog-ban-btn");
        FontIcon banIcon = new FontIcon("fth-user-x");
        banIcon.setIconSize(14);
        banIcon.setIconColor(Color.WHITE);
        banBtn.setGraphic(banIcon);
        banBtn.setOnAction(e -> {
            String reason;
            if (otherRb.isSelected()) {
                reason = customReasonField.getText().trim();
                if (reason.isEmpty()) {
                    customReasonField.setStyle("-fx-border-color: #F43F5E;");
                    return;
                }
            } else {
                Toggle selected = reasonGroup.getSelectedToggle();
                reason = selected != null ? ((RadioButton) selected).getText() : "Violation of terms";
            }
            serviceUser.banUser(user.getId(), reason);
            dialog.close();
            loadUsers();
        });

        footer.getChildren().addAll(cancelBtn, banBtn);

        root.getChildren().addAll(header, body, footer);

        // Respect current application theme for the dialog
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        String themeCss = getClass()
                .getResource(App.isDarkTheme()
                        ? "/com/studyflow/styles/dark-theme.css"
                        : "/com/studyflow/styles/light-theme.css")
                .toExternalForm();
        scene.getStylesheets().add(themeCss);

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void handleUnban(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Unban User");
        confirm.setHeaderText("Unban " + user.getFullName().trim() + "?");
        confirm.setContentText("This will restore the user's access to the platform.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceUser.unbanUser(user.getId());
            loadUsers();
        }
    }

    private void handleDelete(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText("Permanently delete " + user.getFullName().trim() + "?");
        confirm.setContentText("This action cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceUser.delete(user);
            loadUsers();
        }
    }
}
