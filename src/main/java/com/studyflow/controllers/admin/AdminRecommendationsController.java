package com.studyflow.controllers.admin;

import com.studyflow.models.RecommendationStress;
import com.studyflow.services.ServiceRecommendationStress;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class AdminRecommendationsController implements Initializable {
    private static final int TITLE_MIN_LENGTH = 10;
    private static final int TITLE_MAX_LENGTH = 100;
    private static final int CONTENT_MIN_LENGTH = 11;
    private static final Pattern TITLE_ONLY_DIGITS_PATTERN = Pattern.compile("^\\d+$");
    private static final String TITLE_REQUIRED_MESSAGE = "Le titre est obligatoire.";
    private static final String TITLE_MIN_MESSAGE = "Le titre doit contenir au minimum 10 caractères.";
    private static final String TITLE_MAX_MESSAGE = "Le titre ne doit pas dépasser 100 caractères.";
    private static final String TITLE_DIGITS_ONLY_MESSAGE = "Le titre ne peut pas contenir uniquement des chiffres.";
    private static final String CONTENT_REQUIRED_MESSAGE = "Le contenu est obligatoire.";
    private static final String CONTENT_MIN_MESSAGE = "Le contenu doit contenir plus de 10 caractères.";
    private static final String INVALID_FIELD_STYLE = "-fx-border-color: #ef4444; -fx-border-width: 1.6; -fx-border-radius: 8;";
    private static final long DELETE_CONFIRM_WINDOW_MS = 5000;

    @FXML private ScrollPane listSection;
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
    @FXML private Label notificationLabel;

    private ServiceRecommendationStress service;
    private RecommendationStress editingItem;
    private RecommendationStress selectedItem;
    private Integer pendingDeleteRecommendationId;
    private long pendingDeleteDeadlineMs;
    private final DateTimeFormatter shortDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private final DateTimeFormatter longDateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        service = new ServiceRecommendationStress();
        recommendationsListBox.setSpacing(10);
        enforceTextLimit(titleField, TITLE_MAX_LENGTH);
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

        for (RecommendationStress item : items) {
            recommendationsListBox.getChildren().add(createRow(item));
        }
    }

    private Node createRow(RecommendationStress item) {
        HBox row = new HBox(14);
        row.getStyleClass().add("reco-list-item");
        row.setPadding(new Insets(14, 16, 14, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(item.getTitle());
        title.getStyleClass().add("reco-list-title");

        Label level = new Label(levelText(item.getLevel()));
        level.getStyleClass().addAll("badge", levelBadge(item.getLevel()));
        level.setAlignment(Pos.CENTER);

        Label preview = new Label(item.getContent() == null ? "" : item.getContent());
        preview.getStyleClass().add("reco-list-preview");
        preview.setWrapText(true);
        preview.setMaxWidth(Double.MAX_VALUE);

        Label status = new Label(item.isActive() ? "Active" : "Inactive");
        status.getStyleClass().addAll("badge", item.isActive() ? "success" : "secondary");
        status.setAlignment(Pos.CENTER);

        HBox meta = new HBox(8, level, status);
        meta.setAlignment(Pos.CENTER_LEFT);
        VBox content = new VBox(6, title, preview, meta);
        content.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(content, Priority.ALWAYS);

        Button viewBtn = createActionButton("View", "btn-ghost");
        viewBtn.setOnAction(e -> showDetails(item));
        Button editBtn = createActionButton("Edit", "btn-secondary");
        editBtn.setOnAction(e -> startEdit(item));
        Button deleteBtn = createActionButton("Delete", "btn-danger");
        deleteBtn.setOnAction(e -> deleteRecommendation(item));
        HBox actions = new HBox(8, viewBtn, editBtn, deleteBtn);
        actions.getStyleClass().add("reco-actions");
        actions.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(content, actions);
        return row;
    }

    @FXML
    private void handleAddRecommendation() {
        editingItem = null;
        formTitleLabel.setText("Create New Recommendation");
        saveButton.setText("Create Recommendation");
        titleField.clear();
        contentArea.clear();
        markFieldValid(titleField);
        markFieldValid(contentArea);
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
        markFieldValid(titleField);
        markFieldValid(contentArea);
        levelCombo.setValue(item.getLevel());
        activeCheckBox.setSelected(item.isActive());
        hideFormError();
        showFormMode();
    }

    private void showDetails(RecommendationStress item) {
        try {
            selectedItem = service.findById(item.getId());
            if (selectedItem == null) {
                showNotification("La recommandation sélectionnée est introuvable.", "warning");
                return;
            }
        } catch (RuntimeException e) {
            showNotification(e.getMessage() == null ? "Échec du chargement de la recommandation." : e.getMessage(), "error");
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
            markFieldInvalid(titleField);
            markFieldValid(contentArea);
            showFormError(TITLE_REQUIRED_MESSAGE);
            return;
        }
        if (title.length() < TITLE_MIN_LENGTH) {
            markFieldInvalid(titleField);
            markFieldValid(contentArea);
            showFormError(TITLE_MIN_MESSAGE);
            return;
        }
        if (title.length() > TITLE_MAX_LENGTH) {
            markFieldInvalid(titleField);
            markFieldValid(contentArea);
            showFormError(TITLE_MAX_MESSAGE);
            return;
        }
        if (TITLE_ONLY_DIGITS_PATTERN.matcher(title).matches()) {
            markFieldInvalid(titleField);
            markFieldValid(contentArea);
            showFormError(TITLE_DIGITS_ONLY_MESSAGE);
            return;
        }
        if (content.isBlank()) {
            markFieldValid(titleField);
            markFieldInvalid(contentArea);
            showFormError(CONTENT_REQUIRED_MESSAGE);
            return;
        }
        if (content.length() < CONTENT_MIN_LENGTH) {
            markFieldValid(titleField);
            markFieldInvalid(contentArea);
            showFormError(CONTENT_MIN_MESSAGE);
            return;
        }
        markFieldValid(titleField);
        markFieldValid(contentArea);
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
        long now = System.currentTimeMillis();
        if (pendingDeleteRecommendationId == null || pendingDeleteRecommendationId != item.getId() || now > pendingDeleteDeadlineMs) {
            pendingDeleteRecommendationId = item.getId();
            pendingDeleteDeadlineMs = now + DELETE_CONFIRM_WINDOW_MS;
            showNotification("Cliquez encore sur Delete dans 5 secondes pour confirmer la suppression.", "warning");
            return;
        }
        try {
            service.delete(item.getId());
            pendingDeleteRecommendationId = null;
            showNotification("Recommandation supprimée avec succès.", "success");
            loadRecommendations();
            showListMode();
        } catch (RuntimeException e) {
            showNotification(e.getMessage() == null ? "Échec de suppression." : e.getMessage(), "error");
        }
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

    private void showNotification(String message, String type) {
        if (notificationLabel == null) {
            return;
        }
        String background = switch (type) {
            case "success" -> "#16a34a";
            case "warning" -> "#d97706";
            default -> "#dc2626";
        };
        notificationLabel.setText(message);
        notificationLabel.setStyle(
                "-fx-background-color: " + background + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 10 14 10 14;" +
                        "-fx-background-radius: 8;"
        );
        notificationLabel.setVisible(true);
        notificationLabel.setManaged(true);
    }

    private void enforceTextLimit(TextField field, int maxLength) {
        field.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= maxLength ? change : null));
    }

    private void markFieldInvalid(TextField field) {
        field.setStyle(INVALID_FIELD_STYLE);
    }

    private void markFieldInvalid(TextArea area) {
        area.setStyle(INVALID_FIELD_STYLE);
    }

    private void markFieldValid(TextField field) {
        field.setStyle("");
    }

    private void markFieldValid(TextArea area) {
        area.setStyle("");
    }

}