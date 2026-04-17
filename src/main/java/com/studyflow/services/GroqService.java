package com.studyflow.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * AI Service — uses Groq API (Llama 3.3 70B) for fast inference.
 * Kept as GeminiService to avoid renaming across the project.
 */
public class GroqService {



    private final HttpClient client;

    public GroqService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * Send a message to the AI with a system instruction and get the response text.
     */
    public String chat(String systemPrompt, String userMessage) throws Exception {
        String requestBody = "{"
                + "\"model\":\"llama-3.3-70b-versatile\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":" + jsonEscape(systemPrompt) + "},"
                + "{\"role\":\"user\",\"content\":" + jsonEscape(userMessage) + "}"
                + "],"
                + "\"temperature\":0.3,"
                + "\"max_tokens\":1024"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("API_URL"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + "API_KEY")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API error (" + response.statusCode() + "): " + response.body());
        }

        return extractContent(response.body());
    }

    /**
     * Extract the assistant message content from the Groq/OpenAI-format JSON response.
     */
    private String extractContent(String json) {
        // Find "content": "..." inside the choices array
        String marker = "\"content\"";
        // Skip the first "content" which is in the request echo — find the one after "assistant"
        int assistantIdx = json.indexOf("\"assistant\"");
        if (assistantIdx == -1) assistantIdx = 0;

        int idx = json.indexOf(marker, assistantIdx);
        if (idx == -1) return "No response from AI.";

        int colonIdx = json.indexOf(':', idx + marker.length());
        if (colonIdx == -1) return "No response from AI.";

        // Skip whitespace and find the opening quote
        int start = json.indexOf('"', colonIdx + 1);
        if (start == -1) return "No response from AI.";
        start++;

        // Parse the string value (handle escaped characters)
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == '"') { sb.append('"'); i++; }
                else if (next == 'n') { sb.append('\n'); i++; }
                else if (next == '\\') { sb.append('\\'); i++; }
                else if (next == 't') { sb.append('\t'); i++; }
                else if (next == '/') { sb.append('/'); i++; }
                else { sb.append(c); }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Escape a string for JSON embedding.
     */
    private String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
