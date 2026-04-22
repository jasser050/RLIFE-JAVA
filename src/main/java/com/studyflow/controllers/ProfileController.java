package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import com.studyflow.utils.AvatarCard;
import com.studyflow.utils.GlbLoader;
import com.studyflow.utils.UserSession;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private StackPane avatarContainer;
    @FXML private Label avatarInitials;
    @FXML private Label profileName;
    @FXML private Label profileBio;
    @FXML private Label profileEmail;
    @FXML private Label profileUsername;
    @FXML private Label coinsLabel;
    @FXML private Label saveStatus;
    @FXML private Label saveStatusBottom;

    @FXML private StackPane avatarPickerOverlay;
    @FXML private FlowPane profileAvatarContainer;

    private final List<AvatarCard> avatarCards = new ArrayList<>();
    private String selectedAvatar;
    private String currentAvatar;
    private AnimationTimer currentModelRotator = null;
    private AnimationTimer currentAvatarRotator = null;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField usernameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private ToggleButton genderMaleBtn;
    @FXML private ToggleButton genderFemaleBtn;
    @FXML private TextArea bioField;
    @FXML private Label bioCharCount;

    @FXML private TextField universityField;
    @FXML private TextField studentIdField;

    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Button themeLightBtn;
    @FXML private Button themeDarkBtn;
    @FXML private Button themeAutoBtn;
    @FXML private Label fontSizeLabel;
    @FXML private Slider fontSizeSlider;
    @FXML private ColorPicker accentColorPicker;
    @FXML private Label accentColorLabel;
    @FXML private CheckBox reduceMotionToggle;
    @FXML private CheckBox highContrastToggle;

    // Font family
    @FXML private Button fontSegoe;
    @FXML private Button fontInter;
    @FXML private Button fontJetbrains;
    @FXML private Button fontGeorgia;
    @FXML private Button fontCascadia;
    @FXML private Label fontFamilyLabel;

    // Accent buttons
    @FXML private Button btnDefault;
    @FXML private Button btnViolet;
    @FXML private Button btnIndigo;
    @FXML private Button btnBlue;
    @FXML private Button btnCyan;
    @FXML private Button btnGreen;
    @FXML private Button btnOrange;
    @FXML private Button btnPink;
    @FXML private Button btnRed;

    private boolean isLightMode = false;
    private String currentAccentHex = null; // null = default (no override)
    private int currentFontSizePx = 14;
    private String currentFontFamily = null; // null = default
    private Button activeAccentBtn;
    private Button activeFontBtn;
    private String dynamicCssPath;

    private final ServiceUser serviceUser = new ServiceUser();
    private User currentUser;
    private AnimationTimer avatarRotator;

    private static final int BIO_MAX_CHARS = 280;

    // Font family mappings
    private static final String[][] FONTS = {
        {"Segoe UI", "Segoe UI"},
        {"Inter", "Inter"},
        {"JetBrains Mono", "JetBrains Mono"},
        {"Georgia", "Georgia"},
        {"Cascadia Code", "Cascadia Code"}
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Gender toggle group
        ToggleGroup genderGroup = new ToggleGroup();
        genderMaleBtn.setToggleGroup(genderGroup);
        genderFemaleBtn.setToggleGroup(genderGroup);
        genderGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) oldVal.setSelected(true);
        });

        currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            populateFields(currentUser);
        }

        // Bio character counter
        bioField.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal == null ? 0 : newVal.length();
            bioCharCount.setText(len + " / " + BIO_MAX_CHARS);
            if (len > BIO_MAX_CHARS) {
                bioCharCount.setStyle("-fx-text-fill: #EF4444;");
                bioField.setText(newVal.substring(0, BIO_MAX_CHARS));
            } else if (len > BIO_MAX_CHARS * 0.85) {
                bioCharCount.setStyle("-fx-text-fill: #F59E0B;");
            } else {
                bioCharCount.setStyle("-fx-text-fill: #64748B;");
            }
        });
        bioCharCount.setText((bioField.getText() == null ? 0 : bioField.getText().length()) + " / " + BIO_MAX_CHARS);

        // Font size slider — dynamic
        fontSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int val = newVal.intValue();
            fontSizeSlider.setValue(val);
            String[] names = {"Small", "Compact", "Normal", "Large", "Extra Large"};
            int[] pxSizes = {12, 13, 14, 15, 17};
            if (val >= 1 && val <= 5) {
                fontSizeLabel.setText(names[val - 1]);
                currentFontSizePx = pxSizes[val - 1];
                rebuildDynamicCss();
            }
        });

        // Default states
        activeAccentBtn = btnDefault;
        markAccentActive(btnDefault);
        accentColorPicker.setValue(Color.web("#8B5CF6"));

        activeFontBtn = fontSegoe;
        markFontActive(fontSegoe);
    }

    // ============================================
    // UNIFIED DYNAMIC CSS — rebuilds one stylesheet
    // for accent + font size + font family
    // ============================================

    private void rebuildDynamicCss() {
        StringBuilder css = new StringBuilder();
        css.append("/* StudyFlow dynamic overrides */\n");

        // --- Font size override (always, overrides .root) ---
        css.append(".root {\n");
        css.append("    -fx-font-size: ").append(currentFontSizePx).append("px;\n");
        if (currentFontFamily != null) {
            css.append("    -fx-font-family: \"").append(currentFontFamily).append("\";\n");
        }
        css.append("}\n");

        // --- Accent color overrides (only if non-default) ---
        if (currentAccentHex != null) {
            Color c = Color.web(currentAccentHex);
            String r = String.valueOf((int)(c.getRed() * 255));
            String g = String.valueOf((int)(c.getGreen() * 255));
            String b = String.valueOf((int)(c.getBlue() * 255));
            String rgb = r + "," + g + "," + b;

            Color lighter = c.deriveColor(0, 0.85, 1.25, 1.0);
            String lHex = String.format("#%02X%02X%02X",
                    (int)(lighter.getRed()*255), (int)(lighter.getGreen()*255), (int)(lighter.getBlue()*255));
            String hex = currentAccentHex;

            css.append(".auth-btn-primary, .auth-btn-primary-sm {\n")
               .append("    -fx-background-color: ").append(hex).append(";\n}\n");
            css.append(".auth-btn-primary:hover, .auth-btn-primary-sm:hover {\n")
               .append("    -fx-background-color: ").append(lHex).append(";\n}\n");
            css.append(".btn-primary {\n")
               .append("    -fx-background-color: ").append(hex).append(";\n}\n");
            css.append(".btn-primary:hover {\n")
               .append("    -fx-background-color: ").append(lHex).append(";\n}\n");
            css.append(".nav-button.active {\n")
               .append("    -fx-background-color: rgba(").append(rgb).append(", 0.15);\n")
               .append("    -fx-text-fill: ").append(lHex).append(";\n}\n");
            css.append(".nav-button.active .ikonli-font-icon {\n")
               .append("    -fx-icon-color: ").append(hex).append(";\n}\n");
            css.append(".nav-button:hover {\n")
               .append("    -fx-background-color: rgba(").append(rgb).append(", 0.08);\n}\n");
            css.append(".theme-btn.active {\n")
               .append("    -fx-background-color: rgba(").append(rgb).append(", 0.12);\n")
               .append("    -fx-border-color: ").append(hex).append(";\n}\n");
            css.append(".theme-btn.active .theme-icon-circle {\n")
               .append("    -fx-background-color: ").append(hex).append(";\n}\n");
            css.append(".font-size-preview {\n")
               .append("    -fx-text-fill: ").append(hex).append(";\n")
               .append("    -fx-background-color: rgba(").append(rgb).append(", 0.15);\n}\n");
            css.append(".profile-avatar-ring {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(lHex).append(", ").append(hex).append(", derive(").append(hex).append(", -30%));\n")
               .append("    -fx-effect: dropshadow(gaussian, rgba(").append(rgb).append(", 0.6), 20, 0, 0, 0);\n}\n");
            css.append(".profile-avatar-text {\n")
               .append("    -fx-text-fill: ").append(lHex).append(";\n}\n");
            css.append(".profile-hero-banner {\n")
               .append("    -fx-border-color: rgba(").append(rgb).append(", 0.2);\n")
               .append("    -fx-effect: dropshadow(gaussian, rgba(").append(rgb).append(", 0.25), 30, 0, 0, 8);\n}\n");
            css.append(".profile-glow-orb {\n")
               .append("    -fx-background-color: radial-gradient(center 50% 50%, radius 50%, rgba(").append(rgb).append(", 0.4) 0%, transparent 70%);\n}\n");
            css.append(".profile-grad-card.profile-grad-purple {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(hex).append(", derive(").append(hex).append(", -25%));\n}\n");
            css.append(".auth-link {\n")
               .append("    -fx-text-fill: ").append(lHex).append(";\n}\n");
            css.append(".toggle-icon {\n")
               .append("    -fx-background-color: linear-gradient(to bottom right, ").append(lHex).append(", ").append(hex).append(");\n}\n");
            css.append(".check-box:selected .box {\n")
               .append("    -fx-background-color: ").append(hex).append(";\n")
               .append("    -fx-border-color: ").append(hex).append(";\n}\n");
            css.append(".toggle-switch:selected .box {\n")
               .append("    -fx-background-color: ").append(hex).append(";\n")
               .append("    -fx-border-color: ").append(lHex).append(";\n}\n");
            css.append(".gender-card:selected {\n")
               .append("    -fx-background-color: rgba(").append(rgb).append(", 0.12);\n")
               .append("    -fx-border-color: ").append(hex).append(";\n}\n");
            css.append(".font-size-slider .thumb {\n")
               .append("    -fx-background-color: ").append(hex).append(";\n")
               .append("    -fx-effect: dropshadow(gaussian, rgba(").append(rgb).append(", 0.5), 8, 0, 0, 2);\n}\n");
            css.append(".font-size-slider .track {\n")
               .append("    -fx-background-color: rgba(").append(rgb).append(", 0.25);\n}\n");
            css.append(".font-family-btn.font-active {\n")
               .append("    -fx-background-color: rgba(").append(rgb).append(", 0.12);\n")
               .append("    -fx-border-color: ").append(hex).append(";\n}\n");
            css.append(".bio-textarea:focused {\n")
               .append("    -fx-border-color: ").append(hex).append(";\n}\n");
            css.append(".auth-field:focused {\n")
               .append("    -fx-border-color: ").append(hex).append(";\n}\n");
        }

        applyDynamicStylesheet(css.toString());
    }

    private void applyDynamicStylesheet(String css) {
        try {
            if (dynamicCssPath != null) {
                App.getScene().getStylesheets().removeIf(s -> s.contains("studyflow-dynamic"));
            }
            File tmpFile = File.createTempFile("studyflow-dynamic", ".css");
            tmpFile.deleteOnExit();
            dynamicCssPath = tmpFile.toURI().toString();
            try (FileWriter fw = new FileWriter(tmpFile)) {
                fw.write(css);
            }
            App.getScene().getStylesheets().add(dynamicCssPath);
        } catch (IOException e) {
            System.err.println("Failed to apply dynamic CSS: " + e.getMessage());
        }
    }

    // ============================================
    // ACCENT COLOR
    // ============================================

    private void applyAccentColor(String hex, String name) {
        currentAccentHex = hex;
        accentColorLabel.setText(name);
        if (hex != null) {
            accentColorPicker.setValue(Color.web(hex));
        }
        rebuildDynamicCss();
    }

    private void markAccentActive(Button btn) {
        if (activeAccentBtn != null) {
            activeAccentBtn.getStyleClass().remove("accent-active");
        }
        activeAccentBtn = btn;
        btn.getStyleClass().add("accent-active");
    }

    @FXML
    private void setAccentDefault() {
        markAccentActive(btnDefault);
        applyAccentColor(null, "Default");
        accentColorPicker.setValue(Color.web("#8B5CF6"));
    }
    @FXML
    private void setAccentViolet() { markAccentActive(btnViolet); applyAccentColor("#8B5CF6", "Violet"); }
    @FXML
    private void setAccentIndigo() { markAccentActive(btnIndigo); applyAccentColor("#6366F1", "Indigo"); }
    @FXML
    private void setAccentBlue() { markAccentActive(btnBlue); applyAccentColor("#3B82F6", "Blue"); }
    @FXML
    private void setAccentCyan() { markAccentActive(btnCyan); applyAccentColor("#06B6D4", "Cyan"); }
    @FXML
    private void setAccentGreen() { markAccentActive(btnGreen); applyAccentColor("#10B981", "Green"); }
    @FXML
    private void setAccentOrange() { markAccentActive(btnOrange); applyAccentColor("#F97316", "Orange"); }
    @FXML
    private void setAccentPink() { markAccentActive(btnPink); applyAccentColor("#EC4899", "Pink"); }
    @FXML
    private void setAccentRed() { markAccentActive(btnRed); applyAccentColor("#EF4444", "Red"); }

    @FXML
    private void changeAccentColor() {
        Color c = accentColorPicker.getValue();
        String hex = String.format("#%02X%02X%02X",
                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
        if (activeAccentBtn != null) activeAccentBtn.getStyleClass().remove("accent-active");
        activeAccentBtn = null;
        accentColorLabel.setText("Custom");
        currentAccentHex = hex;
        rebuildDynamicCss();
    }

    // ============================================
    // FONT FAMILY
    // ============================================

    private void markFontActive(Button btn) {
        if (activeFontBtn != null) {
            activeFontBtn.getStyleClass().remove("font-active");
        }
        activeFontBtn = btn;
        btn.getStyleClass().add("font-active");
    }

    private void applyFontFamily(String family, String displayName, Button btn) {
        markFontActive(btn);
        currentFontFamily = family;
        fontFamilyLabel.setText(displayName);
        rebuildDynamicCss();
    }

    @FXML
    private void setFontSegoe() { applyFontFamily(null, "Segoe UI", fontSegoe); currentFontFamily = null; rebuildDynamicCss(); }
    @FXML
    private void setFontInter() { applyFontFamily("Inter", "Inter", fontInter); }
    @FXML
    private void setFontJetbrains() { applyFontFamily("JetBrains Mono", "JetBrains", fontJetbrains); }
    @FXML
    private void setFontGeorgia() { applyFontFamily("Georgia", "Georgia", fontGeorgia); }
    @FXML
    private void setFontCascadia() { applyFontFamily("Cascadia Code", "Cascadia", fontCascadia); }

    // ============================================
    // AVATAR 3D
    // ============================================

    private void loadAvatar3D(String avatarKey) {
        String path = "/com/studyflow/avatars/" + avatarKey + ".glb";
        if (GlbLoader.class.getResourceAsStream(path) == null) return;

        Thread loader = new Thread(() -> {
            try {
                Group loaded = GlbLoader.loadFromResource(path);
                javafx.application.Platform.runLater(() -> {
                    double sceneSize = 134;
                    Group modelGroup = new Group();
                    Group world = new Group();
                    world.getChildren().add(modelGroup);

                    AmbientLight ambient = new AmbientLight(Color.color(0.5, 0.47, 0.55));
                    PointLight key = new PointLight(Color.color(0.9, 0.85, 0.95));
                    key.setTranslateX(3); key.setTranslateY(-4); key.setTranslateZ(5);
                    PointLight fill = new PointLight(Color.color(0.4, 0.38, 0.65));
                    fill.setTranslateX(-3); fill.setTranslateY(2); fill.setTranslateZ(-3);
                    world.getChildren().addAll(ambient, key, fill);

                    PerspectiveCamera camera = new PerspectiveCamera(true);
                    camera.setFieldOfView(38);
                    camera.setNearClip(0.01);
                    camera.setFarClip(200);
                    camera.setTranslateZ(-3.2);

                    SubScene sub = new SubScene(world, sceneSize, sceneSize, true, SceneAntialiasing.BALANCED);
                    sub.setCamera(camera);
                    sub.setFill(Color.TRANSPARENT);

                    modelGroup.getChildren().add(loaded);
                    Bounds b = loaded.getBoundsInLocal();
                    if (!b.isEmpty() && b.getWidth() > 1e-6) {
                        double cx = (b.getMinX() + b.getMaxX()) / 2.0;
                        double cy = (b.getMinY() + b.getMaxY()) / 2.0;
                        double cz = (b.getMinZ() + b.getMaxZ()) / 2.0;
                        double maxDim = Math.max(b.getWidth(), Math.max(b.getHeight(), b.getDepth()));
                        double fit = 1.8 / maxDim;
                        loaded.getTransforms().clear();
                        loaded.getTransforms().add(new Scale(fit, fit, fit));
                        loaded.getTransforms().add(new Translate(-cx, -cy, -cz));
                    }

                    Rotate rot = new Rotate(0, Rotate.Y_AXIS);
                    modelGroup.getTransforms().add(rot);
                    if (avatarRotator != null) avatarRotator.stop();
                    avatarRotator = new AnimationTimer() {
                        private long last = 0;
                        @Override public void handle(long now) {
                            if (last != 0) rot.setAngle(rot.getAngle() + (now - last) * 1e-9 * 35.0);
                            last = now;
                        }
                    };
                    avatarRotator.start();

                    avatarInitials.setVisible(false);
                    avatarInitials.setManaged(false);
                    avatarContainer.getChildren().clear();
                    StackPane innerBg = new StackPane();
                    innerBg.getStyleClass().add("profile-avatar-inner");
                    innerBg.getChildren().add(sub);
                    avatarContainer.getChildren().add(innerBg);

                    sub.setOpacity(0);
                    sub.setScaleX(0.7);
                    sub.setScaleY(0.7);
                    FadeTransition fade = new FadeTransition(Duration.millis(600), sub);
                    fade.setFromValue(0); fade.setToValue(1);
                    ScaleTransition scale = new ScaleTransition(Duration.millis(600), sub);
                    scale.setFromX(0.7); scale.setFromY(0.7);
                    scale.setToX(1.0); scale.setToY(1.0);
                    fade.play(); scale.play();
                });
            } catch (Exception e) {
                System.err.println("Profile avatar load failed: " + e.getMessage());
            }
        }, "profile-avatar-loader");
        loader.setDaemon(true);
        loader.start();
    }

    // ============================================
    // POPULATE & SAVE
    // ============================================

    private void populateFields(User user) {
        String pic = user.getProfilePic();
        currentAvatar = pic; // Store current avatar for picker
        if (pic != null && !pic.isEmpty() && pic.contains("avatar")) {
            loadAvatar3D(pic);
        } else {
            avatarInitials.setText(user.getInitials().isEmpty() ? "??" : user.getInitials());
        }
        profileName.setText(user.getFullName().trim());
        profileEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        profileUsername.setText("@" + (user.getUsername() != null ? user.getUsername() : ""));
        coinsLabel.setText(String.valueOf(user.getCoins()));
        
        // Display bio in header
        String userBio = user.getBio();
        if (userBio != null && !userBio.isEmpty()) {
            profileBio.setText(userBio);
            profileBio.setVisible(true);
            profileBio.setManaged(true);
        }

        firstNameField.setText(orEmpty(user.getFirstName()));
        lastNameField.setText(orEmpty(user.getLastName()));
        usernameField.setText(orEmpty(user.getUsername()));
        phoneField.setText(orEmpty(user.getPhoneNumber()));
        emailField.setText(orEmpty(user.getEmail()));
        bioField.setText(orEmpty(user.getBio()));
        universityField.setText(orEmpty(user.getUniversity()));
        studentIdField.setText(orEmpty(user.getStudentId()));

        if ("female".equalsIgnoreCase(user.getGender())) {
            genderFemaleBtn.setSelected(true);
        } else {
            genderMaleBtn.setSelected(true);
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
        currentUser.setGender(genderMaleBtn.isSelected() ? "male" : "female");
        
        // Save avatar if changed
        if (selectedAvatar != null && !selectedAvatar.isEmpty()) {
            currentUser.setProfilePic(selectedAvatar);
        }

        serviceUser.update(currentUser);

        profileName.setText(currentUser.getFullName().trim());
        profileUsername.setText("@" + currentUser.getUsername());
        
        // Update bio display
        String userBio = currentUser.getBio();
        if (userBio != null && !userBio.isEmpty()) {
            profileBio.setText(userBio);
            profileBio.setVisible(true);
            profileBio.setManaged(true);
        } else {
            profileBio.setVisible(false);
            profileBio.setManaged(false);
        }

        newPasswordField.clear();
        confirmPasswordField.clear();

        showStatus("Changes saved successfully!", true);
        
        // Reset selected avatar after saving
        selectedAvatar = null;
    }

    @FXML
    private void handleDeleteAccount() {
        if (currentUser == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Account");
        confirm.setHeaderText("Are you sure you want to delete your account?");
        confirm.setContentText("This action cannot be undone. All your data will be permanently removed.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceUser.delete(currentUser);
            UserSession.getInstance().logout();
            try {
                App.setRoot("views/Login");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        // Update top badge
        saveStatus.setText(msg);
        saveStatus.getStyleClass().removeAll("success", "danger");
        saveStatus.getStyleClass().add(success ? "success" : "danger");
        saveStatus.setVisible(true);
        saveStatus.setManaged(true);
        
        // Update bottom label
        saveStatusBottom.setText(msg);
        saveStatusBottom.getStyleClass().removeAll("success", "danger");
        saveStatusBottom.getStyleClass().add(success ? "success" : "danger");
        saveStatusBottom.setVisible(true);
        saveStatusBottom.setManaged(true);
        
        // Auto-hide after 3 seconds
        Thread timerThread = new Thread(() -> {
            try {
                Thread.sleep(3000);
                javafx.application.Platform.runLater(() -> {
                    saveStatus.setVisible(false);
                    saveStatus.setManaged(false);
                    saveStatusBottom.setVisible(false);
                    saveStatusBottom.setManaged(false);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        timerThread.setDaemon(true);
        timerThread.start();
    }

    private String orEmpty(String val) {
        return val != null ? val : "";
    }

    // ============================================
    // THEME
    // ============================================

    @FXML
    private void setThemeLight() {
        isLightMode = true;
        updateThemeButtons();
        applyTheme(true);
    }

    @FXML
    private void setThemeDark() {
        isLightMode = false;
        updateThemeButtons();
        applyTheme(false);
    }

    @FXML
    private void setThemeAuto() {
        updateThemeButtons();
    }

    private void updateThemeButtons() {
        themeLightBtn.getStyleClass().remove("active");
        themeDarkBtn.getStyleClass().remove("active");
        themeAutoBtn.getStyleClass().remove("active");

        if (isLightMode) {
            themeLightBtn.getStyleClass().add("active");
        } else {
            themeDarkBtn.getStyleClass().add("active");
        }
    }

    private void applyTheme(boolean light) {
        javafx.scene.Scene scene = themeLightBtn.getScene();
        URL lightUrl = getClass().getResource("/com/studyflow/styles/auth-light.css");
        if (lightUrl == null) return;
        String lightCss = lightUrl.toExternalForm();

        if (light) {
            if (!scene.getStylesheets().contains(lightCss)) {
                scene.getStylesheets().add(lightCss);
            }
        } else {
            scene.getStylesheets().remove(lightCss);
        }
    }

    // ============================================
    // AVATAR PICKER
    // ============================================

    private static final String[] AVATAR_KEYS = {
        "male-avatar", "male-avatar1", "male-avatar2", "male-avatar3",
        "male-avatar4", "male-avatar5", "male-avatar6", "male-avatar7",
        "female-avatar", "female-avatar2", "female-avatar3"
    };
    private static final String[] AVATAR_NAMES = {
        "Student", "Athlete", "Artist", "Scientist", "Creative", "Medic", "Classic", "Tech Lady", "Scholar", "Leader", "Gamer"
    };

    @FXML
    private void showAvatarPicker() {
        // Load avatar cards one by one with delay to avoid memory issues
        if (profileAvatarContainer.getChildren().isEmpty()) {
            loadAvatarCardsWithDelay();
        }
        
        // Show overlay
        avatarPickerOverlay.setVisible(true);
        avatarPickerOverlay.setManaged(true);
    }
    
    private void loadAvatarCardsWithDelay() {
        // Load first few avatars immediately
        int count = Math.min(4, AVATAR_KEYS.length);
        for (int i = 0; i < count; i++) {
            addAvatarCard(i);
        }
        
        // Load remaining after a delay
        if (AVATAR_KEYS.length > count) {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    javafx.application.Platform.runLater(() -> {
                        for (int i = count; i < AVATAR_KEYS.length; i++) {
                            addAvatarCard(i);
                        }
                    });
                } catch (InterruptedException e) {}
            }).start();
        }
    }
    
    private void addAvatarCard(int i) {
        final String key = AVATAR_KEYS[i];
        final String name = AVATAR_NAMES[i];
        final String gen = key.startsWith("female") ? "female" : "male";
        
        AvatarCard card = new AvatarCard(key, name, gen, () -> {
            javafx.application.Platform.runLater(() -> onProfileAvatarSelected(key));
        });
        avatarCards.add(card);
        profileAvatarContainer.getChildren().add(card);
    }

    @FXML
    private void hideAvatarPicker() {
        avatarPickerOverlay.setVisible(false);
        avatarPickerOverlay.setManaged(false);
    }

    private void onProfileAvatarSelected(String avatarKey) {
        selectedAvatar = avatarKey;
        
        // Update UI to show selected
        for (AvatarCard c : avatarCards) {
            c.setSelected(c.getAvatarKey().equals(avatarKey));
        }
        
        // Immediately load new avatar (direct change, no transition delay)
        avatarContainer.getChildren().clear();
        loadAvatar3D(avatarKey);
        
        // Hide picker after selection
        hideAvatarPicker();
    }
}
