package com.studyflow.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * ═══════════════════════════════════════════════════════════════
 *  AntiFraudServer — Serveur HTTP REST embarqué (JDK pur)
 * ═══════════════════════════════════════════════════════════════
 *
 *  Démarre un mini-serveur REST sur http://localhost:8085
 *  SANS dépendance externe — utilise com.sun.net.httpserver
 *  inclus dans le JDK 11+.
 *
 *  DÉMARRAGE (dans App.java ou MainController) :
 *
 *    AntiFraudServer server = new AntiFraudServer();
 *    server.start();
 *
 *  ARRÊT (quand l'application se ferme) :
 *
 *    server.stop();
 *
 *  ENDPOINTS disponibles :
 *    POST http://localhost:8085/api/fraud/session/start
 *    POST http://localhost:8085/api/fraud/event
 *    POST http://localhost:8085/api/fraud/session/end
 *    GET  http://localhost:8085/api/fraud/session/{id}
 *    GET  http://localhost:8085/api/fraud/health
 */
public class AntiFraudServer {

    private static final int    PORT    = 8085;
    private static final String BASE    = "/api/fraud";

    private HttpServer         server;
    private final FraudApiHandler handler = new FraudApiHandler();

    // ═══════════════════════════════════════════════════════════
    //  START
    // ═══════════════════════════════════════════════════════════

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // ── Routes ────────────────────────────────────────
            server.createContext(BASE + "/health",          this::handleHealth);
            server.createContext(BASE + "/session/start",   this::handleSessionStart);
            server.createContext(BASE + "/session/end",     this::handleSessionEnd);
            server.createContext(BASE + "/event",           this::handleEvent);
            server.createContext(BASE + "/session/",        this::handleGetSession);

            // Pool de threads dédié (ne bloque pas le thread JavaFX)
            server.setExecutor(Executors.newFixedThreadPool(4));
            server.start();

            System.out.println("[AntiFraudServer] ✅ Started on http://localhost:" + PORT + BASE);
        } catch (Exception e) {
            System.err.println("[AntiFraudServer] ❌ Failed to start: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            System.out.println("[AntiFraudServer] Stopped.");
        }
    }

    public boolean isRunning() { return server != null; }

    // ═══════════════════════════════════════════════════════════
    //  HANDLERS
    // ═══════════════════════════════════════════════════════════

    /** GET /api/fraud/health */
    private void handleHealth(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) { send(ex, 405, "{\"error\":\"Method not allowed\"}"); return; }
        send(ex, 200, handler.health());
    }

    /** POST /api/fraud/session/start */
    private void handleSessionStart(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { send(ex, 405, "{\"error\":\"Method not allowed\"}"); return; }

        String body = readBody(ex);
        String sessionId = extractString(body, "sessionId");

        if (sessionId.isEmpty()) { send(ex, 400, "{\"error\":\"sessionId required\"}"); return; }

        String result = handler.startSession(sessionId);
        send(ex, 200, result);
    }

    /** POST /api/fraud/session/end */
    private void handleSessionEnd(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { send(ex, 405, "{\"error\":\"Method not allowed\"}"); return; }

        String body         = readBody(ex);
        String sessionId    = extractString(body, "sessionId");
        double rawScore     = extractDouble(body, "rawScore");
        int    penaltyCount = extractInt(body, "penaltyCount");

        if (sessionId.isEmpty()) { send(ex, 400, "{\"error\":\"sessionId required\"}"); return; }

        String result = handler.endSession(sessionId, rawScore, penaltyCount);
        send(ex, 200, result);
    }

    /** POST /api/fraud/event */
    private void handleEvent(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { send(ex, 405, "{\"error\":\"Method not allowed\"}"); return; }

        String body       = readBody(ex);
        String sessionId  = extractString(body, "sessionId");
        String eventType  = extractString(body, "eventType");
        String details    = extractString(body, "details");
        int    fraudScore = extractInt(body, "fraudScore");
        String severity   = extractString(body, "severity");

        if (sessionId.isEmpty() || eventType.isEmpty()) {
            send(ex, 400, "{\"error\":\"sessionId and eventType required\"}"); return;
        }

        String result = handler.logEvent(sessionId, eventType, details, fraudScore, severity);
        send(ex, 200, result);
    }

    /** GET /api/fraud/session/{sessionId} */
    private void handleGetSession(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) { send(ex, 405, "{\"error\":\"Method not allowed\"}"); return; }

        // Extraire l'ID depuis l'URL : /api/fraud/session/abc-123
        String path      = ex.getRequestURI().getPath();
        String prefix    = BASE + "/session/";
        String sessionId = path.startsWith(prefix) ? path.substring(prefix.length()) : "";

        if (sessionId.isEmpty()) { send(ex, 400, "{\"error\":\"sessionId missing in URL\"}"); return; }

        String result = handler.getSession(sessionId);
        send(ex, 200, result);
    }

    // ═══════════════════════════════════════════════════════════
    //  UTILS HTTP
    // ═══════════════════════════════════════════════════════════

    /** Envoie une réponse JSON */
    private void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type",  "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); // CORS
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    /** Lit le corps de la requête */
    private String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  MINI-PARSER JSON (sans dépendance externe)
    //  Parse uniquement les valeurs simples de premier niveau
    // ═══════════════════════════════════════════════════════════

    private String extractString(String json, String key) {
        String search = "\"" + key + "\"";
        int ki = json.indexOf(search);
        if (ki == -1) return "";
        int ci = json.indexOf(':', ki + search.length());
        if (ci == -1) return "";
        // Trouver la valeur — peut être string ou null
        int start = ci + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length()) return "";
        if (json.charAt(start) == '"') {
            int end = start + 1;
            while (end < json.length() && json.charAt(end) != '"') {
                if (json.charAt(end) == '\\') end++; // skip escaped char
                end++;
            }
            return json.substring(start + 1, Math.min(end, json.length()));
        }
        // Valeur non-string (number, bool)
        int end = start;
        while (end < json.length() && ",}".indexOf(json.charAt(end)) == -1) end++;
        return json.substring(start, end).trim();
    }

    private int extractInt(String json, String key) {
        try { return Integer.parseInt(extractString(json, key)); }
        catch (Exception e) { return 0; }
    }

    private double extractDouble(String json, String key) {
        try { return Double.parseDouble(extractString(json, key)); }
        catch (Exception e) { return 0.0; }
    }
}