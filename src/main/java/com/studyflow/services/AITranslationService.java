package com.studyflow.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.studyflow.models.Flashcard;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AITranslationService {

    public record TranslationResult(String title, String question, String answer, String languageLabel) {}

    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String API_KEY = "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";
    private static final String[] MODEL_CANDIDATES = {
            "openrouter/free",
            "openai/gpt-oss-20b:free"
    };

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public TranslationResult translateFlashcard(Flashcard flashcard, String languageCode, String languageLabel) throws Exception {
        String prompt = """
                Translate the following flashcard into %s.

                Return only raw JSON with exactly these keys:
                {
                  "title": "...",
                  "question": "...",
                  "answer": "..."
                }

                Keep the meaning precise and study-friendly.
                Do not add explanations.

                Title: %s
                Question: %s
                Answer: %s
                """.formatted(
                languageLabel,
                safe(flashcard.getTitre()),
                safe(flashcard.getQuestion()),
                safe(flashcard.getReponse())
        );

        String content = null;
        StringBuilder failures = new StringBuilder();
        for (String model : MODEL_CANDIDATES) {
            try {
                content = requestCompletion(prompt, model);
                break;
            } catch (Exception e) {
                if (failures.length() > 0) failures.append(" | ");
                failures.append(model).append(" -> ").append(truncate(e.getMessage(), 140));
            }
        }

        if (content == null) {
            throw new Exception("Translation failed: " + failures);
        }

        JsonNode node = mapper.readTree(cleanJson(content));
        String title = node.path("title").asText(safe(flashcard.getTitre())).trim();
        String question = node.path("question").asText(safe(flashcard.getQuestion())).trim();
        String answer = node.path("answer").asText(safe(flashcard.getReponse())).trim();

        return new TranslationResult(
                title.isEmpty() ? safe(flashcard.getTitre()) : title,
                question.isEmpty() ? safe(flashcard.getQuestion()) : question,
                answer.isEmpty() ? safe(flashcard.getReponse()) : answer,
                languageLabel + " (" + languageCode.toUpperCase() + ")"
        );
    }

    private String requestCompletion(String prompt, String model) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0.2);
        body.put("max_tokens", 1200);

        ArrayNode messages = mapper.createArrayNode();
        ObjectNode userMsg = mapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);
        body.set("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .header("HTTP-Referer", "https://studyflow.app")
                .header("X-Title", "StudyFlow")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("OpenRouter API error " + response.statusCode() + " - " + extractErrorMessage(response.body()));
        }

        JsonNode root = mapper.readTree(response.body());
        String content = root.path("choices").path(0).path("message").path("content").asText("").trim();
        if (content.isEmpty()) {
            throw new Exception("Empty translation response for model " + model);
        }
        return content;
    }

    private String extractErrorMessage(String rawBody) {
        try {
            JsonNode root = mapper.readTree(rawBody);
            JsonNode messageNode = root.path("error").path("message");
            if (!messageNode.isMissingNode() && !messageNode.asText("").isBlank()) {
                return messageNode.asText();
            }
        } catch (Exception ignored) {
            // Fall through to raw body.
        }
        return truncate(rawBody, 200);
    }

    private String cleanJson(String raw) {
        raw = raw.replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        int objectStart = raw.indexOf('{');
        int objectEnd = raw.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            raw = raw.substring(objectStart, objectEnd + 1);
        }
        return raw.trim();
    }

    private String truncate(String s, int max) {
        if (s == null) return "null";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }
}
