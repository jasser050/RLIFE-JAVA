package com.studyflow.controllers;

import com.studyflow.models.User;
import com.studyflow.services.ServiceUser;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class AdminDashboardController implements Initializable {

    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label bannedUsersLabel;
    @FXML private Label newUsersLabel;
    @FXML private Label galaxyCountLabel;
    @FXML private StackPane galaxyContainer;

    @FXML private BarChart<String, Number> registrationsChart;
    @FXML private PieChart genderChart;
    @FXML private BarChart<String, Number> activityChart;
    @FXML private AreaChart<String, Number> growthChart;

    @FXML private TableView<User> recentUsersTable;
    @FXML private TableColumn<User, String> colId;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colGender;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, String> colDate;

    private final ServiceUser serviceUser = new ServiceUser();
    private List<User> allUsers;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        allUsers = serviceUser.getAll();
        loadStats();
        loadGalaxy();
        loadRegistrationsChart();
        loadGenderChart();
        loadActivityChart();
        loadGrowthChart();
        loadRecentUsers();
    }

    private void loadStats() {
        int total = allUsers.size();
        long banned = allUsers.stream().filter(User::isBanned).count();
        long active = total - banned;

        String currentMonth = String.format("%tY-%<tm", new Date());
        long newThisMonth = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().startsWith(currentMonth))
                .count();

        totalUsersLabel.setText(String.valueOf(total));
        activeUsersLabel.setText(String.valueOf(active));
        bannedUsersLabel.setText(String.valueOf(banned));
        newUsersLabel.setText(String.valueOf(newThisMonth));
    }

    private void loadGalaxy() {
        try {
            UserGalaxyPanel galaxy = new UserGalaxyPanel();
            galaxyContainer.getChildren().add(galaxy);
            galaxyCountLabel.setText(allUsers.size() + " stars");
        } catch (Exception e) {
            System.out.println("Galaxy failed to load: " + e.getMessage());
            Label fallback = new Label("Galaxy requires 3D support");
            fallback.setStyle("-fx-text-fill: #64748B; -fx-font-size: 14px;");
            galaxyContainer.getChildren().add(fallback);
        }
    }

    private void loadRegistrationsChart() {
        Map<String, Integer> regByMonth = serviceUser.getUserRegistrationsByMonth();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Registrations");
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(regByMonth.entrySet());
        Collections.reverse(entries);
        for (Map.Entry<String, Integer> e : entries) {
            series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        }
        registrationsChart.getData().add(series);
    }

    private void loadGenderChart() {
        long males = allUsers.stream().filter(u -> "male".equalsIgnoreCase(u.getGender())).count();
        long females = allUsers.stream().filter(u -> "female".equalsIgnoreCase(u.getGender())).count();
        long other = allUsers.size() - males - females;
        genderChart.setData(FXCollections.observableArrayList(
            new PieChart.Data("Male (" + males + ")", males),
            new PieChart.Data("Female (" + females + ")", females)
        ));
        if (other > 0) genderChart.getData().add(new PieChart.Data("Other (" + other + ")", other));
    }

    @SuppressWarnings("unchecked")
    private void loadActivityChart() {
        long active = allUsers.stream().filter(u -> !u.isBanned()).count();
        long banned = allUsers.stream().filter(User::isBanned).count();

        XYChart.Series<String, Number> activeSeries = new XYChart.Series<>();
        activeSeries.setName("Active");
        activeSeries.getData().add(new XYChart.Data<>("Users", active));

        XYChart.Series<String, Number> bannedSeries = new XYChart.Series<>();
        bannedSeries.setName("Banned");
        bannedSeries.getData().add(new XYChart.Data<>("Users", banned));

        activityChart.getData().addAll(activeSeries, bannedSeries);
    }

    private void loadGrowthChart() {
        Map<String, Integer> reg = serviceUser.getUserRegistrationsByMonth();
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(reg.entrySet());
        Collections.reverse(entries);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Cumulative");
        int cumulative = 0;
        for (Map.Entry<String, Integer> e : entries) {
            cumulative += e.getValue();
            series.getData().add(new XYChart.Data<>(e.getKey(), cumulative));
        }
        growthChart.getData().add(series);
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

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("Banned".equals(item)
                    ? "-fx-text-fill: #F43F5E; -fx-font-weight: bold;"
                    : "-fx-text-fill: #34D399; -fx-font-weight: bold;");
            }
        });

        List<User> sorted = new ArrayList<>(allUsers);
        sorted.sort((a, b) -> {
            String da = a.getCreatedAt() != null ? a.getCreatedAt() : "";
            String db = b.getCreatedAt() != null ? b.getCreatedAt() : "";
            return db.compareTo(da);
        });
        recentUsersTable.setItems(FXCollections.observableArrayList(
                sorted.subList(0, Math.min(10, sorted.size()))));
    }

    // ── PDF Export (saves to file via FileChooser) ────────────────

    @FXML
    private void exportUserListPdf() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"ID", "Name", "Email", "Gender", "Status"});
        for (User u : allUsers) {
            rows.add(new String[]{
                String.valueOf(u.getId()),
                trunc(u.getFullName().trim(), 24),
                trunc(u.getEmail(), 30),
                u.getGender() != null ? u.getGender() : "N/A",
                u.isBanned() ? "BANNED" : "Active"
            });
        }
        savePdf("RLife - User List", "Total Users: " + allUsers.size(), rows, "user_list.pdf");
    }

    @FXML
    private void exportBannedPdf() {
        List<User> banned = allUsers.stream().filter(User::isBanned).toList();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"ID", "Name", "Email", "Ban Reason"});
        for (User u : banned) {
            rows.add(new String[]{
                String.valueOf(u.getId()),
                trunc(u.getFullName().trim(), 24),
                trunc(u.getEmail(), 30),
                trunc(u.getBanReason() != null ? u.getBanReason() : "No reason", 28)
            });
        }
        savePdf("RLife - Banned Users", "Banned: " + banned.size() + " / " + allUsers.size() + " total", rows, "banned_users.pdf");
    }

    @FXML
    private void exportAuditPdf() {
        List<String[]> log = serviceUser.getAuditLog();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"ID", "Email", "Name", "Action", "Date"});
        for (String[] e : log) {
            rows.add(new String[]{e[0], trunc(e[1], 28), trunc(e[2], 18), trunc(e[3], 22), trunc(e[4], 18)});
        }
        savePdf("RLife - Audit Log", "Entries: " + log.size(), rows, "audit_log.pdf");
    }

    private void savePdf(String title, String subtitle, List<String[]> rows, String defaultName) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save PDF");
        fc.setInitialFileName(defaultName);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fc.showSaveDialog(totalUsersLabel.getScene().getWindow());
        if (file == null) return;

        try (PDDocument doc = new PDDocument()) {
            int rowsPerPage = 38;
            int totalRows = rows.size() - 1; // exclude header
            int pages = Math.max(1, (int) Math.ceil(totalRows / (double) rowsPerPage));

            for (int page = 0; page < pages; page++) {
                PDPage pdPage = new PDPage(PDRectangle.A4);
                doc.addPage(pdPage);

                try (PDPageContentStream cs = new PDPageContentStream(doc, pdPage)) {
                    float y = 780;
                    float marginLeft = 40;

                    // Title
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                    cs.beginText(); cs.newLineAtOffset(marginLeft, y); cs.showText(title); cs.endText();
                    y -= 22;

                    // Subtitle + date
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    cs.beginText(); cs.newLineAtOffset(marginLeft, y);
                    cs.showText(subtitle + "  |  Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
                    cs.endText();
                    y -= 8;

                    // Line separator
                    cs.setStrokingColor(0.6f, 0.4f, 0.9f);
                    cs.setLineWidth(1.5f);
                    cs.moveTo(marginLeft, y); cs.lineTo(555, y); cs.stroke();
                    y -= 20;

                    // Header row
                    String[] header = rows.get(0);
                    int cols = header.length;
                    float colWidth = 515f / cols;

                    cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
                    for (int c = 0; c < cols; c++) {
                        cs.beginText(); cs.newLineAtOffset(marginLeft + c * colWidth, y); cs.showText(header[c]); cs.endText();
                    }
                    y -= 4;
                    cs.setStrokingColor(0.8f, 0.8f, 0.8f);
                    cs.setLineWidth(0.5f);
                    cs.moveTo(marginLeft, y); cs.lineTo(555, y); cs.stroke();
                    y -= 14;

                    // Data rows for this page
                    cs.setFont(PDType1Font.HELVETICA, 8);
                    int startRow = 1 + page * rowsPerPage;
                    int endRow = Math.min(startRow + rowsPerPage, rows.size());
                    for (int r = startRow; r < endRow; r++) {
                        String[] row = rows.get(r);
                        for (int c = 0; c < Math.min(row.length, cols); c++) {
                            cs.beginText(); cs.newLineAtOffset(marginLeft + c * colWidth, y); cs.showText(row[c] != null ? row[c] : ""); cs.endText();
                        }
                        y -= 16;
                    }

                    // Footer
                    cs.setFont(PDType1Font.HELVETICA, 7);
                    cs.beginText(); cs.newLineAtOffset(marginLeft, 30);
                    cs.showText("RLife Admin  -  Page " + (page + 1) + " of " + pages);
                    cs.endText();
                }
            }

            doc.save(file);
            showAlert("PDF saved successfully!\n" + file.getAbsolutePath());

            // Open the PDF
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(file);
            }
        } catch (IOException e) {
            showAlert("Failed to save PDF: " + e.getMessage());
        }
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("PDF Export");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "." : s;
    }
}
