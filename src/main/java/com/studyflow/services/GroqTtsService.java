package com.studyflow.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class GroqTtsService {
    private final HttpClient client;
    private final String apiKey;
    private final String model;
    private final String voice;

    public GroqTtsService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(12))
                .build();
        this.apiKey = firstNonBlank(System.getenv("GROQ_API_KEY"));
        this.model = firstNonBlank(
                System.getenv("GROQ_TTS_MODEL"),
                "canopylabs/orpheus-v1-english"
        );
        this.voice = firstNonBlank(
                System.getenv("GROQ_TTS_VOICE"),
                "hannah"
        );
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public byte[] synthesizeEnglishRelaxing(String text) {
        if (!isConfigured()) {
            throw new IllegalStateException("Missing GROQ_API_KEY for TTS.");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("TTS input text is empty.");
        }

        String directed = "[soft] [slow] [reassuring] " + text.trim();
        if (directed.length() > 200) {
            directed = directed.substring(0, 200);
        }

        String payload = "{"
                + "\"model\":\"" + escapeJson(model) + "\","
                + "\"voice\":\"" + escapeJson(voice) + "\","
                + "\"input\":\"" + escapeJson(directed) + "\","
                + "\"response_format\":\"wav\""
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/audio/speech"))
                .timeout(Duration.ofSeconds(35))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String err = new String(response.body(), StandardCharsets.UTF_8);
                throw new IllegalStateException("Groq TTS failed (" + response.statusCode() + "): " + err);
            }
            if (response.body() == null || response.body().length == 0) {
                throw new IllegalStateException("Groq TTS returned empty audio.");
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Groq TTS connection error: " + e.getMessage(), e);
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String escapeJson(String value) {
        String v = value == null ? "" : value;
        return v.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

