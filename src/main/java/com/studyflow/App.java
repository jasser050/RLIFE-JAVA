package com.studyflow;

import com.studyflow.api.WellbeingRecommendationsApiServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

/**
 * StudyFlow - Modern Student Productivity Dashboard
 * A beautiful, elegant JavaFX application with dark theme
 */
public class App extends Application {

    private static Scene scene;
    private static Stage primaryStage;
    private static final WellbeingRecommendationsApiServer recommendationsApiServer = new WellbeingRecommendationsApiServer();

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        // Load the main layout
        Parent root = loadFXML("views/Landing");

        // Create scene with dark background
        scene = new Scene(root, 1400, 900);
        scene.setFill(Color.TRANSPARENT);

        // Load stylesheets
        scene.getStylesheets().add(getClass().getResource("styles/dark-theme.css").toExternalForm());

        // Configure stage - UNDECORATED for custom title bar
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("StudyFlow - Student Dashboard");
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(700);

        // Show the stage
        stage.show();

        // Start local recommendations API
        if (!recommendationsApiServer.start(8085)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText("API Startup Warning");
            alert.setContentText("Recommendations API could not start on port 8085.\nThe UI is running normally.");
            alert.show();
        }
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
        launch(args);
    }

    @Override
    public void stop() {
        recommendationsApiServer.stop();
    }
}
