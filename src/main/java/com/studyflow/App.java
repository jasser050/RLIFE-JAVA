package com.studyflow;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

/**
 * StudyFlow - Modern Student Productivity Dashboard.
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
        LocalServer.start();

        Parent root = loadFXML("views/Landing");
        scene = new Scene(root, 1400, 900);
        scene.setFill(Color.TRANSPARENT);
        applyTheme();

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("StudyFlow - Student Dashboard");
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.show();
    }

    public static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
        applyTheme();
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
        scene.getStylesheets().add(App.class.getResource(darkTheme ? DARK_THEME : LIGHT_THEME).toExternalForm());
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static Scene getScene() {
        return scene;
    }

    public static void main(String[] args) {
        System.setProperty("prism.forceGPU", "true");
        System.setProperty("prism.vsync", "false");
        launch(args);
    }
}
