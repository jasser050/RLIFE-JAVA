package com.studyflow.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  AIQuizService — Génération de quiz via OpenRouter AI
 * ═══════════════════════════════════════════════════════════════════
 *
 *  OpenRouter donne accès à des dizaines de modèles (gratuits et payants).
 *  Obtenez votre clé sur : https://openrouter.ai/keys
 *
 *  SÉCURISER LA CLÉ (recommandé) :
 *    Windows  : setx OPENROUTER_API_KEY "sk-or-v1-..."
 *    Linux/Mac: export OPENROUTER_API_KEY="sk-or-v1-..."
 *
 *  Modèles gratuits disponibles sur OpenRouter :
 *    "mistralai/mistral-7b-instruct:free"
 *    "google/gemma-3-27b-it:free"
 *    "meta-llama/llama-3.1-8b-instruct:free"
 *    "deepseek/deepseek-chat-v3-0324:free"
 *    "openrouter/free"  ← auto-select modèle gratuit
 */
public class AIQuizService {

    // ── Configuration ─────────────────────────────────────────────

    /**
     * Clé API OpenRouter.
     * Priorité : variable d'environnement → valeur ci-dessous.
     * Remplacez la valeur par votre clé si vous ne configurez pas
     * la variable d'environnement.
     */
    private static final String API_KEY = System.getenv("OPENROUTER_API_KEY") != null
            ? System.getenv("OPENROUTER_API_KEY")
            : "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";   // ← remplacer

    /** Modèle utilisé. "openrouter/free" sélectionne automatiquement un modèle gratuit. */
    private static final String MODEL = "openrouter/free";

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";

