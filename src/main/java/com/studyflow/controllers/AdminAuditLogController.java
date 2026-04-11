package com.studyflow.controllers;

import com.studyflow.services.ServiceUser;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AdminAuditLogController implements Initializable {

    @FXML private TableView<String[]> auditTable;
    @FXML private TableColumn<String[], String> colLogId;
    @FXML private TableColumn<String[], String> colLogEmail;
    @FXML private TableColumn<String[], String> colLogName;
    @FXML private TableColumn<String[], String> colLogAction;
    @FXML private TableColumn<String[], String> colLogDate;
    @FXML private Label logCountLabel;

    private final ServiceUser serviceUser = new ServiceUser();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colLogId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[0]));
        colLogEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[1]));
        colLogName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[2]));
        colLogAction.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[3]));
        colLogDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[4]));

        // Color the action column
        colLogAction.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if (item.startsWith("BANNED")) {
                    setStyle("-fx-text-fill: #F43F5E; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #34D399; -fx-font-weight: bold;");
                }
            }
        });

        loadLog();
    }

    private void loadLog() {
        List<String[]> log = serviceUser.getAuditLog();
        auditTable.setItems(FXCollections.observableArrayList(log));
        logCountLabel.setText(log.size() + " entries");
    }

    @FXML
    private void refresh() { loadLog(); }
}
