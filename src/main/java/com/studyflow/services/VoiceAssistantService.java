package com.studyflow.services;

import javafx.application.Platform;

import javax.sound.sampled.*;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * VoiceAssistantService — Groq Edition
 *
 *  STT  : Groq Whisper large-v3
 *  LLM  : Groq llama-3.3-70b-versatile
 *  TTS  : Google Translate TTS via JavaFX MediaPlayer (0 PowerShell)
 */
public class VoiceAssistantService {

    // ── Groq endpoints ────────────────────────────────────────────
    private static final String GROQ_BASE      = "https://api.groq.com/openai/v1";
    private static final String GROQ_STT_URL   = GROQ_BASE + "/audio/transcriptions";
    private static final String GROQ_CHAT_URL  = GROQ_BASE + "/chat/completions";

    // ── Modèles ───────────────────────────────────────────────────
    private static final String STT_MODEL  = "whisper-large-v3";
    private static final String CHAT_MODEL = "llama-3.3-70b-versatile";

    // ── ⚠️  Remplace par ta clé Groq (console.groq.com) ──────────
    private static final String API_KEY = firstNonBlank(
            System.getenv("GROQ_API_KEY"),
            System.getProperty("groq.api.key"),
            loadGroqKeyFromDotEnv()
    );

    // ── Format audio optimal pour Whisper ────────────────────────
    private static final AudioFormat FORMAT_16K_MONO =
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    16000f, 16, 1, 2, 16000f, false);
    private static final AudioFormat FORMAT_44K_STEREO =
            new AudioFormat(44100f, 16, 2, true, false);

    // ── État ──────────────────────────────────────────────────────
    private final HttpClient      httpClient;
    private final ExecutorService executor;

    private TargetDataLine   micLine;
    private volatile boolean recording  = false;
    private long             recStartMs = 0;
    private File             wavFile;
    private String           selectedLanguage = "fr";

    // ── Callbacks UI ──────────────────────────────────────────────
    public Consumer<String> onTranscription;
    public Consumer<String> onAIResponse;
    public Consumer<String> onStatus;
    public Consumer<String> onError;
    public Runnable         onAudioStart;
    public Runnable         onAudioDone;

    // ─────────────────────────────────────────────────────────────
    public VoiceAssistantService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(20))
                .build();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "voice-worker");
            t.setDaemon(true);
            return t;
        });
    }

    public void    setLanguage(String code) { this.selectedLanguage = code; }
    public String  getLanguage()            { return selectedLanguage; }
    public boolean isRecording()            { return recording; }
    public boolean hasApiKey()              { return API_KEY != null && !API_KEY.isBlank() && !API_KEY.contains("METTEZ"); }

    // ══════════════════════════════════════════════════════════════
    //  ENREGISTREMENT MICRO
    // ══════════════════════════════════════════════════════════════

    public void startRecording() throws LineUnavailableException, IOException {
        if (recording) return;

        wavFile = File.createTempFile("voice_", ".wav");
        wavFile.deleteOnExit();

        AudioFormat chosenFormat;
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT_16K_MONO);
        if (AudioSystem.isLineSupported(info)) {
            chosenFormat = FORMAT_16K_MONO;
            System.out.println("[MIC] Format: 16kHz mono (optimal Whisper)");
        } else {
            info = new DataLine.Info(TargetDataLine.class, FORMAT_44K_STEREO);
            if (!AudioSystem.isLineSupported(info))
                throw new LineUnavailableException("Aucun microphone compatible.");
            chosenFormat = FORMAT_44K_STEREO;
            System.out.println("[MIC] Format: 44kHz stéréo (sera converti)");
        }

        micLine = (TargetDataLine) AudioSystem.getLine(info);
        micLine.open(chosenFormat);
        micLine.start();
        recording  = true;
        recStartMs = System.currentTimeMillis();

        final AudioFormat finalFormat = chosenFormat;
        Thread recThread = new Thread(() -> {
            try (AudioInputStream ais = new AudioInputStream(micLine)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavFile);
            } catch (IOException e) {
                System.err.println("[MIC] Erreur écriture: " + e.getMessage());
            }
        }, "mic-recorder");
        recThread.setDaemon(true);
        recThread.start();

        notifyStatus("🎙️ Enregistrement… Parlez maintenant");
    }

    public void stopAndProcess() {
        if (!recording) return;
        long elapsed = System.currentTimeMillis() - recStartMs;
        recording = false;
        if (micLine != null) { micLine.stop(); micLine.close(); }
        notifyStatus("⏳ Traitement…");

        long wait = Math.max(800, 1500 - elapsed);
        executor.submit(() -> {
            try { Thread.sleep(wait); } catch (InterruptedException ignored) {}
            processAudio();
        });
    }

    public void cancel() {
        recording = false;
        if (micLine != null) { micLine.stop(); micLine.close(); }
        notifyStatus("❌ Annulé");
    }

    // ══════════════════════════════════════════════════════════════
    //  PIPELINE PRINCIPAL
    // ══════════════════════════════════════════════════════════════

    private void processAudio() {
        try {
            long size = wavFile == null ? 0 : wavFile.length();
            System.out.println("[VOICE] WAV size: " + size + " bytes");

            if (size < 8000) {
                notifyError("Enregistrement trop court. Maintenez le bouton et parlez clairement.");
                return;
            }

            notifyStatus("🔍 Transcription Groq Whisper…");
            String text = transcribeWithGroq();

            if (text == null || text.isBlank()) {
                notifyError("Impossible de comprendre. Parlez plus fort et plus lentement.");
                return;
            }

            text = cleanTranscription(text);
            System.out.println("[VOICE] Transcription: '" + text + "'");

            if (text.isBlank()) {
                notifyError("Transcription vide après nettoyage. Réessayez.");
                return;
            }

            notifyOnMain(onTranscription, text);

            notifyStatus("🤖 Génération de la réponse (Groq)…");
            String reply = chatWithGroq(text);
            if (reply == null || reply.isBlank())
                reply = "Désolé, je n'ai pas pu générer une réponse.";

            System.out.println("[VOICE] Réponse IA: " + reply);
            notifyOnMain(onAIResponse, reply);

            if (onAudioStart != null) runOnMain(onAudioStart);
            notifyStatus("🔊 Lecture…");
            speakText(reply);
            if (onAudioDone != null) runOnMain(onAudioDone);

            notifyStatus("🎤 Prêt – cliquez sur le micro pour parler");

        } catch (Exception e) {
            e.printStackTrace();
            notifyError("Erreur : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ÉTAPE 1 — STT : GROQ WHISPER
    // ══════════════════════════════════════════════════════════════

    private String transcribeWithGroq() throws Exception {
        File audioFile = ensureOptimalFormat(wavFile);
        byte[] audioBytes = Files.readAllBytes(audioFile.toPath());
        System.out.println("[STT] Envoi à Groq Whisper: " + audioBytes.length + " bytes");

        String langCode   = getWhisperLangCode();
        String promptHint = getWhisperPromptHint();
        String boundary   = "----GB" + System.currentTimeMillis();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeField(baos, boundary, "model", STT_MODEL);
        if (!"auto".equals(langCode))
            writeField(baos, boundary, "language", langCode);
        writeField(baos, boundary, "response_format", "json");
        writeField(baos, boundary, "prompt", promptHint);
        writeFilePart(baos, boundary, "file", "audio.wav", "audio/wav", audioBytes);
        baos.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));

        byte[] body = baos.toByteArray();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_STT_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(java.time.Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("[STT] Groq status: " + resp.statusCode());
        System.out.println("[STT] Groq body: " + resp.body());

        if (resp.statusCode() == 200) {
            String text = parseWhisperResponse(resp.body());
            if (text != null && !text.isBlank()) return text;
        }

        System.err.println("[STT] Groq Whisper échoué. Vérifiez votre clé API.");
        throw new IOException("Groq Whisper failed: " + resp.statusCode() + " " + resp.body());
    }

    // ══════════════════════════════════════════════════════════════
    //  ÉTAPE 2 — LLM : GROQ CHAT
    // ══════════════════════════════════════════════════════════════

    private String chatWithGroq(String userText) throws Exception {
        String sysPrompt = switch (selectedLanguage) {
            case "en" -> "You are a helpful assistant for students. Answer every question thoroughly and completely in English.";
            case "ar" -> "أنت مساعد مفيد للطلاب. أجب على كل سؤال بشكل شامل وكامل باللغة العربية.";
            case "es" -> "Eres un asistente para estudiantes. Responde cada pregunta de forma completa en español.";
            case "de" -> "Du bist ein Assistent für Studenten. Beantworte jede Frage vollständig auf Deutsch.";
            default   -> "Tu es un assistant pour les étudiants. Réponds à chaque question de manière complète en français.";
        };

        String jsonBody = "{"
                + "\"model\":\"" + CHAT_MODEL + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escapeJson(sysPrompt) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escapeJson(userText) + "\"}"
                + "],"
                + "\"max_tokens\":1024,"
                + "\"temperature\":0.7"
                + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_CHAT_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("[CHAT] Groq status: " + resp.statusCode());
        System.out.println("[CHAT] Groq body: " + resp.body().substring(0, Math.min(300, resp.body().length())));

        if (resp.statusCode() == 200) {
            return extractChatContent(resp.body());
        }

        throw new IOException("Groq chat échoué: " + resp.statusCode() + " " + resp.body());
    }

    // ══════════════════════════════════════════════════════════════
    //  ÉTAPE 3 — TTS : GOOGLE TTS + JAVAFX MEDIAPLAYER (0 POWERSHELL)
    // ══════════════════════════════════════════════════════════════

    private void speakText(String text) {
        for (String chunk : splitIntoChunks(text, 180)) {
            if (chunk.isBlank()) continue;
            try {
                googleTTSChunk(chunk);
            } catch (Exception e) {
                System.err.println("[TTS] Erreur: " + e.getMessage());
                // Fallback silencieux — pas de PowerShell
            }
        }
    }

    private void googleTTSChunk(String chunk) throws Exception {
        String encoded = URLEncoder.encode(chunk, "UTF-8");
        String lang    = getGoogleLangCode();
        String url     = "https://translate.google.com/translate_tts"
                + "?ie=UTF-8&q=" + encoded + "&tl=" + lang + "&client=tw-ob";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(java.time.Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
        System.out.println("[TTS] Google status=" + resp.statusCode() + " size=" + resp.body().length);

        if (resp.statusCode() == 200 && resp.body().length > 100) {
            File mp3 = File.createTempFile("tts_", ".mp3");
            mp3.deleteOnExit();
            Files.write(mp3.toPath(), resp.body());
            playMp3WithJavaFX(mp3);
        } else {
            throw new IOException("Google TTS status: " + resp.statusCode());
        }
    }

    /**
     * Lecture MP3 via JavaFX MediaPlayer — aucun PowerShell requis.
     * Bloque jusqu'à la fin de la lecture.
     */
    private void playMp3WithJavaFX(File mp3) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                javafx.scene.media.Media media =
                        new javafx.scene.media.Media(mp3.toURI().toString());
                javafx.scene.media.MediaPlayer player =
                        new javafx.scene.media.MediaPlayer(media);

                player.setOnEndOfMedia(() -> {
                    player.dispose();
                    latch.countDown();
                });
                player.setOnError(() -> {
                    System.err.println("[TTS-FX] Erreur lecteur: " + player.getError());
                    latch.countDown();
                });
                player.play();
            } catch (Exception e) {
                System.err.println("[TTS-FX] " + e.getMessage());
                latch.countDown();
            }
        });

        // Attendre max 60 secondes
        latch.await(60, TimeUnit.SECONDS);
    }

    private List<String> splitIntoChunks(String text, int maxLen) {
        List<String> parts = new ArrayList<>();
        if (text == null || text.isBlank()) return parts;
        String[] sentences = text.split("(?<=[.!?;])\\s+");
        StringBuilder cur = new StringBuilder();
        for (String s : sentences) {
            if (cur.length() + s.length() + 1 <= maxLen) {
                if (cur.length() > 0) cur.append(" ");
                cur.append(s);
            } else {
                if (cur.length() > 0) { parts.add(cur.toString().trim()); cur.setLength(0); }
                if (s.length() > maxLen) {
                    String rem = s;
                    while (rem.length() > maxLen) {
                        int cut = rem.lastIndexOf(' ', maxLen);
                        if (cut == -1) cut = maxLen;
                        parts.add(rem.substring(0, cut).trim());
                        rem = rem.substring(cut).trim();
                    }
                    if (!rem.isBlank()) cur.append(rem);
                } else { cur.append(s); }
            }
        }
        if (cur.length() > 0) parts.add(cur.toString().trim());
        return parts;
    }

    // ══════════════════════════════════════════════════════════════
    //  CONVERSION AUDIO
    // ══════════════════════════════════════════════════════════════

    private File ensureOptimalFormat(File inputWav) {
        try (AudioInputStream src = AudioSystem.getAudioInputStream(inputWav)) {
            AudioFormat fmt = src.getFormat();

            boolean already16kMono =
                    fmt.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
                            && Math.abs(fmt.getSampleRate() - 16000f) < 1f
                            && fmt.getChannels() == 1
                            && fmt.getSampleSizeInBits() == 16;

            if (already16kMono) {
                System.out.println("[AUDIO] Déjà 16kHz mono — pas de conversion");
                return inputWav;
            }

            System.out.println("[AUDIO] Conversion vers 16kHz mono 16bit PCM…");

            AudioFormat pcm = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    fmt.getSampleRate(), 16,
                    fmt.getChannels(),
                    fmt.getChannels() * 2,
                    fmt.getSampleRate(), false);
            AudioInputStream pcmStream = AudioSystem.getAudioInputStream(pcm, src);

            AudioFormat target = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    16000f, 16, 1, 2, 16000f, false);
            AudioInputStream targetStream = AudioSystem.getAudioInputStream(target, pcmStream);

            File out = File.createTempFile("voice_opt_", ".wav");
            out.deleteOnExit();
            AudioSystem.write(targetStream, AudioFileFormat.Type.WAVE, out);
            System.out.println("[AUDIO] Converti: " + out.length() + " bytes");
            return out;

        } catch (Exception e) {
            System.err.println("[AUDIO] Conversion échouée: " + e.getMessage() + " → fichier original");
            return inputWav;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS MULTIPART
    // ══════════════════════════════════════════════════════════════

    private void writeField(ByteArrayOutputStream out, String boundary,
                            String name, String value) throws IOException {
        String part = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n";
        out.write(part.getBytes("UTF-8"));
    }

    private void writeFilePart(ByteArrayOutputStream out, String boundary,
                               String fieldName, String fileName,
                               String mimeType, byte[] data) throws IOException {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + fieldName
                + "\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: " + mimeType + "\r\n\r\n";
        out.write(header.getBytes("UTF-8"));
        out.write(data);
        out.write("\r\n".getBytes("UTF-8"));
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS JSON / LANGUE
    // ══════════════════════════════════════════════════════════════

    private String parseWhisperResponse(String body) {
        int idx = body.indexOf("\"text\":");
        if (idx == -1) return null;
        idx += 7;
        while (idx < body.length() && (body.charAt(idx) == ' ' || body.charAt(idx) == '\n')) idx++;
        if (idx >= body.length() || body.charAt(idx) != '"') return null;
        idx++;

        StringBuilder sb = new StringBuilder();
        for (int i = idx; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '\\' && i + 1 < body.length()) {
                char nx = body.charAt(++i);
                switch (nx) {
                    case '"'  -> sb.append('"');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    case '\\' -> sb.append('\\');
                    default   -> { sb.append('\\'); sb.append(nx); }
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    private String cleanTranscription(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        s = s.replaceAll("<\\|[^|]+\\|>", "");
        s = s.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}\\p{M}]", " ");
        s = s.replaceAll("\\s+", " ").trim();
        if (s.length() < 2) return "";
        return s;
    }

    private String extractChatContent(String json) {
        int msgIdx = json.indexOf("\"message\"");
        if (msgIdx != -1) {
            String val = extractJsonString(json.substring(msgIdx), "content");
            if (val != null && !val.isBlank()) return val.trim();
        }
        return extractJsonString(json, "content");
    }

    private String extractJsonString(String json, String key) {
        String token = "\"" + key + "\":\"";
        int start = json.indexOf(token);
        if (start == -1) return null;
        start += token.length();
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char nx = json.charAt(++i);
                switch (nx) {
                    case '"'  -> sb.append('"');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    case '\\' -> sb.append('\\');
                    default   -> { sb.append('\\'); sb.append(nx); }
                }
            } else if (c == '"') { break; }
            else { sb.append(c); }
        }
        String res = sb.toString().trim();
        return res.isEmpty() ? null : res;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String getWhisperLangCode() {
        return switch (selectedLanguage) {
            case "en" -> "en";
            case "ar" -> "ar";
            case "es" -> "es";
            case "de" -> "de";
            case "fr" -> "fr";
            default   -> "auto";
        };
    }

    private String getWhisperPromptHint() {
        return switch (selectedLanguage) {
            case "en" -> "Student question in English:";
            case "ar" -> "سؤال الطالب:";
            case "es" -> "Pregunta del estudiante en español:";
            case "de" -> "Studentenfrage auf Deutsch:";
            default   -> "Question de l'étudiant en français :";
        };
    }

    private String getGoogleLangCode() {
        return switch (selectedLanguage) {
            case "en" -> "en-US";
            case "ar" -> "ar-SA";
            case "es" -> "es-ES";
            case "de" -> "de-DE";
            default   -> "fr-FR";
        };
    }

    // ══════════════════════════════════════════════════════════════
    //  NOTIFICATIONS UI
    // ══════════════════════════════════════════════════════════════

    private void notifyStatus(String msg) { if (onStatus != null) runOnMain(() -> onStatus.accept(msg)); }
    private void notifyError(String msg)  { if (onError  != null) runOnMain(() -> onError.accept(msg));  }
    private void notifyOnMain(Consumer<String> c, String v) { if (c != null) runOnMain(() -> c.accept(v)); }

    private void runOnMain(Runnable r) {
        try { Platform.runLater(r); }
        catch (Exception e) { r.run(); }
    }

    public void shutdown() { cancel(); executor.shutdownNow(); }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String loadGroqKeyFromDotEnv() {
        for (String fileName : List.of(".env", "config/.env")) {
            Path path = Path.of(fileName);
            if (!Files.exists(path)) continue;
            try {
                for (String line : Files.readAllLines(path)) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("GROQ_API_KEY=")) {
                        String value = trimmed.substring("GROQ_API_KEY=".length()).trim();
                        if ((value.startsWith("\"") && value.endsWith("\""))
                                || (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        return value;
                    }
                }
            } catch (IOException ignored) {
                // Environment variable remains the primary configuration path.
            }
        }
        return "";
    }
}
