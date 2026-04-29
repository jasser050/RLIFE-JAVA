package com.studyflow.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class FootballDataService {

    private static final String API_BASE = "https://api.football-data.org/v4";
    private static final String API_KEY_ENV = "FOOTBALL_DATA_API_KEY";
    private static final String API_KEY_PROPERTY = "football.data.api.key";
    private static final String FALLBACK_API_KEY = "99ae936a515249fd8f3e6af0e2c8c004";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public List<TeamOption> getSuggestedTeams() {
        List<TeamOption> teams = new ArrayList<>();
        teams.add(new TeamOption(57, "Arsenal"));
        teams.add(new TeamOption(61, "Chelsea"));
        teams.add(new TeamOption(64, "Liverpool"));
        teams.add(new TeamOption(65, "Manchester City"));
        teams.add(new TeamOption(66, "Manchester United"));
        teams.add(new TeamOption(73, "Tottenham"));
        teams.add(new TeamOption(81, "Barcelona"));
        teams.add(new TeamOption(86, "Real Madrid"));
        teams.add(new TeamOption(78, "Atletico Madrid"));
        teams.add(new TeamOption(98, "AC Milan"));
        teams.add(new TeamOption(108, "Inter"));
        teams.add(new TeamOption(109, "Juventus"));
        teams.add(new TeamOption(5, "Bayern Munich"));
        teams.add(new TeamOption(524, "PSG"));
        return teams;
    }

    public NextMatch fetchNextMatch(TeamOption team) throws IOException {
        if (team == null) {
            throw new IOException("Please choose a team first.");
        }

        String apiKey = resolveApiKey();
        if (apiKey.isBlank()) {
            throw new IOException("Football API key is missing.");
        }

        String endpoint = API_BASE + "/teams/" + team.id() + "/matches?status=SCHEDULED&limit=10";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("X-Auth-Token", apiKey)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IOException("Football API request interrupted.", interruptedException);
        }

        int status = response.statusCode();
        if (status == 403 || status == 401) {
            throw new IOException("Football API authentication failed.");
        }
        if (status < 200 || status >= 300) {
            throw new IOException("Football API error (" + status + ").");
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray matches = root.has("matches") && root.get("matches").isJsonArray()
                ? root.getAsJsonArray("matches")
                : new JsonArray();

        for (JsonElement element : matches) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject matchObject = element.getAsJsonObject();
            NextMatch nextMatch = mapMatch(matchObject, team);
            if (nextMatch != null) {
                return nextMatch;
            }
        }

        throw new IOException("No upcoming matches found for " + team.name() + ".");
    }

    private NextMatch mapMatch(JsonObject matchObject, TeamOption team) {
        if (matchObject == null || !matchObject.has("utcDate")) {
            return null;
        }

        String utcDateRaw = matchObject.get("utcDate").getAsString();
        OffsetDateTime kickoffUtc;
        try {
            kickoffUtc = OffsetDateTime.parse(utcDateRaw);
        } catch (Exception exception) {
            return null;
        }

        JsonObject homeTeam = matchObject.has("homeTeam") && matchObject.get("homeTeam").isJsonObject()
                ? matchObject.getAsJsonObject("homeTeam")
                : null;
        JsonObject awayTeam = matchObject.has("awayTeam") && matchObject.get("awayTeam").isJsonObject()
                ? matchObject.getAsJsonObject("awayTeam")
                : null;

        String homeName = homeTeam != null && homeTeam.has("name") ? homeTeam.get("name").getAsString() : "Home";
        String awayName = awayTeam != null && awayTeam.has("name") ? awayTeam.get("name").getAsString() : "Away";

        JsonObject competition = matchObject.has("competition") && matchObject.get("competition").isJsonObject()
                ? matchObject.getAsJsonObject("competition")
                : null;
        String competitionName = competition != null && competition.has("name")
                ? competition.get("name").getAsString()
                : "Competition";

        boolean favoriteIsHome = normalize(homeName).equals(normalize(team.name()));
        String opponentName = favoriteIsHome ? awayName : homeName;
        String summary = favoriteIsHome
                ? homeName + " vs " + awayName
                : awayName + " vs " + homeName;

        return new NextMatch(
                team,
                homeName,
                awayName,
                opponentName,
                competitionName,
                kickoffUtc,
                summary,
                favoriteIsHome
        );
    }

    private String resolveApiKey() {
        String fromProperty = sanitize(System.getProperty(API_KEY_PROPERTY));
        if (!fromProperty.isBlank()) {
            return fromProperty;
        }

        String fromEnv = sanitize(System.getenv(API_KEY_ENV));
        if (!fromEnv.isBlank()) {
            return fromEnv;
        }

        return sanitize(FALLBACK_API_KEY);
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    public record TeamOption(int id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    public record NextMatch(
            TeamOption team,
            String homeTeam,
            String awayTeam,
            String opponent,
            String competition,
            OffsetDateTime kickoffUtc,
            String summary,
            boolean favoriteIsHome
    ) {
        public String homeLogoUrl() { return ""; }
        public String awayLogoUrl() { return ""; }
    }
}


