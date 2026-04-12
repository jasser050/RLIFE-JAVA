package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.Assignment;
import com.studyflow.models.Project;
import com.studyflow.models.User;
import com.studyflow.services.AssignmentService;
import com.studyflow.services.NotificationService;
import com.studyflow.services.ProjectService;
import com.studyflow.utils.PdfExportUtil;
import com.studyflow.utils.UserSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ProjectsController implements Initializable {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter PDF_FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @FXML private Label totalProjectsLabel;
    @FXML private Label inProgressProjectsLabel;
    @FXML private Label completedProjectsLabel;
    @FXML private Label linkedAssignmentsLabel;
    @FXML private Label feedbackLabel;
    @FXML private TextField projectSearchField;
    @FXML private ComboBox<String> projectSortCombo;
    @FXML private Button exportProjectsPdfButton;
    @FXML private PieChart projectStatusChart;
    @FXML private BarChart<String, Number> projectLoadChart;
    @FXML private TextField createProjectTitleField;
    @FXML private Label createProjectTitleError;
    @FXML private DatePicker createProjectStartDatePicker;
    @FXML private Label createProjectStartDateError;
    @FXML private DatePicker createProjectEndDatePicker;
    @FXML private Label createProjectEndDateError;
    @FXML private ComboBox<String> createProjectStatusCombo;
    @FXML private TextArea createProjectDescriptionArea;
    @FXML private Button createProjectButton;
    @FXML private Button clearCreateProjectButton;
    @FXML private VBox updateProjectCard;
    @FXML private Label updateProjectHintLabel;
    @FXML private TextField updateProjectTitleField;
    @FXML private Label updateProjectTitleError;
    @FXML private DatePicker updateProjectStartDatePicker;
    @FXML private Label updateProjectStartDateError;
    @FXML private DatePicker updateProjectEndDatePicker;
    @FXML private Label updateProjectEndDateError;
    @FXML private ComboBox<String> updateProjectStatusCombo;
    @FXML private TextArea updateProjectDescriptionArea;
    @FXML private Button updateProjectButton;
    @FXML private Button clearProjectSelectionButton;
    @FXML private VBox projectList;
    @FXML private Label selectedProjectTitleLabel;
    @FXML private Label selectedProjectMetaLabel;
    @FXML private Label selectedProjectStatusLabel;
    @FXML private Label selectedProjectCountLabel;
    @FXML private VBox projectAssignmentsList;

    private final ProjectService projectService = new ProjectService();
    private final AssignmentService assignmentService = new AssignmentService();
    private final NotificationService notificationService = new NotificationService();

    private final List<Project> projects = new ArrayList<>();
    private Project selectedProject;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<String> statuses = List.of("Planned", "In Progress", "On Hold", "Completed");
        createProjectStatusCombo.getItems().addAll(statuses);
        updateProjectStatusCombo.getItems().addAll(statuses);
        projectSortCombo.getItems().addAll("Newest Added", "Title A-Z", "Start Date", "End Date", "Status");
        projectSortCombo.setValue("Newest Added");
        projectSearchField.textProperty().addListener((obs, oldValue, newValue) -> renderProjects());
        projectSortCombo.valueProperty().addListener((obs, oldValue, newValue) -> renderProjects());
        bindProjectValidationReset();
        configureAccessibility();

        if (!isReady()) {
            disableForm();
            return;
        }

        resetCreateForm();
        clearProjectSelection();
        refreshProjects();
    }

    @FXML
    private void handleCreateProject() {
        if (!isReady()) {
            return;
        }

        clearCreateValidation();
        if (!validateProjectForm(
                createProjectTitleField, createProjectTitleError,
                createProjectStartDatePicker, createProjectStartDateError,
                createProjectEndDatePicker, createProjectEndDateError)) {
            showFeedback("Please fix the highlighted project fields.", true);
            return;
        }

        Project project = new Project();
        project.setUserId(getCurrentUser().getId());
        project.setTitle(createProjectTitleField.getText().trim());
        project.setDescription(defaultText(createProjectDescriptionArea.getText()));
        project.setStartDate(createProjectStartDatePicker.getValue());
        project.setEndDate(createProjectEndDatePicker.getValue());
        project.setStatus(defaultProjectStatus(createProjectStatusCombo.getValue()));

        projectService.add(project);
        refreshProjects();
        resetCreateForm();
        selectProject(findProjectById(project.getId()));
        showFeedback("Project created successfully.", false);
    }

    @FXML
    private void handleUpdateProject() {
        if (!isReady() || selectedProject == null) {
            showFeedback("Select a project before updating it.", true);
            return;
        }

        clearUpdateValidation();
        if (!validateProjectForm(
                updateProjectTitleField, updateProjectTitleError,
                updateProjectStartDatePicker, updateProjectStartDateError,
                updateProjectEndDatePicker, updateProjectEndDateError)) {
            showFeedback("Please fix the highlighted project fields.", true);
            return;
        }

        selectedProject.setUserId(getCurrentUser().getId());
        selectedProject.setTitle(updateProjectTitleField.getText().trim());
        selectedProject.setDescription(defaultText(updateProjectDescriptionArea.getText()));
        selectedProject.setStartDate(updateProjectStartDatePicker.getValue());
        selectedProject.setEndDate(updateProjectEndDatePicker.getValue());
        selectedProject.setStatus(defaultProjectStatus(updateProjectStatusCombo.getValue()));

        projectService.update(selectedProject);
        int selectedId = selectedProject.getId();
        refreshProjects();
        selectProject(findProjectById(selectedId));
        showFeedback("Project updated successfully.", false);
    }

    @FXML
    private void handleResetCreateProject() {
        resetCreateForm();
        showFeedback("Project form cleared.", false);
    }

    @FXML
    private void handleClearProjectSelection() {
        clearProjectSelection();
        renderProjects();
    }

    @FXML
    private void handleExportProjectsPdf() {
        List<Project> visibleProjects = getVisibleProjects();
        if (visibleProjects.isEmpty()) {
            showFeedback("No projects available to export.", true);
            return;
        }

        File file = PdfExportUtil.chooseExportFile(
                App.getPrimaryStage(),
                "projects-report-" + PDF_FILE_FORMATTER.format(LocalDateTime.now()) + ".pdf"
        );
        if (file == null) {
            showFeedback("PDF export canceled.", true);
            return;
        }
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            file = new File(file.getParentFile(), file.getName() + ".pdf");
        }

        List<List<String>> rows = visibleProjects.stream()
                .map(project -> List.of(
                        project.getTitle(),
                        project.getStatus(),
                        formatDate(project.getStartDate()),
                        formatDate(project.getEndDate()),
                        String.valueOf(project.getAssignmentCount())))
                .collect(Collectors.toList());

        List<String> summary = List.of(
                "Total projects: " + totalProjectsLabel.getText(),
                "In progress: " + inProgressProjectsLabel.getText(),
                "Completed: " + completedProjectsLabel.getText(),
                "Linked assignments: " + linkedAssignmentsLabel.getText());

        try {
            PdfExportUtil.exportTableReport(
                    file,
                    "Projects Report",
                    "Professional export generated from StudyFlow",
                    summary,
                    List.of("Project", "Status", "Start", "End", "Assignments"),
                    rows
            );
            showFeedback("Projects PDF exported to: " + file.getAbsolutePath(), false);
        } catch (Exception e) {
            showFeedback("Unable to export the projects PDF: " + e.getMessage(), true);
        }
    }

    private void refreshProjects() {
        projects.clear();
        projects.addAll(projectService.getByUserId(getCurrentUser().getId()));
        notificationService.syncDueDateNotifications(getCurrentUser().getId(), projects, assignmentService.getByUserId(getCurrentUser().getId()));
        updateStats();
        updateCharts();
        renderProjects();

        if (selectedProject != null) {
            selectProject(findProjectById(selectedProject.getId()));
        } else {
            showSelectedProjectDetails(null);
        }
    }

    private void updateStats() {
        totalProjectsLabel.setText(String.valueOf(projects.size()));
        inProgressProjectsLabel.setText(String.valueOf(projects.stream().filter(Project::isInProgress).count()));
        completedProjectsLabel.setText(String.valueOf(projects.stream().filter(Project::isCompleted).count()));
        linkedAssignmentsLabel.setText(String.valueOf(projects.stream().mapToInt(Project::getAssignmentCount).sum()));
    }

    private void updateCharts() {
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        statusCounts.put("Planned", 0);
        statusCounts.put("In Progress", 0);
        statusCounts.put("On Hold", 0);
        statusCounts.put("Completed", 0);
        for (Project project : projects) {
            statusCounts.computeIfPresent(project.getStatus(), (key, value) -> value + 1);
        }

        List<PieChart.Data> pieData = statusCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> new PieChart.Data(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        if (pieData.isEmpty()) {
            pieData = List.of(new PieChart.Data("No Data", 1));
        }
        projectStatusChart.setData(FXCollections.observableArrayList(pieData));
        projectStatusChart.setLabelsVisible(false);
        projectStatusChart.setLegendVisible(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Assignments");
        List<Project> topProjects = projects.stream()
                .sorted(Comparator.comparingInt(Project::getAssignmentCount).reversed())
                .limit(6)
                .collect(Collectors.toList());
        for (Project project : topProjects) {
            series.getData().add(new XYChart.Data<>(truncate(project.getTitle(), 14), project.getAssignmentCount()));
        }
        projectLoadChart.getData().setAll(series);
        projectLoadChart.setLegendVisible(false);
        projectLoadChart.setAnimated(false);
    }

    private void renderProjects() {
        projectList.getChildren().clear();
        List<Project> visibleProjects = getVisibleProjects();
        if (visibleProjects.isEmpty()) {
            projectList.getChildren().add(createEmptyState("No projects match the current search."));
            return;
        }
        for (Project project : visibleProjects) {
            projectList.getChildren().add(createProjectCard(project));
        }
    }

    private List<Project> getVisibleProjects() {
        return projects.stream()
                .filter(this::matchesSearch)
                .sorted(projectComparator())
                .collect(Collectors.toList());
    }

    private Comparator<Project> projectComparator() {
        String sort = projectSortCombo.getValue();
        if ("Title A-Z".equals(sort)) {
            return Comparator.comparing(project -> safeText(project.getTitle()), String.CASE_INSENSITIVE_ORDER);
        }
        if ("Start Date".equals(sort)) {
            return Comparator.comparing(Project::getStartDate, Comparator.nullsLast(LocalDate::compareTo))
                    .thenComparing(project -> safeText(project.getTitle()), String.CASE_INSENSITIVE_ORDER);
        }
        if ("End Date".equals(sort)) {
            return Comparator.comparing(Project::getEndDate, Comparator.nullsLast(LocalDate::compareTo))
                    .thenComparing(project -> safeText(project.getTitle()), String.CASE_INSENSITIVE_ORDER);
        }
        if ("Status".equals(sort)) {
            return Comparator.<Project, String>comparing(project -> safeText(project.getStatus()), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(project -> safeText(project.getTitle()), String.CASE_INSENSITIVE_ORDER);
        }
        return Comparator.comparing(Project::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Project::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private VBox createProjectCard(Project project) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("card", "crud-item");
        if (selectedProject != null && selectedProject.getId() == project.getId()) {
            card.getStyleClass().add("crud-item-selected");
        }
        card.setPadding(new Insets(18));
        card.setAccessibleText("Project card for " + project.getTitle());

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(6);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label titleLabel = new Label(project.getTitle());
        titleLabel.getStyleClass().add("item-title");

        Label metaLabel = new Label(formatDate(project.getStartDate()) + " -> " + formatDate(project.getEndDate()));
        metaLabel.getStyleClass().add("item-meta");
        titleBox.getChildren().addAll(titleLabel, metaLabel);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button editButton = new Button("Edit");
        editButton.getStyleClass().add("action-outline-button");
        editButton.setGraphic(new FontIcon("fth-edit-3"));
        editButton.setAccessibleText("Edit project " + project.getTitle());
        editButton.setOnAction(event -> {
            selectProject(project);
            event.consume();
        });

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("danger-outline-button");
        deleteButton.setGraphic(new FontIcon("fth-trash-2"));
        deleteButton.setAccessibleText("Delete project " + project.getTitle());
        deleteButton.setOnAction(event -> {
            if (!confirmDeletion("Delete project", "Delete \"" + project.getTitle() + "\" and remove it from the list?")) {
                event.consume();
                return;
            }
            projectService.delete(project);
            if (selectedProject != null && selectedProject.getId() == project.getId()) {
                clearProjectSelection();
            }
            refreshProjects();
            showFeedback("Project deleted successfully.", false);
            event.consume();
        });

        actions.getChildren().addAll(editButton, deleteButton);
        topRow.getChildren().addAll(titleBox, actions);

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getChildren().addAll(
                createBadge(project.getStatus(), project.getStatusStyleClass()),
                createBadge(project.getAssignmentCount() + " assignments", "secondary")
        );

        Label descriptionLabel = new Label(defaultText(project.getDescription()));
        descriptionLabel.getStyleClass().add("item-desc");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setManaged(!descriptionLabel.getText().isEmpty());
        descriptionLabel.setVisible(!descriptionLabel.getText().isEmpty());

        card.setOnMouseClicked(event -> selectProject(project));
        card.getChildren().addAll(topRow, badges);
        if (!descriptionLabel.getText().isEmpty()) {
            card.getChildren().add(descriptionLabel);
        }
        return card;
    }

    private void selectProject(Project project) {
        selectedProject = project;
        renderProjects();
        showSelectedProjectDetails(project);
        populateUpdateForm(project);
    }

    private void showSelectedProjectDetails(Project project) {
        projectAssignmentsList.getChildren().clear();

        if (project == null) {
            selectedProjectTitleLabel.setText("No project selected");
            selectedProjectMetaLabel.setText("Select a project to review its details and assignments.");
            selectedProjectStatusLabel.setText("--");
            selectedProjectStatusLabel.getStyleClass().setAll("badge", "secondary");
            selectedProjectCountLabel.setText("0 assignments");
            projectAssignmentsList.getChildren().add(createEmptyState("Assignments linked to the selected project will appear here."));
            return;
        }

        selectedProjectTitleLabel.setText(project.getTitle());
        selectedProjectMetaLabel.setText(formatDate(project.getStartDate()) + " -> " + formatDate(project.getEndDate()));
        selectedProjectStatusLabel.setText(project.getStatus());
        selectedProjectStatusLabel.getStyleClass().setAll("badge", project.getStatusStyleClass());
        selectedProjectCountLabel.setText(project.getAssignmentCount() + " assignments");

        List<Assignment> assignments = assignmentService.getByProjectId(project.getId(), getCurrentUser().getId());
        if (assignments.isEmpty()) {
            projectAssignmentsList.getChildren().add(createEmptyState("No assignments are attached to this project yet."));
            return;
        }

        for (Assignment assignment : assignments) {
            projectAssignmentsList.getChildren().add(createAssignmentPreview(assignment));
        }
    }

    private HBox createAssignmentPreview(Assignment assignment) {
        HBox row = new HBox(12);
        row.getStyleClass().add("detail-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 14, 12, 14));

        FontIcon icon = new FontIcon(assignment.isCompleted() ? "fth-check-circle" : "fth-clipboard");
        icon.getStyleClass().add("detail-row-icon");

        VBox textBox = new VBox(4);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label title = new Label(assignment.getTitle());
        title.getStyleClass().add("item-title");

        Label meta = new Label(assignment.getStatus() + " | " + assignment.getPriority() + " | due " + formatDate(assignment.getEndDate()));
        meta.getStyleClass().add("item-meta");
        textBox.getChildren().addAll(title, meta);

        row.getChildren().addAll(icon, textBox, createBadge(assignment.getPriority(), assignment.getPriorityStyleClass()));
        return row;
    }

    private VBox createEmptyState(String message) {
        VBox box = new VBox(10);
        box.getStyleClass().addAll("card", "empty-state-card");
        box.setPadding(new Insets(28));
        box.setAlignment(Pos.CENTER);

        FontIcon icon = new FontIcon("fth-folder");
        icon.getStyleClass().add("empty-state-icon");

        Label title = new Label("Nothing here yet");
        title.getStyleClass().add("item-title");

        Label text = new Label(message);
        text.getStyleClass().add("text-muted");
        text.setWrapText(true);

        box.getChildren().addAll(icon, title, text);
        return box;
    }

    private HBox createBadge(String text, String styleClass) {
        HBox badge = new HBox();
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.getStyleClass().addAll("badge", styleClass);
        badge.getChildren().add(new Label(text));
        return badge;
    }

    private void populateUpdateForm(Project project) {
        boolean hasSelection = project != null;
        updateProjectCard.setDisable(!hasSelection);
        updateProjectCard.setOpacity(hasSelection ? 1 : 0.55);
        clearUpdateValidation();

        if (!hasSelection) {
            updateProjectHintLabel.setText("Select a project from the registry to edit it here.");
            updateProjectTitleField.clear();
            updateProjectDescriptionArea.clear();
            updateProjectStartDatePicker.setValue(null);
            updateProjectEndDatePicker.setValue(null);
            updateProjectStatusCombo.setValue("Planned");
            return;
        }

        updateProjectHintLabel.setText("Updating project #" + project.getId() + ".");
        updateProjectTitleField.setText(project.getTitle());
        updateProjectDescriptionArea.setText(defaultText(project.getDescription()));
        updateProjectStartDatePicker.setValue(project.getStartDate());
        updateProjectEndDatePicker.setValue(project.getEndDate());
        updateProjectStatusCombo.setValue(project.getStatus());
    }

    private void resetCreateForm() {
        createProjectTitleField.clear();
        createProjectDescriptionArea.clear();
        createProjectStartDatePicker.setValue(null);
        createProjectEndDatePicker.setValue(null);
        createProjectStatusCombo.setValue("Planned");
        clearCreateValidation();
    }

    private void clearProjectSelection() {
        selectedProject = null;
        populateUpdateForm(null);
        showSelectedProjectDetails(null);
    }

    private boolean validateProjectForm(
            TextField titleField, Label titleError,
            DatePicker startPicker, Label startError,
            DatePicker endPicker, Label endError) {
        boolean valid = true;
        String title = defaultText(titleField.getText());
        if (title.isEmpty()) {
            showFieldError(titleField, titleError, "Title is required.");
            valid = false;
        } else if (title.length() < 3) {
            showFieldError(titleField, titleError, "Title must contain at least 3 characters.");
            valid = false;
        }
        if (startPicker.getValue() == null) {
            showFieldError(startPicker, startError, "Start date is required.");
            valid = false;
        }
        if (endPicker.getValue() == null) {
            showFieldError(endPicker, endError, "End date is required.");
            valid = false;
        }
        if (startPicker.getValue() != null && endPicker.getValue() != null && endPicker.getValue().isBefore(startPicker.getValue())) {
            showFieldError(endPicker, endError, "End date cannot be earlier than start date.");
            valid = false;
        }
        return valid;
    }

    private boolean matchesSearch(Project project) {
        if (isBlank(projectSearchField.getText())) {
            return true;
        }
        String query = projectSearchField.getText().trim().toLowerCase();
        return defaultText(project.getTitle()).toLowerCase().contains(query)
                || defaultText(project.getDescription()).toLowerCase().contains(query)
                || defaultText(project.getStatus()).toLowerCase().contains(query)
                || formatDate(project.getStartDate()).toLowerCase().contains(query)
                || formatDate(project.getEndDate()).toLowerCase().contains(query);
    }

    private void bindProjectValidationReset() {
        createProjectTitleField.textProperty().addListener((obs, oldValue, newValue) -> clearFieldError(createProjectTitleField, createProjectTitleError));
        createProjectStartDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> clearFieldError(createProjectStartDatePicker, createProjectStartDateError));
        createProjectEndDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> clearFieldError(createProjectEndDatePicker, createProjectEndDateError));
        updateProjectTitleField.textProperty().addListener((obs, oldValue, newValue) -> clearFieldError(updateProjectTitleField, updateProjectTitleError));
        updateProjectStartDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> clearFieldError(updateProjectStartDatePicker, updateProjectStartDateError));
        updateProjectEndDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> clearFieldError(updateProjectEndDatePicker, updateProjectEndDateError));
    }

    private void configureAccessibility() {
        projectSearchField.setAccessibleText("Search projects");
        projectSortCombo.setAccessibleText("Sort projects");
        exportProjectsPdfButton.setAccessibleText("Export visible projects to PDF");
        exportProjectsPdfButton.setTooltip(new Tooltip("Export the visible project list as a PDF report"));
        createProjectTitleField.setAccessibleText("Project title");
        createProjectStartDatePicker.setAccessibleText("Project start date");
        createProjectEndDatePicker.setAccessibleText("Project end date");
        createProjectStatusCombo.setAccessibleText("Project status");
        createProjectDescriptionArea.setAccessibleText("Project description");
        updateProjectTitleField.setAccessibleText("Selected project title");
        updateProjectStartDatePicker.setAccessibleText("Selected project start date");
        updateProjectEndDatePicker.setAccessibleText("Selected project end date");
        updateProjectStatusCombo.setAccessibleText("Selected project status");
        updateProjectDescriptionArea.setAccessibleText("Selected project description");
    }

    private void clearCreateValidation() {
        clearFieldError(createProjectTitleField, createProjectTitleError);
        clearFieldError(createProjectStartDatePicker, createProjectStartDateError);
        clearFieldError(createProjectEndDatePicker, createProjectEndDateError);
    }

    private void clearUpdateValidation() {
        clearFieldError(updateProjectTitleField, updateProjectTitleError);
        clearFieldError(updateProjectStartDatePicker, updateProjectStartDateError);
        clearFieldError(updateProjectEndDatePicker, updateProjectEndDateError);
    }

    private void showFieldError(Control control, Label errorLabel, String message) {
        if (!control.getStyleClass().contains("field-invalid")) {
            control.getStyleClass().add("field-invalid");
        }
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearFieldError(Control control, Label errorLabel) {
        control.getStyleClass().remove("field-invalid");
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void showFeedback(String message, boolean error) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("inline-alert-success", "inline-alert-error");
        feedbackLabel.getStyleClass().add(error ? "inline-alert-error" : "inline-alert-success");
        feedbackLabel.setVisible(true);
        feedbackLabel.setManaged(true);
    }

    private boolean confirmDeletion(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void disableForm() {
        showFeedback("Database unavailable or no user is logged in.", true);
        createProjectButton.setDisable(true);
        clearCreateProjectButton.setDisable(true);
        updateProjectCard.setDisable(true);
        exportProjectsPdfButton.setDisable(true);
    }

    private boolean isReady() {
        return projectService.isDatabaseAvailable() && assignmentService.isDatabaseAvailable() && getCurrentUser() != null;
    }

    private User getCurrentUser() {
        return UserSession.getInstance().getCurrentUser();
    }

    private Project findProjectById(int id) {
        return projects.stream().filter(project -> project.getId() == id).findFirst().orElse(null);
    }

    private String defaultProjectStatus(String value) {
        return isBlank(value) ? "Planned" : value;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return safeText(value);
        }
        return value.substring(0, max - 3) + "...";
    }

    private String defaultText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "--" : DATE_FORMATTER.format(date);
    }
}
