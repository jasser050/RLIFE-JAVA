package com.studyflow.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class WindowsSpeechRecognitionService {

    public boolean isSupported() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win");
    }

    public String recognizeOnce(String languageCode) {
        if (!isSupported()) {
            return null;
        }

        String culture = mapCulture(languageCode);
        String script = buildScript(culture);
        ProcessBuilder pb = new ProcessBuilder(
                "powershell",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-Command", script
        );

        try {
            Process process = pb.start();
            byte[] out = readAll(process.getInputStream());
            byte[] err = readAll(process.getErrorStream());
            int code = process.waitFor();
            if (code != 0) {
                return null;
            }

            String stdout = new String(out, StandardCharsets.UTF_8).trim();
            if (!stdout.isBlank()) {
                return stdout;
            }

            String stderr = new String(err, StandardCharsets.UTF_8).trim();
            if (!stderr.isBlank()) {
                return null;
            }
            return null;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    private String mapCulture(String languageCode) {
        if (languageCode == null || languageCode.isBlank() || "auto".equalsIgnoreCase(languageCode)) {
            return "";
        }
        if (languageCode.startsWith("fr")) {
            return "fr-FR";
        }
        if (languageCode.startsWith("ar")) {
            return "ar-SA";
        }
        return "en-US";
    }

    private String buildScript(String culture) {
        String safeCulture = culture == null ? "" : culture.replace("'", "");
        return """
                Add-Type -AssemblyName System.Speech;
                try {
                  if ('%s' -ne '') {
                    $ci = New-Object System.Globalization.CultureInfo('%s');
                    $r = New-Object System.Speech.Recognition.SpeechRecognitionEngine($ci);
                  } else {
                    $r = New-Object System.Speech.Recognition.SpeechRecognitionEngine;
                  }
                } catch {
                  $r = New-Object System.Speech.Recognition.SpeechRecognitionEngine;
                }
                $r.SetInputToDefaultAudioDevice();
                $g = New-Object System.Speech.Recognition.DictationGrammar;
                $r.LoadGrammar($g);
                $r.InitialSilenceTimeout = [TimeSpan]::FromSeconds(1.5);
                $r.BabbleTimeout = [TimeSpan]::FromSeconds(1.2);
                $r.EndSilenceTimeout = [TimeSpan]::FromSeconds(0.7);
                $result = $r.Recognize([TimeSpan]::FromSeconds(4));
                if ($result -and $result.Text) { [Console]::Write($result.Text) }
                """.formatted(safeCulture, safeCulture);
    }

    private byte[] readAll(InputStream in) throws IOException {
        return in.readAllBytes();
    }
}
