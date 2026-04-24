package com.studyflow.controllers.admin;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

final class AdminViewFactory {

    private AdminViewFactory() {
    }

    static VBox createTimelineCard(String title, String subtitle, String badgeText) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "admin-list-card");
        card.setPadding(new Insets(16));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label(badgeText);
        badge.getStyleClass().addAll("badge", "accent");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("text-small");
        subtitleLabel.setWrapText(true);

        header.getChildren().addAll(titleLabel, spacer, badge);
        card.getChildren().addAll(header, subtitleLabel);
        return card;
    }

    static HBox createUserCard(String title, String subtitle, String role) {
        HBox row = new HBox(14);
        row.getStyleClass().addAll("card", "admin-user-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16));

        VBox avatar = new VBox();
        avatar.getStyleClass().add("admin-avatar");
        avatar.setAlignment(Pos.CENTER);
        avatar.setPrefSize(44, 44);
        Label initial = new Label(title != null && !title.isBlank() ? title.substring(0, 1).toUpperCase() : "?");
        initial.getStyleClass().add("admin-avatar-text");
        avatar.getChildren().add(initial);

        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);
        Label titleLabel = new Label(title == null || title.isBlank() ? "Unnamed User" : title);
        titleLabel.getStyleClass().add("card-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("text-small");
        content.getChildren().addAll(titleLabel, subtitleLabel);

        Label badge = new Label(role);
        badge.getStyleClass().addAll("badge", "primary");

        row.getChildren().addAll(avatar, content, badge);
        return row;
    }

    static VBox createSubjectCard(String title, String description, String metric, String accent) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("card", "admin-subject-card");
        card.setPadding(new Insets(18));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("text-small");
        descLabel.setWrapText(true);

        Label metricLabel = new Label(metric);
        metricLabel.getStyleClass().addAll("badge", accent);

        card.getChildren().addAll(titleLabel, descLabel, metricLabel);
        return card;
    }
}
