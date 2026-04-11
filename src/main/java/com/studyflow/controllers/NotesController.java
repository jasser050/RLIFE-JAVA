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
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Notes view
 */
public class NotesController implements Initializable {

    @FXML private Label totalNotesLabel;
    @FXML private Label foldersLabel;
    @FXML private Label favoritesLabel;
    @FXML private Label recentLabel;
    @FXML private VBox foldersBox;
    @FXML private FlowPane notesGrid;

    private record FolderData(String name, String icon, String color, int noteCount) {}
    private record NoteData(String title, String preview, String folder, String color, String date, boolean favorite) {}

    private List<FolderData> folders;
    private List<NoteData> notes;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadFolders();
        loadNotes();
        updateStats();
        displayFolders();
        displayNotes();
    }

    private void loadFolders() {
        folders = List.of(
            new FolderData("Data Structures", "fth-database", "primary", 12),
            new FolderData("Algorithms", "fth-git-branch", "success", 8),
            new FolderData("Database Systems", "fth-hard-drive", "warning", 15),
            new FolderData("Software Engineering", "fth-code", "accent", 6),
            new FolderData("Networks", "fth-wifi", "danger", 9),
            new FolderData("Machine Learning", "fth-cpu", "primary", 11)
        );
    }

    private void loadNotes() {
        notes = List.of(
            new NoteData("Binary Search Trees", "Binary search trees are a fundamental data structure that allows for efficient searching, insertion, and deletion operations...", "Data Structures", "primary", "Today", true),
            new NoteData("Graph Traversal Algorithms", "BFS and DFS are two fundamental ways to traverse or search through a graph. BFS uses a queue while DFS uses a stack...", "Algorithms", "success", "Today", true),
            new NoteData("SQL JOIN Operations", "Understanding different types of JOINs: INNER JOIN returns only matching rows, LEFT JOIN returns all rows from left table...", "Database Systems", "warning", "Yesterday", false),
            new NoteData("Design Patterns Overview", "Creational, Structural, and Behavioral patterns form the three main categories. Singleton ensures only one instance...", "Software Engineering", "accent", "Yesterday", true),
            new NoteData("TCP/IP Protocol Stack", "The TCP/IP model has 4 layers: Application, Transport, Internet, and Network Interface. Each layer has specific responsibilities...", "Networks", "danger", "Jan 22", false),
            new NoteData("Neural Network Basics", "Artificial neural networks consist of layers of interconnected nodes. The input layer receives data, hidden layers process it...", "Machine Learning", "primary", "Jan 22", true),
            new NoteData("Heap Data Structure", "A heap is a complete binary tree that satisfies the heap property. In a max-heap, parent nodes are always greater...", "Data Structures", "primary", "Jan 21", false),
            new NoteData("Sorting Algorithm Comparison", "QuickSort: O(n log n) average, O(n²) worst. MergeSort: O(n log n) always. HeapSort: O(n log n) always...", "Algorithms", "success", "Jan 21", false),
            new NoteData("Database Normalization", "1NF: Eliminate repeating groups. 2NF: Remove partial dependencies. 3NF: Remove transitive dependencies...", "Database Systems", "warning", "Jan 20", true),
            new NoteData("Agile Methodology", "Agile emphasizes iterative development, collaboration, and flexibility. Key practices include sprints, daily standups...", "Software Engineering", "accent", "Jan 20", false),
            new NoteData("HTTP Protocol", "HTTP is a stateless protocol. Common methods: GET, POST, PUT, DELETE. Status codes: 2xx success, 4xx client error...", "Networks", "danger", "Jan 19", false),
            new NoteData("Gradient Descent", "Optimization algorithm used to minimize the cost function. Learning rate determines step size. Batch vs Stochastic...", "Machine Learning", "primary", "Jan 19", false)
        );
    }

    private void updateStats() {
        totalNotesLabel.setText(String.valueOf(notes.size()));
        foldersLabel.setText(String.valueOf(folders.size()));
        long favorites = notes.stream().filter(NoteData::favorite).count();
        favoritesLabel.setText(String.valueOf(favorites));
        recentLabel.setText("7"); // Recent notes count
    }

    private void displayFolders() {
        foldersBox.getChildren().clear();

        // All Notes item
        HBox allItem = createFolderItem("All Notes", "fth-file-text", "#94A3B8", notes.size(), true);
        foldersBox.getChildren().add(allItem);

        // Favorites item
        long favCount = notes.stream().filter(NoteData::favorite).count();
        HBox favItem = createFolderItem("Favorites", "fth-star", "#FBBF24", (int) favCount, false);
        foldersBox.getChildren().add(favItem);

        // Separator
        Region separator = new Region();
        separator.setPrefHeight(1);
        separator.setStyle("-fx-background-color: #1E293B;");
        VBox.setMargin(separator, new Insets(8, 0, 8, 0));
        foldersBox.getChildren().add(separator);

        // Folder items
        for (FolderData folder : folders) {
            HBox item = createFolderItem(folder.name, folder.icon, getColorHex(folder.color), folder.noteCount, false);
            foldersBox.getChildren().add(item);
        }
    }

    private HBox createFolderItem(String name, String icon, String color, int count, boolean active) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 12, 10, 12));
        item.setStyle("-fx-background-color: " + (active ? "#1E293B" : "transparent") + "; -fx-background-radius: 8; -fx-cursor: hand;");

        FontIcon folderIcon = new FontIcon(icon);
        folderIcon.setIconSize(16);
        folderIcon.setIconColor(Color.web(color));

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: " + (active ? "#F8FAFC" : "#94A3B8") + "; -fx-font-size: 13px;");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Label countLabel = new Label(String.valueOf(count));
        countLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        item.getChildren().addAll(folderIcon, nameLabel, countLabel);

        item.setOnMouseEntered(e -> {
            if (!active) item.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 8; -fx-cursor: hand;");
        });
        item.setOnMouseExited(e -> {
            if (!active) item.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand;");
        });

        return item;
    }

    private void displayNotes() {
        notesGrid.getChildren().clear();

        for (NoteData note : notes) {
            VBox card = createNoteCard(note);
            notesGrid.getChildren().add(card);
        }
    }

    private VBox createNoteCard(NoteData note) {
        VBox card = new VBox(12);
        card.setPrefWidth(280);
        card.setPrefHeight(180);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #0F172A; -fx-background-radius: 12; -fx-cursor: hand;");

        // Header with folder badge and favorite
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label folderBadge = new Label(note.folder);
        folderBadge.setStyle("-fx-background-color: " + getColorWithAlpha(note.color) +
                "; -fx-text-fill: " + getColorHex(note.color) +
                "; -fx-padding: 3 10; -fx-background-radius: 100; -fx-font-size: 10px; -fx-font-weight: 500;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (note.favorite) {
            FontIcon starIcon = new FontIcon("fth-star");
            starIcon.setIconSize(14);
            starIcon.setIconColor(Color.web("#FBBF24"));
            header.getChildren().addAll(folderBadge, spacer, starIcon);
        } else {
            header.getChildren().addAll(folderBadge, spacer);
        }

        // Title
        Label titleLabel = new Label(note.title);
        titleLabel.setStyle("-fx-text-fill: #F8FAFC; -fx-font-weight: 600; -fx-font-size: 14px;");
        titleLabel.setWrapText(true);

        // Preview
        Label previewLabel = new Label(note.preview);
        previewLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        previewLabel.setWrapText(true);
        previewLabel.setMaxHeight(48);
        VBox.setVgrow(previewLabel, Priority.ALWAYS);

        // Footer with date
        HBox footer = new HBox(6);
        footer.setAlignment(Pos.CENTER_LEFT);

        FontIcon clockIcon = new FontIcon("fth-clock");
        clockIcon.setIconSize(12);
        clockIcon.setIconColor(Color.web("#64748B"));

        Label dateLabel = new Label(note.date);
        dateLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        footer.getChildren().addAll(clockIcon, dateLabel);

        card.getChildren().addAll(header, titleLabel, previewLabel, footer);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 12; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #0F172A; -fx-background-radius: 12; -fx-cursor: hand;"));

        return card;
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
    private void showNewNoteDialog() {
        System.out.println("Show new note dialog");
    }
}
