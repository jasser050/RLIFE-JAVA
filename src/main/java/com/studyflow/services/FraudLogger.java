package com.studyflow.services;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════
 *  FraudLogger — Journalise les événements suspects
 *  vers un fichier local + optionnellement la BDD
 * ═══════════════════════════════════════════════════
 */
public class FraudLogger {

    private static final String LOG_DIR = "logs";
    private final String logFilePath;

    public FraudLogger() {
        try { Files.createDirectories(Paths.get(LOG_DIR)); }
        catch (IOException e) { System.err.println("[FraudLogger] Cannot create log dir: " + e.getMessage()); }

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        this.logFilePath = LOG_DIR + "/fraud_" + today + ".log";
    }

    // ── Log un événement ─────────────────────────────────────────
    public void log(FraudEvent event) {
        logToFile(event);
        // saveToDatabase(event); // décommenter si BDD activée
    }

    // ── Fichier local ────────────────────────────────────────────
    private void logToFile(FraudEvent event) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(logFilePath, true))) {
            w.write(event.toString());
            w.newLine();
        } catch (IOException e) {
            System.err.println("[FraudLogger] Write error: " + e.getMessage());
        }
    }

    /*
     * Pour activer la BDD, créez cette table :
     *
     * CREATE TABLE fraud_log (
     *   id          INT AUTO_INCREMENT PRIMARY KEY,
     *   event_type  VARCHAR(50),
     *   details     TEXT,
     *   fraud_score INT,
     *   timestamp   DATETIME DEFAULT CURRENT_TIMESTAMP
     * );
     *
     * private void saveToDatabase(FraudEvent event) {
     *     try (Connection con = DatabaseConnection.getConnection();
     *          PreparedStatement ps = con.prepareStatement(
     *              "INSERT INTO fraud_log (event_type, details, fraud_score) VALUES (?,?,?)")) {
     *         ps.setString(1, event.getType().name());
     *         ps.setString(2, event.getDetails());
     *         ps.setInt(3, event.getFraudScoreAtTime());
     *         ps.executeUpdate();
     *     } catch (Exception e) { e.printStackTrace(); }
     * }
     */

    // ── Rapport de session ───────────────────────────────────────
    public String generateReport(List<FraudEvent> events, int finalScore,
                                 double rawScore, double adjustedScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════\n");
        sb.append("    STUDYFLOW — ANTI-FRAUD REPORT\n");
        sb.append("═══════════════════════════════════════════\n");
        sb.append("Date        : ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        sb.append("Events      : ").append(events.size()).append("\n");
        sb.append("Fraud score : ").append(finalScore).append("/").append(AntiFraudEngine.THRESHOLD_TERMINATE).append("\n");
        sb.append("Raw score   : ").append(String.format("%.1f", rawScore)).append("\n");
        sb.append("Adj. score  : ").append(String.format("%.1f", adjustedScore)).append("\n");
        sb.append("───────────────────────────────────────────\n");
        sb.append("ACTIVITY LOG:\n");
        for (FraudEvent e : events) sb.append("  ").append(e).append("\n");
        sb.append("═══════════════════════════════════════════\n");
        return sb.toString();
    }

    public void saveReport(String report, String filename) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(LOG_DIR + "/" + filename))) {
            w.write(report);
        } catch (IOException e) {
            System.err.println("[FraudLogger] Cannot save report: " + e.getMessage());
        }
    }
}