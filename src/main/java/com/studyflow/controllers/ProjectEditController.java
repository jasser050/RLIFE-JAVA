package com.studyflow.controllers;

import com.studyflow.models.Project;
import com.studyflow.services.ProjectService;
import com.studyflow.utils.CrudViewContext;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.net.URL;
import java.util.ResourceBundle;

public class ProjectEditController implements Initializable {
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    @FXML private Label feedbackLabel;
    @FXML private TextField titleField;
    @FXML private Label titleError;
    @FXML private DatePicker startDatePicker;
    @FXML private Label startDateError;
    @FXML private DatePicker endDatePicker;
    @FXML private Label endDateError;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextArea descriptionArea;
    @FXML private Label descriptionHintLabel;
    @FXML private Button saveButton;

    private final ProjectService projectService = new ProjectService();
    private Project project;
    private Runnable onSaved;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusCombo.setItems(FXCollections.observableArrayList("Planned", "In Progress", "On Hold", "Completed"));
        descriptionArea.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= MAX_DESCRIPTION_LENGTH ? change : null));
        descriptionArea.textProperty().addListener((obs, oldValue, newValue) -> updateDescriptionHint());
        updateDescriptionHint();

        Project contextProject = CrudViewContext.consumeProject();
        if (contextProject != null) {
            setData(contextProject, null);
        }
    }

    public void setData(Project project, Runnable onSaved) {
        this.project = project;
        this.onSaved = onSaved;
        if (project != null) {
            titleField.setText(project.getTitle());
            startDatePicker.setValue(project.getStartDate());
            endDatePicker.setValue(project.getEndDate());
            statusCombo.setValue(project.getStatus());
            descriptionArea.setText(project.getDescription() == null ? "" : project.getDescription());
        }
    }

    @FXML
    private void handleSave() {
        if (project == null) {
            showFeedback("No project selected.", true);
            return;
        }
        clearErrors();
        if (!validateForm()) {
            showFeedback("Please fix the highlighted project fields.", true);
            return;
        }
        project.setTitle(titleField.getText().trim());
        project.setStartDate(startDatePicker.getValue());
        project.setEndDate(endDatePicker.getValue());
        project.setStatus(statusCombo.getValue());
        project.setDescription(descriptionArea.getText().trim());
        projectService.update(project);
        if (onSaved != null) {
            onSaved.run();
        }
        CrudViewContext.rememberProjectSelection(project.getId());
        CrudViewContext.setFlashMessage("Project updated successfully.", false);
        MainController.loadContentInMainArea("views/Projects.fxml");
    }

    @FXML
    private void handleCancel() {
        CrudViewContext.rememberProjectSelection(project == null ? null : project.getId());
        MainController.loadContentInMainArea("views/Projects.fxml");
    }

    private boolean validateForm() {
        boolean valid = true;
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        if (title.isEmpty()) {
            titleError.setText("Title is required.");
            titleError.setVisible(true);
            titleError.setManaged(true);
            valid = false;
        } else if (title.length() < 3) {
            titleError.setText("Title must contain at least 3 characters.");
            titleError.setVisible(true);
            titleError.setManaged(true);
            valid = false;
        }
        if (startDatePicker.getValue() == null) {
            startDateError.setText("Start date is required.");
            startDateError.setVisible(true);
            startDateError.setManaged(true);
            valid = false;
        }
        if (endDatePicker.getValue() == null) {
            endDateError.setText("End date is required.");
            endDateError.setVisible(true);
            endDateError.setManaged(true);
            valid = false;
        }
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null
                && endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
            endDateError.setText("End date cannot be earlier than start date.");
            endDateError.setVisible(true);
            endDateError.setManaged(true);
            valid = false;
        }
        return valid;
    }

    private void clearErrors() {
        titleError.setVisible(false);
        titleError.setManaged(false);
        startDateError.setVisible(false);
        startDateError.setManaged(false);
        endDateError.setVisible(false);
        endDateError.setManaged(false);
    }

    private void showFeedback(String message, boolean error) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("inline-alert-success", "inline-alert-error");
        feedbackLabel.getStyleClass().add(error ? "inline-alert-error" : "inline-alert-success");
        feedbackLabel.setVisible(true);
        feedbackLabel.setManaged(true);
    }

    private void updateDescriptionHint() {
        int currentLength = descriptionArea.getText() == null ? 0 : descriptionArea.getText().length();
        descriptionHintLabel.setText(currentLength + "/" + MAX_DESCRIPTION_LENGTH + " characters");
    }
}
