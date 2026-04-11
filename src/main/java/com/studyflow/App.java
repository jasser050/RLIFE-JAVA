package com.studyflow;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;

/**
 * StudyFlow - Modern Student Productivity Dashboard
 * A beautiful, elegant JavaFX application with dark theme
 */
public class App extends Application {

    private static Scene scene;
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        // Load the main layout
        Parent root = loadFXML("views/Landing");

        // Create scene with dark background
        scene = new Scene(root, 1400, 900);
        scene.setFill(Color.TRANSPARENT);

        // Load stylesheets
        scene.getStylesheets().add(getClass().getResource("/com/studyflow/styles/dark-theme.css").toExternalForm());

        // Configure stage - UNDECORATED for custom title bar
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("StudyFlow - Student Dashboard");
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(700);

        // Show the stage
        stage.show();
    }

    /**
     * Load an FXML file using an absolute classpath path.
     */
    public static Parent loadFXML(String fxml) throws IOException {
        String path = "/com/studyflow/" + fxml + ".fxml";
        URL resource = App.class.getResource(path);
        if (resource == null) {
            throw new IOException("FXML not found on classpath: " + path);
        }
        FXMLLoader fxmlLoader = new FXMLLoader(resource);
        return fxmlLoader.load();
    }

    /**
     * Set the root of the scene
     */
    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    /**
     * Get the primary stage
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Get the current scene
     */
    public static Scene getScene() {
        return scene;
    }

    public static void main(String[] args) {
        launch(args);
    }
}