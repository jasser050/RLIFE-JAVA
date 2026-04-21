package com.studyflow.api;

import com.studyflow.models.RecommendationStress;
import com.studyflow.services.ServiceRecommendationStress;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class WellbeingRecommendationsApiServer {

    private static final String BASE_PATH = "/api/wellbeing/recommendations";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ServiceRecommendationStress service = new ServiceRecommendationStress();
    private HttpServer server;
    private int activePort = -1;

    public boolean start(int port) {
        if (server != null) {
            return true;
        }

        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            int candidatePort = port + i;
            try {
                server = HttpServer.create(new InetSocketAddress(candidatePort), 0);
                server.createContext(BASE_PATH, this::handleRecommendations);
                server.setExecutor(null);
                server.start();
                activePort = candidatePort;
                System.out.println("Recommendations API started on " + getBaseUrl());
                return true;
            } catch (IOException e) {
                if (i == 0) {
                    System.err.println("Port " + candidatePort + " is unavailable, trying another port...");
                }
            }
        }

        System.err.println("Recommendations API could not start (ports " + port + " to " + (port + maxAttempts - 1) + ").");
        return false;
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            activePort = -1;
        }
    }

    public int getActivePort() {
        return activePort;
    }

    public String getBaseUrl() {
        return activePort > 0 ? "http://localhost:" + activePort + BASE_PATH : "N/A";
    }

    private void handleRecommendations(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, """
                        {"success":false,"error":"Method not allowed"}""");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String suffix = path.length() > BASE_PATH.length() ? path.substring(BASE_PATH.length()) : "";

            if ("/health".equals(suffix)) {
                handleHealth(exchange);
                return;
            }

            if (suffix.isEmpty() || "/".equals(suffix)) {
                handleList(exchange);
                return;
            }

            if (suffix.startsWith("/by-level/")) {
                String level = suffix.substring("/by-level/".length()).trim();
                handleByLevel(exchange, level);
                return;
            }

            if (suffix.startsWith("/")) {
                String idRaw = suffix.substring(1).trim();
                handleShow(exchange, idRaw);
                return;
            }

            writeJson(exchange, 404, """
                    {"success":false,"error":"Endpoint not found"}""");
        } catch (RuntimeException e) {
            writeJson(exchange, 500,
                    "{\"success\":false,\"error\":\"Internal server error\",\"message\":\"" + escape(safeMessage(e)) + "\"}");
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        try {
            int activeCount = service.findAllActive().size();
            writeJson(exchange, 200, "{\"success\":true,\"status\":\"UP\",\"activeRecommendations\":" + activeCount + "}");
        } catch (RuntimeException e) {
            writeJson(exchange, 503,
                    "{\"success\":false,\"status\":\"DOWN\",\"error\":\"Database unavailable\",\"message\":\""
                            + escape(safeMessage(e)) + "\"}");
        }
    }

    private void handleList(HttpExchange exchange) throws IOException {
        List<RecommendationStress> recommendations = service.findAllActive();
        StringBuilder json = new StringBuilder();
        json.append("{\"success\":true,\"data\":[");
        for (int i = 0; i < recommendations.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            appendRecommendationBase(json, recommendations.get(i));
        }
        json.append("]}");
        writeJson(exchange, 200, json.toString());
    }

    private void handleByLevel(HttpExchange exchange, String levelRaw) throws IOException {
        String level = levelRaw == null ? "" : levelRaw.toLowerCase();
        if (!"low".equals(level) && !"medium".equals(level) && !"high".equals(level)) {
            writeJson(exchange, 400, """
                    {"success":false,"error":"Invalid level. Use: low, medium, or high"}""");
            return;
        }

        List<RecommendationStress> recommendations = service.findByLevel(level);
        StringBuilder json = new StringBuilder();
        json.append("{\"success\":true,\"level\":\"")
                .append(escape(level))
                .append("\",\"count\":")
                .append(recommendations.size())
                .append(",\"data\":[");

        for (int i = 0; i < recommendations.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            appendRecommendationBase(json, recommendations.get(i));
        }
        json.append("]}");
        writeJson(exchange, 200, json.toString());
    }

    private void handleShow(HttpExchange exchange, String idRaw) throws IOException {
        int id;
        try {
            id = Integer.parseInt(idRaw);
        } catch (NumberFormatException e) {
            writeJson(exchange, 400, """
                    {"success":false,"error":"Invalid recommendation id"}""");
            return;
        }

        RecommendationStress rec = service.findById(id);
        if (rec == null) {
            writeJson(exchange, 404, """
                    {"success":false,"error":"Recommendation not found"}""");
            return;
        }

        String createdAt = rec.getCreatedAt() == null ? "null" : "\"" + rec.getCreatedAt().format(DATE_TIME_FORMATTER) + "\"";
        String json = "{\"success\":true,\"data\":{"
                + "\"id\":" + rec.getId() + ","
                + "\"title\":\"" + escape(rec.getTitle()) + "\","
                + "\"content\":\"" + escape(rec.getContent()) + "\","
                + "\"level\":\"" + escape(rec.getLevel()) + "\","
                + "\"isActive\":" + rec.isActive() + ","
                + "\"createdAt\":" + createdAt
                + "}}";

        writeJson(exchange, 200, json);
    }

    private void appendRecommendationBase(StringBuilder json, RecommendationStress rec) {
        json.append("{")
                .append("\"id\":").append(rec.getId()).append(",")
                .append("\"title\":\"").append(escape(rec.getTitle())).append("\",")
                .append("\"content\":\"").append(escape(rec.getContent())).append("\",")
                .append("\"level\":\"").append(escape(rec.getLevel())).append("\"")
                .append("}");
    }

    private void writeJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
