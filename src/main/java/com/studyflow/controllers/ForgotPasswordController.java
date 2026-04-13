package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import com.studyflow.utils.EmailService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ForgotPasswordController implements Initializable {

    @FXML private TextField emailField;
    @FXML private TextField codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Label subtitleLabel;
    @FXML private Button sendCodeBtn;

    @FXML private VBox emailPane;
    @FXML private VBox codePane;
    @FXML private VBox passwordPane;

    private final ServiceUser serviceUser = new ServiceUser();
    private String currentResetCode;
    private String currentEmail;
    private long codeTimestamp;
    private double xOffset = 0, yOffset = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        emailField.setOnAction(e -> handleSendCode());
        codeField.setOnAction(e -> handleVerifyCode());
        confirmPasswordField.setOnAction(e -> handleResetPassword());
    }

    @FXML
    private void handleSendCode() {
        hideMessages();
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError("Please enter your email address.");
            return;
        }

        User user = serviceUser.findByEmail(email);
        if (user == null) {
            showError("No account found with this email.");
            return;
        }

        currentEmail = email;
        currentResetCode = EmailService.generateResetCode();
        codeTimestamp = System.currentTimeMillis();

        sendCodeBtn.setDisable(true);
        sendCodeBtn.setText("Sending...");

        // Send email in background thread
        new Thread(() -> {
            try {
                EmailService.sendPasswordResetEmail(email, currentResetCode);
                Platform.runLater(() -> {
                    showSuccess("Reset code sent to " + email + "! Check your inbox.");
                    subtitleLabel.setText("Enter the 6-digit code we sent to your email");

                    // Move to step 2
                    emailPane.setVisible(false);
                    emailPane.setManaged(false);
                    codePane.setVisible(true);
                    codePane.setManaged(true);
                    codeField.requestFocus();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showError("Failed to send email: " + ex.getMessage());
                    sendCodeBtn.setDisable(false);
                    sendCodeBtn.setText("Send Reset Code");
                });
            }
        }).start();
    }

    @FXML
    private void handleVerifyCode() {
        hideMessages();
        String code = codeField.getText().trim();

        if (code.isEmpty()) {
            showError("Please enter the reset code.");
            return;
        }

        // Check expiration (10 minutes)
        if (System.currentTimeMillis() - codeTimestamp > 10 * 60 * 1000) {
            showError("Reset code has expired. Please request a new one.");
            // Go back to step 1
            codePane.setVisible(false);
            codePane.setManaged(false);
            emailPane.setVisible(true);
            emailPane.setManaged(true);
            sendCodeBtn.setDisable(false);
            sendCodeBtn.setText("Send Reset Code");
            subtitleLabel.setText("Enter your email to receive a reset code");
            return;
        }

        if (!code.equals(currentResetCode)) {
            showError("Invalid reset code. Please try again.");
            return;
        }

        // Move to step 3
        showSuccess("Code verified! Set your new password.");
        subtitleLabel.setText("Create a new password for your account");
        codePane.setVisible(false);
        codePane.setManaged(false);
        passwordPane.setVisible(true);
        passwordPane.setManaged(true);
        newPasswordField.requestFocus();
    }

    @FXML
    private void handleResetPassword() {
        hideMessages();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            showError("Please fill in both password fields.");
            return;
        }

        if (newPass.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showError("Passwords do not match.");
            return;
        }

        try {
            serviceUser.updatePassword(currentEmail, newPass);
            showSuccess("Password reset successfully! Redirecting to login...");

            // Redirect to login after a short delay
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    try { App.setRoot("views/Login"); } catch (IOException e) { e.printStackTrace(); }
                });
            }).start();
        } catch (Exception e) {
            showError("Failed to reset password: " + e.getMessage());
        }
    }

    @FXML
    private void goToLogin() throws IOException {
        App.setRoot("views/Login");
    }

    // ── Window controls ─────────────────────────────────────────────

    @FXML private void onDragStart(MouseEvent e) {
        xOffset = App.getPrimaryStage().getX() - e.getScreenX();
        yOffset = App.getPrimaryStage().getY() - e.getScreenY();
    }
    @FXML private void onDragged(MouseEvent e) {
        App.getPrimaryStage().setX(e.getScreenX() + xOffset);
        App.getPrimaryStage().setY(e.getScreenY() + yOffset);
    }
    @FXML private void minimizeWindow() { App.getPrimaryStage().setIconified(true); }
    @FXML private void maximizeWindow() {
        Stage stage = App.getPrimaryStage();
        stage.setMaximized(!stage.isMaximized());
    }
    @FXML private void closeWindow() { Platform.exit(); }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    private void showSuccess(String msg) {
        successLabel.setText(msg);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void hideMessages() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }
}
