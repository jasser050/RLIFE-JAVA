package com.studyflow.utils;

import com.studyflow.pets.CatPetScene;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

public final class PetPreviewSupport {

    private static final double FIT_SCALE = 1.8;
    private static final double SCENE_DEPTH = 3.2;
    private static final double ROTATION_SPEED_DEGREES = 60.0;
    private static final double IDLE_TILT_AMPLITUDE_DEGREES = 5.0;
    private static final Duration IDLE_TILT_DURATION = Duration.seconds(1.8);
    private static final String PREVIEW_ANIMATIONS_KEY = "petPreviewAnimations";

    private PetPreviewSupport() {
    }

    public static Node createPreview(String type, double width, double height) {
        String normalized = PetUiSupport.normalizeType(type);
        if (hasGlbModel(normalized)) {
            return createGlbPreview(normalized, width, height);
        }

        ImageView imageView = new ImageView(PetUiSupport.loadPetImage(normalized));
        imageView.setFitWidth(width - 16);
        imageView.setFitHeight(height - 16);
        imageView.setPreserveRatio(true);
        return wrap(imageView, width, height);
    }

    private static boolean hasGlbModel(String type) {
        return type != null && (
            "cat".equals(type) || "dog".equals(type) || "hamster".equals(type) ||
            "panda".equals(type) || "dragon".equals(type) || "fox".equals(type) ||
            "bird".equals(type) || "rabbit".equals(type)
        );
    }

    private static String getGlbResource(String type) {
        return "/com/studyflow/pets/" + type + ".glb";
    }

    private static Node createGlbPreview(String type, double width, double height) {
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

        SubScene subScene = new SubScene(world, width, height, true, SceneAntialiasing.DISABLED);
        subScene.setFill(Color.TRANSPARENT);
        subScene.setCamera(camera);
        subScene.widthProperty().bind(container.widthProperty());
        subScene.heightProperty().bind(container.heightProperty());
        subScene.setManaged(false);

        container.getChildren().add(subScene);

        loadModelAsync(type, modelGroup, container, width, height);
        return container;
    }

    private static void loadModelAsync(String type, Group modelGroup, StackPane container, double width, double height) {
        Thread loader = new Thread(() -> {
            try {
                Group loaded = GlbLoader.loadFromResource(getGlbResource(type));
                Platform.runLater(() -> {
                    fitModel(loaded, modelGroup, type);
                    startRotation(container, modelGroup);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    modelGroup.getChildren().clear();
                    CatPetScene fallback = new CatPetScene(width, height);
                    container.getChildren().setAll(fallback.getSubScene());
                });
            }
        }, "pet-glb-loader-" + type);
        loader.setDaemon(true);
        loader.start();
    }

    private static double getRotationAngle(String type) {
        return switch (type) {
            case "panda" -> 0;
            case "rabbit" -> 0;
            default -> -90;
        };
    }

    private static void fitModel(Group loaded, Group modelGroup, String type) {
        if (loaded.getChildren().isEmpty()) return;

        modelGroup.getChildren().add(loaded);

        Bounds b = loaded.getBoundsInLocal();
        if (b.isEmpty() || b.getWidth() < 1e-6) return;

        double cx = (b.getMinX() + b.getMaxX()) / 2.0;
        double cy = (b.getMinY() + b.getMaxY()) / 2.0;
        double cz = (b.getMinZ() + b.getMaxZ()) / 2.0;
        double maxDim = Math.max(b.getWidth(), Math.max(b.getHeight(), b.getDepth()));
        double fit = FIT_SCALE / maxDim;
        double rotationAngle = getRotationAngle(type);

        loaded.getTransforms().clear();
        loaded.getTransforms().add(new Rotate(rotationAngle, Rotate.X_AXIS));
        loaded.getTransforms().add(new Scale(fit, fit, fit));
        loaded.getTransforms().add(new Translate(-cx, -cy, -cz));
    }

    private static void startRotation(StackPane container, Group modelGroup) {
        Rotate pitch = new Rotate(-8, Rotate.X_AXIS);
        modelGroup.getTransforms().add(pitch);

        RotateTransition yawAnimation = new RotateTransition(Duration.seconds(360.0 / ROTATION_SPEED_DEGREES), modelGroup);
        yawAnimation.setAxis(Rotate.Y_AXIS);
        yawAnimation.setByAngle(360.0);
        yawAnimation.setCycleCount(Animation.INDEFINITE);
        yawAnimation.setInterpolator(Interpolator.LINEAR);

        Timeline pitchAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(pitch.angleProperty(), -8 - IDLE_TILT_AMPLITUDE_DEGREES, Interpolator.EASE_BOTH)),
            new KeyFrame(IDLE_TILT_DURATION, new KeyValue(pitch.angleProperty(), -8 + IDLE_TILT_AMPLITUDE_DEGREES, Interpolator.EASE_BOTH))
        );
        pitchAnimation.setAutoReverse(true);
        pitchAnimation.setCycleCount(Animation.INDEFINITE);

        registerAnimationLifecycle(container, yawAnimation, pitchAnimation);
        yawAnimation.play();
        pitchAnimation.play();
    }

    private static void registerAnimationLifecycle(StackPane container, Animation... animations) {
        Object existing = container.getProperties().get(PREVIEW_ANIMATIONS_KEY);
        if (existing instanceof Animation[] previousAnimations) {
            for (Animation animation : previousAnimations) {
                animation.stop();
            }
        }
        container.getProperties().put(PREVIEW_ANIMATIONS_KEY, animations);
        container.sceneProperty().addListener((obs, oldScene, newScene) -> {
            for (Animation animation : animations) {
                if (newScene == null) {
                    animation.stop();
                } else {
                    animation.play();
                }
            }
        });
    }

    private static StackPane wrap(Node node, double width, double height) {
        StackPane pane = new StackPane(node);
        pane.setAlignment(Pos.CENTER);
        pane.setPrefSize(width, height);
        pane.setMinSize(width, height);
        pane.setMaxSize(width, height);
        if (node instanceof SubScene subScene) {
            subScene.widthProperty().bind(pane.widthProperty());
            subScene.heightProperty().bind(pane.heightProperty());
        }
        return pane;
    }
}
