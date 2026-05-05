package com.studyflow.utils;

import com.studyflow.LocalServer;
import javafx.animation.AnimationTimer;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class PetPreviewSupport {

    private static final double FIT_SCALE = 1.8;
    private static final double SCENE_DEPTH = 3.2;
    private static final double JCEF_MIN_SIZE = 180;

    private PetPreviewSupport() {
    }

    public static Node createPreview(String type, double width, double height) {
        String normalized = PetUiSupport.normalizeType(type);
        if (supportsScene3D() && hasGlbModel(normalized)) {
            return createGlbPreview(normalized, width, height);
        }
        if (shouldUseJcef(width, height) && hasBrowser3dSupport()) {
            return createJcefPreview(normalized, width, height);
        }
        if (hasBrowser3dSupport()) {
            return createBrowserPreview(normalized, width, height);
        }

        return createImagePreview(normalized, width, height);
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

    private static boolean supportsScene3D() {
        return Platform.isSupported(ConditionalFeature.SCENE3D);
    }

    private static boolean hasBrowser3dSupport() {
        return LocalServer.getPort() > 0;
    }

    private static boolean shouldUseJcef(double width, double height) {
        return width >= JCEF_MIN_SIZE && height >= JCEF_MIN_SIZE;
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

        SubScene subScene = new SubScene(world, width, height, true, SceneAntialiasing.BALANCED);
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
                    startRotation(modelGroup);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    modelGroup.getChildren().clear();
                    container.getChildren().setAll(createImagePreview(type, width, height));
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

    private static double getZRotationAngle(String type) {
        return switch (type) {
            default -> 0;
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
        double zRotationAngle = getZRotationAngle(type);

        loaded.getTransforms().clear();
        loaded.getTransforms().add(new Rotate(rotationAngle, Rotate.X_AXIS));
        if (Math.abs(zRotationAngle) > 1e-6) {
            loaded.getTransforms().add(new Rotate(zRotationAngle, Rotate.Z_AXIS));
        }
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

    private static Node createImagePreview(String type, double width, double height) {
        ImageView imageView = new ImageView(PetUiSupport.loadPetImage(type));
        imageView.setFitWidth(Math.max(1, width - 16));
        imageView.setFitHeight(Math.max(1, height - 16));
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        return wrap(imageView, width, height);
    }

    private static Node createJcefPreview(String type, double width, double height) {
        StackPane container = new StackPane();
        container.setAlignment(Pos.CENTER);
        container.setPrefSize(width, height);
        container.setMinSize(width, height);
        container.setMaxSize(width, height);

        SwingNode swingNode = new SwingNode();
        swingNode.setFocusTraversable(false);
        swingNode.resize(width, height);
        container.getChildren().add(swingNode);

        container.widthProperty().addListener((obs, oldValue, newValue) ->
                swingNode.resize(newValue.doubleValue(), container.getHeight()));
        container.heightProperty().addListener((obs, oldValue, newValue) ->
                swingNode.resize(container.getWidth(), newValue.doubleValue()));

        EmbeddedWebBrowser.attach(swingNode, browserPreviewUrl(type), message -> { })
                .exceptionally(ex -> {
                    Platform.runLater(() -> container.getChildren().setAll(createBrowserPreview(type, width, height)));
                    return null;
                });

        return container;
    }

    private static Node createBrowserPreview(String type, double width, double height) {
        StackPane container = new StackPane();
        container.setAlignment(Pos.CENTER);
        container.setPrefSize(width, height);
        container.setMinSize(width, height);
        container.setMaxSize(width, height);

        WebView webView = new WebView();
        webView.setContextMenuEnabled(false);
        webView.setFocusTraversable(false);
        webView.setMinSize(width, height);
        webView.setPrefSize(width, height);
        webView.setMaxSize(width, height);
        webView.setStyle("-fx-background-color: transparent;");

        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.FAILED || newState == javafx.concurrent.Worker.State.CANCELLED) {
                container.getChildren().setAll(createImagePreview(type, width, height));
            }
        });

        container.getChildren().add(webView);
        container.widthProperty().addListener((obs, oldValue, newValue) -> webView.setPrefWidth(newValue.doubleValue()));
        container.heightProperty().addListener((obs, oldValue, newValue) -> webView.setPrefHeight(newValue.doubleValue()));
        engine.load(browserPreviewUrl(type));
        return container;
    }

    private static String browserPreviewUrl(String type) {
        return LocalServer.url("/views/pet-preview.html")
                + "?type=" + URLEncoder.encode(type, StandardCharsets.UTF_8);
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
