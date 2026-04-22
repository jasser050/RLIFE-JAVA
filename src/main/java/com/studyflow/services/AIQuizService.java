package com.studyflow.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Service AI Quiz utilisant OpenRouter.
 * Obtenir votre clé API : https://openrouter.ai/keys
 *
 * IMPORTANT POUR GIT : Stocker la clé dans une variable d'environnement :
 *   Windows : setx OPENROUTER_API_KEY "sk-or-..."
 *   Linux/Mac : export OPENROUTER_API_KEY="sk-or-..."
 *
 * Puis dans le code : System.getenv("OPENROUTER_API_KEY")
 */
public class AIQuizService {

    // ✅ Clé lue depuis variable d'environnement
    private static final String API_KEY = System.getenv("OPENROUTER_API_KEY") != null
            ? System.getenv("OPENROUTER_API_KEY")
            : "//METTEZ_VOTRE_CLE_OPENROUTER_ICI"; //METTEZ_VOTRE_CLE_OPENROUTER_ICI  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

    // Modèles gratuits disponibles sur OpenRouter :
    // "mistralai/mistral-7b-instruct:free"
    // "google/gemma-3-27b-it:free"
    // "meta-llama/llama-3.1-8b-instruct:free"
    // "deepseek/deepseek-chat-v3-0324:free"
// Remplacez la ligne 28 par celle-ci :
    private static final String MODEL = "openrouter/free";    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";

    /**
     * Génère un quiz AI via OpenRouter pour une matière et section donnée.
     */
    public List<ParsedQuestion> generateQuizQuestions(String matiere, String section, String level, int count) throws Exception {
        String prompt = buildPrompt(matiere, section, level, count);

        // Format OpenAI-compatible (utilisé par OpenRouter)
        String jsonBody = "{"
                + "\"model\": \"" + MODEL + "\","
                + "\"max_tokens\": 2048,"
                + "\"messages\": ["
                + "  {"
                + "    \"role\": \"user\","
                + "    \"content\": " + escapeJson(prompt)
                + "  }"
                + "]"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENROUTER_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .header("HTTP-Referer", "https://studyflow.app")   // recommandé par OpenRouter
                .header("X-Title", "StudyFlow")                     // nom de l'app
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API Error " + response.statusCode() + ": " + response.body());
        }

