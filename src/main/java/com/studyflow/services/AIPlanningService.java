package com.studyflow.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIPlanningService {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-3-5-haiku-latest";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final Pattern TEXT_BLOCK_PATTERN = Pattern.compile(
            "\"type\"\\s*:\\s*\"text\"\\s*,\\s*\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.DOTALL
    );

    private final HttpClient httpClient;

    public AIPlanningService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public ClaudeResult interpretPlanningCommand(String apiKey,
                                                 String userMessage,
                                                 LocalDate today,
                                                 List<String> knownSessionTitles,
                                                 List<String> knownSessionTypes) {
        if (apiKey == null || apiKey.isBlank()) {
            return ClaudeResult.error("Missing ANTHROPIC_API_KEY environment variable.");
        }
        if (userMessage == null || userMessage.isBlank()) {
            return ClaudeResult.error("Empty user message.");
        }

        try {
            String payload = buildPayload(userMessage, today, knownSessionTitles, knownSessionTypes);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return ClaudeResult.error("Claude API error (HTTP " + response.statusCode() + ").");
            }

            String responseText = extractAssistantText(response.body());
            if (responseText == null || responseText.isBlank()) {
                return ClaudeResult.error("Claude returned an empty response.");
            }

            ClaudeCommand command = parseStructuredJson(responseText);
            return ClaudeResult.success(command);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ClaudeResult.error("Claude request failed: " + ex.getMessage());
        } catch (IOException ex) {
            return ClaudeResult.error("Claude request failed: " + ex.getMessage());
        } catch (Exception ex) {
            return ClaudeResult.error("Claude parsing failed: " + ex.getMessage());
        }
    }

    private String buildPayload(String userMessage,
                                LocalDate today,
                                List<String> knownSessionTitles,
                                List<String> knownSessionTypes) {
        String systemPrompt = "You are a planning command parser for a student app. "
                + "Convert the user message into one strict JSON object only (no markdown, no prose). "
                + "Allowed actions: create, delete, update, help, unknown. "
                + "Output schema: "
                + "{\"action\":\"create|delete|update|help|unknown\",\"title\":string|null,\"sessionType\":string|null,"
                + "\"targetDate\":\"yyyy-MM-dd\"|null,\"date\":\"yyyy-MM-dd\"|null,\"startTime\":\"HH:mm\"|null,\"endTime\":\"HH:mm\"|null,\"message\":string}. "
                + "Rules: "
                + "1) For create, return title/sessionType/date/startTime. endTime can be null if absent. "
                + "2) For delete, return date and optional title. "
                + "3) For update, return targetDate (session to modify), and optional new date/startTime/endTime. "
                + "4) Resolve relative dates from Today=" + today + ". "
                + "5) Understand date inputs like yyyy-MM-dd, dd/MM/yyyy, dd-MM-yyyy and weekdays. "
                + "6) Understand time inputs like 14h, 2pm, 14:30 and convert to HH:mm. "
                + "7) Planning hours are 06:00 to 23:30. "
                + "8) Keep app language in English. "
                + "9) If user asks to create a new session, pick sessionType from known types when possible. "
                + "10) Return only the JSON object.";

        String titles = knownSessionTitles == null || knownSessionTitles.isEmpty()
                ? "none"
                : String.join(", ", knownSessionTitles);
        String types = knownSessionTypes == null || knownSessionTypes.isEmpty()
                ? "none"
                : String.join(", ", knownSessionTypes);

        String userContent = "Known session titles: " + titles
                + "\nKnown session types: " + types
                + "\nUser command: " + userMessage;

        return "{" +
                "\"model\":\"" + escapeJson(MODEL) + "\"," +
                "\"max_tokens\":220," +
                "\"temperature\":0," +
                "\"system\":\"" + escapeJson(systemPrompt) + "\"," +
                "\"messages\":[{" +
                "\"role\":\"user\"," +
                "\"content\":[{" +
                "\"type\":\"text\"," +
                "\"text\":\"" + escapeJson(userContent) + "\"" +
                "}]" +
                "}]" +
                "}";
    }

    private String extractAssistantText(String body) {
        StringBuilder builder = new StringBuilder();
        Matcher matcher = TEXT_BLOCK_PATTERN.matcher(body == null ? "" : body);
        while (matcher.find()) {
            String text = unescapeJson(matcher.group(1));
            if (text != null && !text.isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }
                builder.append(text);
            }
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    private ClaudeCommand parseStructuredJson(String raw) {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```") && cleaned.endsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }

        String objectPayload = extractFirstJsonObject(cleaned);
        if (objectPayload == null || objectPayload.isBlank()) {
            throw new IllegalArgumentException("No JSON object found in AI response");
        }

        String action = normalize(readJsonStringField(objectPayload, "action"));
        if (action.isBlank()) {
            action = "unknown";
        }

        String title = blankToNull(readJsonStringField(objectPayload, "title"));
        String sessionType = blankToNull(readJsonStringField(objectPayload, "sessionType"));
        String targetDate = blankToNull(readJsonStringField(objectPayload, "targetDate"));
        String date = blankToNull(readJsonStringField(objectPayload, "date"));
        String startTime = blankToNull(readJsonStringField(objectPayload, "startTime"));
        String endTime = blankToNull(readJsonStringField(objectPayload, "endTime"));
        String message = blankToNull(readJsonStringField(objectPayload, "message"));

        return new ClaudeCommand(action, title, sessionType, targetDate, date, startTime, endTime, message);
    }

    private String readJsonStringField(String jsonObject, String key) {
        if (jsonObject == null || jsonObject.isBlank() || key == null || key.isBlank()) {
            return null;
        }

        Pattern stringPattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL);
        Matcher stringMatcher = stringPattern.matcher(jsonObject);
        if (stringMatcher.find()) {
            return unescapeJson(stringMatcher.group(1));
        }

        Pattern nullPattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*null", Pattern.CASE_INSENSITIVE);
        if (nullPattern.matcher(jsonObject).find()) {
            return null;
        }

        return null;
    }

    private String extractFirstJsonObject(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        int start = -1;
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }

            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record ClaudeCommand(String action,
                                String title,
                                String sessionType,
                                String targetDate,
                                String date,
                                String startTime,
                                String endTime,
                                String message) {
        public ClaudeCommand {
            Objects.requireNonNull(action, "action");
        }
    }

    public static final class ClaudeResult {
        private final boolean success;
        private final ClaudeCommand command;
        private final String error;

        private ClaudeResult(boolean success, ClaudeCommand command, String error) {
            this.success = success;
            this.command = command;
            this.error = error;
        }

        public static ClaudeResult success(ClaudeCommand command) {
            return new ClaudeResult(true, command, null);
        }

        public static ClaudeResult error(String error) {
            return new ClaudeResult(false, null, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public ClaudeCommand getCommand() {
            return command;
        }

        public String getError() {
            return error;
        }
    }
}


