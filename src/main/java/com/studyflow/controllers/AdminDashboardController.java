package com.studyflow.controllers.admin;

import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private Label totalUsersLabel;
    @FXML private Label newUsersLabel;
    @FXML private Label totalCoursesLabel;
    @FXML private Label totalDecksLabel;
    @FXML private Label totalAssignmentsLabel;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String>  colName;
    @FXML private TableColumn<User, String>  colEmail;
    @FXML private TableColumn<User, String>  colUsername;
    @FXML private TableColumn<User, Integer> colCoins;
    @FXML private TableColumn<User, String>  colActions;

    private final ServiceUser serviceUser = new ServiceUser();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        loadData();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName().trim()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colUsername.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getUsername() != null ? "@" + c.getValue().getUsername() : "—"));
        colCoins.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getCoins()).asObject());

        // Actions column — Delete button per row
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("Delete");
            {
                deleteBtn.getStyleClass().addAll("btn-danger-sm");
                deleteBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleDeleteUser(user);
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }

    private void loadData() {
        List<User> users = serviceUser.getAll();

        // Stats
        totalUsersLabel.setText(String.valueOf(users.size()));
        newUsersLabel.setText("+" + Math.min(users.size(), 5) + " this week");

        // Placeholder stats (replace with real service calls when available)
        totalCoursesLabel.setText("—");
        totalDecksLabel.setText("—");
        totalAssignmentsLabel.setText("—");

        // Table — show last 20 users
        int limit = Math.min(users.size(), 20);
        usersTable.setItems(FXCollections.observableArrayList(users.subList(0, limit)));
    }

    private void handleDeleteUser(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText("Delete " + user.getFullName().trim() + "?");
        confirm.setContentText("This action cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                serviceUser.delete(user);
                loadData(); // refresh
            }
        });
    }

    @FXML
    private void viewAllUsers() {
        // Reload full list
        List<User> users = serviceUser.getAll();
        usersTable.setItems(FXCollections.observableArrayList(users));
        totalUsersLabel.setText(String.valueOf(users.size()));
    }
}
