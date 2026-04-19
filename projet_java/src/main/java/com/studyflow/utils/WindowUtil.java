package com.studyflow.utils;

import com.studyflow.App;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public final class WindowUtil {
    private WindowUtil() {
    }

    public static Stage createModal(String title, Parent root, double width, double height) {
        Stage stage = new Stage();
        Scene scene = buildScene(root, width, height);
        stage.initOwner(App.getPrimaryStage());
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.setMinWidth(width);
        stage.setMinHeight(height);
        return stage;
    }

    public static Stage createWindow(String title, Parent root, double width, double height) {
        Stage stage = new Stage();
        Scene scene = buildScene(root, width, height);
        stage.initOwner(App.getPrimaryStage());
        stage.setTitle(title);
        stage.setScene(scene);
        stage.setMinWidth(width);
        stage.setMinHeight(height);
        return stage;
    }

    private static Scene buildScene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        String stylesheet = App.isDarkTheme()
                ? "/com/studyflow/styles/dark-theme.css"
                : "/com/studyflow/styles/light-theme.css";
        scene.getStylesheets().add(WindowUtil.class.getResource(stylesheet).toExternalForm());
        return scene;
    }
}
