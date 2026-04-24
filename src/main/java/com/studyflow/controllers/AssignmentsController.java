package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.Assignment;
import com.studyflow.models.AssignmentComment;
import com.studyflow.models.Project;
import com.studyflow.models.User;
import com.studyflow.services.AiAssignmentGeneratorService;
import com.studyflow.services.AiProjectInsightsService;
import com.studyflow.services.AssignmentService;
import com.studyflow.services.GitIntegrationService;
import com.studyflow.services.NotificationService;
import com.studyflow.services.ProjectService;
import com.studyflow.utils.CrudViewContext;
import com.studyflow.utils.PdfExportUtil;
import com.studyflow.utils.UserSession;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AssignmentsController implements Initializable {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter PDF_FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    @FXML private Label totalAssignmentsLabel;
    @FXML private Label overdueLabel;
    @FXML private Label inProgressLabel;
    @FXML private Label reviewLabel;
    @FXML private Label completedLabel;
    @FXML private Label feedbackLabel;
    @FXML private Label createAssignmentHintLabel;
    @FXML private Label selectedAssignmentTitleLabel;
    @FXML private Label selectedAssignmentMetaLabel;
    @FXML private Label todoCountLabel;
    @FXML private Label progressCountLabel;
    @FXML private Label reviewCountLabel;
    @FXML private Label doneCountLabel;
    @FXML private Label deleteAssignmentTitleLabel;
    @FXML private Label deleteAssignmentMetaLabel;
    @FXML private Label commentsAssignmentLabel;
    @FXML private TextField assignmentSearchField;
    @FXML private ComboBox<String> assignmentSortCombo;
    @FXML private Button exportAssignmentsPdfButton;
    @FXML private PieChart assignmentStatusChart;
    @FXML private BarChart<String, Number> assignmentPriorityChart;
    @FXML private TextField createAssignmentTitleField;
    @FXML private Label createAssignmentTitleError;
    @FXML private ComboBox<Project> createAssignmentProjectCombo;
    @FXML private Label createAssignmentProjectError;
    @FXML private ComboBox<String> createAssignmentPriorityCombo;
    @FXML private ComboBox<String> createAssignmentStatusCombo;
    @FXML private DatePicker createAssignmentStartDatePicker;
    @FXML private Label createAssignmentStartDateError;
    @FXML private ComboBox<String> createAssignmentStartTimeCombo;
    @FXML private DatePicker createAssignmentEndDatePicker;
    @FXML private Label createAssignmentEndDateError;
    @FXML private ComboBox<String> createAssignmentEndTimeCombo;
    @FXML private TextArea createAssignmentDescriptionArea;
    @FXML private Button createAssignmentButton;
    @FXML private Button generateAiAssignmentsButton;
    @FXML private Button clearCreateAssignmentButton;
    @FXML private Label updateAssignmentHintLabel;
    @FXML private TextField updateAssignmentTitleField;
    @FXML private Label updateAssignmentTitleError;
    @FXML private ComboBox<Project> updateAssignmentProjectCombo;
    @FXML private Label updateAssignmentProjectError;
    @FXML private ComboBox<String> updateAssignmentPriorityCombo;
    @FXML private ComboBox<String> updateAssignmentStatusCombo;
    @FXML private DatePicker updateAssignmentStartDatePicker;
    @FXML private Label updateAssignmentStartDateError;
    @FXML private DatePicker updateAssignmentEndDatePicker;
    @FXML private Label updateAssignmentEndDateError;
    @FXML private TextArea updateAssignmentDescriptionArea;
    @FXML private Button updateAssignmentButton;
    @FXML private Button clearAssignmentSelectionButton;
    @FXML private TextField shareAssignmentEmailField;
    @FXML private Button shareAssignmentButton;
    @FXML private Button showCreatePanelButton;
    @FXML private Button showEditPanelButton;
    @FXML private Button showDeletePanelButton;
    @FXML private Button showCommentsPanelButton;
    @FXML private Button showSharePanelButton;
    @FXML private Button showGitPanelButton;
    @FXML private Button showStatsPanelButton;
    @FXML private Button addCommentButton;
    @FXML private Button deleteAssignmentConfirmButton;
    @FXML private Label taskCountLabel;
    @FXML private VBox assignmentsList;
    @FXML private VBox todoColumn;
    @FXML private VBox progressColumn;
    @FXML private VBox reviewColumn;
    @FXML private VBox doneColumn;
    @FXML private VBox createPanel;
    @FXML private VBox editPanel;
    @FXML private VBox deletePanel;
    @FXML private VBox commentsPanel;
    @FXML private VBox sharePanel;
    @FXML private VBox sharedAssignmentUsersList;
    @FXML private VBox gitPanel;
    @FXML private VBox statsPanel;
    @FXML private StackPane actionPanelStack;
    @FXML private VBox commentsList;
    @FXML private TextArea commentInputArea;
    @FXML private Label assignmentGitHintLabel;
    @FXML private Label assignmentGitRepoLabel;
    @FXML private Label assignmentGitStatusLabel;
    @FXML private Label assignmentGitLastCommitLabel;
    @FXML private TextField assignmentGitCommitMessageField;
    @FXML private TextField assignmentGitPathspecField;
    @FXML private Button saveAssignmentGitButton;
    @FXML private Button refreshAssignmentGitButton;
    @FXML private Button commitAssignmentGitButton;
    @FXML private Button commitAndPushAssignmentGitButton;
    @FXML private VBox aiLoadingOverlay;
    @FXML private Label aiLoadingTitleLabel;
    @FXML private Label aiLoadingSubtitleLabel;

    private final AssignmentService assignmentService = new AssignmentService();
    private final ProjectService projectService = new ProjectService();
    private final AiAssignmentGeneratorService aiAssignmentGeneratorService = new AiAssignmentGeneratorService();
    private final AiProjectInsightsService aiProjectInsightsService = new AiProjectInsightsService();
    private final GitIntegrationService gitIntegrationService = new GitIntegrationService();
    private final NotificationService notificationService = new NotificationService();
    private final com.studyflow.services.ServiceUser userService = new com.studyflow.services.ServiceUser();

    private final List<Assignment> assignments = new ArrayList<>();
    private final List<Project> projects = new ArrayList<>();
    private final List<Project> ownedProjects = new ArrayList<>();
    private Assignment selectedAssignment;
    private String activePanel = "CREATE";
    private Integer draggedAssignmentId;
    private String activeFilter = "ALL";
    private final ContextMenu shareSuggestionsMenu = new ContextMenu();
    private boolean loadingData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<String> priorities = List.of("High", "Medium", "Low");
        List<String> statuses = List.of("To Do", "In Progress", "Review", "Completed");

        configureProjectCombo(createAssignmentProjectCombo);
        configureProjectCombo(updateAssignmentProjectCombo);
        createAssignmentPriorityCombo.getItems().addAll(priorities);
        if (updateAssignmentPriorityCombo != null) {
            updateAssignmentPriorityCombo.getItems().addAll(priorities);
        }
        createAssignmentStatusCombo.getItems().addAll(statuses);
        if (updateAssignmentStatusCombo != null) {
            updateAssignmentStatusCombo.getItems().addAll(statuses);
        }
        createAssignmentStartTimeCombo.getItems().addAll(buildTimeOptions());
        createAssignmentEndTimeCombo.getItems().addAll(buildTimeOptions());
        assignmentSortCombo.getItems().addAll("Newest Added", "Title A-Z", "Due Date", "Priority", "Status");
        assignmentSortCombo.setValue("Newest Added");

        assignmentSearchField.textProperty().addListener((obs, oldValue, newValue) -> renderAssignments());
        assignmentSortCombo.valueProperty().addListener((obs, oldValue, newValue) -> renderAssignments());
        bindAssignmentValidationReset();
        configureDescriptionLimits();
        configureAccessibility();
        configureBoardDropTargets();
        configureShareSuggestions();

        if (!isReady()) {
            disableForm();
            return;
        }

        resetCreateForm();
        clearAssignmentSelection();
        setActivePanel("CREATE");
        Integer targetSelectionId = CrudViewContext.consumeAssignmentSelection();
        CrudViewContext.FlashMessage flashMessage = CrudViewContext.consumeFlashMessage();
        refreshData(targetSelectionId, () -> {
            if (flashMessage != null) {
                showFeedback(flashMessage.message(), flashMessage.error());
            }
        });
    }

    @FXML
    private void handleCreateAssignment() {
        if (!isReady()) {
            return;
        }

        clearCreateValidation();
        if (!validateAssignmentForm(
                createAssignmentProjectCombo, createAssignmentProjectError,
                createAssignmentTitleField, createAssignmentTitleError,
                createAssignmentStartDatePicker, createAssignmentStartDateError,
                createAssignmentStartTimeCombo,
                createAssignmentEndDatePicker, createAssignmentEndDateError,
                createAssignmentEndTimeCombo,
                createAssignmentDescriptionArea)) {
            showFeedback("Please fix the highlighted assignment fields.", true);
            return;
        }

        Project project = createAssignmentProjectCombo.getValue();
        Assignment assignment = new Assignment();
        assignment.setUserId(getCurrentUser().getId());
        assignment.setProjectId(project.getId());
        assignment.setProjectTitle(project.getTitle());
        assignment.setTitle(createAssignmentTitleField.getText().trim());
        assignment.setDescription(defaultText(createAssignmentDescriptionArea.getText()));
        assignment.setStartDate(createAssignmentStartDatePicker.getValue());
        assignment.setStartTime(parseTime(createAssignmentStartTimeCombo.getValue(), LocalTime.of(9, 0)));
        assignment.setEndDate(createAssignmentEndDatePicker.getValue());
        assignment.setEndTime(parseTime(createAssignmentEndTimeCombo.getValue(), LocalTime.of(13, 0)));
        assignment.setPriority(defaultPriority(createAssignmentPriorityCombo.getValue()));
        assignment.setStatus(defaultStatus(createAssignmentStatusCombo.getValue()));
        applyPlanningEstimate(assignment);

        assignmentService.add(assignment);
        resetCreateForm();
        refreshData(assignment.getId(), () -> showFeedback(buildPlanningMessage("Assignment created successfully.", assignment), false));
    }

    @FXML
    private void handleGenerateAiAssignments() {
        if (!isReady()) {
            return;
        }
        Project project = createAssignmentProjectCombo == null ? null : createAssignmentProjectCombo.getValue();
        if (project == null) {
            showFeedback("Choose a project before generating AI assignments.", true);
            return;
        }
        if (!project.isOwnedByCurrentUser()) {
            showFeedback("AI generation is available only for your own projects.", true);
            return;
        }
        generateAiAssignmentsForProject(project);
    }

    @FXML
    private void handleUpdateAssignment() {
        if (!isReady() || selectedAssignment == null) {
            showFeedback("Select an assignment before updating it.", true);
            return;
        }
        if (!selectedAssignment.canCurrentUserEdit()) {
            showFeedback("You need editor access on this shared project to update the assignment.", true);
            return;
        }

        clearUpdateValidation();
        if (!validateAssignmentForm(
                updateAssignmentProjectCombo, updateAssignmentProjectError,
                updateAssignmentTitleField, updateAssignmentTitleError,
                updateAssignmentStartDatePicker, updateAssignmentStartDateError,
                null,
                updateAssignmentEndDatePicker, updateAssignmentEndDateError,
                null,
                updateAssignmentDescriptionArea)) {
            showFeedback("Please fix the highlighted assignment fields.", true);
            return;
        }

        Project project = updateAssignmentProjectCombo.getValue();
        selectedAssignment.setCurrentUserId(getCurrentUser().getId());
        selectedAssignment.setProjectId(project.getId());
        selectedAssignment.setProjectTitle(project.getTitle());
        selectedAssignment.setTitle(updateAssignmentTitleField.getText().trim());
        selectedAssignment.setDescription(defaultText(updateAssignmentDescriptionArea.getText()));
        selectedAssignment.setStartDate(updateAssignmentStartDatePicker.getValue());
        if (selectedAssignment.getStartTime() == null) {
            selectedAssignment.setStartTime(LocalTime.of(9, 0));
        }
        selectedAssignment.setEndDate(updateAssignmentEndDatePicker.getValue());
        if (selectedAssignment.getEndTime() == null) {
            selectedAssignment.setEndTime(LocalTime.of(13, 0));
        }
        selectedAssignment.setPriority(defaultPriority(updateAssignmentPriorityCombo.getValue()));
        selectedAssignment.setStatus(defaultStatus(updateAssignmentStatusCombo.getValue()));
        applyPlanningEstimate(selectedAssignment);

        assignmentService.update(selectedAssignment);
        int selectedId = selectedAssignment.getId();
        refreshData(selectedId, () -> showFeedback(buildPlanningMessage("Assignment updated successfully.", selectedAssignment), false));
    }

    @FXML
    private void handleShareAssignment() {
        if (!isReady() || selectedAssignment == null) {
            showFeedback("Select an assignment before sharing it.", true);
            return;
        }
        if (!selectedAssignment.isOwnedByCurrentUser()) {
            showFeedback("Only the assignment owner can share this assignment.", true);
            return;
        }

        String recipientIdentifier = defaultText(shareAssignmentEmailField.getText());
        if (recipientIdentifier.isEmpty()) {
            showFeedback("Enter a username, email, phone, or student id before sharing.", true);
            return;
        }

        boolean shared = assignmentService.shareAssignmentWithUser(selectedAssignment.getId(), getCurrentUser().getId(), recipientIdentifier);
        if (!shared) {
            showFeedback("Unable to share this assignment. Share the project first and verify the user information.", true);
            return;
        }

        shareAssignmentEmailField.clear();
        refreshData(selectedAssignment.getId(), () -> showFeedback("Assignment shared successfully.", false));
    }

    @FXML
    private void handleResetCreateAssignment() {
        resetCreateForm();
        showFeedback("Assignment form cleared.", false);
    }

    @FXML
    private void handleClearAssignmentSelection() {
        clearAssignmentSelection();
        renderAssignments();
    }

    @FXML
    private void handleShowCreatePanel() {
        setActivePanel("CREATE");
    }

    @FXML
    private void handleShowEditPanel() {
        if (!isReady() || selectedAssignment == null) {
            showFeedback("Select an assignment before editing it.", true);
            return;
        }
        if (!selectedAssignment.canCurrentUserEdit()) {
            showFeedback("You need editor access on this shared project to edit the assignment.", true);
            return;
        }
        openAssignmentEditWindow();
    }

    @FXML
    private void handleShowDeletePanel() {
        if (!isReady() || selectedAssignment == null) {
            showFeedback("Select an assignment before deleting it.", true);
            return;
        }
        if (!selectedAssignment.isOwnedByCurrentUser()) {
            showFeedback("Shared assignments cannot be deleted from your workspace.", true);
            return;
        }
        openAssignmentDeleteWindow();
    }

    @FXML
    private void handleShowCommentsPanel() {
        setActivePanel("COMMENTS");
    }

    @FXML
    private void handleShowSharePanel() {
        setActivePanel("SHARE");
    }

    @FXML
    private void handleShowGitPanel() {
        setActivePanel("GIT");
    }

    @FXML
    private void handleShowStatsPanel() {
        setActivePanel("STATS");
    }

    @FXML
    private void handleAddComment() {
        if (!isReady() || selectedAssignment == null) {
            showFeedback("Select an assignment before adding comments.", true);
            return;
        }
        boolean added = assignmentService.addComment(selectedAssignment.getId(), getCurrentUser().getId(), commentInputArea.getText());
        if (!added) {
            showFeedback("Unable to add comment to this assignment.", true);
            return;
        }
        commentInputArea.clear();
        refreshData(selectedAssignment.getId(), () -> showFeedback("Comment added successfully.", false));
    }

    @FXML
    private void handleDeleteSelectedAssignment() {
        if (!isReady() || selectedAssignment == null) {
            showFeedback("Select an assignment before deleting it.", true);
            return;
        }
        if (!selectedAssignment.isOwnedByCurrentUser()) {
            showFeedback("Shared assignments cannot be deleted from your workspace.", true);
            return;
        }
        if (!confirmDeletion("Delete assignment", "Delete \"" + selectedAssignment.getTitle() + "\" from the board?")) {
            return;
        }
        assignmentService.delete(selectedAssignment);
        clearAssignmentSelection();
        refreshData(null, () -> showFeedback("Assignment deleted successfully.", false));
    }

    @FXML
    private void handleFilterAll() {
        activeFilter = "ALL";
        applyFilterState();
        renderAssignments();
    }

    @FXML
    private void handleFilterTodo() {
        activeFilter = "TODO";
        applyFilterState();
        renderAssignments();
    }

    @FXML
    private void handleFilterProgress() {
        activeFilter = "PROGRESS";
        applyFilterState();
        renderAssignments();
    }

    @FXML
    private void handleFilterDone() {
        activeFilter = "DONE";
        applyFilterState();
        renderAssignments();
    }

    @FXML
    private void handleFilterOverdue() {
        activeFilter = "OVERDUE";
        applyFilterState();
        renderAssignments();
    }

    @FXML
    private void handleExportAssignmentsPdf() {
        List<Assignment> visibleAssignments = getVisibleAssignments();
        if (visibleAssignments.isEmpty()) {
            showFeedback("No assignments available to export.", true);
            return;
        }

        File file = PdfExportUtil.chooseExportFile(
                App.getPrimaryStage(),
                "assignments-report-" + PDF_FILE_FORMATTER.format(LocalDateTime.now()) + ".pdf"
        );
        if (file == null) {
            showFeedback("PDF export canceled.", true);
            return;
        }
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            file = new File(file.getParentFile(), file.getName() + ".pdf");
        }

        List<List<String>> rows = visibleAssignments.stream()
                .map(assignment -> List.of(
                        assignment.getTitle(),
                        safeText(assignment.getProjectTitle()),
                        assignment.getStatus(),
                        assignment.getPriority(),
                        formatDate(assignment.getEndDate())))
                .collect(Collectors.toList());

        List<String> summary = List.of(
                "Total assignments: " + totalAssignmentsLabel.getText(),
                "Overdue: " + overdueLabel.getText(),
                "In progress: " + inProgressLabel.getText(),
                "Completed: " + completedLabel.getText());

        try {
            PdfExportUtil.exportTableReport(
                    file,
                    "Assignments Report",
                    "Professional export generated from StudyFlow",
                    summary,
                    List.of("Assignment", "Project", "Status", "Priority", "Due Date"),
                    rows
            );
            showFeedback("Assignments PDF exported to: " + file.getAbsolutePath(), false);
        } catch (Exception e) {
            showFeedback("Unable to export the assignments PDF: " + e.getMessage(), true);
        }
    }

    private void refreshData() {
        refreshData(null, null);
    }

    private void refreshData(Integer preferredSelectionId, Runnable onComplete) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return;
        }

        loadingData = true;
        showFeedback("Loading assignments...", false);
        createAssignmentButton.setDisable(true);
        if (generateAiAssignmentsButton != null) {
            generateAiAssignmentsButton.setDisable(true);
        }
        if (updateAssignmentButton != null) {
            updateAssignmentButton.setDisable(true);
        }
        if (shareAssignmentButton != null) {
            shareAssignmentButton.setDisable(true);
        }
        if (deleteAssignmentConfirmButton != null) {
            deleteAssignmentConfirmButton.setDisable(true);
        }

        Integer targetSelectionId = preferredSelectionId != null
                ? preferredSelectionId
                : (selectedAssignment == null ? null : selectedAssignment.getId());

        Task<AssignmentViewData> task = new Task<>() {
            @Override
            protected AssignmentViewData call() {
                List<Project> loadedProjects = projectService.getByUserId(currentUser.getId());
                List<Project> loadedOwnedProjects = loadedProjects.stream()
                        .filter(Project::isOwnedByCurrentUser)
                        .collect(Collectors.toList());
                List<Assignment> loadedAssignments = assignmentService.getByUserId(currentUser.getId());
                notificationService.syncDueDateNotifications(currentUser.getId(), loadedProjects, loadedAssignments);
                return new AssignmentViewData(loadedProjects, loadedOwnedProjects, loadedAssignments);
            }
        };

        task.setOnSucceeded(event -> {
            AssignmentViewData data = task.getValue();
            projects.clear();
            projects.addAll(data.projects());
            ownedProjects.clear();
            ownedProjects.addAll(data.ownedProjects());
            assignments.clear();
            assignments.addAll(data.assignments());

        createAssignmentProjectCombo.getItems().setAll(ownedProjects);
            if (updateAssignmentProjectCombo != null) {
                updateAssignmentProjectCombo.getItems().setAll(ownedProjects);
            }

            if (!ownedProjects.isEmpty() && createAssignmentProjectCombo.getValue() == null) {
                createAssignmentProjectCombo.setValue(ownedProjects.get(0));
            }

            updateStats();
            updateCharts();
            renderAssignments();
            updateFormAvailability();

            if (targetSelectionId != null) {
                Assignment restored = findAssignmentById(targetSelectionId);
                if (restored != null) {
                    selectAssignment(restored);
                } else {
                    clearAssignmentSelection();
                }
            } else {
                clearAssignmentSelection();
            }

            loadingData = false;
            if (onComplete != null) {
                onComplete.run();
            } else {
                feedbackLabel.setVisible(false);
                feedbackLabel.setManaged(false);
            }
        });

        task.setOnFailed(event -> {
            loadingData = false;
            Throwable exception = task.getException();
            String message = exception == null || isBlank(exception.getMessage())
                    ? "Unable to load assignments."
                    : "Unable to load assignments: " + exception.getMessage();
            showFeedback(message, true);
            updateFormAvailability();
        });

        Thread thread = new Thread(task, "assignments-refresh");
        thread.setDaemon(true);
        thread.start();
    }

    private void updateStats() {
        totalAssignmentsLabel.setText(String.valueOf(assignments.size()));
        overdueLabel.setText(String.valueOf(assignments.stream().filter(Assignment::isOverdue).count()));
        inProgressLabel.setText(String.valueOf(assignments.stream().filter(this::isInProgressStatus).count()));
        if (reviewLabel != null) {
            reviewLabel.setText(String.valueOf(assignments.stream().filter(this::isReviewStatus).count()));
        }
        completedLabel.setText(String.valueOf(assignments.stream().filter(this::isDoneStatus).count()));
        taskCountLabel.setText(assignments.size() + " assignments");
        if (todoCountLabel != null) {
            todoCountLabel.setText(String.valueOf(assignments.stream().filter(this::isTodoStatus).count()));
        }
        if (progressCountLabel != null) {
            progressCountLabel.setText(String.valueOf(assignments.stream().filter(this::isInProgressStatus).count()));
        }
        if (reviewCountLabel != null) {
            reviewCountLabel.setText(String.valueOf(assignments.stream().filter(this::isReviewStatus).count()));
        }
        if (doneCountLabel != null) {
            doneCountLabel.setText(String.valueOf(assignments.stream().filter(this::isDoneStatus).count()));
        }
    }

    private void updateCharts() {
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        statusCounts.put("To Do", 0);
        statusCounts.put("In Progress", 0);
        statusCounts.put("Review", 0);
        statusCounts.put("Completed", 0);
        for (Assignment assignment : assignments) {
            statusCounts.computeIfPresent(normalizedStatus(assignment.getStatus()), (key, value) -> value + 1);
        }

        List<PieChart.Data> pieData = statusCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> new PieChart.Data(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        if (pieData.isEmpty()) {
            pieData = List.of(new PieChart.Data("No Data", 1));
        }
        assignmentStatusChart.setData(FXCollections.observableArrayList(pieData));
        assignmentStatusChart.setLabelsVisible(false);
        assignmentStatusChart.setLegendVisible(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Assignments");
        series.getData().add(new XYChart.Data<>("High", assignments.stream().filter(a -> "High".equalsIgnoreCase(a.getPriority())).count()));
        series.getData().add(new XYChart.Data<>("Medium", assignments.stream().filter(a -> "Medium".equalsIgnoreCase(a.getPriority())).count()));
        series.getData().add(new XYChart.Data<>("Low", assignments.stream().filter(a -> "Low".equalsIgnoreCase(a.getPriority())).count()));
        assignmentPriorityChart.getData().setAll(series);
        assignmentPriorityChart.setLegendVisible(false);
        assignmentPriorityChart.setAnimated(false);
    }

    private void renderAssignments() {
        if (todoColumn != null && progressColumn != null && reviewColumn != null && doneColumn != null) {
            todoColumn.getChildren().clear();
            progressColumn.getChildren().clear();
            reviewColumn.getChildren().clear();
            doneColumn.getChildren().clear();
        } else {
            assignmentsList.getChildren().clear();
        }
        List<Assignment> visibleAssignments = getVisibleAssignments();
        if (visibleAssignments.isEmpty()) {
            if (todoColumn != null) {
                addEmptyColumnState(todoColumn, "Drop or create assignments here.");
                addEmptyColumnState(progressColumn, "Move active work here.");
                addEmptyColumnState(reviewColumn, "Keep items here before completion.");
                addEmptyColumnState(doneColumn, "Completed assignments will appear here.");
            } else {
                assignmentsList.getChildren().add(createEmptyState("No assignments match the current search."));
            }
            return;
        }
        for (Assignment assignment : visibleAssignments) {
            if (todoColumn != null) {
                assignmentColumn(assignment).getChildren().add(createAssignmentCard(assignment));
            } else {
                assignmentsList.getChildren().add(createAssignmentCard(assignment));
            }
        }
        if (todoColumn != null) {
            addEmptyColumnState(todoColumn, "Drop or create assignments here.");
            addEmptyColumnState(progressColumn, "Move active work here.");
            addEmptyColumnState(reviewColumn, "Keep items here before completion.");
            addEmptyColumnState(doneColumn, "Completed assignments will appear here.");
        }
    }

    private List<Assignment> getVisibleAssignments() {
        return assignments.stream()
                .filter(this::matchesSearch)
                .sorted(assignmentComparator())
                .collect(Collectors.toList());
    }

    private Comparator<Assignment> assignmentComparator() {
        String sort = assignmentSortCombo.getValue();
        if ("Title A-Z".equals(sort)) {
            return Comparator.comparing(assignment -> safeText(assignment.getTitle()), String.CASE_INSENSITIVE_ORDER);
        }
        if ("Due Date".equals(sort)) {
            return Comparator.comparing(Assignment::getEndDate, Comparator.nullsLast(LocalDate::compareTo))
                    .thenComparing(assignment -> safeText(assignment.getTitle()), String.CASE_INSENSITIVE_ORDER);
        }
        if ("Priority".equals(sort)) {
            return Comparator.comparingInt(this::priorityRank)
                    .thenComparing(assignment -> safeText(assignment.getTitle()), String.CASE_INSENSITIVE_ORDER);
        }
        if ("Status".equals(sort)) {
            return Comparator.comparingInt(this::statusRank)
                    .thenComparing(assignment -> safeText(assignment.getTitle()), String.CASE_INSENSITIVE_ORDER);
        }
        return Comparator.comparing(Assignment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Assignment::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int priorityRank(Assignment assignment) {
        if ("High".equalsIgnoreCase(assignment.getPriority())) {
            return 0;
        }
        if ("Medium".equalsIgnoreCase(assignment.getPriority())) {
            return 1;
        }
        return 2;
    }

    private int statusRank(Assignment assignment) {
        String status = normalizedStatus(assignment.getStatus());
        if ("To Do".equals(status)) {
            return 0;
        }
        if ("In Progress".equals(status)) {
            return 1;
        }
        if ("Review".equals(status)) {
            return 2;
        }
        return 3;
    }

    private VBox createAssignmentCard(Assignment assignment) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("card", "board-task-card");
        if (selectedAssignment != null && selectedAssignment.getId() == assignment.getId()) {
            card.getStyleClass().add("crud-item-selected");
        }
        if (!assignment.isOwnedByCurrentUser()) {
            card.getStyleClass().add("board-task-shared");
        }
        card.setPadding(new Insets(16));
        card.setAccessibleText("Assignment card for " + assignment.getTitle());

        HBox tagsRow = new HBox(8);
        tagsRow.getChildren().addAll(
                createBadge(safeText(assignment.getProjectTitle()), "secondary"),
                createBadge(assignment.getPriority(), assignment.getPriorityStyleClass())
        );

        Label titleLabel = new Label(assignment.getTitle());
        titleLabel.getStyleClass().add("item-title");
        titleLabel.setWrapText(true);

        Label descriptionLabel = new Label(defaultText(assignment.getDescription()));
        descriptionLabel.getStyleClass().add("item-desc");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setManaged(!descriptionLabel.getText().isEmpty());
        descriptionLabel.setVisible(!descriptionLabel.getText().isEmpty());

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        Label avatar = new Label(initialsFor(assignment));
        avatar.getStyleClass().add("board-avatar-chip");
        Label dueLabel = new Label("Due " + formatDate(assignment.getEndDate()));
        dueLabel.getStyleClass().add("item-meta");
        HBox.setHgrow(dueLabel, Priority.ALWAYS);
        footer.getChildren().addAll(avatar, dueLabel);

        card.setOnMouseClicked(event -> {
            selectAssignment(assignment);
            if (event.getClickCount() >= 2) {
                openAssignmentDetailsWindow(assignment);
            }
        });
        card.setOnDragDetected(event -> {
            if (!assignment.canCurrentUserEdit()) {
                return;
            }
            draggedAssignmentId = assignment.getId();
            Dragboard dragboard = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(assignment.getId()));
            dragboard.setContent(content);
            event.consume();
        });

        card.getChildren().addAll(tagsRow, titleLabel);
        if (!descriptionLabel.getText().isEmpty()) {
            card.getChildren().add(descriptionLabel);
        }
        card.getChildren().add(footer);
        return card;
    }

    private VBox createEmptyState(String message) {
        VBox box = new VBox(10);
        box.getStyleClass().addAll("card", "empty-state-card");
        box.setPadding(new Insets(28));
        box.setAlignment(Pos.CENTER);

        FontIcon icon = new FontIcon("fth-clipboard");
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

    private VBox createCommentRow(AssignmentComment comment) {
        VBox box = new VBox(4);
        box.getStyleClass().add("detail-row");
        box.setPadding(new Insets(10, 12, 10, 12));

        Label meta = new Label(safeText(comment.getAuthorName()) + " | " + formatDateTime(comment.getCreatedAt()));
        meta.getStyleClass().add("item-meta");

        Label content = new Label(defaultText(comment.getContent()));
        content.getStyleClass().add("item-desc");
        content.setWrapText(true);

        box.getChildren().addAll(meta, content);
        return box;
    }

    private VBox assignmentColumn(Assignment assignment) {
        String status = normalizedStatus(assignment.getStatus());
        if ("In Progress".equals(status)) {
            return progressColumn;
        }
        if ("Review".equals(status)) {
            return reviewColumn;
        }
        if ("Completed".equals(status)) {
            return doneColumn;
        }
        return todoColumn;
    }

    private void addEmptyColumnState(VBox column, String message) {
        if (column == null || !column.getChildren().isEmpty()) {
            return;
        }
        VBox empty = new VBox(8);
        empty.getStyleClass().addAll("detail-row", "board-empty-card");
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(18));

        FontIcon icon = new FontIcon("fth-inbox");
        icon.getStyleClass().add("empty-state-icon");

        Label text = new Label(message);
        text.getStyleClass().add("text-muted");
        text.setWrapText(true);

        empty.getChildren().addAll(icon, text);
        column.getChildren().add(empty);
    }

    private String displayStatus(Assignment assignment) {
        return "Completed".equals(normalizedStatus(assignment.getStatus())) ? "Done" : normalizedStatus(assignment.getStatus());
    }

    private String initialsFor(Assignment assignment) {
        String source = assignment.isOwnedByCurrentUser() ? "You" : safeText(assignment.getOwnerName());
        String[] parts = source.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "A";
        }
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    private void selectAssignment(Assignment assignment) {
        selectedAssignment = assignment;
        renderAssignments();
        updateAssignmentSummary(assignment);
        populateUpdateForm(assignment);
        populateDeletePanel(assignment);
        populateCommentsPanel(assignment);
        populateSharePanel(assignment);
        populateGitPanel(assignment);
    }

    private void updateAssignmentSummary(Assignment assignment) {
        if (selectedAssignmentTitleLabel == null || selectedAssignmentMetaLabel == null) {
            return;
        }
        if (assignment == null) {
            selectedAssignmentTitleLabel.setText("Select a card to manage comments, sharing, editing, deletion, or statistics.");
            selectedAssignmentMetaLabel.setText("No assignment selected");
            return;
        }
        String ownerMeta = assignment.isOwnedByCurrentUser() ? "Owned by you" : "Shared by " + safeText(assignment.getOwnerName());
        selectedAssignmentTitleLabel.setText(assignment.getTitle());
        selectedAssignmentMetaLabel.setText(safeText(assignment.getProjectTitle()) + " | " + displayStatus(assignment) + " | " + ownerMeta
                + buildPlanningSummarySuffix(assignment));
    }

    private void populateDeletePanel(Assignment assignment) {
        if (deleteAssignmentTitleLabel == null || deleteAssignmentMetaLabel == null || deleteAssignmentConfirmButton == null) {
            return;
        }
        if (assignment == null) {
            deleteAssignmentTitleLabel.setText("No assignment selected");
            deleteAssignmentMetaLabel.setText("Select one assignment from the board first.");
            deleteAssignmentConfirmButton.setDisable(true);
            return;
        }
        deleteAssignmentTitleLabel.setText(assignment.getTitle());
        deleteAssignmentMetaLabel.setText(safeText(assignment.getProjectTitle()) + " | due " + formatDate(assignment.getEndDate()));
        deleteAssignmentConfirmButton.setDisable(!assignment.isOwnedByCurrentUser());
    }

    private void populateCommentsPanel(Assignment assignment) {
        if (commentsList == null || commentsAssignmentLabel == null || commentInputArea == null || addCommentButton == null) {
            return;
        }
        commentsList.getChildren().clear();
        commentInputArea.clear();
        if (assignment == null) {
            commentsAssignmentLabel.setText("Select an assignment to open its comments thread.");
            addCommentButton.setDisable(true);
            commentsList.getChildren().add(createEmptyState("Assignment comments will appear here."));
            return;
        }
        commentsAssignmentLabel.setText("Comments for \"" + assignment.getTitle() + "\"");
        addCommentButton.setDisable(false);
        List<AssignmentComment> comments = assignmentService.getCommentsByAssignmentId(assignment.getId(), getCurrentUser().getId());
        if (comments.isEmpty()) {
            commentsList.getChildren().add(createEmptyState("No comments yet for this assignment."));
            return;
        }
        for (AssignmentComment comment : comments) {
            commentsList.getChildren().add(createCommentRow(comment));
        }
    }

    private void populateSharePanel(Assignment assignment) {
        if (shareAssignmentEmailField == null || shareAssignmentButton == null) {
            return;
        }
        shareAssignmentEmailField.clear();
        boolean enabled = assignment != null && assignment.isOwnedByCurrentUser();
        shareAssignmentEmailField.setDisable(!enabled);
        shareAssignmentButton.setDisable(!enabled);
        renderSharedAssignmentUsers(assignment);
    }

    private void renderSharedAssignmentUsers(Assignment assignment) {
        if (sharedAssignmentUsersList == null) {
            return;
        }
        sharedAssignmentUsersList.getChildren().clear();

        if (assignment == null) {
            sharedAssignmentUsersList.getChildren().add(createSharedUserEmptyState("Select an assignment to view shared users."));
            return;
        }

        List<User> sharedUsers = assignmentService.getSharedUsers(assignment.getId());
        if (sharedUsers.isEmpty()) {
            sharedAssignmentUsersList.getChildren().add(createSharedUserEmptyState("This assignment is not shared with anyone yet."));
            return;
        }

        for (User sharedUser : sharedUsers) {
            sharedAssignmentUsersList.getChildren().add(createSharedUserRow(sharedUser));
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

    private void populateGitPanel(Assignment assignment) {
        if (assignmentGitHintLabel == null
                || assignmentGitRepoLabel == null
                || assignmentGitStatusLabel == null
                || assignmentGitLastCommitLabel == null
                || assignmentGitCommitMessageField == null
                || assignmentGitPathspecField == null
                || saveAssignmentGitButton == null
                || refreshAssignmentGitButton == null
                || commitAssignmentGitButton == null
                || commitAndPushAssignmentGitButton == null) {
            return;
        }

        if (assignment == null) {
            assignmentGitHintLabel.setText("Select a completed assignment to save commit settings or commit it to Git.");
            assignmentGitRepoLabel.setText("No project repository selected");
            assignmentGitStatusLabel.setText("Repository status unavailable");
            assignmentGitLastCommitLabel.setText("No commit recorded");
            assignmentGitCommitMessageField.clear();
            assignmentGitPathspecField.clear();
            saveAssignmentGitButton.setDisable(true);
            refreshAssignmentGitButton.setDisable(true);
            commitAssignmentGitButton.setDisable(true);
            commitAndPushAssignmentGitButton.setDisable(true);
            return;
        }

        Project linkedProject = findProjectForAssignment(assignment);
        boolean editable = assignment.canCurrentUserEdit();
        boolean completed = assignment.isCompleted();
        boolean repoConfigured = linkedProject != null && !isBlank(linkedProject.getGitRepoPath());

        assignmentGitHintLabel.setText(editable
                ? (completed
                ? "Commit this completed assignment directly to the linked project repository."
                : "Mark the assignment as Completed before committing it.")
                : "You need editor access on this shared project for Git actions.");
        assignmentGitRepoLabel.setText(linkedProject == null
                ? "No linked project"
                : safeText(linkedProject.getGitRepoPath()) + (isBlank(linkedProject.getGitDefaultBranch()) ? "" : " | " + linkedProject.getGitDefaultBranch()));
        assignmentGitStatusLabel.setText(linkedProject == null || isBlank(linkedProject.getGitLastStatusSummary())
                ? "Repository status not checked yet"
                : linkedProject.getGitLastStatusSummary());
        assignmentGitLastCommitLabel.setText(isBlank(assignment.getGitLastCommitHash())
                ? "No commit recorded"
                : assignment.getGitLastCommitHash() + " | " + formatDateTime(assignment.getGitLastCommitAt()));
        assignmentGitCommitMessageField.setText(isBlank(assignment.getGitCommitMessage())
                ? "complete(assignment): " + assignment.getTitle()
                : assignment.getGitCommitMessage());
        assignmentGitPathspecField.setText(defaultText(assignment.getGitCommitPathspec()));

        saveAssignmentGitButton.setDisable(!editable);
        refreshAssignmentGitButton.setDisable(!repoConfigured);
        commitAssignmentGitButton.setDisable(!editable || !completed || !repoConfigured);
        commitAndPushAssignmentGitButton.setDisable(!editable || !completed || !repoConfigured);
    }
    private void populateUpdateForm(Assignment assignment) {
        if (updateAssignmentHintLabel == null
                || updateAssignmentTitleField == null
                || updateAssignmentDescriptionArea == null
                || updateAssignmentStartDatePicker == null
                || updateAssignmentEndDatePicker == null
                || updateAssignmentPriorityCombo == null
                || updateAssignmentStatusCombo == null
                || updateAssignmentProjectCombo == null
                || updateAssignmentButton == null) {
            return;
        }
        boolean hasSelection = assignment != null;
        if (editPanel != null) {
            editPanel.setDisable(!hasSelection);
            editPanel.setOpacity(hasSelection ? 1 : 0.55);
        }
        clearUpdateValidation();

        if (!hasSelection) {
            updateAssignmentHintLabel.setText("Select an assignment from the board to edit it here.");
            updateAssignmentTitleField.clear();
            updateAssignmentDescriptionArea.clear();
            updateAssignmentStartDatePicker.setValue(null);
            updateAssignmentEndDatePicker.setValue(null);
            updateAssignmentPriorityCombo.setValue("Medium");
            updateAssignmentStatusCombo.setValue("To Do");
            updateAssignmentProjectCombo.setValue(ownedProjects.isEmpty() ? null : ownedProjects.get(0));
            shareAssignmentEmailField.clear();
            shareAssignmentEmailField.setDisable(true);
            shareAssignmentButton.setDisable(true);
            return;
        }

        boolean editable = assignment.canCurrentUserEdit();
        updateAssignmentHintLabel.setText(editable
                ? "Updating assignment #" + assignment.getId() + "."
                : "This assignment is shared with you as view-only.");
        updateAssignmentTitleField.setText(assignment.getTitle());
        updateAssignmentDescriptionArea.setText(defaultText(assignment.getDescription()));
        updateAssignmentStartDatePicker.setValue(assignment.getStartDate());
        updateAssignmentEndDatePicker.setValue(assignment.getEndDate());
        updateAssignmentPriorityCombo.setValue(assignment.getPriority());
        updateAssignmentStatusCombo.setValue(normalizedStatus(assignment.getStatus()));
        updateAssignmentProjectCombo.setValue(projects.stream()
                .filter(project -> project.getId() == assignment.getProjectId())
                .findFirst()
                .orElse(null));
        updateAssignmentTitleField.setDisable(!editable);
        updateAssignmentDescriptionArea.setDisable(!editable);
        updateAssignmentStartDatePicker.setDisable(!editable);
        updateAssignmentEndDatePicker.setDisable(!editable);
        updateAssignmentPriorityCombo.setDisable(!editable);
        updateAssignmentStatusCombo.setDisable(!editable);
        updateAssignmentProjectCombo.setDisable(!editable || projects.isEmpty());
        updateAssignmentButton.setDisable(!editable);
        shareAssignmentEmailField.clear();
        shareAssignmentEmailField.setDisable(!editable);
        shareAssignmentButton.setDisable(!editable);
    }

    private void resetCreateForm() {
        createAssignmentTitleField.clear();
        createAssignmentDescriptionArea.clear();
        createAssignmentStartDatePicker.setValue(null);
        createAssignmentEndDatePicker.setValue(null);
        createAssignmentStartTimeCombo.setValue("09:00");
        createAssignmentEndTimeCombo.setValue("13:00");
        createAssignmentPriorityCombo.setValue("Medium");
        createAssignmentStatusCombo.setValue("To Do");
        createAssignmentProjectCombo.setValue(ownedProjects.isEmpty() ? null : ownedProjects.get(0));
        clearCreateValidation();
    }

    private void clearAssignmentSelection() {
        selectedAssignment = null;
        populateUpdateForm(null);
        updateAssignmentSummary(null);
        populateDeletePanel(null);
        populateCommentsPanel(null);
        populateSharePanel(null);
        populateGitPanel(null);
    }

    private void setActivePanel(String panel) {
        activePanel = panel;
        togglePanel(createPanel, "CREATE".equals(panel));
        togglePanel(editPanel, false);
        togglePanel(deletePanel, false);
        togglePanel(commentsPanel, "COMMENTS".equals(panel));
        togglePanel(sharePanel, "SHARE".equals(panel));
        togglePanel(gitPanel, "GIT".equals(panel));
        togglePanel(statsPanel, "STATS".equals(panel));
        updatePanelButton(showCreatePanelButton, "CREATE".equals(panel));
        updatePanelButton(showEditPanelButton, false);
        updatePanelButton(showDeletePanelButton, false);
        updatePanelButton(showCommentsPanelButton, "COMMENTS".equals(panel));
        updatePanelButton(showSharePanelButton, "SHARE".equals(panel));
        updatePanelButton(showGitPanelButton, "GIT".equals(panel));
        updatePanelButton(showStatsPanelButton, "STATS".equals(panel));
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

    private void generateAiAssignmentsForProject(Project project) {
        if (project == null) {
            return;
        }
        if (!aiAssignmentGeneratorService.isConfigured()) {
            showFeedback("AI assignments are unavailable. Set OPENROUTER_API_KEY first.", true);
            return;
        }

        if (generateAiAssignmentsButton != null) {
            generateAiAssignmentsButton.setDisable(true);
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
            if (generateAiAssignmentsButton != null) {
                generateAiAssignmentsButton.setDisable(false);
            }

            CrudViewContext.setAiAssignmentSuggestions(task.getValue(), project, "views/Assignments.fxml");
            CrudViewContext.rememberProjectSelection(project.getId());
            MainController.loadContentInMainArea("views/AiAssignmentReview.fxml");
        });

        task.setOnFailed(event -> {
            setAiLoadingState(false, null, null);
            if (generateAiAssignmentsButton != null) {
                generateAiAssignmentsButton.setDisable(false);
            }
            Throwable error = task.getException();
            showFeedback("AI generation failed: " + (error == null ? "unknown error" : error.getMessage()), true);
        });

        Thread thread = new Thread(task, "assignment-ai-generator");
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
    private void handleSaveAssignmentGitSettings() {
        if (!isReady() || selectedAssignment == null) {
            showFeedback("Select an assignment before saving Git settings.", true);
            return;
        }
        if (!selectedAssignment.canCurrentUserEdit()) {
            showFeedback("You need editor access on this shared project to save assignment Git settings.", true);
            return;
        }
        applyAssignmentGitFields();
        selectedAssignment.setCurrentUserId(getCurrentUser().getId());
        assignmentService.updateGitMetadata(selectedAssignment);
        refreshData(selectedAssignment.getId(), () -> showFeedback("Assignment Git settings saved.", false));
    }

    @FXML
    private void handleRefreshAssignmentGitStatus() {
        runAssignmentGitTask(false, true);
    }

    @FXML
    private void handleCommitAssignmentToGit() {
        runAssignmentGitTask(false, false);
    }

    @FXML
    private void handleCommitAndPushAssignmentToGit() {
        runAssignmentGitTask(true, false);
    }

    private void applyAssignmentGitFields() {
        if (selectedAssignment == null) {
            return;
        }
        selectedAssignment.setGitCommitMessage(defaultText(assignmentGitCommitMessageField == null ? null : assignmentGitCommitMessageField.getText()));
        selectedAssignment.setGitCommitPathspec(defaultText(assignmentGitPathspecField == null ? null : assignmentGitPathspecField.getText()));
    }

    private void runAssignmentGitTask(boolean push, boolean refreshOnly) {
        if (!isReady() || selectedAssignment == null) {
            showFeedback("Select an assignment before using Git actions.", true);
            return;
        }
        if (!refreshOnly && !selectedAssignment.canCurrentUserEdit()) {
            showFeedback("You need editor access on this shared project to commit this assignment.", true);
            return;
        }

        Project linkedProject = findProjectForAssignment(selectedAssignment);
        if (linkedProject == null || isBlank(linkedProject.getGitRepoPath())) {
            showFeedback("Configure the project repository in Projects -> Git first.", true);
            return;
        }
        if (!refreshOnly && !selectedAssignment.isCompleted()) {
            showFeedback("Only completed assignments can be committed.", true);
            return;
        }

        applyAssignmentGitFields();
        selectedAssignment.setCurrentUserId(getCurrentUser().getId());
        assignmentService.updateGitMetadata(selectedAssignment);
        setAiLoadingState(true, refreshOnly ? "Git status" : (push ? "Commit and push" : "Committing assignment"),
                refreshOnly ? "Checking repository status..." : "Running Git actions for \"" + selectedAssignment.getTitle() + "\".");

        Task<GitIntegrationService.GitOperationResult> task = new Task<>() {
            @Override
            protected GitIntegrationService.GitOperationResult call() {
                return refreshOnly
                        ? gitIntegrationService.describeStatus(linkedProject)
                        : gitIntegrationService.commitAssignment(linkedProject, selectedAssignment, push);
            }
        };

        task.setOnSucceeded(event -> {
            setAiLoadingState(false, null, null);
            GitIntegrationService.GitOperationResult result = task.getValue();
            linkedProject.setGitLastStatusSummary(result.details());
            linkedProject.setGitLastSyncAt(result.completedAt());
            linkedProject.setCurrentUserId(getCurrentUser().getId());
            projectService.updateGitSettings(linkedProject);
            if (!refreshOnly && result.success()) {
                selectedAssignment.setGitLastCommitHash(result.commitHash());
                selectedAssignment.setGitLastCommitAt(result.completedAt());
                selectedAssignment.setCurrentUserId(getCurrentUser().getId());
                assignmentService.updateGitMetadata(selectedAssignment);
            }
            refreshData(selectedAssignment.getId(), () -> showFeedback(result.summary(), !result.success()));
        });

        task.setOnFailed(event -> {
            setAiLoadingState(false, null, null);
            Throwable error = task.getException();
            showFeedback("Git action failed: " + (error == null ? "unknown error" : error.getMessage()), true);
        });

        Thread thread = new Thread(task, "assignment-git-task");
        thread.setDaemon(true);
        thread.start();
    }

    private Project findProjectForAssignment(Assignment assignment) {
        if (assignment == null) {
            return null;
        }
        for (Project project : projects) {
            if (project.getId() == assignment.getProjectId()) {
                return project;
            }
        }
        for (Project project : ownedProjects) {
            if (project.getId() == assignment.getProjectId()) {
                return project;
            }
        }
        return projectService.getProjectById(assignment.getProjectId());
    }
    private void configureBoardDropTargets() {
        configureDropTarget(todoColumn, "To Do");
        configureDropTarget(progressColumn, "In Progress");
        configureDropTarget(reviewColumn, "Review");
        configureDropTarget(doneColumn, "Completed");
    }

    private void configureShareSuggestions() {
        if (shareAssignmentEmailField == null) {
            return;
        }

        shareAssignmentEmailField.textProperty().addListener((obs, oldVal, newVal) -> {
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
                for (User u : matches) {
                    String title = isBlank(u.getUsername()) ? safeText(u.getFullName()).trim() : safeText(u.getUsername());
                    String subtitle = buildUserSuggestionSubtitle(u);
                    Label itemLabel = new Label(title + (subtitle.isEmpty() ? "" : " — " + subtitle));
                    itemLabel.getStyleClass().add("autocomplete-item");
                    CustomMenuItem menuItem = new CustomMenuItem(itemLabel, true);
                    menuItem.setOnAction(evt -> {
                        String id = preferredUserIdentifier(u);
                        shareAssignmentEmailField.setText(id);
                        shareAssignmentEmailField.positionCaret(id.length());
                        shareSuggestionsMenu.hide();
                    });
                    items.add(menuItem);
                }
                shareSuggestionsMenu.getItems().setAll(items);
                if (!shareSuggestionsMenu.isShowing()) {
                    shareSuggestionsMenu.show(shareAssignmentEmailField, Side.BOTTOM, 0, 0);
                }
            } catch (Exception ex) {
                // fail silently - suggestions are non-critical
                shareSuggestionsMenu.hide();
            }
        });

        shareAssignmentEmailField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                shareSuggestionsMenu.hide();
            }
        });
    }

    private void configureDropTarget(VBox column, String targetStatus) {
        if (column == null) {
            return;
        }
        column.setOnDragOver(event -> {
            if (draggedAssignmentId != null) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        column.setOnDragEntered(event -> column.getStyleClass().add("board-column-drop-target"));
        column.setOnDragExited(event -> column.getStyleClass().remove("board-column-drop-target"));
        column.setOnDragDropped(event -> {
            boolean success = false;
            Assignment assignment = draggedAssignmentId == null ? null : findAssignmentById(draggedAssignmentId);
            if (assignment != null && assignment.canCurrentUserEdit()) {
                String previousStatus = normalizedStatus(assignment.getStatus());
                assignment.setCurrentUserId(getCurrentUser() == null ? 0 : getCurrentUser().getId());
                assignment.setStatus(targetStatus);
                if (!"Completed".equals(previousStatus) && "Completed".equals(targetStatus)) {
                    LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
                    assignment.setEndDate(now.toLocalDate());
                    assignment.setEndTime(now.toLocalTime());
                }
                assignmentService.update(assignment);
                refreshData(assignment.getId(), () -> showFeedback("Assignment moved to " + displayStatus(assignment) + ".", false));
                success = true;
            }
            draggedAssignmentId = null;
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void applyFilterState() {
    }

    private boolean isTodoStatus(Assignment assignment) {
        return "To Do".equals(normalizedStatus(assignment.getStatus()));
    }

    private boolean isInProgressStatus(Assignment assignment) {
        return "In Progress".equals(normalizedStatus(assignment.getStatus()));
    }

    private boolean isReviewStatus(Assignment assignment) {
        return "Review".equals(normalizedStatus(assignment.getStatus()));
    }

    private boolean isDoneStatus(Assignment assignment) {
        return "Completed".equals(normalizedStatus(assignment.getStatus()));
    }

    private String normalizedStatus(String value) {
        if (isBlank(value)) {
            return "To Do";
        }
        if ("review".equalsIgnoreCase(value.trim())) {
            return "Review";
        }
        if ("completed".equalsIgnoreCase(value.trim()) || "done".equalsIgnoreCase(value.trim())) {
            return "Completed";
        }
        if ("in progress".equalsIgnoreCase(value.trim())) {
            return "In Progress";
        }
        if ("to do".equalsIgnoreCase(value.trim())) {
            return "To Do";
        }
        return value.trim();
    }

    private boolean matchesSearch(Assignment assignment) {
        if (isBlank(assignmentSearchField.getText())) {
            return true;
        }
        String query = assignmentSearchField.getText().trim().toLowerCase();
        return safeText(assignment.getTitle()).toLowerCase().contains(query)
                || safeText(assignment.getProjectTitle()).toLowerCase().contains(query)
                || safeText(assignment.getOwnerName()).toLowerCase().contains(query)
                || defaultText(assignment.getDescription()).toLowerCase().contains(query)
                || displayStatus(assignment).toLowerCase().contains(query)
                || safeText(assignment.getPriority()).toLowerCase().contains(query)
                || formatDate(assignment.getStartDate()).toLowerCase().contains(query)
                || formatDate(assignment.getEndDate()).toLowerCase().contains(query);
    }

    private boolean validateAssignmentForm(
            ComboBox<Project> projectCombo, Label projectError,
            TextField titleField, Label titleError,
            DatePicker startPicker, Label startError,
            ComboBox<String> startTimeCombo,
            DatePicker endPicker, Label endError,
            ComboBox<String> endTimeCombo,
            TextArea descriptionArea) {
        boolean valid = true;
        if (projectCombo.getValue() == null) {
            showFieldError(projectCombo, projectError, "Project is required.");
            valid = false;
        }
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
        if (descriptionArea != null && descriptionArea.getText() != null && descriptionArea.getText().length() > MAX_DESCRIPTION_LENGTH) {
            showFeedback("Assignment description cannot exceed 500 characters.", true);
            valid = false;
        }
        if (startPicker.getValue() != null && endPicker.getValue() != null
                && startTimeCombo != null && endTimeCombo != null
                && startPicker.getValue().isEqual(endPicker.getValue())) {
            LocalDateTime start = LocalDateTime.of(startPicker.getValue(), parseTime(startTimeCombo.getValue(), LocalTime.of(9, 0)));
            LocalDateTime end = LocalDateTime.of(endPicker.getValue(), parseTime(endTimeCombo.getValue(), LocalTime.of(13, 0)));
            if (end.isBefore(start)) {
                showFieldError(endPicker, endError, "End must be after start.");
                valid = false;
            } else if (Duration.between(start, end).toHours() < 4) {
                showFieldError(endPicker, endError, "Same-day assignments need at least 4 hours.");
                valid = false;
            }
        }
        return valid;
    }

    private String statusStyleClass(Assignment assignment) {
        if (isDoneStatus(assignment)) {
            return "success";
        }
        if (isInProgressStatus(assignment)) {
            return "primary";
        }
        if (isReviewStatus(assignment)) {
            return "warning";
        }
        if (assignment.isOverdue()) {
            return "danger";
        }
        return "secondary";
    }

    private void updateFormAvailability() {
        boolean hasProjects = !ownedProjects.isEmpty();
        createAssignmentButton.setDisable(!hasProjects);
        clearCreateAssignmentButton.setDisable(!hasProjects);
        createAssignmentProjectCombo.setDisable(!hasProjects);
        if (generateAiAssignmentsButton != null && !loadingData) {
            generateAiAssignmentsButton.setDisable(!hasProjects);
        }

        if (hasProjects) {
            createAssignmentHintLabel.setText("Create a new assignment and link it to a project.");
        } else {
            createAssignmentHintLabel.setText("Create your own project first, then come back to add assignments.");
            clearAssignmentSelection();
        }

        if (selectedAssignment == null) {
            populateUpdateForm(null);
        }
    }

    private void bindAssignmentValidationReset() {
        createAssignmentTitleField.textProperty().addListener((obs, oldValue, newValue) -> clearFieldError(createAssignmentTitleField, createAssignmentTitleError));
        createAssignmentProjectCombo.valueProperty().addListener((obs, oldValue, newValue) -> clearFieldError(createAssignmentProjectCombo, createAssignmentProjectError));
        createAssignmentStartDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> clearFieldError(createAssignmentStartDatePicker, createAssignmentStartDateError));
        createAssignmentEndDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> clearFieldError(createAssignmentEndDatePicker, createAssignmentEndDateError));
        if (updateAssignmentTitleField != null && updateAssignmentTitleError != null) {
            updateAssignmentTitleField.textProperty().addListener((obs, oldValue, newValue) -> clearFieldError(updateAssignmentTitleField, updateAssignmentTitleError));
        }
        if (updateAssignmentProjectCombo != null && updateAssignmentProjectError != null) {
            updateAssignmentProjectCombo.valueProperty().addListener((obs, oldValue, newValue) -> clearFieldError(updateAssignmentProjectCombo, updateAssignmentProjectError));
        }
        if (updateAssignmentStartDatePicker != null && updateAssignmentStartDateError != null) {
            updateAssignmentStartDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> clearFieldError(updateAssignmentStartDatePicker, updateAssignmentStartDateError));
        }
        if (updateAssignmentEndDatePicker != null && updateAssignmentEndDateError != null) {
            updateAssignmentEndDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> clearFieldError(updateAssignmentEndDatePicker, updateAssignmentEndDateError));
        }
    }

    private void configureAccessibility() {
        assignmentSearchField.setAccessibleText("Search assignments");
        assignmentSortCombo.setAccessibleText("Sort assignments");
        exportAssignmentsPdfButton.setAccessibleText("Export visible assignments to PDF");
        exportAssignmentsPdfButton.setTooltip(new Tooltip("Export the visible assignment list as a PDF report"));
        createAssignmentTitleField.setAccessibleText("Assignment title");
        createAssignmentProjectCombo.setAccessibleText("Assignment project");
        createAssignmentPriorityCombo.setAccessibleText("Assignment priority");
        createAssignmentStatusCombo.setAccessibleText("Assignment status");
        createAssignmentStartDatePicker.setAccessibleText("Assignment start date");
        createAssignmentEndDatePicker.setAccessibleText("Assignment end date");
        createAssignmentStartTimeCombo.setAccessibleText("Assignment start time");
        createAssignmentEndTimeCombo.setAccessibleText("Assignment end time");
        createAssignmentDescriptionArea.setAccessibleText("Assignment description");
        if (updateAssignmentTitleField != null) {
            updateAssignmentTitleField.setAccessibleText("Selected assignment title");
        }
        if (updateAssignmentProjectCombo != null) {
            updateAssignmentProjectCombo.setAccessibleText("Selected assignment project");
        }
        if (updateAssignmentPriorityCombo != null) {
            updateAssignmentPriorityCombo.setAccessibleText("Selected assignment priority");
        }
        if (updateAssignmentStatusCombo != null) {
            updateAssignmentStatusCombo.setAccessibleText("Selected assignment status");
        }
        if (updateAssignmentStartDatePicker != null) {
            updateAssignmentStartDatePicker.setAccessibleText("Selected assignment start date");
        }
        if (updateAssignmentEndDatePicker != null) {
            updateAssignmentEndDatePicker.setAccessibleText("Selected assignment end date");
        }
        if (updateAssignmentDescriptionArea != null) {
            updateAssignmentDescriptionArea.setAccessibleText("Selected assignment description");
        }
        shareAssignmentEmailField.setAccessibleText("Share assignment using username, email, phone, or student id");
    }

    private void configureDescriptionLimits() {
        createAssignmentDescriptionArea.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= MAX_DESCRIPTION_LENGTH ? change : null));
        if (updateAssignmentDescriptionArea != null) {
            updateAssignmentDescriptionArea.setTextFormatter(new TextFormatter<String>(change ->
                    change.getControlNewText().length() <= MAX_DESCRIPTION_LENGTH ? change : null));
        }
    }

    private void openAssignmentEditWindow() {
        CrudViewContext.setAssignmentContext(selectedAssignment, projects);
        CrudViewContext.rememberAssignmentSelection(selectedAssignment == null ? null : selectedAssignment.getId());
        MainController.loadContentInMainArea("views/AssignmentEditDialog.fxml");
    }

    private void openAssignmentDetailsWindow(Assignment assignment) {
        if (assignment == null) {
            return;
        }
        CrudViewContext.setAssignmentContext(assignment, projects);
        CrudViewContext.rememberAssignmentSelection(assignment.getId());
        MainController.loadContentInMainArea("views/AssignmentDetails.fxml");
    }
    private void openAssignmentDeleteWindow() {
        CrudViewContext.setAssignmentContext(selectedAssignment, ownedProjects);
        MainController.loadContentInMainArea("views/AssignmentDeleteDialog.fxml");
    }

    private void clearCreateValidation() {
        clearFieldError(createAssignmentTitleField, createAssignmentTitleError);
        clearFieldError(createAssignmentProjectCombo, createAssignmentProjectError);
        clearFieldError(createAssignmentStartDatePicker, createAssignmentStartDateError);
        clearFieldError(createAssignmentEndDatePicker, createAssignmentEndDateError);
    }

    private void clearUpdateValidation() {
        if (updateAssignmentTitleField == null
                || updateAssignmentTitleError == null
                || updateAssignmentProjectCombo == null
                || updateAssignmentProjectError == null
                || updateAssignmentStartDatePicker == null
                || updateAssignmentStartDateError == null
                || updateAssignmentEndDatePicker == null
                || updateAssignmentEndDateError == null) {
            return;
        }
        clearFieldError(updateAssignmentTitleField, updateAssignmentTitleError);
        clearFieldError(updateAssignmentProjectCombo, updateAssignmentProjectError);
        clearFieldError(updateAssignmentStartDatePicker, updateAssignmentStartDateError);
        clearFieldError(updateAssignmentEndDatePicker, updateAssignmentEndDateError);
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
        createAssignmentButton.setDisable(true);
        if (generateAiAssignmentsButton != null) {
            generateAiAssignmentsButton.setDisable(true);
        }
        clearCreateAssignmentButton.setDisable(true);
        if (editPanel != null) {
            editPanel.setDisable(true);
        }
        exportAssignmentsPdfButton.setDisable(true);
    }

    private boolean isReady() {
        return assignmentService.isDatabaseAvailable() && projectService.isDatabaseAvailable() && getCurrentUser() != null;
    }

    private User getCurrentUser() {
        return UserSession.getInstance().getCurrentUser();
    }

    private Assignment findAssignmentById(int id) {
        return assignments.stream().filter(assignment -> assignment.getId() == id).findFirst().orElse(null);
    }

    private void configureProjectCombo(ComboBox<Project> comboBox) {
        if (comboBox == null) {
            return;
        }
        comboBox.setButtonCell(new ProjectListCell());
        comboBox.setCellFactory(list -> new ProjectListCell());
    }

    private String defaultPriority(String value) {
        return isBlank(value) ? "Medium" : value;
    }

    private String defaultStatus(String value) {
        return isBlank(value) ? "To Do" : value;
    }

    private String defaultText(String value) {
        return value == null ? "" : value.trim();
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

    private String buildPlanningMessage(String prefix, Assignment assignment) {
        if (assignment == null || assignment.getAiSuggestedDueDate() == null) {
            return prefix;
        }
        String complexity = isBlank(assignment.getComplexityLevel()) ? "AI-rated" : assignment.getComplexityLevel();
        return prefix + " " + complexity + " complexity, estimated "
                + safeText(String.valueOf(assignment.getEstimatedMinDays())) + "-" + safeText(String.valueOf(assignment.getEstimatedMaxDays()))
                + " day(s), suggested due " + formatDate(assignment.getAiSuggestedDueDate()) + ".";
    }

    private String buildPlanningSummarySuffix(Assignment assignment) {
        if (assignment == null || assignment.getAiSuggestedDueDate() == null) {
            return "";
        }
        String range = assignment.getEstimatedMinDays() != null && assignment.getEstimatedMaxDays() != null
                ? assignment.getEstimatedMinDays() + "-" + assignment.getEstimatedMaxDays() + "d"
                : "--";
        return " | AI " + safeText(assignment.getComplexityLevel()) + " / " + range + " / due "
                + formatDate(assignment.getAiSuggestedDueDate());
    }

    private LocalTime parseTime(String value, LocalTime fallback) {
        if (isBlank(value)) {
            return fallback;
        }
        return LocalTime.parse(value, TIME_FORMATTER);
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

    private String safeText(String value) {
        return value == null ? "" : value;
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

    private String formatDate(LocalDate date) {
        return date == null ? "--" : DATE_FORMATTER.format(date);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "--" : DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm").format(dateTime);
    }

    private record AssignmentViewData(List<Project> projects, List<Project> ownedProjects, List<Assignment> assignments) {
    }

    private static class ProjectListCell extends ListCell<Project> {
        @Override
        protected void updateItem(Project item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.getTitle());
        }
    }
}
