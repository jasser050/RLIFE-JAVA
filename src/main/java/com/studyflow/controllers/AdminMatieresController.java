package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.services.MatiereService;
import com.studyflow.models.Matiere;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminMatieresController implements Initializable {

    @FXML
    private Label pageTitle;
    @FXML
    private Label totalLabel;
    @FXML
    private TextField searchField;
    @FXML
    private Button btnHeaderAction;
    @FXML
    private FontIcon btnHeaderIcon;

    @FXML
    private VBox listView;
    @FXML
    private VBox formPanel;

    @FXML
    private Label formTitle;
    @FXML
    private TextField fldNom;
    @FXML
    private TextField fldCode;
    @FXML
    private TextField fldCoeff;
    @FXML
    private TextField fldHeures;
    @FXML
    private ComboBox<String> cmbSection;
    @FXML
    private ComboBox<String> cmbType;
    @FXML
    private TextArea fldDesc;
    @FXML
    private Label formError;
    @FXML
    private Button btnSave;
    @FXML
    private Label descCharCount;

    @FXML
    private Label totalMatieresLabel;
    @FXML
    private Label totalCoursLabel;
    @FXML
    private Label totalTDLabel;
    @FXML
    private Label avgCoeffLabel;

    @FXML
    private FlowPane matieresGrid;

    private final MatiereService service = new MatiereService();
    private ObservableList<Matiere> allItems = FXCollections.observableArrayList();
    private Matiere editTarget = null;

    private static final String[] COLORS = {"primary", "success", "accent", "warning", "danger"};

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configuration des ComboBox
        cmbType.setItems(FXCollections.observableArrayList("Course", "Tutorial", "Lab"));
        cmbType.getSelectionModel().selectFirst();
        setupDescriptionValidation();

        // Empêcher les chiffres dans le nom du subject
        fldNom.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("[a-zA-ZÀ-ÿ\\s-]*")) {
                return change;
            }
            return null;
        }));

        cmbSection.getItems().addAll(
                "Science", "Literature", "Mathematics",
                "Computer Science", "Economics", "Technology");

        // Listeners pour la recherche
        searchField.textProperty().addListener((o, old, val) -> filter(val));

        // Listeners pour la validation en temps réel
        fldNom.textProperty().addListener((obs, o, n) -> validateForm());
        fldCode.textProperty().addListener((obs, o, n) -> validateForm());
        fldCoeff.textProperty().addListener((obs, o, n) -> validateForm());
        fldHeures.textProperty().addListener((obs, o, n) -> validateForm());
        cmbSection.valueProperty().addListener((obs, o, n) -> validateForm());
        cmbType.valueProperty().addListener((obs, o, n) -> validateForm());

        // Validation des champs numériques
        fldCoeff.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                fldCoeff.setText(oldVal);
            }
        });

        fldHeures.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                fldHeures.setText(oldVal);
            }
        });

        fldCode.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("[a-zA-Z0-9]*")) {
                fldCode.setText(oldVal);
            }
        });

        showView("list");
        loadData();
        Platform.runLater(() -> fixTextAreaVisibility());
    }

    private void setupDescriptionValidation() {
        fldDesc.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                int currentLength = newVal.length();

                if (descCharCount != null) {
                    descCharCount.setText(currentLength + " / 500");

                    if (currentLength > 500) {
                        descCharCount.setStyle("-fx-text-fill: #FB7185; -fx-font-size: 11px; -fx-font-weight: bold;");
                    } else if (currentLength > 450) {
                        descCharCount.setStyle("-fx-text-fill: #FBBF24; -fx-font-size: 11px; -fx-font-weight: bold;");
                    } else {
                        descCharCount.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
                    }
                }

                if (currentLength > 500) {
                    String truncated = newVal.substring(0, 500);
                    fldDesc.setText(truncated);
                    showError("Description limited to 500 characters.");
                    Platform.runLater(() -> fldDesc.positionCaret(500));
                }

                if (!newVal.trim().isEmpty()) {
                    fldDesc.setStyle("-fx-border-color: #34D399; -fx-border-width: 2;");
                } else if (!newVal.isEmpty() && newVal.trim().isEmpty()) {
                    fldDesc.setStyle("-fx-border-color: #FB7185; -fx-border-width: 2;");
                }
            }
        });
    }

    private boolean validateDescription(String description) {
        if (description == null || description.isBlank()) {
            fldDesc.setStyle(null);
            return true;
        }

        if (description.trim().isEmpty()) {
            fldDesc.setStyle("-fx-border-color: #FB7185; -fx-border-width: 2;");
            showError("Description cannot contain only spaces.");
            return false;
        }

        if (description.length() > 500) {
            fldDesc.setStyle("-fx-border-color: #FB7185; -fx-border-width: 2;");
            showError("Description cannot exceed 500 characters (current: " + description.length() + ").");
            return false;
        }

        fldDesc.setStyle("-fx-border-color: #34D399; -fx-border-width: 2;");
        return true;
    }

    private void showView(String view) {
        boolean isList = "list".equals(view);

        listView.setVisible(isList);
        listView.setManaged(isList);
        formPanel.setVisible(!isList);
        formPanel.setManaged(!isList);
        searchField.setVisible(isList);
        searchField.setManaged(isList);

        if (isList) {
            btnHeaderAction.setText("Add Subject");
            btnHeaderIcon.setIconLiteral("fth-plus");
            pageTitle.setText("Subject Management");
            totalLabel.setText(allItems.size() + " subjects");
        } else {
            btnHeaderAction.setText("Back");
            btnHeaderIcon.setIconLiteral("fth-arrow-left");
            pageTitle.setText(editTarget == null ? "New Subject" : "Edit Subject");
            totalLabel.setText("Fill in the fields below");
        }
    }

    @FXML
    private void handleHeaderAction() {
        if (formPanel.isVisible()) {
            handleCancel();
        } else {
            handleShowForm();
        }
    }

    private void loadData() {
        try {
            allItems = FXCollections.observableArrayList(service.findAll());
            updateStats(allItems);
            renderCards(allItems);
            totalLabel.setText(allItems.size() + " subjects");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStats(ObservableList<Matiere> list) {
        totalMatieresLabel.setText(String.valueOf(list.size()));

        long courses = list.stream().filter(m -> "Course".equalsIgnoreCase(normalizeType(m.getTypeMatiere()))).count();
        long tutorialsAndLabs = list.stream().filter(m ->
                "Tutorial".equalsIgnoreCase(normalizeType(m.getTypeMatiere())) ||
                        "Lab".equalsIgnoreCase(normalizeType(m.getTypeMatiere()))).count();
        double avg = list.stream().mapToDouble(Matiere::getCoefficientMatiere).average().orElse(0);

        totalCoursLabel.setText(String.valueOf(courses));
        totalTDLabel.setText(String.valueOf(tutorialsAndLabs));
        avgCoeffLabel.setText(String.format("%.1f", avg));
    }

    private void renderCards(List<Matiere> list) {
        matieresGrid.getChildren().clear();
        for (int i = 0; i < list.size(); i++) {
            matieresGrid.getChildren().add(buildCard(list.get(i), COLORS[i % COLORS.length]));
        }
    }

    private VBox buildCard(Matiere matiere, String color) {
        VBox card = new VBox(0);
        card.setPrefWidth(280);
        applyCardStyle(card, color, false);

        HBox topBar = new HBox();
        topBar.setPrefHeight(6);
        topBar.setStyle("-fx-background-color: " + colorHex(color) + "; -fx-background-radius: 16 16 0 0;");

        VBox body = new VBox(12);
        body.setPadding(new Insets(16));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label codeBadge = new Label(matiere.getCode() != null ? matiere.getCode() : "-");
        codeBadge.setStyle("-fx-background-color: " + colorHex(color) + "22; " +
                "-fx-text-fill: " + colorHex(color) + "; -fx-font-size: 11px; -fx-font-weight: bold; " +
                "-fx-padding: 3 8; -fx-background-radius: 6;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label typeBadge = new Label(displayType(matiere.getTypeMatiere()));
        typeBadge.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #94A3B8; -fx-font-size: 10px; -fx-padding: 3 8; -fx-background-radius: 6;");

        header.getChildren().addAll(codeBadge, spacer, typeBadge);

        Label nameLabel = new Label(matiere.getNomMatiere());
        nameLabel.setStyle("-fx-text-fill: #F8FAFC; -fx-font-size: 15px; -fx-font-weight: bold;");
        nameLabel.setWrapText(true);
        body.getChildren().addAll(header, nameLabel);

        if (matiere.getDescription() != null && !matiere.getDescription().isBlank()) {
            Label description = new Label(matiere.getDescription());
            description.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
            description.setWrapText(true);
            body.getChildren().add(description);
        }

        HBox infos = new HBox(16);
        infos.setAlignment(Pos.CENTER_LEFT);
        infos.setPadding(new Insets(8, 0, 0, 0));
        infos.setStyle("-fx-border-color: #1E293B; -fx-border-width: 1 0 0 0;");
        infos.getChildren().addAll(
                infoChip("fth-award", "Coeff. " + matiere.getCoefficientMatiere(), color),
                infoChip("fth-clock", matiere.getHeureMatiere() + "h", "muted"),
                infoChip("fth-tag", matiere.getSectionMatiere() != null ? matiere.getSectionMatiere() : "-", "muted")
        );

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button btnEdit = new Button("Edit");
        btnEdit.setStyle("-fx-background-color: transparent; -fx-text-fill: " + colorHex(color) + "; " +
                "-fx-border-color: " + colorHex(color) + "; -fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        btnEdit.setOnAction(e -> startEdit(matiere));

        Button btnDelete = new Button("Delete");
        btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #FB7185; " +
                "-fx-border-color: #FB7185; -fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-font-size: 12px; -fx-padding: 6 14; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> deleteItem(matiere));

        actions.getChildren().addAll(btnEdit, btnDelete);
        body.getChildren().addAll(infos, actions);
        card.getChildren().addAll(topBar, body);

        card.setOnMouseEntered(e -> applyCardStyle(card, color, true));
        card.setOnMouseExited(e -> applyCardStyle(card, color, false));
        return card;
    }

    private HBox infoChip(String icon, String text, String color) {
        HBox chip = new HBox(5);
        chip.setAlignment(Pos.CENTER_LEFT);
        FontIcon ic = new FontIcon(icon);
        ic.setIconSize(12);
        ic.setIconColor(javafx.scene.paint.Color.web("muted".equals(color) ? "#64748B" : colorHex(color)));
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: " + ("muted".equals(color) ? "#64748B" : colorHex(color)) + "; -fx-font-size: 11px;");
        chip.getChildren().addAll(ic, lbl);
        return chip;
    }

    @FXML
    private void handleShowForm() {
        editTarget = null;
        clearForm();
        formTitle.setText("Subject Information");
        btnSave.setText("Save Subject");
        showView("form");
    }

    @FXML
    private void handleSave() {
        formError.setVisible(false);
        if (!validateForm()) {
            return;
        }

        try {
            Matiere matiere = editTarget != null ? editTarget : new Matiere();
            matiere.setNomMatiere(fldNom.getText().trim());
            matiere.setCode(fldCode.getText().trim());
            matiere.setCoefficientMatiere(Double.parseDouble(fldCoeff.getText().trim()));
            matiere.setHeureMatiere(Double.parseDouble(fldHeures.getText().trim()));
            matiere.setSectionMatiere(cmbSection.getValue());
            matiere.setTypeMatiere(normalizeType(cmbType.getValue()));

            String description = fldDesc.getText().trim();
            matiere.setDescription(description.isEmpty() ? null : description);

            matiere.setUserId(1);

            if (editTarget == null) {
                service.create(matiere);
            } else {
                service.update(matiere);
            }

            clearForm();
            showView("list");
            loadData();
        } catch (Exception ex) {
            showError("Unexpected error: " + ex.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        clearForm();
        showView("list");
    }

    private void startEdit(Matiere matiere) {
        editTarget = matiere;
        fldNom.setText(matiere.getNomMatiere());
        fldCode.setText(matiere.getCode());
        fldCoeff.setText(String.valueOf(matiere.getCoefficientMatiere()));
        fldHeures.setText(String.valueOf(matiere.getHeureMatiere()));
        cmbSection.setValue(matiere.getSectionMatiere());
        cmbType.setValue(normalizeType(matiere.getTypeMatiere()));
        fldDesc.setText(matiere.getDescription() != null ? matiere.getDescription() : "");
        formTitle.setText("Edit: " + matiere.getNomMatiere());
        btnSave.setText("Update Subject");
        showView("form");
    }

    private void deleteItem(Matiere matiere) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + matiere.getNomMatiere() + "\"?", ButtonType.YES, ButtonType.CANCEL);
        alert.setTitle("Confirm deletion");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                service.delete(matiere.getId());
                loadData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void filterAll() {
        renderCards(allItems);
    }

    @FXML
    private void filterCours() {
        renderCards(allItems.filtered(m -> "Course".equalsIgnoreCase(normalizeType(m.getTypeMatiere()))));
    }

    @FXML
    private void filterTD() {
        renderCards(allItems.filtered(m -> "Tutorial".equalsIgnoreCase(normalizeType(m.getTypeMatiere()))));
    }

    @FXML
    private void filterTP() {
        renderCards(allItems.filtered(m -> "Lab".equalsIgnoreCase(normalizeType(m.getTypeMatiere()))));
    }

    private void filter(String term) {
        if (term == null || term.isBlank()) {
            renderCards(allItems);
            return;
        }
        String low = term.toLowerCase();
        renderCards(allItems.filtered(m ->
                (m.getNomMatiere() != null && m.getNomMatiere().toLowerCase().contains(low)) ||
                        (m.getCode() != null && m.getCode().toLowerCase().contains(low))
        ));
    }

    private void clearForm() {
        editTarget = null;
        fldNom.clear();
        fldCode.clear();
        fldCoeff.clear();
        fldHeures.clear();
        cmbSection.getSelectionModel().clearSelection();
        cmbType.getSelectionModel().selectFirst();
        fldDesc.clear();
        formError.setVisible(false);
        fldNom.setStyle(null);
        fldCode.setStyle(null);
        fldCoeff.setStyle(null);
        fldHeures.setStyle(null);
        cmbSection.setStyle(null);
        cmbType.setStyle(null);
        fldDesc.setStyle(null);

        if (descCharCount != null) {
            descCharCount.setText("0 / 500");
            descCharCount.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        }
    }

    private void showError(String msg) {
        formError.setText("⚠ " + msg);
        formError.setVisible(true);

        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> formError.setVisible(false));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String colorHex(String color) {
        return switch (color) {
            case "primary" -> "#A78BFA";
            case "success" -> "#34D399";
            case "accent" -> "#FB923C";
            case "warning" -> "#FBBF24";
            case "danger" -> "#FB7185";
            default -> "#94A3B8";
        };
    }

    private boolean validateField(TextField field, String value, String regex, String errorMsg) {
        if (value.isEmpty() || !value.matches(regex)) {
            field.setStyle("-fx-border-color: #FB7185; -fx-border-width: 2;");
            showError(errorMsg);
            return false;
        }
        field.setStyle("-fx-border-color: #34D399; -fx-border-width: 2;");
        return true;
    }

    private boolean validateCombo(ComboBox<?> combo, String errorMsg) {
        if (combo.getValue() == null) {
            combo.setStyle("-fx-border-color: #FB7185; -fx-border-width: 2;");
            showError(errorMsg);
            return false;
        }
        combo.setStyle("-fx-border-color: #34D399; -fx-border-width: 2;");
        return true;
    }

    private boolean validateForm() {
        boolean valid = true;
        valid &= validateField(fldNom, fldNom.getText().trim(), ".{3,}", "Subject name must have at least 3 characters.");
        valid &= validateField(fldCode, fldCode.getText().trim(), "[A-Za-z0-9]+", "Code must be alphanumeric.");
        valid &= validateField(fldCoeff, fldCoeff.getText().trim(), "\\d+(\\.\\d+)?", "Coefficient must be a valid number.");
        valid &= validateField(fldHeures, fldHeures.getText().trim(), "\\d+(\\.\\d+)?", "Hours must be a valid number.");
        valid &= validateCombo(cmbSection, "Please select a section.");
        valid &= validateCombo(cmbType, "Please select a type.");
        valid &= validateDescription(fldDesc.getText());

        return valid;
    }

    private void applyCardStyle(VBox card, String color, boolean hover) {
        String background = hover ? "#1E293B" : "#0F172A";
        String border = hover ? colorHex(color) : "#1E293B";
        card.setStyle("-fx-background-color: " + background + "; -fx-background-radius: 16; " +
                "-fx-border-color: " + border + "; -fx-border-radius: 16; -fx-border-width: 1;");
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "";
        }
        return switch (type.toLowerCase()) {
            case "cours", "course" -> "Course";
            case "td", "tutorial" -> "Tutorial";
            case "tp", "lab" -> "Lab";
            default -> type;
        };
    }

    private String displayType(String type) {
        String normalized = normalizeType(type);
        return normalized.isBlank() ? "-" : normalized;
    }

    private void fixTextAreaVisibility() {
        fldDesc.setStyle(
                "-fx-control-inner-background: #1e293b;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: #94a3b8;" +
                        "-fx-highlight-fill: #334155;" +
                        "-fx-highlight-text-fill: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: #334155;" +
                        "-fx-border-width: 2;" +
                        "-fx-padding: 10;"
        );

        Platform.runLater(() -> {
            if (fldDesc.lookup(".content") != null) {
                fldDesc.lookup(".content")
                        .setStyle("-fx-background-color: transparent;");
            }
        });
    }
}