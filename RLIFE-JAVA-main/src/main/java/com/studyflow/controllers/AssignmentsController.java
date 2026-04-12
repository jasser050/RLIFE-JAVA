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
import javafx.scene.control.ListCell;
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

public class AssignmentsController implements Initializable {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter PDF_FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @FXML private Label totalAssignmentsLabel;
    @FXML private Label overdueLabel;
    @FXML private Label inProgressLabel;
    @FXML private Label completedLabel;
    @FXML private Label feedbackLabel;
    @FXML private Label createAssignmentHintLabel;
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
    @FXML private DatePicker createAssignmentEndDatePicker;
    @FXML private Label createAssignmentEndDateError;
    @FXML private TextArea createAssignmentDescriptionArea;
    @FXML private Button createAssignmentButton;
    @FXML private Button clearCreateAssignmentButton;
    @FXML private VBox updateAssignmentCard;
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
    @FXML private Button filterAllButton;
    @FXML private Button filterTodoButton;
    @FXML private Button filterProgressButton;
    @FXML private Button filterDoneButton;
    @FXML private Button filterOverdueButton;
    @FXML private Label taskCountLabel;
    @FXML private VBox assignmentsList;

    private final AssignmentService assignmentService = new AssignmentService();
    private final ProjectService projectService = new ProjectService();
    private final NotificationService notificationService = new NotificationService();

    private final List<Assignment> assignments = new ArrayList<>();
    private final List<Project> projects = new ArrayList<>();
    private Assignment selectedAssignment;
    private String activeFilter = "ALL";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<String> priorities = List.of("High", "Medium", "Low");
        List<String> statuses = List.of("To Do", "In Progress", "Completed");

        configureProjectCombo(createAssignmentProjectCombo);
        configureProjectCombo(updateAssignmentProjectCombo);
        createAssignmentPriorityCombo.getItems().addAll(priorities);
        updateAssignmentPriorityCombo.getItems().addAll(priorities);
        createAssignmentStatusCombo.getItems().addAll(statuses);
        updateAssignmentStatusCombo.getItems().addAll(statuses);
        assignmentSortCombo.getItems().addAll("Newest Added", "Title A-Z", "Due Date", "Priority", "Status");
        assignmentSortCombo.setValue("Newest Added");

        assignmentSearchField.textProperty().addListener((obs, oldValue, newValue) -> renderAssignments());
        assignmentSortCombo.valueProperty().addListener((obs, oldValue, newValue) -> renderAssignments());
        bindAssignmentValidationReset();
        configureAccessibility();

        if (!isReady()) {
            disableForm();
            return;
        }

