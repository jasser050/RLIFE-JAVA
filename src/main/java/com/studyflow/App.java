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
import java.util.Objects;

/**
 * StudyFlow - Modern Student Productivity Dashboard.
 */
public class App extends Application {
    private static final String DARK_THEME = "styles/dark-theme.css";
    private static final String LIGHT_THEME = "styles/light-theme.css";

    public enum Theme {
        DARK,
        LIGHT
    }

    private static Scene scene;
    private static Stage primaryStage;
    private static boolean darkTheme = true;

    public enum Theme {
        DARK,
        LIGHT
    }

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
                    startView = "admin@rlife.com".equalsIgnoreCase(user.getEmail())
                            ? "views/AdminLayout" : "views/MainLayout";
                } else {
                    UserSession.getInstance().clearSession();
                }
            } catch (Exception e) {
                System.out.println("Auto-login failed: " + e.getMessage());
                UserSession.getInstance().clearSession();
            }
        }

        Parent root = loadFXML(startView);

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

    public static Theme getCurrentTheme() {
        return darkTheme ? Theme.DARK : Theme.LIGHT;
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
        System.setProperty("prism.forceGPU", "true");
        System.setProperty("prism.vsync", "false");
        launch(args);
    }
}
