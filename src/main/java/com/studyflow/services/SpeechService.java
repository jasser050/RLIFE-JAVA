package com.studyflow.services;

public class SpeechService {

    public void speakAsync(String text, String languageTag) {
        if (text == null || text.isBlank()) {
            return;
        }

        Thread thread = new Thread(() -> {
            try {
                speak(text, languageTag);
            } catch (Exception e) {
                System.err.println("[SpeechService] Speech failed: " + e.getMessage());
            }
        }, "flashcard-speech");
        thread.setDaemon(true);
        thread.start();
    }

    private void speak(String text, String languageTag) throws Exception {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            throw new UnsupportedOperationException("Speech is only configured for Windows in this app.");
        }

        String escapedText = text.replace("'", "''");
        String escapedLang = (languageTag == null || languageTag.isBlank() ? "en-US" : languageTag).replace("'", "''");
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
        process.waitFor();
    }
}
