package com.studyflow.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenRouterService {
    public record ChatMessage(String role, String content) {}

    private static final Pattern CONTENT_PATTERN =
            Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private final HttpClient client;
    private final String apiKey;

    public OpenRouterService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(12))
                .build();
        this.apiKey = firstNonBlank(
                System.getenv("OPENROUTER_API_KEY"),
                System.getenv("OPENAI_API_KEY"),
                readWindowsUserEnvironmentVariable("OPENROUTER_API_KEY"),
                readWindowsUserEnvironmentVariable("OPENAI_API_KEY")
        );
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String chat(List<ChatMessage> messages, String model, double temperature, int maxTokens) {
        if (!isConfigured() || messages == null || messages.isEmpty()) {
            return null;
        }

        String selectedModel = (model == null || model.isBlank())
                ? "anthropic/claude-3-haiku"
                : model;

        String payload = buildPayload(messages, selectedModel, temperature, maxTokens);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "http://localhost")
                .header("X-Title", "StudyFlow Wellbeing")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            return extractAssistantContent(response.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    private String buildPayload(List<ChatMessage> messages, String model, double temperature, int maxTokens) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"model\":\"").append(escapeJson(model)).append("\",");
        sb.append("\"temperature\":").append(temperature).append(",");
        sb.append("\"max_tokens\":").append(Math.max(64, maxTokens)).append(",");
        sb.append("\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage m = messages.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\"role\":\"")
                    .append(escapeJson(m.role() == null ? "user" : m.role()))
                    .append("\",\"content\":\"")
                    .append(escapeJson(m.content() == null ? "" : m.content()))
                    .append("\"}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String extractAssistantContent(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        Matcher matcher = CONTENT_PATTERN.matcher(body);
        if (!matcher.find()) {
            return null;
        }
        String raw = matcher.group(1);
        return unescapeJson(raw).trim();
    }

    private String escapeJson(String value) {
        String v = value == null ? "" : value;
        return v.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String readWindowsUserEnvironmentVariable(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String os = System.getProperty("os.name", "");
        if (!os.toLowerCase().contains("win")) {
            return null;
        }

        Process process = null;
        try {
            process = new ProcessBuilder("reg", "query", "HKCU\\Environment", "/v", name)
                    .redirectErrorStream(true)
                    .start();
            String output;
            try (InputStream stream = process.getInputStream()) {
                output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0 || output == null || output.isBlank()) {
                return null;
            }

            String[] lines = output.split("\\R");
            for (String line : lines) {
                String trimmed = line == null ? "" : line.trim();
                if (!trimmed.startsWith(name + " ")) {
                    continue;
                }
                String[] parts = trimmed.split("\\s{2,}");
                if (parts.length >= 3) {
                    return parts[2].trim();
                }
                List<String> compactParts = new ArrayList<>(List.of(trimmed.split("\\s+")));
                if (compactParts.size() >= 3) {
                    return compactParts.get(compactParts.size() - 1).trim();
                }
            }
        } catch (IOException | InterruptedException ignored) {
            if (ignored instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return null;
    }
}
