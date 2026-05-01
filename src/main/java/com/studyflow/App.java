package com.studyflow;

import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import com.studyflow.utils.UserSession;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        LocalServer.start();

        // Check for saved session — auto-login if token exists
        String startView = "views/Landing";
        String savedEmail = UserSession.getInstance().loadSession();
        if (savedEmail != null) {
            try {
                ServiceUser serviceUser = new ServiceUser();
                User user = serviceUser.findByEmail(savedEmail);
                if (user != null && !user.isBanned()) {
                    UserSession.getInstance().setCurrentUser(user);
                    startView = "views/MainLayout";
                } else {
                    UserSession.getInstance().clearSession();
                }
            } catch (Exception e) {
                System.out.println("Auto-login failed: " + e.getMessage());
                UserSession.getInstance().clearSession();
            }
        }

        Parent root = loadFXML(startView);

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
        // Enable hardware acceleration and WebGL for WebView (Spline + Three.js)
        System.setProperty("prism.forceGPU", "true");
        System.setProperty("prism.vsync", "false");
        launch(args);
    }
}
