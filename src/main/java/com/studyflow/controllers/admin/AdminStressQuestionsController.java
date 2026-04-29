package com.studyflow.controllers.admin;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.studyflow.models.QuestionStress;
import com.studyflow.services.ServiceQuestionStress;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminStressQuestionsController implements Initializable {
    private static final int MIN_TEXT_LENGTH = 5;
    private static final int MAX_TEXT_LENGTH = 1000;
    private static final String QUESTION_VALIDATION_RULE = "Condition: la question est obligatoire, minimum 5 caractères et maximum 1000 caractères.";
    private static final String INVALID_FIELD_STYLE = "-fx-border-color: #ef4444; -fx-border-width: 1.6; -fx-border-radius: 8;";
    private static final long DELETE_CONFIRM_WINDOW_MS = 5000;


    @FXML private ScrollPane listSection;
    @FXML private ScrollPane formSection;
    @FXML private ScrollPane detailSection;

    @FXML private Label totalQuestionsLabel;
    @FXML private Label activeQuestionsLabel;
    @FXML private Label inactiveQuestionsLabel;
    @FXML private Label listFeedbackLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> orderCombo;
    @FXML private VBox questionsListBox;

    @FXML private Label formTitleLabel;
    @FXML private Label formErrorLabel;
    @FXML private TextArea questionTextArea;
    @FXML private CheckBox activeCheckBox;
    @FXML private Button saveButton;

    @FXML private Label detailHeaderLabel;
    @FXML private Label detailCreatedLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label detailQuestionLabel;
    @FXML private Label detailUpdatedLabel;
    @FXML private Label notificationLabel;

    private final ServiceQuestionStress service = new ServiceQuestionStress();

    private QuestionStress editingQuestion;
    private QuestionStress detailQuestion;
    private boolean reorderEnabled;
    private Integer pendingDeleteId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        questionsListBox.setSpacing(10);
        enforceTextLimit(questionTextArea, MAX_TEXT_LENGTH);
        initCombos();
        addSearchListener();
        questionTextArea.textProperty().addListener((obs, oldVal, newVal) -> questionTextArea.getStyleClass().remove("admin-field-error"));
        loadQuestions();
    }

    private void initCombos() {
        sortCombo.setItems(FXCollections.observableArrayList("Sort by ID", "Sort by Number", "Sort by Date"));
        sortCombo.getSelectionModel().select("Sort by Number");
        sortCombo.setOnAction(e -> loadQuestions());

        orderCombo.setItems(FXCollections.observableArrayList("Ascending", "Descending"));
        orderCombo.getSelectionModel().selectFirst();
        orderCombo.setOnAction(e -> loadQuestions());
    }

    private void addSearchListener() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadQuestions());
    }

    private void loadQuestions() {
        List<QuestionStress> all = service.findAll();

        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        if (!search.isEmpty()) {
            all = all.stream()
                    .filter(q -> {
                        String text = q.getQuestionText() == null ? "" : q.getQuestionText().toLowerCase();
                        return text.contains(search);
                    })
                    .collect(Collectors.toList());
        }

        String sortVal = sortCombo.getValue();
        boolean asc = !"Descending".equals(orderCombo.getValue());

        if ("Sort by Number".equals(sortVal)) {
            all.sort((a, b) -> asc
                    ? Integer.compare(a.getQuestionNumber(), b.getQuestionNumber())
                    : Integer.compare(b.getQuestionNumber(), a.getQuestionNumber()));
        } else if ("Sort by Date".equals(sortVal)) {
            all.sort((a, b) -> {
                if (a.getCreatedAt() == null && b.getCreatedAt() == null) {
                    return 0;
                }
                if (a.getCreatedAt() == null) {
                    return asc ? -1 : 1;
                }
                if (b.getCreatedAt() == null) {
                    return asc ? 1 : -1;
                }
                int cmp = a.getCreatedAt().compareTo(b.getCreatedAt());
                return asc ? cmp : -cmp;
            });
        } else {
            all.sort((a, b) -> asc
                    ? Integer.compare(a.getId(), b.getId())
                    : Integer.compare(b.getId(), a.getId()));
        }

        reorderEnabled = search.isEmpty() && "Sort by Number".equals(sortVal) && asc;
        updateStats(all);
        renderTable(all);
    }

    private void updateStats(List<QuestionStress> questions) {
        long active = questions.stream().filter(QuestionStress::isActive).count();
        long inactive = questions.size() - active;
        totalQuestionsLabel.setText(String.valueOf(questions.size()));
        activeQuestionsLabel.setText(String.valueOf(active));
        inactiveQuestionsLabel.setText(String.valueOf(inactive));
    }

    private void renderTable(List<QuestionStress> questions) {
        questionsListBox.getChildren().clear();

        if (questions.isEmpty()) {
            Label empty = new Label("No questions found. Start by adding your first stress assessment question.");
            empty.setWrapText(true);
            empty.getStyleClass().add("text-small");
            VBox emptyBox = new VBox(empty);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40));
            questionsListBox.getChildren().add(emptyBox);
            return;
        }

        for (QuestionStress q : questions) {
            questionsListBox.getChildren().add(buildTableRow(q));
        }
    }

    private HBox buildTableRow(QuestionStress q) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 16, 14, 16));
        row.getStyleClass().add("stress-list-item");

        Label numLabel = new Label(String.valueOf(q.getQuestionNumber()));
        numLabel.getStyleClass().add("stress-list-number");

        String fullText = q.getQuestionText() == null ? "" : q.getQuestionText();
        String truncated = fullText.length() > 120 ? fullText.substring(0, 120) + "..." : fullText;
        Label textLabel = new Label(truncated);
        textLabel.getStyleClass().add("stress-list-question");
        textLabel.setWrapText(true);

        Label statusBadge = buildStatusBadge(q.isActive());

        String dateStr = q.getCreatedAt() != null
                ? q.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                : "-";
        Label dateLabel = new Label(dateStr);
        dateLabel.getStyleClass().add("stress-list-date");

        VBox contentBox = new VBox(6, textLabel, new HBox(8, statusBadge, dateLabel));
        contentBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(contentBox, javafx.scene.layout.Priority.ALWAYS);

        HBox actions = buildActionButtons(q);
        actions.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(numLabel, contentBox, actions);
        configureRowDragAndDrop(row, q);
        return row;
    }

    private Label buildStatusBadge(boolean active) {
        Label badge = new Label(active ? "Active" : "Inactive");
        badge.getStyleClass().addAll("badge", active ? "success" : "secondary");
        badge.setAlignment(Pos.CENTER);
        return badge;
    }

    private HBox buildActionButtons(QuestionStress q) {
        Button viewBtn = new Button("View");
        Button editBtn = new Button("Edit");
        Button deleteBtn = new Button("Delete");
        viewBtn.getStyleClass().addAll("btn-ghost", "stress-action-btn");
        editBtn.getStyleClass().addAll("btn-secondary", "stress-action-btn");
        deleteBtn.getStyleClass().addAll("btn-danger", "stress-action-btn");
        viewBtn.setOnAction(e -> showDetail(q));
        editBtn.setOnAction(e -> showEditForm(q));
        deleteBtn.setOnAction(e -> handleDelete(q));
        HBox box = new HBox(6, viewBtn, editBtn, deleteBtn);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void configureRowDragAndDrop(HBox row, QuestionStress targetQuestion) {
        if (!reorderEnabled) {
            return;
        }

        row.getStyleClass().add("stress-draggable-row");

        row.setOnDragDetected(event -> {
            Dragboard dragboard = row.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(targetQuestion.getId()));
            dragboard.setContent(content);
            row.getStyleClass().add("stress-drag-source");
            event.consume();
        });

        row.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasString() && !db.getString().equals(String.valueOf(targetQuestion.getId()))) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        row.setOnDragEntered(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasString() && !db.getString().equals(String.valueOf(targetQuestion.getId()))) {
                row.getStyleClass().add("stress-drag-target");
            }
            event.consume();
        });

        row.setOnDragExited(event -> {
            row.getStyleClass().remove("stress-drag-target");
            event.consume();
        });

        row.setOnDragDone(event -> {
            row.getStyleClass().remove("stress-drag-source");
            event.consume();
        });

        row.setOnDragDropped(event -> {
            boolean success = false;
            Dragboard db = event.getDragboard();
            if (db.hasString()) {
                try {
                    int draggedId = Integer.parseInt(db.getString());
                    reorderQuestions(draggedId, targetQuestion.getId());
                    success = true;
                } catch (NumberFormatException ignored) {
                    success = false;
                }
            }
            row.getStyleClass().remove("stress-drag-target");
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void reorderQuestions(int draggedId, int targetId) {
        if (draggedId == targetId) {
            return;
        }

        List<QuestionStress> ordered = service.findAll().stream()
                .sorted(Comparator.comparingInt(QuestionStress::getQuestionNumber))
                .collect(Collectors.toList());

        int fromIndex = -1;
        int toIndex = -1;
        for (int i = 0; i < ordered.size(); i++) {
            int id = ordered.get(i).getId();
            if (id == draggedId) {
                fromIndex = i;
            }
            if (id == targetId) {
                toIndex = i;
            }
        }
        if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) {
            return;
        }

        QuestionStress moved = ordered.remove(fromIndex);
        if (fromIndex < toIndex) {
            toIndex--;
        }
        ordered.add(toIndex, moved);

        List<Integer> orderedIds = ordered.stream().map(QuestionStress::getId).collect(Collectors.toList());
        service.reorderByIds(orderedIds);
        loadQuestions();
    }

    private void showSection(Node target) {
        listSection.setVisible(false);
        listSection.setManaged(false);
        formSection.setVisible(false);
        formSection.setManaged(false);
        detailSection.setVisible(false);
        detailSection.setManaged(false);
        target.setVisible(true);
        target.setManaged(true);
    }

    @FXML
    private void handleBackToList() {
        editingQuestion = null;
        detailQuestion = null;
        pendingDeleteId = null;
        showSection(listSection);
        loadQuestions();
    }

    @FXML
    private void handleAddQuestion() {
        editingQuestion = null;
        formTitleLabel.setText("New Question");
        saveButton.setText("Create Question");
        clearForm();
        hideFormError();
        hideListFeedback();
        showSection(formSection);
    }

    private void showEditForm(QuestionStress q) {
        editingQuestion = q;
        formTitleLabel.setText("Edit Question #" + q.getQuestionNumber());
        saveButton.setText("Update Question");
        questionTextArea.setText(q.getQuestionText() == null ? "" : q.getQuestionText());
        activeCheckBox.setSelected(q.isActive());
        markFieldValid(questionTextArea);
        hideFormError();
        questionTextArea.getStyleClass().remove("admin-field-error");
        showSection(formSection);
    }

    @FXML
    private void handleSaveQuestion() {
        hideFormError();
        questionTextArea.getStyleClass().remove("admin-field-error");
        String text = questionTextArea.getText() == null ? "" : questionTextArea.getText().trim();
        if (text.isEmpty()) {
            markFieldError(questionTextArea);
            showFormError("Question text is required.");
            return;
        }
        if (text.length() < MIN_TEXT_LENGTH) {
            markFieldInvalid(questionTextArea);
            showFormError(QUESTION_VALIDATION_RULE);
            return;
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            markFieldInvalid(questionTextArea);
            showFormError(QUESTION_VALIDATION_RULE);
            return;
        }
        markFieldValid(questionTextArea);

        boolean active = activeCheckBox.isSelected();

        if (editingQuestion == null) {
            QuestionStress newQ = new QuestionStress();
            newQ.setQuestionNumber(service.nextPosition());
            newQ.setQuestionText(text);
            newQ.setActive(active);
            newQ.setCreatedAt(LocalDateTime.now());
            service.save(newQ);
            showListFeedback("Question created successfully!", true);
        } else {
            editingQuestion.setQuestionText(text);
            editingQuestion.setActive(active);
            editingQuestion.setUpdatedAt(LocalDateTime.now());
            service.update(editingQuestion);
            showListFeedback("Question updated successfully!", true);
        }
        handleBackToList();
    }

    private void showDetail(QuestionStress q) {
        detailQuestion = q;
        detailHeaderLabel.setText("Question #" + q.getQuestionNumber());
        detailCreatedLabel.setText(q.getCreatedAt() != null
                ? "Created on " + q.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                : "Created on -");
        detailStatusLabel.setText(q.isActive() ? "Active" : "Inactive");
        detailStatusLabel.getStyleClass().removeAll("success", "secondary");
        if (!detailStatusLabel.getStyleClass().contains("badge")) {
            detailStatusLabel.getStyleClass().add("badge");
        }
        detailStatusLabel.getStyleClass().add(q.isActive() ? "success" : "secondary");
        detailQuestionLabel.setText(q.getQuestionText() == null ? "" : q.getQuestionText());
        detailUpdatedLabel.setText(q.getUpdatedAt() != null
                ? "Last updated: " + q.getUpdatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm"))
                : "No updates yet.");
        showSection(detailSection);
    }

    @FXML
    private void handleDetailEdit() {
        if (detailQuestion != null) {
            showEditForm(detailQuestion);
        }
    }

    @FXML
    private void handleDetailDelete() {
        if (detailQuestion != null) {
            handleDelete(detailQuestion);
        }
    }

    private void handleDelete(QuestionStress q) {
        if (pendingDeleteId == null || pendingDeleteId != q.getId()) {
            pendingDeleteId = q.getId();
            showListFeedback("Click Delete again to confirm removal of Question #" + q.getQuestionNumber() + ".", false);
            return;
        }
        try {
            service.delete(q.getId());
            pendingDeleteId = null;
            showListFeedback("Question deleted successfully!", true);
            handleBackToList();
        } catch (RuntimeException e) {
            showListFeedback(e.getMessage() == null ? "Failed to delete question." : e.getMessage(), false);
        }
        showNotification("Question supprimée avec succès.", "success");
        handleBackToList();
    }

    @FXML
    private void handleExportPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save PDF");
        chooser.setInitialFileName("stress_questions.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = chooser.showSaveDialog(listSection.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            generatePdf(file, service.findAll());
            showListFeedback("PDF exported: " + file.getAbsolutePath(), true);
        } catch (Exception ex) {
            showListFeedback("Export failed: " + ex.getMessage(), false);
        }
    }

    private void generatePdf(File file, List<QuestionStress> questions) throws Exception {
        Document document = new Document(new PdfDocument(new PdfWriter(file.getAbsolutePath())));
        DeviceRgb indigo = new DeviceRgb(99, 102, 241);

        document.add(new Paragraph("Stress Assessment Questions")
                .setFontColor(indigo).setFontSize(20).setBold().setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("Generated on " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")))
                .setFontColor(ColorConstants.GRAY).setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

        Table table = new Table(UnitValue.createPercentArray(new float[]{10, 55, 15, 20}));
        table.setWidth(UnitValue.createPercentValue(100));
        for (String h : new String[]{"#", "Question", "Status", "Created"}) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setBold().setFontColor(ColorConstants.WHITE).setFontSize(10))
                    .setBackgroundColor(indigo).setPadding(8));
        }

        DeviceRgb lightGray = new DeviceRgb(249, 250, 251);
        DeviceRgb green = new DeviceRgb(34, 197, 94);
        DeviceRgb gray = new DeviceRgb(107, 114, 128);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d, yyyy");
        int rowIndex = 0;

        for (QuestionStress q : questions) {
            Color rowBg = (rowIndex % 2 == 0) ? ColorConstants.WHITE : lightGray;
            String created = q.getCreatedAt() != null ? q.getCreatedAt().format(fmt) : "-";

            table.addCell(styledCell(String.valueOf(q.getQuestionNumber()), rowBg, ColorConstants.BLACK, 10));
            table.addCell(styledCell(q.getQuestionText() == null ? "" : q.getQuestionText(), rowBg, ColorConstants.BLACK, 10));
            table.addCell(new Cell()
                    .add(new Paragraph(q.isActive() ? "Active" : "Inactive")
                            .setFontColor(q.isActive() ? green : gray).setFontSize(10).setBold())
                    .setBackgroundColor(rowBg).setPadding(6));
            table.addCell(styledCell(created, rowBg, ColorConstants.BLACK, 10));
            rowIndex++;
        }

        document.add(table);
        document.add(new Paragraph("\nRLife Admin - Wellbeing Module\nTotal Questions: " + questions.size())
                .setFontColor(ColorConstants.GRAY).setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(30));
        document.close();
    }

    private Cell styledCell(String text, Color bg, Color fg, float size) {
        return new Cell()
                .add(new Paragraph(text).setFontColor(fg).setFontSize(size))
                .setBackgroundColor(bg).setPadding(6);
    }

    private void showNotification(String message, String type) {
        boolean success = "success".equalsIgnoreCase(type);
        showListFeedback(message, success);
        if (notificationLabel != null) {
            notificationLabel.setText(message);
            notificationLabel.getStyleClass().removeAll("auth-error", "auth-success");
            notificationLabel.getStyleClass().add(success ? "auth-success" : "auth-error");
            notificationLabel.setVisible(true);
            notificationLabel.setManaged(true);
        }
    }

    private void clearForm() {
        questionTextArea.clear();
        markFieldValid(questionTextArea);
        activeCheckBox.setSelected(false);
    }

    private void showFormError(String msg) {
        formErrorLabel.setText(msg);
        formErrorLabel.setVisible(true);
        formErrorLabel.setManaged(true);
    }

    private void hideFormError() {
        formErrorLabel.setVisible(false);
        formErrorLabel.setManaged(false);
    }

    private void markFieldError(TextArea field) {
        if (!field.getStyleClass().contains("admin-field-error")) {
            field.getStyleClass().add("admin-field-error");
        }
    }

    private void showListFeedback(String message, boolean success) {
        listFeedbackLabel.setText(message);
        listFeedbackLabel.getStyleClass().removeAll("auth-error", "auth-success");
        listFeedbackLabel.getStyleClass().add(success ? "auth-success" : "auth-error");
        listFeedbackLabel.setVisible(true);
        listFeedbackLabel.setManaged(true);
    }

    private void hideListFeedback() {
        listFeedbackLabel.setVisible(false);
        listFeedbackLabel.setManaged(false);
    }

    private void enforceTextLimit(TextArea area, int maxLength) {
        area.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= maxLength ? change : null));
    }

    private void markFieldInvalid(TextArea area) {
        area.setStyle(INVALID_FIELD_STYLE);
    }

    private void markFieldValid(TextArea area) {
        area.setStyle("");
    }

}
