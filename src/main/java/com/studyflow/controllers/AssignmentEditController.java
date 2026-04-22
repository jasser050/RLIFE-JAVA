package com.studyflow.controllers;

import com.studyflow.models.Assignment;
import com.studyflow.models.Project;
import com.studyflow.services.AiProjectInsightsService;
import com.studyflow.services.AssignmentService;
import com.studyflow.utils.CrudViewContext;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AssignmentEditController implements Initializable {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    @FXML private Label feedbackLabel;
    @FXML private TextField titleField;
    @FXML private Label titleError;
    @FXML private ComboBox<Project> projectCombo;
    @FXML private Label projectError;
    @FXML private ComboBox<String> priorityCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private Label startDateError;
    @FXML private ComboBox<String> startTimeCombo;
    @FXML private DatePicker endDatePicker;
    @FXML private Label endDateError;
    @FXML private ComboBox<String> endTimeCombo;
    @FXML private TextArea descriptionArea;
    @FXML private Label descriptionHintLabel;
    @FXML private Button saveButton;

    private final AssignmentService assignmentService = new AssignmentService();
    private final AiProjectInsightsService aiProjectInsightsService = new AiProjectInsightsService();
    private Assignment assignment;
    private Runnable onSaved;
    private List<Project> ownedProjects = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        priorityCombo.setItems(FXCollections.observableArrayList("High", "Medium", "Low"));
        statusCombo.setItems(FXCollections.observableArrayList("To Do", "In Progress", "Review", "Completed"));
        List<String> timeOptions = buildTimeOptions();
        startTimeCombo.setItems(FXCollections.observableArrayList(timeOptions));
        endTimeCombo.setItems(FXCollections.observableArrayList(timeOptions));
        configureProjectCombo();
        descriptionArea.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= MAX_DESCRIPTION_LENGTH ? change : null));
        descriptionArea.textProperty().addListener((obs, oldValue, newValue) -> updateDescriptionHint());
        updateDescriptionHint();

        Assignment contextAssignment = CrudViewContext.consumeAssignment();
        List<Project> contextProjects = CrudViewContext.consumeOwnedProjects();
        if (contextAssignment != null) {
            setData(contextAssignment, contextProjects, null);
        }
    }

    public void setData(Assignment assignment, List<Project> ownedProjects, Runnable onSaved) {
        this.assignment = assignment;
        this.ownedProjects = ownedProjects == null ? new ArrayList<>() : new ArrayList<>(ownedProjects);
        this.onSaved = onSaved;
        projectCombo.getItems().setAll(this.ownedProjects);
        populateForm();
    }

    @FXML
    private void handleSave() {
        if (assignment == null) {
            showFeedback("No assignment selected.", true);
            return;
        }
        clearErrors();
        if (!validateForm()) {
            showFeedback("Please fix the highlighted assignment fields.", true);
            return;
        }

        String nextStatus = statusCombo.getValue();
        boolean movedToCompleted = !"Completed".equalsIgnoreCase(assignment.getStatus()) && "Completed".equalsIgnoreCase(nextStatus);
        assignment.setProjectId(projectCombo.getValue().getId());
        assignment.setProjectTitle(projectCombo.getValue().getTitle());
        assignment.setTitle(titleField.getText().trim());
        assignment.setDescription(descriptionArea.getText().trim());
        assignment.setStartDate(startDatePicker.getValue());
        assignment.setStartTime(LocalTime.parse(startTimeCombo.getValue(), TIME_FORMATTER));
        assignment.setEndDate(endDatePicker.getValue());
        assignment.setEndTime(LocalTime.parse(endTimeCombo.getValue(), TIME_FORMATTER));
        assignment.setPriority(priorityCombo.getValue());
        assignment.setStatus(nextStatus);
        if (movedToCompleted) {
            LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
            assignment.setEndDate(now.toLocalDate());
            assignment.setEndTime(now.toLocalTime());
        }
        applyPlanningEstimate(assignment);

        assignmentService.update(assignment);
        if (onSaved != null) {
            onSaved.run();
        }
        CrudViewContext.rememberAssignmentSelection(assignment.getId());
        CrudViewContext.setFlashMessage(buildPlanningMessage(assignment), false);
        MainController.loadContentInMainArea("views/Assignments.fxml");
    }

    @FXML
    private void handleCancel() {
        CrudViewContext.rememberAssignmentSelection(assignment == null ? null : assignment.getId());
        MainController.loadContentInMainArea("views/Assignments.fxml");
    }

    private void populateForm() {
        if (assignment == null) {
            return;
        }
        titleField.setText(assignment.getTitle());
        descriptionArea.setText(assignment.getDescription() == null ? "" : assignment.getDescription());
        startDatePicker.setValue(assignment.getStartDate());
        endDatePicker.setValue(assignment.getEndDate());
        startTimeCombo.setValue(formatTime(assignment.getStartTime(), "09:00"));
        endTimeCombo.setValue(formatTime(assignment.getEndTime(), "13:00"));
        priorityCombo.setValue(assignment.getPriority() == null ? "Medium" : assignment.getPriority());
        statusCombo.setValue(assignment.getStatus() == null ? "To Do" : assignment.getStatus());
        projectCombo.setValue(ownedProjects.stream()
                .filter(project -> project.getId() == assignment.getProjectId())
                .findFirst()
                .orElse(ownedProjects.isEmpty() ? null : ownedProjects.get(0)));
    }

    private boolean validateForm() {
        boolean valid = true;
        if (projectCombo.getValue() == null) {
            projectError.setText("Project is required.");
            projectError.setVisible(true);
            projectError.setManaged(true);
            valid = false;
        }
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
        if (descriptionArea.getText() != null && descriptionArea.getText().length() > MAX_DESCRIPTION_LENGTH) {
            showFeedback("Description cannot exceed 500 characters.", true);
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
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
            LocalDateTime start = LocalDateTime.of(startDatePicker.getValue(), LocalTime.parse(startTimeCombo.getValue(), TIME_FORMATTER));
            LocalDateTime end = LocalDateTime.of(endDatePicker.getValue(), LocalTime.parse(endTimeCombo.getValue(), TIME_FORMATTER));
            if (end.isBefore(start)) {
                endDateError.setText("End must be after start.");
                endDateError.setVisible(true);
                endDateError.setManaged(true);
                valid = false;
            } else if (start.toLocalDate().isEqual(end.toLocalDate()) && Duration.between(start, end).toHours() < 4) {
                endDateError.setText("Same-day assignments need at least 4 hours.");
                endDateError.setVisible(true);
                endDateError.setManaged(true);
                valid = false;
            }
        }
        return valid;
    }

    private void clearErrors() {
        titleError.setVisible(false);
        titleError.setManaged(false);
        projectError.setVisible(false);
        projectError.setManaged(false);
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

    private void configureProjectCombo() {
        projectCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Project item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });
        projectCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Project item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });
    }

    private List<String> buildTimeOptions() {
        List<String> options = new ArrayList<>();
        LocalTime time = LocalTime.MIDNIGHT;
        for (int slot = 0; slot < 48; slot++) {
            options.add(time.format(TIME_FORMATTER));
            time = time.plusMinutes(30);
        }
        return options;
    }

    private String formatTime(LocalTime time, String fallback) {
        return (time == null ? LocalTime.parse(fallback, TIME_FORMATTER) : time.withSecond(0).withNano(0)).format(TIME_FORMATTER);
    }

    private void applyPlanningEstimate(Assignment assignment) {
        if (assignment == null) {
            return;
        }
        AiProjectInsightsService.AssignmentPlanningEstimate estimate = aiProjectInsightsService.estimateAssignmentPlan(assignment);
        assignment.setEstimatedMinDays(estimate.minDays());
        assignment.setEstimatedMaxDays(estimate.maxDays());
        assignment.setComplexityLevel(estimate.complexityLevel());
        assignment.setAiSuggestedDueDate(estimate.suggestedDueDate());
    }

    private String buildPlanningMessage(Assignment assignment) {
        if (assignment == null || assignment.getAiSuggestedDueDate() == null) {
            return "Assignment updated successfully.";
        }
        return "Assignment updated successfully. AI suggests " + assignment.getComplexityLevel()
                + " complexity and due " + assignment.getAiSuggestedDueDate() + ".";
    }
}
