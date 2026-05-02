package com.studyflow.pets;

import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;

public class CatPetScene {
    private final SubScene subScene;
    private final CatPet3D cat;

    private double lastMouseX;
    private double lastMouseY;

    public CatPetScene(double width, double height) {
        this(width, height, new CatPet3D());
    }

    public CatPetScene(double width, double height, CatPet3D cat) {
        this.cat = cat;
        this.subScene = buildSubScene(width, height);
    }

    private SubScene buildSubScene(double width, double height) {
        Rotate rotateX = new Rotate(18, Rotate.X_AXIS);
        Rotate rotateY = new Rotate(-20, Rotate.Y_AXIS);
        cat.getTransforms().addAll(rotateX, rotateY);

        AmbientLight ambient = new AmbientLight(Color.rgb(55, 55, 60));

        PointLight keyLight = new PointLight(Color.rgb(255, 245, 215));
        keyLight.setTranslateX(-220);
        keyLight.setTranslateY(-280);
        keyLight.setTranslateZ(-380);

        PointLight fillLight = new PointLight(Color.rgb(140, 160, 210, 0.65));
        fillLight.setTranslateX(280);
        fillLight.setTranslateY(-80);
        fillLight.setTranslateZ(-200);

        PointLight rimLight = new PointLight(Color.rgb(200, 175, 130, 0.45));
        rimLight.setTranslateX(0);
        rimLight.setTranslateY(220);
        rimLight.setTranslateZ(320);

        Group root = new Group(ambient, keyLight, fillLight, rimLight, cat);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-420);
        camera.setNearClip(0.1);
        camera.setFarClip(2000.0);
        camera.setFieldOfView(42.0);

        SubScene scene = new SubScene(root, width, height, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.TRANSPARENT);
        scene.setCamera(camera);

        scene.setOnMousePressed(event -> {
            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
        });

        scene.setOnMouseDragged(event -> {
            double dx = event.getSceneX() - lastMouseX;
            double dy = event.getSceneY() - lastMouseY;
            rotateY.setAngle(rotateY.getAngle() + dx * 0.45);
            rotateX.setAngle(rotateX.getAngle() + dy * 0.45);
            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
        });

        scene.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                rotateX.setAngle(18);
                rotateY.setAngle(-20);
            }
        });

        return scene;
    }

    public SubScene getSubScene() {
        return subScene;
    }

    public CatPet3D getCat() {
        return cat;
    }

    public void resize(double width, double height) {
        subScene.setWidth(width);
        subScene.setHeight(height);
    }
}
