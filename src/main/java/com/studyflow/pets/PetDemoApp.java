package com.studyflow.pets;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PetDemoApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        CatPetScene petScene = new CatPetScene(420, 460);

        Label title = new Label("Mon Chat 3D");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #5a3a1a;");

        Label hint = new Label("Glissez pour faire tourner. Double-clic pour reinitialiser.");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #999999;");

        VBox root = new VBox(10, title, petScene.getSubScene(), hint);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #fdf6ee, #f0e0c8);");

        Scene scene = new Scene(root, 460, 560);
        primaryStage.setTitle("JavaFX 3D Cat Pet");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
