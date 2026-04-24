package com.studyflow.controllers.admin;

import com.studyflow.models.User;
import com.studyflow.models.WellBeing;
import com.studyflow.services.ServiceUser;
import com.studyflow.services.ServiceWellBeing;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AdminStatsController implements Initializable {

    @FXML private Label usersCountLabel;
    @FXML private Label adminRateLabel;
    @FXML private Label avgSleepLabel;
    @FXML private Label avgEnergyLabel;
    @FXML private VBox highlightsBox;

    private final ServiceUser serviceUser = new ServiceUser();
    private final ServiceWellBeing serviceWellBeing = new ServiceWellBeing();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            List<User> users = serviceUser.getAll();
            List<WellBeing> entries = serviceWellBeing.getAll();
            long admins = users.stream().filter(User::isAdmin).count();
            double adminRate = users.isEmpty() ? 0 : (admins * 100.0) / users.size();
            double averageSleep = entries.stream().mapToDouble(WellBeing::getSleepHours).average().orElse(0);
            double averageEnergy = entries.stream().mapToInt(WellBeing::getEnergyLevel).average().orElse(0);

            usersCountLabel.setText(String.valueOf(users.size()));
            adminRateLabel.setText(String.format("%.0f%%", adminRate));
            avgSleepLabel.setText(String.format("%.1fh", averageSleep));
            avgEnergyLabel.setText(String.format("%.1f/10", averageEnergy));

            highlightsBox.getChildren().setAll(
                    AdminViewFactory.createTimelineCard("Adoption", users.isEmpty() ? "No users available yet." : "The platform currently stores " + users.size() + " accounts.", "Growth"),
                    AdminViewFactory.createTimelineCard("Operational signal", entries.isEmpty() ? "No wellbeing metrics collected yet." : "Average energy is " + String.format("%.1f/10", averageEnergy) + ".", "Energy"),
                    AdminViewFactory.createTimelineCard("Admin access", "Current admin share among users: " + String.format("%.0f%%", adminRate) + ".", "Security")
            );
        } catch (RuntimeException e) {
            usersCountLabel.setText("0");
            adminRateLabel.setText("0%");
            avgSleepLabel.setText("0.0h");
            avgEnergyLabel.setText("0.0/10");
            highlightsBox.getChildren().setAll(
                    AdminViewFactory.createTimelineCard("Statistics unavailable", "The admin stats view opened without database metrics.", "Fallback")
            );
        }
    }
}
