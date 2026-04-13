package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import com.studyflow.utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private Label avatarInitials;
    @FXML private Label profileName;
    @FXML private Label profileEmail;
    @FXML private Label profileUsername;
    @FXML private Label coinsLabel;
    @FXML private Label saveStatus;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField usernameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private RadioButton genderMale;
    @FXML private RadioButton genderFemale;
    @FXML private TextArea bioField;

    @FXML private TextField universityField;
    @FXML private TextField studentIdField;

    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    private final ServiceUser serviceUser = new ServiceUser();
    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Gender toggle group
        ToggleGroup genderGroup = new ToggleGroup();
        genderMale.setToggleGroup(genderGroup);
        genderFemale.setToggleGroup(genderGroup);

        currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            populateFields(currentUser);
        }
    }

    private void populateFields(User user) {
        // Hero card
        avatarInitials.setText(user.getInitials().isEmpty() ? "??" : user.getInitials());
        profileName.setText(user.getFullName().trim());
        profileEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        profileUsername.setText("@" + (user.getUsername() != null ? user.getUsername() : ""));
        coinsLabel.setText(String.valueOf(user.getCoins()));

        // Form fields
        firstNameField.setText(orEmpty(user.getFirstName()));
        lastNameField.setText(orEmpty(user.getLastName()));
        usernameField.setText(orEmpty(user.getUsername()));
        phoneField.setText(orEmpty(user.getPhoneNumber()));
        emailField.setText(orEmpty(user.getEmail()));
        bioField.setText(orEmpty(user.getBio()));
        universityField.setText(orEmpty(user.getUniversity()));
        studentIdField.setText(orEmpty(user.getStudentId()));

        if ("female".equalsIgnoreCase(user.getGender())) {
            genderFemale.setSelected(true);
        } else {
            genderMale.setSelected(true);
        }
    }

    @FXML
    private void handleSave() {
        if (currentUser == null) return;

        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (!newPass.isEmpty()) {
            if (newPass.length() < 6) {
                showStatus("Password must be at least 6 characters.", false);
                return;
            }
            if (!newPass.equals(confirmPass)) {
                showStatus("Passwords do not match.", false);
                return;
            }
            currentUser.setPassword(newPass);
        }

        currentUser.setFirstName(firstNameField.getText().trim());
        currentUser.setLastName(lastNameField.getText().trim());
        currentUser.setUsername(usernameField.getText().trim());
        currentUser.setPhoneNumber(phoneField.getText().trim());
        currentUser.setBio(bioField.getText().trim());
        currentUser.setUniversity(universityField.getText().trim());
        currentUser.setStudentId(studentIdField.getText().trim());
        currentUser.setGender(genderMale.isSelected() ? "male" : "female");

        serviceUser.update(currentUser);

        // Refresh hero card
        avatarInitials.setText(currentUser.getInitials());
        profileName.setText(currentUser.getFullName().trim());
        profileUsername.setText("@" + currentUser.getUsername());

        newPasswordField.clear();
        confirmPasswordField.clear();

        showStatus("Changes saved successfully!", true);
    }

    @FXML
    private void handleLogout() {
        UserSession.getInstance().logout();
        try {
            App.setRoot("views/Landing");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showStatus(String msg, boolean success) {
        saveStatus.setText(msg);
        saveStatus.getStyleClass().removeAll("success", "danger");
        saveStatus.getStyleClass().add(success ? "success" : "danger");
        saveStatus.setVisible(true);
        saveStatus.setManaged(true);
    }

    private String orEmpty(String val) {
        return val != null ? val : "";
    }
}
