package com.studyflow.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalTime;
import java.util.ResourceBundle;

/**
 * Controller for the Dashboard view
 * Manages charts, stats, and dynamic content
 */
public class DashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label streakLabel;
    @FXML private Label assignmentsDueLabel;
    @FXML private Label completedLabel;
    @FXML private Label studyHoursLabel;
    @FXML private Label cardsReviewedLabel;

    @FXML private AreaChart<String, Number> studyTimeChart;
    @FXML private BarChart<String, Number> weeklyProgressChart;

    @FXML private VBox scheduleList;
    @FXML private VBox tasksList;
    @FXML private VBox activityList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupWelcomeMessage();
        setupStats();
        setupStudyTimeChart();
        setupWeeklyProgressChart();
        setupScheduleList();
        setupTasksList();
        setupActivityList();
    }

    private void setupWelcomeMessage() {
        int hour = LocalTime.now().getHour();
        String greeting;
        if (hour < 12) {
            greeting = "Good morning";
        } else if (hour < 17) {
            greeting = "Good afternoon";
        } else {
            greeting = "Good evening";
        }
        welcomeLabel.setText(greeting + ", Jasser");
    }

    private void setupStats() {
        streakLabel.setText("7");
        assignmentsDueLabel.setText("5");
        completedLabel.setText("12");
        studyHoursLabel.setText("24");
        cardsReviewedLabel.setText("48");
    }

    private void setupStudyTimeChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Study Hours");

        series.getData().add(new XYChart.Data<>("Mon", 2.5));
        series.getData().add(new XYChart.Data<>("Tue", 3.2));
        series.getData().add(new XYChart.Data<>("Wed", 4.1));
        series.getData().add(new XYChart.Data<>("Thu", 2.8));
        series.getData().add(new XYChart.Data<>("Fri", 3.5));
        series.getData().add(new XYChart.Data<>("Sat", 5.0));
        series.getData().add(new XYChart.Data<>("Sun", 4.2));

        studyTimeChart.getData().add(series);
        studyTimeChart.setAnimated(true);
    }

    private void setupWeeklyProgressChart() {
        XYChart.Series<String, Number> completedSeries = new XYChart.Series<>();
        completedSeries.setName("Completed");

        completedSeries.getData().add(new XYChart.Data<>("Mon", 3));
        completedSeries.getData().add(new XYChart.Data<>("Tue", 5));
        completedSeries.getData().add(new XYChart.Data<>("Wed", 4));
        completedSeries.getData().add(new XYChart.Data<>("Thu", 6));
        completedSeries.getData().add(new XYChart.Data<>("Fri", 2));
        completedSeries.getData().add(new XYChart.Data<>("Sat", 4));
        completedSeries.getData().add(new XYChart.Data<>("Sun", 3));

        XYChart.Series<String, Number> pendingSeries = new XYChart.Series<>();
        pendingSeries.setName("Pending");

        pendingSeries.getData().add(new XYChart.Data<>("Mon", 2));
        pendingSeries.getData().add(new XYChart.Data<>("Tue", 1));
        pendingSeries.getData().add(new XYChart.Data<>("Wed", 3));
        pendingSeries.getData().add(new XYChart.Data<>("Thu", 1));
        pendingSeries.getData().add(new XYChart.Data<>("Fri", 4));
        pendingSeries.getData().add(new XYChart.Data<>("Sat", 2));
        pendingSeries.getData().add(new XYChart.Data<>("Sun", 2));

        weeklyProgressChart.getData().addAll(completedSeries, pendingSeries);
        weeklyProgressChart.setAnimated(true);
    }

    private void setupScheduleList() {
        scheduleList.getChildren().clear();

        addScheduleItem("Data Structures Lecture", "09:00 AM", "Room 301", "primary");
        addScheduleItem("Algorithm Study Group", "11:30 AM", "Library", "success");
        addScheduleItem("Database Lab", "02:00 PM", "Lab 205", "warning");
        addScheduleItem("Project Meeting", "04:30 PM", "Online", "accent");
    }

    private void addScheduleItem(String title, String time, String location, String color) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12));
        item.setStyle("-fx-background-color: transparent; -fx-background-radius: 12; -fx-cursor: hand;");

        // Color indicator
        Region colorBar = new Region();
        colorBar.setPrefWidth(4);
        colorBar.setPrefHeight(48);
        colorBar.setStyle("-fx-background-color: " + getColorHex(color) + "; -fx-background-radius: 2;");

        // Content
        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("text-body");
        titleLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #F8FAFC;");

        HBox details = new HBox(8);
        details.setAlignment(Pos.CENTER_LEFT);

        FontIcon clockIcon = new FontIcon("fth-clock");
        clockIcon.setIconSize(12);
        clockIcon.setIconColor(Color.web("#94A3B8"));

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("text-small");

        Label separator = new Label("·");
        separator.setStyle("-fx-text-fill: #475569;");

        FontIcon locationIcon = new FontIcon("fth-map-pin");
        locationIcon.setIconSize(12);
        locationIcon.setIconColor(Color.web("#94A3B8"));

        Label locationLabel = new Label(location);
        locationLabel.getStyleClass().add("text-small");

        details.getChildren().addAll(clockIcon, timeLabel, separator, locationIcon, locationLabel);
        content.getChildren().addAll(titleLabel, details);

        // Badge
        Label badge = new Label(color.toUpperCase().substring(0, 3));
        badge.getStyleClass().addAll("badge", color);
        badge.setStyle("-fx-font-size: 10px;");

        item.getChildren().addAll(colorBar, content, badge);

        // Hover effect
        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 12; -fx-cursor: hand;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-background-color: transparent; -fx-background-radius: 12; -fx-cursor: hand;"));

        scheduleList.getChildren().add(item);
    }

    private void setupTasksList() {
        tasksList.getChildren().clear();

        addTaskItem("Complete Algorithm Assignment", "CS301", "high", "Jan 26");
        addTaskItem("Database ER Diagram", "CS305", "medium", "Jan 27");
        addTaskItem("Read Chapter 5", "CS202", "low", "Jan 28");
        addTaskItem("Submit Lab Report", "CS310", "high", "Jan 29");
    }

    private void addTaskItem(String title, String course, String priority, String dueDate) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12));
        item.setStyle("-fx-background-color: transparent; -fx-background-radius: 12; -fx-cursor: hand;");

        // Checkbox circle using Region for proper rendering
        Region checkbox = new Region();
        checkbox.setMinSize(20, 20);
        checkbox.setPrefSize(20, 20);
        checkbox.setMaxSize(20, 20);
        checkbox.setStyle("-fx-background-color: transparent; -fx-border-color: #475569; -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10;");

        // Content
        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("text-body");
        titleLabel.setStyle("-fx-font-weight: 500; -fx-text-fill: #F8FAFC;");

        Label courseLabel = new Label(course);
        courseLabel.getStyleClass().add("text-small");

        content.getChildren().addAll(titleLabel, courseLabel);

        // Right side
        VBox right = new VBox(4);
        right.setAlignment(Pos.CENTER_RIGHT);

        String priorityColor = priority.equals("high") ? "danger" : (priority.equals("medium") ? "warning" : "success");
        Label priorityBadge = new Label(priority.toUpperCase());
        priorityBadge.getStyleClass().addAll("badge", priorityColor);
        priorityBadge.setStyle("-fx-font-size: 10px;");

        Label dueDateLabel = new Label(dueDate);
        dueDateLabel.getStyleClass().add("text-small");

        right.getChildren().addAll(priorityBadge, dueDateLabel);

        item.getChildren().addAll(checkbox, content, right);

        // Hover effect
        item.setOnMouseEntered(e -> {
            item.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 12; -fx-cursor: hand;");
            checkbox.setStyle("-fx-background-color: transparent; -fx-border-color: #A78BFA; -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10;");
        });
        item.setOnMouseExited(e -> {
            item.setStyle("-fx-background-color: transparent; -fx-background-radius: 12; -fx-cursor: hand;");
            checkbox.setStyle("-fx-background-color: transparent; -fx-border-color: #475569; -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10;");
        });

        tasksList.getChildren().add(item);
    }

    private void setupActivityList() {
        activityList.getChildren().clear();

        addActivityItem("fth-check", "primary", "Completed Data Structures Assignment", "2 hours ago");
        addActivityItem("fth-layers", "warning", "Reviewed 15 flashcards in Algorithms", "4 hours ago");
        addActivityItem("fth-zap", "accent", "7 day study streak achieved!", "Yesterday");
        addActivityItem("fth-file-text", "success", "Added new notes for Database Systems", "Yesterday");
    }

    private void addActivityItem(String icon, String color, String message, String time) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.TOP_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(32, 32);
        iconBox.getStyleClass().addAll("stat-icon-box", color);
        iconBox.setStyle(iconBox.getStyle() + "-fx-pref-width: 32; -fx-pref-height: 32; -fx-min-width: 32; -fx-min-height: 32;");

        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(14);
        fontIcon.setIconColor(Color.web(getColorHex(color)));
        iconBox.getChildren().add(fontIcon);

        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("text-body");
        messageLabel.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 13px;");
        messageLabel.setWrapText(true);

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("text-small");
        timeLabel.setStyle("-fx-text-fill: #64748B;");

        content.getChildren().addAll(messageLabel, timeLabel);

        item.getChildren().addAll(iconBox, content);
        activityList.getChildren().add(item);
    }

    private String getColorHex(String color) {
        switch (color) {
            case "primary":
                return "#A78BFA";
            case "success":
                return "#34D399";
            case "warning":
                return "#FBBF24";
            case "danger":
                return "#FB7185";
            case "accent":
                return "#FB923C";
            default:
                return "#94A3B8";
        }
    }
}
