package com.studyflow.utils;

import javafx.animation.AnimationTimer;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.control.Label;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

/**
 * A JavaFX card that displays a GLB avatar in a native 3D SubScene.
 * No WebView, no WebGL — pure JavaFX 3D.
 * Clicking the card calls the provided onSelect callback.
 */
public class AvatarCard extends StackPane {

    private static final double CARD_W = 118;
    private static final double CARD_H = 156;
    private static final double SCENE_W = 100;
    private static final double SCENE_H = 114;

    private boolean selected = false;
    private final String avatarKey;
    private AnimationTimer rotator;

    public AvatarCard(String avatarKey, String displayName, String gender, Runnable onSelect) {
        this.avatarKey = avatarKey;

        setPrefSize(CARD_W, CARD_H);
        setMaxSize(CARD_W, CARD_H);
        getStyleClass().add("av-card");

        // ── 3D SubScene ──────────────────────────────────────────────
        Group modelGroup = new Group();
        Group world      = new Group();
        world.getChildren().add(modelGroup);

        // Lighting — balanced: vivid colors without washing out
        AmbientLight ambient = new AmbientLight(Color.color(0.5, 0.47, 0.55));
        PointLight key = new PointLight(Color.color(0.9, 0.85, 0.95));
        key.setTranslateX(3); key.setTranslateY(-4); key.setTranslateZ(5);
        PointLight fill = new PointLight(Color.color(0.4, 0.38, 0.65));
        fill.setTranslateX(-3); fill.setTranslateY(2); fill.setTranslateZ(-3);
        world.getChildren().addAll(ambient, key, fill);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(40);
        camera.setNearClip(0.01);
        camera.setFarClip(200);
        camera.setTranslateZ(-3.2);

        SubScene sub = new SubScene(world, SCENE_W, SCENE_H, true, SceneAntialiasing.BALANCED);
        sub.setCamera(camera);
        sub.setFill(Color.TRANSPARENT);

        // Load GLB in background thread, then update JavaFX on FX thread
        Thread loader = new Thread(() -> {
            try {
                Group loaded = GlbLoader.loadFromResource(
                    "/com/studyflow/avatars/" + avatarKey + ".glb"
                );
                javafx.application.Platform.runLater(() -> {
                    fitModel(loaded, modelGroup);
                    startRotation(modelGroup);
                });
            } catch (Exception e) {
                System.err.println("GLB LOAD FAILED [" + avatarKey + "]: " + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    // Fallback: coloured sphere
                    Sphere sphere = new Sphere(0.55);
                    PhongMaterial m = new PhongMaterial(
                        gender.equals("female")
                            ? Color.color(0.85, 0.35, 0.6)
                            : Color.color(0.45, 0.38, 0.92)
                    );
                    m.setSpecularColor(Color.WHITE);
                    m.setSpecularPower(40);
                    sphere.setMaterial(m);
                    modelGroup.getChildren().add(sphere);
                    startRotation(modelGroup);
                });
            }
        }, "glb-loader-" + avatarKey);
        loader.setDaemon(true);
        loader.start();

        // ── Labels ───────────────────────────────────────────────────
        Label nameLabel = new Label(displayName);
        nameLabel.getStyleClass().add("av-card-name");

        Label genderPill = new Label(gender.toUpperCase());
        genderPill.getStyleClass().addAll("av-card-pill",
            gender.equals("female") ? "av-pill-female" : "av-pill-male");

        VBox labels = new VBox(4, nameLabel, genderPill);
        labels.setAlignment(Pos.CENTER);

        // Check badge (shown when selected)
        Label checkBadge = new Label("✓");
        checkBadge.getStyleClass().add("av-card-check");
        checkBadge.setVisible(false);

        // Layout: 3D scene on top, labels at bottom
        VBox content = new VBox(2, sub, labels);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPickOnBounds(false);

        getChildren().addAll(content, checkBadge);
        StackPane.setAlignment(checkBadge, Pos.TOP_RIGHT);

        // ── Interaction ──────────────────────────────────────────────
        setOnMouseClicked(e -> {
            onSelect.run();
            setSelected(true);
        });
        setOnMouseEntered(e -> { if (!selected) pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("hover"), true); });
        setOnMouseExited(e  -> pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("hover"), false));

        // Store check badge ref for toggling
        getProperties().put("checkBadge", checkBadge);
    }

    public void setSelected(boolean sel) {
        this.selected = sel;
        Label check = (Label) getProperties().get("checkBadge");
        if (check != null) check.setVisible(sel);
        if (sel) {
            getStyleClass().add("av-card-selected");
        } else {
            getStyleClass().remove("av-card-selected");
        }
    }

    public String getAvatarKey() { return avatarKey; }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void fitModel(Group loaded, Group modelGroup) {
        if (loaded.getChildren().isEmpty()) return;

        // First add to get bounds computed
        modelGroup.getChildren().add(loaded);

        Bounds b = loaded.getBoundsInLocal();
        if (b.isEmpty() || b.getWidth() < 1e-6) return;

        double cx = (b.getMinX() + b.getMaxX()) / 2.0;
        double cy = (b.getMinY() + b.getMaxY()) / 2.0;
        double cz = (b.getMinZ() + b.getMaxZ()) / 2.0;
        double maxDim = Math.max(b.getWidth(), Math.max(b.getHeight(), b.getDepth()));
        double fit = 1.8 / maxDim;

        loaded.getTransforms().clear();
        loaded.getTransforms().add(new Scale(fit, fit, fit));
        loaded.getTransforms().add(new Translate(-cx, -cy, -cz));
    }

    private void startRotation(Group modelGroup) {
        Rotate rot = new Rotate(0, Rotate.Y_AXIS);
        modelGroup.getTransforms().add(rot);

        rotator = new AnimationTimer() {
            private long last = 0;
            @Override public void handle(long now) {
                if (last != 0) {
                    rot.setAngle(rot.getAngle() + (now - last) * 1e-9 * 55.0); // 55°/s
                }
                last = now;
            }
        };
        rotator.start();
    }
}
