package com.studyflow.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for the Planning/Calendar view
 */
public class PlanningController implements Initializable {

    @FXML private Label currentMonthLabel;
    @FXML private Label todayDateLabel;
    @FXML private GridPane calendarGrid;
    @FXML private VBox todayEventsList;
    @FXML private VBox upcomingEventsList;

    private YearMonth currentYearMonth;
    private Map<Integer, List<EventData>> eventsByDay;

    private record EventData(String title, String time, String type, String color) {}

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentYearMonth = YearMonth.now();
        loadEvents();
        updateCalendar();
        updateTodayLabel();
        loadTodayEvents();
        loadUpcomingEvents();
    }

    private void loadEvents() {
        eventsByDay = new HashMap<>();

        // Sample events for the current month
        int today = LocalDate.now().getDayOfMonth();

        eventsByDay.put(today, List.of(
            new EventData("Data Structures Lecture", "09:00 AM", "class", "primary"),
            new EventData("Algorithm Study Group", "11:30 AM", "study", "success"),
            new EventData("Database Lab", "02:00 PM", "lab", "warning"),
            new EventData("Project Meeting", "04:30 PM", "meeting", "accent")
        ));

        eventsByDay.put(today + 1, List.of(
            new EventData("Machine Learning Class", "10:00 AM", "class", "primary"),
            new EventData("Office Hours", "03:00 PM", "meeting", "accent")
        ));

        eventsByDay.put(today + 2, List.of(
            new EventData("Software Eng. Lecture", "09:00 AM", "class", "primary"),
            new EventData("Team Standup", "11:00 AM", "meeting", "accent"),
            new EventData("Code Review", "02:00 PM", "work", "success")
        ));

        eventsByDay.put(today + 3, List.of(
            new EventData("Algorithms Midterm", "10:00 AM", "exam", "danger"),
            new EventData("Study Session", "06:00 PM", "study", "success")
        ));

        eventsByDay.put(today + 5, List.of(
            new EventData("Database Project Due", "11:59 PM", "deadline", "danger")
        ));

        eventsByDay.put(today - 2, List.of(
            new EventData("Networks Lecture", "09:00 AM", "class", "primary")
        ));

        eventsByDay.put(today - 4, List.of(
            new EventData("Quiz Prep", "04:00 PM", "study", "success")
        ));
    }

    private void updateCalendar() {
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();

        // Set up column constraints
        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setHgrow(Priority.ALWAYS);
            col.setFillWidth(true);
            calendarGrid.getColumnConstraints().add(col);
        }

        // Set up row constraints for 6 weeks
        for (int i = 0; i < 6; i++) {
            RowConstraints row = new RowConstraints();
            row.setVgrow(Priority.ALWAYS);
            row.setFillHeight(true);
            calendarGrid.getRowConstraints().add(row);
        }

        currentMonthLabel.setText(currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7; // Sunday = 0
        int daysInMonth = currentYearMonth.lengthOfMonth();
        int today = LocalDate.now().getDayOfMonth();
        boolean isCurrentMonth = currentYearMonth.equals(YearMonth.now());

        int row = 0;
        int col = dayOfWeek;

        for (int day = 1; day <= daysInMonth; day++) {
            VBox cell = createCalendarCell(day, isCurrentMonth && day == today);
            calendarGrid.add(cell, col, row);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }

        // Fill remaining cells with empty cells
        while (row < 6) {
            while (col <= 6) {
                VBox emptyCell = new VBox();
                emptyCell.setStyle("-fx-background-color: #0F172A;");
                calendarGrid.add(emptyCell, col, row);
                col++;
            }
            col = 0;
            row++;
        }
    }

    private VBox createCalendarCell(int day, boolean isToday) {
        VBox cell = new VBox(4);
        cell.setPadding(new Insets(8));
        cell.setStyle("-fx-background-color: #0F172A;");

        // Day number
        Label dayLabel = new Label(String.valueOf(day));
        if (isToday) {
            StackPane todayCircle = new StackPane();
            todayCircle.setPrefSize(28, 28);
            todayCircle.setMaxSize(28, 28);
            todayCircle.setStyle("-fx-background-color: #7C3AED; -fx-background-radius: 100;");
            dayLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 600; -fx-font-size: 13px;");
            todayCircle.getChildren().add(dayLabel);
            cell.getChildren().add(todayCircle);
        } else {
            dayLabel.setStyle("-fx-text-fill: #CBD5E1; -fx-font-weight: 500; -fx-font-size: 13px;");
            cell.getChildren().add(dayLabel);
        }

        // Events for this day
        List<EventData> events = eventsByDay.get(day);
        if (events != null) {
            int maxEvents = Math.min(events.size(), 3);
            for (int i = 0; i < maxEvents; i++) {
                EventData event = events.get(i);
                Label eventLabel = new Label(event.title);
                eventLabel.setMaxWidth(Double.MAX_VALUE);
                eventLabel.setStyle("-fx-background-color: " + getColorWithAlpha(event.color) +
                        "; -fx-text-fill: " + getColorHex(event.color) +
                        "; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px;");
                eventLabel.setWrapText(false);
                cell.getChildren().add(eventLabel);
            }
            if (events.size() > 3) {
                Label moreLabel = new Label("+" + (events.size() - 3) + " more");
                moreLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 10px;");
                cell.getChildren().add(moreLabel);
            }
        }

        // Hover effect
        cell.setOnMouseEntered(e -> cell.setStyle("-fx-background-color: #1E293B;"));
        cell.setOnMouseExited(e -> cell.setStyle("-fx-background-color: #0F172A;"));

        return cell;
    }

    private void updateTodayLabel() {
        todayDateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")));
    }

    private void loadTodayEvents() {
        todayEventsList.getChildren().clear();
        int today = LocalDate.now().getDayOfMonth();
        List<EventData> events = eventsByDay.get(today);

        if (events != null) {
            for (EventData event : events) {
                HBox eventItem = createEventItem(event);
                todayEventsList.getChildren().add(eventItem);
            }
        } else {
            Label noEvents = new Label("No events scheduled");
            noEvents.setStyle("-fx-text-fill: #64748B;");
            todayEventsList.getChildren().add(noEvents);
        }
    }

    private void loadUpcomingEvents() {
        upcomingEventsList.getChildren().clear();
        int today = LocalDate.now().getDayOfMonth();

        List<Map.Entry<Integer, List<EventData>>> upcoming = eventsByDay.entrySet().stream()
                .filter(e -> e.getKey() > today)
                .sorted(Map.Entry.comparingByKey())
                .limit(5)
                .toList();

        for (Map.Entry<Integer, List<EventData>> entry : upcoming) {
            LocalDate date = LocalDate.now().withDayOfMonth(entry.getKey());
            String dateStr = date.format(DateTimeFormatter.ofPattern("EEE, MMM d"));

            Label dateLabel = new Label(dateStr);
            dateLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 8 0 4 0;");
            upcomingEventsList.getChildren().add(dateLabel);

            for (EventData event : entry.getValue()) {
                HBox eventItem = createEventItem(event);
                upcomingEventsList.getChildren().add(eventItem);
            }
        }
    }

    private HBox createEventItem(EventData event) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 12, 10, 12));
        item.setStyle("-fx-background-color: transparent; -fx-background-radius: 10; -fx-cursor: hand;");

        // Color bar
        Region colorBar = new Region();
        colorBar.setPrefWidth(4);
        colorBar.setPrefHeight(36);
        colorBar.setStyle("-fx-background-color: " + getColorHex(event.color) + "; -fx-background-radius: 2;");

        // Content
        VBox content = new VBox(2);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label titleLabel = new Label(event.title);
        titleLabel.setStyle("-fx-text-fill: #F8FAFC; -fx-font-weight: 500; -fx-font-size: 13px;");

        HBox timeRow = new HBox(4);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon clockIcon = new FontIcon("fth-clock");
        clockIcon.setIconSize(11);
        clockIcon.setIconColor(Color.web("#94A3B8"));
        Label timeLabel = new Label(event.time);
        timeLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        timeRow.getChildren().addAll(clockIcon, timeLabel);

        content.getChildren().addAll(titleLabel, timeRow);

        // Type badge
        Label typeBadge = new Label(event.type.toUpperCase());
        typeBadge.setStyle("-fx-background-color: " + getColorWithAlpha(event.color) +
                "; -fx-text-fill: " + getColorHex(event.color) +
                "; -fx-padding: 2 8; -fx-background-radius: 100; -fx-font-size: 9px; -fx-font-weight: 600;");

        item.getChildren().addAll(colorBar, content, typeBadge);

        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 10; -fx-cursor: hand;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-background-color: transparent; -fx-background-radius: 10; -fx-cursor: hand;"));

        return item;
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

    private String getColorWithAlpha(String color) {
        switch (color) {
            case "primary":
                return "rgba(139, 92, 246, 0.2)";
            case "success":
                return "rgba(16, 185, 129, 0.2)";
            case "warning":
                return "rgba(245, 158, 11, 0.2)";
            case "danger":
                return "rgba(244, 63, 94, 0.2)";
            case "accent":
                return "rgba(249, 115, 22, 0.2)";
            default:
                return "rgba(148, 163, 184, 0.2)";
        }
    }
}
