package com.studyflow.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.studyflow.models.SlideContent;
import javafx.concurrent.Task;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

public class ClaudeService {
    private static final String DEFAULT_ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String DEFAULT_MODEL = "claude-sonnet-4-20250514";
    private static final String DEFAULT_VERSION = "2023-06-01";
    private static final int DEFAULT_MAX_TOKENS = 4096;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Properties config = loadConfig();

    public Task<List<SlideContent>> createGenerationTask(
            String topic,
            String theme,
            String language,
            String audience,
            int slideCount
    ) {
        return new Task<>() {
            @Override
            protected List<SlideContent> call() throws Exception {
                return generatePresentationContent(topic, theme, language, audience, slideCount);
            }
        };
    }

    public boolean hasApiKey() {
        return !resolveApiKey().isBlank();
    }

    public List<SlideContent> generatePresentationContent(
            String topic,
            String theme,
            String language,
            String audience,
            int slideCount
    ) throws Exception {
        String normalizedTopic = safe(topic);
        if (normalizedTopic.isBlank()) {
            throw new IllegalArgumentException("Le sujet de la présentation est vide.");
        }

        String apiKey = resolveApiKey();
        if (apiKey.isBlank()) {
            throw new IllegalStateException("La clé API Anthropic est absente dans config.properties.");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getProperty("anthropic.endpoint", DEFAULT_ENDPOINT)))
                .timeout(Duration.ofSeconds(30))
                .header("x-api-key", apiKey)
                .header("anthropic-version", config.getProperty("anthropic.version", DEFAULT_VERSION))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildPayload(
                        normalizedTopic,
                        safe(theme),
                        safe(language),
                        safe(audience),
                        slideCount
                ), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            if (exception.getMessage() != null && exception.getMessage().toLowerCase(Locale.ROOT).contains("timed out")) {
                throw new TimeoutException("Le service Anthropic a dépassé le délai de 30 secondes.");
            }
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        }

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new IllegalStateException("Clé API Anthropic invalide ou non autorisée.");
        }
        if (response.statusCode() == 429) {
            throw new IllegalStateException("La limite Anthropic a été atteinte. Réessaie dans un moment.");
        }
        if (response.statusCode() >= 500) {
            throw new IllegalStateException("Anthropic est momentanément indisponible.");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Erreur Anthropic " + response.statusCode() + ": " + compact(response.body(), 220));
        }

        return parseSlides(response.body(), normalizedTopic, slideCount);
    }

    private String buildPayload(String topic, String theme, String language, String audience, int slideCount) throws Exception {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("model", config.getProperty("anthropic.model", DEFAULT_MODEL));
        payload.put("max_tokens", Integer.parseInt(config.getProperty("anthropic.max_tokens", String.valueOf(DEFAULT_MAX_TOKENS))));
        payload.put("temperature", 0.35);

        ArrayNode messages = mapper.createArrayNode();
        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", buildPrompt(topic, theme, language, audience, slideCount));
        messages.add(userMessage);
        payload.set("messages", messages);

        return mapper.writeValueAsString(payload);
    }

    private String buildPrompt(String topic, String theme, String language, String audience, int slideCount) {
        return """
                Tu es un expert en conception de présentations académiques et professionnelles.
                Génère EXACTEMENT %d slides sur le sujet suivant : %s

                Contexte:
                - Thème visuel: %s
                - Langue de sortie: %s
                - Public cible: %s

                Contraintes:
                - La réponse doit être UNIQUEMENT un tableau JSON valide.
                - Pas de markdown.
                - Pas d'explication avant ou après le JSON.
                - Chaque slide doit contenir 3 à 5 bullet points maximum.
                - Utilise des titres courts, clairs et impactants.
                - Prévois une progression logique: ouverture, développement, synthèse, conclusion.
                - La première slide doit être de type "title" ou "content".
                - La dernière slide doit être de type "conclusion" ou "summary".

                Format exact attendu:
                [
                  {
                    "slideNumber": 1,
                    "title": "Titre",
                    "bulletPoints": ["Point 1", "Point 2", "Point 3"],
                    "speakerNotes": "Notes du présentateur",
                    "type": "title|content|diagram|summary|conclusion"
                  }
                ]
                """.formatted(slideCount, topic, theme, language, audience);
    }

    private List<SlideContent> parseSlides(String rawResponse, String topic, int requestedSlideCount) throws Exception {
        JsonNode root = mapper.readTree(rawResponse);
        JsonNode contentNode = root.path("content");
        if (!contentNode.isArray() || contentNode.isEmpty()) {
            throw new IllegalStateException("Réponse Claude invalide: contenu absent.");
        }

        StringBuilder textBuilder = new StringBuilder();
        for (JsonNode item : contentNode) {
            if ("text".equalsIgnoreCase(item.path("type").asText())) {
                textBuilder.append(item.path("text").asText(""));
            }
        }

        String jsonArrayText = extractJsonArray(textBuilder.toString());
        JsonNode slidesNode = mapper.readTree(jsonArrayText);
        if (!slidesNode.isArray()) {
            throw new IllegalStateException("Claude n'a pas retourné un tableau JSON de slides.");
        }

        List<SlideContent> slides = new ArrayList<>();
        for (JsonNode slideNode : slidesNode) {
            SlideContent slide = new SlideContent();
            slide.setSlideNumber(slideNode.path("slideNumber").asInt(slides.size() + 1));
            slide.setTitle(defaultIfBlank(slideNode.path("title").asText(""), "Slide " + (slides.size() + 1)));

            List<String> bullets = new ArrayList<>();
            JsonNode bulletsNode = slideNode.path("bulletPoints");
            if (bulletsNode.isArray()) {
                for (JsonNode bullet : bulletsNode) {
                    String text = compact(bullet.asText(""), 160);
                    if (!text.isBlank()) {
                        bullets.add(text);
                    }
                }
            }
            if (bullets.isEmpty()) {
                bullets.add("Développer le sujet principal.");
                bullets.add("Ajouter un exemple concret.");
                bullets.add("Conclure avec une idée clé.");
            }
            slide.setBulletPoints(bullets);
            slide.setSpeakerNotes(defaultIfBlank(
                    slideNode.path("speakerNotes").asText(""),
                    "Mettre en avant les idées essentielles de cette slide."
            ));
            slide.setType(SlideContent.SlideType.fromApiValue(slideNode.path("type").asText("content")));
            slides.add(slide);
        }

        slides = normalizeSlides(slides, topic, requestedSlideCount);
        renumberSlides(slides);
        return slides;
    }

    private List<SlideContent> normalizeSlides(List<SlideContent> slides, String topic, int requestedSlideCount) {
        List<SlideContent> normalized = new ArrayList<>(slides == null ? List.of() : slides);
        if (normalized.size() > requestedSlideCount) {
            normalized = new ArrayList<>(normalized.subList(0, requestedSlideCount));
        }

        while (normalized.size() < requestedSlideCount) {
            int number = normalized.size() + 1;
            SlideContent filler = new SlideContent();
            filler.setSlideNumber(number);
            filler.setTitle(number == requestedSlideCount ? "Conclusion" : "Point clé " + number);
            filler.setBulletPoints(List.of(
                    "Synthétiser l'idée principale sur " + topic + ".",
                    "Ajouter une illustration ou un exemple parlant.",
                    "Préparer une transition claire vers la slide suivante."
            ));
            filler.setSpeakerNotes("Compléter cette slide avec un exemple lié au sujet.");
            filler.setType(number == requestedSlideCount ? SlideContent.SlideType.CONCLUSION : SlideContent.SlideType.CONTENT);
            normalized.add(filler);
        }

        if (!normalized.isEmpty()) {
            normalized.get(0).setType(SlideContent.SlideType.TITLE);
            SlideContent lastSlide = normalized.get(normalized.size() - 1);
            if (lastSlide.getType() == SlideContent.SlideType.CONTENT) {
                lastSlide.setType(SlideContent.SlideType.CONCLUSION);
                lastSlide.setTitle("Conclusion");
            }
        }
        return normalized;
    }

    private void renumberSlides(List<SlideContent> slides) {
        for (int i = 0; i < slides.size(); i++) {
            slides.get(i).setSlideNumber(i + 1);
        }
    }

    private String extractJsonArray(String content) {
        String normalized = safe(content)
                .replace("```json", "")
                .replace("```", "")
                .trim();

        int start = normalized.indexOf('[');
        int end = normalized.lastIndexOf(']');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("Claude n'a pas retourné un JSON de slides exploitable.");
        }
        return normalized.substring(start, end + 1);
    }

    private Properties loadConfig() {
        Properties properties = new Properties();

        Path external = Path.of("config.properties");
        if (Files.exists(external)) {
            try (InputStream stream = Files.newInputStream(external)) {
                properties.load(stream);
                return properties;
            } catch (IOException ignored) {
                // Fall back to bundled config below.
            }
        }

        try (InputStream stream = ClaudeService.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException ignored) {
            // Missing config is handled when the API key is requested.
        }

        return properties;
    }

    private String resolveApiKey() {
        String configured = firstNonBlank(
                config.getProperty("anthropic.api.key"),
                System.getenv("ANTHROPIC_API_KEY"),
                System.getProperty("anthropic.api.key")
        );
        return configured == null ? "" : configured.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        return safe(value).isBlank() ? fallback : value.trim();
    }

    private String compact(String value, int maxLength) {
        String normalized = safe(value).replaceAll("\\s+", " ");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}