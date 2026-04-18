package com.studyflow.controllers;

import com.studyflow.models.Assignment;
import com.studyflow.models.AssignmentComment;
import com.studyflow.models.Project;
import com.studyflow.models.User;
import com.studyflow.services.AssignmentService;
import com.studyflow.services.ProjectService;
import com.studyflow.services.ServiceUser;
import com.studyflow.utils.CrudNavigationState;
import com.studyflow.utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AssignmentsController implements Initializable {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final List<String> STATUSES = List.of("To Do", "In Progress", "Completed");
    private static final int DESCRIPTION_LIMIT = 500;

    // ── Toolbar
    @FXML private Label feedbackLabel;
    @FXML private TextField assignmentSearchField;
    @FXML private ComboBox<String> assignmentSortCombo;
    @FXML private Label taskCountLabel;
    @FXML private Label totalAssignmentsLabel;
    @FXML private Label todoSummaryLabel;
    @FXML private Label progressSummaryLabel;
    @FXML private Label completedSummaryLabel;
    @FXML private Label overdueSummaryLabel;

    // ── Board columns
    @FXML private VBox todoColumn;
    @FXML private VBox progressColumn;
    @FXML private VBox completedColumn;
    @FXML private Label todoCountLabel;
    @FXML private Label progressCountLabel;
    @FXML private Label completedCountLabel;

    // ── Create form
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
    @FXML private Label createDescriptionCountLabel;

    // ── Right panel: selected card strip
    @FXML private Label selectedAssignmentTitleLabel;
    @FXML private Label selectedAssignmentStatusLabel;
    @FXML private Label selectedAssignmentOwnerLabel;
    @FXML private Label selectedAssignmentMetaLabel;
    @FXML private Button openEditorButton;

    // ── Right panel: share
    @FXML private Label shareHintLabel;
    @FXML private TextField shareEmailField;
    @FXML private Button shareButton;

    // ── Right panel: comments
    @FXML private Label commentCountLabel;
    @FXML private VBox commentsList;
    @FXML private TextArea newCommentArea;
    @FXML private Button addCommentButton;

    private final AssignmentService assignmentService = new AssignmentService();
    private final ProjectService projectService = new ProjectService();
    private final ServiceUser userService = new ServiceUser();
    private final javafx.scene.control.ContextMenu shareSuggestionsMenu =
            new javafx.scene.control.ContextMenu();

    private final List<Assignment> assignments = new ArrayList<>();
    private final List<Project> ownedProjects = new ArrayList<>();
    private final Map<String, List<Integer>> boardOrder = new LinkedHashMap<>();

    private Assignment selectedAssignment;
    private Assignment draggedAssignment;

    // ── Lifecycle ─────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assignmentSortCombo.getItems().addAll("Newest Added", "Title A-Z", "Due Date", "Priority");
        assignmentSortCombo.setValue("Newest Added");

        createAssignmentPriorityCombo.getItems().addAll("High", "Medium", "Low");
        createAssignmentStatusCombo.getItems().addAll(STATUSES);
        createAssignmentPriorityCombo.setValue("Medium");
        createAssignmentStatusCombo.setValue("To Do");

        configureProjectCombo(createAssignmentProjectCombo);
        bindCreateValidationReset();
        bindSearchAndSort();
        bindDescriptionLimit(createAssignmentDescriptionArea, createDescriptionCountLabel);
        configureShareSuggestions();

        if (!isReady()) {
            disableCreateForm();
            showFeedback("Database unavailable or no user is logged in.", true);
            return;
        }
        refreshData();
        applyNavigationState();
    }

    // ── Create card ───────────────────────────────────────────────────

    @FXML
    private void handleCreateAssignment() {
        clearCreateValidation();
        if (!validateCreateForm()) {
            showFeedback("Please fix the highlighted fields.", true);
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
        assignment.setPriority(defaultValue(createAssignmentPriorityCombo.getValue(), "Medium"));
        assignment.setStatus(defaultValue(createAssignmentStatusCombo.getValue(), "To Do"));

        assignmentService.add(assignment);
        refreshData();
        resetCreateForm();
        showFeedback("Assignment card created.", false);
    }

    @FXML
    private void handleResetCreateAssignment() {
        resetCreateForm();
        showFeedback("Assignment draft cleared.", false);
    }

    // ── Column shortcuts ──────────────────────────────────────────────

    @FXML private void handlePrepareTodoCard()     { createAssignmentStatusCombo.setValue("To Do"); }
    @FXML private void handlePrepareProgressCard() { createAssignmentStatusCombo.setValue("In Progress"); }
    @FXML private void handlePrepareDoneCard()     { createAssignmentStatusCombo.setValue("Completed"); }

    @FXML
    private void handleOpenStatsPage() {
        MainController.showStatisticsPage();
    }

    @FXML
    private void handleOpenSelectedAssignmentEditor() {
        if (selectedAssignment == null) {
            showFeedback("Select an assignment first.", true);
            return;
        }
        openAssignmentEditor(selectedAssignment);
    }

    private void openAssignmentEditor(Assignment assignment) {
        if (assignment == null) {
            showFeedback("No assignment selected.", true);
            return;
        }
        if (!assignment.isOwnedByCurrentUser()) {
            showFeedback("Shared assignments are read-only here.", true);
            return;
        }
        CrudNavigationState.setAssignmentId(assignment.getId());
        MainController.showAssignmentEditPage();
    }

    // ── Right panel: clear selection ─────────────────────────────────

    @FXML
    private void handleClearSelection() {
        clearSelection();
    }

    // ── Right panel: share ────────────────────────────────────────────

    @FXML
    private void handleShareAssignment() {
        if (selectedAssignment == null) {
            showFeedback("Select a card first.", true);
            return;
        }
        if (!selectedAssignment.isOwnedByCurrentUser()) {
            showFeedback("Only the assignment owner can share it.", true);
            return;
        }
        String identifier = defaultText(shareEmailField.getText());
        if (identifier.isEmpty()) {
            showFeedback("Enter a username or email before sharing.", true);
            return;
        }
        boolean shared = assignmentService.shareAssignmentWithUser(
                selectedAssignment.getId(), getCurrentUser().getId(), identifier);
        if (!shared) {
            showFeedback("Could not share — share the project with this user first.", true);
            return;
        }
        shareEmailField.clear();
        shareSuggestionsMenu.hide();
        int id = selectedAssignment.getId();
        refreshData();
        selectAssignment(findById(id));
        showFeedback("Assignment shared successfully.", false);
    }

    // ── Right panel: comments ─────────────────────────────────────────

    @FXML
    private void handleAddComment() {
        if (selectedAssignment == null) {
            showFeedback("Select a card before commenting.", true);
            return;
        }
        boolean added = assignmentService.addComment(
                selectedAssignment.getId(), getCurrentUser().getId(), newCommentArea.getText());
        if (!added) {
            showFeedback("Could not add comment.", true);
            return;
        }
        int id = selectedAssignment.getId();
        newCommentArea.clear();
        refreshData();
        selectAssignment(findById(id));
        showFeedback("Comment added.", false);
    }

    // ── Details modal ─────────────────────────────────────────────────

    private void openDetailsModal(Assignment assignment) {
        com.studyflow.utils.CrudNavigationState.setAssignmentId(assignment.getId());
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/studyflow/views/AssignmentDetails.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Assignment — " + assignment.getTitle());
            stage.setScene(new Scene(root));
            stage.setMinWidth(620);
            stage.setMinHeight(600);
            stage.showAndWait();

            int prevId = assignment.getId();
            refreshData();
            selectAssignment(findById(prevId)); // re-select so right panel stays in sync
        } catch (IOException e) {
            showFeedback("Could not open assignment details: " + e.getMessage(), true);
        }
    }

    // ── Data ──────────────────────────────────────────────────────────

    private void refreshData() {
        List<Project> projects = projectService.getByUserId(getCurrentUser().getId());
        ownedProjects.clear();
        ownedProjects.addAll(projects.stream()
                .filter(Project::isOwnedByCurrentUser)
                .collect(Collectors.toList()));

        assignments.clear();
        assignments.addAll(assignmentService.getByUserId(getCurrentUser().getId()));

        createAssignmentProjectCombo.getItems().setAll(ownedProjects);
        if (!ownedProjects.isEmpty() && createAssignmentProjectCombo.getValue() == null) {
            createAssignmentProjectCombo.setValue(ownedProjects.get(0));
        }
        pruneBoardOrder();
        renderBoard();
        updateCreateAvailability();
    }

    // ── Board rendering ───────────────────────────────────────────────

    private void renderBoard() {
        todoColumn.getChildren().clear();
        progressColumn.getChildren().clear();
        completedColumn.getChildren().clear();

        List<Assignment> visible = getVisibleAssignments();
        Map<String, List<Assignment>> grouped = new LinkedHashMap<>();
        grouped.put("To Do",       orderedByBoard("To Do",       visible));
        grouped.put("In Progress", orderedByBoard("In Progress", visible));
        grouped.put("Completed",   orderedByBoard("Completed",   visible));

        fillColumn(todoColumn,      grouped.get("To Do"),       "To Do");
        fillColumn(progressColumn,  grouped.get("In Progress"), "In Progress");
        fillColumn(completedColumn, grouped.get("Completed"),   "Completed");

        todoCountLabel.setText(String.valueOf(grouped.get("To Do").size()));
        progressCountLabel.setText(String.valueOf(grouped.get("In Progress").size()));
        completedCountLabel.setText(String.valueOf(grouped.get("Completed").size()));
        taskCountLabel.setText(visible.size() + " visible assignments");
        updateSummaryCards(visible, grouped);

        // Refresh right panel after board re-render
        if (selectedAssignment != null) {
            Assignment refreshed = findById(selectedAssignment.getId());
            selectedAssignment = refreshed;
        }
        renderRightPanel();
    }

    private void fillColumn(VBox column, List<Assignment> items, String status) {
        configureColumnDrop(column, status);
        if (items.isEmpty()) {
            column.getChildren().add(emptyColumnState(status));
            return;
        }
        for (Assignment a : items) {
            column.getChildren().add(createCard(a));
        }
    }

    // ── Card factory ──────────────────────────────────────────────────

    private VBox createCard(Assignment assignment) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("card", "board-card");
        if (selectedAssignment != null && selectedAssignment.getId() == assignment.getId()) {
            card.getStyleClass().add("crud-item-selected");
        }
        card.setPadding(new Insets(14));

        // Top: project badge + priority badge
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getChildren().addAll(
                badge(safe(assignment.getProjectTitle()), "secondary"),
                spacer(),
                badge(assignment.getPriority(), assignment.getPriorityStyleClass()));

        // Title
        Label title = new Label(assignment.getTitle());
        title.getStyleClass().add("item-title");
        title.setWrapText(true);

        // Meta
        Label meta = new Label("Due " + fmt(assignment.getEndDate()) + "  ·  " + ownerText(assignment));
        meta.getStyleClass().add("item-meta");
        meta.setWrapText(true);

        card.getChildren().addAll(top, title, meta);

        // Description preview
        String desc = defaultText(assignment.getDescription());
        if (!desc.isEmpty()) {
            Label descLabel = new Label(desc.length() > 90 ? desc.substring(0, 90) + "…" : desc);
            descLabel.getStyleClass().add("item-desc");
            descLabel.setWrapText(true);
            card.getChildren().add(descLabel);
        }

        // Footer: status + ownership + edit entry point
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getChildren().addAll(
                badge(assignment.getStatus(), statusStyleClass(assignment)),
                badge(assignment.isOwnedByCurrentUser() ? "Owned" : "Shared",
                        assignment.isOwnedByCurrentUser() ? "primary" : "accent"),
                spacer());

        Button detailsBtn = new Button(assignment.isOwnedByCurrentUser() ? "Edit" : "Read only");
        detailsBtn.getStyleClass().add(assignment.isOwnedByCurrentUser() ? "action-outline-button" : "btn-ghost");
        detailsBtn.setDisable(!assignment.isOwnedByCurrentUser());
        FontIcon icon = new FontIcon(assignment.isOwnedByCurrentUser() ? "fth-edit-2" : "fth-lock");
        icon.setIconSize(13);
        detailsBtn.setGraphic(icon);
        detailsBtn.setOnAction(e -> {
            e.consume();
            openAssignmentEditor(assignment);
        });
        footer.getChildren().add(detailsBtn);
        card.getChildren().add(footer);

        // Click to select
        card.setOnMouseClicked(e -> selectAssignment(assignment));
        configureCardDrag(card, assignment);
        configureCardDrop(card, assignment);
        return card;
    }

    private VBox emptyColumnState(String status) {
        VBox box = new VBox(6);
        box.getStyleClass().add("board-empty-state");
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(18));
        Label t = new Label("No cards");
        t.getStyleClass().add("item-title");
        Label s = new Label("Drop here or click Add for " + status + ".");
        s.getStyleClass().add("text-muted");
        s.setWrapText(true);
        box.getChildren().addAll(t, s);
        return box;
    }

    // ── Right panel rendering ─────────────────────────────────────────

    private void selectAssignment(Assignment assignment) {
        selectedAssignment = assignment;
        renderBoard(); // re-renders board to update selected highlight + right panel
    }

    private void clearSelection() {
        selectedAssignment = null;
        renderRightPanel();
        renderBoard();
    }

    private void renderRightPanel() {
        if (selectedAssignment == null) {
            selectedAssignmentTitleLabel.setText("No assignment selected");
            selectedAssignmentStatusLabel.setText("--");
            selectedAssignmentStatusLabel.getStyleClass().setAll("badge", "secondary");
            selectedAssignmentOwnerLabel.setText("--");
            selectedAssignmentMetaLabel.setText(
                    "Click a card on the board to share it or leave a comment.");
            openEditorButton.setDisable(true);
            openEditorButton.setText("Open Separate Editor");
            // Share
            shareHintLabel.setText("Select a card to share it by username or email.");
            shareEmailField.setDisable(true);
            shareButton.setDisable(true);
            // Comments
            commentCountLabel.setText("0 comments");
            commentsList.getChildren().clear();
            commentsList.getChildren().add(commentEmptyState());
            newCommentArea.setDisable(true);
            addCommentButton.setDisable(true);
            return;
        }

        // Selected card strip
        selectedAssignmentTitleLabel.setText(selectedAssignment.getTitle());
        selectedAssignmentStatusLabel.setText(selectedAssignment.getStatus());
        selectedAssignmentStatusLabel.getStyleClass().setAll("badge", statusStyleClass(selectedAssignment));
        selectedAssignmentOwnerLabel.setText(ownerText(selectedAssignment));
        selectedAssignmentMetaLabel.setText(
                "Project: " + safe(selectedAssignment.getProjectTitle())
                        + "  ·  Due " + fmt(selectedAssignment.getEndDate()));

        openEditorButton.setDisable(!selectedAssignment.isOwnedByCurrentUser());
        openEditorButton.setText(selectedAssignment.isOwnedByCurrentUser()
                ? "Open Separate Editor"
                : "Read Only Assignment");

        // Share panel
        boolean ownedShare = selectedAssignment.isOwnedByCurrentUser();
        shareEmailField.setDisable(!ownedShare);
        shareButton.setDisable(!ownedShare);
        if (!ownedShare) {
            shareHintLabel.setText("Only the owner can share this assignment.");
        } else {
            shareHintLabel.setText("Share \"" + selectedAssignment.getTitle() + "\" by username or email.");
        }

        // Comments
        newCommentArea.setDisable(false);
        addCommentButton.setDisable(false);
        renderComments();
    }

    private void renderComments() {
        if (selectedAssignment == null) return;
        List<AssignmentComment> comments = assignmentService.getCommentsByAssignmentId(
                selectedAssignment.getId(), getCurrentUser().getId());
        commentCountLabel.setText(comments.size() + (comments.size() == 1 ? " comment" : " comments"));
        commentsList.getChildren().clear();
        if (comments.isEmpty()) {
            commentsList.getChildren().add(commentEmptyState());
            return;
        }
        for (AssignmentComment c : comments) {
            commentsList.getChildren().add(commentRow(c));
        }
    }

    private Label commentEmptyState() {
        Label l = new Label("No comments yet — be the first!");
        l.getStyleClass().add("text-muted");
        l.setWrapText(true);
        return l;
    }

    private VBox commentRow(AssignmentComment comment) {
        VBox box = new VBox(4);
        box.getStyleClass().add("detail-row");
        box.setPadding(new Insets(10, 12, 10, 12));
        Label meta = new Label(comment.getAuthorName() + "  ·  " + fmtDt(comment.getCreatedAt()));
        meta.getStyleClass().add("item-meta");
        Label content = new Label(defaultText(comment.getContent()));
        content.getStyleClass().add("item-desc");
        content.setWrapText(true);
        box.getChildren().addAll(meta, content);
        return box;
    }

    // ── Share autocomplete ────────────────────────────────────────────

    private void updateSummaryCards(List<Assignment> visible, Map<String, List<Assignment>> grouped) {
        totalAssignmentsLabel.setText(String.valueOf(visible.size()));
        todoSummaryLabel.setText(String.valueOf(grouped.getOrDefault("To Do", List.of()).size()));
        progressSummaryLabel.setText(String.valueOf(grouped.getOrDefault("In Progress", List.of()).size()));
        completedSummaryLabel.setText(String.valueOf(grouped.getOrDefault("Completed", List.of()).size()));
        overdueSummaryLabel.setText(String.valueOf(visible.stream().filter(Assignment::isOverdue).count()));
    }

    private void applyNavigationState() {
        CrudNavigationState.Feedback feedback = CrudNavigationState.consumeFeedback();
        Integer assignmentId = CrudNavigationState.consumeAssignmentId();
        if (assignmentId != null) {
            Assignment restored = findById(assignmentId);
            if (restored != null) {
                selectAssignment(restored);
            } else {
                clearSelection();
            }
        } else {
            clearSelection();
        }
        if (feedback != null) {
            showFeedback(feedback.message(), feedback.error());
        }
    }

    private void configureShareSuggestions() {
        shareEmailField.textProperty().addListener((obs, o, n) -> showSuggestions(n));
        shareEmailField.focusedProperty().addListener((obs, o, focused) -> {
            if (!focused) shareSuggestionsMenu.hide();
        });
    }

    private void showSuggestions(String query) {
        if (getCurrentUser() == null || defaultText(query).length() < 1) {
            shareSuggestionsMenu.hide();
            return;
        }
        List<User> users = userService.searchUsers(query, getCurrentUser().getId(), 6);
        if (users.isEmpty()) { shareSuggestionsMenu.hide(); return; }
        List<CustomMenuItem> items = users.stream().map(this::suggestionItem).collect(Collectors.toList());
        shareSuggestionsMenu.getItems().setAll(items);
        if (!shareSuggestionsMenu.isShowing()) {
            shareSuggestionsMenu.show(shareEmailField, javafx.geometry.Side.BOTTOM, 0, 4);
        }
    }

    private CustomMenuItem suggestionItem(User user) {
        VBox content = new VBox(2);
        Label name = new Label(safe(user.getUsername()));
        name.getStyleClass().add("item-title");
        name.setStyle("-fx-font-size: 13px;");
        Label meta = new Label("@" + safe(user.getUsername()) + "  ·  " + safe(user.getEmail()));
        meta.getStyleClass().add("item-meta");
        content.getChildren().addAll(name, meta);
        CustomMenuItem item = new CustomMenuItem(content, true);
        item.setOnAction(e -> {
            shareEmailField.setText(!defaultText(user.getUsername()).isEmpty()
                    ? user.getUsername() : user.getEmail());
            shareEmailField.positionCaret(shareEmailField.getText().length());
            shareSuggestionsMenu.hide();
        });
        return item;
    }

    // ── Drag & Drop ───────────────────────────────────────────────────

    private void configureCardDrag(VBox card, Assignment assignment) {
        if (!assignment.isOwnedByCurrentUser()) return;
        card.setOnDragDetected(event -> {
            draggedAssignment = assignment;
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent cc = new ClipboardContent();
            cc.putString(String.valueOf(assignment.getId()));
            db.setContent(cc);
            card.setOpacity(0.55);
            event.consume();
        });
        card.setOnDragDone(event -> {
            card.setOpacity(1.0);
            draggedAssignment = null;
        });
    }

    private void configureCardDrop(VBox card, Assignment targetAssignment) {
        card.setOnDragOver(event -> {
            if (draggedAssignment != null && draggedAssignment.getId() != targetAssignment.getId())
                event.acceptTransferModes(TransferMode.MOVE);
            event.consume();
        });
        card.setOnDragDropped(event -> {
            if (draggedAssignment == null) return;
            moveAssignment(draggedAssignment, targetAssignment.getStatus(), targetAssignment.getId());
            event.setDropCompleted(true);
            event.consume();
        });
    }

    private void configureColumnDrop(VBox column, String targetStatus) {
        column.setOnDragOver(event -> {
            if (draggedAssignment != null) event.acceptTransferModes(TransferMode.MOVE);
            event.consume();
        });
        column.setOnDragDropped(event -> {
            if (draggedAssignment == null) return;
            moveAssignment(draggedAssignment, targetStatus, null);
            event.setDropCompleted(true);
            event.consume();
        });
    }

    private void moveAssignment(Assignment assignment, String targetStatus, Integer beforeId) {
        if (assignment == null || !assignment.isOwnedByCurrentUser()) return;

        String normTarget  = defaultValue(targetStatus, "To Do");
        String normCurrent = defaultValue(assignment.getStatus(), "To Do");

        boardOrder.computeIfAbsent(normCurrent, k -> new ArrayList<>())
                .remove(Integer.valueOf(assignment.getId()));
        List<Integer> dest = boardOrder.computeIfAbsent(normTarget, k -> new ArrayList<>());
        dest.remove(Integer.valueOf(assignment.getId()));
        int at = beforeId == null ? dest.size()
                : (dest.contains(beforeId) ? dest.indexOf(beforeId) : dest.size());
        dest.add(Math.max(0, at), assignment.getId());

        if (!normCurrent.equalsIgnoreCase(normTarget)) {
            assignment.setStatus(normTarget);
            assignmentService.update(assignment); // sets end_date = today on status change
            int prevId = assignment.getId();
            refreshData();
            selectAssignment(findById(prevId));
            showFeedback("Moved to \"" + normTarget + "\". End date set to today.", false);
        } else {
            renderBoard();
        }
        draggedAssignment = null;
    }

    // ── Filtering / sorting ───────────────────────────────────────────

    private void bindSearchAndSort() {
        assignmentSearchField.textProperty().addListener((obs, o, n) -> renderBoard());
        assignmentSortCombo.valueProperty().addListener((obs, o, n) -> renderBoard());
    }

    private List<Assignment> getVisibleAssignments() {
        return assignments.stream()
                .filter(this::matchesSearch)
                .sorted(buildComparator())
                .collect(Collectors.toList());
    }

    private List<Assignment> orderedByBoard(String status, List<Assignment> visible) {
        Map<Integer, Assignment> byId = visible.stream()
                .filter(a -> status.equalsIgnoreCase(a.getStatus()))
                .collect(Collectors.toMap(Assignment::getId, a -> a));
        List<Assignment> ordered = new ArrayList<>();
        for (Integer id : boardOrder.getOrDefault(status, List.of())) {
            Assignment a = byId.remove(id);
            if (a != null) ordered.add(a);
        }
        ordered.addAll(byId.values().stream().sorted(buildComparator()).collect(Collectors.toList()));
        return ordered;
    }

    private Comparator<Assignment> buildComparator() {
        String sort = assignmentSortCombo.getValue();
        if ("Title A-Z".equals(sort))
            return Comparator.comparing(a -> safe(a.getTitle()), String.CASE_INSENSITIVE_ORDER);
        if ("Due Date".equals(sort))
            return Comparator.comparing(Assignment::getEndDate, Comparator.nullsLast(LocalDate::compareTo))
                    .thenComparing(a -> safe(a.getTitle()), String.CASE_INSENSITIVE_ORDER);
        if ("Priority".equals(sort))
            return Comparator.comparingInt(this::priorityRank)
                    .thenComparing(a -> safe(a.getTitle()), String.CASE_INSENSITIVE_ORDER);
        return Comparator
                .comparing(Assignment::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Assignment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int priorityRank(Assignment a) {
        if ("High".equalsIgnoreCase(a.getPriority()))   return 0;
        if ("Medium".equalsIgnoreCase(a.getPriority())) return 1;
        return 2;
    }

    private boolean matchesSearch(Assignment a) {
        String q = defaultText(assignmentSearchField.getText()).toLowerCase();
        if (q.isEmpty()) return true;
        return safe(a.getTitle()).toLowerCase().contains(q)
                || safe(a.getProjectTitle()).toLowerCase().contains(q)
                || safe(a.getOwnerName()).toLowerCase().contains(q)
                || safe(a.getStatus()).toLowerCase().contains(q)
                || safe(a.getPriority()).toLowerCase().contains(q)
                || defaultText(a.getDescription()).toLowerCase().contains(q);
    }

    // ── Board order maintenance ───────────────────────────────────────

    private void pruneBoardOrder() {
        Map<String, List<Integer>> pruned = new HashMap<>();
        for (String status : STATUSES) {
            List<Integer> ids = assignments.stream()
                    .filter(a -> status.equalsIgnoreCase(a.getStatus()))
                    .map(Assignment::getId)
                    .collect(Collectors.toList());
            List<Integer> existing = new ArrayList<>(boardOrder.getOrDefault(status, List.of()));
            existing.retainAll(ids);
            pruned.put(status, existing);
        }
        boardOrder.clear();
        boardOrder.putAll(pruned);
    }

    // ── Create form helpers ───────────────────────────────────────────

    private boolean validateCreateForm() {
        boolean valid = true;
        if (createAssignmentProjectCombo.getValue() == null) {
            showFieldError(createAssignmentProjectCombo, createAssignmentProjectError, "Project is required.");
            valid = false;
        }
        String title = defaultText(createAssignmentTitleField.getText());
        if (title.isEmpty()) {
            showFieldError(createAssignmentTitleField, createAssignmentTitleError, "Title is required.");
            valid = false;
        } else if (title.length() < 3) {
            showFieldError(createAssignmentTitleField, createAssignmentTitleError, "Min 3 characters.");
            valid = false;
        }
        if (createAssignmentStartDatePicker.getValue() == null) {
            showFieldError(createAssignmentStartDatePicker, createAssignmentStartDateError, "Start date is required.");
            valid = false;
        }
        if (createAssignmentEndDatePicker.getValue() == null) {
            showFieldError(createAssignmentEndDatePicker, createAssignmentEndDateError, "End date is required.");
            valid = false;
        }
        if (createAssignmentStartDatePicker.getValue() != null
                && createAssignmentEndDatePicker.getValue() != null
                && !createAssignmentEndDatePicker.getValue().isAfter(createAssignmentStartDatePicker.getValue())) {
            showFieldError(createAssignmentEndDatePicker, createAssignmentEndDateError, "End must be after start.");
            valid = false;
        }
        return valid;
    }

    private void bindCreateValidationReset() {
        createAssignmentTitleField.textProperty().addListener((obs, o, n) ->
                clearFieldError(createAssignmentTitleField, createAssignmentTitleError));
        createAssignmentProjectCombo.valueProperty().addListener((obs, o, n) ->
                clearFieldError(createAssignmentProjectCombo, createAssignmentProjectError));
        createAssignmentStartDatePicker.valueProperty().addListener((obs, o, n) ->
                clearFieldError(createAssignmentStartDatePicker, createAssignmentStartDateError));
        createAssignmentEndDatePicker.valueProperty().addListener((obs, o, n) ->
                clearFieldError(createAssignmentEndDatePicker, createAssignmentEndDateError));
    }

    private void clearCreateValidation() {
        clearFieldError(createAssignmentTitleField, createAssignmentTitleError);
        clearFieldError(createAssignmentProjectCombo, createAssignmentProjectError);
        clearFieldError(createAssignmentStartDatePicker, createAssignmentStartDateError);
        clearFieldError(createAssignmentEndDatePicker, createAssignmentEndDateError);
    }

    private void resetCreateForm() {
        createAssignmentTitleField.clear();
        createAssignmentDescriptionArea.clear();
        createAssignmentStartDatePicker.setValue(null);
        createAssignmentEndDatePicker.setValue(null);
        createAssignmentPriorityCombo.setValue("Medium");
        createAssignmentStatusCombo.setValue("To Do");
        createAssignmentProjectCombo.setValue(ownedProjects.isEmpty() ? null : ownedProjects.get(0));
        clearCreateValidation();
    }

    private void updateCreateAvailability() {
        boolean has = !ownedProjects.isEmpty();
        createAssignmentProjectCombo.setDisable(!has);
        createAssignmentTitleField.setDisable(!has);
        createAssignmentDescriptionArea.setDisable(!has);
        createAssignmentStartDatePicker.setDisable(!has);
        createAssignmentEndDatePicker.setDisable(!has);
        createAssignmentPriorityCombo.setDisable(!has);
        createAssignmentStatusCombo.setDisable(!has);
        if (!has) showFeedback("Create your own project first, then add cards.", true);
    }

    private void disableCreateForm() {
        createAssignmentProjectCombo.setDisable(true);
        createAssignmentTitleField.setDisable(true);
        createAssignmentDescriptionArea.setDisable(true);
        createAssignmentStartDatePicker.setDisable(true);
        createAssignmentEndDatePicker.setDisable(true);
        createAssignmentPriorityCombo.setDisable(true);
        createAssignmentStatusCombo.setDisable(true);
    }

    // ── 500-char description limit ────────────────────────────────────

    private void bindDescriptionLimit(TextArea area, Label counter) {
        area.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > DESCRIPTION_LIMIT) {
                area.setText(newVal.substring(0, DESCRIPTION_LIMIT));
                area.positionCaret(DESCRIPTION_LIMIT);
                return;
            }
            counter.setText(newVal.length() + "/" + DESCRIPTION_LIMIT);
        });
    }

    // ── Field error helpers ───────────────────────────────────────────

    private void showFieldError(Control control, Label errorLabel, String message) {
        if (!control.getStyleClass().contains("field-invalid"))
            control.getStyleClass().add("field-invalid");
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

    // ── Utilities ─────────────────────────────────────────────────────

    private void configureProjectCombo(ComboBox<Project> combo) {
        combo.setButtonCell(new ProjectCell());
        combo.setCellFactory(list -> new ProjectCell());
    }

    private Assignment findById(int id) {
        return assignments.stream().filter(a -> a.getId() == id).findFirst().orElse(null);
    }

    private boolean isReady() {
        return assignmentService.isDatabaseAvailable()
                && projectService.isDatabaseAvailable()
                && userService.isDatabaseAvailable()
                && getCurrentUser() != null;
    }

    private User getCurrentUser() { return UserSession.getInstance().getCurrentUser(); }
    private String defaultText(String v) { return v == null ? "" : v.trim(); }
    private String defaultValue(String v, String fb) { return v == null || v.isBlank() ? fb : v; }
    private String safe(String v) { return v == null ? "" : v; }

    private String fmt(LocalDate d) { return d == null ? "--" : DATE_FORMATTER.format(d); }
    private String fmtDt(LocalDateTime dt) { return dt == null ? "--" : DATE_TIME_FORMATTER.format(dt); }

    private String ownerText(Assignment a) {
        return a.isOwnedByCurrentUser() ? "Owned by you" : "Shared by " + safe(a.getOwnerName());
    }

    private String statusStyleClass(Assignment a) {
        if (a.isCompleted()) return "success";
        if (a.isInProgress()) return "primary";
        if (a.isOverdue())    return "danger";
        return "warning";
    }

    private Label badge(String text, String styleClass) {
        Label l = new Label(text);
        l.getStyleClass().addAll("badge", styleClass);
        return l;
    }

    private Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private static class ProjectCell extends ListCell<Project> {
        @Override
        protected void updateItem(Project item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.getTitle());
        }
    }
}
