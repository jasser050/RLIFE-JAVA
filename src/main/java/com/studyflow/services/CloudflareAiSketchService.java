package com.studyflow.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CloudflareAiSketchService {
    private static final String DEFAULT_PROMPT_MODEL = "@cf/meta/llama-3.1-8b-instruct-fast";
    private static final String DEFAULT_IMAGE_MODEL = "@cf/stabilityai/stable-diffusion-xl-base-1.0";
    private static final String API_BASE = "https://api.cloudflare.com/client/v4/accounts/";

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String apiToken;
    private final String accountId;
    private final String promptModel;
    private final String imageModel;

    public CloudflareAiSketchService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(12))
                .build();
        this.mapper = new ObjectMapper();
        this.apiToken = firstNonBlank(
                System.getenv("CLOUDFLARE_API_TOKEN"),
                System.getProperty("cloudflare.api.token"),
                readWindowsUserEnvironmentVariable("CLOUDFLARE_API_TOKEN")
        );
        this.accountId = firstNonBlank(
                System.getenv("CLOUDFLARE_ACCOUNT_ID"),
                System.getProperty("cloudflare.account.id"),
                readWindowsUserEnvironmentVariable("CLOUDFLARE_ACCOUNT_ID")
        );
        this.promptModel = firstNonBlank(
                System.getProperty("cloudflare.ai.model"),
                System.getenv("CLOUDFLARE_AI_MODEL"),
                readWindowsUserEnvironmentVariable("CLOUDFLARE_AI_MODEL"),
                DEFAULT_PROMPT_MODEL
        );
        this.imageModel = firstNonBlank(
                System.getProperty("cloudflare.ai.image.model"),
                System.getenv("CLOUDFLARE_AI_IMAGE_MODEL"),
                readWindowsUserEnvironmentVariable("CLOUDFLARE_AI_IMAGE_MODEL"),
                DEFAULT_IMAGE_MODEL
        );
    }

    public boolean isConfigured() {
        return !apiToken.isBlank() && !accountId.isBlank();
    }

    public String configurationHint() {
        if (isConfigured()) {
            return "";
        }
        return "Set CLOUDFLARE_API_TOKEN and CLOUDFLARE_ACCOUNT_ID to enable Cloudflare sketch generation.";
    }

    public boolean shouldUseImageGeneration(String prompt, String style) {
        String normalizedPrompt = safe(prompt).toLowerCase(Locale.ROOT);
        String normalizedStyle = safe(style).toLowerCase(Locale.ROOT);

        if (normalizedStyle.contains("mindmap") || normalizedStyle.contains("diagram")) {
            return false;
        }

        Set<String> diagramHints = Set.of(
                "diagram", "mindmap", "flowchart", "process", "timeline", "compare", "comparison",
                "graph", "chart", "anatomy", "system", "cycle", "steps", "workflow", "map", "schema",
                "architecture diagram", "network", "org chart", "concept map"
        );

        for (String hint : diagramHints) {
            if (normalizedPrompt.contains(hint)) {
                return false;
            }
        }
        return true;
    }

    public SketchPlan generateSketchPlan(String prompt, String style, String noteTitle, String noteBody) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException(configurationHint());
        }

        ObjectNode payload = mapper.createObjectNode();
        payload.put("max_tokens", 1200);
        payload.put("temperature", 0.25);
        payload.set("messages", buildMessages(prompt, style, noteTitle, noteBody));
        payload.set("response_format", buildResponseFormat());
        JsonNode root = runJsonModel(promptModel, payload);
        JsonNode responseNode = root.path("result").path("response");
        if (responseNode.isMissingNode() || responseNode.isNull()) {
            throw new IllegalStateException("Cloudflare AI returned no sketch payload.");
        }

        JsonNode normalizedNode = normalizeResponseNode(responseNode);
        SketchPlan rawPlan = mapper.treeToValue(normalizedNode, SketchPlan.class);
        return sanitizePlan(rawPlan, prompt, style);
    }

    public GeneratedScene generateScene(String prompt, String style, String noteTitle, String noteBody) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException(configurationHint());
        }

        EnhancedScenePrompt scenePrompt = buildEnhancedScenePrompt(prompt, style, noteTitle, noteBody);
        byte[] imageBytes = runImageModel(scenePrompt);
        return new GeneratedScene(
                scenePrompt.title(),
                scenePrompt.caption(),
                scenePrompt.prompt(),
                imageBytes
        );
    }

    private ArrayNode buildMessages(String prompt, String style, String noteTitle, String noteBody) {
        ArrayNode messages = mapper.createArrayNode();
        messages.add(message("system", """
                You generate a structured whiteboard sketch plan for a student notes canvas.
                Keep it concise, visual, and easy to render.
                Return short labels, short details, and simple relationships.
                Prefer educational layouts with 3 to 7 nodes.
                Choose the layout that best fits the request:
                - mindmap for branching concepts
                - process for step-by-step flows
                - timeline for chronological content
                - comparison for two-sided contrasts
                - diagram for general explanatory structures
                Use a single accent color in hex format.
                """));

        String context = compact(safe(noteBody), 420);
        String title = safe(noteTitle);
        String styleName = safe(style).isBlank() ? "Default" : style.trim();
        messages.add(message("user", """
                Create a clean sketch plan for this prompt.

                Prompt: %s
                Requested style: %s
                Note title: %s
                Relevant note context: %s

                Rules:
                - Keep labels under 28 characters when possible.
                - Keep details under 70 characters.
                - Use 3 to 7 nodes.
                - Use only the allowed layouts and shapes from the schema.
                - Make the output suitable for a single 1120x680 board.
                """.formatted(
                safe(prompt),
                styleName,
                title.isBlank() ? "Untitled" : title,
                context.isBlank() ? "No extra note context." : context
        )));
        return messages;
    }

    private EnhancedScenePrompt buildEnhancedScenePrompt(String prompt, String style, String noteTitle, String noteBody) {
        try {
            ObjectNode payload = mapper.createObjectNode();
            payload.put("max_tokens", 900);
            payload.put("temperature", 0.45);
            payload.set("messages", buildScenePromptMessages(prompt, style, noteTitle, noteBody));
            payload.set("response_format", buildSceneResponseFormat());

            JsonNode root = runJsonModel(promptModel, payload);
            JsonNode responseNode = root.path("result").path("response");
            if (responseNode.isMissingNode() || responseNode.isNull()) {
                return fallbackScenePrompt(prompt, style, noteTitle);
            }

            JsonNode normalizedNode = normalizeResponseNode(responseNode);
            EnhancedScenePrompt raw = mapper.treeToValue(normalizedNode, EnhancedScenePrompt.class);
            return sanitizeScenePrompt(raw, prompt, style, noteTitle);
        } catch (Exception ignored) {
            return fallbackScenePrompt(prompt, style, noteTitle);
        }
    }

    private ArrayNode buildScenePromptMessages(String prompt, String style, String noteTitle, String noteBody) {
        ArrayNode messages = mapper.createArrayNode();
        messages.add(message("system", """
                You convert short sketch requests into rich image prompts for a text-to-image model.
                Keep the subject faithful to the user request.
                Make the result visually strong, polished, detailed, and compositionally clear.
                Prefer one strong focal subject, coherent lighting, depth, materials, and background support.
                Avoid abstraction unless the user explicitly asks for it.
                """));

        messages.add(message("user", """
                Build one high-quality image prompt for a note canvas illustration.

                User prompt: %s
                Requested style: %s
                Note title: %s
                Extra note context: %s

                Requirements:
                - Keep the subject exactly aligned with the user request.
                - Produce a vivid, impressive, realistic or polished illustration.
                - Add environment, materials, lighting, camera framing, and surface details.
                - Avoid text inside the generated image.
                - Avoid deformed anatomy, duplicates, clutter, blur, bad perspective, low detail.
                """.formatted(
                safe(prompt),
                safe(style).isBlank() ? "Default" : style,
                safe(noteTitle).isBlank() ? "Untitled" : noteTitle,
                compact(safe(noteBody), 260).isBlank() ? "No extra context." : compact(safe(noteBody), 260)
        )));
        return messages;
    }

    private ObjectNode buildSceneResponseFormat() {
        ObjectNode responseFormat = mapper.createObjectNode();
        responseFormat.put("type", "json_schema");

        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = mapper.createObjectNode();
        properties.set("title", simpleStringSchema());
        properties.set("caption", simpleStringSchema());
        properties.set("prompt", simpleStringSchema());
        properties.set("negativePrompt", simpleStringSchema());
        schema.set("properties", properties);
        schema.set("required", mapper.valueToTree(List.of("title", "caption", "prompt", "negativePrompt")));

        responseFormat.set("json_schema", schema);
        return responseFormat;
    }

    private EnhancedScenePrompt sanitizeScenePrompt(EnhancedScenePrompt raw, String prompt, String style, String noteTitle) {
        String title = compact(firstNonBlank(raw.title(), safe(noteTitle), safe(prompt), "AI Scene"), 34);
        String caption = compact(firstNonBlank(raw.caption(), "AI image render"), 90);
        String positivePrompt = compact(
                firstNonBlank(raw.prompt(), fallbackScenePrompt(prompt, style, noteTitle).prompt()),
                900
        );
        String negativePrompt = compact(
                firstNonBlank(raw.negativePrompt(), defaultNegativePrompt()),
                320
        );
        return new EnhancedScenePrompt(title, caption, positivePrompt, negativePrompt);
    }

    private EnhancedScenePrompt fallbackScenePrompt(String prompt, String style, String noteTitle) {
        String safePrompt = safe(prompt);
        String styleHint = switch (safe(style).toLowerCase(Locale.ROOT)) {
            case "sketch" -> "hand-drawn concept art with refined linework, soft shading";
            case "chalk" -> "dramatic chalk illustration on a premium dark board, textured strokes";
            case "diagram" -> "clean educational concept illustration";
            default -> "high-detail cinematic concept art";
        };

        String title = compact(firstNonBlank(safe(noteTitle), safePrompt, "AI Scene"), 34);
        String finalPrompt = compact("""
                %s, %s, visually rich composition, strong focal subject, realistic materials,
                balanced lighting, depth, clean background support, premium design presentation,
                highly detailed, polished, beautiful scene, no text, centered composition
                """.formatted(safePrompt, styleHint), 900);

        return new EnhancedScenePrompt(
                title,
                "High-detail render based on the prompt",
                finalPrompt,
                defaultNegativePrompt()
        );
    }

    private byte[] runImageModel(EnhancedScenePrompt scenePrompt) throws Exception {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("prompt", scenePrompt.prompt());
        payload.put("negative_prompt", scenePrompt.negativePrompt());
        payload.put("width", 1024);
        payload.put("height", 768);
        payload.put("num_steps", 20);
        payload.put("guidance", 8.0);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + accountId + "/ai/run/" + imageModel))
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + apiToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        String contentType = firstNonBlank(
                response.headers().firstValue("content-type").orElse(""),
                response.headers().firstValue("Content-Type").orElse("")
        ).toLowerCase(Locale.ROOT);

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String errorBody = new String(response.body(), StandardCharsets.UTF_8);
            throw new IllegalStateException("Cloudflare image model error " + response.statusCode() + ": " + extractErrorMessage(errorBody));
        }

        if (contentType.contains("application/json") || contentType.contains("text/json")) {
            String body = new String(response.body(), StandardCharsets.UTF_8);
            JsonNode root = mapper.readTree(body);
            JsonNode imageNode = root.path("result").path("image");
            if (!imageNode.isMissingNode() && imageNode.isTextual() && !imageNode.asText().isBlank()) {
                return java.util.Base64.getDecoder().decode(imageNode.asText());
            }
            throw new IllegalStateException("Cloudflare returned JSON instead of image bytes.");
        }

        if (response.body() == null || response.body().length == 0) {
            throw new IllegalStateException("Cloudflare returned an empty image.");
        }
        return response.body();
    }

    private JsonNode runJsonModel(String modelName, ObjectNode payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + accountId + "/ai/run/" + modelName))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Cloudflare AI error " + response.statusCode() + ": " + extractErrorMessage(response.body()));
        }
        return mapper.readTree(response.body());
    }

    private ObjectNode buildResponseFormat() {
        ObjectNode responseFormat = mapper.createObjectNode();
        responseFormat.put("type", "json_schema");

        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = mapper.createObjectNode();
        properties.set("title", simpleStringSchema());
        properties.set("layout", enumSchema("mindmap", "diagram", "process", "comparison", "timeline"));
        properties.set("accentColor", simpleStringSchema());

        ObjectNode nodeSchema = mapper.createObjectNode();
        nodeSchema.put("type", "object");
        ObjectNode nodeProperties = mapper.createObjectNode();
        nodeProperties.set("id", simpleStringSchema());
        nodeProperties.set("label", simpleStringSchema());
        nodeProperties.set("detail", simpleStringSchema());
        nodeProperties.set("shape", enumSchema("rounded", "circle", "pill"));
        nodeProperties.set("importance", enumSchema("high", "medium", "low"));
        nodeSchema.set("properties", nodeProperties);
        nodeSchema.set("required", mapper.valueToTree(List.of("id", "label", "detail", "shape", "importance")));

        ObjectNode nodes = mapper.createObjectNode();
        nodes.put("type", "array");
        nodes.put("minItems", 3);
        nodes.put("maxItems", 7);
        nodes.set("items", nodeSchema);
        properties.set("nodes", nodes);

        ObjectNode connectionSchema = mapper.createObjectNode();
        connectionSchema.put("type", "object");
        ObjectNode connectionProperties = mapper.createObjectNode();
        connectionProperties.set("from", simpleStringSchema());
        connectionProperties.set("to", simpleStringSchema());
        connectionProperties.set("label", simpleStringSchema());
        connectionSchema.set("properties", connectionProperties);
        connectionSchema.set("required", mapper.valueToTree(List.of("from", "to", "label")));

        ObjectNode connections = mapper.createObjectNode();
        connections.put("type", "array");
        connections.set("items", connectionSchema);
        properties.set("connections", connections);

        ObjectNode footerTips = mapper.createObjectNode();
        footerTips.put("type", "array");
        footerTips.put("minItems", 2);
        footerTips.put("maxItems", 3);
        footerTips.set("items", simpleStringSchema());
        properties.set("footerTips", footerTips);

        schema.set("properties", properties);
        schema.set("required", mapper.valueToTree(List.of("title", "layout", "accentColor", "nodes", "connections", "footerTips")));
        responseFormat.set("json_schema", schema);
        return responseFormat;
    }

    private ObjectNode simpleStringSchema() {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "string");
        return schema;
    }

    private ObjectNode enumSchema(String... values) {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "string");
        schema.set("enum", mapper.valueToTree(values));
        return schema;
    }

    private ObjectNode message(String role, String content) {
        ObjectNode node = mapper.createObjectNode();
        node.put("role", role);
        node.put("content", content);
        return node;
    }

    private JsonNode normalizeResponseNode(JsonNode node) throws IOException {
        if (node.isObject()) {
            return node;
        }
        if (node.isTextual()) {
            return mapper.readTree(node.asText());
        }
        throw new IllegalStateException("Unexpected Cloudflare AI payload format.");
    }

    private SketchPlan sanitizePlan(SketchPlan plan, String prompt, String style) {
        String normalizedTitle = firstNonBlank(safe(plan.title()), safe(prompt), "AI Sketch");
        String normalizedLayout = normalizeLayout(plan.layout(), style);
        String accentColor = normalizeColor(plan.accentColor());

        List<SketchNode> sourceNodes = plan.nodes() == null ? List.of() : plan.nodes();
        List<SketchNode> nodes = new ArrayList<>();
        int index = 1;
        for (SketchNode node : sourceNodes) {
            if (node == null || safe(node.label()).isBlank()) {
                continue;
            }
            nodes.add(new SketchNode(
                    safe(node.id()).isBlank() ? "n" + index : safe(node.id()),
                    compact(node.label(), 28),
                    compact(firstNonBlank(node.detail(), node.label()), 70),
                    normalizeShape(node.shape()),
                    normalizeImportance(node.importance())
            ));
            index++;
            if (nodes.size() == 7) {
                break;
            }
        }

        if (nodes.size() < 3) {
            nodes = fallbackNodes(prompt, normalizedTitle);
        }

        Set<String> validIds = new HashSet<>();
        for (SketchNode node : nodes) {
            validIds.add(node.id());
        }

        List<SketchConnection> connections = new ArrayList<>();
        if (plan.connections() != null) {
            for (SketchConnection connection : plan.connections()) {
                if (connection == null) {
                    continue;
                }
                String from = safe(connection.from());
                String to = safe(connection.to());
                if (!validIds.contains(from) || !validIds.contains(to) || from.equals(to)) {
                    continue;
                }
                connections.add(new SketchConnection(from, to, compact(connection.label(), 22)));
            }
        }
        if (connections.isEmpty()) {
            if ("mindmap".equals(normalizedLayout)) {
                String hub = nodes.get(0).id();
                for (int i = 1; i < nodes.size(); i++) {
                    connections.add(new SketchConnection(hub, nodes.get(i).id(), ""));
                }
            } else {
                for (int i = 0; i < nodes.size() - 1; i++) {
                    connections.add(new SketchConnection(nodes.get(i).id(), nodes.get(i + 1).id(), ""));
                }
            }
        }

        List<String> footerTips = new ArrayList<>();
        if (plan.footerTips() != null) {
            for (String tip : plan.footerTips()) {
                String cleaned = compact(tip, 48);
                if (!cleaned.isBlank()) {
                    footerTips.add(cleaned);
                }
                if (footerTips.size() == 3) {
                    break;
                }
            }
        }
        if (footerTips.size() < 2) {
            footerTips.add("Keep labels short");
            footerTips.add("Link cause to effect");
        }

        return new SketchPlan(normalizedTitle, normalizedLayout, accentColor, nodes, connections, footerTips);
    }

    private List<SketchNode> fallbackNodes(String prompt, String title) {
        List<String> tokens = extractKeywords(prompt);
        List<SketchNode> nodes = new ArrayList<>();
        nodes.add(new SketchNode("n1", compact(title, 28), "Main concept", "rounded", "high"));
        for (int i = 0; i < tokens.size() && nodes.size() < 5; i++) {
            String token = tokens.get(i);
            nodes.add(new SketchNode("n" + (i + 2), compact(capitalize(token), 28), "Key idea", "rounded", "medium"));
        }
        while (nodes.size() < 3) {
            int next = nodes.size() + 1;
            nodes.add(new SketchNode("n" + next, "Point " + next, "Supporting idea", "rounded", "medium"));
        }
        return nodes;
    }

    private List<String> extractKeywords(String prompt) {
        List<String> result = new ArrayList<>();
        for (String token : safe(prompt).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ").split("\\s+")) {
            if (token.length() < 4 || result.contains(token)) {
                continue;
            }
            result.add(token);
            if (result.size() == 4) {
                break;
            }
        }
        return result;
    }

    private String normalizeLayout(String value, String style) {
        String layout = safe(value).toLowerCase(Locale.ROOT);
        if (Set.of("mindmap", "diagram", "process", "comparison", "timeline").contains(layout)) {
            return layout;
        }
        String requestedStyle = safe(style).toLowerCase(Locale.ROOT);
        if (requestedStyle.contains("mind")) {
            return "mindmap";
        }
        if (requestedStyle.contains("chalk")) {
            return "process";
        }
        if (requestedStyle.contains("diagram")) {
            return "diagram";
        }
        return "diagram";
    }

    private String normalizeShape(String value) {
        String shape = safe(value).toLowerCase(Locale.ROOT);
        return switch (shape) {
            case "circle", "pill" -> shape;
            default -> "rounded";
        };
    }

    private String normalizeImportance(String value) {
        String importance = safe(value).toLowerCase(Locale.ROOT);
        return switch (importance) {
            case "high", "low" -> importance;
            default -> "medium";
        };
    }

    private String normalizeColor(String value) {
        String color = safe(value).toUpperCase(Locale.ROOT);
        if (color.matches("^#[0-9A-F]{6}$")) {
            return color;
        }
        return "#8B5CF6";
    }

    private String extractErrorMessage(String rawBody) {
        try {
            JsonNode root = mapper.readTree(rawBody);
            JsonNode errors = root.path("errors");
            if (errors.isArray() && !errors.isEmpty()) {
                JsonNode message = errors.get(0).path("message");
                if (!message.isMissingNode() && !message.asText("").isBlank()) {
                    return message.asText();
                }
            }
            JsonNode result = root.path("result");
            if (!result.isMissingNode() && result.isTextual()) {
                return compact(result.asText(), 180);
            }
        } catch (Exception ignored) {
            // Fall back to raw body.
        }
        return compact(rawBody, 180);
    }

    private String defaultNegativePrompt() {
        return "low quality, blurry, extra limbs, duplicate objects, distorted perspective, cropped, text, watermark, logo, oversaturated, cluttered background, bad anatomy";
    }

    private String compact(String value, int maxLength) {
        String cleaned = safe(value).replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }

    private String capitalize(String value) {
        String cleaned = safe(value);
        if (cleaned.isBlank()) {
            return "";
        }
        return cleaned.substring(0, 1).toUpperCase(Locale.ROOT) + cleaned.substring(1);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String readWindowsUserEnvironmentVariable(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String os = System.getProperty("os.name", "");
        if (!os.toLowerCase(Locale.ROOT).contains("win")) {
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

            for (String line : output.split("\\R")) {
                String trimmed = line == null ? "" : line.trim();
                if (!trimmed.startsWith(name + " ")) {
                    continue;
                }
                String[] parts = trimmed.split("\\s{2,}");
                if (parts.length >= 3) {
                    return parts[2].trim();
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

    public record SketchPlan(
            String title,
            String layout,
            String accentColor,
            List<SketchNode> nodes,
            List<SketchConnection> connections,
            List<String> footerTips
    ) {}

    public record SketchNode(
            String id,
            String label,
            String detail,
            String shape,
            String importance
    ) {}

    public record SketchConnection(
            String from,
            String to,
            String label
    ) {}

    public record GeneratedScene(
            String title,
            String caption,
            String promptUsed,
            byte[] imageBytes
    ) {}

    public record EnhancedScenePrompt(
            String title,
            String caption,
            String prompt,
            String negativePrompt
    ) {}
}