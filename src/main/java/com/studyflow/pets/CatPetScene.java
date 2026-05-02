package com.studyflow.pets;

import com.studyflow.utils.GlbLoader;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public class CatPetScene {
    private static final String CAT_GLB_PATH = "/com/studyflow/pets/cat.glb";

    private final SubScene subScene;
    private final Group previewGroup = new Group();

    public CatPetScene(double width, double height) {
        Group root = new Group();
        root.getChildren().add(previewGroup);

        AmbientLight ambient = new AmbientLight(Color.rgb(90, 90, 95));

        PointLight keyLight = new PointLight(Color.rgb(255, 245, 215));
        keyLight.setTranslateX(-220);
        keyLight.setTranslateY(-280);
        keyLight.setTranslateZ(-380);

        PointLight fillLight = new PointLight(Color.rgb(140, 160, 210));
        fillLight.setTranslateX(280);
        fillLight.setTranslateY(-80);
        fillLight.setTranslateZ(-200);

        PointLight rimLight = new PointLight(Color.rgb(200, 175, 130));
        rimLight.setTranslateX(0);
        rimLight.setTranslateY(220);
        rimLight.setTranslateZ(320);

        root.getChildren().addAll(ambient, keyLight, fillLight, rimLight);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-420);
        camera.setNearClip(0.1);
        camera.setFarClip(5000.0);
        camera.setFieldOfView(42.0);

        subScene = new SubScene(root, width, height, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);
        subScene.setCamera(camera);

        loadModel();
    }

    public SubScene getSubScene() {
        return subScene;
    }

    public void resize(double width, double height) {
        subScene.setWidth(width);
        subScene.setHeight(height);
    }

    private void loadModel() {
        Thread loader = new Thread(() -> {
            Node previewNode;
            try {
                Group loaded = GlbLoader.loadFromResource(CAT_GLB_PATH);
                applyCatTransforms(loaded);
                previewNode = loaded;
            } catch (Throwable ex) {
                previewNode = fallbackNode();
            }

            Node finalPreviewNode = previewNode;
            Platform.runLater(() -> {
                previewGroup.getChildren().setAll(finalPreviewNode);
                centerModel(finalPreviewNode);
            });
        }, "cat-glb-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void applyCatTransforms(Group loaded) {
        // Order matters in JavaFX: transforms applied first-to-last.
        // 1. Scale up first, 2. Fix GLTF Y-up → JavaFX Y-down (-90° on X), 3. Face camera (180° on Y)
        loaded.getTransforms().setAll(
                new Scale(200, 200, 200),
                new Rotate(-90, Rotate.X_AXIS),
                new Rotate(180, Rotate.Y_AXIS)
        );
    }

    private void centerModel(Node node) {
        Bounds bounds = node.getBoundsInParent();
        if (bounds == null || bounds.isEmpty()) {
            return;
        }
        // Center X and Z, shift Y so cat sits in the middle of the view
        double centerX = (bounds.getMinX() + bounds.getMaxX()) / 2.0;
        double centerY = (bounds.getMinY() + bounds.getMaxY()) / 2.0;
        double centerZ = (bounds.getMinZ() + bounds.getMaxZ()) / 2.0;
        node.getTransforms().add(new Translate(-centerX, -centerY, -centerZ));
    }

    private Node fallbackNode() {
        Box box = new Box(140, 110, 180);
        box.setMaterial(new javafx.scene.paint.PhongMaterial(Color.web("#e8a664")));
        return box;
    }
}
