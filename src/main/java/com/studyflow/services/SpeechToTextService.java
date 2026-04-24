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
    private static final String[] OPENAI_STT_MODELS = {"gpt-4o-mini-transcribe", "whisper-1"};
    private static final String[] GROQ_STT_MODELS = {"whisper-large-v3-turbo", "whisper-large-v3"};
    private static final String[] OPENROUTER_STT_MODELS = {"whisper-1", "gpt-4o-mini-transcribe"};
    private static final Pattern TEXT_PATTERN =
            Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern TRANSCRIPT_PATTERN =
            Pattern.compile("\"transcript\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern LANGUAGE_PATTERN =
            Pattern.compile("\"language\"\\s*:\\s*\"([a-zA-Z\\-_]+)\"");

    public record TranscriptionResult(String text, String languageCode) {}

    private final HttpClient client;
    private final String openAiApiKey;
    private final String groqApiKey;
    private final String openRouterApiKey;
    private volatile String lastError;

    public SpeechToTextService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.openAiApiKey = firstNonBlank(
                System.getenv("OPENAI_API_KEY"),
                System.getenv("OPENAI_KEY"),
                System.getProperty("OPENAI_API_KEY"),
                System.getProperty("openai.api.key")
        );
        this.groqApiKey = firstNonBlank(
                System.getenv("GROQ_API_KEY"),
                System.getenv("GROQ_KEY"),
                System.getProperty("GROQ_API_KEY"),
                System.getProperty("groq.api.key")
        );
        this.openRouterApiKey = firstNonBlank(
                System.getenv("OPENROUTER_API_KEY"),
                System.getenv("OPENROUTER_KEY"),
                System.getProperty("OPENROUTER_API_KEY"),
                System.getProperty("openrouter.api.key")
        );
    }

    public boolean isConfigured() {
        return isNonBlank(openAiApiKey) || isNonBlank(groqApiKey) || isNonBlank(openRouterApiKey);
    }

    public String transcribeWav(byte[] wavData, String languageCode) {
        TranscriptionResult result = transcribeWavDetailed(wavData, languageCode);
        return result == null ? null : result.text();
    }

    public TranscriptionResult transcribeWavDetailed(byte[] wavData, String languageCode) {
        lastError = null;
        if (!isConfigured() || wavData == null || wavData.length == 0) {
            if (!isConfigured()) {
                lastError = "Speech API key is not configured.";
            } else {
                lastError = "No audio data captured from microphone.";
            }
            return null;
        }

        String endpoint;
        String[] modelsToTry;
        String provider;
        if (isNonBlank(groqApiKey)) {
            endpoint = "https://api.groq.com/openai/v1/audio/transcriptions";
            modelsToTry = GROQ_STT_MODELS;
            provider = "groq";
        } else if (isNonBlank(openAiApiKey)) {
            endpoint = "https://api.openai.com/v1/audio/transcriptions";
            modelsToTry = OPENAI_STT_MODELS;
            provider = "openai";
        } else {
            endpoint = "https://openrouter.ai/api/v1/audio/transcriptions";
            modelsToTry = OPENROUTER_STT_MODELS;
            provider = "openrouter";
        }

        for (String model : modelsToTry) {
            String boundary = "----StudyFlowSpeechBoundary" + System.currentTimeMillis();
            byte[] payload = buildMultipartPayload(boundary, wavData, languageCode, model);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload));

            if ("openai".equals(provider)) {
                requestBuilder.header("Authorization", "Bearer " + openAiApiKey);
            } else if ("groq".equals(provider)) {
                requestBuilder.header("Authorization", "Bearer " + groqApiKey);
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
                    lastError = "Speech API (" + model + ") HTTP " + response.statusCode() + ": " + shortBody(response.body());
                    continue;
                }
                TranscriptionResult result = extractTranscription(response.body());
                if (result == null || !isNonBlank(result.text())) {
                    lastError = "Speech API (" + model + ") returned empty transcription.";
                    continue;
                }
                return result;
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    lastError = "Speech API request interrupted.";
                    return null;
                }
                lastError = "Speech API request failed (" + model + "): " + e.getMessage();
            }
        }
        return null;
    }

    public String getLastError() {
        return lastError;
    }

    public String getConfiguredProviderLabel() {
        if (isNonBlank(groqApiKey)) {
            return "Groq";
        }
        if (isNonBlank(openAiApiKey)) {
            return "OpenAI";
        }
        if (isNonBlank(openRouterApiKey)) {
            return "OpenRouter";
        }
        return "none";
    }

    private byte[] buildMultipartPayload(String boundary, byte[] wavData, String languageCode, String model) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            appendTextPart(out, boundary, "model", isNonBlank(model) ? model : "whisper-1");
            appendTextPart(out, boundary, "response_format", "verbose_json");

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

    private TranscriptionResult extractTranscription(String body) {
        if (!isNonBlank(body)) {
            return null;
        }
        String text = null;
        Matcher matcher = TEXT_PATTERN.matcher(body);
        if (matcher.find()) {
            text = unescapeJson(matcher.group(1)).trim();
        } else {
            Matcher altMatcher = TRANSCRIPT_PATTERN.matcher(body);
            if (altMatcher.find()) {
                text = unescapeJson(altMatcher.group(1)).trim();
            }
        }
        if (!isNonBlank(text)) {
            return null;
        }

        String detectedLanguage = null;
        Matcher languageMatcher = LANGUAGE_PATTERN.matcher(body);
        if (languageMatcher.find()) {
            detectedLanguage = normalizeDetectedLanguage(languageMatcher.group(1));
        }
        return new TranscriptionResult(text, detectedLanguage);
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
                return value.trim();
            }
        }
        return null;
    }

    private String shortBody(String value) {
        if (!isNonBlank(value)) {
            return "(empty response)";
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.length() > 180 ? normalized.substring(0, 180) + "..." : normalized;
    }

    private String normalizeDetectedLanguage(String rawCode) {
        if (!isNonBlank(rawCode)) {
            return null;
        }
        String normalized = rawCode.trim().toLowerCase().replace('_', '-');
        if (normalized.startsWith("fr")) {
            return "fr-FR";
        }
        if (normalized.startsWith("ar-tn")) {
            return "ar-TN";
        }
        if (normalized.startsWith("ar")) {
            return "ar-SA";
        }
        if (normalized.startsWith("en")) {
            return "en-US";
        }
        return null;
    }
}
