package com.studyflow.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class VoiceAssistantService {
    private static final String GROQ_BASE = "https://api.groq.com/openai/v1";
    private static final String DEFAULT_GROQ_API_KEY = "api key" +
            "";

    private static final String GROQ_CHAT_MODEL = "llama-3.3-70b-versatile";

    private static final AudioFormat RECORDING_FORMAT =
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000f, 16, 1, 2, 16000f, false);
    private static final AudioFormat FALLBACK_RECORDING_FORMAT =
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 16, 2, 4, 44100f, false);

    private final HttpClient httpClient;
    private final ExecutorService executor;
    private String groqApiKey;

    private TargetDataLine micLine;
    private Thread recorderThread;
    private volatile boolean recording = false;
    private long recStartMs = 0;
    private File wavFile;
    private String selectedLanguage = "fr";
    private volatile boolean speakResponses = true;
    private volatile boolean speechCancelled = false;
    private volatile javafx.scene.media.MediaPlayer currentPlayer;
    private volatile CountDownLatch currentPlaybackLatch;
    private volatile Process currentSpeechProcess;

    public Consumer<String> onTranscription;
    public Consumer<String> onAIResponse;
    public Consumer<String> onStatus;
    public Consumer<String> onError;
    public Runnable onAudioStart;
    public Runnable onAudioDone;

    public VoiceAssistantService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "voice-worker");
            t.setDaemon(true);
            return t;
        });
        this.groqApiKey = firstNonBlank(
                DEFAULT_GROQ_API_KEY,
                configValue("GROQ_API_KEY", "GROQ_KEY", "groq.api.key")
        );
    }

    public void setLanguage(String code) {
        this.selectedLanguage = normalizeUiLanguage(code);
    }

    public String getLanguage() {
        return selectedLanguage;
    }

    public boolean isRecording() {
        return recording;
    }

    public boolean hasApiKey() {
        return hasSpeechProvider() && hasChatProvider();
    }

    public void setSpeakResponses(boolean speakResponses) {
        this.speakResponses = speakResponses;
        if (!speakResponses) {
            stopSpeaking();
        }
    }

    public boolean configureGroqApiKey(String apiKey, boolean saveToDotenv) {
        if (!isNonBlank(apiKey)) {
            return false;
        }
        String trimmed = apiKey.trim();
        this.groqApiKey = trimmed;
        if (saveToDotenv) {
            try {
                saveGroqApiKeyToDotenv(trimmed);
            } catch (IOException e) {
                System.err.println("[VoiceAssistant] Failed to save .env: " + e.getMessage());
            }
        }
        return true;
    }

    public String getConfiguredProviderLabel() {
        if (isNonBlank(groqApiKey)) return "Groq";
        return "none";
    }

    public void startRecording() throws LineUnavailableException, IOException {
        if (recording) return;

        if (!hasSpeechProvider()) {
            throw new LineUnavailableException("Cle GROQ_API_KEY introuvable. Ajoutez-la dans .env ou dans les variables d'environnement.");
        }

        wavFile = File.createTempFile("voice_", ".wav");
        wavFile.deleteOnExit();

        AudioFormat selectedFormat = RECORDING_FORMAT;
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, selectedFormat);
        if (!AudioSystem.isLineSupported(info)) {
            selectedFormat = FALLBACK_RECORDING_FORMAT;
            info = new DataLine.Info(TargetDataLine.class, selectedFormat);
        }
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Aucun microphone compatible n'a ete trouve.");
        }

        micLine = (TargetDataLine) AudioSystem.getLine(info);
        micLine.open(selectedFormat);
        micLine.start();
        recording = true;
        recStartMs = System.currentTimeMillis();

        recorderThread = new Thread(() -> {
            try (AudioInputStream ais = new AudioInputStream(micLine)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavFile);
            } catch (IOException e) {
                System.err.println("[VoiceAssistant] Audio write failed: " + e.getMessage());
            }
        }, "mic-recorder");
        recorderThread.setDaemon(true);
        recorderThread.start();

        notifyStatus("Recording... speak now");
    }

    public void stopAndProcess() {
        if (!recording) return;
        long elapsed = System.currentTimeMillis() - recStartMs;
        recording = false;
        closeMicLine();
        notifyStatus("Processing...");

        executor.submit(() -> {
            waitForRecorder();
            long wait = Math.max(250, 900 - elapsed);
            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            processAudio();
        });
    }

    public void cancel() {
        recording = false;
        closeMicLine();
        waitForRecorder();
        notifyStatus("Cancelled");
    }

    public void shutdown() {
        cancel();
        executor.shutdownNow();
    }

    private void processAudio() {
        try {
            long size = wavFile == null ? 0 : wavFile.length();
            if (size < 6000) {
                notifyError("Enregistrement trop court. Parlez clairement pendant au moins une seconde.");
                return;
            }

            notifyStatus("Transcription...");
            String text = transcribeWithBestProvider(Files.readAllBytes(wavFile.toPath()));
            text = cleanTranscription(text);
            if (text.isBlank()) {
                notifyError("Impossible de comprendre l'audio. Rapprochez-vous du micro et reessayez.");
                return;
            }
            notifyOnMain(onTranscription, text);

            notifyStatus("Generation de la reponse...");
            String reply = chatWithBestProvider(text);
            if (!isNonBlank(reply)) {
                notifyError("Aucune reponse IA recue. Verifiez votre cle API et votre connexion.");
                return;
            }
            reply = normalizeAssistantReply(reply);
            notifyOnMain(onAIResponse, reply);

            if (speakResponses) {
                if (onAudioStart != null) runOnMain(onAudioStart);
                notifyStatus("Lecture audio...");
                speakText(reply);
            }
            if (onAudioDone != null) runOnMain(onAudioDone);

            notifyStatus("Ready - click mic to ask");
        } catch (Exception e) {
            e.printStackTrace();
            notifyError("Erreur assistant vocal: " + e.getMessage());
        }
    }

    private String transcribeWithBestProvider(byte[] wavData) throws Exception {
        List<ProviderAttempt> attempts = new ArrayList<>();
        if (isNonBlank(groqApiKey)) {
            attempts.add(new ProviderAttempt("Groq", GROQ_BASE + "/audio/transcriptions", groqApiKey,
                    List.of("whisper-large-v3-turbo", "whisper-large-v3")));
        }
        String lastError = "Cle GROQ_API_KEY introuvable.";
        for (ProviderAttempt attempt : attempts) {
            for (String model : attempt.models()) {
                try {
                    String text = transcribe(attempt.endpoint(), attempt.apiKey(), model, wavData);
                    if (isNonBlank(text)) return text;
                    lastError = attempt.name() + " returned empty transcription.";
                } catch (Exception e) {
                    lastError = attempt.name() + " " + model + ": " + e.getMessage();
                }
            }
        }
        throw new IOException(lastError);
    }

    private String transcribe(String endpoint, String apiKey, String model, byte[] wavData) throws Exception {
        String boundary = "----RLifeVoice" + System.currentTimeMillis();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeField(body, boundary, "model", model);
        writeField(body, boundary, "response_format", "json");
        String lang = whisperLanguageCode();
        if (lang != null) writeField(body, boundary, "language", lang);
        writeFilePart(body, boundary, "file", "voice.wav", "audio/wav", wavData);
        body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()));
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + ": " + shortBody(response.body()));
        }
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        return json.has("text") ? json.get("text").getAsString() : null;
    }

    private String chatWithBestProvider(String userText) throws Exception {
        String systemPrompt = systemPrompt();
        if (isNonBlank(groqApiKey)) {
            return chatWithGroq(systemPrompt, userText);
        }
        throw new IOException("Cle GROQ_API_KEY introuvable.");
    }

    private String chatWithGroq(String systemPrompt, String userText) throws Exception {
        JsonObject root = new JsonObject();
        root.addProperty("model", GROQ_CHAT_MODEL);
        root.addProperty("temperature", 0.4);
        root.addProperty("max_tokens", 1200);

        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", systemPrompt);
        messages.add(system);
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userText);
        messages.add(user);
        root.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_BASE + "/chat/completions"))
                .timeout(Duration.ofSeconds(35))
                .header("Authorization", "Bearer " + groqApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(root.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + ": " + shortBody(response.body()));
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray choices = json.getAsJsonArray("choices");
        if (choices == null || choices.size() == 0) return null;
        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        return message == null || !message.has("content") ? null : message.get("content").getAsString().trim();
    }

    private void speakText(String text) {
        speechCancelled = false;
        boolean playedAny = false;
        for (String chunk : splitIntoChunks(text, 180)) {
            if (speechCancelled) break;
            if (chunk.isBlank()) continue;
            try {
                googleTtsChunk(chunk, ttsLanguageCode(chunk));
                playedAny = true;
            } catch (Exception e) {
                System.err.println("[VoiceAssistant] Google TTS failed: " + e.getMessage());
            }
        }
        if (!playedAny && !speechCancelled) {
            try {
                speakWithWindowsSapi(text, ttsLanguageCode(text));
            } catch (Exception e) {
                System.err.println("[VoiceAssistant] Windows TTS failed: " + e.getMessage());
            }
        }
    }

    public void speakAsync(String text) {
        if (!isNonBlank(text)) return;
        executor.submit(() -> {
            if (onAudioStart != null) runOnMain(onAudioStart);
            notifyStatus("Lecture audio...");
            speakText(text);
            if (onAudioDone != null) runOnMain(onAudioDone);
            notifyStatus("Ready - click mic to ask");
        });
    }

    public void stopSpeaking() {
        speechCancelled = true;
        CountDownLatch latch = currentPlaybackLatch;
        if (latch != null) {
            latch.countDown();
        }
        Process process = currentSpeechProcess;
        if (process != null) {
            process.destroyForcibly();
            currentSpeechProcess = null;
        }
        javafx.scene.media.MediaPlayer player = currentPlayer;
        if (player != null) {
            Platform.runLater(() -> {
                try {
                    player.stop();
                    player.dispose();
                } catch (Exception ignored) {
                }
            });
            currentPlayer = null;
        }
        notifyStatus("Lecture stoppee");
        if (onAudioDone != null) runOnMain(onAudioDone);
    }

    private void googleTtsChunk(String chunk, String languageCode) throws Exception {
        String encoded = URLEncoder.encode(chunk, StandardCharsets.UTF_8);
        String url = "https://translate.google.com/translate_tts"
                + "?ie=UTF-8&q=" + encoded + "&tl=" + languageCode + "&client=tw-ob";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200 || response.body().length < 100) {
            throw new IOException("HTTP " + response.statusCode());
        }
        File mp3 = File.createTempFile("tts_", ".mp3");
        mp3.deleteOnExit();
        Files.write(mp3.toPath(), response.body());
        playMp3WithJavaFx(mp3);
    }

    private void playMp3WithJavaFx(File mp3) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        currentPlaybackLatch = latch;
        Platform.runLater(() -> {
            try {
                javafx.scene.media.Media media = new javafx.scene.media.Media(mp3.toURI().toString());
                javafx.scene.media.MediaPlayer player = new javafx.scene.media.MediaPlayer(media);
                currentPlayer = player;
                player.setOnEndOfMedia(() -> {
                    player.dispose();
                    currentPlayer = null;
                    latch.countDown();
                });
                player.setOnError(() -> {
                    System.err.println("[VoiceAssistant] MediaPlayer failed: " + player.getError());
                    player.dispose();
                    currentPlayer = null;
                    latch.countDown();
                });
                if (!speechCancelled) {
                    player.play();
                } else {
                    player.dispose();
                    currentPlayer = null;
                    latch.countDown();
                }
            } catch (Exception e) {
                System.err.println("[VoiceAssistant] Media init failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await(60, TimeUnit.SECONDS);
        currentPlaybackLatch = null;
    }

    private void speakWithWindowsSapi(String text, String languageCode) throws Exception {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return;
        String escapedText = text.replace("'", "''");
        String escapedLang = languageCode.replace("'", "''");
        String script = """
                Add-Type -AssemblyName System.Speech;
                $speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer;
                try {
                    $culture = New-Object System.Globalization.CultureInfo('%s');
                    $speaker.SelectVoiceByHints([System.Speech.Synthesis.VoiceGender]::NotSet, [System.Speech.Synthesis.VoiceAge]::NotSet, 0, $culture);
                } catch {}
                $speaker.Speak('%s');
                """.formatted(escapedLang, escapedText);
        Process process = new ProcessBuilder("powershell", "-NoProfile", "-Command", script)
                .redirectErrorStream(true)
                .start();
        currentSpeechProcess = process;
        process.waitFor(90, TimeUnit.SECONDS);
        currentSpeechProcess = null;
    }

    private List<String> splitIntoChunks(String text, int maxLen) {
        List<String> parts = new ArrayList<>();
        if (!isNonBlank(text)) return parts;
        String[] sentences = text.split("(?<=[.!?;])\\s+");
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            if (current.length() + sentence.length() + 1 <= maxLen) {
                if (current.length() > 0) current.append(' ');
                current.append(sentence);
                continue;
            }
            if (current.length() > 0) {
                parts.add(current.toString().trim());
                current.setLength(0);
            }
            while (sentence.length() > maxLen) {
                int split = sentence.lastIndexOf(' ', maxLen);
                if (split < 1) split = maxLen;
                parts.add(sentence.substring(0, split).trim());
                sentence = sentence.substring(split).trim();
            }
            if (!sentence.isBlank()) current.append(sentence);
        }
        if (current.length() > 0) parts.add(current.toString().trim());
        return parts;
    }

    private void closeMicLine() {
        if (micLine == null) return;
        try {
            micLine.stop();
            micLine.close();
        } catch (Exception ignored) {
        }
    }

    private void waitForRecorder() {
        Thread thread = recorderThread;
        if (thread == null) return;
        try {
            thread.join(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void writeField(ByteArrayOutputStream out, String boundary, String name, String value) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private void writeFilePart(ByteArrayOutputStream out, String boundary, String fieldName,
                               String fileName, String mimeType, byte[] data) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + fieldName
                + "\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(data);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String cleanTranscription(String raw) {
        if (raw == null) return "";
        String value = raw.replaceAll("<\\|[^|]+\\|>", " ").trim();
        value = value.replaceAll("\\s+", " ");
        return value.length() < 2 ? "" : value;
    }

    private String normalizeAssistantReply(String raw) {
        if (raw == null) return "";
        String value = raw
                .replace("*", "")
                .replace("•", "")
                .replaceAll("(?m)^\\s*[-+]\\s+", "")
                .replaceAll("(?m)^\\s*\\d+[.)]\\s+", "")
                .replaceAll("\\s+", " ")
                .trim();
        return value;
    }

    private String systemPrompt() {
        String formatRule = " Do not use Markdown, bullet points, numbered lists, asterisks, titles, or symbols. Answer in one complete, detailed paragraph with enough explanation to be useful.";
        return switch (selectedLanguage) {
            case "auto" -> "You are RLife's multilingual voice assistant for students. Detect the user's language from the message and answer in that same language." + formatRule;
            case "en" -> "You are RLife's voice assistant for students. Answer clearly and helpfully in English." + formatRule;
            case "ar" -> "You are RLife's voice assistant for students. Answer clearly and helpfully in Arabic." + formatRule;
            case "es" -> "You are RLife's voice assistant for students. Answer clearly and helpfully in Spanish." + formatRule;
            case "de" -> "You are RLife's voice assistant for students. Answer clearly and helpfully in German." + formatRule;
            case "it" -> "You are RLife's voice assistant for students. Answer clearly and helpfully in Italian." + formatRule;
            case "pt" -> "You are RLife's voice assistant for students. Answer clearly and helpfully in Portuguese." + formatRule;
            case "tr" -> "You are RLife's voice assistant for students. Answer clearly and helpfully in Turkish." + formatRule;
            default -> "Tu es l'assistant vocal RLife pour les etudiants. Reponds clairement et utilement en francais. N'utilise pas Markdown, listes, numeros, asterisques, titres ou symboles. Reponds en un seul paragraphe complet et detaille avec assez d'explication pour etre utile.";
        };
    }

    private String whisperLanguageCode() {
        return switch (selectedLanguage) {
            case "en", "ar", "es", "de", "fr", "it", "pt", "tr" -> selectedLanguage;
            default -> null;
        };
    }

    private String googleLanguageCode() {
        return switch (selectedLanguage) {
            case "en" -> "en-US";
            case "ar" -> "ar-SA";
            case "es" -> "es-ES";
            case "de" -> "de-DE";
            case "it" -> "it-IT";
            case "pt" -> "pt-PT";
            case "tr" -> "tr-TR";
            default -> "fr-FR";
        };
    }

    private String ttsLanguageCode(String text) {
        if (!"auto".equals(selectedLanguage)) {
            return googleLanguageCode();
        }
        if (text != null && text.matches(".*[\\u0600-\\u06FF].*")) {
            return "ar-SA";
        }
        return "fr-FR";
    }

    private static String normalizeUiLanguage(String code) {
        if (!isNonBlank(code)) return "fr";
        String normalized = code.toLowerCase();
        if (normalized.contains("auto")) return "auto";
        if (normalized.contains("english") || normalized.startsWith("en")) return "en";
        if (normalized.contains("arab") || normalized.startsWith("ar")) return "ar";
        if (normalized.contains("spanish") || normalized.contains("espan") || normalized.startsWith("es")) return "es";
        if (normalized.contains("german") || normalized.startsWith("de")) return "de";
        if (normalized.contains("ital") || normalized.startsWith("it")) return "it";
        if (normalized.contains("port") || normalized.startsWith("pt")) return "pt";
        if (normalized.contains("turk") || normalized.startsWith("tr")) return "tr";
        return "fr";
    }

    private boolean hasSpeechProvider() {
        return isNonBlank(groqApiKey);
    }

    private boolean hasChatProvider() {
        return hasSpeechProvider();
    }

    private void notifyStatus(String msg) {
        if (onStatus != null) runOnMain(() -> onStatus.accept(msg));
    }

    private void notifyError(String msg) {
        if (onError != null) runOnMain(() -> onError.accept(msg));
    }

    private void notifyOnMain(Consumer<String> callback, String value) {
        if (callback != null) runOnMain(() -> callback.accept(value));
    }

    private void runOnMain(Runnable runnable) {
        try {
            Platform.runLater(runnable);
        } catch (Exception e) {
            runnable.run();
        }
    }

    private static String configValue(String envName, String altEnvName, String propertyName) {
        return firstNonBlank(
                System.getenv(envName),
                System.getenv(altEnvName),
                System.getProperty(envName),
                System.getProperty(propertyName),
                loadFromDotEnv(envName),
                loadFromDotEnv(altEnvName),
                readWindowsUserEnvironmentVariable(envName),
                readWindowsUserEnvironmentVariable(altEnvName)
        );
    }

    private static String loadFromDotEnv(String key) {
        if (!isNonBlank(key)) return null;
        for (Path path : dotenvCandidates()) {
            if (!Files.exists(path)) continue;
            try {
                for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("#") || !trimmed.startsWith(key + "=")) continue;
                    String value = trimmed.substring((key + "=").length()).trim();
                    if ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    return value;
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private static List<Path> dotenvCandidates() {
        List<Path> candidates = new ArrayList<>();
        addDotenvCandidates(candidates, Path.of("").toAbsolutePath().normalize());

        try {
            URI codeUri = VoiceAssistantService.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI();
            Path codePath = Path.of(codeUri).toAbsolutePath().normalize();
            addDotenvCandidates(candidates, codePath);
            addDotenvCandidates(candidates, codePath.getParent());
            Path current = codePath;
            for (int i = 0; i < 5 && current != null; i++) {
                if (Files.exists(current.resolve("pom.xml"))) {
                    addDotenvCandidates(candidates, current);
                    break;
                }
                current = current.getParent();
            }
        } catch (Exception ignored) {
        }

        return candidates.stream().distinct().toList();
    }

    private static void saveGroqApiKeyToDotenv(String apiKey) throws IOException {
        Path dotenv = defaultDotenvPath();
        List<String> lines = Files.exists(dotenv)
                ? new ArrayList<>(Files.readAllLines(dotenv, StandardCharsets.UTF_8))
                : new ArrayList<>();
        boolean replaced = false;
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("GROQ_API_KEY=") || trimmed.startsWith("GROQ_KEY=")) {
                lines.set(i, "GROQ_API_KEY=" + apiKey);
                replaced = true;
            }
        }
        if (!replaced) {
            if (!lines.isEmpty() && !lines.get(lines.size() - 1).isBlank()) {
                lines.add("");
            }
            lines.add("GROQ_API_KEY=" + apiKey);
        }
        Files.createDirectories(dotenv.getParent());
        Files.write(dotenv, lines, StandardCharsets.UTF_8);
    }

    private static Path defaultDotenvPath() {
        for (Path candidate : dotenvCandidates()) {
            Path base = candidate.getParent();
            if (base != null && Files.exists(base.resolve("pom.xml"))) {
                return base.resolve(".env");
            }
        }
        Path current = Path.of("").toAbsolutePath().normalize();
        Path probe = current;
        while (probe != null) {
            if (Files.exists(probe.resolve("pom.xml"))) {
                return probe.resolve(".env");
            }
            probe = probe.getParent();
        }
        return current.resolve(".env");
    }

    private static void addDotenvCandidates(List<Path> candidates, Path base) {
        if (base == null) return;
        candidates.add(base.resolve(".env").normalize());
        candidates.add(base.resolve("config").resolve(".env").normalize());
        Path parent = base.getParent();
        if (parent != null) {
            candidates.add(parent.resolve(".env").normalize());
            candidates.add(parent.resolve("config").resolve(".env").normalize());
        }
    }

    private static String readWindowsUserEnvironmentVariable(String name) {
        if (!isNonBlank(name) || !System.getProperty("os.name", "").toLowerCase().contains("win")) return null;
        Process process = null;
        try {
            process = new ProcessBuilder("reg", "query", "HKCU\\Environment", "/v", name)
                    .redirectErrorStream(true)
                    .start();
            String output;
            try (InputStream stream = process.getInputStream()) {
                output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            if (process.waitFor() != 0 || !isNonBlank(output)) return null;
            for (String line : output.split("\\R")) {
                String trimmed = line.trim();
                if (!trimmed.startsWith(name + " ")) continue;
                String[] parts = trimmed.split("\\s{2,}");
                if (parts.length >= 3) return parts[2].trim();
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        } finally {
            if (process != null) process.destroy();
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (isNonBlank(value)) return value.trim();
        }
        return null;
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String shortBody(String value) {
        if (!isNonBlank(value)) return "(empty response)";
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.length() > 220 ? normalized.substring(0, 220) + "..." : normalized;
    }

    private record ProviderAttempt(String name, String endpoint, String apiKey, List<String> models) {
    }
}
