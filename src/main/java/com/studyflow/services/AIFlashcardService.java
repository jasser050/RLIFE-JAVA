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
import java.util.ArrayList;
import java.util.List;

/**
 * Calls OpenRouter API to generate flashcards from a topic or text.
 */
public class AIFlashcardService {

    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    // Move this to an env variable or config file in production.
    private static final String API_KEY = "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";
    private static final String[] MODEL_CANDIDATES = {
            "openrouter/free",
            "mistralai/mistral-7b-instruct:free",
            "mistralai/mistral-7b-instruct-v0.3",
            "mistralai/mistral-7b-instruct"
    };

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public List<Flashcard> generate(String topic, int count, String difficulty) throws Exception {
        String prompt = """
            You are a flashcard generator. Given a topic or text, create exactly %d flashcards.
            Difficulty level: %s.

            IMPORTANT: Respond ONLY with a raw JSON array. No markdown fences, no explanation, no extra text.
            The first character of your response must be '[' and the last must be ']'.

            Each object in the array must have exactly these keys:
            - "titre": short card title (max 60 chars)
            - "question": the question (max 300 chars)
            - "reponse": the answer (max 300 chars)

            Example format:
            [{"titre":"...","question":"...","reponse":"..."},{"titre":"...","question":"...","reponse":"..."}]

            Topic/Text:
            %s
            """.formatted(count, difficulty, topic);

        String content = null;
        StringBuilder failures = new StringBuilder();
        for (String model : MODEL_CANDIDATES) {
            try {
                content = requestCompletion(prompt, model);
                System.out.println("[AIFlashcardService] Model success: " + model);
                break;
            } catch (Exception e) {
                if (failures.length() > 0) failures.append(" | ");
                failures.append(model).append(" -> ").append(truncate(e.getMessage(), 160));
                System.err.println("[AIFlashcardService] Model failed: " + model + " - " + e.getMessage());
            }
        }

        if (content == null) {
            throw new Exception("All OpenRouter model attempts failed: " + failures);
        }

        System.out.println("[AIFlashcardService] Raw response: " + truncate(content, 300));

        content = cleanJson(content);

        JsonNode parsed = mapper.readTree(content);
        JsonNode cards;
        if (parsed.isArray()) {
            cards = parsed;
        } else if (parsed.isObject()) {
            JsonNode found = null;
            var it = parsed.fields();
            while (it.hasNext()) {
                JsonNode val = it.next().getValue();
                if (val.isArray()) {
                    found = val;
                    break;
                }
            }
            if (found == null) {
                throw new Exception("AI returned an object but no array field was found inside it.");
            }
            cards = found;
        } else {
            throw new Exception("AI response is not a JSON array. Got: " + truncate(content, 100));
        }

        List<Flashcard> result = new ArrayList<>();
        int diffLevel = switch (difficulty) {
            case "Medium" -> 2;
            case "Hard" -> 3;
            default -> 1;
        };

        for (JsonNode card : cards) {
            String titre = card.path("titre").asText("").trim();
            String question = card.path("question").asText("").trim();
            String reponse = card.path("reponse").asText("").trim();

            if (question.isEmpty() && reponse.isEmpty()) {
                System.out.println("[AIFlashcardService] Skipping empty card.");
                continue;
            }

            Flashcard fc = new Flashcard();
            fc.setTitre(titre.isEmpty() ? "Generated Card" : titre);
            fc.setQuestion(question.isEmpty() ? "?" : question);
            fc.setReponse(reponse.isEmpty() ? "N/A" : reponse);
            fc.setNiveauDifficulte(diffLevel);
            fc.setEtat("new");
            result.add(fc);
        }

        if (result.isEmpty()) {
            throw new Exception("AI returned cards but all were empty after parsing.");
        }

        System.out.println("[AIFlashcardService] Successfully parsed " + result.size() + " cards.");
        return result;
    }

    private String requestCompletion(String prompt, String model) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0.7);
        body.put("max_tokens", 2000);

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
            String apiMessage = extractErrorMessage(response.body());
            throw new Exception("OpenRouter API error " + response.statusCode()
                    + " for model " + model + " - " + apiMessage);
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        String content = contentNode.asText("").trim();
        if (content.isEmpty()) {
            throw new Exception("Empty response body for model " + model);
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
            // Fall back to raw text below.
        }
        return truncate(rawBody, 200);
    }

    private String cleanJson(String raw) {
        raw = raw.replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        int arrayStart = raw.indexOf('[');
        int arrayEnd = raw.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            raw = raw.substring(arrayStart, arrayEnd + 1);
        }

        return raw.trim();
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "null";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
