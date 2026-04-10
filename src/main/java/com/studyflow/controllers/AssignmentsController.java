package com.studyflow.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Assignments view
 */
public class AssignmentsController implements Initializable {

    @FXML private Label overdueLabel;
    @FXML private Label dueSoonLabel;
    @FXML private Label inProgressLabel;
    @FXML private Label completedLabel;
    @FXML private Label taskCountLabel;
    @FXML private VBox assignmentsList;

    private record AssignmentData(String title, String course, String courseCode, String dueDate,
                                   String priority, String status, double progress) {}

    private List<AssignmentData> assignments;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadAssignments();
        updateStats();
        displayAssignments();
    }

    private void loadAssignments() {
        assignments = List.of(
            new AssignmentData("Complete Algorithm Analysis Report", "Data Structures", "CS301", "Jan 20", "high", "overdue", 0.75),
            new AssignmentData("Database ER Diagram Project", "Database Systems", "CS305", "Jan 22", "high", "overdue", 0.60),
            new AssignmentData("Binary Tree Implementation", "Data Structures", "CS301", "Jan 24", "high", "in_progress", 0.45),
            new AssignmentData("SQL Query Optimization Lab", "Database Systems", "CS305", "Jan 25", "medium", "in_progress", 0.30),
            new AssignmentData("Sorting Algorithms Comparison", "Algorithms", "CS202", "Jan 26", "high", "in_progress", 0.20),
            new AssignmentData("Network Protocol Analysis", "Computer Networks", "CS320", "Jan 27", "medium", "pending", 0.0),
            new AssignmentData("Software Design Patterns Essay", "Software Engineering", "CS310", "Jan 28", "low", "pending", 0.0),
            new AssignmentData("Machine Learning Dataset Prep", "Machine Learning", "CS401", "Jan 29", "medium", "pending", 0.0),
            new AssignmentData("Code Review Assignment", "Software Engineering", "CS310", "Jan 30", "low", "pending", 0.0),
            new AssignmentData("Graph Traversal Homework", "Algorithms", "CS202", "Feb 1", "medium", "pending", 0.0),
            new AssignmentData("Midterm Study Guide", "Data Structures", "CS301", "Jan 15", "high", "completed", 1.0),
            new AssignmentData("Database Normalization Quiz", "Database Systems", "CS305", "Jan 12", "medium", "completed", 1.0)
        );
    }

    private void updateStats() {
        long overdue = assignments.stream().filter(a -> a.status.equals("overdue")).count();
        long dueSoon = assignments.stream().filter(a -> a.status.equals("pending") || a.status.equals("in_progress")).count();
        long inProgress = assignments.stream().filter(a -> a.status.equals("in_progress")).count();
        long completed = assignments.stream().filter(a -> a.status.equals("completed")).count();

        overdueLabel.setText(String.valueOf(overdue));
        dueSoonLabel.setText(String.valueOf(dueSoon));
        inProgressLabel.setText(String.valueOf(inProgress));
        completedLabel.setText(String.valueOf(completed));
        taskCountLabel.setText(assignments.size() + " tasks");
    }

    private void displayAssignments() {
        assignmentsList.getChildren().clear();

        for (AssignmentData assignment : assignments) {
            HBox item = createAssignmentItem(assignment);
            assignmentsList.getChildren().add(item);
        }
    }

    private HBox createAssignmentItem(AssignmentData assignment) {
        HBox item = new HBox(16);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(16));
        item.setStyle("-fx-background-color: transparent; -fx-background-radius: 12; -fx-cursor: hand;");

        // Status indicator / checkbox
        StackPane statusBox = new StackPane();
        statusBox.setPrefSize(24, 24);
        statusBox.setMinSize(24, 24);
        statusBox.setMaxSize(24, 24);

        if (assignment.status.equals("completed")) {
            statusBox.setStyle("-fx-background-color: #10B981; -fx-background-radius: 12;");
            FontIcon checkIcon = new FontIcon("fth-check");
            checkIcon.setIconSize(14);
            checkIcon.setIconColor(Color.WHITE);
            statusBox.getChildren().add(checkIcon);
        } else if (assignment.status.equals("in_progress")) {
            statusBox.setStyle("-fx-background-color: transparent; -fx-border-color: #8B5CF6; -fx-border-width: 2; -fx-background-radius: 12; -fx-border-radius: 12;");
        } else if (assignment.status.equals("overdue")) {
            statusBox.setStyle("-fx-background-color: transparent; -fx-border-color: #F43F5E; -fx-border-width: 2; -fx-background-radius: 12; -fx-border-radius: 12;");
        } else {
            statusBox.setStyle("-fx-background-color: transparent; -fx-border-color: #475569; -fx-border-width: 2; -fx-background-radius: 12; -fx-border-radius: 12;");
        }

        // Main content
        VBox content = new VBox(6);
        HBox.setHgrow(content, Priority.ALWAYS);

        // Title row
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(assignment.title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: " +
                (assignment.status.equals("completed") ? "#64748B;" : "#F8FAFC;") +
                (assignment.status.equals("completed") ? " -fx-strikethrough: true;" : ""));

        if (assignment.status.equals("overdue")) {
            Label overdueTag = new Label("OVERDUE");
            overdueTag.setStyle("-fx-background-color: rgba(244, 63, 94, 0.2); -fx-text-fill: #FB7185; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
            titleRow.getChildren().addAll(titleLabel, overdueTag);
        } else {
            titleRow.getChildren().add(titleLabel);
        }

        // Course info row
        HBox infoRow = new HBox(12);
        infoRow.setAlignment(Pos.CENTER_LEFT);

        Label courseLabel = new Label(assignment.course);
        courseLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");

        Label separator = new Label("·");
        separator.setStyle("-fx-text-fill: #475569;");

        HBox dueDateBox = new HBox(4);
        dueDateBox.setAlignment(Pos.CENTER_LEFT);
        FontIcon calIcon = new FontIcon("fth-calendar");
        calIcon.setIconSize(12);
        calIcon.setIconColor(Color.web("#94A3B8"));
        Label dueDateLabel = new Label(assignment.dueDate);
        dueDateLabel.setStyle("-fx-text-fill: " + (assignment.status.equals("overdue") ? "#FB7185;" : "#94A3B8;") + " -fx-font-size: 12px;");
        dueDateBox.getChildren().addAll(calIcon, dueDateLabel);

        infoRow.getChildren().addAll(courseLabel, separator, dueDateBox);

        // Progress bar (only for in-progress items)
        if (assignment.status.equals("in_progress") && assignment.progress > 0) {
            HBox progressRow = new HBox(8);
            progressRow.setAlignment(Pos.CENTER_LEFT);

            ProgressBar progressBar = new ProgressBar(assignment.progress);
            progressBar.setPrefWidth(120);
            progressBar.setPrefHeight(4);
            progressBar.getStyleClass().add("primary");

            Label progressLabel = new Label((int)(assignment.progress * 100) + "%");
            progressLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");

            progressRow.getChildren().addAll(progressBar, progressLabel);
            content.getChildren().addAll(titleRow, infoRow, progressRow);
        } else {
            content.getChildren().addAll(titleRow, infoRow);
        }

        // Right side - priority badge and course code
        VBox rightSide = new VBox(8);
        rightSide.setAlignment(Pos.CENTER_RIGHT);

        String priorityColor = switch (assignment.priority) {
            case "high" -> "danger";
            case "medium" -> "warning";
            default -> "success";
        };

        Label priorityBadge = new Label(assignment.priority.toUpperCase());
        priorityBadge.getStyleClass().addAll("badge", priorityColor);
        priorityBadge.setStyle("-fx-font-size: 10px;");

        Label codeLabel = new Label(assignment.courseCode);
        codeLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-weight: 600;");

        rightSide.getChildren().addAll(priorityBadge, codeLabel);

        item.getChildren().addAll(statusBox, content, rightSide);

        // Hover effect
        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 12; -fx-cursor: hand;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-background-color: transparent; -fx-background-radius: 12; -fx-cursor: hand;"));

        return item;
    }

    @FXML
    private void showNewAssignmentDialog() {
        // Dialog would be shown here
        System.out.println("Show new assignment dialog");
    }
}
