package com.studyflow.controllers.admin;

import com.studyflow.models.User;
import com.studyflow.utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminSettingsController implements Initializable {

    @FXML private Label currentModeLabel;
    @FXML private Label routingRuleLabel;
    @FXML private Label adminEmailLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        currentModeLabel.setText("Back Office Active");
        routingRuleLabel.setText("Email suffix @rlife.com");
        adminEmailLabel.setText(currentUser != null ? currentUser.getEmail() : "No active session");
    }
}
