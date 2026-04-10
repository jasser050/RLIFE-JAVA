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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Wellbeing view
 */
public class WellbeingController implements Initializable {

    @FXML private Label moodLabel;
    @FXML private Label sleepLabel;
    @FXML private Label stressLabel;
    @FXML private Label energyLabel;
    @FXML private VBox moodWeekBox;
    @FXML private VBox habitsBox;
    @FXML private VBox mindfulnessBox;
    @FXML private VBox sleepLogBox;

    private record MoodEntry(String day, String mood, String icon, String color) {}
    private record HabitData(String name, String icon, String color, boolean[] weekProgress) {}
    private record MindfulnessSession(String title, String duration, String icon, String color) {}
    private record SleepEntry(String day, double hours, String quality) {}

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadMoodWeek();
        loadHabits();
        loadMindfulnessSessions();
        loadSleepLog();
    }

    private void loadMoodWeek() {
        moodWeekBox.getChildren().clear();

        List<MoodEntry> moods = List.of(
            new MoodEntry("Mon", "Great", "fth-smile", "success"),
            new MoodEntry("Tue", "Good", "fth-smile", "success"),
            new MoodEntry("Wed", "Okay", "fth-meh", "warning"),
            new MoodEntry("Thu", "Stressed", "fth-frown", "danger"),
            new MoodEntry("Fri", "Good", "fth-smile", "success"),
            new MoodEntry("Sat", "Great", "fth-smile", "success"),
            new MoodEntry("Sun", "Good", "fth-smile", "success")
        );

        HBox moodRow = new HBox(12);
        moodRow.setAlignment(Pos.CENTER);

        for (MoodEntry mood : moods) {
            VBox dayBox = createMoodDayBox(mood);
            HBox.setHgrow(dayBox, Priority.ALWAYS);
            moodRow.getChildren().add(dayBox);
        }

        moodWeekBox.getChildren().add(moodRow);
    }

    private VBox createMoodDayBox(MoodEntry mood) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 12;");

        Label dayLabel = new Label(mood.day);
        dayLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-weight: 600;");

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(40, 40);
        iconBox.setStyle("-fx-background-color: " + getColorWithAlpha(mood.color) + "; -fx-background-radius: 100;");
        FontIcon icon = new FontIcon(mood.icon);
        icon.setIconSize(20);
        icon.setIconColor(Color.web(getColorHex(mood.color)));
        iconBox.getChildren().add(icon);

        Label moodLabel = new Label(mood.mood);
        moodLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px;");

        box.getChildren().addAll(dayLabel, iconBox, moodLabel);
        return box;
    }

    private void loadHabits() {
        habitsBox.getChildren().clear();

        List<HabitData> habits = List.of(
            new HabitData("Morning Meditation", "fth-sun", "warning", new boolean[]{true, true, false, true, true, true, false}),
            new HabitData("Exercise", "fth-activity", "success", new boolean[]{true, false, true, false, true, true, false}),
            new HabitData("Read 30 mins", "fth-book", "primary", new boolean[]{true, true, true, true, true, false, true}),
            new HabitData("No Social Media", "fth-smartphone", "danger", new boolean[]{false, true, false, true, true, false, false}),
            new HabitData("8 Hours Sleep", "fth-moon", "accent", new boolean[]{true, true, true, false, true, true, true}),
            new HabitData("Drink Water", "fth-droplet", "primary", new boolean[]{true, true, true, true, true, true, true})
        );

        for (HabitData habit : habits) {
            HBox item = createHabitItem(habit);
            habitsBox.getChildren().add(item);
        }
    }

    private HBox createHabitItem(HabitData habit) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(14, 16, 14, 16));
        item.setStyle("-fx-border-color: #1E293B; -fx-border-width: 0 0 1 0;");

        // Icon
        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(32, 32);
        iconBox.setStyle("-fx-background-color: " + getColorWithAlpha(habit.color) + "; -fx-background-radius: 8;");
        FontIcon icon = new FontIcon(habit.icon);
        icon.setIconSize(14);
        icon.setIconColor(Color.web(getColorHex(habit.color)));
        iconBox.getChildren().add(icon);

        // Name
        Label nameLabel = new Label(habit.name);
        nameLabel.setStyle("-fx-text-fill: #F8FAFC; -fx-font-size: 13px;");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        // Week progress dots
        HBox dotsBox = new HBox(6);
        dotsBox.setAlignment(Pos.CENTER_RIGHT);
        String[] days = {"M", "T", "W", "T", "F", "S", "S"};
        for (int i = 0; i < 7; i++) {
            VBox dayDot = new VBox(2);
            dayDot.setAlignment(Pos.CENTER);

            Label dayLabel = new Label(days[i]);
            dayLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 9px;");

            Region dot = new Region();
            dot.setPrefSize(18, 18);
            if (habit.weekProgress[i]) {
                dot.setStyle("-fx-background-color: " + getColorHex(habit.color) + "; -fx-background-radius: 100;");
            } else {
                dot.setStyle("-fx-background-color: #334155; -fx-background-radius: 100;");
            }

            dayDot.getChildren().addAll(dayLabel, dot);
            dotsBox.getChildren().add(dayDot);
        }

        // Completion percentage
        long completed = 0;
        for (boolean b : habit.weekProgress) if (b) completed++;
        int percentage = (int)((completed / 7.0) * 100);

        Label percentLabel = new Label(percentage + "%");
        percentLabel.setStyle("-fx-text-fill: " + getColorHex(habit.color) + "; -fx-font-size: 12px; -fx-font-weight: 600;");
        percentLabel.setMinWidth(40);
        percentLabel.setAlignment(Pos.CENTER_RIGHT);

        item.getChildren().addAll(iconBox, nameLabel, dotsBox, percentLabel);
        return item;
    }

    private void loadMindfulnessSessions() {
        mindfulnessBox.getChildren().clear();

        List<MindfulnessSession> sessions = List.of(
            new MindfulnessSession("Deep Breathing", "5 min", "fth-wind", "primary"),
            new MindfulnessSession("Body Scan", "10 min", "fth-user", "success"),
            new MindfulnessSession("Focus Meditation", "15 min", "fth-target", "warning"),
            new MindfulnessSession("Sleep Meditation", "20 min", "fth-moon", "accent")
        );

        for (MindfulnessSession session : sessions) {
            HBox item = createMindfulnessItem(session);
            mindfulnessBox.getChildren().add(item);
        }
    }

    private HBox createMindfulnessItem(MindfulnessSession session) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12));
        item.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 10; -fx-cursor: hand;");

        // Icon
        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(40, 40);
        iconBox.setStyle("-fx-background-color: " + getColorWithAlpha(session.color) + "; -fx-background-radius: 10;");
        FontIcon icon = new FontIcon(session.icon);
        icon.setIconSize(18);
        icon.setIconColor(Color.web(getColorHex(session.color)));
        iconBox.getChildren().add(icon);

        // Content
        VBox content = new VBox(2);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label titleLabel = new Label(session.title);
        titleLabel.setStyle("-fx-text-fill: #F8FAFC; -fx-font-size: 13px; -fx-font-weight: 500;");

        Label durationLabel = new Label(session.duration);
        durationLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        content.getChildren().addAll(titleLabel, durationLabel);

        // Play button
        StackPane playBtn = new StackPane();
        playBtn.setPrefSize(32, 32);
        playBtn.setStyle("-fx-background-color: " + getColorHex(session.color) + "; -fx-background-radius: 100;");
        FontIcon playIcon = new FontIcon("fth-play");
        playIcon.setIconSize(14);
        playIcon.setIconColor(Color.WHITE);
        playBtn.getChildren().add(playIcon);

        item.getChildren().addAll(iconBox, content, playBtn);

        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #334155; -fx-background-radius: 10; -fx-cursor: hand;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 10; -fx-cursor: hand;"));

        return item;
    }

    private void loadSleepLog() {
        sleepLogBox.getChildren().clear();

        List<SleepEntry> sleepData = List.of(
            new SleepEntry("Today", 7.5, "good"),
            new SleepEntry("Yesterday", 6.0, "fair"),
            new SleepEntry("Mon", 8.0, "great"),
            new SleepEntry("Sun", 7.0, "good"),
            new SleepEntry("Sat", 9.0, "great"),
            new SleepEntry("Fri", 5.5, "poor"),
            new SleepEntry("Thu", 7.5, "good")
        );

        for (SleepEntry entry : sleepData) {
            HBox item = createSleepItem(entry);
            sleepLogBox.getChildren().add(item);
        }
    }

    private HBox createSleepItem(SleepEntry entry) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 0, 10, 0));
        item.setStyle("-fx-border-color: #1E293B; -fx-border-width: 0 0 1 0;");

        // Day
        Label dayLabel = new Label(entry.day);
        dayLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        dayLabel.setMinWidth(70);

        // Progress bar representing hours (max 10h)
        VBox barBox = new VBox(4);
        HBox.setHgrow(barBox, Priority.ALWAYS);

        ProgressBar bar = new ProgressBar(entry.hours / 10.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(8);
        String barColor = switch (entry.quality) {
            case "great" -> "success";
            case "good" -> "primary";
            case "fair" -> "warning";
            default -> "danger";
        };
        bar.getStyleClass().add(barColor);

        barBox.getChildren().add(bar);

        // Hours label
        Label hoursLabel = new Label(entry.hours + "h");
        hoursLabel.setStyle("-fx-text-fill: #F8FAFC; -fx-font-size: 13px; -fx-font-weight: 600;");
        hoursLabel.setMinWidth(40);
        hoursLabel.setAlignment(Pos.CENTER_RIGHT);

        // Quality indicator
        String qualityColor = switch (entry.quality) {
            case "great" -> "#10B981";
            case "good" -> "#8B5CF6";
            case "fair" -> "#F59E0B";
            default -> "#F43F5E";
        };

        Label qualityLabel = new Label(entry.quality.substring(0, 1).toUpperCase() + entry.quality.substring(1));
        qualityLabel.setStyle("-fx-background-color: " + qualityColor + "33; -fx-text-fill: " + qualityColor +
                "; -fx-padding: 2 8; -fx-background-radius: 100; -fx-font-size: 10px; -fx-font-weight: 600;");

        item.getChildren().addAll(dayLabel, barBox, hoursLabel, qualityLabel);
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

    @FXML
    private void showMoodDialog() {
        System.out.println("Show mood dialog");
    }
}
