package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
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
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private StackPane avatarContainer;
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
    private AnimationTimer avatarRotator;

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

                    // Bright lighting for vivid colors
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

                    // Fit model into view
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

                    // Slow rotation
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

                    // Hide initials, show 3D scene with fade-in
                    avatarInitials.setVisible(false);
                    avatarInitials.setManaged(false);

                    // Replace inner content — keep only the ring, put SubScene inside
                    avatarContainer.getChildren().clear();
                    StackPane innerBg = new StackPane();
                    innerBg.getStyleClass().add("profile-avatar-inner");
                    innerBg.getChildren().add(sub);
                    avatarContainer.getChildren().add(innerBg);

                    // Smooth entrance animation
                    sub.setOpacity(0);
                    sub.setScaleX(0.7);
                    sub.setScaleY(0.7);
                    FadeTransition fade = new FadeTransition(Duration.millis(600), sub);
                    fade.setFromValue(0);
                    fade.setToValue(1);
                    ScaleTransition scale = new ScaleTransition(Duration.millis(600), sub);
                    scale.setFromX(0.7); scale.setFromY(0.7);
                    scale.setToX(1.0); scale.setToY(1.0);
                    fade.play();
                    scale.play();
                });
            } catch (Exception e) {
                System.err.println("Profile avatar load failed: " + e.getMessage());
            }
        }, "profile-avatar-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void populateFields(User user) {
        // Hero card — try loading 3D GLB avatar, else show initials
        String pic = user.getProfilePic();
        if (pic != null && !pic.isEmpty() && pic.contains("avatar")) {
            loadAvatar3D(pic);
        } else {
            avatarInitials.setText(user.getInitials().isEmpty() ? "??" : user.getInitials());
        }
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
        profileName.setText(currentUser.getFullName().trim());
        profileUsername.setText("@" + currentUser.getUsername());

        newPasswordField.clear();
        confirmPasswordField.clear();

        showStatus("Changes saved successfully!", true);
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
