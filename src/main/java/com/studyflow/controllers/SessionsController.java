package com.studyflow.controllers;

import com.studyflow.models.Seance;
import com.studyflow.models.TypeSeance;
import com.studyflow.models.User;
import com.studyflow.services.ServiceSeance;
import com.studyflow.services.ServiceTypeSeance;
import com.studyflow.utils.UserSession;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.net.URL;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class SessionsController implements Initializable {

    @FXML private Label sessionsCountLabel;
    @FXML private Label formTitleLabel;
    @FXML private Label formSubtitleLabel;
    @FXML private Label formMessageLabel;
    @FXML private VBox formCard;
    @FXML private TextField titleField;
    @FXML private ComboBox<TypeSeance> typeComboBox;
    @FXML private TextArea descriptionArea;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private TableView<Seance> sessionsTable;
    @FXML private TableColumn<Seance, String> titleColumn;
    @FXML private TableColumn<Seance, String> typeColumn;
    @FXML private TableColumn<Seance, String> createdColumn;
    @FXML private TableColumn<Seance, Void> actionsColumn;

    private final ServiceSeance serviceSeance = new ServiceSeance();
    private final ServiceTypeSeance serviceTypeSeance = new ServiceTypeSeance();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private Seance editingSeance;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureTable();
        loadTypes();
        resetForm();
        refreshTable();
    }

    @FXML
    private void handleAddSession() {
        resetForm();
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

        if (title.isEmpty()) {
            showError("Title is required.");
            return;
        }

        if (selectedType == null) {
            showError("Type is required.");
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

        refreshTable();
        String successMessage = editingSeance == null
                ? "Session added successfully."
                : "Session updated successfully.";
        resetForm();
        showSuccess(successMessage);
    }

    @FXML
    private void handleCancelForm() {
        resetForm();
        showInfo("The form was reset.");
    }

    @FXML
    private void handleBackToPlanning() {
        MainController mainController = MainController.getInstance();
        if (mainController != null) {
            mainController.showPlanning();
        }
    }

    private void configureTable() {
        titleColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getTitre()));
        typeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(resolveTypeLabel(data.getValue())));
        createdColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatTimestamp(data.getValue().getCreatedAt())));
        actionsColumn.setCellFactory(createActionsCellFactory());
    }

    private Callback<TableColumn<Seance, Void>, TableCell<Seance, Void>> createActionsCellFactory() {
        return column -> new TableCell<Seance, Void>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox actionsBox = new HBox(8, editButton, deleteButton);

            {
                editButton.getStyleClass().add("btn-secondary");
                deleteButton.getStyleClass().add("btn-danger");

                editButton.setOnAction(event -> {
                    Seance seance = getTableView().getItems().get(getIndex());
                    SessionsController.this.startEdit(seance);
                });

                deleteButton.setOnAction(event -> {
                    Seance seance = getTableView().getItems().get(getIndex());
                    serviceSeance.delete(seance);
                    refreshTable();
                    if (editingSeance != null && editingSeance.getId() == seance.getId()) {
                        resetForm();
                    }
                    showSuccess("Session deleted successfully.");
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionsBox);
            }
        };
    }

    private void loadTypes() {
        List<TypeSeance> availableTypes = serviceTypeSeance.getAvailableTypes();
        typeComboBox.setItems(FXCollections.observableArrayList(availableTypes));
    }

    private void refreshTable() {
        List<Seance> seances = serviceSeance.getAll();
        sessionsTable.setItems(FXCollections.observableArrayList(seances));
        sessionsCountLabel.setText(seances.size() + " sessions");
    }

    private void startEdit(Seance seance) {
        editingSeance = seance;
        formCard.setVisible(true);
        formCard.setManaged(true);
        formTitleLabel.setText("Edit Session");
        formSubtitleLabel.setText("Update the selected session in the same window");
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

        hideMessage();
    }

    private void resetForm() {
        editingSeance = null;
        formCard.setVisible(true);
        formCard.setManaged(true);
        formTitleLabel.setText("Add Session");
        formSubtitleLabel.setText("Create a new session without leaving this page");
        titleField.clear();
        typeComboBox.setValue(null);
        descriptionArea.clear();
        hideMessage();
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
        formMessageLabel.setText(message);
        formMessageLabel.getStyleClass().setAll("auth-error");
        formMessageLabel.setStyle("");
        formMessageLabel.setVisible(true);
        formMessageLabel.setManaged(true);
    }

    private void showSuccess(String message) {
        formMessageLabel.setText(message);
        formMessageLabel.getStyleClass().clear();
        formMessageLabel.setStyle("-fx-background-color: rgba(16,185,129,0.12); -fx-border-color: rgba(16,185,129,0.35); -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #34D399; -fx-padding: 12 16; -fx-font-size: 13px;");
        formMessageLabel.setVisible(true);
        formMessageLabel.setManaged(true);
    }

    private void showInfo(String message) {
        formMessageLabel.setText(message);
        formMessageLabel.getStyleClass().clear();
        formMessageLabel.setStyle("-fx-background-color: rgba(139,92,246,0.12); -fx-border-color: rgba(139,92,246,0.35); -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #C4B5FD; -fx-padding: 12 16; -fx-font-size: 13px;");
        formMessageLabel.setVisible(true);
        formMessageLabel.setManaged(true);
    }

    private void hideMessage() {
        formMessageLabel.setText("");
        formMessageLabel.setVisible(false);
        formMessageLabel.setManaged(false);
        formMessageLabel.setStyle("");
    }
}
