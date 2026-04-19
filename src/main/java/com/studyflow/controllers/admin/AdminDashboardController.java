package com.studyflow.controllers.admin;

import com.studyflow.models.User;
import com.studyflow.models.WellBeing;
import com.studyflow.services.ServiceUser;
import com.studyflow.services.ServiceWellBeing;
import com.studyflow.utils.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private Label adminNameLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label adminUsersLabel;
    @FXML private Label wellbeingEntriesLabel;
    @FXML private Label averageStressLabel;
    @FXML private VBox recentActivityBox;

    private final ServiceUser serviceUser = new ServiceUser();
    private final ServiceWellBeing serviceWellBeing = new ServiceWellBeing();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            String fullName = currentUser.getFullName().trim();
            adminNameLabel.setText(fullName.isEmpty() ? currentUser.getUsername() : fullName);
        }

        try {
            List<User> users = serviceUser.getAll();
            List<WellBeing> entries = serviceWellBeing.getAll();
            long adminCount = users.stream().filter(User::isAdmin).count();
            double avgStress = entries.stream().mapToInt(WellBeing::getStressLevel).average().orElse(0);

            totalUsersLabel.setText(String.valueOf(users.size()));
            adminUsersLabel.setText(String.valueOf(adminCount));
            wellbeingEntriesLabel.setText(String.valueOf(entries.size()));
            averageStressLabel.setText(String.format("%.1f/10", avgStress));

            populateRecentActivity(users, entries);
        } catch (RuntimeException e) {
            totalUsersLabel.setText("0");
            adminUsersLabel.setText("0");
            wellbeingEntriesLabel.setText("0");
            averageStressLabel.setText("0.0/10");
            populateRecentActivity(List.of(), List.of());
        }
    }

    private void populateRecentActivity(List<User> users, List<WellBeing> entries) {
        recentActivityBox.getChildren().clear();
        recentActivityBox.getChildren().add(AdminViewFactory.createTimelineCard(
                "Authentication rule",
                "Any authenticated email ending with @rlife.com opens this back office.",
                "Routing"
        ));
        recentActivityBox.getChildren().add(AdminViewFactory.createTimelineCard(
                "Registered users",
                users.isEmpty() ? "No users found in the current database." : users.size() + " user accounts available for management.",
                "Users"
        ));
        recentActivityBox.getChildren().add(AdminViewFactory.createTimelineCard(
                "Wellbeing data",
                entries.isEmpty() ? "No wellbeing entries recorded yet." : entries.size() + " wellbeing entries are available for reporting.",
                "Insights"
        ));
    }
}
