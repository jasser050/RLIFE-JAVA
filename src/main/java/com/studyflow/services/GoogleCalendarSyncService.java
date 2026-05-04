package com.studyflow.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.studyflow.models.Planning;
import com.studyflow.models.Seance;
import com.studyflow.models.TypeSeance;
import com.studyflow.models.User;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class GoogleCalendarSyncService {
    private static final String GOOGLE_CLIENT_ID = System.getenv("GOOGLE_CLIENT_ID");
    private static final String GOOGLE_CLIENT_SECRET = System.getenv("GOOGLE_CLIENT_SECRET");
    private static final String GOOGLE_REDIRECT_URI = System.getenv("GOOGLE_REDIRECT_URI");

    private static final String GOOGLE_SCOPE = "https://www.googleapis.com/auth/calendar.readonly";
    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String EVENTS_ENDPOINT = "https://www.googleapis.com/calendar/v3/calendars/primary/events";

    private static final String IMPORTED_TYPE_NAME = "Google Imported";
    private static final String IMPORTED_COLOR = "#3B82F6";

    private final ServicePlanning servicePlanning = new ServicePlanning();
    private final ServiceSeance serviceSeance = new ServiceSeance();
    private final ServiceTypeSeance serviceTypeSeance = new ServiceTypeSeance();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    public record SyncResult(int importedCount, int skippedCount, String message) {}

    private record CalendarEvent(String title, LocalDate date, LocalTime start, LocalTime end) {}

    public SyncResult syncFromGoogleCalendar(User user) throws Exception {
        if (user == null) {
            return new SyncResult(0, 0, "No logged-in user found.");
        }
        if (GOOGLE_CLIENT_ID == null || GOOGLE_CLIENT_ID.isBlank()
                || GOOGLE_CLIENT_SECRET == null || GOOGLE_CLIENT_SECRET.isBlank()
                || GOOGLE_REDIRECT_URI == null || GOOGLE_REDIRECT_URI.isBlank()) {
            return new SyncResult(0, 0, "Google OAuth configuration is missing.");
        }

        String authCode = requestAuthorizationCode();
        if (authCode == null || authCode.isBlank()) {
            return new SyncResult(0, 0, "Google authorization was cancelled.");
        }

        String accessToken = exchangeAuthorizationCodeForAccessToken(authCode);
        List<CalendarEvent> events = fetchCalendarEvents(accessToken);
        if (events.isEmpty()) {
            return new SyncResult(0, 0, "No upcoming events found in Google Calendar.");
        }

        TypeSeance importedType = resolveOrCreateImportedType();
        List<Planning> existingPlanning = servicePlanning.getAll();

        int imported = 0;
        int skipped = 0;
        for (CalendarEvent event : events) {
            if (event.title() == null || event.title().isBlank() || event.date() == null
                    || event.start() == null || event.end() == null || !event.end().isAfter(event.start())) {
                skipped++;
                continue;
            }
            boolean duplicate = existingPlanning.stream().anyMatch(p ->
                    Objects.equals(safeLower(p.getSeanceTitle()), safeLower(event.title()))
                            && Objects.equals(p.getPlanningDate(), event.date())
                            && Objects.equals(p.getStartTime(), event.start())
                            && Objects.equals(p.getEndTime(), event.end()));
            if (duplicate) {
                skipped++;
                continue;
            }

            Seance seance = new Seance();
            seance.setUserId(user.getId());
            seance.setTitre(event.title());
            seance.setDescription("Imported from Google Calendar");
            seance.setTypeSeance(importedType.getName());
            seance.setTypeSeanceName(importedType.getName());
            seance.setTypeSeanceId(importedType.getId());
            seance.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            serviceSeance.add(seance);

            Planning planning = new Planning();
            planning.setUserId(user.getId());
            planning.setSeanceId(seance.getId());
            planning.setSeanceTitle(seance.getTitre());
            planning.setPlanningDate(event.date());
            planning.setStartTime(event.start());
            planning.setEndTime(event.end());
            planning.setColorHex(IMPORTED_COLOR);
            servicePlanning.add(planning);

            existingPlanning.add(planning);
            imported++;
        }

        return new SyncResult(imported, skipped, "Google Calendar sync completed.");
    }

    private TypeSeance resolveOrCreateImportedType() {
        Optional<TypeSeance> existing = serviceTypeSeance.getAvailableTypes().stream()
                .filter(type -> type != null && type.getName() != null
                        && IMPORTED_TYPE_NAME.equalsIgnoreCase(type.getName().trim()))
                .findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }
        TypeSeance created = new TypeSeance();
        created.setName(IMPORTED_TYPE_NAME);
        serviceTypeSeance.add(created);
        return serviceTypeSeance.getAvailableTypes().stream()
                .filter(type -> type != null && type.getName() != null
                        && IMPORTED_TYPE_NAME.equalsIgnoreCase(type.getName().trim()))
                .findFirst()
                .orElse(created);
    }

    private String requestAuthorizationCode() throws Exception {
        String state = UUID.randomUUID().toString();
        CompletableFuture<String> codeFuture = new CompletableFuture<>();

        URI redirect = URI.create(GOOGLE_REDIRECT_URI);
        int port = redirect.getPort() == -1 ? 80 : redirect.getPort();
        String callbackPath = redirect.getPath();

        HttpServer callbackServer = HttpServer.create(new InetSocketAddress(port), 0);
        callbackServer.createContext(callbackPath, exchange -> handleCallback(exchange, state, codeFuture));
        callbackServer.start();

        String authUrl = AUTH_ENDPOINT
                + "?client_id=" + encode(GOOGLE_CLIENT_ID)
                + "&redirect_uri=" + encode(GOOGLE_REDIRECT_URI)
                + "&response_type=code"
                + "&scope=" + encode(GOOGLE_SCOPE)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + encode(state);

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI.create(authUrl));
        } else {
            callbackServer.stop(0);
            throw new IllegalStateException("Desktop browser is not supported on this environment.");
        }

        try {
            return codeFuture.get(180, TimeUnit.SECONDS);
        } finally {
            callbackServer.stop(0);
        }
    }

    private void handleCallback(HttpExchange exchange, String expectedState, CompletableFuture<String> codeFuture) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String code = queryParam(query, "code");
        String state = queryParam(query, "state");
        String error = queryParam(query, "error");

        String html;
        if (error != null) {
            html = "<html><body><h3>Google authorization failed.</h3><p>" + escapeHtml(error) + "</p></body></html>";
            codeFuture.completeExceptionally(new IllegalStateException("Google authorization failed: " + error));
        } else if (code == null || state == null || !expectedState.equals(state)) {
            html = "<html><body><h3>Invalid OAuth callback.</h3><p>Please close this tab and retry.</p></body></html>";
            codeFuture.completeExceptionally(new IllegalStateException("Invalid OAuth callback state."));
        } else {
            html = "<html><body><h3>Google Calendar connected.</h3><p>You can return to the app.</p></body></html>";
            codeFuture.complete(code);
        }

        byte[] response = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private String exchangeAuthorizationCodeForAccessToken(String code) throws Exception {
        String body = "code=" + encode(code)
                + "&client_id=" + encode(GOOGLE_CLIENT_ID)
                + "&client_secret=" + encode(GOOGLE_CLIENT_SECRET)
                + "&redirect_uri=" + encode(GOOGLE_REDIRECT_URI)
                + "&grant_type=authorization_code";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_ENDPOINT))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Token exchange failed: HTTP " + response.statusCode() + " - " + response.body());
        }
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!json.has("access_token")) {
            throw new IllegalStateException("Token exchange failed: access_token missing.");
        }
        return json.get("access_token").getAsString();
    }

    private List<CalendarEvent> fetchCalendarEvents(String accessToken) throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneId.systemDefault());
        OffsetDateTime until = now.plusDays(60);
        String url = EVENTS_ENDPOINT
                + "?singleEvents=true"
                + "&orderBy=startTime"
                + "&maxResults=250"
                + "&timeMin=" + encode(now.toInstant().toString())
                + "&timeMax=" + encode(until.toInstant().toString());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Calendar events request failed: HTTP " + response.statusCode() + " - " + response.body());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray items = json.has("items") && json.get("items").isJsonArray() ? json.getAsJsonArray("items") : new JsonArray();
        List<CalendarEvent> events = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            JsonObject item = items.get(i).getAsJsonObject();
            CalendarEvent event = toCalendarEvent(item);
            if (event != null) {
                events.add(event);
            }
        }
        events.sort(Comparator.comparing(CalendarEvent::date).thenComparing(CalendarEvent::start));
        return events;
    }

    private CalendarEvent toCalendarEvent(JsonObject item) {
        if (item == null) {
            return null;
        }
        String summary = item.has("summary") ? item.get("summary").getAsString() : "Google Calendar Event";
        JsonObject start = item.has("start") && item.get("start").isJsonObject() ? item.getAsJsonObject("start") : null;
        JsonObject end = item.has("end") && item.get("end").isJsonObject() ? item.getAsJsonObject("end") : null;
        if (start == null || end == null) {
            return null;
        }

        try {
            if (start.has("dateTime") && end.has("dateTime")) {
                OffsetDateTime startDateTime = OffsetDateTime.parse(start.get("dateTime").getAsString());
                OffsetDateTime endDateTime = OffsetDateTime.parse(end.get("dateTime").getAsString());
                LocalDate date = startDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
                LocalTime startTime = startDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0);
                LocalTime endTime = endDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0);
                if (!endTime.isAfter(startTime)) {
                    endTime = startTime.plusHours(1);
                }
                return new CalendarEvent(summary.trim(), date, startTime, endTime);
            }
            if (start.has("date")) {
                LocalDate date = LocalDate.parse(start.get("date").getAsString());
                return new CalendarEvent(summary.trim(), date, LocalTime.of(9, 0), LocalTime.of(10, 0));
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String queryParam(String query, String key) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String k = pair.substring(0, idx);
            if (!key.equals(k)) {
                continue;
            }
            String v = pair.substring(idx + 1);
            return java.net.URLDecoder.decode(v, StandardCharsets.UTF_8);
        }
        return null;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
