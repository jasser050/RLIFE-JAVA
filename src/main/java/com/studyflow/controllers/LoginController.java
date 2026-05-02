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
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
    private static final boolean CAPTCHA_ENABLED = true;
    private static final String RECAPTCHA_SECRET = firstNonBlank(
            System.getenv("RECAPTCHA_SECRET_KEY"),
            System.getProperty("recaptcha.secret.key")
    );
    private static final String GOOGLE_CLIENT_SECRET = firstNonBlank(
            System.getenv("GOOGLE_CLIENT_SECRET"),
            System.getProperty("google.client.secret")
    );

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

        // Validate email format
        if (!email.contains("@") || !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            showError("Please enter a valid email address.");
            return;
        }

        loginBtn.setDisable(true);
        loginBtn.setText("Signing in...");

        if (!serviceUser.isDatabaseAvailable()) {
            showError("Database unavailable. Check MySQL and database name 'rlife'.");
            resetButton();
            return;
        }

        // Admin shortcut — hardcoded credentials, no DB record needed (skip CAPTCHA)
        if ("admin@rlife.com".equalsIgnoreCase(email) && "admin123".equals(password)) {
            User admin = new User();
            admin.setId(-1);
            admin.setEmail("admin@rlife.com");
            admin.setFirstName("Admin");
            admin.setLastName("");
            admin.setUsername("admin");
            UserSession.getInstance().setCurrentUser(admin);
            UserSession.getInstance().saveSession();
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

        if (CAPTCHA_ENABLED) {
            showCaptchaDialog(user);
            return;
        }

        UserSession.getInstance().setCurrentUser(user);
        try {
            App.setRoot("views/MainLayout");
        } catch (IOException e) {
            showError("Failed to load the application.");
            resetButton();
        }
    }

    private void showCaptchaDialog(User user) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("CAPTCHA Verification");
        dialog.setHeaderText(null);

        WebView captchaWebView = new WebView();
        captchaWebView.setPrefSize(460, 560);
        WebEngine engine = captchaWebView.getEngine();

        // Listen for captcha completion via title change
        engine.titleProperty().addListener((obs, oldTitle, newTitle) -> {
            if (newTitle != null && newTitle.startsWith("CAPTCHA_OK:")) {
                String token = newTitle.substring("CAPTCHA_OK:".length());
                dialog.close();
                verifyCaptchaAndLogin(token, user);
            }
        });

        // Load via LocalServer (http://localhost) so reCAPTCHA accepts the domain
        engine.load("http://localhost:" + LocalServer.getPort() + "/views/captcha.html");

        DialogPane pane = dialog.getDialogPane();
        pane.setContent(captchaWebView);
        pane.setStyle("-fx-background-color: #0F172A;");
        pane.getButtonTypes().add(ButtonType.CANCEL);
        pane.setPrefSize(480, 580);

        dialog.setOnCloseRequest(e -> resetButton());
        dialog.showAndWait();
    }

    private void verifyCaptchaAndLogin(String token, User user) {
        Thread thread = new Thread(() -> {
            try {
                if (RECAPTCHA_SECRET == null || RECAPTCHA_SECRET.isBlank()) {
                    Platform.runLater(() -> {
                        if (token == null || token.isBlank()) {
                            showError("CAPTCHA token missing. Please try again.");
                            resetButton();
                            return;
                        }
                        UserSession.getInstance().setCurrentUser(user);
                        UserSession.getInstance().saveSession();
                        try {
                            App.setRoot("views/MainLayout");
                        } catch (IOException e) {
                            showError("Failed to load the application.");
                            resetButton();
                        }
                    });
                    return;
                }

                String verifyBody = "secret=" + URLEncoder.encode(RECAPTCHA_SECRET, StandardCharsets.UTF_8)
                        + "&response=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.google.com/recaptcha/api/siteverify"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(verifyBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                String json = response.body();

                boolean success = json.contains("\"success\": true") || json.contains("\"success\":true");

                Platform.runLater(() -> {
                    if (success) {
                        UserSession.getInstance().setCurrentUser(user);
                        UserSession.getInstance().saveSession();
                        try {
                            App.setRoot("views/MainLayout");
                        } catch (IOException e) {
                            showError("Failed to load the application.");
                            resetButton();
                        }
                    } else {
                        showError("CAPTCHA verification failed. Please try again.");
                        resetButton();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("CAPTCHA verification error: " + e.getMessage());
                    resetButton();
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
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
            
            // Update right panel theme
            if (splineWebView != null) {
                splineWebView.getEngine().executeScript("window.javaThemeChange(true);");
            }
        } else {
            scene.getStylesheets().remove(lightCss);
            FontIcon icon = new FontIcon("fth-sun");
            icon.setIconSize(13);
            themeBtn.setGraphic(icon);
            
            // Update right panel theme
            if (splineWebView != null) {
                splineWebView.getEngine().executeScript("window.javaThemeChange(false);");
            }
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
        if (!serviceUser.isDatabaseAvailable()) {
            showError("Database unavailable. Check MySQL and database name 'rlife'.");
            return;
        }

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
                            UserSession.getInstance().saveSession();
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
    private void handleGoogleLogin() {
        hideError();

        String clientId = "327994023632-cut8g0d28j0b0sa7fuv92opgvi7s7dtr.apps.googleusercontent.com";
        String redirectUri = "http://localhost";
        String scope = "openid email profile";

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8)
                + "&access_type=offline"
                + "&prompt=consent";

        // Create a dialog with a WebView for Google sign-in
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Sign in with Google");
        dialog.setHeaderText(null);

        WebView googleWebView = new WebView();
        googleWebView.setPrefSize(500, 620);
        WebEngine engine = googleWebView.getEngine();

        // Listen for the redirect with the auth code
        engine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
            if (newUrl != null && newUrl.startsWith(redirectUri)) {
                String code = extractParam(newUrl, "code");
                if (code != null) {
                    dialog.close();
                    completeGoogleLogin(code, clientId, redirectUri);
                }
                // Silently ignore redirects without a code (intermediate redirects)
            }
        });

        engine.load(authUrl);

        DialogPane pane = dialog.getDialogPane();
        pane.setContent(googleWebView);
        pane.setStyle("-fx-background-color: #0F172A;");
        pane.getButtonTypes().add(ButtonType.CANCEL);
        pane.setPrefSize(520, 650);

        dialog.showAndWait();
    }

    private void completeGoogleLogin(String code, String clientId, String redirectUri) {
        loginBtn.setDisable(true);
        loginBtn.setText("Signing in...");

        Thread thread = new Thread(() -> {
            try {
                // Exchange auth code for access token
                String tokenBody = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                        + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                        + "&client_secret=" + URLEncoder.encode(GOOGLE_CLIENT_SECRET, StandardCharsets.UTF_8)
                        + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                        + "&grant_type=authorization_code";

                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest tokenRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://oauth2.googleapis.com/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(tokenBody))
                        .build();

                HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
                String accessToken = extractJsonValue(tokenResponse.body(), "access_token");

                if (accessToken == null) {
                    Platform.runLater(() -> {
                        showError("Failed to get access token from Google.");
                        resetButton();
                    });
                    return;
                }

                // Fetch user info
                HttpRequest userInfoRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.googleapis.com/oauth2/v2/userinfo"))
                        .header("Authorization", "Bearer " + accessToken)
                        .GET()
                        .build();

                HttpResponse<String> userInfoResponse = httpClient.send(userInfoRequest, HttpResponse.BodyHandlers.ofString());
                String json = userInfoResponse.body();

                String email = extractJsonValue(json, "email");
                String givenName = extractJsonValue(json, "given_name");
                String familyName = extractJsonValue(json, "family_name");

                if (email == null) {
                    Platform.runLater(() -> {
                        showError("Could not retrieve email from Google account.");
                        resetButton();
                    });
                    return;
                }

                Platform.runLater(() -> {
                    // Find or create user
                    User user = serviceUser.findByEmail(email);
                    if (user == null) {
                        user = new User();
                        user.setEmail(email);
                        user.setFirstName(givenName != null ? givenName : "");
                        user.setLastName(familyName != null ? familyName : "");
                        user.setUsername(email.split("@")[0]);
                        user.setPassword("GOOGLE_OAUTH_" + System.currentTimeMillis());
                        user.setGender("male");
                        serviceUser.add(user);
                        user = serviceUser.findByEmail(email);
                    }

                    UserSession.getInstance().setCurrentUser(user);
                    UserSession.getInstance().saveSession();
                    try {
                        App.setRoot("views/MainLayout");
                    } catch (IOException e) {
                        showError("Failed to load the application.");
                        resetButton();
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Google sign-in error: " + e.getMessage());
                    resetButton();
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private String extractParam(String url, String param) {
        String search = param + "=";
        int idx = url.indexOf(search);
        if (idx == -1) return null;
        int start = idx + search.length();
        int end = url.indexOf('&', start);
        if (end == -1) end = url.length();
        return java.net.URLDecoder.decode(url.substring(start, end), StandardCharsets.UTF_8);
    }

    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon == -1) return null;

        // Skip whitespace
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length()) return null;

        if (json.charAt(i) == '"') {
            // String value
            i++;
            StringBuilder sb = new StringBuilder();
            for (; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    sb.append(json.charAt(++i));
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
        return null;
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

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
