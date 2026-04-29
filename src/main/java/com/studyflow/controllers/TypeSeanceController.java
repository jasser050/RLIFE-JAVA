package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.TypeSeance;
import com.studyflow.models.User;
import com.studyflow.services.ServiceTypeSeance;
import com.studyflow.utils.UserSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class TypeSeanceController implements Initializable {

    private static final Pattern NAME_PATTERN = Pattern.compile("^(?=.{2,60}$)(?=.*\\p{L})[\\p{L} _-]+$");

    @FXML private Label typesCountLabel;
    @FXML private Label pageMessageLabel;
    @FXML private Label formTitleLabel;
    @FXML private VBox formCard;
    @FXML private VBox listCard;
    @FXML private TextField typeNameField;
    @FXML private Label typeNameErrorLabel;
    @FXML private ListView<TypeSeance> typesListView;

    private final ServiceTypeSeance serviceTypeSeance = new ServiceTypeSeance();
    private TypeSeance editingType;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureListView();
        configureFormValidation();
        resetForm();
        showListMode();
        refreshList();
    }

    @FXML
    private void handleBackToPlanning() {
        MainController mainController = MainController.getInstance();
        if (mainController != null) {
            mainController.showPlanning();
            return;
        }
        try {
            App.setRoot("views/Planning");
        } catch (IOException exception) {
            showError("Unable to open planning page: " + exception.getMessage());
        }
    }

    @FXML
    private void handleAddType() {
        startAdd();
    }

    @FXML
    private void handleSaveType() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            showError("No logged-in user found.");
            return;
        }

        String name = sanitizeName(typeNameField.getText());
        if (!validateName(name, editingType == null ? null : editingType.getId(), true)) {
            showError("Please fix the highlighted field.");
            return;
        }

        boolean isCreation = editingType == null;
        TypeSeance typeSeance = isCreation ? new TypeSeance() : editingType;
        typeSeance.setName(name);
        typeSeance.setUserId(currentUser.getId());

        if (isCreation) {
            serviceTypeSeance.add(typeSeance);
        } else {
            serviceTypeSeance.update(typeSeance);
        }

        refreshList();
        resetForm();
        showListMode();
        showSuccess(isCreation ? "Session type added successfully." : "Session type updated successfully.");
    }

    @FXML
    private void handleCancelType() {
        resetForm();
        showListMode();
        showInfo("Action cancelled.");
    }

    private void configureListView() {
        typesListView.setCellFactory(listView -> new TypeSeanceCell());
    }

    private void configureFormValidation() {
        if (typeNameField == null) {
            return;
        }
        typeNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (typeNameErrorLabel != null && typeNameErrorLabel.isVisible()) {
                validateName(sanitizeName(newValue), editingType == null ? null : editingType.getId(), true);
            }
        });
    }

    private void refreshList() {
        List<TypeSeance> allTypes = serviceTypeSeance.getAll();
        typesListView.setItems(FXCollections.observableArrayList(allTypes));
        typesCountLabel.setText(allTypes.size() + " types");
    }

    private void startAdd() {
        editingType = null;
        formTitleLabel.setText("Add Type");
        typeNameField.clear();
        clearValidationErrors();
        showFormMode();
        showInfo("Enter a type name then save.");
    }

    private void startEdit(TypeSeance typeSeance) {
        if (!serviceTypeSeance.isOwnedByCurrentUser(typeSeance)) {
            showInfo("Global types are read-only.");
            return;
        }

        editingType = typeSeance;
        formTitleLabel.setText("Edit Type");
        typeNameField.setText(typeSeance.getName() == null ? "" : typeSeance.getName());
        clearValidationErrors();
        showFormMode();
        showInfo("Update the selected type and save.");
    }

    private void deleteType(TypeSeance typeSeance) {
        if (!serviceTypeSeance.isOwnedByCurrentUser(typeSeance)) {
            showInfo("Global types cannot be deleted.");
            return;
        }

        if (serviceTypeSeance.hasLinkedSessions(typeSeance.getId())) {
            showError("Cannot delete this type because it is linked to existing sessions.");
            return;
        }

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Delete Session Type");
        confirmationAlert.setHeaderText("Delete selected session type?");
        confirmationAlert.setContentText("You are about to delete \"" + typeSeance.getName() + "\".");

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            showInfo("Deletion cancelled.");
            return;
        }

        serviceTypeSeance.delete(typeSeance);
        refreshList();
        if (editingType != null && editingType.getId() == typeSeance.getId()) {
            resetForm();
            showListMode();
        }
        showSuccess("Session type deleted successfully.");
    }

    private String sanitizeName(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean validateName(String name, Integer excludedTypeId, boolean showMessage) {
        if (name.isBlank()) {
            if (showMessage) {
                markFieldInvalid(typeNameField, typeNameErrorLabel, "Type name is required.");
            }
            return false;
        }

        if (!NAME_PATTERN.matcher(name).matches()) {
            if (showMessage) {
                markFieldInvalid(typeNameField, typeNameErrorLabel,
                        "Use letters only. Spaces, '_' and '-' are allowed.");
            }
            return false;
        }

        if (serviceTypeSeance.isNameUsedByCurrentUser(name, excludedTypeId)) {
            if (showMessage) {
                markFieldInvalid(typeNameField, typeNameErrorLabel, "You already have a type with this name.");
            }
            return false;
        }

        clearFieldValidation(typeNameField, typeNameErrorLabel);
        return true;
    }

    private void resetForm() {
        editingType = null;
        if (formTitleLabel != null) {
            formTitleLabel.setText("Add Type");
        }
        if (typeNameField != null) {
            typeNameField.clear();
        }
        clearValidationErrors();
    }

    private void clearValidationErrors() {
        clearFieldValidation(typeNameField, typeNameErrorLabel);
    }

    private void showFormMode() {
        if (formCard != null) {
            formCard.setVisible(true);
            formCard.setManaged(true);
        }
        if (listCard != null) {
            listCard.setVisible(false);
            listCard.setManaged(false);
        }
    }

    private void showListMode() {
        if (formCard != null) {
            formCard.setVisible(false);
            formCard.setManaged(false);
        }
        if (listCard != null) {
            listCard.setVisible(true);
            listCard.setManaged(true);
        }
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

    private void showError(String message) {
        pageMessageLabel.setText(message);
        pageMessageLabel.getStyleClass().setAll("auth-error");
        pageMessageLabel.setStyle("");
        pageMessageLabel.setVisible(true);
        pageMessageLabel.setManaged(true);
    }

    private void showInfo(String message) {
        showStyledMessage(message,
                "-fx-background-color: rgba(139,92,246,0.12); -fx-border-color: rgba(139,92,246,0.35); -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #C4B5FD; -fx-padding: 12 16; -fx-font-size: 13px;");
    }

    private void showSuccess(String message) {
        showStyledMessage(message,
                "-fx-background-color: rgba(16,185,129,0.12); -fx-border-color: rgba(16,185,129,0.35); -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #34D399; -fx-padding: 12 16; -fx-font-size: 13px;");
    }

    private void showStyledMessage(String message, String style) {
        pageMessageLabel.setText(message);
        pageMessageLabel.getStyleClass().clear();
        pageMessageLabel.setStyle(style);
        pageMessageLabel.setVisible(true);
        pageMessageLabel.setManaged(true);
    }

    private final class TypeSeanceCell extends ListCell<TypeSeance> {
        private final HBox row = new HBox(10);
        private final VBox infoBox = new VBox(2);
        private final Label nameLabel = new Label();
        private final Label scopeLabel = new Label();
        private final Region spacer = new Region();
        private final HBox actionsBox = new HBox(8);
        private final Button editButton = new Button("Edit");
        private final Button deleteButton = new Button("Delete");

        private TypeSeanceCell() {
            nameLabel.getStyleClass().add("planning-upcoming-item-title");
            scopeLabel.getStyleClass().add("text-small");

            editButton.getStyleClass().add("btn-secondary");
            deleteButton.getStyleClass().add("btn-danger");

            editButton.setOnAction(event -> {
                TypeSeance item = getItem();
                if (item != null) {
                    TypeSeanceController.this.startEdit(item);
                }
            });

            deleteButton.setOnAction(event -> {
                TypeSeance item = getItem();
                if (item != null) {
                    TypeSeanceController.this.deleteType(item);
                }
            });

            infoBox.getChildren().addAll(nameLabel, scopeLabel);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            actionsBox.setAlignment(Pos.CENTER_RIGHT);
            actionsBox.getChildren().addAll(editButton, deleteButton);

            row.getStyleClass().add("planning-upcoming-item-card");
            row.setPadding(new Insets(12));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(infoBox, spacer, actionsBox);

            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(TypeSeance item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            boolean ownedByCurrentUser = serviceTypeSeance.isOwnedByCurrentUser(item);
            nameLabel.setText(item.getName());
            scopeLabel.setText(ownedByCurrentUser ? "My type" : "Global type (read-only)");
            editButton.setDisable(!ownedByCurrentUser);
            deleteButton.setDisable(!ownedByCurrentUser);
            setGraphic(row);
        }
    }
}

