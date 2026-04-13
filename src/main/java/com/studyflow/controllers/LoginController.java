package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import com.studyflow.utils.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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
        String splineHtml = getClass().getResource("/com/studyflow/views/spline.html").toExternalForm();
        splineView.getEngine().load(splineHtml);
        splineView.getEngine().setJavaScriptEnabled(true);

        // Explicit event wiring to avoid silent FXML action binding failures.
        loginBtn.setOnAction(e -> handleLogin());
        loginBtn.setDefaultButton(true);

        passwordField.setOnAction(e -> handleLogin());
        emailField.setOnAction(e -> passwordField.requestFocus());
    }

    @FXML
    private void handleLogin() {
        try {
            String email = emailField.getText() == null ? "" : emailField.getText().trim().toLowerCase();
            String password = passwordField.getText() == null ? "" : passwordField.getText();

            hideError();

            if (email.isEmpty() || password.isEmpty()) {
                showError("Please fill in all fields.");
                return;
            }

            loginBtn.setDisable(true);
            loginBtn.setText("Signing in...");

            User user = resolveLoginUser(email, password);
            if (user == null) {
                showError("Invalid email or password.");
                resetButton();
                return;
            }

            UserSession.getInstance().setCurrentUser(user);
            App.setRoot(user.isAdmin() ? "views/admin/AdminMainLayout" : "views/MainLayout");
        } catch (Exception e) {
            showError("Sign in failed: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
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
    private void minimizeWindow() {
        App.getPrimaryStage().setIconified(true);
    }

    @FXML
    private void maximizeWindow() {
        Stage stage = App.getPrimaryStage();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML
    private void closeWindow() {
        Platform.exit();
    }

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

    private User resolveLoginUser(String email, String password) {
        User demoAdmin = buildDemoAdmin(email, password);
        if (demoAdmin != null) {
            return demoAdmin;
        }
        return serviceUser.authenticate(email, password);
    }

    private User buildDemoAdmin(String email, String password) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (!"admin123".equals(password)) {
            return null;
        }

        if (!"admin@rlife.com".equals(normalizedEmail) && !"admin1@rlife.com".equals(normalizedEmail)) {
            return null;
        }

        User user = new User();
        user.setId(-1);
        user.setEmail(normalizedEmail);
        user.setFirstName("Admin");
        user.setLastName("RLife");
        user.setUsername(normalizedEmail.substring(0, normalizedEmail.indexOf('@')));
        user.setPassword(password);
        user.setUniversity("RLife Administration");
        return user;
    }
}
