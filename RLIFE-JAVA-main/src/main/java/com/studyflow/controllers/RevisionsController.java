package com.studyflow.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Revisions/Flashcards view
 */
public class RevisionsController implements Initializable {

    @FXML private Label totalDecksLabel;
    @FXML private Label masteredLabel;
    @FXML private Label dueReviewLabel;
    @FXML private Label streakLabel;
    @FXML private FlowPane decksGrid;
    @FXML private VBox dueDecksBox;

    private record DeckData(String name, String course, String color, int totalCards, int mastered, int dueToday, String icon) {}

    private List<DeckData> decks;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadDecks();
        updateStats();
        displayDecks();
        displayDueDecks();
    }

    private void loadDecks() {
        decks = List.of(
            new DeckData("Data Structures Fundamentals", "CS301", "primary", 120, 85, 12, "fth-database"),
            new DeckData("Algorithm Complexity", "CS202", "success", 75, 60, 8, "fth-trending-up"),
            new DeckData("SQL Commands", "CS305", "warning", 90, 72, 15, "fth-terminal"),
            new DeckData("Design Patterns", "CS310", "accent", 45, 30, 5, "fth-grid"),
            new DeckData("Network Protocols", "CS320", "danger", 60, 35, 10, "fth-wifi"),
            new DeckData("Machine Learning Terms", "CS401", "primary", 80, 25, 20, "fth-cpu"),
            new DeckData("Operating Systems", "CS330", "success", 55, 40, 7, "fth-hard-drive"),
            new DeckData("Software Testing", "CS310", "warning", 40, 32, 3, "fth-check-square")
        );
    }

    private void updateStats() {
        totalDecksLabel.setText(String.valueOf(decks.size()));

        int totalMastered = decks.stream().mapToInt(DeckData::mastered).sum();
        masteredLabel.setText(String.valueOf(totalMastered));

        int totalDue = decks.stream().mapToInt(DeckData::dueToday).sum();
        dueReviewLabel.setText(String.valueOf(totalDue));

        streakLabel.setText("14"); // Sample streak
    }

    private void displayDecks() {
        decksGrid.getChildren().clear();

        for (DeckData deck : decks) {
            VBox card = createDeckCard(deck);
            decksGrid.getChildren().add(card);
        }
    }

    private VBox createDeckCard(DeckData deck) {
        VBox card = new VBox(12);
        card.setPrefWidth(240);
        card.setPadding(new Insets(0));
        card.setStyle("-fx-background-color: #0F172A; -fx-background-radius: 16; -fx-cursor: hand;");

        // Colored header with icon
        StackPane header = new StackPane();
        header.setPrefHeight(80);
        header.setStyle("-fx-background-color: " + getColorGradient(deck.color) + "; -fx-background-radius: 16 16 0 0;");

        FontIcon icon = new FontIcon(deck.icon);
        icon.setIconSize(32);
        icon.setIconColor(Color.WHITE);
        header.getChildren().add(icon);

        // Content
        VBox content = new VBox(8);
        content.setPadding(new Insets(16));

        Label nameLabel = new Label(deck.name);
        nameLabel.setStyle("-fx-text-fill: #F8FAFC; -fx-font-weight: 600; -fx-font-size: 14px;");
        nameLabel.setWrapText(true);

        Label courseLabel = new Label(deck.course);
        courseLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        // Progress section
        VBox progressSection = new VBox(6);
        progressSection.setPadding(new Insets(8, 0, 0, 0));

        double progress = (double) deck.mastered / deck.totalCards;

        HBox progressHeader = new HBox();
        progressHeader.setAlignment(Pos.CENTER_LEFT);
        Label progressText = new Label(deck.mastered + "/" + deck.totalCards + " mastered");
        progressText.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label percentLabel = new Label((int)(progress * 100) + "%");
        percentLabel.setStyle("-fx-text-fill: " + getColorHex(deck.color) + "; -fx-font-size: 11px; -fx-font-weight: 600;");
        progressHeader.getChildren().addAll(progressText, spacer, percentLabel);

        ProgressBar progressBar = new ProgressBar(progress);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(6);
        progressBar.getStyleClass().add(deck.color);

        progressSection.getChildren().addAll(progressHeader, progressBar);

        // Due badge
        if (deck.dueToday > 0) {
            HBox dueRow = new HBox(6);
            dueRow.setAlignment(Pos.CENTER_LEFT);
            dueRow.setPadding(new Insets(8, 0, 0, 0));

            FontIcon clockIcon = new FontIcon("fth-clock");
            clockIcon.setIconSize(12);
            clockIcon.setIconColor(Color.web("#FBBF24"));

            Label dueLabel = new Label(deck.dueToday + " cards due today");
            dueLabel.setStyle("-fx-text-fill: #FBBF24; -fx-font-size: 11px; -fx-font-weight: 500;");

            dueRow.getChildren().addAll(clockIcon, dueLabel);
            content.getChildren().addAll(nameLabel, courseLabel, progressSection, dueRow);
        } else {
            content.getChildren().addAll(nameLabel, courseLabel, progressSection);
        }

        card.getChildren().addAll(header, content);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 16; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #0F172A; -fx-background-radius: 16; -fx-cursor: hand;"));

        return card;
    }

    private void displayDueDecks() {
        dueDecksBox.getChildren().clear();

        List<DeckData> dueDecks = decks.stream()
                .filter(d -> d.dueToday > 0)
                .sorted((a, b) -> b.dueToday - a.dueToday)
                .limit(4)
                .toList();

        for (DeckData deck : dueDecks) {
            HBox item = createDueDeckItem(deck);
            dueDecksBox.getChildren().add(item);
        }
    }

    private HBox createDueDeckItem(DeckData deck) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 12, 10, 12));
        item.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 8; -fx-cursor: hand;");

        // Color indicator
        Region colorBar = new Region();
        colorBar.setPrefWidth(4);
        colorBar.setPrefHeight(32);
        colorBar.setStyle("-fx-background-color: " + getColorHex(deck.color) + "; -fx-background-radius: 2;");

        // Content
        VBox content = new VBox(2);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label nameLabel = new Label(deck.name);
        nameLabel.setStyle("-fx-text-fill: #F8FAFC; -fx-font-size: 12px; -fx-font-weight: 500;");
        nameLabel.setMaxWidth(160);

        Label dueLabel = new Label(deck.dueToday + " cards due");
        dueLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");

        content.getChildren().addAll(nameLabel, dueLabel);

        // Play button
        StackPane playBtn = new StackPane();
        playBtn.setPrefSize(28, 28);
        playBtn.setStyle("-fx-background-color: " + getColorWithAlpha(deck.color) + "; -fx-background-radius: 100;");
        FontIcon playIcon = new FontIcon("fth-play");
        playIcon.setIconSize(12);
        playIcon.setIconColor(Color.web(getColorHex(deck.color)));
        playBtn.getChildren().add(playIcon);

        item.getChildren().addAll(colorBar, content, playBtn);

        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #334155; -fx-background-radius: 8; -fx-cursor: hand;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 8; -fx-cursor: hand;"));

        return item;
    }

    private String getColorHex(String color) {
        return switch (color) {
            case "primary" -> "#A78BFA";
            case "success" -> "#34D399";
            case "warning" -> "#FBBF24";
            case "danger" -> "#FB7185";
            case "accent" -> "#FB923C";
            default -> "#94A3B8";
        };
    }

    private String getColorWithAlpha(String color) {
        return switch (color) {
            case "primary" -> "rgba(139, 92, 246, 0.2)";
            case "success" -> "rgba(16, 185, 129, 0.2)";
            case "warning" -> "rgba(245, 158, 11, 0.2)";
            case "danger" -> "rgba(244, 63, 94, 0.2)";
            case "accent" -> "rgba(249, 115, 22, 0.2)";
            default -> "rgba(148, 163, 184, 0.2)";
        };
    }

    private String getColorGradient(String color) {
        return switch (color) {
            case "primary" -> "linear-gradient(to bottom right, #7C3AED, #8B5CF6)";
            case "success" -> "linear-gradient(to bottom right, #059669, #10B981)";
            case "warning" -> "linear-gradient(to bottom right, #D97706, #F59E0B)";
            case "danger" -> "linear-gradient(to bottom right, #DC2626, #F43F5E)";
            case "accent" -> "linear-gradient(to bottom right, #EA580C, #F97316)";
            default -> "linear-gradient(to bottom right, #475569, #64748B)";
        };
    }

    @FXML
    private void showNewDeckDialog() {
        System.out.println("Show new deck dialog");
    }
}
