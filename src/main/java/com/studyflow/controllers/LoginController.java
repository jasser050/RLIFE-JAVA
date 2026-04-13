package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.LocalServer;
import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;

import com.studyflow.utils.UserSession;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginBtn;
    @FXML private Button themeBtn;
    @FXML private StackPane robotContainer;
    @FXML private WebView splineWebView;

    private final ServiceUser serviceUser = new ServiceUser();
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean lightMode = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        passwordField.setOnAction(e -> handleLogin());
        emailField.setOnAction(e -> passwordField.requestFocus());

        // Load marketing page with 3D robot in the right panel using WebView
        String htmlPath = getClass().getResource("/com/studyflow/views/spline.html").toExternalForm();
        splineWebView.getEngine().load(htmlPath);
        
        // Make WebView fill the container
        splineWebView.prefWidthProperty().bind(robotContainer.widthProperty());
        splineWebView.prefHeightProperty().bind(robotContainer.heightProperty());
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        hideError();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        loginBtn.setDisable(true);
        loginBtn.setText("Signing in...");

        // Admin shortcut — hardcoded credentials, no DB record needed
        if ("admin@rlife.com".equalsIgnoreCase(email) && "admin123".equals(password)) {
            User admin = new User();
            admin.setId(-1);
            admin.setEmail("admin@rlife.com");
            admin.setFirstName("Admin");
            admin.setLastName("");
            admin.setUsername("admin");
            UserSession.getInstance().setCurrentUser(admin);
            try {
                App.setRoot("views/AdminLayout");
            } catch (IOException e) {
                showError("Failed to load admin panel.");
                resetButton();
            }
            return;
        }

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
    private void toggleTheme() {
        lightMode = !lightMode;
        String lightCss = getClass().getResource("/com/studyflow/styles/auth-light.css").toExternalForm();
        javafx.scene.Scene scene = themeBtn.getScene();
        if (lightMode) {
            if (!scene.getStylesheets().contains(lightCss)) {
                scene.getStylesheets().add(lightCss);
            }
            FontIcon icon = new FontIcon("fth-moon");
            icon.setIconSize(13);
            themeBtn.setGraphic(icon);
        } else {
            scene.getStylesheets().remove(lightCss);
            FontIcon icon = new FontIcon("fth-sun");
            icon.setIconSize(13);
            themeBtn.setGraphic(icon);
        }
    }

    @FXML
    private void handleFaceLogin() {
        hideError();
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError("Please enter your email first, then use Face ID.");
            return;
        }

        // Check if user exists and has face data
        User user = serviceUser.findByEmail(email);
        if (user == null) {
            showError("No account found with this email.");
            return;
        }
        if (!LocalServer.hasFaceData(email)) {
            showError("No Face ID registered for this account.");
            return;
        }

        // Generate QR code for login mode
        String faceUrl = LocalServer.lanUrl("/views/face-id-mobile.html") + "?mode=login&email=" + email;
        LocalServer.resetFaceIdStatus();

        WritableImage qrImage;
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(faceUrl, BarcodeFormat.QR_CODE, 240, 240,
                    Map.of(EncodeHintType.MARGIN, 1));
            qrImage = new WritableImage(240, 240);
            PixelWriter pw = qrImage.getPixelWriter();
            for (int y = 0; y < 240; y++)
                for (int x = 0; x < 240; x++)
                    pw.setColor(x, y, matrix.get(x, y) ? Color.web("#E2E8F0") : Color.web("#0F172A"));
        } catch (WriterException e) {
            showError("Failed to generate QR code.");
            return;
        }

        // Show QR dialog
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Face ID Login");
        dialog.setHeaderText(null);

        ImageView qrView = new ImageView(qrImage);
        Label instruction = new Label("Scan with your phone to verify your face");
        instruction.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");
        Label status = new Label("Waiting for face scan...");
        status.setStyle("-fx-text-fill: #8B5CF6; -fx-font-size: 14px; -fx-font-weight: bold;");

        VBox content = new VBox(16, qrView, instruction, status);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0F172A;");

        DialogPane pane = dialog.getDialogPane();
        pane.setContent(content);
        pane.setStyle("-fx-background-color: #0F172A;");
        pane.getButtonTypes().add(ButtonType.CANCEL);

        // Poll for face result
        final User foundUser = user;
        Thread poller = new Thread(() -> {
            while (dialog.isShowing()) {
                try { Thread.sleep(800); } catch (InterruptedException e) { break; }
                String faceStatus = LocalServer.getFaceIdStatus();
                if ("captured".equals(faceStatus)) {
                    boolean match = LocalServer.isFaceLoginMatch();
                    Platform.runLater(() -> {
                        if (match) {
                            status.setText("Face verified! Logging in...");
                            status.setStyle("-fx-text-fill: #34D399; -fx-font-size: 14px; -fx-font-weight: bold;");
                            dialog.close();
                            UserSession.getInstance().setCurrentUser(foundUser);
                            try {
                                App.setRoot("views/MainLayout");
                            } catch (IOException ex) {
                                showError("Failed to load app.");
                            }
                        } else {
                            status.setText("Face does not match! Try again.");
                            status.setStyle("-fx-text-fill: #F43F5E; -fx-font-size: 14px; -fx-font-weight: bold;");
                            LocalServer.resetFaceIdStatus();
                        }
                    });
                    if (match) break;
                }
            }
        });
        poller.setDaemon(true);
        poller.start();

        dialog.showAndWait();
    }

    @FXML
    private void forgotPassword() throws IOException {
        App.setRoot("views/ForgotPassword");
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
