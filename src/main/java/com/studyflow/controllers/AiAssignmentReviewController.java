package com.studyflow.controllers;

import com.studyflow.models.Assignment;
import com.studyflow.models.Project;
import com.studyflow.services.AiAssignmentGeneratorService;
import com.studyflow.utils.CrudViewContext;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AiAssignmentReviewController implements Initializable {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final String SAVE_BUTTON_TEXT = "Save Selected";

    @FXML private VBox loadingOverlay;
    @FXML private Label loadingLabel;
    @FXML private Label loadingSubtitleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label projectTitleLabel;
    @FXML private Label suggestionCountLabel;
    @FXML private Label feedbackLabel;
    @FXML private VBox suggestionsList;
    @FXML private Button saveButton;

    private final AiAssignmentGeneratorService aiAssignmentGeneratorService = new AiAssignmentGeneratorService();
    private final List<Assignment> suggestions = new ArrayList<>();
    private final List<CheckBox> checkBoxes = new ArrayList<>();
    private Project project;
    private String originView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        project = CrudViewContext.consumeProject();
        originView = CrudViewContext.consumeAiSuggestionOriginView();
        String projectTitle = CrudViewContext.consumeAiSuggestionProjectTitle();
        suggestions.clear();
        suggestions.addAll(CrudViewContext.consumeAiAssignmentSuggestions());

        String safeProjectTitle = projectTitle == null || projectTitle.isBlank() ? "this project" : projectTitle;
        projectTitleLabel.setText(safeProjectTitle);
        subtitleLabel.setText("Review AI suggestions for " + safeProjectTitle + ".");
        suggestionCountLabel.setText(suggestions.size() + " suggestions");
        setSaving(false);

        if (suggestions.isEmpty()) {
            saveButton.setDisable(true);
            suggestionsList.getChildren().setAll(createEmptyState());
            return;
        }

        renderSuggestions();
    }

    @FXML
    private void handleSelectAll() {
        checkBoxes.forEach(box -> box.setSelected(true));
    }

    @FXML
    private void handleClearAll() {
        checkBoxes.forEach(box -> box.setSelected(false));
    }

    @FXML
    private void handleSaveSelected() {
        List<Assignment> accepted = new ArrayList<>();
        for (int i = 0; i < suggestions.size(); i++) {
            if (checkBoxes.get(i).isSelected()) {
                accepted.add(suggestions.get(i));
            }
        }

        if (accepted.isEmpty()) {
            showFeedback("Select at least one assignment or go back.", true);
            return;
        }

        setSaving(true);
        showFeedback("Saving selected AI assignments...", false);

        PauseTransition pause = new PauseTransition(Duration.millis(75));
        pause.setOnFinished(event -> {
            Task<List<Assignment>> saveTask = new Task<>() {
                @Override
                protected List<Assignment> call() {
                    return aiAssignmentGeneratorService.saveSuggestions(accepted);
                }
            };

            saveTask.setOnSucceeded(successEvent -> {
                List<Assignment> saved = saveTask.getValue();
                if (project != null) {
                    CrudViewContext.rememberProjectSelection(project.getId());
                }
                CrudViewContext.setFlashMessage(saved.size() + " AI assignments saved successfully.", false);
                setSaving(false);
                MainController.loadContentInMainArea(resolveReturnView());
            });

            saveTask.setOnFailed(failedEvent -> {
                setSaving(false);
                Throwable exception = saveTask.getException();
                String message = exception == null || exception.getMessage() == null || exception.getMessage().isBlank()
                        ? "Failed to save AI assignments."
                        : exception.getMessage();
                showFeedback(message, true);
            });

            Thread saveThread = new Thread(saveTask, "AI-Assignment-Save");
            saveThread.setDaemon(true);
            saveThread.start();
        });
        pause.play();
    }

    @FXML
    private void handleCancel() {
        if (project != null) {
            CrudViewContext.rememberProjectSelection(project.getId());
        }
        CrudViewContext.setFlashMessage("AI suggestions discarded.", false);
        MainController.loadContentInMainArea(resolveReturnView());
    }

    private void renderSuggestions() {
        suggestionsList.getChildren().clear();
        checkBoxes.clear();
        for (Assignment assignment : suggestions) {
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected(true);
            checkBox.getStyleClass().add("admin-checkbox");
            checkBoxes.add(checkBox);
            suggestionsList.getChildren().add(createSuggestionCard(assignment, checkBox));
        }
    }

    private HBox createSuggestionCard(Assignment assignment, CheckBox checkBox) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(16));
        row.getStyleClass().addAll("card", "crud-item", "project-assignment-row", "ai-review-item");

        FontIcon icon = new FontIcon("fth-zap");
        icon.getStyleClass().addAll("detail-row-icon", "project-assignment-icon");

        VBox body = new VBox(6);
        HBox.setHgrow(body, Priority.ALWAYS);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(assignment.getTitle());
        title.getStyleClass().addAll("item-title", "project-assignment-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label priority = new Label(assignment.getPriority());
        priority.getStyleClass().addAll("badge", assignment.getPriorityStyleClass());
        titleRow.getChildren().addAll(title, spacer, priority);

        Label meta = new Label("To Do | " + formatDate(assignment) + " | " + safe(assignment.getProjectTitle()));
        meta.getStyleClass().addAll("item-meta", "project-assignment-meta");

        Label description = new Label(safe(assignment.getDescription()));
        description.setWrapText(true);
        description.getStyleClass().addAll("item-desc", "project-card-description");

        body.getChildren().addAll(titleRow, meta, description);
        row.getChildren().addAll(checkBox, icon, body);
        return row;
    }

    private VBox createEmptyState() {
        VBox box = new VBox(10);
        box.getStyleClass().addAll("card", "empty-state-card");
        box.setPadding(new Insets(28));
        box.setAlignment(Pos.CENTER);

        FontIcon icon = new FontIcon("fth-zap");
        icon.getStyleClass().add("empty-state-icon");

        Label title = new Label("No suggestions available");
        title.getStyleClass().add("item-title");

        Label text = new Label("Generate AI assignments from Projects or Assignments to review them here.");
        text.getStyleClass().add("text-muted");
        text.setWrapText(true);

        box.getChildren().addAll(icon, title, text);
        return box;
    }

    private void showFeedback(String message, boolean error) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("inline-alert-success", "inline-alert-error");
        feedbackLabel.getStyleClass().add(error ? "inline-alert-error" : "inline-alert-success");
        feedbackLabel.setVisible(true);
        feedbackLabel.setManaged(true);
    }

    private void setSaving(boolean saving) {
        saveButton.setDisable(saving || suggestions.isEmpty());
        saveButton.setText(saving ? "Saving..." : SAVE_BUTTON_TEXT);
        suggestionsList.setDisable(saving);
        if (loadingOverlay != null) {
            if (saving) {
                loadingOverlay.toFront();
            }
            loadingOverlay.setVisible(saving);
            loadingOverlay.setManaged(saving);
        }
        if (loadingLabel != null) {
            loadingLabel.setText("Saving assignments");
        }
        if (loadingSubtitleLabel != null) {
            loadingSubtitleLabel.setText("Saving the selected AI assignments to your workspace.");
        }
    }

    private String resolveReturnView() {
        return "views/Assignments.fxml".equals(originView) ? "views/Assignments.fxml" : "views/Projects.fxml";
    }

    private String formatDate(Assignment assignment) {
        if (assignment.getStartDate() == null || assignment.getEndDate() == null) {
            return "schedule pending";
        }
        return DATE_FORMATTER.format(assignment.getStartDate()) + " -> " + DATE_FORMATTER.format(assignment.getEndDate());
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
