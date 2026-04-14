package com.studyflow.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.*;
import java.util.ResourceBundle;

/**
 * Controller for the Projects/Kanban view
 */
public class ProjectsController implements Initializable {

    @FXML private Label activeProjectsLabel;
    @FXML private Label teamMembersLabel;
    @FXML private Label totalTasksLabel;
    @FXML private Label completedTasksLabel;
    @FXML private HBox projectTabsBox;
    @FXML private HBox kanbanBoard;

    private record TaskData(String title, String description, String priority, String assignee, String dueDate, List<String> tags) {}
    private record ProjectData(String name, String color, Map<String, List<TaskData>> columns) {}

    private List<ProjectData> projects;
    private int selectedProjectIndex = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadProjects();
        updateStats();
        displayProjectTabs();
        displayKanbanBoard();
    }

    private void loadProjects() {
        // Database Project
        Map<String, List<TaskData>> dbColumns = new LinkedHashMap<>();
        dbColumns.put("To Do", List.of(
            new TaskData("Design ER Diagram", "Create entity-relationship diagram for the database schema", "high", "Alice", "Jan 25", List.of("design", "schema")),
            new TaskData("Write SQL Queries", "Implement CRUD operations for all entities", "medium", "Bob", "Jan 27", List.of("sql", "backend")),
            new TaskData("Documentation", "Document database structure and relationships", "low", "Carol", "Jan 30", List.of("docs"))
        ));
        dbColumns.put("In Progress", List.of(
            new TaskData("Normalization Review", "Review and apply 3NF normalization rules", "high", "Alice", "Jan 24", List.of("review", "schema")),
            new TaskData("Setup Test Data", "Create sample data for testing queries", "medium", "David", "Jan 26", List.of("testing"))
        ));
        dbColumns.put("Review", List.of(
            new TaskData("Index Optimization", "Add indexes for frequently queried columns", "medium", "Bob", "Jan 23", List.of("performance"))
        ));
        dbColumns.put("Done", List.of(
            new TaskData("Project Setup", "Initialize database and configure connections", "high", "David", "Jan 18", List.of("setup")),
            new TaskData("Requirements Analysis", "Gather and document project requirements", "medium", "Carol", "Jan 15", List.of("planning"))
        ));

        // Algorithm Project
        Map<String, List<TaskData>> algoColumns = new LinkedHashMap<>();
        algoColumns.put("To Do", List.of(
            new TaskData("Implement QuickSort", "Code and test quicksort algorithm", "high", "Eve", "Jan 28", List.of("algorithm", "sorting")),
            new TaskData("Complexity Analysis", "Analyze time and space complexity", "medium", "Frank", "Jan 29", List.of("analysis"))
        ));
        algoColumns.put("In Progress", List.of(
            new TaskData("BFS Implementation", "Implement breadth-first search for graphs", "high", "Eve", "Jan 25", List.of("algorithm", "graph")),
            new TaskData("Unit Tests", "Write comprehensive unit tests", "medium", "Grace", "Jan 26", List.of("testing"))
        ));
        algoColumns.put("Review", List.of(
            new TaskData("DFS Algorithm", "Review depth-first search implementation", "medium", "Frank", "Jan 24", List.of("algorithm", "graph"))
        ));
        algoColumns.put("Done", List.of(
            new TaskData("Project Structure", "Set up project with proper file organization", "low", "Grace", "Jan 20", List.of("setup"))
        ));

        // ML Project
        Map<String, List<TaskData>> mlColumns = new LinkedHashMap<>();
        mlColumns.put("To Do", List.of(
            new TaskData("Data Preprocessing", "Clean and normalize the dataset", "high", "Henry", "Feb 1", List.of("data", "preprocessing")),
            new TaskData("Feature Engineering", "Extract and select relevant features", "high", "Ivy", "Feb 3", List.of("features"))
        ));
        mlColumns.put("In Progress", List.of(
            new TaskData("Model Selection", "Compare different ML algorithms", "high", "Henry", "Jan 28", List.of("model", "research"))
        ));
        mlColumns.put("Review", List.of());
        mlColumns.put("Done", List.of(
            new TaskData("Dataset Collection", "Gather training data from sources", "medium", "Ivy", "Jan 22", List.of("data"))
        ));

        projects = List.of(
            new ProjectData("Database Systems", "primary", dbColumns),
            new ProjectData("Algorithm Analysis", "success", algoColumns),
            new ProjectData("ML Capstone", "warning", mlColumns)
        );
    }

    private void updateStats() {
        activeProjectsLabel.setText(String.valueOf(projects.size()));
        teamMembersLabel.setText("8"); // Sample count

        int totalTasks = 0;
        int completedTasks = 0;
        for (ProjectData project : projects) {
            for (Map.Entry<String, List<TaskData>> entry : project.columns.entrySet()) {
                totalTasks += entry.getValue().size();
                if (entry.getKey().equals("Done")) {
                    completedTasks += entry.getValue().size();
                }
            }
        }
        totalTasksLabel.setText(String.valueOf(totalTasks));
        completedTasksLabel.setText(String.valueOf(completedTasks));
    }

    private void displayProjectTabs() {
        projectTabsBox.getChildren().clear();

        for (int i = 0; i < projects.size(); i++) {
            ProjectData project = projects.get(i);
            Button tab = new Button(project.name);
            tab.getStyleClass().add("filter-tab");
            if (i == selectedProjectIndex) {
                tab.getStyleClass().add("active");
            }
            int index = i;
            tab.setOnAction(e -> {
                selectedProjectIndex = index;
                displayProjectTabs();
                displayKanbanBoard();
            });
            projectTabsBox.getChildren().add(tab);
        }
    }

    private void displayKanbanBoard() {
        kanbanBoard.getChildren().clear();
        ProjectData project = projects.get(selectedProjectIndex);

        for (Map.Entry<String, List<TaskData>> entry : project.columns.entrySet()) {
            VBox column = createKanbanColumn(entry.getKey(), entry.getValue(), project.color);
            kanbanBoard.getChildren().add(column);
        }
    }

    private VBox createKanbanColumn(String title, List<TaskData> tasks, String projectColor) {
        VBox column = new VBox(12);
        column.setPrefWidth(300);
        column.setMinWidth(300);
        column.setPadding(new Insets(0));

        // Column header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 16, 12, 16));
        header.setStyle("-fx-background-color: #0F172A; -fx-background-radius: 12 12 0 0;");

        String columnColor = getColumnColor(title);
        Region colorDot = new Region();
        colorDot.setPrefSize(8, 8);
        colorDot.setStyle("-fx-background-color: " + columnColor + "; -fx-background-radius: 100;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #F8FAFC; -fx-font-weight: 600; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label countLabel = new Label(String.valueOf(tasks.size()));
        countLabel.setStyle("-fx-text-fill: #64748B; -fx-background-color: #1E293B; -fx-padding: 2 8; -fx-background-radius: 100; -fx-font-size: 12px;");

        Button addBtn = new Button();
        addBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        FontIcon addIcon = new FontIcon("fth-plus");
        addIcon.setIconSize(14);
        addIcon.setIconColor(Color.web("#64748B"));
        addBtn.setGraphic(addIcon);

        header.getChildren().addAll(colorDot, titleLabel, spacer, countLabel, addBtn);

        // Tasks container with scroll
        VBox tasksContainer = new VBox(8);
        tasksContainer.setPadding(new Insets(12));
        tasksContainer.setStyle("-fx-background-color: #0F172A;");

        for (TaskData task : tasks) {
            VBox taskCard = createTaskCard(task, projectColor);
            tasksContainer.getChildren().add(taskCard);
        }

        // Add placeholder for empty columns
        if (tasks.isEmpty()) {
            Label emptyLabel = new Label("No tasks");
            emptyLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");
            emptyLabel.setPadding(new Insets(20));
            tasksContainer.getChildren().add(emptyLabel);
        }

        ScrollPane scrollPane = new ScrollPane(tasksContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #0F172A; -fx-background: #0F172A; -fx-background-radius: 0 0 12 12;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        column.getChildren().addAll(header, scrollPane);
        return column;
    }

    private VBox createTaskCard(TaskData task, String projectColor) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 10; -fx-cursor: hand;");

        // Tags row
        if (!task.tags.isEmpty()) {
            HBox tagsRow = new HBox(6);
            for (String tag : task.tags) {
                Label tagLabel = new Label(tag);
                tagLabel.setStyle("-fx-background-color: " + getColorWithAlpha(projectColor) +
                        "; -fx-text-fill: " + getColorHex(projectColor) +
                        "; -fx-padding: 2 8; -fx-background-radius: 4; -fx-font-size: 10px;");
                tagsRow.getChildren().add(tagLabel);
            }
            card.getChildren().add(tagsRow);
        }

        // Title
        Label titleLabel = new Label(task.title);
        titleLabel.setStyle("-fx-text-fill: #F8FAFC; -fx-font-weight: 600; -fx-font-size: 13px;");
        titleLabel.setWrapText(true);
        card.getChildren().add(titleLabel);

        // Description
        if (task.description != null && !task.description.isEmpty()) {
            Label descLabel = new Label(task.description);
            descLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
            descLabel.setWrapText(true);
            descLabel.setMaxHeight(36);
            card.getChildren().add(descLabel);
        }

        // Footer with assignee and due date
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(6, 0, 0, 0));

        // Assignee avatar
        StackPane avatar = new StackPane();
        avatar.setPrefSize(24, 24);
        avatar.setStyle("-fx-background-color: " + getColorHex(projectColor) + "; -fx-background-radius: 100;");
        Label initial = new Label(task.assignee.substring(0, 1));
        initial.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 600;");
        avatar.getChildren().add(initial);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Due date
        HBox dueDateBox = new HBox(4);
        dueDateBox.setAlignment(Pos.CENTER_LEFT);
        FontIcon calIcon = new FontIcon("fth-calendar");
        calIcon.setIconSize(11);
        calIcon.setIconColor(Color.web("#64748B"));
        Label dueLabel = new Label(task.dueDate);
        dueLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        dueDateBox.getChildren().addAll(calIcon, dueLabel);

        // Priority indicator
        String priorityColor = switch (task.priority) {
            case "high" -> "#F43F5E";
            case "medium" -> "#F59E0B";
            default -> "#10B981";
        };
        Region priorityDot = new Region();
        priorityDot.setPrefSize(8, 8);
        priorityDot.setStyle("-fx-background-color: " + priorityColor + "; -fx-background-radius: 100;");

        footer.getChildren().addAll(avatar, spacer, dueDateBox, priorityDot);
        card.getChildren().add(footer);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #334155; -fx-background-radius: 10; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 10; -fx-cursor: hand;"));

        return card;
    }

    private String getColumnColor(String column) {
        return switch (column) {
            case "To Do" -> "#94A3B8";
            case "In Progress" -> "#8B5CF6";
            case "Review" -> "#F59E0B";
            case "Done" -> "#10B981";
            default -> "#64748B";
        };
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
    private void showNewProjectDialog() {
        System.out.println("Show new project dialog");
    }
}
