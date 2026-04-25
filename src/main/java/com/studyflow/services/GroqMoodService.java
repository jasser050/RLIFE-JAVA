package com.studyflow.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroqMoodService {
    public record MoodDetectionResult(String mood, double confidence, String reason, String source) {}

    private static final Pattern CONTENT_PATTERN =
            Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern MOOD_PATTERN =
            Pattern.compile("\"mood\"\\s*:\\s*\"(great|good|okay|stressed|tired)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONFIDENCE_PATTERN =
            Pattern.compile("\"confidence\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern REASON_PATTERN =
            Pattern.compile("\"reason\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private final HttpClient client;
    private final String apiKey;
    private final String model;

    public GroqMoodService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(12))
                .build();
        this.apiKey = firstNonBlank(System.getenv("GROQ_API_KEY"));
        this.model = firstNonBlank(
                System.getenv("GROQ_VISION_MODEL"),
                "meta-llama/llama-4-scout-17b-16e-instruct"
        );
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public MoodDetectionResult detectMoodFromDataUrl(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank() || !dataUrl.startsWith("data:image/")) {
            throw new IllegalArgumentException("Invalid camera frame.");
        }
        if (!isConfigured()) {
            throw new IllegalStateException("Missing GROQ_API_KEY environment variable.");
        }

        String prompt = """
                You are a professional affect analysis assistant.
                Analyze ONLY visible facial expression and classify current emotional state.
                Return strict JSON only:
                {"mood":"great|good|okay|stressed|tired","confidence":0.0-1.0,"reason":"short reason"}
                Rules:
                - Choose one of: great, good, okay, stressed, tired
                - confidence must be numeric between 0 and 1
                - reason max 16 words, factual, no diagnosis
                - if face visibility is poor, lower confidence
                - no markdown
                """;

        String payload = buildPayload(prompt, dataUrl);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .timeout(Duration.ofSeconds(35))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Groq request failed (" + response.statusCode() + ").");
            }

            String assistantContent = extractAssistantContent(response.body());
            if (assistantContent == null || assistantContent.isBlank()) {
                throw new IllegalStateException("Empty response from Groq.");
            }
            return parseDetection(assistantContent);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Groq connection error: " + e.getMessage(), e);
        }
    }

    private String buildPayload(String prompt, String dataUrl) {
        return "{"
                + "\"model\":\"" + escapeJson(model) + "\","
                + "\"temperature\":0.1,"
                + "\"max_tokens\":120,"
                + "\"messages\":[{"
                + "\"role\":\"user\","
                + "\"content\":["
                + "{\"type\":\"text\",\"text\":\"" + escapeJson(prompt) + "\"},"
                + "{\"type\":\"image_url\",\"image_url\":{\"url\":\"" + escapeJson(dataUrl) + "\"}}"
                + "]"
                + "}]"
                + "}";
    }

    private MoodDetectionResult parseDetection(String rawContent) {
        String json = rawContent.trim();

        Matcher moodMatcher = MOOD_PATTERN.matcher(json);
        String mood = moodMatcher.find() ? moodMatcher.group(1).toLowerCase(Locale.ROOT) : guessMood(json);

        Matcher confidenceMatcher = CONFIDENCE_PATTERN.matcher(json);
        double confidence = confidenceMatcher.find() ? safeParseConfidence(confidenceMatcher.group(1)) : 0.55;

        Matcher reasonMatcher = REASON_PATTERN.matcher(json);
        String reason = reasonMatcher.find() ? unescapeJson(reasonMatcher.group(1)).trim() : "Detected from facial expression.";
        if (reason.isBlank()) {
            reason = "Detected from facial expression.";
        }

        return new MoodDetectionResult(mood, confidence, reason, "groq");
    }

    private String extractAssistantContent(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        Matcher matcher = CONTENT_PATTERN.matcher(body);
        String last = null;
        while (matcher.find()) {
            last = matcher.group(1);
        }
        return last == null ? null : unescapeJson(last);
    }

    private double safeParseConfidence(String value) {
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < 0d) {
                return 0d;
            }
            if (parsed > 1d) {
                return 1d;
            }
            return parsed;
        } catch (Exception ignored) {
            return 0.55;
        }
    }

    private String guessMood(String text) {
        String v = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (v.contains("stressed") || v.contains("anxious") || v.contains("tense")) {
            return "stressed";
        }
        if (v.contains("tired") || v.contains("fatigue") || v.contains("sleepy")) {
            return "tired";
        }
        if (v.contains("great") || v.contains("happy") || v.contains("excited")) {
            return "great";
        }
        if (v.contains("good") || v.contains("calm") || v.contains("relaxed")) {
            return "good";
        }
        return "okay";
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    public String getModel() {
        return model;
    }
}