        resetCreateForm();
        clearAssignmentSelection();
        refreshData();
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
                createAssignmentEndDatePicker, createAssignmentEndDateError)) {
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
        assignment.setEndDate(createAssignmentEndDatePicker.getValue());
        assignment.setPriority(defaultPriority(createAssignmentPriorityCombo.getValue()));
        assignment.setStatus(defaultStatus(createAssignmentStatusCombo.getValue()));

        assignmentService.add(assignment);
        refreshData();
        resetCreateForm();
        selectAssignment(findAssignmentById(assignment.getId()));
        showFeedback("Assignment created successfully.", false);
    }

    @FXML
    private void handleUpdateAssignment() {
        if (!isReady() || selectedAssignment == null) {
            showFeedback("Select an assignment before updating it.", true);
            return;
        }

        clearUpdateValidation();
        if (!validateAssignmentForm(
                updateAssignmentProjectCombo, updateAssignmentProjectError,
                updateAssignmentTitleField, updateAssignmentTitleError,
                updateAssignmentStartDatePicker, updateAssignmentStartDateError,
                updateAssignmentEndDatePicker, updateAssignmentEndDateError)) {
            showFeedback("Please fix the highlighted assignment fields.", true);
            return;
        }

        Project project = updateAssignmentProjectCombo.getValue();
        selectedAssignment.setUserId(getCurrentUser().getId());
        selectedAssignment.setProjectId(project.getId());
        selectedAssignment.setProjectTitle(project.getTitle());
        selectedAssignment.setTitle(updateAssignmentTitleField.getText().trim());
        selectedAssignment.setDescription(defaultText(updateAssignmentDescriptionArea.getText()));
        selectedAssignment.setStartDate(updateAssignmentStartDatePicker.getValue());
        selectedAssignment.setEndDate(updateAssignmentEndDatePicker.getValue());
        selectedAssignment.setPriority(defaultPriority(updateAssignmentPriorityCombo.getValue()));
        selectedAssignment.setStatus(defaultStatus(updateAssignmentStatusCombo.getValue()));

        assignmentService.update(selectedAssignment);
        int selectedId = selectedAssignment.getId();
        refreshData();
        selectAssignment(findAssignmentById(selectedId));
        showFeedback("Assignment updated successfully.", false);
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
        projects.clear();
        projects.addAll(projectService.getByUserId(getCurrentUser().getId()));

        assignments.clear();
        assignments.addAll(assignmentService.getByUserId(getCurrentUser().getId()));
        notificationService.syncDueDateNotifications(getCurrentUser().getId(), projects, assignments);

        createAssignmentProjectCombo.getItems().setAll(projects);
        updateAssignmentProjectCombo.getItems().setAll(projects);

        if (!projects.isEmpty() && createAssignmentProjectCombo.getValue() == null) {
            createAssignmentProjectCombo.setValue(projects.get(0));
        }

        updateStats();
        updateCharts();
        applyFilterState();
        renderAssignments();
        updateFormAvailability();

        if (selectedAssignment != null) {
            selectAssignment(findAssignmentById(selectedAssignment.getId()));
        }
    }

    private void updateStats() {
        totalAssignmentsLabel.setText(String.valueOf(assignments.size()));
        overdueLabel.setText(String.valueOf(assignments.stream().filter(Assignment::isOverdue).count()));
        inProgressLabel.setText(String.valueOf(assignments.stream().filter(Assignment::isInProgress).count()));
        completedLabel.setText(String.valueOf(assignments.stream().filter(Assignment::isCompleted).count()));
        taskCountLabel.setText(assignments.size() + " assignments");
    }

    private void updateCharts() {
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        statusCounts.put("To Do", 0);
        statusCounts.put("In Progress", 0);
        statusCounts.put("Completed", 0);
        for (Assignment assignment : assignments) {
            statusCounts.computeIfPresent(assignment.getStatus(), (key, value) -> value + 1);
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
        assignmentsList.getChildren().clear();
        List<Assignment> visibleAssignments = getVisibleAssignments();
        if (visibleAssignments.isEmpty()) {
            assignmentsList.getChildren().add(createEmptyState("No assignments match the current search or filter."));
            return;
        }
        for (Assignment assignment : visibleAssignments) {
            assignmentsList.getChildren().add(createAssignmentCard(assignment));
        }
    }

    private List<Assignment> getVisibleAssignments() {
        return assignments.stream()
                .filter(this::matchesFilter)
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
            return Comparator.<Assignment, String>comparing(assignment -> safeText(assignment.getStatus()), String.CASE_INSENSITIVE_ORDER)
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

    private VBox createAssignmentCard(Assignment assignment) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("card", "crud-item");
        if (selectedAssignment != null && selectedAssignment.getId() == assignment.getId()) {
            card.getStyleClass().add("crud-item-selected");
        }
        card.setPadding(new Insets(18));
        card.setAccessibleText("Assignment card for " + assignment.getTitle());

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(6);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label titleLabel = new Label(assignment.getTitle());
        titleLabel.getStyleClass().add("item-title");
        titleLabel.setWrapText(true);

        Label metaLabel = new Label(safeText(assignment.getProjectTitle()) + " | "
                + formatDate(assignment.getStartDate()) + " -> " + formatDate(assignment.getEndDate()));
        metaLabel.getStyleClass().add("item-meta");
        titleBox.getChildren().addAll(titleLabel, metaLabel);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button editButton = new Button("Edit");
        editButton.getStyleClass().add("action-outline-button");
        editButton.setGraphic(new FontIcon("fth-edit-3"));
        editButton.setAccessibleText("Edit assignment " + assignment.getTitle());
        editButton.setOnAction(event -> {
            selectAssignment(assignment);
            event.consume();
        });

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("danger-outline-button");
        deleteButton.setGraphic(new FontIcon("fth-trash-2"));
        deleteButton.setAccessibleText("Delete assignment " + assignment.getTitle());
        deleteButton.setOnAction(event -> {
            if (!confirmDeletion("Delete assignment", "Delete \"" + assignment.getTitle() + "\" from the backlog?")) {
                event.consume();
                return;
            }
            assignmentService.delete(assignment);
            if (selectedAssignment != null && selectedAssignment.getId() == assignment.getId()) {
                clearAssignmentSelection();
            }
            refreshData();
            showFeedback("Assignment deleted successfully.", false);
            event.consume();
        });

        actions.getChildren().addAll(editButton, deleteButton);
        topRow.getChildren().addAll(titleBox, actions);

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getChildren().addAll(
                createBadge(assignment.getPriority(), assignment.getPriorityStyleClass()),
                createBadge(assignment.getStatus(), statusStyleClass(assignment)),
                createBadge(assignment.isOverdue() ? "Overdue" : "On Track", assignment.isOverdue() ? "danger" : "secondary")
        );

        Label descriptionLabel = new Label(defaultText(assignment.getDescription()));
        descriptionLabel.getStyleClass().add("item-desc");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setManaged(!descriptionLabel.getText().isEmpty());
        descriptionLabel.setVisible(!descriptionLabel.getText().isEmpty());

        card.setOnMouseClicked(event -> selectAssignment(assignment));
        card.getChildren().addAll(topRow, badges);
        if (!descriptionLabel.getText().isEmpty()) {
            card.getChildren().add(descriptionLabel);
        }
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

    private void selectAssignment(Assignment assignment) {
        selectedAssignment = assignment;
        renderAssignments();
        populateUpdateForm(assignment);
    }

    private void populateUpdateForm(Assignment assignment) {
        boolean hasSelection = assignment != null;
        updateAssignmentCard.setDisable(!hasSelection);
        updateAssignmentCard.setOpacity(hasSelection ? 1 : 0.55);
        clearUpdateValidation();

        if (!hasSelection) {
            updateAssignmentHintLabel.setText("Select an assignment from the backlog to edit it here.");
            updateAssignmentTitleField.clear();
            updateAssignmentDescriptionArea.clear();
            updateAssignmentStartDatePicker.setValue(null);
            updateAssignmentEndDatePicker.setValue(null);
            updateAssignmentPriorityCombo.setValue("Medium");
            updateAssignmentStatusCombo.setValue("To Do");
            updateAssignmentProjectCombo.setValue(projects.isEmpty() ? null : projects.get(0));
            return;
        }

        updateAssignmentHintLabel.setText("Updating assignment #" + assignment.getId() + ".");
        updateAssignmentTitleField.setText(assignment.getTitle());
        updateAssignmentDescriptionArea.setText(defaultText(assignment.getDescription()));
        updateAssignmentStartDatePicker.setValue(assignment.getStartDate());
        updateAssignmentEndDatePicker.setValue(assignment.getEndDate());
        updateAssignmentPriorityCombo.setValue(assignment.getPriority());
        updateAssignmentStatusCombo.setValue(assignment.getStatus());
        updateAssignmentProjectCombo.setValue(projects.stream()
                .filter(project -> project.getId() == assignment.getProjectId())
                .findFirst()
                .orElse(null));
    }

    private void resetCreateForm() {
        createAssignmentTitleField.clear();
        createAssignmentDescriptionArea.clear();
        createAssignmentStartDatePicker.setValue(null);
        createAssignmentEndDatePicker.setValue(null);
        createAssignmentPriorityCombo.setValue("Medium");
        createAssignmentStatusCombo.setValue("To Do");
        createAssignmentProjectCombo.setValue(projects.isEmpty() ? null : projects.get(0));
        clearCreateValidation();
    }

    private void clearAssignmentSelection() {
        selectedAssignment = null;
        populateUpdateForm(null);
    }

    private void applyFilterState() {
        updateFilterButton(filterAllButton, "ALL".equals(activeFilter));
        updateFilterButton(filterTodoButton, "TODO".equals(activeFilter));
        updateFilterButton(filterProgressButton, "PROGRESS".equals(activeFilter));
        updateFilterButton(filterDoneButton, "DONE".equals(activeFilter));
        updateFilterButton(filterOverdueButton, "OVERDUE".equals(activeFilter));
    }

    private void updateFilterButton(Button button, boolean active) {
        button.getStyleClass().remove("active");
        if (active) {
            button.getStyleClass().add("active");
        }
    }

    private boolean matchesFilter(Assignment assignment) {
        if ("TODO".equals(activeFilter)) {
            return assignment.isTodo();
        }
        if ("PROGRESS".equals(activeFilter)) {
            return assignment.isInProgress();
        }
        if ("DONE".equals(activeFilter)) {
            return assignment.isCompleted();
        }
        return !"OVERDUE".equals(activeFilter) || assignment.isOverdue();
    }

    private boolean matchesSearch(Assignment assignment) {
        if (isBlank(assignmentSearchField.getText())) {
            return true;
        }
        String query = assignmentSearchField.getText().trim().toLowerCase();
        return safeText(assignment.getTitle()).toLowerCase().contains(query)
                || safeText(assignment.getProjectTitle()).toLowerCase().contains(query)
                || defaultText(assignment.getDescription()).toLowerCase().contains(query)
                || safeText(assignment.getStatus()).toLowerCase().contains(query)
                || safeText(assignment.getPriority()).toLowerCase().contains(query)
                || formatDate(assignment.getStartDate()).toLowerCase().contains(query)
                || formatDate(assignment.getEndDate()).toLowerCase().contains(query);
    }

    private boolean validateAssignmentForm(
            ComboBox<Project> projectCombo, Label projectError,
            TextField titleField, Label titleError,
            DatePicker startPicker, Label startError,
            DatePicker endPicker, Label endError) {
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
        return valid;
    }

    private String statusStyleClass(Assignment assignment) {
        if (assignment.isCompleted()) {
            return "success";
        }
        if (assignment.isInProgress()) {
            return "primary";
        }
        if (assignment.isOverdue()) {
            return "danger";
        }
        return "warning";
    }

    private void updateFormAvailability() {
        boolean hasProjects = !projects.isEmpty();
        createAssignmentButton.setDisable(!hasProjects);
        clearCreateAssignmentButton.setDisable(!hasProjects);
        createAssignmentProjectCombo.setDisable(!hasProjects);

        if (hasProjects) {
            createAssignmentHintLabel.setText("Create a new assignment and link it to a project.");
        } else {
            createAssignmentHintLabel.setText("Create a project first, then come back to add assignments.");
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
        updateAssignmentTitleField.textProperty().addListener((obs, oldValue, newValue) -> clearFieldError(updateAssignmentTitleField, updateAssignmentTitleError));
        updateAssignmentProjectCombo.valueProperty().addListener((obs, oldValue, newValue) -> clearFieldError(updateAssignmentProjectCombo, updateAssignmentProjectError));
        updateAssignmentStartDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> clearFieldError(updateAssignmentStartDatePicker, updateAssignmentStartDateError));
        updateAssignmentEndDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> clearFieldError(updateAssignmentEndDatePicker, updateAssignmentEndDateError));
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
        createAssignmentDescriptionArea.setAccessibleText("Assignment description");
        updateAssignmentTitleField.setAccessibleText("Selected assignment title");
        updateAssignmentProjectCombo.setAccessibleText("Selected assignment project");
        updateAssignmentPriorityCombo.setAccessibleText("Selected assignment priority");
        updateAssignmentStatusCombo.setAccessibleText("Selected assignment status");
        updateAssignmentStartDatePicker.setAccessibleText("Selected assignment start date");
        updateAssignmentEndDatePicker.setAccessibleText("Selected assignment end date");
        updateAssignmentDescriptionArea.setAccessibleText("Selected assignment description");
    }

    private void clearCreateValidation() {
        clearFieldError(createAssignmentTitleField, createAssignmentTitleError);
        clearFieldError(createAssignmentProjectCombo, createAssignmentProjectError);
        clearFieldError(createAssignmentStartDatePicker, createAssignmentStartDateError);
        clearFieldError(createAssignmentEndDatePicker, createAssignmentEndDateError);
    }

    private void clearUpdateValidation() {
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
        clearCreateAssignmentButton.setDisable(true);
        updateAssignmentCard.setDisable(true);
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

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "--" : DATE_FORMATTER.format(date);
    }

    private static class ProjectListCell extends ListCell<Project> {
        @Override
        protected void updateItem(Project item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.getTitle());
        }
    }
}
