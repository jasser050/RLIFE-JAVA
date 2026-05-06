package com.studyflow.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.studyflow.models.Planning;
import com.studyflow.models.Seance;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlanningPdfApiService {
    private static final String APDF_CREATE_ENDPOINT = "https://apdf.io/api/pdf/file/create";
    private static final String HARDCODED_APDF_API_KEY = "Vv241JbA4LNtXl4clZ8MOBABfUN30rqGD53FQQSza18ebf23";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final HttpClient httpClient;

    public PlanningPdfApiService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public void exportSessionsPdf(List<Seance> sessions, Path outputPath) throws IOException, InterruptedException {
        if (sessions == null || sessions.isEmpty()) throw new IOException("No sessions available to export.");
        if (outputPath == null) throw new IOException("Invalid output path.");
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) throw new IOException("Missing aPDF.io API key. Set APDF_IO_API_KEY.");
        writePdfFromHtml(buildSessionsHtml(sessions), outputPath, apiKey);
    }

    public void exportPlanningPdf(List<Planning> planningEntries, Path outputPath) throws IOException, InterruptedException {
        exportPlanningPdf(planningEntries, outputPath, YearMonth.now(), "🙂", "", true);
    }

    public void exportPlanningPdf(List<Planning> planningEntries, Path outputPath, YearMonth exportMonth, String feedbackEmoji, String feedbackText)
            throws IOException, InterruptedException {
        exportPlanningPdf(planningEntries, outputPath, exportMonth, feedbackEmoji, feedbackText, true);
    }

    public void exportPlanningPdf(List<Planning> planningEntries, Path outputPath, YearMonth exportMonth, String feedbackEmoji, String feedbackText, boolean lightTheme)
            throws IOException, InterruptedException {
        if (planningEntries == null || planningEntries.isEmpty()) throw new IOException("No planning entries available to export.");
        if (outputPath == null) throw new IOException("Invalid output path.");
        if (exportMonth == null) throw new IOException("Invalid month to export.");
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) throw new IOException("Missing aPDF.io API key. Set APDF_IO_API_KEY.");
        String html = buildPlanningHtml(planningEntries, exportMonth, feedbackEmoji, feedbackText, lightTheme);
        writePdfFromHtml(html, outputPath, apiKey);
    }

    private void writePdfFromHtml(String html, Path outputPath, String apiKey) throws IOException, InterruptedException {
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
        if (!json.has("file") || json.get("file").isJsonNull()) throw new IOException("aPDF.io response does not contain file URL.");
        String fileUrl = json.get("file").getAsString();

        HttpRequest downloadRequest = HttpRequest.newBuilder().uri(URI.create(fileUrl)).GET().build();
        HttpResponse<byte[]> pdfResponse = httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofByteArray());
        if (pdfResponse.statusCode() < 200 || pdfResponse.statusCode() >= 300) {
            throw new IOException("aPDF.io file download failed (HTTP " + pdfResponse.statusCode() + ").");
        }
        if (outputPath.getParent() != null) Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, pdfResponse.body());
    }

    private String buildPlanningHtml(List<Planning> planningEntries, YearMonth exportMonth, String feedbackEmoji, String feedbackText, boolean lightTheme) {
        String logoDataUri = buildLogoDataUri();
        List<Planning> monthEntries = planningEntries.stream()
                .filter(e -> e.getPlanningDate() != null && YearMonth.from(e.getPlanningDate()).equals(exportMonth))
                .sorted(Comparator.comparing(Planning::getPlanningDate).thenComparing(Planning::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        Map<LocalDate, List<Planning>> entriesByDate = new LinkedHashMap<>();
        for (Planning entry : monthEntries) entriesByDate.computeIfAbsent(entry.getPlanningDate(), ignored -> new ArrayList<>()).add(entry);

        String monthLabel = exportMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + exportMonth.getYear();
        String calendarHtml = buildCalendarHtml(exportMonth, entriesByDate);
        String statsHtml = buildStatsHtml(monthEntries, entriesByDate);
        String feedbackEmojiSafe = escape(defaultValue(feedbackEmoji));
        String feedbackTextSafe = feedbackText == null || feedbackText.isBlank() ? "No written feedback." : escape(feedbackText.trim());

        String bodyBg = lightTheme ? "#F5F7FB" : "#050B1A";
        String cardBg = lightTheme ? "#FFFFFF" : "#0B1224";
        String border = lightTheme ? "#DBE3F1" : "#24324D";
        String title = lightTheme ? "#0F172A" : "#E2E8F0";
        String muted = lightTheme ? "#475569" : "#9CB0D0";
        String softBg = lightTheme ? "#F8FAFC" : "#111B31";
        String headerBg = lightTheme ? "linear-gradient(135deg,#FFFFFF,#F4F8FF)" : "linear-gradient(135deg,#0F172A,#1E293B)";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<style>"
                + "@page{size:A4; margin:16mm 12mm 16mm 12mm;}"
                + "body{font-family:Arial,sans-serif;background:" + bodyBg + ";color:" + title + ";margin:0;}"
                + ".card{background:" + cardBg + ";border:1px solid " + border + ";border-radius:18px;overflow:hidden;box-shadow:0 12px 30px rgba(15,23,42,.22);margin:0 0 10px 0;}"
                + ".header{padding:18px 22px;border-bottom:1px solid " + border + ";background:" + headerBg + ";}"
                + ".brand{display:flex;align-items:center;gap:12px;}.brand img{height:38px;width:auto;}"
                + "h1{margin:0;color:" + title + ";font-size:24px;}"
                + ".meta{margin-top:6px;color:" + muted + ";font-size:12px;font-weight:600;}"
                + ".calendar-wrap{padding:16px 18px 18px 18px;}"
                + ".calendar{width:100%;border-collapse:collapse;table-layout:fixed;}"
                + ".calendar th{background:" + softBg + ";color:" + title + ";padding:9px;font-size:11px;text-transform:uppercase;border:1px solid " + border + ";}"
                + ".calendar td{height:104px;vertical-align:top;padding:6px;border:1px solid " + border + ";background:" + cardBg + ";}"
                + ".calendar .muted{background:" + softBg + ";color:" + muted + ";}"
                + ".calendar .upcoming-day{background:linear-gradient(135deg,rgba(59,130,246,.12),rgba(124,58,237,.12));}"
                + ".day-number{font-size:11px;font-weight:800;color:" + title + ";margin-bottom:5px;}"
                + ".event{display:block;font-size:9.5px;line-height:1.25;padding:3px 5px;margin:3px 0;border-radius:8px;color:#fff;word-break:break-word;white-space:normal;}"
                + ".empty-day-icon{display:inline-flex;align-items:center;justify-content:center;width:24px;height:24px;border-radius:999px;background:linear-gradient(135deg,#DBEAFE,#EDE9FE);color:#6366F1;font-size:14px;font-weight:800;margin-top:2px;}"
                + ".page-break{page-break-before:always;}"
                + ".feedback{margin:16px;border:1px solid " + border + ";border-radius:14px;background:" + softBg + ";padding:14px 16px;}"
                + ".feedback-title{font-size:14px;font-weight:800;color:" + title + ";margin-bottom:10px;}"
                + ".emoji-row{font-size:24px;letter-spacing:8px;margin-bottom:12px;}"
                + ".selected{display:inline-block;padding:4px 10px;border:2px solid #7C3AED;border-radius:999px;background:#F5F3FF;box-shadow:0 6px 14px rgba(124,58,237,.15);}"
                + ".comment-box{background:" + cardBg + ";border:1px solid " + border + ";border-radius:12px;padding:12px;font-size:12px;color:" + title + ";min-height:64px;white-space:pre-wrap;line-height:1.45;}"
                + ".stats{display:grid;grid-template-columns:repeat(5,1fr);gap:8px;padding:0;}"
                + ".stat{border:1px solid " + border + ";background:" + cardBg + ";border-radius:10px;padding:8px 9px;}"
                + ".stat-k{font-size:10px;color:" + muted + ";text-transform:uppercase;letter-spacing:.06em;}"
                + ".stat-v{font-size:16px;color:" + title + ";font-weight:800;margin-top:2px;}"
                + "</style></head><body>"
                + "<div class='card'>"
                + "<div class='header'><div class='brand'>"
                + (logoDataUri == null ? "" : "<img src='" + logoDataUri + "' alt='RLIFE logo'/>")
                + "<h1>RLIFE Planning Calendar</h1></div>"
                + "<div class='meta'>Month: " + escape(monthLabel) + "</div></div>"
                + "<div class='calendar-wrap'>" + calendarHtml + "</div>"
                + "</div>"
                + "<div class='page-break'></div>"
                + "<div class='card'>"
                + "<div class='header'><h1 style='font-size:20px;'>Monthly Summary</h1><div class='meta'>Feedback and statistics</div></div>"
                + "<div class='feedback'><div class='feedback-title'>Student Monthly Feedback</div>"
                + "<div class='emoji-row'>&#128542; &#128528; &#128578; &#128516; &#11088; <span class='selected'>" + feedbackEmojiSafe + "</span></div>"
                + "<div class='comment-box'>" + feedbackTextSafe + "</div></div>"
                + "<div class='feedback'><div class='feedback-title'>Monthly Statistics</div>" + statsHtml + "</div>"
                + "</div></body></html>";
    }

    private String buildStatsHtml(List<Planning> monthEntries, Map<LocalDate, List<Planning>> entriesByDate) {
        int totalEvents = monthEntries.size();
        int activeDays = entriesByDate.size();
        long totalMinutes = monthEntries.stream().mapToLong(this::durationMinutes).sum();
        double totalHours = totalMinutes / 60.0;
        double avgEventsPerActiveDay = activeDays == 0 ? 0.0 : (double) totalEvents / activeDays;
        String busiestDay = entriesByDate.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().size()))
                .map(e -> e.getKey().format(DateTimeFormatter.ofPattern("dd MMM")) + " (" + e.getValue().size() + ")")
                .orElse("-");
        return "<div class='stats'>"
                + stat("Total Events", String.valueOf(totalEvents))
                + stat("Active Days", String.valueOf(activeDays))
                + stat("Total Hours", String.format(Locale.ENGLISH, "%.1f h", totalHours))
                + stat("Avg / Day", String.format(Locale.ENGLISH, "%.1f", avgEventsPerActiveDay))
                + stat("Busiest Day", busiestDay)
                + "</div>";
    }

    private String stat(String key, String value) {
        return "<div class='stat'><div class='stat-k'>" + escape(key) + "</div><div class='stat-v'>" + escape(value) + "</div></div>";
    }

    private String buildCalendarHtml(YearMonth exportMonth, Map<LocalDate, List<Planning>> entriesByDate) {
        StringBuilder html = new StringBuilder();
        html.append("<table class='calendar'><thead><tr>")
                .append("<th>Mon</th><th>Tue</th><th>Wed</th><th>Thu</th><th>Fri</th><th>Sat</th><th>Sun</th>")
                .append("</tr></thead><tbody>");
        LocalDate firstDay = exportMonth.atDay(1);
        LocalDate start = firstDay;
        while (start.getDayOfWeek() != DayOfWeek.MONDAY) start = start.minusDays(1);

        LocalDate cursor = start;
        for (int row = 0; row < 6; row++) {
            html.append("<tr>");
            for (int col = 0; col < 7; col++) {
                boolean inMonth = YearMonth.from(cursor).equals(exportMonth);
                boolean upcoming = inMonth && cursor.isAfter(LocalDate.now());
                String cellClass = inMonth ? (upcoming ? "upcoming-day" : "") : "muted";
                if (cellClass.isBlank()) html.append("<td>"); else html.append("<td class='").append(cellClass).append("'>");
                html.append("<div class='day-number'>").append(cursor.getDayOfMonth()).append("</div>");

                List<Planning> dayEntries = entriesByDate.getOrDefault(cursor, List.of());
                if (dayEntries.isEmpty()) {
                    if (inMonth) html.append("<span class='empty-day-icon'>◈</span>");
                } else {
                    int maxVisible = 2;
                    for (int i = 0; i < dayEntries.size(); i++) {
                        if (i >= maxVisible) {
                            int remaining = dayEntries.size() - maxVisible;
                            html.append("<span class='event' style='background:#64748B;'>+").append(remaining).append(" more</span>");
                            break;
                        }
                        Planning entry = dayEntries.get(i);
                        String color = normalizeHexColor(entry.getColorHex());
                        String title = escape(defaultValue(entry.getSeanceTitle()));
                        String time = escape(formatTimeRange(entry));
                        html.append("<span class='event' style='background:").append(color).append(";'>")
                                .append(time).append(" • ").append(title)
                                .append("</span>");
                    }
                }
                html.append("</td>");
                cursor = cursor.plusDays(1);
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
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
                + "<style>body{font-family:Arial,sans-serif;color:#0F172A;padding:24px;}h1{margin:0 0 8px 0;color:#1E3A8A;}p{margin:0 0 16px 0;color:#334155;}table{width:100%;border-collapse:collapse;font-size:12px;}th,td{border:1px solid #CBD5E1;padding:8px;vertical-align:top;text-align:left;}th{background:#E2E8F0;}</style>"
                + "</head><body><h1>RLIFE Sessions Export</h1><p>Total sessions: " + sessions.size() + "</p>"
                + "<table><thead><tr><th>Title</th><th>Type</th><th>Created At</th><th>Description</th></tr></thead><tbody>" + rows + "</tbody></table></body></html>";
    }

    private String buildLogoDataUri() {
        String[] candidates = {"/com/studyflow/assets/rlife-logo.png", "/com/studyflow/images/logo.png"};
        for (String path : candidates) {
            try (InputStream stream = PlanningPdfApiService.class.getResourceAsStream(path)) {
                if (stream == null) continue;
                byte[] bytes = stream.readAllBytes();
                if (bytes.length == 0) continue;
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private String normalizeHexColor(String color) {
        if (color == null) return "#64748B";
        String cleaned = color.trim();
        if (!cleaned.startsWith("#")) cleaned = "#" + cleaned;
        if (cleaned.matches("^#[0-9a-fA-F]{3}$")) {
            char r = cleaned.charAt(1), g = cleaned.charAt(2), b = cleaned.charAt(3);
            return ("#" + r + r + g + g + b + b).toUpperCase();
        }
        if (cleaned.matches("^#[0-9a-fA-F]{6}$")) return cleaned.toUpperCase();
        return "#64748B";
    }

    private String formatTimeRange(Planning planning) {
        String start = planning.getStartTime() == null ? "-" : planning.getStartTime().toString();
        String end = planning.getEndTime() == null ? "-" : planning.getEndTime().toString();
        return start + " - " + end;
    }

    private long durationMinutes(Planning planning) {
        LocalTime start = planning.getStartTime();
        LocalTime end = planning.getEndTime();
        if (start == null || end == null || !end.isAfter(start)) return 0;
        return Duration.between(start, end).toMinutes();
    }

    private String td(String value) { return "<td>" + value + "</td>"; }

    private String resolveTypeLabel(Seance seance) {
        String type = seance.getTypeSeance();
        if (type == null || type.isBlank()) type = seance.getTypeSeanceName();
        return (type == null || type.isBlank()) ? "-" : type;
    }

    private String formatTimestamp(Timestamp timestamp) {
        return timestamp == null ? "-" : DATE_FORMATTER.format(timestamp.toLocalDateTime());
    }

    private String defaultValue(String value) {
        if (value == null || value.isBlank()) return "-";
        return value.trim();
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String shorten(String value) {
        if (value == null) return "";
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240) + "...";
    }

    private String resolveApiKey() {
        if (HARDCODED_APDF_API_KEY != null && !HARDCODED_APDF_API_KEY.isBlank()) return HARDCODED_APDF_API_KEY.trim();
        String fromEnv = System.getenv("APDF_IO_API_KEY");
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv.trim();
        String fromProperty = System.getProperty("apdf.io.api.key");
        if (fromProperty != null && !fromProperty.isBlank()) return fromProperty.trim();
        return null;
    }
}
