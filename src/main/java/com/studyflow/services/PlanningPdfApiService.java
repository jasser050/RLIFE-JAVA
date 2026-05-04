package com.studyflow.services;

import com.studyflow.models.Planning;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.studyflow.models.Seance;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PlanningPdfApiService {

    private static final String APDF_CREATE_ENDPOINT = "https://apdf.io/api/pdf/file/create";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final HttpClient httpClient;

    public PlanningPdfApiService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public void exportSessionsPdf(List<Seance> sessions, Path outputPath) throws IOException, InterruptedException {
        if (sessions == null || sessions.isEmpty()) {
            throw new IOException("No sessions available to export.");
        }
        if (outputPath == null) {
            throw new IOException("Invalid output path.");
        }

        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException("Missing aPDF.io API key. Set APDF_IO_API_KEY.");
        }

        String html = buildSessionsHtml(sessions);
        String formBody = "html=" + URLEncoder.encode(html, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(APDF_CREATE_ENDPOINT))
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("aPDF.io create failed (HTTP " + response.statusCode() + "): " + shorten(response.body()));
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!json.has("file") || json.get("file").isJsonNull()) {
            throw new IOException("aPDF.io response does not contain file URL.");
        }
        String fileUrl = json.get("file").getAsString();

        HttpRequest downloadRequest = HttpRequest.newBuilder()
                .uri(URI.create(fileUrl))
                .GET()
                .build();
        HttpResponse<byte[]> pdfResponse = httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofByteArray());
        if (pdfResponse.statusCode() < 200 || pdfResponse.statusCode() >= 300) {
            throw new IOException("aPDF.io file download failed (HTTP " + pdfResponse.statusCode() + ").");
        }

        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        Files.write(outputPath, pdfResponse.body());
    }

    public void exportPlanningPdf(List<Planning> planningEntries, Path outputPath) throws IOException, InterruptedException {
        if (planningEntries == null || planningEntries.isEmpty()) {
            throw new IOException("No planning entries available to export.");
        }
        if (outputPath == null) {
            throw new IOException("Invalid output path.");
        }

        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException("Missing aPDF.io API key. Set APDF_IO_API_KEY.");
        }

        String html = buildPlanningHtml(planningEntries);
        String formBody = "html=" + URLEncoder.encode(html, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(APDF_CREATE_ENDPOINT))
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("aPDF.io create failed (HTTP " + response.statusCode() + "): " + shorten(response.body()));
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!json.has("file") || json.get("file").isJsonNull()) {
            throw new IOException("aPDF.io response does not contain file URL.");
        }
        String fileUrl = json.get("file").getAsString();

        HttpRequest downloadRequest = HttpRequest.newBuilder()
                .uri(URI.create(fileUrl))
                .GET()
                .build();
        HttpResponse<byte[]> pdfResponse = httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofByteArray());
        if (pdfResponse.statusCode() < 200 || pdfResponse.statusCode() >= 300) {
            throw new IOException("aPDF.io file download failed (HTTP " + pdfResponse.statusCode() + ").");
        }

        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        Files.write(outputPath, pdfResponse.body());
    }

    private String resolveApiKey() {
        String fromEnv = System.getenv("APDF_IO_API_KEY");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromProperty = System.getProperty("apdf.io.api.key");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }
        return null;
    }

    private String buildSessionsHtml(List<Seance> sessions) {
        StringBuilder rows = new StringBuilder();
        for (Seance seance : sessions) {
            rows.append("<tr>")
                    .append(td(escape(defaultValue(seance.getTitre()))))
                    .append(td(escape(resolveTypeLabel(seance))))
                    .append(td(escape(formatTimestamp(seance.getCreatedAt()))))
                    .append(td(escape(defaultValue(seance.getDescription()))))
                    .append("</tr>");
        }

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<style>"
                + "body{font-family:Arial,sans-serif;color:#0F172A;padding:24px;}"
                + "h1{margin:0 0 8px 0;color:#1E3A8A;} p{margin:0 0 16px 0;color:#334155;}"
                + "table{width:100%;border-collapse:collapse;font-size:12px;}"
                + "th,td{border:1px solid #CBD5E1;padding:8px;vertical-align:top;text-align:left;}"
                + "th{background:#E2E8F0;}"
                + "</style></head><body>"
                + "<h1>RLIFE Sessions Export</h1>"
                + "<p>Total sessions: " + sessions.size() + "</p>"
                + "<table><thead><tr><th>Title</th><th>Type</th><th>Created At</th><th>Description</th></tr></thead>"
                + "<tbody>" + rows + "</tbody></table>"
                + "</body></html>";
    }

    private String buildPlanningHtml(List<Planning> planningEntries) {
        StringBuilder rows = new StringBuilder();
        for (Planning planning : planningEntries) {
            rows.append("<tr>")
                    .append(td(escape(defaultValue(planning.getSeanceTitle()))))
                    .append(td(escape(planning.getPlanningDate() == null ? "-" : planning.getPlanningDate().toString())))
                    .append(td(escape(formatTimeRange(planning))))
                    .append(td(escape(defaultValue(planning.getColorHex()))))
                    .append(td(escape(defaultValue(planning.getFeedback()))))
                    .append("</tr>");
        }

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<style>"
                + "body{font-family:Arial,sans-serif;color:#0F172A;padding:24px;}"
                + "h1{margin:0 0 8px 0;color:#1E3A8A;} p{margin:0 0 16px 0;color:#334155;}"
                + "table{width:100%;border-collapse:collapse;font-size:12px;}"
                + "th,td{border:1px solid #CBD5E1;padding:8px;vertical-align:top;text-align:left;}"
                + "th{background:#E2E8F0;}"
                + "</style></head><body>"
                + "<h1>RLIFE Planning Export</h1>"
                + "<p>Total planning entries: " + planningEntries.size() + "</p>"
                + "<table><thead><tr><th>Session</th><th>Date</th><th>Time</th><th>Color</th><th>Feedback</th></tr></thead>"
                + "<tbody>" + rows + "</tbody></table>"
                + "</body></html>";
    }

    private String formatTimeRange(Planning planning) {
        String start = planning.getStartTime() == null ? "-" : planning.getStartTime().toString();
        String end = planning.getEndTime() == null ? "-" : planning.getEndTime().toString();
        return start + " - " + end;
    }

    private String td(String value) {
        return "<td>" + value + "</td>";
    }

    private String resolveTypeLabel(Seance seance) {
        String type = seance.getTypeSeance();
        if (type == null || type.isBlank()) {
            type = seance.getTypeSeanceName();
        }
        if (type == null || type.isBlank()) {
            return "-";
        }
        return type;
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "-";
        }
        return DATE_FORMATTER.format(timestamp.toLocalDateTime());
    }

    private String defaultValue(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }

    private String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String shorten(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= 240) {
            return normalized;
        }
        return normalized.substring(0, 240) + "...";
    }
}
