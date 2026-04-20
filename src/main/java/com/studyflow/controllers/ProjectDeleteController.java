package com.studyflow.controllers;

import com.studyflow.models.Project;
import com.studyflow.services.ProjectService;
import com.studyflow.utils.CrudViewContext;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ProjectDeleteController {
    @FXML private Label titleLabel;
    @FXML private Label metaLabel;
    @FXML private Label feedbackLabel;
    @FXML private Button deleteButton;

    private final ProjectService projectService = new ProjectService();
    private Project project;
    private Runnable onDeleted;

    @FXML
    private void initialize() {
        Project contextProject = CrudViewContext.consumeProject();
        if (contextProject != null) {
            setData(contextProject, null);
        }
    }

    public void setData(Project project, Runnable onDeleted) {
        this.project = project;
        this.onDeleted = onDeleted;
        if (project != null) {
            titleLabel.setText(project.getTitle());
            metaLabel.setText((project.getStatus() == null ? "" : project.getStatus())
                    + " | " + (project.getAssignmentCount()) + " assignments");
        }
    }

    @FXML
    private void handleDelete() {
        if (project == null) {
            feedbackLabel.setText("No project selected.");
            return;
        }
        projectService.delete(project);
        if (onDeleted != null) {
            onDeleted.run();
        }
        CrudViewContext.setFlashMessage("Project deleted successfully.", false);
        MainController.loadContentInMainArea("views/Projects.fxml");
    }

    @FXML
    private void handleCancel() {
        CrudViewContext.rememberProjectSelection(project == null ? null : project.getId());
        MainController.loadContentInMainArea("views/Projects.fxml");
    }
}