    // ═══════════════════════════════════════════════════════════════
    //  MÉTHODE PRINCIPALE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Génère des questions de quiz via OpenRouter.
     *
     * @param matiere   Nom de la matière  (ex : "Mathématiques")
     * @param section   Section            (ex : "Terminale S")
     * @param level     Niveau adaptatif   ("easy" | "medium" | "hard")
     * @param count     Nombre de questions
     * @return          Liste de questions parsées
     * @throws Exception En cas d'erreur réseau ou API
     */
    public List<ParsedQuestion> generateQuizQuestions(
            String matiere, String section, String level, int count) throws Exception {

        String prompt = buildPrompt(matiere, section, level, count);

        // Corps JSON (format OpenAI-compatible utilisé par OpenRouter)
        String jsonBody = "{"
                + "\"model\": \"" + MODEL + "\","
                + "\"max_tokens\": 3000,"
                + "\"messages\": ["
                + "  {"
                + "    \"role\": \"user\","
                + "    \"content\": " + escapeJson(prompt)
                + "  }"
                + "]"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENROUTER_URL))
                .header("Content-Type",   "application/json")
                .header("Authorization",  "Bearer " + API_KEY)
                .header("HTTP-Referer",   "https://studyflow.app")
                .header("X-Title",        "StudyFlow")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Gestion des erreurs HTTP
        if (response.statusCode() == 401)
            throw new RuntimeException("401: Clé API invalide. Vérifiez sur openrouter.ai/keys");
        if (response.statusCode() == 429)
            throw new RuntimeException("429: Rate limit dépassé. Attendez et réessayez.");
        if (response.statusCode() != 200)
            throw new RuntimeException("Erreur API " + response.statusCode() + ": " + response.body());

        String text = extractTextFromResponse(response.body());
        return parseQuizResponse(text);
    }

    // ═══════════════════════════════════════════════════════════════
    //  PROMPT
    // ═══════════════════════════════════════════════════════════════

    private String buildPrompt(String matiere, String section, String level, int count) {
        return "You are an expert teacher. Generate exactly " + count + " multiple choice questions "
                + "for a student studying \"" + matiere + "\" in section \"" + section + "\". "
                + "Difficulty: " + level + ".\n\n"
                + "Return ONLY a valid JSON array — no text before or after, no markdown backticks.\n"
                + "Format:\n"
                + "[\n"
                + "  {\n"
                + "    \"question\": \"What is ...?\",\n"
                + "    \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],\n"
                + "    \"correct\": \"Option A\",\n"
                + "    \"explanation\": \"Option A is correct because... The others are wrong because...\",\n"
                + "    \"difficulty\": \"" + level + "\",\n"
                + "    \"category\": \"" + matiere + "\"\n"
                + "  }\n"
                + "]\n\n"
                + "RULES:\n"
                + "- Exactly " + count + " questions.\n"
                + "- 'correct' must be the exact text of one option (not 'A', 'B', etc.).\n"
                + "- 'explanation' : 2-3 sentences — why correct + why others are wrong.\n"
                + "- Return ONLY the JSON array.";
    }

    // ═══════════════════════════════════════════════════════════════
    //  PARSING RÉPONSE OPENROUTER (format OpenAI-compatible)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Extrait le texte brut depuis la réponse OpenRouter.
     * Format : {"choices":[{"message":{"content":"..."}}]}
     */
    private String extractTextFromResponse(String json) {
        int choicesIdx = json.indexOf("\"choices\"");
        if (choicesIdx == -1)
            throw new RuntimeException("Format de réponse inattendu : " + json);

        int contentIdx = json.indexOf("\"content\"", choicesIdx);
        if (contentIdx == -1)
            throw new RuntimeException("Pas de contenu dans la réponse : " + json);

        int colon = json.indexOf(':', contentIdx + 9);
        if (colon == -1)
            throw new RuntimeException("Champ content mal formé : " + json);

        int qStart = json.indexOf('"', colon + 1);
        if (qStart == -1)
            throw new RuntimeException("Valeur content absente : " + json);

        int qEnd = findStringEnd(json, qStart + 1);
        String raw = json.substring(qStart + 1, qEnd);

        return raw
                .replace("\\n",  "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\/",  "/");
    }

    // ═══════════════════════════════════════════════════════════════
    //  PARSING JSON DES QUESTIONS
    // ═══════════════════════════════════════════════════════════════

    private List<ParsedQuestion> parseQuizResponse(String text) {
        List<ParsedQuestion> list = new ArrayList<>();

        // Supprimer les éventuels backticks markdown
        text = text.trim();
        if (text.startsWith("```json")) text = text.substring(7);
        if (text.startsWith("```"))     text = text.substring(3);
        if (text.endsWith("```"))       text = text.substring(0, text.length() - 3);
        text = text.trim();

        int i = 0;
        while (i < text.length()) {
            int start = text.indexOf('{', i);
            if (start == -1) break;
            int end = findMatchingBrace(text, start);
            if (end == -1) break;
            ParsedQuestion q = parseOneQuestion(text.substring(start, end + 1));
            if (q != null && !q.question.isBlank()) list.add(q);
            i = end + 1;
        }
        return list;
    }

    private ParsedQuestion parseOneQuestion(String obj) {
        try {
            ParsedQuestion q = new ParsedQuestion();
            q.question    = extractString(obj, "question");
            q.correct     = extractString(obj, "correct");
            q.explanation = extractString(obj, "explanation");
            q.difficulty  = extractString(obj, "difficulty");
            q.category    = extractString(obj, "category");

            // Parser le tableau options
            int optIdx = obj.indexOf("\"options\"");
            if (optIdx != -1) {
                int aStart = obj.indexOf('[', optIdx);
                int aEnd   = obj.indexOf(']', aStart);
                if (aStart != -1 && aEnd != -1) {
                    String arr = obj.substring(aStart + 1, aEnd);
                    int j = 0;
                    while (j < arr.length()) {
                        int qs = arr.indexOf('"', j);
                        if (qs == -1) break;
                        int qe = findStringEnd(arr, qs + 1);
                        String opt = arr.substring(qs + 1, qe)
                                .replace("\\\"", "\"").replace("\\n", "\n");
                        if (!opt.isBlank()) q.options.add(opt);
                        j = qe + 1;
                    }
                }
            }

            // Vérifier que correct est dans les options
            if (!q.options.isEmpty() && !q.options.contains(q.correct)) {
                if (q.correct.length() == 1 && Character.isLetter(q.correct.charAt(0))) {
                    int idx = Character.toUpperCase(q.correct.charAt(0)) - 'A';
                    if (idx >= 0 && idx < q.options.size())
                        q.correct = q.options.get(idx);
                    else
                        q.correct = q.options.get(0);
                } else {
                    q.correct = q.options.get(0);
                }
            }
            return q;
        } catch (Exception e) {
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  UTILITAIRES JSON (parser minimal sans dépendance externe)
    // ═══════════════════════════════════════════════════════════════

    private String extractString(String json, String key) {
        String k = "\"" + key + "\"";
        int ki = json.indexOf(k);
        if (ki == -1) return "";
        int ci = json.indexOf(':', ki + k.length());
        if (ci == -1) return "";
        int qs = json.indexOf('"', ci + 1);
        if (qs == -1) return "";
        int qe = findStringEnd(json, qs + 1);
        return json.substring(qs + 1, qe)
                .replace("\\\"", "\"")
                .replace("\\n",  "\n")
                .replace("\\\\", "\\");
    }

    /**
     * Trouve la fin d'une chaîne JSON (gère les caractères échappés).
     */
    private int findStringEnd(String s, int start) {
        int i = start;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') i += 2;
            else if (c == '"') return i;
            else i++;
        }
        return s.length();
    }

    /**
     * Trouve l'accolade fermante correspondant à une accolade ouvrante.
     */
    private int findMatchingBrace(String s, int start) {
        int depth = 0;
        boolean inStr = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && inStr) { i++; continue; }
            if (c == '"') { inStr = !inStr; continue; }
            if (!inStr) {
                if (c == '{') depth++;
                else if (c == '}') { depth--; if (depth == 0) return i; }
            }
        }
        return -1;
    }

    /**
     * Échappe une chaîne pour l'inclure dans du JSON.
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

    // ═══════════════════════════════════════════════════════════════
    //  MODÈLE — Question parsée
    // ═══════════════════════════════════════════════════════════════

    public static class ParsedQuestion {
        /** Texte de la question */
        public String       question    = "";
        /** 4 options de réponse */
        public List<String> options     = new ArrayList<>();
        /** Texte exact de la bonne réponse (doit correspondre à une option) */
        public String       correct     = "";
        /** Explication pédagogique (2-3 phrases) */
        public String       explanation = "";
        /** Niveau : "easy" | "medium" | "hard" */
        public String       difficulty  = "Medium";
        /** Catégorie / matière */
        public String       category    = "General";
    }
}