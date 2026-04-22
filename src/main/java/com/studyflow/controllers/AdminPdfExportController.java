package com.studyflow.controllers;

import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.print.*;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class AdminPdfExportController implements Initializable {

    @FXML private Label statusLabel;
    @FXML private Label filePathLabel;

    private final ServiceUser serviceUser = new ServiceUser();

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    @FXML
    private void exportUserList() {
        List<User> users = serviceUser.getAll();
        StringBuilder sb = new StringBuilder();
        sb.append("RLife - User List Report\n");
        sb.append("Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())).append("\n");
        sb.append("Total Users: ").append(users.size()).append("\n\n");
        sb.append(String.format("%-6s %-20s %-30s %-10s %-10s\n", "ID", "Name", "Email", "Gender", "Status"));
        sb.append("─".repeat(80)).append("\n");
        for (User u : users) {
            sb.append(String.format("%-6d %-20s %-30s %-10s %-10s\n",
                u.getId(),
                truncate(u.getFullName().trim(), 18),
                truncate(u.getEmail(), 28),
                u.getGender() != null ? u.getGender() : "N/A",
                u.isBanned() ? "BANNED" : "Active"
            ));
        }
        printDocument("User List", sb.toString());
    }

    @FXML
    private void exportAuditLog() {
        List<String[]> log = serviceUser.getAuditLog();
        StringBuilder sb = new StringBuilder();
        sb.append("RLife - Audit Log Report\n");
        sb.append("Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())).append("\n");
        sb.append("Total Entries: ").append(log.size()).append("\n\n");
        sb.append(String.format("%-6s %-30s %-20s %-20s\n", "ID", "Email", "Action", "Date"));
        sb.append("─".repeat(80)).append("\n");
        for (String[] entry : log) {
            sb.append(String.format("%-6s %-30s %-20s %-20s\n",
                entry[0],
                truncate(entry[1], 28),
                truncate(entry[3], 18),
                truncate(entry[4], 18)
            ));
        }
        printDocument("Audit Log", sb.toString());
    }

    @FXML
    private void exportBannedUsers() {
        List<User> users = serviceUser.getAll();
        List<User> banned = users.stream().filter(User::isBanned).toList();
        StringBuilder sb = new StringBuilder();
        sb.append("RLife - Banned Users Report\n");
        sb.append("Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())).append("\n");
        sb.append("Banned Users: ").append(banned.size()).append(" / ").append(users.size()).append(" total\n\n");
        if (banned.isEmpty()) {
            sb.append("No banned users found.\n");
        } else {
            sb.append(String.format("%-6s %-20s %-30s %-25s\n", "ID", "Name", "Email", "Ban Reason"));
            sb.append("─".repeat(85)).append("\n");
            for (User u : banned) {
                sb.append(String.format("%-6d %-20s %-30s %-25s\n",
                    u.getId(),
                    truncate(u.getFullName().trim(), 18),
                    truncate(u.getEmail(), 28),
                    truncate(u.getBanReason() != null ? u.getBanReason() : "No reason", 23)
                ));
            }
        }
        printDocument("Banned Users", sb.toString());
    }

    private void printDocument(String title, String content) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            statusLabel.setText("No printer available!");
            statusLabel.setStyle("-fx-text-fill: #F43F5E; -fx-font-size: 16px;");
            return;
        }

        boolean proceed = job.showPrintDialog(statusLabel.getScene().getWindow());
        if (!proceed) {
            statusLabel.setText("Print cancelled.");
            statusLabel.setStyle("-fx-text-fill: #FBBF24; -fx-font-size: 16px;");
            job.endJob();
            return;
        }

        // Build printable node
        Text text = new Text(content);
        text.setFont(Font.font("Consolas", 10));
        text.setFill(javafx.scene.paint.Color.BLACK);
        TextFlow flow = new TextFlow(text);
        flow.setPadding(new Insets(20));
        flow.setPrefWidth(500);
        flow.setStyle("-fx-background-color: white;");

        boolean success = job.printPage(flow);
        if (success) {
            job.endJob();
            statusLabel.setText("\"" + title + "\" sent to printer successfully!");
            statusLabel.setStyle("-fx-text-fill: #34D399; -fx-font-size: 16px;");
            filePathLabel.setText("Document printed at " + new SimpleDateFormat("HH:mm:ss").format(new Date()));
        } else {
            job.endJob();
            statusLabel.setText("Print failed.");
            statusLabel.setStyle("-fx-text-fill: #F43F5E; -fx-font-size: 16px;");
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "." : s;
    }
}
