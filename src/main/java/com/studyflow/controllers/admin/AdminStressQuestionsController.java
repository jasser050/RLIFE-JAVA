package com.studyflow.controllers.admin;

import com.studyflow.models.QuestionStress;
import com.studyflow.services.ServiceQuestionStress;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

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

import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminStressQuestionsController implements Initializable {

    @FXML private VBox       listSection;
    @FXML private ScrollPane formSection;
    @FXML private ScrollPane detailSection;

    @FXML private Label totalQuestionsLabel;
    @FXML private Label activeQuestionsLabel;
    @FXML private Label inactiveQuestionsLabel;

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> orderCombo;

    @FXML private VBox questionsListBox;

    @FXML private Label     formTitleLabel;
    @FXML private Label     formErrorLabel;
    @FXML private TextField questionNumberField;
    @FXML private TextArea  questionTextArea;
    @FXML private CheckBox  activeCheckBox;
    @FXML private Button    saveButton;

    @FXML private Label detailHeaderLabel;
    @FXML private Label detailCreatedLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label detailQuestionLabel;
    @FXML private Label detailUpdatedLabel;

    // FIX 1 : import com.studyflow.services.ServiceQuestionStress ajouté
    private final ServiceQuestionStress service = new ServiceQuestionStress();

    private QuestionStress editingQuestion = null;
    private QuestionStress detailQuestion  = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initCombos();
        addSearchListener();
        loadQuestions();
    }

    private void initCombos() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Sort by ID", "Sort by Number", "Sort by Date"));
        sortCombo.getSelectionModel().selectFirst();
        sortCombo.setOnAction(e -> loadQuestions());

        orderCombo.setItems(FXCollections.observableArrayList("Ascending", "Descending"));
        orderCombo.getSelectionModel().selectFirst();
        orderCombo.setOnAction(e -> loadQuestions());
    }

    private void addSearchListener() {
        searchField.textProperty().addListener((obs, o, n) -> loadQuestions());
    }

    private void loadQuestions() {
        List<QuestionStress> all = service.findAll();

        String search = searchField.getText().trim().toLowerCase();
        if (!search.isEmpty()) {
            all = all.stream()
                    .filter(q -> q.getQuestionText().toLowerCase().contains(search))
                    .collect(Collectors.toList());
        }

        String  sortVal = sortCombo.getValue();
        boolean asc     = !"Descending".equals(orderCombo.getValue());

        if ("Sort by Number".equals(sortVal)) {
            all.sort((a, b) -> asc
                    ? Integer.compare(a.getQuestionNumber(), b.getQuestionNumber())
                    : Integer.compare(b.getQuestionNumber(), a.getQuestionNumber()));
        } else if ("Sort by Date".equals(sortVal)) {
            all.sort((a, b) -> {
                if (a.getCreatedAt() == null) return asc ? -1 : 1;
                if (b.getCreatedAt() == null) return asc ? 1 : -1;
                int cmp = a.getCreatedAt().compareTo(b.getCreatedAt());
                return asc ? cmp : -cmp;
            });
        } else {
            all.sort((a, b) -> asc
                    ? Integer.compare(a.getId(), b.getId())
                    : Integer.compare(b.getId(), a.getId()));
        }

        updateStats(all);
        renderTable(all);
    }

    private void updateStats(List<QuestionStress> questions) {
        long active   = questions.stream().filter(QuestionStress::isActive).count();
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
            empty.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 13px;");
            VBox emptyBox = new VBox(empty);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40));
            questionsListBox.getChildren().add(emptyBox);
            return;
        }

        questionsListBox.getChildren().add(buildTableHeader());
        for (QuestionStress q : questions) {
            questionsListBox.getChildren().add(buildTableRow(q));
        }
    }

    private HBox buildTableHeader() {
        HBox header = new HBox();
        header.setStyle("-fx-background-color: #f3f4f6; -fx-padding: 10 16;");
        header.getChildren().addAll(
                headerCell("#",        60),
                headerCell("Question", 400),
                headerCell("Status",   100),
                headerCell("Created",  120),
                headerCell("Actions",  160));
        return header;
    }

    private Label headerCell(String text, double width) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width);
        lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #374151;");
        return lbl;
    }

    private HBox buildTableRow(QuestionStress q) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 16, 8, 16));
        row.setStyle("-fx-border-color: transparent transparent #e5e7eb transparent; -fx-background-color: white;");
        row.setOnMouseEntered(e -> row.setStyle("-fx-border-color: transparent transparent #e5e7eb transparent; -fx-background-color: #f9fafb;"));
        row.setOnMouseExited(e  -> row.setStyle("-fx-border-color: transparent transparent #e5e7eb transparent; -fx-background-color: white;"));

        Label numLabel = new Label(String.valueOf(q.getQuestionNumber()));
        numLabel.setPrefWidth(60);
        numLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        String truncated = q.getQuestionText().length() > 80
                ? q.getQuestionText().substring(0, 80) + "…" : q.getQuestionText();
        Label textLabel = new Label(truncated);
        textLabel.setPrefWidth(400);
        textLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");

        Label statusBadge = buildStatusBadge(q.isActive());
        statusBadge.setPrefWidth(100);

        String dateStr = q.getCreatedAt() != null
                ? q.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy")) : "—";
        Label dateLabel = new Label(dateStr);
        dateLabel.setPrefWidth(120);
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

        HBox actions = buildActionButtons(q);
        actions.setPrefWidth(160);

        row.getChildren().addAll(numLabel, textLabel, statusBadge, dateLabel, actions);
        return row;
    }

    private Label buildStatusBadge(boolean active) {
        Label badge = new Label(active ? "✓ Active" : "✗ Inactive");
        badge.setPadding(new Insets(3, 10, 3, 10));
        badge.setStyle(active
                ? "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;"
                : "-fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;");
        return badge;
    }

    private HBox buildActionButtons(QuestionStress q) {
        Button viewBtn   = new Button("👁");
        Button editBtn   = new Button("✏");
        Button deleteBtn = new Button("🗑");
        styleBtn(viewBtn,   "#e0e7ff", "#3730a3");
        styleBtn(editBtn,   "#dbeafe", "#1e40af");
        styleBtn(deleteBtn, "#fee2e2", "#991b1b");
        viewBtn.setOnAction(e   -> showDetail(q));
        editBtn.setOnAction(e   -> showEditForm(q));
        deleteBtn.setOnAction(e -> handleDelete(q));
        HBox box = new HBox(6, viewBtn, editBtn, deleteBtn);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void styleBtn(Button btn, String bg, String fg) {
        btn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: 6; " +
                        "-fx-font-size: 13px; -fx-cursor: hand; -fx-pref-width: 32; -fx-pref-height: 28;", bg, fg));
    }

    private void showSection(javafx.scene.Node target) {
        listSection.setVisible(false);   listSection.setManaged(false);
        formSection.setVisible(false);   formSection.setManaged(false);
        detailSection.setVisible(false); detailSection.setManaged(false);
        target.setVisible(true);
        target.setManaged(true);
    }

    @FXML private void handleBackToList() {
        editingQuestion = null;
        detailQuestion  = null;
        showSection(listSection);
        loadQuestions();
    }

    @FXML private void handleAddQuestion() {
        editingQuestion = null;
        formTitleLabel.setText("New Question");
        saveButton.setText("Create Question");
        clearForm();
        hideFormError();
        showSection(formSection);
    }

    private void showEditForm(QuestionStress q) {
        editingQuestion = q;
        formTitleLabel.setText("Edit Question #" + q.getQuestionNumber());
        saveButton.setText("Update Question");
        questionNumberField.setText(String.valueOf(q.getQuestionNumber()));
        questionTextArea.setText(q.getQuestionText());
        activeCheckBox.setSelected(q.isActive());
        hideFormError();
        showSection(formSection);
    }

    @FXML private void handleSaveQuestion() {
        hideFormError();
        String numberStr = questionNumberField.getText().trim();
        String text      = questionTextArea.getText().trim();

        if (numberStr.isEmpty() || text.isEmpty()) { showFormError("All fields are required."); return; }

        int number;
        try {
            number = Integer.parseInt(numberStr);
            if (number < 1) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showFormError("Question number must be a positive integer."); return;
        }

        boolean active = activeCheckBox.isSelected();

        if (editingQuestion == null) {
            QuestionStress newQ = new QuestionStress();
            newQ.setQuestionNumber(number);
            newQ.setQuestionText(text);
            newQ.setActive(active);
            newQ.setCreatedAt(LocalDateTime.now());
            service.save(newQ);
            showAlert(Alert.AlertType.INFORMATION, "Question created successfully!");
        } else {
            editingQuestion.setQuestionNumber(number);
            editingQuestion.setQuestionText(text);
            editingQuestion.setActive(active);
            editingQuestion.setUpdatedAt(LocalDateTime.now());
            service.update(editingQuestion);
            showAlert(Alert.AlertType.INFORMATION, "Question updated successfully!");
        }
        handleBackToList();
    }

    private void showDetail(QuestionStress q) {
        detailQuestion = q;
        detailHeaderLabel.setText("Question #" + q.getQuestionNumber());
        detailCreatedLabel.setText(q.getCreatedAt() != null
                ? "Created on " + q.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                : "Created on —");
        detailStatusLabel.setText(q.isActive() ? "Active" : "Inactive");
        detailStatusLabel.setStyle(q.isActive()
                ? "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-background-radius: 12; -fx-padding: 3 12; -fx-font-weight: bold;"
                : "-fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-background-radius: 12; -fx-padding: 3 12; -fx-font-weight: bold;");
        detailQuestionLabel.setText(q.getQuestionText());
        detailUpdatedLabel.setText(q.getUpdatedAt() != null
                ? "Last updated: " + q.getUpdatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm"))
                : "No updates yet.");
        showSection(detailSection);
    }

    @FXML private void handleDetailEdit()   { if (detailQuestion != null) showEditForm(detailQuestion); }
    @FXML private void handleDetailDelete() { if (detailQuestion != null) handleDelete(detailQuestion); }

    private void handleDelete(QuestionStress q) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Question");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("Question #" + q.getQuestionNumber() + " will be permanently deleted.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            service.delete(q.getId());
            showAlert(Alert.AlertType.INFORMATION, "Question deleted successfully!");
            handleBackToList();
        }
    }

    @FXML private void handleExportPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save PDF");
        chooser.setInitialFileName("stress_questions.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = chooser.showSaveDialog(listSection.getScene().getWindow());
        if (file == null) return;
        try {
            generatePdf(file, service.findAll());
            showAlert(Alert.AlertType.INFORMATION, "PDF exported!\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Export failed: " + ex.getMessage());
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
        DeviceRgb green     = new DeviceRgb(34, 197, 94);
        DeviceRgb gray      = new DeviceRgb(107, 114, 128);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d, yyyy");
        int rowIndex = 0;

        for (QuestionStress q : questions) {
            // FIX 2 : Color (type parent) au lieu de DeviceRgb
            // pour accepter ColorConstants.WHITE et DeviceRgb sans conflit de type
            Color rowBg = (rowIndex % 2 == 0) ? ColorConstants.WHITE : lightGray;
            String created = q.getCreatedAt() != null ? q.getCreatedAt().format(fmt) : "—";

            table.addCell(styledCell(String.valueOf(q.getQuestionNumber()), rowBg, ColorConstants.BLACK, 10));
            table.addCell(styledCell(q.getQuestionText(), rowBg, ColorConstants.BLACK, 10));
            table.addCell(new Cell()
                    .add(new Paragraph(q.isActive() ? "✓ Active" : "✗ Inactive")
                            .setFontColor(q.isActive() ? green : gray).setFontSize(10).setBold())
                    .setBackgroundColor(rowBg).setPadding(6));
            table.addCell(styledCell(created, rowBg, ColorConstants.BLACK, 10));
            rowIndex++;
        }

        document.add(table);
        document.add(new Paragraph("\nRLife Admin – Wellbeing Module\nTotal Questions: " + questions.size())
                .setFontColor(ColorConstants.GRAY).setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(30));
        document.close();
    }

    // FIX 2 : paramètres bg et fg en type Color (pas DeviceRgb)
    private Cell styledCell(String text, Color bg, Color fg, float size) {
        return new Cell()
                .add(new Paragraph(text).setFontColor(fg).setFontSize(size))
                .setBackgroundColor(bg).setPadding(6);
    }

    private void clearForm() {
        questionNumberField.clear();
        questionTextArea.clear();
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

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}