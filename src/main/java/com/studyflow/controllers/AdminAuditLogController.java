package com.studyflow.controllers;

import com.studyflow.services.ServiceUser;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AdminAuditLogController implements Initializable {

    @FXML private VBox logCardsContainer;
    @FXML private Label logCountLabel;

    private final ServiceUser serviceUser = new ServiceUser();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadLog();
    }

    private void loadLog() {
        logCardsContainer.getChildren().clear();
        // log entries: [id, email, name, action, date]
        List<String[]> log = serviceUser.getAuditLog();
        logCountLabel.setText(log.size() + " entries");

        for (String[] entry : log) {
            logCardsContainer.getChildren().add(buildLogCard(entry));
        }
    }

    private HBox buildLogCard(String[] entry) {
        String email = entry[1];
        String name = entry[2];
        String action = entry[3];
        String date = entry[4];
        boolean isBanned = action.startsWith("BANNED");

        HBox card = new HBox(14);
        card.getStyleClass().add("admin-log-card");
        if (isBanned) card.getStyleClass().add("admin-log-card-banned");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 18, 14, 18));

        // Action icon
        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("admin-log-icon");
        iconBox.getStyleClass().add(isBanned ? "admin-log-icon-danger" : "admin-log-icon-success");
        FontIcon icon = new FontIcon(isBanned ? "fth-shield-off" : "fth-user-plus");
        icon.setIconSize(16);
        icon.setIconColor(Color.WHITE);
        iconBox.getChildren().add(icon);

        // Info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox nameRow = new HBox(10);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("admin-log-name");

        HBox emailPill = new HBox(4);
        emailPill.setAlignment(Pos.CENTER_LEFT);
        emailPill.getStyleClass().add("admin-user-pill");
        FontIcon mailIcon = new FontIcon("fth-mail");
        mailIcon.setIconSize(11);
        mailIcon.setIconColor(Color.web("#64748B"));
        Label emailLabel = new Label(email);
        emailLabel.getStyleClass().add("admin-user-detail");
        emailLabel.setStyle("-fx-font-size: 11px;");
        emailPill.getChildren().addAll(mailIcon, emailLabel);

        nameRow.getChildren().addAll(nameLabel, emailPill);

        // Action label
        Label actionLabel = new Label(action);
        actionLabel.getStyleClass().add("admin-log-action");
        if (isBanned) {
            actionLabel.setStyle("-fx-text-fill: #F43F5E; -fx-font-weight: bold;");
        } else {
            actionLabel.setStyle("-fx-text-fill: #34D399;");
        }

        info.getChildren().addAll(nameRow, actionLabel);

        // Date
        VBox dateBox = new VBox(2);
        dateBox.setAlignment(Pos.CENTER_RIGHT);
        FontIcon clockIcon = new FontIcon("fth-clock");
        clockIcon.setIconSize(13);
        clockIcon.setIconColor(Color.web("#475569"));
        Label dateLabel = new Label(date);
        dateLabel.getStyleClass().add("admin-log-date");
        dateBox.getChildren().addAll(clockIcon, dateLabel);

        card.getChildren().addAll(iconBox, info, dateBox);
        return card;
    }

    @FXML
    private void refresh() { loadLog(); }
}
