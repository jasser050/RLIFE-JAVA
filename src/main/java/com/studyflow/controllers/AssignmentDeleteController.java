package com.studyflow.controllers;

import com.studyflow.models.Assignment;
import com.studyflow.services.AssignmentService;
import com.studyflow.utils.CrudViewContext;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class AssignmentDeleteController {
    @FXML private Label titleLabel;
    @FXML private Label metaLabel;
    @FXML private Label feedbackLabel;
    @FXML private Button deleteButton;

    private final AssignmentService assignmentService = new AssignmentService();
    private Assignment assignment;
    private Runnable onDeleted;

    @FXML
    private void initialize() {
        Assignment contextAssignment = CrudViewContext.consumeAssignment();
        if (contextAssignment != null) {
            setData(contextAssignment, null);
        }
    }

    public void setData(Assignment assignment, Runnable onDeleted) {
        this.assignment = assignment;
        this.onDeleted = onDeleted;
        if (assignment != null) {
            titleLabel.setText(assignment.getTitle());
            metaLabel.setText((assignment.getProjectTitle() == null ? "" : assignment.getProjectTitle())
                    + " | " + (assignment.getStatus() == null ? "" : assignment.getStatus()));
        }
    }

    @FXML
    private void handleDelete() {
        if (assignment == null) {
            feedbackLabel.setText("No assignment selected.");
            return;
        }
        assignmentService.delete(assignment);
        if (onDeleted != null) {
            onDeleted.run();
        }
        CrudViewContext.setFlashMessage("Assignment deleted successfully.", false);
        MainController.loadContentInMainArea("views/Assignments.fxml");
    }

    @FXML
    private void handleCancel() {
        CrudViewContext.rememberAssignmentSelection(assignment == null ? null : assignment.getId());
        MainController.loadContentInMainArea("views/Assignments.fxml");
    }
}
