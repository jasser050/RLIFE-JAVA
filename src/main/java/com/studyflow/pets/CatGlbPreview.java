package com.studyflow.pets;

import com.studyflow.utils.GlbLoader;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public final class CatGlbPreview {

    private static final String CAT_RESOURCE = "/com/studyflow/pets/cat.glb";
    private static final double FIT_SCALE = 1.8;
    private static final double SCENE_DEPTH = 3.2;

    private CatGlbPreview() {
    }

    public static Node create(double width, double height) {
        StackPane container = new StackPane();
        container.setAlignment(Pos.CENTER);
        container.setPrefSize(width, height);
        container.setMinSize(width, height);
        container.setMaxSize(width, height);

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
        camera.setFieldOfView(40);
        camera.setNearClip(0.01);
        camera.setFarClip(200);
        camera.setTranslateZ(-SCENE_DEPTH);

        SubScene subScene = new SubScene(world, width, height, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);
        subScene.setCamera(camera);
        subScene.widthProperty().bind(container.widthProperty());
        subScene.heightProperty().bind(container.heightProperty());
        subScene.setManaged(false);

        container.getChildren().add(subScene);

        loadModelAsync(modelGroup, container, width, height);
        return container;
    }

    private static void loadModelAsync(Group modelGroup, StackPane container, double width, double height) {
        Thread loader = new Thread(() -> {
            try {
                Group loaded = GlbLoader.loadFromResource(CAT_RESOURCE);
                Platform.runLater(() -> {
                    fitModel(loaded, modelGroup);
                    startRotation(modelGroup);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    modelGroup.getChildren().clear();
                    CatPetScene fallback = new CatPetScene(width, height);
                    container.getChildren().setAll(fallback.getSubScene());
                });
            }
        }, "cat-glb-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private static void fitModel(Group loaded, Group modelGroup) {
        if (loaded.getChildren().isEmpty()) return;

        modelGroup.getChildren().add(loaded);

        Bounds b = loaded.getBoundsInLocal();
        if (b.isEmpty() || b.getWidth() < 1e-6) return;

        double cx = (b.getMinX() + b.getMaxX()) / 2.0;
        double cy = (b.getMinY() + b.getMaxY()) / 2.0;
        double cz = (b.getMinZ() + b.getMaxZ()) / 2.0;
        double maxDim = Math.max(b.getWidth(), Math.max(b.getHeight(), b.getDepth()));
        double fit = FIT_SCALE / maxDim;

        loaded.getTransforms().clear();
        loaded.getTransforms().add(new Rotate(-90, Rotate.X_AXIS));
        loaded.getTransforms().add(new Scale(fit, fit, fit));
        loaded.getTransforms().add(new Translate(-cx, -cy, -cz));
    }

    private static void startRotation(Group modelGroup) {
        Rotate rot = new Rotate(0, Rotate.Y_AXIS);
        modelGroup.getTransforms().add(rot);

        AnimationTimer rotator = new AnimationTimer() {
            private long last = 0;
            @Override public void handle(long now) {
                if (last != 0) {
                    rot.setAngle(rot.getAngle() + (now - last) * 1e-9 * 55.0);
                }
                last = now;
            }
        };
        rotator.start();
    }
}