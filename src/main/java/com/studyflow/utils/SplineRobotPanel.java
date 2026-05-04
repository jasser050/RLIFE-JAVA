package com.studyflow.utils;

import javafx.animation.AnimationTimer;
import javafx.geometry.Bounds;
import javafx.scene.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

/**
 * Displays the Spline robot GLB in a JavaFX 3D SubScene.
 * Uses the same loading approach as AvatarCard (fitModel auto-scale/center).
 * The robot smoothly follows the mouse cursor.
 */
public class SplineRobotPanel {

    private static final String ROBOT_RESOURCE = "/com/studyflow/avatars/spline-robot.glb";

    public static StackPane create() {
        StackPane container = new StackPane();
        container.setStyle("-fx-background-color: #080412;");

        // ── 3D scene setup (same pattern as AvatarCard) ──
        Group modelGroup = new Group();
        Group world = new Group();
        world.getChildren().add(modelGroup);

        // Lighting — purple/pink dramatic look
        AmbientLight ambient = new AmbientLight(Color.color(0.15, 0.1, 0.2));
        world.getChildren().add(ambient);

        // Key light — purple from front-above
        PointLight keyLight = new PointLight(Color.color(0.7, 0.55, 0.95));
        keyLight.setTranslateX(0);
        keyLight.setTranslateY(-5);
        keyLight.setTranslateZ(-8);
        world.getChildren().add(keyLight);

        // Pink/magenta uplight from below
        PointLight underLight = new PointLight(Color.color(0.85, 0.15, 0.55));
        underLight.setTranslateX(0);
        underLight.setTranslateY(6);
        underLight.setTranslateZ(-2);
        world.getChildren().add(underLight);

        // Side fill — left purple glow
        PointLight leftFill = new PointLight(Color.color(0.5, 0.2, 0.7));
        leftFill.setTranslateX(-6);
        leftFill.setTranslateY(2);
        leftFill.setTranslateZ(-3);
        world.getChildren().add(leftFill);

        // Rim light — right side
        PointLight rimLight = new PointLight(Color.color(0.35, 0.2, 0.6));
        rimLight.setTranslateX(5);
        rimLight.setTranslateY(-1);
        rimLight.setTranslateZ(3);
        world.getChildren().add(rimLight);

        // Camera — same style as AvatarCard
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(35);
        camera.setNearClip(0.01);
        camera.setFarClip(500);
        camera.setTranslateZ(-5.5);
        camera.setTranslateY(-0.3);

        SubScene sub = new SubScene(world, 100, 100, true, SceneAntialiasing.BALANCED);
        sub.setCamera(camera);
        sub.setFill(Color.web("#080412"));

        // Bind SubScene size to fill the container
        sub.widthProperty().bind(container.widthProperty());
        sub.heightProperty().bind(container.heightProperty());

        container.getChildren().add(sub);

        // ── Rotation transforms for cursor following ──
        Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
        Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
        modelGroup.getTransforms().addAll(rotateY, rotateX);

        // ── Load GLB in background (same as AvatarCard) ──
        Thread loader = new Thread(() -> {
            try {
                Group loaded = GlbLoader.loadFromResource(ROBOT_RESOURCE);
                javafx.application.Platform.runLater(() -> {
                    fitModel(loaded, modelGroup);
                    setupCursorFollow(container, rotateY, rotateX);
                });
            } catch (Exception e) {
                System.err.println("[SplineRobot] GLB LOAD FAILED: " + e.getMessage());
                e.printStackTrace();
            }
        }, "spline-robot-loader");
        loader.setDaemon(true);
        loader.start();

        return container;
    }

    /**
     * Auto-center and auto-scale the model to fit nicely — same as AvatarCard.fitModel()
     * but with a larger fit size for the bigger panel.
     */
    private static void fitModel(Group loaded, Group modelGroup) {
        if (loaded.getChildren().isEmpty()) return;

        modelGroup.getChildren().add(loaded);

        Bounds b = loaded.getBoundsInLocal();
        if (b.isEmpty() || b.getWidth() < 1e-6) return;

        double cx = (b.getMinX() + b.getMaxX()) / 2.0;
        double cy = (b.getMinY() + b.getMaxY()) / 2.0;
        double cz = (b.getMinZ() + b.getMaxZ()) / 2.0;
        double maxDim = Math.max(b.getWidth(), Math.max(b.getHeight(), b.getDepth()));
        double fit = 3.0 / maxDim;  // larger than avatar cards (1.8) since panel is bigger

        loaded.getTransforms().clear();
        loaded.getTransforms().add(new Scale(fit, fit, fit));
        loaded.getTransforms().add(new Translate(-cx, -cy, -cz));
    }

    private static void setupCursorFollow(StackPane container, Rotate rotateY, Rotate rotateX) {
        final double[] targetYaw = {0};
        final double[] targetPitch = {0};
        final double[] currentYaw = {0};
        final double[] currentPitch = {0};
        final double MAX_YAW = 20;
        final double MAX_PITCH = 12;
        final double SMOOTHING = 0.06;

        container.setOnMouseMoved(e -> {
            double w = container.getWidth();
            double h = container.getHeight();
            if (w <= 0 || h <= 0) return;

            double nx = (e.getX() / w - 0.5) * 2.0;
            double ny = (e.getY() / h - 0.5) * 2.0;

            targetYaw[0] = nx * MAX_YAW;
            targetPitch[0] = -ny * MAX_PITCH;
        });

        container.setOnMouseExited(e -> {
            targetYaw[0] = 0;
            targetPitch[0] = 0;
        });

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                currentYaw[0] += (targetYaw[0] - currentYaw[0]) * SMOOTHING;
                currentPitch[0] += (targetPitch[0] - currentPitch[0]) * SMOOTHING;
                rotateY.setAngle(currentYaw[0]);
                rotateX.setAngle(currentPitch[0]);
            }
        };
        timer.start();

        container.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) timer.stop();
        });
    }
}
