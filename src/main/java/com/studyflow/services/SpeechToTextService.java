package com.studyflow.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public class SpeechToTextService {

    private static final String API_URL = "https://api.groq.com/openai/v1/audio/transcriptions";
    private static final String MODEL = "whisper-large-v3-turbo";
    private static final int MAX_RETRIES = 3;
    private final HttpClient httpClient;

    public SpeechToTextService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public TranscriptionResult transcribe(String apiKey, Path audioFile, String language) {
        if (apiKey == null || apiKey.isBlank()) {
            return TranscriptionResult.error("Missing GROQ_API_KEY environment variable.");
        }
        if (audioFile == null || !Files.exists(audioFile)) {
            return TranscriptionResult.error("Audio file not found.");
        }

        try {
            MultipartPayload payload = buildMultipartPayload(audioFile, language);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + payload.boundary())
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload.body()))
                    .build();

            HttpResponse<String> response = null;
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status != 429 && status != 503) {
                    break;
                }
                if (attempt < MAX_RETRIES) {
                    long waitMs = extractRetryAfterMillis(response);
                    if (waitMs <= 0) {
                        waitMs = (long) Math.pow(2, attempt - 1) * 1000L;
                    }
                    Thread.sleep(waitMs);
                }
            }

            if (response == null) {
                return TranscriptionResult.error("Speech-to-text API returned no response.");
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return TranscriptionResult.error(buildApiErrorMessage(response));
            }

            JsonObject root = JsonParser.parseString(response.body() == null ? "{}" : response.body()).getAsJsonObject();
            String text = root.has("text") ? root.get("text").getAsString() : "";
            if (text == null || text.isBlank()) {
                return TranscriptionResult.error("No transcription text returned by API.");
            }

            return TranscriptionResult.success(text.trim());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return TranscriptionResult.error("Transcription interrupted: " + exception.getMessage());
        } catch (Exception exception) {
            return TranscriptionResult.error("Transcription failed: " + exception.getMessage());
        }
    }

    private String buildApiErrorMessage(HttpResponse<String> response) {
        int status = response.statusCode();
        String body = response.body();
        String details = extractJsonErrorMessage(body);
        if (details == null || details.isBlank()) {
            details = body == null ? "" : body.trim();
        }
        if (details == null || details.isBlank()) {
            return "Speech-to-text API error (HTTP " + status + ").";
        }
        return "Speech-to-text API error (HTTP " + status + "): " + details;
    }

    private String extractJsonErrorMessage(String body) {
        try {
            if (body == null || body.isBlank()) {
                return null;
            }
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (root.has("error") && root.get("error").isJsonObject()) {
                JsonObject error = root.getAsJsonObject("error");
                if (error.has("message")) {
                    return error.get("message").getAsString();
                }
                if (error.has("type")) {
                    return error.get("type").getAsString();
                }
            }
            if (root.has("message")) {
                return root.get("message").getAsString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private long extractRetryAfterMillis(HttpResponse<String> response) {
        try {
            return response.headers()
                    .firstValue("retry-after")
                    .map(String::trim)
                    .map(value -> {
                        try {
                            return Long.parseLong(value) * 1000L;
                        } catch (NumberFormatException ignored) {
                            return -1L;
                        }
                    })
                    .orElse(-1L);
        } catch (Exception ignored) {
            return -1L;
        }
    }

    private MultipartPayload buildMultipartPayload(Path audioFile, String language) throws IOException {
        String boundary = "----StudyFlowBoundary" + UUID.randomUUID();
        byte[] fileBytes = Files.readAllBytes(audioFile);
        String contentType = Files.probeContentType(audioFile);
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeFormField(output, boundary, "model", MODEL);
        if (language != null && !language.isBlank()) {
            writeFormField(output, boundary, "language", language.trim());
        }
        writeFormField(output, boundary, "response_format", "json");
        writeFileField(output, boundary, "file", audioFile.getFileName().toString(), contentType, fileBytes);
        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        return new MultipartPayload(boundary, output.toByteArray());
    }

    private void writeFormField(ByteArrayOutputStream output, String boundary, String name, String value) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private void writeFileField(ByteArrayOutputStream output,
                                String boundary,
                                String fieldName,
                                String fileName,
                                String contentType,
                                byte[] fileBytes) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(fileBytes);
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private record MultipartPayload(String boundary, byte[] body) {}

    public static final class TranscriptionResult {
        private final boolean success;
        private final String text;
        private final String error;

        private TranscriptionResult(boolean success, String text, String error) {
            this.success = success;
            this.text = text;
            this.error = error;
        }

        public static TranscriptionResult success(String text) {
            return new TranscriptionResult(true, Objects.requireNonNull(text, "text"), null);
        }

        public static TranscriptionResult error(String error) {
            return new TranscriptionResult(false, null, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getText() {
            return text;
        }

        public String getError() {
            return error;
        }
    }
}
