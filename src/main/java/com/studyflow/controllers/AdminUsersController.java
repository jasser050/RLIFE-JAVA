package com.studyflow.controllers;

import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminUsersController implements Initializable {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colId;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colGender;
    @FXML private TableColumn<User, String> colUniversity;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, Void> colActions;
    @FXML private Label userCountLabel;

    private final ServiceUser serviceUser = new ServiceUser();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        loadUsers();
    }

    private void setupColumns() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName().trim()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colUsername.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getUsername() != null ? c.getValue().getUsername() : ""));
        colGender.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getGender() != null ? c.getValue().getGender() : "N/A"));
        colUniversity.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getUniversity() != null ? c.getValue().getUniversity() : ""));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isBanned() ? "Banned" : "Active"));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if ("Banned".equals(item)) {
                    setStyle("-fx-text-fill: #F43F5E; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #34D399; -fx-font-weight: bold;");
                }
            }
        });

        // Actions column with Ban/Unban + Delete buttons
        colActions.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }

                User user = getTableView().getItems().get(getIndex());
                HBox box = new HBox(6);
                box.setAlignment(Pos.CENTER);

                if (user.isBanned()) {
                    Button unban = new Button("Unban");
                    unban.getStyleClass().addAll("btn-secondary");
                    unban.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
                    FontIcon icon = new FontIcon("fth-user-check");
                    icon.setIconSize(12);
                    unban.setGraphic(icon);
                    unban.setOnAction(e -> handleUnban(user));
                    box.getChildren().add(unban);
                } else {
                    Button ban = new Button("Ban");
                    ban.getStyleClass().addAll("btn-secondary");
                    ban.setStyle("-fx-font-size: 11px; -fx-padding: 4 10; -fx-text-fill: #F43F5E;");
                    FontIcon icon = new FontIcon("fth-user-x");
                    icon.setIconSize(12);
                    ban.setGraphic(icon);
                    ban.setOnAction(e -> handleBan(user));
                    box.getChildren().add(ban);
                }

                Button del = new Button();
                del.getStyleClass().add("btn-ghost");
                del.setStyle("-fx-padding: 4 6;");
                FontIcon trashIcon = new FontIcon("fth-trash-2");
                trashIcon.setIconSize(13);
                trashIcon.setStyle("-fx-icon-color: #F43F5E;");
                del.setGraphic(trashIcon);
                del.setOnAction(e -> handleDelete(user));
                box.getChildren().add(del);

                setGraphic(box);
            }
        });
    }

    private void loadUsers() {
        List<User> users = serviceUser.getAll();
        // Filter out admin account from the list
        users.removeIf(u -> "admin@rlife.com".equalsIgnoreCase(u.getEmail()));
        usersTable.setItems(FXCollections.observableArrayList(users));
        userCountLabel.setText(users.size() + " users");
    }

    @FXML
    private void refreshUsers() { loadUsers(); }

    private void handleBan(User user) {
        TextInputDialog dialog = new TextInputDialog("Violation of terms");
        dialog.setTitle("Ban User");
        dialog.setHeaderText("Ban " + user.getFullName().trim() + " (" + user.getEmail() + ")");
        dialog.setContentText("Reason:");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            serviceUser.banUser(user.getId(), result.get());
            loadUsers();
        }
    }

    private void handleUnban(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Unban User");
        confirm.setHeaderText("Unban " + user.getFullName().trim() + "?");
        confirm.setContentText("This will restore the user's access to the platform.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceUser.unbanUser(user.getId());
            loadUsers();
        }
    }

    private void handleDelete(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText("Permanently delete " + user.getFullName().trim() + "?");
        confirm.setContentText("This action cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceUser.delete(user);
            loadUsers();
        }
    }
}
