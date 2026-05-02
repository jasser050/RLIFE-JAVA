package com.studyflow.utils;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public final class PetPreviewSupport {
    private static final String PREVIEW_MARKER = "pet-preview-3d";
    private static final String PREVIEW_TIMER = "pet-preview-rotator";
    private static final String CAT_GLB_PATH = "/com/studyflow/pets/cat.glb";

    private PetPreviewSupport() {
    }

    public static void showPetPreview(ImageView imageView, String type) {
        if (imageView == null) {
            return;
        }

        Pane parent = imageView.getParent() instanceof Pane pane ? pane : null;
        if (parent == null) {
            imageView.setImage(PetUiSupport.loadPetImage(type));
            return;
        }

        clearPreview(parent);
        String normalizedType = PetUiSupport.previewType(type);
        if (!"cat".equals(normalizedType)) {
            imageView.setVisible(true);
            imageView.setManaged(true);
            imageView.setImage(PetUiSupport.loadPetImage(type));
            return;
        }

        imageView.setVisible(false);
        imageView.setManaged(false);

        double width = preferredSize(parent, imageView.getFitWidth(), 224);
        double height = preferredSize(parent, imageView.getFitHeight(), 224);
        SubScene subScene = createPreviewScene(width, height);
        subScene.getProperties().put(PREVIEW_MARKER, Boolean.TRUE);

        if (parent instanceof Region) {
            Region region = (Region) parent;
            region.setMinSize(width, height);
            region.setPrefSize(width, height);
            region.setMaxSize(width, height);
        }

        if (parent instanceof StackPane stackPane) {
            StackPane.setAlignment(subScene, Pos.CENTER);
            stackPane.getChildren().add(subScene);
        } else {
            parent.getChildren().add(subScene);
        }
    }

    private static SubScene createPreviewScene(double width, double height) {
        Group modelGroup = new Group();
        Group world = new Group();
        world.getChildren().add(modelGroup);

        AmbientLight ambient = new AmbientLight(Color.color(0.5, 0.47, 0.55));
        PointLight key = new PointLight(Color.color(0.9, 0.85, 0.95));
        key.setTranslateX(3);
        key.setTranslateY(-4);
        key.setTranslateZ(5);
        PointLight fill = new PointLight(Color.color(0.4, 0.38, 0.65));
        fill.setTranslateX(-3);
        fill.setTranslateY(2);
        fill.setTranslateZ(-3);
        world.getChildren().addAll(ambient, key, fill);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(38);
        camera.setNearClip(0.01);
        camera.setFarClip(200);
        camera.setTranslateZ(-3.2);

        SubScene subScene = new SubScene(world, width, height, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);
        subScene.setFill(Color.TRANSPARENT);

        Thread loader = new Thread(() -> {
            Group loadedRoot;
            try {
                Group loaded = GlbLoader.loadFromResource(CAT_GLB_PATH);
                loaded.getTransforms().setAll(
                        new Rotate(-90, Rotate.X_AXIS),
                        new Rotate(180, Rotate.Y_AXIS),
                        new Rotate(90, Rotate.Z_AXIS)
                );
                loadedRoot = new Group(loaded);
                fitModelToTarget(loadedRoot, 1.8);
                loadedRoot.getTransforms().add(new Translate(0, 0.18, 0));
            } catch (Throwable ex) {
                loadedRoot = createFallbackNode();
            }

            Group finalLoadedRoot = loadedRoot;
            Platform.runLater(() -> {
                modelGroup.getChildren().setAll(finalLoadedRoot);
                startRotation(modelGroup, subScene);
            });
        }, "pet-preview-loader");
        loader.setDaemon(true);
        loader.start();

        return subScene;
    }

    private static Group createFallbackNode() {
        Sphere sphere = new Sphere(0.55);
        sphere.setMaterial(new javafx.scene.paint.PhongMaterial(Color.color(0.85, 0.55, 0.4)));
        return new Group(sphere);
    }

    private static void fitModelToTarget(Group loaded, double targetSize) {
        Bounds bounds = loaded.getBoundsInLocal();
        if (bounds.isEmpty() || bounds.getWidth() <= 1e-6) {
            return;
        }
        double cx = (bounds.getMinX() + bounds.getMaxX()) / 2.0;
        double cy = (bounds.getMinY() + bounds.getMaxY()) / 2.0;
        double cz = (bounds.getMinZ() + bounds.getMaxZ()) / 2.0;
        double maxDim = Math.max(bounds.getWidth(), Math.max(bounds.getHeight(), bounds.getDepth()));
        double fit = targetSize / maxDim;
        loaded.getTransforms().add(new Scale(fit, fit, fit));
        loaded.getTransforms().add(new Translate(-cx, -cy, -cz));
    }

    private static void startRotation(Group modelGroup, SubScene subScene) {
        Rotate rot = new Rotate(0, Rotate.Y_AXIS);
        modelGroup.getTransforms().setAll(rot);

        AnimationTimer previous = (AnimationTimer) subScene.getProperties().get(PREVIEW_TIMER);
        if (previous != null) {
            previous.stop();
        }

        AnimationTimer rotator = new AnimationTimer() {
            private long last = 0L;

            @Override
            public void handle(long now) {
                if (last != 0L) {
                    rot.setAngle(rot.getAngle() + (now - last) * 1e-9 * 35.0);
                }
                last = now;
            }
        };
        subScene.getProperties().put(PREVIEW_TIMER, rotator);
        rotator.start();
    }

    private static void clearPreview(Pane parent) {
        parent.getChildren().removeIf(node -> {
            if (Boolean.TRUE.equals(node.getProperties().get(PREVIEW_MARKER))) {
                AnimationTimer rotator = (AnimationTimer) node.getProperties().get(PREVIEW_TIMER);
                if (rotator != null) {
                    rotator.stop();
                }
                return true;
            }
            return false;
        });
    }

    private static double preferredSize(Pane parent, double fallback, double defaultValue) {
        if (fallback > 0) {
            return fallback;
        }
        if (parent instanceof Region) {
            Region region = (Region) parent;
            if (region.getPrefWidth() > 0) {
                return region.getPrefWidth();
            }
            if (region.getPrefHeight() > 0) {
                return region.getPrefHeight();
            }
        }
        return defaultValue;
    }
}
