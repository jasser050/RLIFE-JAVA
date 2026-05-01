package com.studyflow.pets;

import com.studyflow.utils.GlbLoader;
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
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public final class CatGlbPreview {
    private static final String CAT_RESOURCE = "/com/studyflow/pets/cat.glb";
    private static final double DEFAULT_ROTATE_X = -8;
    private static final double DEFAULT_ROTATE_Y = 18;
    private static final double BASE_MODEL_ROTATE_X = 90;
    private static final double BASE_MODEL_ROTATE_Y = 180;
    private static final double MODEL_OFFSET_Y = 0.18;
    private static final double FIT_SIZE = 2.15;

    private CatGlbPreview() {
    }

    public static Node create(double width, double height) {
        StackPane container = new StackPane();
        container.setAlignment(Pos.CENTER);
        container.setPrefSize(width, height);
        container.setMinSize(width, height);
        container.setMaxSize(width, height);

        Group modelRoot = new Group();
        modelRoot.setTranslateY(MODEL_OFFSET_Y);
        Rotate rotateX = new Rotate(DEFAULT_ROTATE_X, Rotate.X_AXIS);
        Rotate rotateY = new Rotate(DEFAULT_ROTATE_Y, Rotate.Y_AXIS);
        modelRoot.getTransforms().addAll(rotateX, rotateY);

        AmbientLight ambient = new AmbientLight(Color.rgb(70, 68, 74));

        PointLight keyLight = new PointLight(Color.rgb(255, 244, 220));
        keyLight.setTranslateX(-260);
        keyLight.setTranslateY(-220);
        keyLight.setTranslateZ(-420);

        PointLight fillLight = new PointLight(Color.rgb(162, 180, 220, 0.70));
        fillLight.setTranslateX(250);
        fillLight.setTranslateY(-90);
        fillLight.setTranslateZ(-190);

        PointLight rimLight = new PointLight(Color.rgb(214, 188, 146, 0.55));
        rimLight.setTranslateX(0);
        rimLight.setTranslateY(180);
        rimLight.setTranslateZ(300);

        Group world = new Group(ambient, keyLight, fillLight, rimLight, modelRoot);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-6.4);
        camera.setTranslateY(-0.10);
        camera.setNearClip(0.01);
        camera.setFarClip(500);
        camera.setFieldOfView(30);

        SubScene subScene = new SubScene(world, width, height, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);
        subScene.setCamera(camera);
        subScene.widthProperty().bind(container.widthProperty());
        subScene.heightProperty().bind(container.heightProperty());
        subScene.setManaged(false);

        container.getChildren().add(subScene);
        installMouseRotation(subScene, rotateX, rotateY);
        loadModelAsync(modelRoot, container, width, height);
        return container;
    }

    private static void installMouseRotation(SubScene subScene, Rotate rotateX, Rotate rotateY) {
        final double[] last = new double[2];

        subScene.setOnMousePressed(event -> {
            last[0] = event.getSceneX();
            last[1] = event.getSceneY();
        });

        subScene.setOnMouseDragged(event -> {
            double dx = event.getSceneX() - last[0];
            double dy = event.getSceneY() - last[1];
            rotateY.setAngle(rotateY.getAngle() + dx * 0.40);
            rotateX.setAngle(rotateX.getAngle() + dy * 0.30);
            last[0] = event.getSceneX();
            last[1] = event.getSceneY();
        });

        subScene.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                rotateX.setAngle(DEFAULT_ROTATE_X);
                rotateY.setAngle(DEFAULT_ROTATE_Y);
            }
        });
    }

    private static void loadModelAsync(Group modelRoot, StackPane container, double width, double height) {
        Thread loader = new Thread(() -> {
            try {
                Group loaded = GlbLoader.loadFromResource(CAT_RESOURCE);
                Platform.runLater(() -> fitModel(loaded, modelRoot));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    modelRoot.getChildren().clear();
                    CatPetScene fallback = new CatPetScene(width, height);
                    container.getChildren().setAll(fallback.getSubScene());
                });
            }
        }, "cat-glb-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private static void fitModel(Group loaded, Group modelRoot) {
        modelRoot.getChildren().setAll(loaded);

        Bounds bounds = loaded.getBoundsInLocal();
        if (bounds.isEmpty()) {
            return;
        }

        double centerX = (bounds.getMinX() + bounds.getMaxX()) / 2.0;
        double centerY = (bounds.getMinY() + bounds.getMaxY()) / 2.0;
        double centerZ = (bounds.getMinZ() + bounds.getMaxZ()) / 2.0;
        double maxDim = Math.max(bounds.getWidth(), Math.max(bounds.getHeight(), bounds.getDepth()));
        if (maxDim <= 1e-6) {
            return;
        }

        double fitScale = FIT_SIZE / maxDim;
        loaded.getTransforms().setAll(
                new Translate(-centerX, -centerY, -centerZ),
                new Rotate(BASE_MODEL_ROTATE_X, Rotate.X_AXIS),
                new Rotate(BASE_MODEL_ROTATE_Y, Rotate.Y_AXIS),
                new Scale(fitScale, fitScale, fitScale)
        );
    }
}
