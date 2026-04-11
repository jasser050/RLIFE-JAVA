package com.studyflow.controllers;

import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;

import java.net.URL;
import java.util.*;

public class AdminDashboardController implements Initializable {

    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label bannedUsersLabel;
    @FXML private Label newUsersLabel;

    @FXML private BarChart<String, Number> registrationsChart;
    @FXML private PieChart genderChart;

    @FXML private TableView<User> recentUsersTable;
    @FXML private TableColumn<User, String> colId;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colGender;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, String> colDate;

    private final ServiceUser serviceUser = new ServiceUser();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadStats();
        loadCharts();
        loadRecentUsers();
    }

    private void loadStats() {
        List<User> allUsers = serviceUser.getAll();
        int total = allUsers.size();
        long banned = allUsers.stream().filter(User::isBanned).count();
        long active = total - banned;

        // Count new this month
        String currentMonth = String.format("%tY-%<tm", new Date());
        long newThisMonth = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().startsWith(currentMonth))
                .count();

        totalUsersLabel.setText(String.valueOf(total));
        activeUsersLabel.setText(String.valueOf(active));
        bannedUsersLabel.setText(String.valueOf(banned));
        newUsersLabel.setText(String.valueOf(newThisMonth));
    }

    private void loadCharts() {
        // Registration bar chart
        Map<String, Integer> regByMonth = serviceUser.getUserRegistrationsByMonth();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Registrations");
        // Reverse to show chronological order
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(regByMonth.entrySet());
        Collections.reverse(entries);
        for (Map.Entry<String, Integer> e : entries) {
            series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        }
        registrationsChart.getData().add(series);

        // Gender pie chart
        List<User> allUsers = serviceUser.getAll();
        long males = allUsers.stream().filter(u -> "male".equalsIgnoreCase(u.getGender())).count();
        long females = allUsers.stream().filter(u -> "female".equalsIgnoreCase(u.getGender())).count();
        long other = allUsers.size() - males - females;
        genderChart.setData(FXCollections.observableArrayList(
            new PieChart.Data("Male (" + males + ")", males),
            new PieChart.Data("Female (" + females + ")", females)
        ));
        if (other > 0) genderChart.getData().add(new PieChart.Data("Other (" + other + ")", other));
    }

    private void loadRecentUsers() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName().trim()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colGender.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getGender() != null ? c.getValue().getGender() : "N/A"));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isBanned() ? "Banned" : "Active"));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt().substring(0, Math.min(16, c.getValue().getCreatedAt().length())) : "N/A"));

        // Style the status column
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

        List<User> allUsers = serviceUser.getAll();
        // Sort by created_at desc, take last 10
        allUsers.sort((a, b) -> {
            String da = a.getCreatedAt() != null ? a.getCreatedAt() : "";
            String db = b.getCreatedAt() != null ? b.getCreatedAt() : "";
            return db.compareTo(da);
        });
        List<User> recent = allUsers.subList(0, Math.min(10, allUsers.size()));
        recentUsersTable.setItems(FXCollections.observableArrayList(recent));
    }
}
