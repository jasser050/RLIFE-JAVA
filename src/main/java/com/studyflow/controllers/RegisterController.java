package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    @FXML private WebView splineView;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Button registerBtn;

    private final ServiceUser serviceUser = new ServiceUser();
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        String splineHtml = getClass().getResource("/com/studyflow/views/spline.html").toExternalForm();
        splineView.getEngine().load(splineHtml);
        splineView.getEngine().setJavaScriptEnabled(true);
    }

    @FXML
    private void handleRegister() {
        hideError();

        String firstName = firstNameField.getText() == null ? "" : firstNameField.getText().trim();
        String lastName  = lastNameField.getText() == null ? "" : lastNameField.getText().trim();
        String username  = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String email     = emailField.getText() == null ? "" : emailField.getText().trim().toLowerCase();
        String password  = passwordField.getText() == null ? "" : passwordField.getText();
        String confirm   = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        // Validation
        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty()
                || email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all required fields.");
            return;
        }
        if (username.length() < 3 || username.length() > 20) {
            showError("Username must be between 3 and 20 characters.");
            return;
        }
        if (!email.contains("@")) {
            showError("Please enter a valid email address.");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }
        if (serviceUser.findByEmail(email) != null) {
            showError("An account with this email already exists.");
            return;
        }

        registerBtn.setDisable(true);
        registerBtn.setText("Creating account...");

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);

        serviceUser.add(user);
        if (user.getId() <= 0) {
            showError("Account creation failed. Check database connection or constraints.");
            resetButton();
            return;
        }

        try {
            App.setRoot("views/Login");
        } catch (IOException e) {
            showError("Account created! Please go to Sign In.");
            resetButton();
        }
    }

    @FXML
    private void goToLogin() throws IOException {
        App.setRoot("views/Login");
    }

    @FXML
    private void goToLanding() throws IOException {
        App.setRoot("views/Landing");
    }

    @FXML
    private void onDragStart(MouseEvent e) {
        xOffset = App.getPrimaryStage().getX() - e.getScreenX();
        yOffset = App.getPrimaryStage().getY() - e.getScreenY();
    }

    @FXML
    private void onDragged(MouseEvent e) {
        App.getPrimaryStage().setX(e.getScreenX() + xOffset);
        App.getPrimaryStage().setY(e.getScreenY() + yOffset);
    }

    @FXML
    private void minimizeWindow() { App.getPrimaryStage().setIconified(true); }

    @FXML
    private void maximizeWindow() {
        Stage stage = App.getPrimaryStage();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML
    private void closeWindow() { Platform.exit(); }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void resetButton() {
        registerBtn.setDisable(false);
        registerBtn.setText("Create Account");
    }
}
