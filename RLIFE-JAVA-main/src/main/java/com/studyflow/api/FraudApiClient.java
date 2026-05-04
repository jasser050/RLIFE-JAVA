package com.studyflow.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * ═══════════════════════════════════════════════════════════════
 *  FraudApiClient — Client HTTP utilisé par AntiFraudEngine
 * ═══════════════════════════════════════════════════════════════
 *
 *  Appelle l'API REST anti-fraude locale (AntiFraudServer).
 *  Tous les appels sont NON-BLOQUANTS (timeout 3s max).
 *  En cas d'échec → mode dégradé local (le quiz continue).
 *
 *  UTILISATION dans AntiFraudEngine :
 *
 *    private final FraudApiClient apiClient = new FraudApiClient();
 *
 *    // Démarrer une session
 *    apiClient.startSession(sessionId);
 *
 *    // Loguer un événement → retourne "TERMINATE" ou "CONTINUE"
 *    String decision = apiClient.logEvent(sessionId, "FOCUS_LOST",
 *                                         "Window unfocused", 3, "HIGH");
 *    if ("TERMINATE".equals(decision)) finishQuiz();
 *
 *    // Clore la session
 *    double adjusted = apiClient.endSession(sessionId, rawScore, penaltyCount);
 */
public class FraudApiClient {

    private static final String BASE_URL = "http://localhost:8085/api/fraud";
    private static final int    TIMEOUT_SEC = 3;

    private final HttpClient client;
    private boolean apiAvailable = true; // devient false si le serveur est down

    public FraudApiClient() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SEC))
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    //  START SESSION
    // ═══════════════════════════════════════════════════════════

    /**
     * Démarre une session anti-fraude dans l'API.
     * Appel asynchrone — ne bloque pas l'UI JavaFX.
     */
    public void startSession(String sessionId) {
        if (!apiAvailable) return;
        String body = "{\"sessionId\":\"" + sessionId + "\"}";
        postAsync(BASE_URL + "/session/start", body, response -> {
            System.out.println("[FraudClient] Session started: " + sessionId);
        });
    }

    // ═══════════════════════════════════════════════════════════
    //  LOG EVENT → retourne la décision ("CONTINUE" | "TERMINATE")
    // ═══════════════════════════════════════════════════════════

    /**
     * Envoie un événement suspect à l'API.
     *
     * @param sessionId  UUID de la session en cours
     * @param eventType  Ex : "FOCUS_LOST", "FULLSCREEN_EXIT"
     * @param details    Description humaine
     * @param fraudScore Score de fraude cumulé
     * @param severity   "HIGH" | "MEDIUM" | "LOW"
     * @param callback   Appelé avec "TERMINATE" ou "CONTINUE"
     */
    public void logEvent(String sessionId, String eventType, String details,
                         int fraudScore, String severity,
                         java.util.function.Consumer<String> callback) {
        if (!apiAvailable) {
            if (callback != null) callback.accept("CONTINUE");
            return;
        }

        String body = "{"
                + "\"sessionId\":\"" + sessionId + "\","
                + "\"eventType\":\"" + eventType + "\","
                + "\"details\":\"" + escape(details) + "\","
                + "\"fraudScore\":" + fraudScore + ","
                + "\"severity\":\"" + severity + "\""
                + "}";

        postAsync(BASE_URL + "/event", body, response -> {
            // Extraire la décision depuis la réponse JSON
            String decision = response.contains("TERMINATE") ? "TERMINATE" : "CONTINUE";
            if (callback != null) callback.accept(decision);
        });
    }

    // ═══════════════════════════════════════════════════════════
    //  END SESSION → retourne le score ajusté
    // ═══════════════════════════════════════════════════════════

    /**
     * Clôture la session et récupère le score ajusté après pénalités.
     *
     * @param rawScore     Score brut (avant pénalités)
     * @param penaltyCount Nombre de pénalités appliquées
     * @param callback     Appelé avec le score ajusté (double)
     */
    public void endSession(String sessionId, double rawScore, int penaltyCount,
                           java.util.function.Consumer<Double> callback) {
        if (!apiAvailable) {
            double adjusted = Math.max(0, rawScore - penaltyCount);
            if (callback != null) callback.accept(adjusted);
            return;
        }

        String body = "{"
                + "\"sessionId\":\"" + sessionId + "\","
                + "\"rawScore\":" + rawScore + ","
                + "\"penaltyCount\":" + penaltyCount
                + "}";

        postAsync(BASE_URL + "/session/end", body, response -> {
            double adjusted = parseDouble(response, "adjustedScore",
                    Math.max(0, rawScore - penaltyCount));
            if (callback != null) callback.accept(adjusted);
        });
    }

    // ═══════════════════════════════════════════════════════════
    //  HEALTH CHECK
    // ═══════════════════════════════════════════════════════════

    /**
     * Vérifie que l'API est disponible.
     * @return true si l'API répond
     */
    public boolean checkHealth() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/health"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            apiAvailable = resp.statusCode() == 200;
        } catch (Exception e) {
            apiAvailable = false;
            System.err.println("[FraudClient] API not available — local mode active");
        }
        return apiAvailable;
    }

    public boolean isApiAvailable() { return apiAvailable; }

    // ═══════════════════════════════════════════════════════════
    //  UTILS
    // ═══════════════════════════════════════════════════════════

    /**
     * POST asynchrone — n'appelle pas le callback si l'API est down.
     */
    private void postAsync(String url, String body,
                           java.util.function.Consumer<String> onSuccess) {
        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(TIMEOUT_SEC))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> resp =
                        client.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() == 200) {
                    apiAvailable = true;
                    if (onSuccess != null) onSuccess.accept(resp.body());
                } else {
                    System.err.println("[FraudClient] HTTP " + resp.statusCode()
                            + " for " + url);
                }
            } catch (Exception e) {
                apiAvailable = false; // basculer en mode local
                System.err.println("[FraudClient] Request failed: " + e.getMessage());
            }
        }, "FraudApiThread").start();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "'").replace("\n", " ");
    }

    private double parseDouble(String json, String key, double fallback) {
        try {
            String search = "\"" + key + "\":";
            int idx = json.indexOf(search);
            if (idx == -1) return fallback;
            int start = idx + search.length();
            int end   = start;
            while (end < json.length() && ",}".indexOf(json.charAt(end)) == -1) end++;
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (Exception e) { return fallback; }
    }
}