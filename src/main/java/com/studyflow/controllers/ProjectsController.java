package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.Assignment;
import com.studyflow.models.Project;
import com.studyflow.models.User;
import com.studyflow.services.AiAssignmentGeneratorService;
import com.studyflow.services.AssignmentService;
import com.studyflow.services.GitIntegrationService;
import com.studyflow.services.NotificationService;
import com.studyflow.services.ProjectService;
import com.studyflow.utils.CrudViewContext;
import com.studyflow.utils.PdfExportUtil;
import com.studyflow.utils.UserSession;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TextFormatter;
import javafx.geometry.Side;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
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
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    @FXML private Label totalProjectsLabel;
    @FXML private Label inProgressProjectsLabel;
    @FXML private Label completedProjectsLabel;
    @FXML private Label linkedAssignmentsLabel;
    @FXML private Label feedbackLabel;
    @FXML private TextField projectSearchField;
    @FXML private ComboBox<String> projectSortCombo;
    @FXML private Button exportProjectsPdfButton;
    @FXML private Button generateProjectAiTasksButton;
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
    @FXML private TextField shareProjectEmailField;
    @FXML private ComboBox<String> shareProjectRoleCombo;
    @FXML private Button shareProjectButton;
    @FXML private Button showCreateProjectPanelButton;
    @FXML private Button showEditProjectPanelButton;
    @FXML private Button showDeleteProjectPanelButton;
    @FXML private Button showShareProjectPanelButton;
    @FXML private Button showGitProjectPanelButton;
    @FXML private Button showProjectStatsPanelButton;
    @FXML private Button openProjectAiWorkspaceButton;
    @FXML private Button showGitGuidePanelButton;
    @FXML private Button deleteProjectConfirmButton;
    @FXML private VBox projectList;
    @FXML private Label selectedProjectTitleLabel;
    @FXML private Label selectedProjectMetaLabel;
    @FXML private Label selectedProjectCountLabel;
    @FXML private VBox projectAssignmentsList;
    @FXML private Label deleteProjectTitleLabel;
    @FXML private Label deleteProjectMetaLabel;
    @FXML private StackPane projectActionPanelStack;
    @FXML private VBox createProjectPanel;
    @FXML private VBox editProjectPanel;
    @FXML private VBox deleteProjectPanel;
    @FXML private VBox shareProjectPanel;
    @FXML private VBox sharedProjectUsersList;
    @FXML private VBox gitProjectPanel;
    @FXML private VBox statsProjectPanel;
    @FXML private VBox gitGuidePanel;
    @FXML private Label projectGitHintLabel;
    @FXML private Label projectGitStatusLabel;
    @FXML private TextField projectGitRepoPathField;
    @FXML private TextField projectGitRemoteUrlField;
    @FXML private TextField projectGitBranchField;
    @FXML private TextField projectGitUsernameField;
    @FXML private PasswordField projectGitAccessTokenField;
    @FXML private Button saveProjectGitButton;
    @FXML private Button initProjectRepoButton;
    @FXML private Button cloneProjectRepoButton;
    @FXML private Button pullProjectRepoButton;
    @FXML private Button refreshProjectGitStatusButton;
    @FXML private VBox aiLoadingOverlay;
    @FXML private Label aiLoadingTitleLabel;
    @FXML private Label aiLoadingSubtitleLabel;

    private final ProjectService projectService = new ProjectService();
    private final AssignmentService assignmentService = new AssignmentService();
    private final AiAssignmentGeneratorService aiAssignmentGeneratorService = new AiAssignmentGeneratorService();
    private final GitIntegrationService gitIntegrationService = new GitIntegrationService();
    private final NotificationService notificationService = new NotificationService();
    private final com.studyflow.services.ServiceUser userService = new com.studyflow.services.ServiceUser();

    private final List<Project> projects = new ArrayList<>();
    private Project selectedProject;
    private String activePanel = "CREATE";
    private final ContextMenu shareSuggestionsMenu = new ContextMenu();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<String> statuses = List.of("Planned", "In Progress", "On Hold", "Completed");
        createProjectStatusCombo.getItems().addAll(statuses);
        if (updateProjectStatusCombo != null) {
            updateProjectStatusCombo.getItems().addAll(statuses);
        }
        shareProjectRoleCombo.getItems().addAll("viewer", "editor");
        projectSortCombo.getItems().addAll("Newest Added", "Title A-Z", "Start Date", "End Date", "Status");
        projectSortCombo.setValue("Newest Added");
        shareProjectRoleCombo.setValue("viewer");
        projectSearchField.textProperty().addListener((obs, oldValue, newValue) -> renderProjects());
        projectSortCombo.valueProperty().addListener((obs, oldValue, newValue) -> renderProjects());
        bindProjectValidationReset();
        configureDescriptionLimits();
        configureAccessibility();
        configureShareSuggestions();

        if (!isReady()) {
            disableForm();
            return;
        }

        resetCreateForm();
        clearProjectSelection();
        setActivePanel("CREATE");
        CrudViewContext.FlashMessage flashMessage = CrudViewContext.consumeFlashMessage();
        Integer targetSelectionId = CrudViewContext.consumeProjectSelection();
        refreshProjects();
        if (targetSelectionId != null) {
            selectProject(findProjectById(targetSelectionId));
        }
        if (flashMessage != null) {
            showFeedback(flashMessage.message(), flashMessage.error());
        }
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
        Project savedProject = findProjectById(project.getId());
        selectProject(savedProject);
        showFeedback("Project created successfully. Generating AI suggestions...", false);
        if (savedProject != null) {
            generateAiAssignmentsForProject(savedProject, true);
        }
    }

    @FXML
    private void handleUpdateProject() {
        if (!isReady() || selectedProject == null) {
            showFeedback("Select a project before updating it.", true);
            return;
        }
        if (!selectedProject.isOwnedByCurrentUser()) {
            showFeedback("Shared projects are read-only here.", true);
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
    private void handleShareProject() {
        if (!isReady() || selectedProject == null) {
            showFeedback("Select a project before sharing it.", true);
            return;
        }
        if (!selectedProject.isOwnedByCurrentUser()) {
            showFeedback("Only the project owner can share this project.", true);
            return;
        }

        String recipientIdentifier = defaultText(shareProjectEmailField.getText());
        if (recipientIdentifier.isEmpty()) {
            showFeedback("Enter a username, email, phone, or student id before sharing.", true);
            return;
        }

        boolean shared = projectService.shareProjectWithUser(
                selectedProject.getId(),
                getCurrentUser().getId(),
                recipientIdentifier,
                shareProjectRoleCombo.getValue()
        );
        if (!shared) {
            showFeedback("Unable to share this project. Check the user information or whether it was already shared.", true);
            return;
        }

        shareProjectEmailField.clear();
        refreshProjects();
        selectProject(findProjectById(selectedProject.getId()));
        showFeedback("Project shared successfully.", false);
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
    private void handleShowCreatePanel() {
        setActivePanel("CREATE");
    }

    @FXML
    private void handleShowEditPanel() {
        if (!isReady() || selectedProject == null) {
            showFeedback("Select a project before editing it.", true);
            return;
        }
        if (!selectedProject.isOwnedByCurrentUser()) {
            showFeedback("Shared projects are read-only here.", true);
            return;
        }
        openProjectEditWindow();
    }

    @FXML
    private void handleShowDeletePanel() {
        if (!isReady() || selectedProject == null) {
            showFeedback("Select a project before deleting it.", true);
            return;
        }
        if (!selectedProject.isOwnedByCurrentUser()) {
            showFeedback("Shared projects cannot be deleted from your workspace.", true);
            return;
        }
        openProjectDeleteWindow();
    }

    @FXML
    private void handleShowSharePanel() {
        setActivePanel("SHARE");
    }

    @FXML
    private void handleShowGitProjectPanel() {
        setActivePanel("GIT");
    }

    @FXML
    private void handleShowGitGuidePanel() {
        setActivePanel("GIT_GUIDE");
    }

    @FXML
    private void handleShowStatsPanel() {
        setActivePanel("STATS");
    }

    @FXML
    private void handleGenerateProjectAiTasks() {
        if (!isReady() || selectedProject == null) {
            showFeedback("Select a project before generating AI assignments.", true);
            return;
        }
        if (!selectedProject.isOwnedByCurrentUser()) {
            showFeedback("Only your own projects can receive AI-generated assignments.", true);
            return;
        }
        generateAiAssignmentsForProject(selectedProject, false);
    }

    @FXML
    private void handleOpenProjectAiWorkspace() {
        if (!isReady() || selectedProject == null) {
            showFeedback("Select a project before opening the AI workspace.", true);
            return;
        }
        if (openProjectAiWorkspaceButton != null) {
            openProjectAiWorkspaceButton.setDisable(true);
        }
        setAiLoadingState(true, "Opening AI workspace", "Preparing the AI workspace for \"" + selectedProject.getTitle() + "\".");
        CrudViewContext.setProjectContext(selectedProject);
        CrudViewContext.rememberProjectSelection(selectedProject.getId());

        PauseTransition pause = new PauseTransition(Duration.millis(90));
        pause.setOnFinished(event -> {
            try {
                MainController.loadContentInMainArea("views/ProjectAiWorkspace.fxml");
            } finally {
                setAiLoadingState(false, null, null);
                if (openProjectAiWorkspaceButton != null) {
                    openProjectAiWorkspaceButton.setDisable(false);
                }
            }
        });
        pause.play();
    }

    @FXML
    private void handleDeleteSelectedProject() {
        if (!isReady() || selectedProject == null) {
            showFeedback("Select a project before deleting it.", true);
            return;
        }
        if (!selectedProject.isOwnedByCurrentUser()) {
            showFeedback("Shared projects cannot be deleted from your workspace.", true);
            return;
        }
        if (!confirmDeletion("Delete project", "Delete \"" + selectedProject.getTitle() + "\" and remove it from the registry?")) {
            return;
        }
        projectService.delete(selectedProject);
        clearProjectSelection();
        refreshProjects();
        showFeedback("Project deleted successfully.", false);
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
        card.getStyleClass().addAll("card", "crud-item", "project-registry-card");
        if (selectedProject != null && selectedProject.getId() == project.getId()) {
            card.getStyleClass().add("crud-item-selected");
        }
        card.setPadding(new Insets(18));
        card.setAccessibleText("Project card for " + project.getTitle());

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.getStyleClass().add("project-card-top-row");

        VBox titleBox = new VBox(6);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        titleBox.getStyleClass().add("project-card-title-box");

        Label titleLabel = new Label(project.getTitle());
        titleLabel.getStyleClass().addAll("item-title", "project-card-title");

        String ownerMeta = project.isOwnedByCurrentUser() ? "Owned by you" : "Shared by " + safeText(project.getOwnerName());
        Label metaLabel = new Label(formatDate(project.getStartDate()) + " -> " + formatDate(project.getEndDate()) + " | " + ownerMeta);
        metaLabel.getStyleClass().addAll("item-meta", "project-card-meta");
        titleBox.getChildren().addAll(titleLabel, metaLabel);
        topRow.getChildren().add(titleBox);

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getStyleClass().add("project-card-badges");
        badges.getChildren().addAll(
                createBadge(project.getStatus(), project.getStatusStyleClass()),
                createBadge(project.getAssignmentCount() + " assignments", "secondary"),
                createBadge(project.isOwnedByCurrentUser() ? "Owned" : "Shared", project.isOwnedByCurrentUser() ? "primary" : "accent")
        );
        if (!project.isOwnedByCurrentUser() && !isBlank(project.getSharedRole())) {
            badges.getChildren().add(createBadge(capitalize(project.getSharedRole()), "warning"));
        }

        Label descriptionLabel = new Label(defaultText(project.getDescription()));
        descriptionLabel.getStyleClass().addAll("item-desc", "project-card-description");
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
        if (generateProjectAiTasksButton != null) {
            generateProjectAiTasksButton.setDisable(project == null || !project.isOwnedByCurrentUser());
        }
        if (openProjectAiWorkspaceButton != null) {
            openProjectAiWorkspaceButton.setDisable(project == null);
        }
        renderProjects();
        showSelectedProjectDetails(project);
        populateUpdateForm(project);
        populateDeletePanel(project);
        populateSharePanel(project);
        populateGitProjectPanel(project);
    }

    private void showSelectedProjectDetails(Project project) {
        projectAssignmentsList.getChildren().clear();

        if (project == null) {
            selectedProjectTitleLabel.setText("No project selected");
            selectedProjectMetaLabel.setText("Select a project to review its details and assignments.");
            selectedProjectCountLabel.setText("0 assignments");
            projectAssignmentsList.getChildren().add(createEmptyState("Assignments linked to the selected project will appear here."));
            return;
        }

        selectedProjectTitleLabel.setText(project.getTitle());
        selectedProjectMetaLabel.setText(formatDate(project.getStartDate()) + " -> " + formatDate(project.getEndDate())
                + " | " + (project.isOwnedByCurrentUser() ? "Owned by you" : "Shared by " + safeText(project.getOwnerName())));
        selectedProjectCountLabel.setText(project.getAssignmentCount() + " assignments");

        List<Assignment> assignments = assignmentService.getByProjectId(project.getId(), getCurrentUser().getId());
        if (assignments.isEmpty()) {
            projectAssignmentsList.getChildren().add(createEmptyState("No assignments available to you in this project yet."));
            return;
        }

        for (Assignment assignment : assignments) {
            projectAssignmentsList.getChildren().add(createAssignmentPreview(assignment));
        }
    }

    private HBox createAssignmentPreview(Assignment assignment) {
        HBox row = new HBox(12);
        row.getStyleClass().addAll("detail-row", "project-assignment-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 14, 12, 14));

        FontIcon icon = new FontIcon(assignment.isCompleted() ? "fth-check-circle" : "fth-clipboard");
        icon.getStyleClass().addAll("detail-row-icon", "project-assignment-icon");

        VBox textBox = new VBox(4);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        textBox.getStyleClass().add("project-assignment-content");

        Label title = new Label(assignment.getTitle());
        title.getStyleClass().addAll("item-title", "project-assignment-title");

        String sharedMeta = assignment.isOwnedByCurrentUser() ? "Owned by you" : "Shared by " + safeText(assignment.getOwnerName());
        Label meta = new Label(assignment.getStatus() + " | " + assignment.getPriority() + " | due "
                + formatDate(assignment.getEndDate()) + " | " + sharedMeta);
        meta.getStyleClass().addAll("item-meta", "project-assignment-meta");
        textBox.getChildren().addAll(title, meta);

        row.getChildren().addAll(icon, textBox);
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
        if (updateProjectHintLabel == null
                || updateProjectTitleField == null
                || updateProjectDescriptionArea == null
                || updateProjectStartDatePicker == null
                || updateProjectEndDatePicker == null
                || updateProjectStatusCombo == null
                || updateProjectButton == null) {
            return;
        }
        boolean hasSelection = project != null;
        boolean editable = hasSelection && project.isOwnedByCurrentUser();
        if (updateProjectCard != null) {
            updateProjectCard.setDisable(!hasSelection);
            updateProjectCard.setOpacity(hasSelection ? 1 : 0.55);
        }
        clearUpdateValidation();

        if (!hasSelection) {
            updateProjectHintLabel.setText("Select a project from the registry to edit it here.");
            updateProjectTitleField.clear();
            updateProjectDescriptionArea.clear();
            updateProjectStartDatePicker.setValue(null);
            updateProjectEndDatePicker.setValue(null);
            updateProjectStatusCombo.setValue("Planned");
            shareProjectEmailField.clear();
            shareProjectRoleCombo.setValue("viewer");
            shareProjectEmailField.setDisable(true);
            shareProjectRoleCombo.setDisable(true);
            shareProjectButton.setDisable(true);
            return;
        }

        updateProjectHintLabel.setText(editable
                ? "Updating project #" + project.getId() + "."
                : "This project was shared with you and is read-only.");
        updateProjectTitleField.setText(project.getTitle());
        updateProjectDescriptionArea.setText(defaultText(project.getDescription()));
        updateProjectStartDatePicker.setValue(project.getStartDate());
        updateProjectEndDatePicker.setValue(project.getEndDate());
        updateProjectStatusCombo.setValue(project.getStatus());
        updateProjectTitleField.setDisable(!editable);
        updateProjectDescriptionArea.setDisable(!editable);
        updateProjectStartDatePicker.setDisable(!editable);
        updateProjectEndDatePicker.setDisable(!editable);
        updateProjectStatusCombo.setDisable(!editable);
        updateProjectButton.setDisable(!editable);
        shareProjectEmailField.clear();
        shareProjectRoleCombo.setValue("viewer");
        shareProjectEmailField.setDisable(!editable);
        shareProjectRoleCombo.setDisable(!editable);
        shareProjectButton.setDisable(!editable);
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
        if (generateProjectAiTasksButton != null) {
            generateProjectAiTasksButton.setDisable(true);
        }
        if (openProjectAiWorkspaceButton != null) {
            openProjectAiWorkspaceButton.setDisable(true);
        }
        populateUpdateForm(null);
        showSelectedProjectDetails(null);
        populateDeletePanel(null);
        populateSharePanel(null);
        populateGitProjectPanel(null);
    }

    private void populateDeletePanel(Project project) {
        if (deleteProjectTitleLabel == null || deleteProjectMetaLabel == null || deleteProjectConfirmButton == null) {
            return;
        }
        if (project == null) {
            deleteProjectTitleLabel.setText("No project selected");
            deleteProjectMetaLabel.setText("Select one project from the registry first.");
            deleteProjectConfirmButton.setDisable(true);
            return;
        }
        deleteProjectTitleLabel.setText(project.getTitle());
        deleteProjectMetaLabel.setText(formatDate(project.getStartDate()) + " -> " + formatDate(project.getEndDate()));
        deleteProjectConfirmButton.setDisable(!project.isOwnedByCurrentUser());
    }

    private void populateSharePanel(Project project) {
        if (shareProjectEmailField == null || shareProjectRoleCombo == null || shareProjectButton == null) {
            return;
        }
        boolean enabled = project != null && project.isOwnedByCurrentUser();
        shareProjectEmailField.clear();
        shareProjectRoleCombo.setValue("viewer");
        shareProjectEmailField.setDisable(!enabled);
        shareProjectRoleCombo.setDisable(!enabled);
        shareProjectButton.setDisable(!enabled);
        renderSharedProjectUsers(project);
    }

    private void renderSharedProjectUsers(Project project) {
        if (sharedProjectUsersList == null) {
            return;
        }
        sharedProjectUsersList.getChildren().clear();

        if (project == null) {
            sharedProjectUsersList.getChildren().add(createSharedUserEmptyState("Select a project to view shared users."));
            return;
        }

        List<User> sharedUsers = projectService.getSharedUsers(project.getId());
        if (sharedUsers.isEmpty()) {
            sharedProjectUsersList.getChildren().add(createSharedUserEmptyState("This project is not shared with anyone yet."));
            return;
        }

        for (User sharedUser : sharedUsers) {
            sharedProjectUsersList.getChildren().add(createSharedUserRow(sharedUser));
        }
    }

    private VBox createSharedUserRow(User user) {
        VBox row = new VBox(2);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.getStyleClass().add("detail-row");

        Label nameLabel = new Label(buildSharedUserName(user));
        nameLabel.getStyleClass().add("form-label");
        nameLabel.setWrapText(true);

        Label metaLabel = new Label(buildSharedUserMeta(user));
        metaLabel.getStyleClass().addAll("item-desc", "text-muted");
        metaLabel.setWrapText(true);

        row.getChildren().addAll(nameLabel, metaLabel);
        return row;
    }

    private Label createSharedUserEmptyState(String message) {
        Label label = new Label(message);
        label.getStyleClass().addAll("item-desc", "text-muted");
        label.setWrapText(true);
        return label;
    }

    private String buildSharedUserName(User user) {
        String fullName = defaultText(user.getFullName()).trim();
        if (!fullName.isEmpty()) {
            return fullName;
        }
        String username = defaultText(user.getUsername()).trim();
        if (!username.isEmpty()) {
            return username;
        }
        return defaultText(user.getEmail()).trim();
    }

    private String buildSharedUserMeta(User user) {
        List<String> details = new ArrayList<>();
        String username = defaultText(user.getUsername()).trim();
        String email = defaultText(user.getEmail()).trim();
        String studentId = defaultText(user.getStudentId()).trim();

        if (!username.isEmpty()) {
            details.add("@" + username);
        }
        if (!email.isEmpty()) {
            details.add(email);
        }
        if (!studentId.isEmpty()) {
            details.add("ID " + studentId);
        }

        if (details.isEmpty()) {
            return "User details unavailable";
        }
        return String.join(" | ", details);
    }

    private void populateGitProjectPanel(Project project) {
        if (projectGitHintLabel == null
                || projectGitStatusLabel == null
                || projectGitRepoPathField == null
                || projectGitRemoteUrlField == null
                || projectGitBranchField == null
                || projectGitUsernameField == null
                || projectGitAccessTokenField == null
                || saveProjectGitButton == null
                || initProjectRepoButton == null
                || cloneProjectRepoButton == null
                || pullProjectRepoButton == null
                || refreshProjectGitStatusButton == null) {
            return;
        }

        boolean hasSelection = project != null;
        boolean canUseGit = hasSelection && project.canCurrentUserUseGit();
        boolean canEditRemote = hasSelection && project.isOwnedByCurrentUser();

        if (!hasSelection) {
            projectGitHintLabel.setText("Select a project to configure Git access.");
            projectGitStatusLabel.setText("No repository linked");
            projectGitRepoPathField.clear();
            projectGitRemoteUrlField.clear();
            projectGitBranchField.setText("main");
            projectGitUsernameField.clear();
            projectGitAccessTokenField.clear();
        } else {
            projectGitHintLabel.setText(canUseGit
                    ? (canEditRemote
                    ? "Link a repository, clone a remote repo, or pull updates for \"" + project.getTitle() + "\"."
                    : "This repo URL is shared from the project owner. Add your local repo path, username, token, and branch for your own workspace.")
                    : "You need editor access on this shared project to use Git.");
            projectGitStatusLabel.setText(isBlank(project.getGitLastStatusSummary()) ? "Repository status not checked yet" : project.getGitLastStatusSummary());
            projectGitRepoPathField.setText(defaultText(project.getGitRepoPath()));
            projectGitRemoteUrlField.setText(defaultText(project.getGitRemoteUrl()));
            projectGitBranchField.setText(isBlank(project.getGitDefaultBranch()) ? "main" : project.getGitDefaultBranch());
            projectGitUsernameField.setText(defaultText(project.getGitUsername()));
            projectGitAccessTokenField.setText(defaultText(project.getGitAccessToken()));
        }

        projectGitRepoPathField.setDisable(!canUseGit);
        projectGitRemoteUrlField.setDisable(!canEditRemote);
        projectGitBranchField.setDisable(!canUseGit);
        projectGitUsernameField.setDisable(!canUseGit);
        projectGitAccessTokenField.setDisable(!canUseGit);
        saveProjectGitButton.setDisable(!canUseGit);
        initProjectRepoButton.setDisable(!canUseGit);
        cloneProjectRepoButton.setDisable(!canUseGit);
        pullProjectRepoButton.setDisable(!canUseGit);
        refreshProjectGitStatusButton.setDisable(!canUseGit);
    }
    private void setActivePanel(String panel) {
        activePanel = panel;
        togglePanel(createProjectPanel, "CREATE".equals(panel));
        togglePanel(editProjectPanel, false);
        togglePanel(deleteProjectPanel, false);
        togglePanel(shareProjectPanel, "SHARE".equals(panel));
        togglePanel(gitProjectPanel, "GIT".equals(panel));
        togglePanel(statsProjectPanel, "STATS".equals(panel));
        togglePanel(gitGuidePanel, "GIT_GUIDE".equals(panel));
        updatePanelButton(showCreateProjectPanelButton, "CREATE".equals(panel));
        updatePanelButton(showEditProjectPanelButton, false);
        updatePanelButton(showDeleteProjectPanelButton, false);
        updatePanelButton(showShareProjectPanelButton, "SHARE".equals(panel));
        updatePanelButton(showGitProjectPanelButton, "GIT".equals(panel));
        updatePanelButton(showProjectStatsPanelButton, "STATS".equals(panel));
        updatePanelButton(showGitGuidePanelButton, "GIT_GUIDE".equals(panel));
    }

    private void togglePanel(VBox panel, boolean visible) {
        if (panel == null) {
            return;
        }
        panel.setVisible(visible);
        panel.setManaged(visible);
    }

    private void updatePanelButton(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.getStyleClass().remove("active");
        if (active) {
            button.getStyleClass().add("active");
        }
    }

    private void generateAiAssignmentsForProject(Project project, boolean autoTriggered) {
        if (project == null) {
            return;
        }
        if (!aiAssignmentGeneratorService.isConfigured()) {
            showFeedback("AI assignments are unavailable. Set OPENROUTER_API_KEY first.", true);
            return;
        }

        if (generateProjectAiTasksButton != null) {
            generateProjectAiTasksButton.setDisable(true);
        }
        if (createProjectButton != null && autoTriggered) {
            createProjectButton.setDisable(true);
        }
        setAiLoadingState(true, "Generating assignments", "Building AI task suggestions for \"" + project.getTitle() + "\".");
        showFeedback("Generating AI assignment suggestions for \"" + project.getTitle() + "\"...", false);

        Task<List<Assignment>> task = new Task<>() {
            @Override
            protected List<Assignment> call() {
                return aiAssignmentGeneratorService.generateSuggestionsForProject(project, getCurrentUser().getId());
            }
        };

        task.setOnSucceeded(event -> {
            setAiLoadingState(false, null, null);
            if (generateProjectAiTasksButton != null) {
                generateProjectAiTasksButton.setDisable(false);
            }
            if (createProjectButton != null) {
                createProjectButton.setDisable(false);
            }

            CrudViewContext.setAiAssignmentSuggestions(task.getValue(), project, "views/Projects.fxml");
            CrudViewContext.rememberProjectSelection(project.getId());
            MainController.loadContentInMainArea("views/AiAssignmentReview.fxml");
        });

        task.setOnFailed(event -> {
            setAiLoadingState(false, null, null);
            if (generateProjectAiTasksButton != null) {
                generateProjectAiTasksButton.setDisable(false);
            }
            if (createProjectButton != null) {
                createProjectButton.setDisable(false);
            }
            Throwable error = task.getException();
            showFeedback("AI generation failed: " + (error == null ? "unknown error" : error.getMessage()), true);
        });

        Thread thread = new Thread(task, "project-ai-assignment-generator");
        thread.setDaemon(true);
        thread.start();
    }

    private void setAiLoadingState(boolean loading, String title, String subtitle) {
        if (aiLoadingOverlay != null) {
            if (loading) {
                aiLoadingOverlay.toFront();
            }
            aiLoadingOverlay.setVisible(loading);
            aiLoadingOverlay.setManaged(loading);
        }
        if (aiLoadingTitleLabel != null && title != null && !title.isBlank()) {
            aiLoadingTitleLabel.setText(title);
        }
        if (aiLoadingSubtitleLabel != null && subtitle != null && !subtitle.isBlank()) {
            aiLoadingSubtitleLabel.setText(subtitle);
        }
    }

    @FXML
    private void handleSaveProjectGitSettings() {
        if (!isReady() || selectedProject == null) {
            showFeedback("Select a project before saving Git settings.", true);
            return;
        }
        if (!selectedProject.canCurrentUserUseGit()) {
            showFeedback("You need editor access on this shared project to save Git settings.", true);
            return;
        }
        applyProjectGitFields();
        selectedProject.setCurrentUserId(getCurrentUser().getId());
        projectService.updateGitSettings(selectedProject);
        if (gitIntegrationService.isRepositoryConfigured(selectedProject) && !isBlank(selectedProject.getGitRemoteUrl())) {
            GitIntegrationService.GitOperationResult syncResult = gitIntegrationService.syncRemote(selectedProject);
            selectedProject.setGitLastStatusSummary(syncResult.details());
            selectedProject.setGitLastSyncAt(syncResult.completedAt());
            selectedProject.setCurrentUserId(getCurrentUser().getId());
            projectService.updateGitSettings(selectedProject);
            if (!syncResult.success()) {
                refreshProjects();
                selectProject(findProjectById(selectedProject.getId()));
                showFeedback(syncResult.summary(), true);
                return;
            }
        }
        refreshProjects();
        selectProject(findProjectById(selectedProject.getId()));
        showFeedback("Project Git settings saved.", false);
    }

    @FXML
    private void handleInitProjectRepository() {
        runProjectGitTask("Initializing repository...", project -> gitIntegrationService.initializeRepository(project));
    }

    @FXML
    private void handleCloneProjectRepository() {
        runProjectGitTask("Cloning repository...", project -> gitIntegrationService.cloneRepository(project));
    }

    @FXML
    private void handlePullProjectRepository() {
        runProjectGitTask("Pulling repository updates...", project -> gitIntegrationService.pull(project));
    }

    @FXML
    private void handleRefreshProjectGitStatus() {
        runProjectGitTask("Refreshing repository status...", project -> gitIntegrationService.describeStatus(project));
    }

    private void applyProjectGitFields() {
        if (selectedProject == null) {
            return;
        }
        selectedProject.setGitRepoPath(defaultText(projectGitRepoPathField == null ? null : projectGitRepoPathField.getText()));
        selectedProject.setGitRemoteUrl(defaultText(projectGitRemoteUrlField == null ? null : projectGitRemoteUrlField.getText()));
        selectedProject.setGitDefaultBranch(defaultText(projectGitBranchField == null ? null : projectGitBranchField.getText()));
        selectedProject.setGitUsername(defaultText(projectGitUsernameField == null ? null : projectGitUsernameField.getText()));
        selectedProject.setGitAccessToken(defaultText(projectGitAccessTokenField == null ? null : projectGitAccessTokenField.getText()));
    }

    private void runProjectGitTask(String loadingMessage, java.util.function.Function<Project, GitIntegrationService.GitOperationResult> action) {
        if (!isReady() || selectedProject == null) {
            showFeedback("Select a project before using Git actions.", true);
            return;
        }
        if (!selectedProject.canCurrentUserUseGit()) {
            showFeedback("You need editor access on this shared project to use Git actions.", true);
            return;
        }

        applyProjectGitFields();
        selectedProject.setCurrentUserId(getCurrentUser().getId());
        projectService.updateGitSettings(selectedProject);
        setAiLoadingState(true, "Git workspace", loadingMessage);

        Task<GitIntegrationService.GitOperationResult> task = new Task<>() {
            @Override
            protected GitIntegrationService.GitOperationResult call() {
                return action.apply(selectedProject);
            }
        };

        task.setOnSucceeded(event -> {
            setAiLoadingState(false, null, null);
            GitIntegrationService.GitOperationResult result = task.getValue();
            selectedProject.setGitLastStatusSummary(result.details());
            selectedProject.setGitLastSyncAt(result.completedAt());
            selectedProject.setCurrentUserId(getCurrentUser().getId());
            projectService.updateGitSettings(selectedProject);
            int selectedId = selectedProject.getId();
            refreshProjects();
            selectProject(findProjectById(selectedId));
            showFeedback(result.summary(), !result.success());
        });

        task.setOnFailed(event -> {
            setAiLoadingState(false, null, null);
            Throwable error = task.getException();
            showFeedback("Git action failed: " + (error == null ? "unknown error" : error.getMessage()), true);
        });

        Thread thread = new Thread(task, "project-git-task");
        thread.setDaemon(true);
        thread.start();
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
        if (createProjectDescriptionArea != null && createProjectDescriptionArea.getText() != null
                && createProjectDescriptionArea.getText().length() > MAX_DESCRIPTION_LENGTH) {
            showFeedback("Project description cannot exceed 500 characters.", true);
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
                || safeText(project.getOwnerName()).toLowerCase().contains(query)
                || formatDate(project.getStartDate()).toLowerCase().contains(query)
                || formatDate(project.getEndDate()).toLowerCase().contains(query);
    }

    private void bindProjectValidationReset() {
        createProjectTitleField.textProperty().addListener((obs, oldValue, newValue) -> clearFieldError(createProjectTitleField, createProjectTitleError));
        createProjectStartDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> clearFieldError(createProjectStartDatePicker, createProjectStartDateError));
        createProjectEndDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> clearFieldError(createProjectEndDatePicker, createProjectEndDateError));
        if (updateProjectTitleField != null && updateProjectTitleError != null) {
            updateProjectTitleField.textProperty().addListener((obs, oldValue, newValue) -> clearFieldError(updateProjectTitleField, updateProjectTitleError));
        }
        if (updateProjectStartDatePicker != null && updateProjectStartDateError != null) {
            updateProjectStartDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> clearFieldError(updateProjectStartDatePicker, updateProjectStartDateError));
        }
        if (updateProjectEndDatePicker != null && updateProjectEndDateError != null) {
            updateProjectEndDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> clearFieldError(updateProjectEndDatePicker, updateProjectEndDateError));
        }
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
        if (updateProjectTitleField != null) {
            updateProjectTitleField.setAccessibleText("Selected project title");
        }
        if (updateProjectStartDatePicker != null) {
            updateProjectStartDatePicker.setAccessibleText("Selected project start date");
        }
        if (updateProjectEndDatePicker != null) {
            updateProjectEndDatePicker.setAccessibleText("Selected project end date");
        }
        if (updateProjectStatusCombo != null) {
            updateProjectStatusCombo.setAccessibleText("Selected project status");
        }
        if (updateProjectDescriptionArea != null) {
            updateProjectDescriptionArea.setAccessibleText("Selected project description");
        }
        shareProjectEmailField.setAccessibleText("Share project using username, email, phone, or student id");
        shareProjectRoleCombo.setAccessibleText("Shared project role");
    }

    private void configureDescriptionLimits() {
        createProjectDescriptionArea.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= MAX_DESCRIPTION_LENGTH ? change : null));
        if (updateProjectDescriptionArea != null) {
            updateProjectDescriptionArea.setTextFormatter(new TextFormatter<String>(change ->
                    change.getControlNewText().length() <= MAX_DESCRIPTION_LENGTH ? change : null));
        }
    }

    private void configureShareSuggestions() {
        if (shareProjectEmailField == null) {
            return;
        }

        shareProjectEmailField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                if (isBlank(newVal) || userService == null || !userService.isDatabaseAvailable()) {
                    shareSuggestionsMenu.hide();
                    return;
                }
                User currentUser = getCurrentUser();
                List<User> matches = userService.searchUsers(newVal, 8, currentUser == null ? null : currentUser.getId());
                if (matches.isEmpty()) {
                    shareSuggestionsMenu.hide();
                    return;
                }
                List<CustomMenuItem> items = new ArrayList<>();
                for (User user : matches) {
                    String title = isBlank(user.getUsername()) ? safeText(user.getFullName()).trim() : safeText(user.getUsername());
                    String subtitle = buildUserSuggestionSubtitle(user);
                    Label itemLabel = new Label(title + (subtitle.isEmpty() ? "" : " - " + subtitle));
                    itemLabel.getStyleClass().add("autocomplete-item");
                    CustomMenuItem item = new CustomMenuItem(itemLabel, true);
                    item.setOnAction(event -> {
                        String identifier = preferredUserIdentifier(user);
                        shareProjectEmailField.setText(identifier);
                        shareProjectEmailField.positionCaret(identifier.length());
                        shareSuggestionsMenu.hide();
                    });
                    items.add(item);
                }
                shareSuggestionsMenu.getItems().setAll(items);
                if (!shareSuggestionsMenu.isShowing()) {
                    shareSuggestionsMenu.show(shareProjectEmailField, Side.BOTTOM, 0, 0);
                }
            } catch (Exception ex) {
                shareSuggestionsMenu.hide();
            }
        });

        shareProjectEmailField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                shareSuggestionsMenu.hide();
            }
        });
    }

    private void openProjectEditWindow() {
        CrudViewContext.setProjectContext(selectedProject);
        CrudViewContext.rememberProjectSelection(selectedProject == null ? null : selectedProject.getId());
        MainController.loadContentInMainArea("views/ProjectEditDialog.fxml");
    }

    private void openProjectDeleteWindow() {
        CrudViewContext.setProjectContext(selectedProject);
        MainController.loadContentInMainArea("views/ProjectDeleteDialog.fxml");
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
        if (generateProjectAiTasksButton != null) {
            generateProjectAiTasksButton.setDisable(true);
        }
        if (openProjectAiWorkspaceButton != null) {
            openProjectAiWorkspaceButton.setDisable(true);
        }
        if (updateProjectCard != null) {
            updateProjectCard.setDisable(true);
        }
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

    private String preferredUserIdentifier(User user) {
        if (!isBlank(user.getEmail())) {
            return safeText(user.getEmail());
        }
        if (!isBlank(user.getUsername())) {
            return safeText(user.getUsername());
        }
        if (!isBlank(user.getStudentId())) {
            return safeText(user.getStudentId());
        }
        return safeText(user.getPhoneNumber());
    }

    private String buildUserSuggestionSubtitle(User user) {
        List<String> parts = new ArrayList<>();
        String fullName = safeText(user.getFullName()).trim();
        if (!fullName.isEmpty()) {
            parts.add(fullName);
        }
        if (!isBlank(user.getEmail())) {
            parts.add(user.getEmail());
        } else if (!isBlank(user.getStudentId())) {
            parts.add(user.getStudentId());
        } else if (!isBlank(user.getPhoneNumber())) {
            parts.add(user.getPhoneNumber());
        }
        return String.join(" | ", parts);
    }


    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String capitalize(String value) {
        if (isBlank(value)) {
            return "";
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "--" : DATE_FORMATTER.format(date);
    }
}