        String responseBody = response.body();
        String textContent = extractTextFromOpenRouterResponse(responseBody);
        return parseQuizResponse(textContent);
    }

    /**
     * Ancienne méthode compatible si besoin
     */
    public String generateQuiz(String matiere, String section, String level) throws Exception {
        List<ParsedQuestion> questions = generateQuizQuestions(matiere, section, level, 10);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            ParsedQuestion q = questions.get(i);
            sb.append((i + 1)).append(". ").append(q.question).append("\n");
            for (int j = 0; j < q.options.size(); j++) {
                sb.append((char) ('A' + j)).append(") ").append(q.options.get(j)).append("\n");
            }
            sb.append("Answer: ").append(q.correct).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * Prompt structuré — force JSON pour parsing fiable.
     */
    private String buildPrompt(String matiere, String section, String level, int count) {
        return "You are an expert teacher. Generate exactly " + count + " multiple choice questions "
                + "for a student studying the subject: \"" + matiere + "\" "
                + "in the section: \"" + section + "\". "
                + "The difficulty level is: " + level + ".\n\n"
                + "Return ONLY a valid JSON array, no explanation, no markdown. Format:\n"
                + "[\n"
                + "  {\n"
                + "    \"question\": \"What is ...?\",\n"
                + "    \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],\n"
                + "    \"correct\": \"Option A\",\n"
                + "    \"difficulty\": \"" + level + "\",\n"
                + "    \"category\": \"" + matiere + "\"\n"
                + "  }\n"
                + "]\n\n"
                + "Generate exactly " + count + " questions. Return ONLY the JSON array.";
    }

    /**
     * Extrait le texte de la réponse OpenRouter (format OpenAI-compatible).
     * Format: {"choices": [{"message": {"content": "..."}}]}
     */
    private String extractTextFromOpenRouterResponse(String jsonResponse) {
        // Chercher "content" dans choices[0].message.content
        int choicesIdx = jsonResponse.indexOf("\"choices\"");
        if (choicesIdx == -1) {
            throw new RuntimeException("Unexpected response format: " + jsonResponse);
        }

        int contentIdx = jsonResponse.indexOf("\"content\"", choicesIdx);
        if (contentIdx == -1) {
            throw new RuntimeException("No content in response: " + jsonResponse);
        }

        int colonIdx = jsonResponse.indexOf(':', contentIdx + 9);
        if (colonIdx == -1) {
            throw new RuntimeException("Malformed content field: " + jsonResponse);
        }

        // Trouver le premier guillemet après ':'
        int quoteStart = jsonResponse.indexOf('"', colonIdx + 1);
        if (quoteStart == -1) {
            throw new RuntimeException("No string value for content: " + jsonResponse);
        }

        int quoteEnd = findJsonStringEnd(jsonResponse, quoteStart + 1);
        String rawText = jsonResponse.substring(quoteStart + 1, quoteEnd);

        // Dé-échapper les caractères JSON
        return rawText
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\/", "/");
    }

    /**
     * Trouve la fin d'une string JSON (gère les caractères échappés).
     */
    private int findJsonStringEnd(String json, int start) {
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == '"') {
                return i;
            } else {
                i++;
            }
        }
        return json.length();
    }

    /**
     * Parse la réponse JSON en liste de ParsedQuestion.
     */
    private List<ParsedQuestion> parseQuizResponse(String jsonText) {
        List<ParsedQuestion> questions = new ArrayList<>();

        // Nettoyer les backticks markdown si le modèle en a ajouté
        jsonText = jsonText.trim();
        if (jsonText.startsWith("```json")) jsonText = jsonText.substring(7);
        if (jsonText.startsWith("```"))     jsonText = jsonText.substring(3);
        if (jsonText.endsWith("```"))       jsonText = jsonText.substring(0, jsonText.length() - 3);
        jsonText = jsonText.trim();

        // Parser chaque bloc { ... }
        int i = 0;
        while (i < jsonText.length()) {
            int objStart = jsonText.indexOf('{', i);
            if (objStart == -1) break;

            int objEnd = findMatchingBrace(jsonText, objStart);
            if (objEnd == -1) break;

            String obj = jsonText.substring(objStart, objEnd + 1);
            ParsedQuestion q = parseQuestion(obj);
            if (q != null && !q.question.isEmpty()) {
                questions.add(q);
            }

            i = objEnd + 1;
        }

        return questions;
    }

    /**
     * Parse un objet JSON individuel représentant une question.
     */
    private ParsedQuestion parseQuestion(String obj) {
        try {
            ParsedQuestion q = new ParsedQuestion();
            q.question   = extractJsonString(obj, "question");
            q.correct    = extractJsonString(obj, "correct");
            q.difficulty = extractJsonString(obj, "difficulty");
            q.category   = extractJsonString(obj, "category");

            // Parser le tableau d'options
            int optStart = obj.indexOf("\"options\"");
            if (optStart != -1) {
                int arrStart = obj.indexOf('[', optStart);
                int arrEnd   = obj.indexOf(']', arrStart);
                if (arrStart != -1 && arrEnd != -1) {
                    String arrContent = obj.substring(arrStart + 1, arrEnd);
                    int j = 0;
                    while (j < arrContent.length()) {
                        int quoteStart = arrContent.indexOf('"', j);
                        if (quoteStart == -1) break;
                        int quoteEnd = findJsonStringEnd(arrContent, quoteStart + 1);
                        String option = arrContent.substring(quoteStart + 1, quoteEnd)
                                .replace("\\\"", "\"").replace("\\n", "\n");
                        if (!option.isEmpty()) q.options.add(option);
                        j = quoteEnd + 1;
                    }
                }
            }

            // Vérifier que la réponse correcte est dans les options
            if (!q.options.isEmpty() && !q.options.contains(q.correct)) {
                if (q.correct.length() == 1) {
                    int idx = q.correct.charAt(0) - 'A';
                    if (idx >= 0 && idx < q.options.size()) {
                        q.correct = q.options.get(idx);
                    }
                } else {
                    q.correct = q.options.get(0);
                }
            }

            return q;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extrait la valeur d'une clé string dans un objet JSON simple.
     */
    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIndex = json.indexOf(search);
        if (keyIndex == -1) return "";

        int colonIndex = json.indexOf(':', keyIndex + search.length());
        if (colonIndex == -1) return "";

        int quoteStart = json.indexOf('"', colonIndex + 1);
        if (quoteStart == -1) return "";

        int quoteEnd = findJsonStringEnd(json, quoteStart + 1);
        return json.substring(quoteStart + 1, quoteEnd)
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\\\", "\\");
    }

    /**
     * Trouve l'accolade fermante correspondant à une accolade ouvrante.
     */
    private int findMatchingBrace(String text, int start) {
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && inString) { i++; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (!inString) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    /**
     * Échappe une string pour l'inclure dans du JSON.
     */
    private String escapeJson(String text) {
        return "\"" + text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Modèle de question parsée
    // ══════════════════════════════════════════════════════════════════════════
    public static class ParsedQuestion {
        public String       question   = "";
        public List<String> options    = new ArrayList<>();
        public String       correct    = "";
        public String       difficulty = "Medium";
        public String       category   = "General";
        public char[]       correctAnswer; // gardé pour compatibilité
    }
}