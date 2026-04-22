package com.studyflow.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpeechToTextService {
    private static final Pattern TEXT_PATTERN =
            Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern TRANSCRIPT_PATTERN =
            Pattern.compile("\"transcript\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private final HttpClient client;
    private final String openAiApiKey;
    private final String openRouterApiKey;

    public SpeechToTextService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.openAiApiKey = firstNonBlank(System.getenv("OPENAI_API_KEY"));
        this.openRouterApiKey = firstNonBlank(System.getenv("OPENROUTER_API_KEY"));
    }

    public boolean isConfigured() {
        return isNonBlank(openAiApiKey) || isNonBlank(openRouterApiKey);
    }

    public String transcribeWav(byte[] wavData, String languageCode) {
        if (!isConfigured() || wavData == null || wavData.length == 0) {
            return null;
        }

        String endpoint = isNonBlank(openAiApiKey)
                ? "https://api.openai.com/v1/audio/transcriptions"
                : "https://openrouter.ai/api/v1/audio/transcriptions";

        String boundary = "----StudyFlowSpeechBoundary" + System.currentTimeMillis();
        byte[] payload = buildMultipartPayload(boundary, wavData, languageCode);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload));

        if (isNonBlank(openAiApiKey)) {
            requestBuilder.header("Authorization", "Bearer " + openAiApiKey);
        } else {
            requestBuilder.header("Authorization", "Bearer " + openRouterApiKey);
            requestBuilder.header("HTTP-Referer", "http://localhost");
            requestBuilder.header("X-Title", "StudyFlow Wellbeing Voice Journal");
        }

        try {
            HttpResponse<String> response = client.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            return extractText(response.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    private byte[] buildMultipartPayload(String boundary, byte[] wavData, String languageCode) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            appendTextPart(out, boundary, "model", "whisper-1");
            appendTextPart(out, boundary, "response_format", "json");

            String lang = normalizeLanguage(languageCode);
            if (lang != null) {
                appendTextPart(out, boundary, "language", lang);
            }

            out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write("Content-Disposition: form-data; name=\"file\"; filename=\"voice.wav\"\r\n".getBytes(StandardCharsets.UTF_8));
            out.write("Content-Type: audio/wav\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(wavData);
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));

            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private void appendTextPart(ByteArrayOutputStream out, String boundary, String name, String value) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeLanguage(String languageCode) {
        if (!isNonBlank(languageCode) || "auto".equalsIgnoreCase(languageCode)) {
            return null;
        }
        if (languageCode.startsWith("en")) {
            return "en";
        }
        if (languageCode.startsWith("fr")) {
            return "fr";
        }
        if (languageCode.startsWith("ar")) {
            return "ar";
        }
        return null;
    }

    private String extractText(String body) {
        if (!isNonBlank(body)) {
            return null;
        }
        Matcher matcher = TEXT_PATTERN.matcher(body);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1)).trim();
        }
        Matcher altMatcher = TRANSCRIPT_PATTERN.matcher(body);
        if (altMatcher.find()) {
            return unescapeJson(altMatcher.group(1)).trim();
        }
        return null;
    }

    private String unescapeJson(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (isNonBlank(value)) {
                return value;
            }
        }
        return null;
    }
}
