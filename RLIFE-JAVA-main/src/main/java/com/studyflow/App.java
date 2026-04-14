package com.studyflow;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

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
        LocalServer.start();

        // Load the main layout
        Parent root = loadFXML("views/Landing");

        // Use a standard decorated stage to avoid platform-specific issues
        // with transparent windows and custom window chrome.
        scene = new Scene(root, 1400, 900);

        // Load stylesheets
        scene.getStylesheets().add(getClass().getResource("styles/dark-theme.css").toExternalForm());

        stage.setTitle("StudyFlow - Student Dashboard");
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.centerOnScreen();

        // Show the stage
        stage.show();
    }

    /**
     * Load an FXML file
     */
    public static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
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
        // Keep rendering defaults for broader compatibility across Windows setups.
        System.setProperty("prism.vsync", "false");
        launch(args);
    }
}
