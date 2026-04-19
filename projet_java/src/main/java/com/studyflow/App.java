package com.studyflow;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

/**
 * StudyFlow - Modern Student Productivity Dashboard
 * A beautiful, elegant JavaFX application with dark theme
 */
public class App extends Application {
    private static final String DARK_THEME = "styles/dark-theme.css";
    private static final String LIGHT_THEME = "styles/light-theme.css";

    private static Scene scene;
    private static Stage primaryStage;
    private static boolean darkTheme = true;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        // Load the main layout
        Parent root = loadFXML("views/Landing");

        // Create scene with dark background
        scene = new Scene(root, 1400, 900);
        scene.setFill(Color.TRANSPARENT);

        // Load stylesheets
        applyTheme();

        // Configure stage - UNDECORATED for custom title bar
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("RLIFE");
        stage.setScene(scene);
        stage.getIcons().add(new Image(App.class.getResourceAsStream("/com/studyflow/assets/rlife-logo.png")));
        stage.setMinWidth(1200);
        stage.setMinHeight(700);

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

    public static void toggleTheme() {
        darkTheme = !darkTheme;
        applyTheme();
    }

    public static boolean isDarkTheme() {
        return darkTheme;
    }

    private static void applyTheme() {
        if (scene == null) {
            return;
        }
        scene.getStylesheets().clear();
        String stylesheet = darkTheme ? DARK_THEME : LIGHT_THEME;
        scene.getStylesheets().add(App.class.getResource(stylesheet).toExternalForm());
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
