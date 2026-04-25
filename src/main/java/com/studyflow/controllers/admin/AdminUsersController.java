package com.studyflow.controllers.admin;

import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AdminUsersController implements Initializable {

    @FXML private Label totalUsersLabel;
    @FXML private Label adminUsersLabel;
    @FXML private Label studentUsersLabel;
    @FXML private VBox usersListBox;

    private final ServiceUser serviceUser = new ServiceUser();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        usersListBox.getChildren().clear();
        try {
            List<User> users = serviceUser.getAll();
            long adminCount = users.stream().filter(User::isAdmin).count();

            totalUsersLabel.setText(String.valueOf(users.size()));
            adminUsersLabel.setText(String.valueOf(adminCount));
            studentUsersLabel.setText(String.valueOf(Math.max(0, users.size() - adminCount)));

            if (users.isEmpty()) {
                usersListBox.getChildren().add(AdminViewFactory.createTimelineCard(
                        "No users",
                        "The user table is empty. Add records, then return to this screen.",
                        "Empty"
                ));
                return;
            }

            for (User user : users) {
                String role = user.isAdmin() ? "Admin" : "Student";
                String subtitle = (user.getUsername() == null || user.getUsername().isBlank() ? user.getEmail() : user.getUsername())
                        + "  |  " + user.getEmail();
                usersListBox.getChildren().add(AdminViewFactory.createUserCard(
                        user.getFullName().trim().isEmpty() ? user.getUsername() : user.getFullName().trim(),
                        subtitle,
                        role
                ));
            }
        } catch (RuntimeException e) {
            totalUsersLabel.setText("0");
            adminUsersLabel.setText("0");
            studentUsersLabel.setText("0");
            usersListBox.getChildren().add(AdminViewFactory.createTimelineCard(
                    "Users unavailable",
                    "The admin screen opened, but user data could not be loaded.",
                    "Warning"
            ));
        }
    }
}
