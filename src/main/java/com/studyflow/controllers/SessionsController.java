package com.studyflow.controllers;

import com.studyflow.models.Seance;
import com.studyflow.models.TypeSeance;
import com.studyflow.models.User;
import com.studyflow.services.ServiceSeance;
import com.studyflow.services.ServiceTypeSeance;
import com.studyflow.utils.UserSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SessionsController implements Initializable {

    private static final String[] LOGO_RESOURCES = {
            "/com/studyflow/assets/logo.png",
            "/com/studyflow/assets/logo.jpg",
            "/com/studyflow/assets/logo.jpeg"
    };
    private static final Pattern TITLE_PATTERN = Pattern.compile("^(?=.{3,80}$)(?=.*\\p{L})[\\p{L} _-]+$");

    @FXML private Label sessionsCountLabel;
    @FXML private Label sessionsTotalValueLabel;
    @FXML private Label sessionsTotalTrendLabel;
    @FXML private Label sessionsQualityValueLabel;
    @FXML private Label sessionsQualityTrendLabel;
    @FXML private Label sessionsTypesValueLabel;
    @FXML private Label sessionsTypesTrendLabel;
    @FXML private Label sessionsRecentValueLabel;
    @FXML private Label sessionsRecentTrendLabel;
    @FXML private Label pageMessageLabel;
    @FXML private Label formTitleLabel;
    @FXML private Label formSubtitleLabel;
    @FXML private Label formMessageLabel;
    @FXML private VBox listCard;
    @FXML private VBox formCard;
    @FXML private Button addSessionButton;
    @FXML private TextField titleField;
    @FXML private Label titleErrorLabel;
    @FXML private ComboBox<TypeSeance> typeComboBox;
    @FXML private Label typeErrorLabel;
    @FXML private TextArea descriptionArea;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private ListView<Seance> sessionsListView;

    private final ServiceSeance serviceSeance = new ServiceSeance();
    private final ServiceTypeSeance serviceTypeSeance = new ServiceTypeSeance();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private final ObservableList<Seance> masterSessions = FXCollections.observableArrayList();

    private Seance editingSeance;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureListView();
        configureFilters();
        configureFormValidation();
        loadTypes();
        resetForm();
        refreshList();
        showListMode();
    }

    @FXML
    private void handleAddSession() {
        resetForm();
        showFormMode();
        showInfo("Fill in the required fields marked with *.");
    }

    @FXML
    private void handleSaveSession() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            showError("No logged-in user found. Please sign in before managing sessions.");
            return;
        }

        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        TypeSeance selectedType = typeComboBox.getValue();

        clearValidationErrors();
        boolean hasValidationErrors = !validateTitle(title, true) | !validateType(selectedType, true);
        if (hasValidationErrors) {
            showError("Please fix the highlighted fields.");
            return;
        }

        Seance seance = editingSeance == null ? new Seance() : editingSeance;
        seance.setUserId(currentUser.getId());
        seance.setTitre(title);
        seance.setTypeSeanceId(selectedType.getId());
        seance.setTypeSeance(selectedType.getName());
        seance.setTypeSeanceName(selectedType.getName());
        seance.setDescription(blankToNull(descriptionArea.getText()));
        seance.setStatut(null);
        seance.setPartageAvec(null);
        seance.setMatiereId(null);

        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (editingSeance == null) {
            seance.setCreatedAt(now);
            serviceSeance.add(seance);
        } else {
            seance.setUpdatedAt(now);
            serviceSeance.update(seance);
        }

        boolean isCreation = editingSeance == null;
        refreshList();
        resetForm();
        showListMode();
        if (isCreation) {
            showInfo("Session added successfully.");
        } else {
            showInfo("Session updated successfully.");
        }
    }

    @FXML
    private void handleCancelForm() {
        resetForm();
        showListMode();
    }

    @FXML
    private void handleBackToPlanning() {
        MainController mainController = MainController.getInstance();
        if (mainController != null) {
            mainController.showPlanning();
        }
    }

    @FXML
    private void handleExportExcel() {
        List<Seance> sessionsToExport = new ArrayList<>(sessionsListView.getItems());
        if (sessionsToExport.isEmpty()) {
            showInfo("No sessions available to export.");
            return;
        }

        FileChooser fileChooser = createFileChooser("Export Sessions to Excel", "sessions-export.xlsx",
                new FileChooser.ExtensionFilter("Excel Workbook", "*.xlsx"));
        Path selectedPath = toPath(fileChooser.showSaveDialog(getCurrentWindow()));
        if (selectedPath == null) {
            showInfo("Excel export cancelled.");
            return;
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             OutputStream outputStream = Files.newOutputStream(selectedPath)) {
            Sheet sheet = workbook.createSheet("Sessions");
            configureExcelSheet(sheet);

            int rowIndex = writeExcelBrandHeader(workbook, sheet);
            rowIndex = writeExcelTableHeader(workbook, sheet, rowIndex);
            for (Seance seance : sessionsToExport) {
                Row row = sheet.createRow(rowIndex++);
                writeExcelDataRow(workbook, row, seance, rowIndex % 2 == 0);
            }

            insertExcelLogo(workbook, sheet);

            workbook.write(outputStream);
            showSuccess("Excel file exported successfully.");
        } catch (IOException exception) {
            showErrorOn(pageMessageLabel, "Excel export failed: " + exception.getMessage());
        }
    }

    @FXML
    private void handleExportPdf() {
        List<Seance> sessionsToExport = new ArrayList<>(sessionsListView.getItems());
        if (sessionsToExport.isEmpty()) {
            showInfo("No sessions available to export.");
            return;
        }

        FileChooser fileChooser = createFileChooser("Export Sessions to PDF", "sessions-export.pdf",
                new FileChooser.ExtensionFilter("PDF Document", "*.pdf"));
        Path selectedPath = toPath(fileChooser.showSaveDialog(getCurrentWindow()));
        if (selectedPath == null) {
            showInfo("PDF export cancelled.");
            return;
        }

        try (PDDocument document = new PDDocument()) {
            float margin = 50f;
            PDImageXObject logo = loadPdfLogo(document);
            PDPage page = null;
            PDPageContentStream contentStream = null;
            float y = 0f;

            try {
                for (Seance seance : sessionsToExport) {
                    float estimatedHeight = estimatePdfHeight(seance);
                    if (contentStream == null || y - estimatedHeight < 70) {
                        if (contentStream != null) {
                            contentStream.close();
                        }
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        y = writePdfHeader(contentStream, margin, page.getMediaBox().getHeight() - margin, logo);
                    }
                    y = writePdfSession(contentStream, margin, y, seance);
                }
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }

            document.save(selectedPath.toFile());
            showSuccess("PDF file exported successfully.");
        } catch (IOException exception) {
            showErrorOn(pageMessageLabel, "PDF export failed: " + exception.getMessage());
        }
    }

    @FXML
    private void handlePrintSessions() {
        List<Seance> sessionsToPrint = new ArrayList<>(sessionsListView.getItems());
        if (sessionsToPrint.isEmpty()) {
            showInfo("No sessions available to print.");
            return;
        }

        PrinterJob printerJob = PrinterJob.createPrinterJob();
        if (printerJob == null) {
            showErrorOn(pageMessageLabel, "No printer is available.");
            return;
        }

        VBox printRoot = buildPrintableContent(sessionsToPrint);
        if (!printerJob.showPrintDialog(getCurrentWindow())) {
            showInfo("Printing cancelled.");
            return;
        }

        boolean printed = printerJob.printPage(printRoot);
        if (printed) {
            printerJob.endJob();
            showSuccess("Print job sent successfully.");
        } else {
            showErrorOn(pageMessageLabel, "Printing failed.");
        }
    }

    private void configureListView() {
        sessionsListView.setCellFactory(listView -> new SessionListCell());
    }

    private void configureFilters() {
        sortComboBox.setItems(FXCollections.observableArrayList(
                "Newest First",
                "Oldest First",
                "Title A-Z",
                "Title Z-A",
                "Type A-Z"
        ));
        sortComboBox.setValue("Newest First");

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applySearchAndSort());
        sortComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applySearchAndSort());
    }

    private void loadTypes() {
        List<TypeSeance> availableTypes = serviceTypeSeance.getAvailableTypes();
        typeComboBox.setItems(FXCollections.observableArrayList(availableTypes));
    }

    private void configureFormValidation() {
        titleField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (titleErrorLabel != null && titleErrorLabel.isVisible()) {
                validateTitle(newValue == null ? "" : newValue.trim(), true);
            } else {
                validateTitle(newValue == null ? "" : newValue.trim(), false);
            }
        });

        typeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (typeErrorLabel != null && typeErrorLabel.isVisible()) {
                validateType(newValue, true);
            } else {
                validateType(newValue, false);
            }
        });
    }

    private boolean validateTitle(String title, boolean showMessage) {
        if (title == null || title.isBlank()) {
            if (showMessage) {
                markFieldInvalid(titleField, titleErrorLabel, "Title is required.");
            }
            return false;
        }

        if (!TITLE_PATTERN.matcher(title).matches()) {
            if (showMessage) {
                markFieldInvalid(titleField, titleErrorLabel,
                        "Title must use letters only (spaces, '_' and '-' are allowed). No numbers.");
            }
            return false;
        }

        clearFieldValidation(titleField, titleErrorLabel);
        return true;
    }

    private boolean validateType(TypeSeance selectedType, boolean showMessage) {
        if (selectedType == null) {
            if (showMessage) {
                markFieldInvalid(typeComboBox, typeErrorLabel, "Type is required.");
            }
            return false;
        }
        clearFieldValidation(typeComboBox, typeErrorLabel);
        return true;
    }

    private void clearValidationErrors() {
        clearFieldValidation(titleField, titleErrorLabel);
        clearFieldValidation(typeComboBox, typeErrorLabel);
    }

    private void markFieldInvalid(Control field, Label errorLabel, String message) {
        if (field != null && !field.getStyleClass().contains("field-invalid")) {
            field.getStyleClass().add("field-invalid");
        }
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void clearFieldValidation(Control field, Label errorLabel) {
        if (field != null) {
            field.getStyleClass().remove("field-invalid");
        }
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    private void refreshList() {
        List<Seance> seances = serviceSeance.getAll();
        masterSessions.setAll(seances);
        updateSessionsStatistics();
        applySearchAndSort();
    }

    private void updateSessionsStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime current30Start = now.minusDays(30);
        LocalDateTime previous30Start = now.minusDays(60);
        LocalDateTime previous30End = now.minusDays(30);

        int total = masterSessions.size();
        long totalPrevious = masterSessions.stream()
                .filter(seance -> seance.getCreatedAt() != null)
                .map(Seance::getCreatedAt)
                .map(Timestamp::toLocalDateTime)
                .filter(createdAt -> !createdAt.isBefore(previous30Start) && createdAt.isBefore(previous30End))
                .count();

        double qualityRate = computeDescriptionRate(masterSessions);
        double previousQualityRate = computeDescriptionRate(
                masterSessions.stream()
                        .filter(seance -> seance.getCreatedAt() != null)
                        .filter(seance -> {
                            LocalDateTime created = seance.getCreatedAt().toLocalDateTime();
                            return !created.isBefore(previous30Start) && created.isBefore(previous30End);
                        })
                        .toList()
        );

        long distinctTypes = masterSessions.stream()
                .map(this::resolveTypeLabel)
                .filter(type -> type != null && !type.isBlank() && !"-".equals(type))
                .map(String::toLowerCase)
                .distinct()
                .count();
        long previousDistinctTypes = masterSessions.stream()
                .filter(seance -> seance.getCreatedAt() != null)
                .filter(seance -> {
                    LocalDateTime created = seance.getCreatedAt().toLocalDateTime();
                    return !created.isBefore(previous30Start) && created.isBefore(previous30End);
                })
                .map(this::resolveTypeLabel)
                .filter(type -> type != null && !type.isBlank() && !"-".equals(type))
                .map(String::toLowerCase)
                .distinct()
                .count();

        long recentCount = masterSessions.stream()
                .filter(seance -> seance.getCreatedAt() != null)
                .map(Seance::getCreatedAt)
                .map(Timestamp::toLocalDateTime)
                .filter(createdAt -> !createdAt.isBefore(current30Start))
                .count();
        long previousRecentCount = totalPrevious;

        setKpiValue(sessionsTotalValueLabel, String.valueOf(total));
        setTrendChip(sessionsTotalTrendLabel, total - totalPrevious, "vs prev 30d", false);

        setKpiValue(sessionsQualityValueLabel, Math.round(qualityRate) + "%");
        setTrendChip(sessionsQualityTrendLabel, qualityRate - previousQualityRate, "pts", false);

        setKpiValue(sessionsTypesValueLabel, String.valueOf(distinctTypes));
        setTrendChip(sessionsTypesTrendLabel, distinctTypes - previousDistinctTypes, "types", false);

        setKpiValue(sessionsRecentValueLabel, String.valueOf(recentCount));
        setTrendChip(sessionsRecentTrendLabel, recentCount - previousRecentCount, "vs prev 30d", false);
    }

    private double computeDescriptionRate(List<Seance> seances) {
        if (seances == null || seances.isEmpty()) {
            return 0;
        }
        long withDescription = seances.stream()
                .filter(seance -> seance.getDescription() != null && !seance.getDescription().isBlank())
                .count();
        return (withDescription * 100.0) / seances.size();
    }

    private void setKpiValue(Label label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }

    private void setTrendChip(Label label, double delta, String suffix, boolean invertGoodSignal) {
        if (label == null) {
            return;
        }
        String sign = delta > 0.01 ? "+" : (delta < -0.01 ? "-" : "");
        String number = Math.abs(delta) >= 10
                ? String.valueOf(Math.round(Math.abs(delta)))
                : String.format(java.util.Locale.US, "%.1f", Math.abs(delta));
        label.setText(sign + number + " " + suffix);

        String trendClass = "neutral";
        if (delta > 0.01) {
            trendClass = invertGoodSignal ? "down" : "up";
        } else if (delta < -0.01) {
            trendClass = invertGoodSignal ? "up" : "down";
        }
        label.getStyleClass().setAll("kpi-trend-chip", trendClass);
    }

    private void applySearchAndSort() {
        String query = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase();

        List<Seance> filteredSessions = masterSessions.stream()
                .filter((Seance seance) -> matchesSearch(seance, query))
                .sorted(resolveComparator())
                .collect(Collectors.toList());

        sessionsListView.setItems(FXCollections.observableArrayList(filteredSessions));
        sessionsCountLabel.setText(filteredSessions.size() + " sessions");
    }

    private boolean matchesSearch(Seance seance, String query) {
        if (query.isEmpty()) {
            return true;
        }

        String title = safeLower(seance.getTitre());
        String type = safeLower(resolveTypeLabel(seance));
        String description = safeLower(seance.getDescription());
        return title.contains(query) || type.contains(query) || description.contains(query);
    }

    private Comparator<Seance> resolveComparator() {
        String selectedSort = sortComboBox == null ? "Newest First" : sortComboBox.getValue();

        if ("Oldest First".equals(selectedSort)) {
            return (first, second) -> compareTimestamps(first.getCreatedAt(), second.getCreatedAt());
        }
        if ("Title A-Z".equals(selectedSort)) {
            return Comparator.comparing((Seance seance) -> safeLower(seance.getTitre()));
        }
        if ("Title Z-A".equals(selectedSort)) {
            return (first, second) -> safeLower(second.getTitre()).compareTo(safeLower(first.getTitre()));
        }
        if ("Type A-Z".equals(selectedSort)) {
            return Comparator.comparing((Seance seance) -> safeLower(resolveTypeLabel(seance)), Comparator.naturalOrder())
                    .thenComparing((Seance seance) -> safeLower(seance.getTitre()), Comparator.naturalOrder());
        }

        return (first, second) -> compareTimestamps(second.getCreatedAt(), first.getCreatedAt());
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String defaultValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private int compareTimestamps(Timestamp first, Timestamp second) {
        if (first == null && second == null) {
            return 0;
        }
        if (first == null) {
            return 1;
        }
        if (second == null) {
            return -1;
        }
        return first.compareTo(second);
    }

    private void startEdit(Seance seance) {
        editingSeance = seance;
        showFormMode();
        formTitleLabel.setText("Edit Session");
        formSubtitleLabel.setText("Update the selected session in full page mode");
        titleField.setText(seance.getTitre());
        descriptionArea.setText(seance.getDescription() == null ? "" : seance.getDescription());

        if (seance.getTypeSeanceId() != null) {
            for (TypeSeance typeSeance : typeComboBox.getItems()) {
                if (typeSeance.getId() == seance.getTypeSeanceId()) {
                    typeComboBox.setValue(typeSeance);
                    break;
                }
            }
        } else {
            typeComboBox.setValue(null);
        }

        clearValidationErrors();
        hideMessage();
    }

    private void deleteSession(Seance seance) {
        String sessionLabel = seance.getTitre() == null || seance.getTitre().isBlank()
                ? "this session"
                : "\"" + seance.getTitre() + "\"";

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Delete Session");
        confirmationAlert.setHeaderText("Delete selected session?");
        confirmationAlert.setContentText("You are about to delete " + sessionLabel + ". This action cannot be undone.");

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            showInfo("Session deletion cancelled.");
            return;
        }

        serviceSeance.delete(seance);
        refreshList();
        if (editingSeance != null && editingSeance.getId() == seance.getId()) {
            resetForm();
        }
        showSuccess("Session deleted successfully.");
    }

    private void resetForm() {
        editingSeance = null;
        formTitleLabel.setText("Add Session");
        formSubtitleLabel.setText("Create a new session in full page mode");
        titleField.clear();
        typeComboBox.setValue(null);
        descriptionArea.clear();
        clearValidationErrors();
        hideMessage();
    }

    private void showFormMode() {
        if (listCard != null) {
            listCard.setVisible(false);
            listCard.setManaged(false);
        }
        formCard.setVisible(true);
        formCard.setManaged(true);
        if (addSessionButton != null) {
            addSessionButton.setVisible(false);
            addSessionButton.setManaged(false);
        }
    }

    private void showListMode() {
        if (listCard != null) {
            listCard.setVisible(true);
            listCard.setManaged(true);
        }
        formCard.setVisible(false);
        formCard.setManaged(false);
        if (addSessionButton != null) {
            addSessionButton.setVisible(true);
            addSessionButton.setManaged(true);
        }
    }

    private String resolveTypeLabel(Seance seance) {
        if (seance.getTypeSeanceName() != null && !seance.getTypeSeanceName().isBlank()) {
            return seance.getTypeSeanceName();
        }
        return seance.getTypeSeance() == null || seance.getTypeSeance().isBlank() ? "-" : seance.getTypeSeance();
    }

    private String formatTimestamp(Timestamp timestamp) {
        return timestamp == null ? "-" : timestamp.toLocalDateTime().format(dateFormatter);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void showError(String message) {
        showErrorOn(formMessageLabel, message);
    }

    private void showSuccess(String message) {
        Label target = formCard.isVisible() ? formMessageLabel : pageMessageLabel;
        showStyledMessage(target, message,
                "-fx-background-color: rgba(16,185,129,0.12); -fx-border-color: rgba(16,185,129,0.35); -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #34D399; -fx-padding: 12 16; -fx-font-size: 13px;");
    }

    private void showInfo(String message) {
        Label target = formCard.isVisible() ? formMessageLabel : pageMessageLabel;
        showStyledMessage(target, message,
                "-fx-background-color: rgba(139,92,246,0.12); -fx-border-color: rgba(139,92,246,0.35); -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #C4B5FD; -fx-padding: 12 16; -fx-font-size: 13px;");
    }

    private void hideMessage() {
        hideLabelMessage(pageMessageLabel);
        hideLabelMessage(formMessageLabel);
    }

    private void showErrorOn(Label label, String message) {
        label.setText(message);
        label.getStyleClass().setAll("auth-error");
        label.setStyle("");
        label.setVisible(true);
        label.setManaged(true);
    }

    private void showStyledMessage(Label label, String message, String style) {
        label.setText(message);
        label.getStyleClass().clear();
        label.setStyle(style);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideLabelMessage(Label label) {
        if (label == null) {
            return;
        }
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
        label.setStyle("");
    }

    private FileChooser createFileChooser(String title, String fileName, FileChooser.ExtensionFilter extensionFilter) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialFileName(fileName);
        fileChooser.getExtensionFilters().add(extensionFilter);
        return fileChooser;
    }

    private Path toPath(java.io.File file) {
        return file == null ? null : file.toPath();
    }

    private Window getCurrentWindow() {
        if (sessionsListView != null && sessionsListView.getScene() != null) {
            return sessionsListView.getScene().getWindow();
        }
        if (formCard != null && formCard.getScene() != null) {
            return formCard.getScene().getWindow();
        }
        return null;
    }

    private PDImageXObject loadPdfLogo(PDDocument document) throws IOException {
        for (String resource : LOGO_RESOURCES) {
            try (InputStream inputStream = getClass().getResourceAsStream(resource)) {
                if (inputStream == null) {
                    continue;
                }
                return PDImageXObject.createFromByteArray(document, inputStream.readAllBytes(), "studyflow-logo");
            }
        }
        return null;
    }

    private void addPrintableLine(VBox parent, String label, String value, boolean title) {
        Label caption = new Label(label);
        caption.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #64748B;");

        Label content = new Label(value);
        content.setWrapText(true);
        content.setStyle(title
                ? "-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #F8FAFC;"
                : "-fx-font-size: 13px; -fx-text-fill: #CBD5E1;");

        VBox box = new VBox(4, caption, content);
        parent.getChildren().add(box);
    }

    private VBox buildPrintableContent(List<Seance> sessionsToPrint) {
        VBox root = new VBox(18);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #020617;");
        root.setPrefWidth(720);

        root.getChildren().add(buildPrintableHeader(sessionsToPrint.size()));

        for (Seance seance : sessionsToPrint) {
            VBox sessionBox = new VBox(10);
            sessionBox.setPadding(new Insets(18));
            sessionBox.setStyle("-fx-background-color: #0F172A; -fx-border-color: #1E293B; -fx-border-width: 1; -fx-border-radius: 16; -fx-background-radius: 16;");

            addPrintableLine(sessionBox, "Title", defaultValue(seance.getTitre()), true);
            addPrintableLine(sessionBox, "Type", resolveTypeLabel(seance), false);
            addPrintableLine(sessionBox, "Created At", formatTimestamp(seance.getCreatedAt()), false);
            addPrintableLine(sessionBox, "Description", defaultValue(seance.getDescription()), false);

            root.getChildren().add(sessionBox);
        }

        Platform.runLater(root::applyCss);
        return root;
    }

    private Image loadFxLogo() {
        for (String resource : LOGO_RESOURCES) {
            try (InputStream inputStream = getClass().getResourceAsStream(resource)) {
                if (inputStream != null) {
                    return new Image(inputStream);
                }
            } catch (IOException exception) {
                return null;
            }
        }
        return null;
    }

    private float writePdfLine(PDPageContentStream contentStream, float x, float y, String text, boolean title) throws IOException {
        contentStream.beginText();
        contentStream.setNonStrokingColor(title ? new Color(248, 250, 252) : new Color(203, 213, 225));
        contentStream.setFont(title ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, title ? 13 : 11);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(sanitizePdfText(text));
        contentStream.endText();
        return y - (title ? 18 : 14);
    }

    private float writePdfParagraph(PDPageContentStream contentStream, float x, float y, String text) throws IOException {
        for (String line : wrapPdfText(text, 90)) {
            y = writePdfLine(contentStream, x, y, line, false);
        }
        return y;
    }

    private float writePdfHeader(PDPageContentStream contentStream, float margin, float y, PDImageXObject logo) throws IOException {
        contentStream.setNonStrokingColor(new Color(2, 6, 23));
        contentStream.addRect(0, y - 70, PDRectangle.A4.getWidth(), 90);
        contentStream.fill();

        if (logo != null) {
            contentStream.drawImage(logo, margin, y - 40, 40, 40);
        }

        contentStream.beginText();
        contentStream.setNonStrokingColor(new Color(248, 250, 252));
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
        contentStream.newLineAtOffset(margin + (logo == null ? 0 : 52), y - 18);
        contentStream.showText("StudyFlow Sessions Export");
        contentStream.endText();

        contentStream.beginText();
        contentStream.setNonStrokingColor(new Color(148, 163, 184));
        contentStream.setFont(PDType1Font.HELVETICA, 10);
        contentStream.newLineAtOffset(margin + (logo == null ? 0 : 52), y - 34);
        contentStream.showText("Elegant export generated from the StudyFlow dark workspace");
        contentStream.endText();

        return y - 60;
    }

    private float writePdfSession(PDPageContentStream contentStream, float margin, float y, Seance seance) throws IOException {
        float cardHeight = estimatePdfHeight(seance) - 6;
        contentStream.setNonStrokingColor(new Color(15, 23, 42));
        contentStream.addRect(margin - 10, y - cardHeight + 8, PDRectangle.A4.getWidth() - (margin * 2) + 20, cardHeight);
        contentStream.fill();

        contentStream.setStrokingColor(new Color(30, 41, 59));
        contentStream.addRect(margin - 10, y - cardHeight + 8, PDRectangle.A4.getWidth() - (margin * 2) + 20, cardHeight);
        contentStream.stroke();

        y = writePdfLine(contentStream, margin, y, "Title: " + defaultValue(seance.getTitre()), true);
        y = writePdfLine(contentStream, margin, y, "Type: " + resolveTypeLabel(seance), false);
        y = writePdfLine(contentStream, margin, y, "Created At: " + formatTimestamp(seance.getCreatedAt()), false);
        y = writePdfParagraph(contentStream, margin, y, "Description: " + defaultValue(seance.getDescription()));
        return y - 10;
    }

    private float estimatePdfHeight(Seance seance) {
        int descriptionLength = defaultValue(seance.getDescription()).length();
        int lines = Math.max(1, (descriptionLength / 90) + 1);
        return 60f + (lines * 14f);
    }

    private List<String> wrapPdfText(String text, int maxLength) {
        List<String> lines = new ArrayList<>();
        String normalized = sanitizePdfText(text);
        while (normalized.length() > maxLength) {
            int breakIndex = normalized.lastIndexOf(' ', maxLength);
            if (breakIndex <= 0) {
                breakIndex = maxLength;
            }
            lines.add(normalized.substring(0, breakIndex));
            normalized = normalized.substring(breakIndex).trim();
        }
        lines.add(normalized);
        return lines;
    }

    private String sanitizePdfText(String value) {
        return value.replace("\r", " ").replace("\n", " ");
    }

    private void configureExcelSheet(Sheet sheet) {
        sheet.setColumnWidth(0, 900);
        sheet.setColumnWidth(1, 9000);
        sheet.setColumnWidth(2, 5500);
        sheet.setColumnWidth(3, 6500);
        sheet.setColumnWidth(4, 16000);
        sheet.createFreezePane(0, 4);
    }

    private int writeExcelBrandHeader(XSSFWorkbook workbook, Sheet sheet) {
        sheet.addMergedRegion(new CellRangeAddress(0, 1, 1, 4));
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28);
        Row subtitleRow = sheet.createRow(1);
        subtitleRow.setHeightInPoints(22);
        sheet.createRow(2).setHeightInPoints(12);

        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue("StudyFlow Sessions Export");
        titleCell.setCellStyle(createExcelTitleStyle(workbook));

        Cell subtitleCell = subtitleRow.createCell(1);
        subtitleCell.setCellValue("Dark themed export with title, type, created date and description");
        subtitleCell.setCellStyle(createExcelSubtitleStyle(workbook));

        return 3;
    }

    private int writeExcelTableHeader(XSSFWorkbook workbook, Sheet sheet, int rowIndex) {
        Row headerRow = sheet.createRow(rowIndex);
        headerRow.setHeightInPoints(22);
        String[] headers = {"#", "Title", "Type", "Created At", "Description"};
        CellStyle headerStyle = createExcelHeaderStyle(workbook);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        return rowIndex + 1;
    }

    private void writeExcelDataRow(XSSFWorkbook workbook, Row row, Seance seance, boolean alternate) {
        CellStyle bodyStyle = createExcelBodyStyle(workbook, alternate);
        CellStyle wrapStyle = createExcelWrapStyle(workbook, alternate);

        row.setHeightInPoints(48);

        Cell indexCell = row.createCell(0);
        indexCell.setCellValue(row.getRowNum() - 3);
        indexCell.setCellStyle(bodyStyle);

        Cell titleCell = row.createCell(1);
        titleCell.setCellValue(defaultValue(seance.getTitre()));
        titleCell.setCellStyle(bodyStyle);

        Cell typeCell = row.createCell(2);
        typeCell.setCellValue(resolveTypeLabel(seance));
        typeCell.setCellStyle(bodyStyle);

        Cell createdCell = row.createCell(3);
        createdCell.setCellValue(formatTimestamp(seance.getCreatedAt()));
        createdCell.setCellStyle(bodyStyle);

        Cell descriptionCell = row.createCell(4);
        descriptionCell.setCellValue(defaultValue(seance.getDescription()));
        descriptionCell.setCellStyle(wrapStyle);
    }

    private void insertExcelLogo(XSSFWorkbook workbook, Sheet sheet) throws IOException {
        byte[] logoBytes = loadLogoBytes();
        if (logoBytes == null) {
            return;
        }

        int pictureIndex = workbook.addPicture(logoBytes, resolvePoiPictureType());
        CreationHelper helper = workbook.getCreationHelper();
        ClientAnchor anchor = helper.createClientAnchor();
        anchor.setCol1(0);
        anchor.setRow1(0);
        anchor.setCol2(1);
        anchor.setRow2(2);
        sheet.createDrawingPatriarch().createPicture(anchor, pictureIndex);
    }

    private byte[] loadLogoBytes() throws IOException {
        for (String resource : LOGO_RESOURCES) {
            try (InputStream inputStream = getClass().getResourceAsStream(resource)) {
                if (inputStream != null) {
                    return inputStream.readAllBytes();
                }
            }
        }
        return null;
    }

    private int resolvePoiPictureType() {
        for (String resource : LOGO_RESOURCES) {
            if (getClass().getResource(resource) == null) {
                continue;
            }
            if (resource.endsWith(".png")) {
                return XSSFWorkbook.PICTURE_TYPE_PNG;
            }
            return XSSFWorkbook.PICTURE_TYPE_JPEG;
        }
        return XSSFWorkbook.PICTURE_TYPE_PNG;
    }

    private CellStyle createExcelTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.BLACK.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createExcelSubtitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.BLACK.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createExcelHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createExcelBodyStyle(XSSFWorkbook workbook, boolean alternate) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(alternate ? IndexedColors.GREY_80_PERCENT.getIndex() : IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        return style;
    }

    private CellStyle createExcelWrapStyle(XSSFWorkbook workbook, boolean alternate) {
        CellStyle style = createExcelBodyStyle(workbook, alternate);
        style.setWrapText(true);
        return style;
    }

    private VBox buildPrintableHeader(int count) {
        VBox headerBox = new VBox(10);
        headerBox.setPadding(new Insets(20));
        headerBox.setStyle("-fx-background-color: #0F172A; -fx-border-color: #1E293B; -fx-border-width: 1; -fx-border-radius: 18; -fx-background-radius: 18;");

        HBox topRow = new HBox(14);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Image logoImage = loadFxLogo();
        if (logoImage != null) {
            ImageView logoView = new ImageView(logoImage);
            logoView.setFitWidth(54);
            logoView.setFitHeight(54);
            logoView.setPreserveRatio(true);
            topRow.getChildren().add(logoView);
        }

        VBox textBox = new VBox(4);
        Label heading = new Label("StudyFlow Sessions");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: #F8FAFC;");
        Label subtitle = new Label(count + " session(s) prepared for print");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8;");
        textBox.getChildren().addAll(heading, subtitle);
        topRow.getChildren().add(textBox);

        headerBox.getChildren().add(topRow);
        return headerBox;
    }

    private final class SessionListCell extends ListCell<Seance> {
        private final VBox rowContainer = new VBox(14);
        private final HBox headerRow = new HBox(14);
        private final StackPane iconBox = new StackPane();
        private final Label iconLabel = new Label("S");
        private final VBox fieldsBox = new VBox(12);
        private final VBox titleBox = new VBox(4);
        private final Label titleCaptionLabel = new Label("Title");
        private final Label titleLabel = new Label();
        private final VBox typeBox = new VBox(4);
        private final Label typeCaptionLabel = new Label("Type");
        private final Label typeLabel = new Label();
        private final VBox dateBox = new VBox(4);
        private final Label dateCaptionLabel = new Label("Created At");
        private final Label dateLabel = new Label();
        private final VBox descriptionBox = new VBox(4);
        private final Label descriptionCaptionLabel = new Label("Description");
        private final Region spacer = new Region();
        private final HBox actionsBox = new HBox(10);
        private final Button editButton = new Button("Edit");
        private final Button deleteButton = new Button("Delete");
        private final Label descriptionLabel = new Label();
        private final Region divider = new Region();

        private SessionListCell() {
            iconBox.getStyleClass().add("session-item-icon-box");
            iconLabel.getStyleClass().add("session-item-icon-text");
            iconBox.getChildren().add(iconLabel);

            titleCaptionLabel.getStyleClass().add("session-field-label");
            titleLabel.getStyleClass().add("session-item-title");
            typeCaptionLabel.getStyleClass().add("session-field-label");
            typeLabel.getStyleClass().add("session-item-type");
            dateCaptionLabel.getStyleClass().add("session-field-label");
            dateLabel.getStyleClass().add("session-item-date");
            descriptionCaptionLabel.getStyleClass().add("session-field-label");
            descriptionLabel.getStyleClass().add("session-item-description");

            descriptionLabel.setWrapText(true);
            descriptionLabel.setMaxWidth(Double.MAX_VALUE);

            editButton.getStyleClass().add("btn-secondary");
            deleteButton.getStyleClass().add("btn-danger");

            editButton.setOnAction(event -> {
                Seance seance = getItem();
                if (seance != null) {
                    SessionsController.this.startEdit(seance);
                }
            });

            deleteButton.setOnAction(event -> {
                Seance seance = getItem();
                if (seance != null) {
                    SessionsController.this.deleteSession(seance);
                }
            });

            actionsBox.setAlignment(Pos.CENTER_RIGHT);
            actionsBox.getChildren().addAll(editButton, deleteButton);

            titleBox.getChildren().addAll(titleCaptionLabel, titleLabel);
            typeBox.getChildren().addAll(typeCaptionLabel, typeLabel);
            dateBox.getChildren().addAll(dateCaptionLabel, dateLabel);
            descriptionBox.getChildren().addAll(descriptionCaptionLabel, descriptionLabel);

            HBox.setHgrow(spacer, Priority.ALWAYS);
            headerRow.setAlignment(Pos.TOP_LEFT);
            headerRow.getChildren().addAll(iconBox, spacer, actionsBox);

            fieldsBox.getChildren().addAll(titleBox, typeBox, dateBox, descriptionBox);

            divider.getStyleClass().add("session-item-divider");
            divider.setPrefHeight(1);
            divider.setMinHeight(1);
            divider.setMaxHeight(1);

            rowContainer.getStyleClass().add("session-list-row");
            rowContainer.setPadding(new Insets(16, 0, 16, 0));
            rowContainer.getChildren().addAll(headerRow, fieldsBox, divider);

            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(Seance seance, boolean empty) {
            super.updateItem(seance, empty);
            if (empty || seance == null) {
                setGraphic(null);
                return;
            }

            String type = resolveTypeLabel(seance);
            titleLabel.setText(seance.getTitre() == null || seance.getTitre().isBlank() ? "Untitled session" : seance.getTitre());
            typeLabel.setText(type);
            dateLabel.setText("Created at " + formatTimestamp(seance.getCreatedAt()));
            iconLabel.setText(type.isBlank() || "-".equals(type) ? "S" : type.substring(0, 1).toUpperCase());

            String description = seance.getDescription();
            descriptionLabel.setText(description == null || description.isBlank()
                    ? "No description provided for this session."
                    : description);

            setGraphic(rowContainer);
        }
    }
}
