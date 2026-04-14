package com.studyflow.controllers;

import com.studyflow.models.Course;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
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
 * Controller for the Courses view
 */
public class CoursesController implements Initializable {

    @FXML private FlowPane courseGrid;

    private List<Course> courses;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadCourses();
        displayCourses();
    }

    private void loadCourses() {
        // Sample data
        courses = List.of(
            new Course("1", "CS301", "Data Structures", "Dr. Smith", "MWF 9:00 AM", "primary", 0.68, 12, 5),
            new Course("2", "CS305", "Database Systems", "Prof. Johnson", "TTh 2:00 PM", "success", 0.45, 8, 4),
            new Course("3", "CS202", "Algorithms", "Dr. Williams", "MWF 11:00 AM", "warning", 0.82, 15, 6),
            new Course("4", "CS310", "Software Engineering", "Prof. Brown", "TTh 10:00 AM", "accent", 0.55, 10, 3),
            new Course("5", "CS401", "Machine Learning", "Dr. Davis", "MWF 1:00 PM", "danger", 0.30, 6, 2),
            new Course("6", "CS320", "Computer Networks", "Prof. Miller", "TTh 4:00 PM", "primary", 0.72, 9, 4)
        );
    }

    private void displayCourses() {
        courseGrid.getChildren().clear();

        for (Course course : courses) {
            VBox card = createCourseCard(course);
            courseGrid.getChildren().add(card);
        }
    }

    private VBox createCourseCard(Course course) {
        VBox card = new VBox(0);
        card.getStyleClass().add("card");
        card.setPrefWidth(340);
        card.setStyle("-fx-cursor: hand;");

        // Color header bar
        Region colorBar = new Region();
        colorBar.setPrefHeight(4);
        colorBar.setStyle("-fx-background-color: " + getGradientForColor(course.getColor()) +
                "; -fx-background-radius: 16 16 0 0;");

        // Card content
        VBox content = new VBox(16);
        content.setPadding(new Insets(20));

        // Header with icon and badge
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(48, 48);
        iconBox.getStyleClass().addAll("stat-icon-box", course.getColor());

        FontIcon icon = new FontIcon("fth-book-open");
        icon.setIconSize(22);
        icon.setIconColor(Color.web(getColorHex(course.getColor())));
        iconBox.getChildren().add(icon);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label(course.getCode());
        badge.getStyleClass().addAll("badge", course.getColor());

        header.getChildren().addAll(iconBox, spacer, badge);

        // Course name and instructor
        VBox info = new VBox(4);
        Label nameLabel = new Label(course.getName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #F8FAFC;");

        HBox instructorRow = new HBox(6);
        instructorRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon userIcon = new FontIcon("fth-user");
        userIcon.setIconSize(12);
        userIcon.setIconColor(Color.web("#94A3B8"));
        Label instructorLabel = new Label(course.getInstructor());
        instructorLabel.getStyleClass().add("text-small");
        instructorRow.getChildren().addAll(userIcon, instructorLabel);

        info.getChildren().addAll(nameLabel, instructorRow);

        // Schedule
        HBox scheduleRow = new HBox(6);
        scheduleRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon clockIcon = new FontIcon("fth-clock");
        clockIcon.setIconSize(12);
        clockIcon.setIconColor(Color.web("#94A3B8"));
        Label scheduleLabel = new Label(course.getSchedule());
        scheduleLabel.getStyleClass().add("text-small");
        scheduleRow.getChildren().addAll(clockIcon, scheduleLabel);

        // Stats badges
        HBox stats = new HBox(12);
        stats.setAlignment(Pos.CENTER_LEFT);

        HBox notesBox = new HBox(6);
        notesBox.setAlignment(Pos.CENTER_LEFT);
        notesBox.setStyle("-fx-background-color: #1E293B; -fx-padding: 4 10; -fx-background-radius: 8;");
        FontIcon notesIcon = new FontIcon("fth-file-text");
        notesIcon.setIconSize(12);
        notesIcon.setIconColor(Color.web("#94A3B8"));
        Label notesLabel = new Label(course.getNotesCount() + " notes");
        notesLabel.getStyleClass().add("text-small");
        notesBox.getChildren().addAll(notesIcon, notesLabel);

        HBox assignmentsBox = new HBox(6);
        assignmentsBox.setAlignment(Pos.CENTER_LEFT);
        assignmentsBox.setStyle("-fx-background-color: #1E293B; -fx-padding: 4 10; -fx-background-radius: 8;");
        FontIcon assignIcon = new FontIcon("fth-clipboard");
        assignIcon.setIconSize(12);
        assignIcon.setIconColor(Color.web("#94A3B8"));
        Label assignLabel = new Label(course.getAssignmentsCount() + " tasks");
        assignLabel.getStyleClass().add("text-small");
        assignmentsBox.getChildren().addAll(assignIcon, assignLabel);

        stats.getChildren().addAll(notesBox, assignmentsBox);

        // Progress bar
        VBox progressSection = new VBox(8);
        HBox progressHeader = new HBox();
        progressHeader.setAlignment(Pos.CENTER_LEFT);
        Label progressTitle = new Label("Progress");
        progressTitle.getStyleClass().add("text-small");
        Region progressSpacer = new Region();
        HBox.setHgrow(progressSpacer, Priority.ALWAYS);
        Label progressValue = new Label((int)(course.getProgress() * 100) + "%");
        progressValue.setStyle("-fx-font-weight: 600; -fx-text-fill: #F8FAFC;");
        progressHeader.getChildren().addAll(progressTitle, progressSpacer, progressValue);

        ProgressBar progressBar = new ProgressBar(course.getProgress());
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add(course.getColor());

        progressSection.getChildren().addAll(progressHeader, progressBar);

        content.getChildren().addAll(header, info, scheduleRow, stats, progressSection);

        card.getChildren().addAll(colorBar, content);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-cursor: hand; -fx-border-color: #334155;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-cursor: hand; -fx-border-color: #1E293B;"));

        return card;
    }

    private String getGradientForColor(String color) {
        return switch (color) {
            case "primary" -> "linear-gradient(to right, #8B5CF6, #A78BFA)";
            case "success" -> "linear-gradient(to right, #10B981, #34D399)";
            case "warning" -> "linear-gradient(to right, #F59E0B, #FBBF24)";
            case "accent" -> "linear-gradient(to right, #F97316, #FB923C)";
            case "danger" -> "linear-gradient(to right, #F43F5E, #FB7185)";
            default -> "linear-gradient(to right, #64748B, #94A3B8)";
        };
    }

    private String getColorHex(String color) {
        return switch (color) {
            case "primary" -> "#A78BFA";
            case "success" -> "#34D399";
            case "warning" -> "#FBBF24";
            case "accent" -> "#FB923C";
            case "danger" -> "#FB7185";
            default -> "#94A3B8";
        };
    }
}
