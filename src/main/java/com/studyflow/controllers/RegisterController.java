package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.LocalServer;
import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import com.studyflow.utils.AvatarCard;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    // Title bar
    @FXML private Button themeBtn;
    @FXML private Label titleBarSubtitle;


    // Step indicator circles and connectors
    @FXML private StackPane stepCircle1, stepCircle2, stepCircle3, stepCircle4;
    @FXML private StackPane connector1, connector2, connector3;
    @FXML private Label stepLabel1, stepLabel2, stepLabel3, stepLabel4;

    // Step panels
    @FXML private VBox step1Panel, step2Panel, step3Panel, step4Panel;

    // Step 1 fields
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    // Step 2 fields
    @FXML private TextField phoneField;
    @FXML private TextField universityField;
    @FXML private TextField studentIdField;
    @FXML private RadioButton genderMale;
    @FXML private RadioButton genderFemale;

    // Step 3 - Avatar
    @FXML private FlowPane avatarContainer;
    @FXML private HBox avatarPreviewBox;
    @FXML private Label avatarPreviewIcon;
    @FXML private Label avatarPreviewName;

    private final List<AvatarCard> avatarCards = new ArrayList<>();

    // Step 4 - Face ID
    @FXML private WebView faceIdView;

    // Shared
    @FXML private Label errorLabel;
    @FXML private Button registerBtn;

    private final ServiceUser serviceUser = new ServiceUser();
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean lightMode = false;

    private int currentStep = 1;
    private String selectedAvatar = null;
    private boolean faceIdDone = false;

    private static final String[] STEP_TITLES = {
        "Create Account", "Preferences", "Choose Avatar", "Face ID"
    };
    private static final String[] AVATAR_EMOJIS = {
        "👨", "🧑", "👦", "🧑‍💻", "🧑‍🎓", "🧑‍🔬", "🧑‍🎨", "🧑‍⚕️",
        "👩", "👩‍💻", "👩‍🎓"
    };
    private static final String[] AVATAR_NAMES = {
        "Classic", "Explorer", "Scholar", "Coder", "Graduate",
        "Scientist", "Creative", "Medic", "Classic", "Tech Lady", "Scholar"
    };
    private static final String[] AVATAR_KEYS = {
        "male-avatar", "male-avatar1", "male-avatar2", "male-avatar3",
        "male-avatar4", "male-avatar5", "male-avatar6", "male-avatar7",
        "female-avatar", "female-avatar2", "female-avatar3"
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Gender toggle group
        ToggleGroup genderGroup = new ToggleGroup();
        genderMale.setToggleGroup(genderGroup);
        genderFemale.setToggleGroup(genderGroup);
        genderMale.setSelected(true);

        // Build native JavaFX AvatarCards for each .glb file
        for (int i = 0; i < AVATAR_KEYS.length; i++) {
            final String key  = AVATAR_KEYS[i];
            final String name = AVATAR_NAMES[i];
            final String gen  = key.startsWith("female") ? "female" : "male";
            AvatarCard card = new AvatarCard(key, name, gen, () -> onAvatarSelected(key));
            avatarCards.add(card);
            avatarContainer.getChildren().add(card);
        }

        // Load Face ID via local HTTP server
        WebEngine faceEngine = faceIdView.getEngine();
        faceEngine.setJavaScriptEnabled(true);
        faceEngine.load(LocalServer.url("/views/face-id.html"));
        faceEngine.setOnAlert(event -> {
            String msg = event.getData();
            if (msg != null && msg.startsWith("faceid:")) {
                Platform.runLater(() -> {
                    faceIdDone = true;
                    if (msg.equals("faceid:captured")) {
                        registerBtn.setText("Create Account  ✓");
                    } else {
                        registerBtn.setText("Create Account →");
                    }
                });
            }
        });

        showStep(1);
    }

    private void onAvatarSelected(String avatarKey) {
        selectedAvatar = avatarKey;

        // Deselect all cards, then select the chosen one
        for (AvatarCard c : avatarCards) {
            c.setSelected(c.getAvatarKey().equals(avatarKey));
        }

        // Find name for the selected avatar
        String name = avatarKey;
        String emoji = "👤";
        for (int i = 0; i < AVATAR_KEYS.length; i++) {
            if (AVATAR_KEYS[i].equals(avatarKey)) {
                emoji = AVATAR_EMOJIS[i];
                name = AVATAR_NAMES[i];
                break;
            }
        }

        avatarPreviewIcon.setText(emoji);
        avatarPreviewName.setText(name);
        avatarPreviewBox.setVisible(true);
        avatarPreviewBox.setManaged(true);
    }

    @FXML
    private void nextStep() {
        hideError();
        if (currentStep == 1) {
            if (!validateStep1()) return;
        }
        if (currentStep < 4) {
            showStep(currentStep + 1);
        }
    }

    @FXML
    private void prevStep() {
        if (currentStep > 1) {
            showStep(currentStep - 1);
        }
    }

    private void showStep(int step) {
        currentStep = step;

        // Update title bar
        titleBarSubtitle.setText(STEP_TITLES[step - 1]);

        // Hide all panels
        setPanel(step1Panel, step == 1);
        setPanel(step2Panel, step == 2);
        setPanel(step3Panel, step == 3);
        setPanel(step4Panel, step == 4);

        // Update step indicator circles
        updateStepIndicator(stepCircle1, stepLabel1, step, 1);
        updateStepIndicator(stepCircle2, stepLabel2, step, 2);
        updateStepIndicator(stepCircle3, stepLabel3, step, 3);
        updateStepIndicator(stepCircle4, stepLabel4, step, 4);

        // Update connectors
        updateConnector(connector1, step > 1);
        updateConnector(connector2, step > 2);
        updateConnector(connector3, step > 3);
    }

    private void setPanel(VBox panel, boolean show) {
        panel.setVisible(show);
        panel.setManaged(show);
    }

    private void updateStepIndicator(StackPane circle, Label label, int currentStep, int thisStep) {
        circle.getStyleClass().removeAll("wizard-step-active", "wizard-step-done");
        label.getStyleClass().removeAll("wizard-step-label-active", "wizard-step-label-done");

        if (currentStep == thisStep) {
            circle.getStyleClass().add("wizard-step-active");
            label.getStyleClass().add("wizard-step-label-active");
        } else if (currentStep > thisStep) {
            circle.getStyleClass().add("wizard-step-done");
            label.getStyleClass().add("wizard-step-label-done");
            // Set check icon
            if (!circle.getChildren().isEmpty() && circle.getChildren().get(0) instanceof Label) {
                ((Label) circle.getChildren().get(0)).setText("✓");
            }
        } else {
            // Reset text back to number
            if (!circle.getChildren().isEmpty() && circle.getChildren().get(0) instanceof Label) {
                ((Label) circle.getChildren().get(0)).setText(String.valueOf(thisStep));
            }
        }
    }

    private void updateConnector(StackPane connector, boolean active) {
        connector.getStyleClass().removeAll("wizard-connector-active");
        if (active) connector.getStyleClass().add("wizard-connector-active");
    }

    private boolean validateStep1() {
        String firstName = firstNameField.getText().trim();
        String lastName  = lastNameField.getText().trim();
        String username  = usernameField.getText().trim();
        String email     = emailField.getText().trim();
        String password  = passwordField.getText();
        String confirm   = confirmPasswordField.getText();

        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all required fields.");
            return false;
        }
        if (username.length() < 3 || username.length() > 20) {
            showError("Username must be between 3 and 20 characters.");
            return false;
        }
        if (!email.contains("@")) {
            showError("Please enter a valid email address.");
            return false;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return false;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return false;
        }
        if (serviceUser.findByEmail(email) != null) {
            showError("An account with this email already exists.");
            return false;
        }
        return true;
    }

    @FXML
    private void handleRegister() {
        hideError();

        registerBtn.setDisable(true);
        registerBtn.setText("Creating account...");

        User user = new User();
        user.setFirstName(firstNameField.getText().trim());
        user.setLastName(lastNameField.getText().trim());
        user.setUsername(usernameField.getText().trim());
        user.setEmail(emailField.getText().trim());
        user.setPassword(passwordField.getText());
        user.setPhoneNumber(phoneField.getText().trim());
        user.setUniversity(universityField.getText().trim());
        user.setStudentId(studentIdField.getText().trim());
        user.setGender(genderMale.isSelected() ? "male" : "female");
        if (selectedAvatar != null) {
            user.setProfilePic(selectedAvatar);
        }

        try {
            serviceUser.add(user);
            App.setRoot("views/Login");
        } catch (RuntimeException e) {
            showError(e.getMessage());
            registerBtn.setDisable(false);
            registerBtn.setText("Create Account  ✓");
        } catch (IOException e) {
            showError("Account created! Please go to Sign In.");
            registerBtn.setDisable(false);
            registerBtn.setText("Create Account  ✓");
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
    private void goToLogin() throws IOException { App.setRoot("views/Login"); }

    @FXML
    private void goToLanding() throws IOException { App.setRoot("views/Landing"); }

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
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
