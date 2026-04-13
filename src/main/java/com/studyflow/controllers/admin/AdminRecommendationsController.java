package com.studyflow.controllers.admin;

import com.studyflow.models.RecommendationStress;
import com.studyflow.services.ServiceRecommendationStress;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class AdminRecommendationsController implements Initializable {

    @FXML private VBox listSection;
    @FXML private ScrollPane formSection;
    @FXML private ScrollPane detailSection;
    @FXML private Label lowCountLabel;
    @FXML private Label mediumCountLabel;
    @FXML private Label highCountLabel;
    @FXML private Label totalCountLabel;
    @FXML private VBox recommendationsListBox;
    @FXML private Label formTitleLabel;
    @FXML private Label formErrorLabel;
    @FXML private TextField titleField;
    @FXML private ComboBox<String> levelCombo;
    @FXML private TextArea contentArea;
    @FXML private CheckBox activeCheckBox;
    @FXML private Button saveButton;
    @FXML private Label detailTitleLabel;
    @FXML private Label detailCreatedLabel;
    @FXML private Label detailLevelLabel;
    @FXML private Label detailContentLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label detailUpdatedLabel;

    private ServiceRecommendationStress service;
    private RecommendationStress editingItem;
    private RecommendationStress selectedItem;
    private final DateTimeFormatter shortDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private final DateTimeFormatter longDateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        service = new ServiceRecommendationStress();
        levelCombo.setItems(FXCollections.observableArrayList("low", "medium", "high"));
        levelCombo.setValue("low");
        showListMode();
        loadRecommendations();
    }

    private void loadRecommendations() {
        try {
            List<RecommendationStress> items = service.findAll();
            updateStats(items);
            populateList(items);
        } catch (RuntimeException e) {
            recommendationsListBox.getChildren().setAll(createEmptyCard("Recommendations unavailable", e.getMessage() == null ? "Unexpected error while loading recommendations." : e.getMessage()));
            updateStats(List.of());
        }
    }

    private void updateStats(List<RecommendationStress> items) {
        lowCountLabel.setText(String.valueOf(items.stream().filter(i -> "low".equalsIgnoreCase(i.getLevel())).count()));
        mediumCountLabel.setText(String.valueOf(items.stream().filter(i -> "medium".equalsIgnoreCase(i.getLevel())).count()));
        highCountLabel.setText(String.valueOf(items.stream().filter(i -> "high".equalsIgnoreCase(i.getLevel())).count()));
        totalCountLabel.setText(String.valueOf(items.size()));
    }

    private void populateList(List<RecommendationStress> items) {
        recommendationsListBox.getChildren().clear();
        if (items.isEmpty()) {
            recommendationsListBox.getChildren().add(createEmptyCard("No recommendations yet", "Create your first stress recommendation for students."));
            return;
        }

        recommendationsListBox.getChildren().add(createHeaderRow());
        for (RecommendationStress item : items) {
            recommendationsListBox.getChildren().add(createRow(item));
        }
    }

    private Node createHeaderRow() {
        HBox header = new HBox(16);
        header.getStyleClass().add("admin-table-header");
        header.setPadding(new Insets(14, 16, 14, 16));
        header.getChildren().addAll(
                createColumnLabel("Title", 240),
                createColumnLabel("Level", 150),
                createColumnLabel("Preview", 0),
                createColumnLabel("Status", 120),
                createColumnLabel("Actions", 180)
        );
        HBox.setHgrow(header.getChildren().get(2), Priority.ALWAYS);
        return header;
    }

    private Node createRow(RecommendationStress item) {
        HBox row = new HBox(16);
        row.getStyleClass().add("admin-table-row");
        row.setPadding(new Insets(16));
        row.setAlignment(Pos.CENTER_LEFT);

        Label title = createCellLabel(item.getTitle(), 240);
        Label level = new Label(levelText(item.getLevel()));
        level.getStyleClass().addAll("badge", levelBadge(item.getLevel()));
        level.setMinWidth(150);
        level.setAlignment(Pos.CENTER);

        Label preview = new Label(item.getContent());
        preview.getStyleClass().add("admin-table-text");
        preview.setWrapText(true);
        preview.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(preview, Priority.ALWAYS);

        Label status = new Label(item.isActive() ? "Active" : "Inactive");
        status.getStyleClass().addAll("badge", item.isActive() ? "success" : "secondary");
        status.setMinWidth(120);
        status.setAlignment(Pos.CENTER);

        Button viewBtn = createActionButton("View", "btn-ghost");
        viewBtn.setOnAction(e -> showDetails(item));
        Button editBtn = createActionButton("Edit", "btn-secondary");
        editBtn.setOnAction(e -> startEdit(item));
        Button deleteBtn = createActionButton("Delete", "btn-danger");
        deleteBtn.setOnAction(e -> deleteRecommendation(item));
        HBox actions = new HBox(8, viewBtn, editBtn, deleteBtn);
        actions.setMinWidth(180);
        actions.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(title, level, preview, status, actions);
        return row;
    }

    @FXML
    private void handleAddRecommendation() {
        editingItem = null;
        formTitleLabel.setText("Create New Recommendation");
        saveButton.setText("Create Recommendation");
        titleField.clear();
        contentArea.clear();
        levelCombo.setValue("low");
        activeCheckBox.setSelected(true);
        hideFormError();
        showFormMode();
    }

    private void startEdit(RecommendationStress item) {
        editingItem = item;
        formTitleLabel.setText("Edit Recommendation");
        saveButton.setText("Update Recommendation");
        titleField.setText(item.getTitle());
        contentArea.setText(item.getContent());
        levelCombo.setValue(item.getLevel());
        activeCheckBox.setSelected(item.isActive());
        hideFormError();
        showFormMode();
    }

    private void showDetails(RecommendationStress item) {
        try {
            selectedItem = service.findById(item.getId());
            if (selectedItem == null) {
                showAlert(Alert.AlertType.WARNING, "Recommendation", "The selected recommendation could not be reloaded.");
                return;
            }
        } catch (RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Recommendation", e.getMessage() == null ? "Failed to load recommendation details." : e.getMessage());
            return;
        }

        detailTitleLabel.setText(selectedItem.getTitle());
        detailCreatedLabel.setText("Created on " + formatShortDate(selectedItem.getCreatedAt()));
        detailLevelLabel.setText(levelText(selectedItem.getLevel()));
        detailLevelLabel.getStyleClass().removeAll("success", "warning", "danger");
        detailLevelLabel.getStyleClass().add(levelBadge(selectedItem.getLevel()));
        detailContentLabel.setText(selectedItem.getContent());
        detailStatusLabel.setText(selectedItem.isActive() ? "Active" : "Inactive");
        detailStatusLabel.getStyleClass().removeAll("success", "secondary");
        detailStatusLabel.getStyleClass().add(selectedItem.isActive() ? "success" : "secondary");
        detailUpdatedLabel.setText(selectedItem.getUpdatedAt() == null ? "No updates yet." : "Last updated: " + selectedItem.getUpdatedAt().format(longDateFormatter));
        showDetailMode();
    }

    @FXML
    private void handleSaveRecommendation() {
        hideFormError();
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String content = contentArea.getText() == null ? "" : contentArea.getText().trim();
        String level = levelCombo.getValue();

        if (title.isBlank()) {
            showFormError("Title is required.");
            return;
        }
        if (content.isBlank()) {
            showFormError("Content is required.");
            return;
        }
        if (level == null || level.isBlank()) {
            showFormError("Stress level is required.");
            return;
        }

        RecommendationStress item = editingItem != null ? editingItem : new RecommendationStress();
        item.setTitle(title);
        item.setContent(content);
        item.setLevel(level);
        item.setActive(activeCheckBox.isSelected());

        try {
            if (editingItem == null) {
                item.setCreatedAt(LocalDateTime.now());
                service.add(item);
            } else {
                item.setUpdatedAt(LocalDateTime.now());
                service.update(item);
            }
            showListMode();
            loadRecommendations();
        } catch (RuntimeException e) {
            showFormError(e.getMessage() == null ? "Failed to save recommendation." : e.getMessage());
        }
    }

    private void deleteRecommendation(RecommendationStress item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Delete Recommendation");
        confirm.setContentText("Are you sure you want to delete this recommendation?");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    service.delete(item.getId());
                    loadRecommendations();
                    showListMode();
                } catch (RuntimeException e) {
                    showAlert(Alert.AlertType.ERROR, "Delete failed", e.getMessage() == null ? "Failed to delete recommendation." : e.getMessage());
                }
            }
        });
    }

    @FXML private void handleBackToList() { showListMode(); loadRecommendations(); }
    @FXML private void handleDetailEdit() { if (selectedItem != null) startEdit(selectedItem); }
    @FXML private void handleDetailDelete() { if (selectedItem != null) deleteRecommendation(selectedItem); }

    private void showListMode() {
        listSection.setVisible(true); listSection.setManaged(true);
        formSection.setVisible(false); formSection.setManaged(false);
        detailSection.setVisible(false); detailSection.setManaged(false);
    }

    private void showFormMode() {
        listSection.setVisible(false); listSection.setManaged(false);
        formSection.setVisible(true); formSection.setManaged(true);
        detailSection.setVisible(false); detailSection.setManaged(false);
    }

    private void showDetailMode() {
        listSection.setVisible(false); listSection.setManaged(false);
        formSection.setVisible(false); formSection.setManaged(false);
        detailSection.setVisible(true); detailSection.setManaged(true);
    }

    private String levelText(String level) {
        return switch (level == null ? "" : level.toLowerCase()) {
            case "medium" -> "Medium Stress";
            case "high" -> "High Stress";
            default -> "Low Stress";
        };
    }

    private String levelBadge(String level) {
        return switch (level == null ? "" : level.toLowerCase()) {
            case "medium" -> "warning";
            case "high" -> "danger";
            default -> "success";
        };
    }

    private String formatShortDate(LocalDateTime date) { return date == null ? "-" : date.format(shortDateFormatter); }

    private Label createColumnLabel(String text, double width) {
        Label label = new Label(text);
        label.getStyleClass().add("admin-table-head-label");
        if (width > 0) {
            label.setMinWidth(width);
            label.setPrefWidth(width);
        }
        return label;
    }

    private Label createCellLabel(String text, double width) {
        Label label = new Label(text);
        label.getStyleClass().add("admin-table-text");
        label.setMinWidth(width);
        label.setPrefWidth(width);
        return label;
    }

    private Button createActionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        return button;
    }

    private Node createEmptyCard(String titleText, String subtitleText) {
        VBox box = new VBox(8);
        box.getStyleClass().addAll("card", "admin-panel");
        box.setPadding(new Insets(28));
        Label title = new Label(titleText);
        title.getStyleClass().add("card-title");
        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("text-small");
        subtitle.setWrapText(true);
        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private void showFormError(String message) {
        formErrorLabel.setText(message);
        formErrorLabel.setVisible(true);
        formErrorLabel.setManaged(true);
    }

    private void hideFormError() {
        formErrorLabel.setVisible(false);
        formErrorLabel.setManaged(false);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
