package com.studyflow.controllers;

import com.studyflow.App;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class LandingController {

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    private void onDragStart(MouseEvent e) {
        xOffset = App.getPrimaryStage().getX() - e.getScreenX();
        yOffset = App.getPrimaryStage().getY() - e.getScreenY();
    }

    @FXML
    private void onDragged(MouseEvent e) {
        App.getPrimaryStage().setX(e.getScreenX() + xOffset);
        App.getPrimaryStage().setY(e.getScreenY() + yOffset);
    }

    @FXML
    private void goToLogin() throws IOException {
        App.setRoot("views/Login");
    }

    @FXML
    private void goToRegister() throws IOException {
        App.setRoot("views/Register");
    }

    @FXML
    private void minimizeWindow() {
        App.getPrimaryStage().setIconified(true);
    }

    @FXML
    private void maximizeWindow() {
        Stage stage = App.getPrimaryStage();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML
    private void closeWindow() {
        Platform.exit();
    }
}
