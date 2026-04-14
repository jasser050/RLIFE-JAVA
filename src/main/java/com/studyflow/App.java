package com.studyflow;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;

/**
 * StudyFlow - Modern Student Productivity Dashboard
 * A beautiful, elegant JavaFX application with dark theme
 */
public class App extends Application {

    public enum Theme {
        DARK,
        LIGHT
    }

    private static Scene scene;
    private static Stage primaryStage;
    private static Theme currentTheme = Theme.DARK;

    private static final String DARK_THEME_CSS = "styles/dark-theme.css";
    private static final String LIGHT_THEME_CSS = "styles/light-theme.css";

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        // Load the main layout
        Parent root = loadFXML("views/Landing");

        // Create scene with dark background
        scene = new Scene(root, 1400, 900);
        scene.setFill(Color.TRANSPARENT);

        applyTheme(currentTheme);

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

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static void toggleTheme() {
        if (currentTheme == Theme.DARK) {
            setTheme(Theme.LIGHT);
        } else {
            setTheme(Theme.DARK);
        }
    }

    public static void setTheme(Theme theme) {
        currentTheme = theme;
        applyTheme(theme);
    }

    private static void applyTheme(Theme theme) {
        if (scene == null) {
            return;
        }
        scene.getStylesheets().clear();
        String stylesheet = theme == Theme.LIGHT ? LIGHT_THEME_CSS : DARK_THEME_CSS;
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource(stylesheet)).toExternalForm());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
