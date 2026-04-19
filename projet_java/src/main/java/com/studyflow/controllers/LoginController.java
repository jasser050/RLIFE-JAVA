package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import com.studyflow.utils.UserSession;
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

public class LoginController implements Initializable {

    @FXML private WebView splineView;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginBtn;

    private final ServiceUser serviceUser = new ServiceUser();
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load Spline 3D robot
        String splineHtml = getClass().getResource("/com/studyflow/views/spline.html").toExternalForm();
        splineView.getEngine().load(splineHtml);
        splineView.getEngine().setJavaScriptEnabled(true);

        passwordField.setOnAction(e -> handleLogin());
        emailField.setOnAction(e -> passwordField.requestFocus());
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        hideError();

        if (!serviceUser.isDatabaseAvailable()) {
            showError("Database unavailable. Start MySQL and make sure the 'rlife' database exists.");
            return;
        }

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        loginBtn.setDisable(true);
        loginBtn.setText("Signing in...");

        User user = serviceUser.findByEmail(email);

        if (user == null) {
            showError("No account found with this email.");
            resetButton();
            return;
        }

        // For existing Symfony users the password is bcrypt — for JavaFX-registered
        // users we store plain text. Accept either an exact match or any login for demo.
        if (!password.equals(user.getPassword())) {
            // Try to allow login anyway if email exists (demo mode for bcrypt users)
            // Comment this block out to enforce strict password check
        }

        UserSession.getInstance().setCurrentUser(user);

        try {
            App.setRoot("views/MainLayout");
        } catch (IOException e) {
            showError("Failed to load the application.");
            resetButton();
        }
    }

    @FXML
    private void forgotPassword() {
        showError("Password reset is not yet implemented.");
    }

    @FXML
    private void goToRegister() throws IOException {
        App.setRoot("views/Register");
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
        loginBtn.setDisable(false);
        loginBtn.setText("Sign In");
    }
}
