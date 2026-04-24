package com.studyflow.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class WellbeingAiService {
    private final OpenRouterService openRouterService = new OpenRouterService();

    public record ChatTurn(String role, String content) {}
    public record CoachReply(String reply, String source, String languageCode) {}
    public record RecommendationItem(String title, String description) {}
    public record QuoteResult(String quote, String type, String source) {}

    public QuoteResult generateMotivationQuote(String type) {
        String safeType = normalizeQuoteType(type);

        if (openRouterService.isConfigured()) {
            List<OpenRouterService.ChatMessage> payload = new ArrayList<>();
            payload.add(new OpenRouterService.ChatMessage(
                    "system",
                    """
                    You generate one short quote for a student dashboard.
                    Return only the quote text.
                    Keep it under 18 words.
                    No quotation marks, no author, no markdown.
                    """
            ));
            payload.add(new OpenRouterService.ChatMessage(
                    "user",
                    "Quote type: " + safeType + ". Make it practical and student-friendly."
            ));
            String response = openRouterService.chat(payload, "anthropic/claude-3-haiku", 0.55, 80);
            if (response != null && !response.isBlank()) {
                return new QuoteResult(limit(response.replace("\n", " ").trim(), 140), safeType, "ai");
            }
        }

        return new QuoteResult(fallbackQuote(safeType), safeType, "fallback");
    }

    public List<RecommendationItem> generateRecommendations(int stressLevel10, String mood, List<String> signals) {
        int safeStress = Math.max(1, Math.min(10, stressLevel10));
        String safeMood = mood == null ? "okay" : mood.trim().toLowerCase(Locale.ROOT);
        List<String> safeSignals = signals == null ? List.of() : signals.stream()
                .filter(s -> s != null && !s.isBlank())
                .limit(6)
                .toList();

        if (openRouterService.isConfigured()) {
            List<OpenRouterService.ChatMessage> payload = new ArrayList<>();
            payload.add(new OpenRouterService.ChatMessage(
                    "system",
                    """
                    You generate wellbeing recommendations for students.
                    Return exactly 3 lines.
                    Each line format: Title | Short actionable description
                    Keep each description under 120 chars.
                    No numbering, no markdown, no extra text.
                    """
            ));
            payload.add(new OpenRouterService.ChatMessage(
                    "user",
                    "Stress level (1..10): " + safeStress
                            + ", mood: " + safeMood
                            + ", context: " + String.join(", ", safeSignals)
                            + ". Generate practical recommendations."
            ));
            String response = openRouterService.chat(payload, "anthropic/claude-3-haiku", 0.35, 220);
            List<RecommendationItem> parsed = parseRecommendationLines(response);
            if (!parsed.isEmpty()) {
                return parsed;
            }
        }

        return fallbackRecommendations(safeStress, safeMood);
    }

    public CoachReply coachReply(
            String message,
            List<ChatTurn> history,
            String preferredLanguageCode,
            String mode,
            String style,
            String level
    ) {
        String safeMessage = limit(message, 1500).trim();
        if (safeMessage.isBlank()) {
            return new CoachReply(
                    "Tell me what you are feeling, and we will take it step by step.",
                    "fallback",
                    "en-US"
            );
        }

        String languageCode = normalizeLanguageCode(preferredLanguageCode, safeMessage);
        String intent = detectIntent(safeMessage);
        String languageInstruction = languageInstructionFromCode(languageCode);
        String safeMode = mode == null || mode.isBlank() ? "general" : mode.trim().toLowerCase(Locale.ROOT);
        String safeStyle = style == null || style.isBlank() ? "direct" : style.trim().toLowerCase(Locale.ROOT);
        String safeLevel = level == null || level.isBlank() ? "professional" : level.trim().toLowerCase(Locale.ROOT);

        if (openRouterService.isConfigured()) {
            List<OpenRouterService.ChatMessage> payload = new ArrayList<>();
            payload.add(new OpenRouterService.ChatMessage(
                    "system",
                    """
                    You are RLIFE AI Assistant for students.
                    Rules:
                    - You can answer both general questions and wellbeing questions.
                    - Be warm, practical, and supportive.
                    - Reply ONLY in %s.
                    - Use the student's last message explicitly and respond directly.
                    - Avoid repetitive generic openings.
                    - Provide concise advice with 2 to 5 bullet points.
                    - Ask at most one clarifying question, only if needed.
                    - Do not pretend to be a doctor.
                    - If user mentions self-harm/suicide/danger: show empathy and strongly urge immediate emergency/crisis help.
                    Context:
                    - Detected intent: %s
                    - Mode: %s
                    - Style: %s
                    - Level: %s
                    - Last message: "%s"
                    """.formatted(languageInstruction, intent, safeMode, safeStyle, safeLevel, safeMessage)
            ));

            List<ChatTurn> safeHistory = history == null ? List.of() : history;
            int from = Math.max(0, safeHistory.size() - 12);
            for (int i = from; i < safeHistory.size(); i++) {
                ChatTurn turn = safeHistory.get(i);
                if (turn == null || turn.content() == null || turn.content().isBlank()) {
                    continue;
                }
                if (!"assistant".equals(turn.role()) && !"user".equals(turn.role())) {
                    continue;
                }
                payload.add(new OpenRouterService.ChatMessage(turn.role(), limit(turn.content(), 1500)));
            }
            if (!endsWithSameUserMessage(safeHistory, safeMessage)) {
                payload.add(new OpenRouterService.ChatMessage("user", safeMessage));
            }

            String response = openRouterService.chat(payload, "anthropic/claude-3-haiku", 0.45, 700);
            if (response != null && !response.isBlank()) {
                return new CoachReply(response.trim(), "ai", languageCode);
            }
        }

        return new CoachReply(buildFallbackReply(safeMessage, languageCode, intent, history), "fallback", languageCode);
    }

    public Map<String, String> detectSpeechLanguage(String text) {
        String sample = limit(text, 400).trim();
        if (sample.isBlank()) {
            return Map.of("languageCode", "en-US", "label", "English", "source", "fallback");
        }

        if (sample.matches(".*[\\u0600-\\u06FF].*")) {
            String lower = sample.toLowerCase(Locale.ROOT);
            String[] tunisianHints = {"barsha", "3lech", "chnowa", "brabi", "yesser", "tawa", "behi", "mouch", "famma", "ya3ni"};
            for (String hint : tunisianHints) {
                if (lower.contains(hint)) {
                    return Map.of("languageCode", "ar-TN", "label", "Tunisian Arabic", "source", "fallback");
                }
            }
            return Map.of("languageCode", "ar-SA", "label", "Arabic", "source", "fallback");
        }

        String lower = sample.toLowerCase(Locale.ROOT);
        int frScore = score(lower, "bonjour", "merci", "je ", "suis", "avec", "pour", "pas", "oui", "francais", "ça");
        int enScore = score(lower, "hello", "thanks", "i ", "am", "with", "for", "not", "yes", "english");

        if (frScore > enScore) {
            return Map.of("languageCode", "fr-FR", "label", "Francais", "source", "fallback");
        }
        return Map.of("languageCode", "en-US", "label", "English", "source", "fallback");
    }

    public String detectIntent(String message) {
        String text = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (containsAny(text, "sleep", "insomnia", "can't sleep", "dormir", "sommeil", "n3ass", "noum")) {
            return "sleep";
        }
        if (containsAny(text, "stress", "stressed", "anxiety", "panic", "angoisse", "توتر", "قلق")) {
            return "stress";
        }
        if (containsAny(text, "focus", "concentr", "study", "exam", "revision", "تركيز")) {
            return "focus";
        }
        if (containsAny(text, "motivation", "demotiv", "discouraged", "boost", "تحفيز")) {
            return "motivation";
        }
        if (containsAny(text, "sad", "depressed", "hopeless", "triste", "حزين")) {
            return "sadness";
        }
        return "general";
    }

    public boolean isHighRiskMessage(String text) {
        String value = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return containsAny(value,
                "suicide", "kill myself", "end my life", "self harm", "hurt myself",
                "je veux mourir", "je veux me tuer", "mourir", "me tuer",
                "nheb nmout", "bech nmout", "nmout", "mout");
    }

    public String languageInstructionFromCode(String languageCode) {
        return switch (languageCode) {
            case "fr-FR" -> "French";
            case "ar-TN" -> "Tunisian Arabic";
            case "ar-SA" -> "Arabic";
            default -> "English";
        };
    }

    public String buildFallbackReply(String message, String languageCode, String intent, List<ChatTurn> history) {
        String text = message == null ? "" : message.toLowerCase(Locale.ROOT);
        boolean highRisk = isHighRiskMessage(text);
        String candidateReply;

        if ("fr-FR".equals(languageCode)) {
            if (highRisk) {
                return "Je suis vraiment desole que tu traverses ca. Ta securite passe avant tout. Si tu es en danger maintenant, appelle les urgences immediatement et contacte une personne de confiance.";
            }
            if ("sleep".equals(intent)) {
                candidateReply = "Pour mieux dormir ce soir:\n- Coupe les ecrans 30 a 60 minutes avant.\n- Fais une respiration 4-7-8 pendant 3 minutes.\n- Ecris 2 ou 3 pensees pour demain.\n- Garde une chambre fraiche et sombre.";
                return avoidRepetition(candidateReply, message, languageCode, history);
            }
            if ("stress".equals(intent)) {
                candidateReply = "On reduit le stress maintenant:\n- Respire 4-4-6 pendant 2 minutes.\n- Liste 3 priorites max.\n- Choisis une action de 10 minutes.\nQuel est le stress principal en ce moment ?";
                return avoidRepetition(candidateReply, message, languageCode, history);
            }
            if ("focus".equals(intent)) {
                candidateReply = "Pour te concentrer:\n- 25 min focus + 5 min pause.\n- Telephone hors de vue.\n- Une seule tache claire.\nPar quoi tu veux commencer ?";
                return avoidRepetition(candidateReply, message, languageCode, history);
            }
            if ("motivation".equals(intent)) {
                candidateReply = "Pour relancer la motivation:\n- Un mini objectif de 5 a 10 minutes.\n- Lance un chrono court.\n- Recompense simple apres.";
                return avoidRepetition(candidateReply, message, languageCode, history);
            }
            return avoidRepetition(generalFallbackReply(message, languageCode), message, languageCode, history);
        }

        if ("ar-TN".equals(languageCode) || "ar-SA".equals(languageCode)) {
            if (highRisk) {
                return "Ana ma3ak. Salamtak awwalan. Itha kont fi khatar taw, ittasel bitawari2 fawran w khalli shakhs thiqa ma3ak.";
            }
            if ("sleep".equals(intent)) {
                candidateReply = "Bch t3awn rohek 3al noum:\n- b3ed 3an ecrans 30-60 min.\n- 4-7-8 respiration 3 min.\n- ekteb 2-3 afkar l-ghodwa.";
                return avoidRepetition(candidateReply, message, languageCode, history);
            }
            if ("stress".equals(intent)) {
                candidateReply = "Bch n9allou stress tawa:\n- 4-4-6 respiration 2 min.\n- 3 priorites lyoum.\n- khotwa sghira 10 min.";
                return avoidRepetition(candidateReply, message, languageCode, history);
            }
            if ("focus".equals(intent)) {
                candidateReply = "Bch tzid fel focus:\n- 25 min khidma + 5 min raha.\n- telephone barra.\n- task wa7da wadha.";
                return avoidRepetition(candidateReply, message, languageCode, history);
            }
            if ("motivation".equals(intent)) {
                candidateReply = "Bch n7arkou motivation:\n- objectif sghir 5-10 min.\n- start tawa.\n- recompense sghira ba3d.";
                return avoidRepetition(candidateReply, message, languageCode, history);
            }
            return avoidRepetition(generalFallbackReply(message, languageCode), message, languageCode, history);
        }

        if (highRisk) {
            return "I am really sorry you are going through this. Your safety comes first. If you are in immediate danger, call local emergency services now and contact someone you trust nearby.";
        }
        if ("sleep".equals(intent)) {
            candidateReply = "Let us improve sleep tonight:\n- No screens 30 to 60 minutes before bed.\n- 4-7-8 breathing for 3 minutes.\n- Write 2 to 3 worries for tomorrow.\n- Keep the room cool and dark.";
            return avoidRepetition(candidateReply, message, languageCode, history);
        }
        if ("stress".equals(intent)) {
            candidateReply = "Let us reduce stress now:\n- Breathe 4-4-6 for 2 minutes.\n- List your top 3 priorities.\n- Pick one 10-minute action.\nWhat feels hardest right now?";
            return avoidRepetition(candidateReply, message, languageCode, history);
        }
        if ("focus".equals(intent)) {
            candidateReply = "For focus:\n- 25 min focus + 5 min break.\n- Put your phone out of sight.\n- One clear task only.\nWhat is your first task now?";
            return avoidRepetition(candidateReply, message, languageCode, history);
        }
        if ("motivation".equals(intent)) {
            candidateReply = "To boost motivation:\n- Set a tiny 5-10 min goal.\n- Start a short timer.\n- Reward yourself after this first step.";
            return avoidRepetition(candidateReply, message, languageCode, history);
        }
        return avoidRepetition(generalFallbackReply(message, languageCode), message, languageCode, history);
    }

    private String normalizeLanguageCode(String preferredLanguageCode, String message) {
        if (preferredLanguageCode != null && !preferredLanguageCode.isBlank() && !"auto".equalsIgnoreCase(preferredLanguageCode)) {
            return preferredLanguageCode;
        }
        return detectSpeechLanguage(message).getOrDefault("languageCode", "en-US");
    }

    private int score(String text, String... tokens) {
        int value = 0;
        for (String token : tokens) {
            if (text.contains(token)) {
                value++;
            }
        }
        return value;
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String generalFallbackReply(String message, String languageCode) {
        String text = message == null ? "" : message.toLowerCase(Locale.ROOT);
        String topic = summarizeMessage(message);

        if (isChocolateCakeRequest(text)) {
            return chocolateCakeReply(languageCode);
        }
        if (isEmailRequest(text)) {
            return emailHelpReply(languageCode);
        }
        if (isConceptRequest(text)) {
            return conceptHelpReply(languageCode);
        }
        if (isMovieRequest(text)) {
            return movieRecommendationReply(languageCode, text);
        }
        String generalIntent = detectGeneralIntent(text);
        if (!"unknown".equals(generalIntent)) {
            return generalIntentReply(languageCode, generalIntent, topic);
        }

        if ("fr-FR".equals(languageCode)) {
            return "J'ai bien lu: \"" + topic + "\".\nJe peux t'aider sur presque toute demande. Donne-moi juste 1 detail en plus (objectif, niveau ou contrainte), et je te reponds directement.";
        }

        if ("ar-TN".equals(languageCode) || "ar-SA".equals(languageCode)) {
            return "Fhemtk: \"" + topic + "\".\nNajem n3awnek fi akther talab. Zidni b detail wa7ed (objectif/niveau/contraintes) w njawbik direct b chay moufid.";
        }

        return "I read your message: \"" + topic + "\".\nI can handle most requests. Give me one extra detail (goal, level, or constraint) and I will answer directly.";
    }

    private boolean isChocolateCakeRequest(String text) {
        return containsAny(text,
                "chocolate cake", "cake au chocolat", "gateau au chocolat", "gateau chocolat", "recette gateau");
    }

    private boolean isEmailRequest(String text) {
        return containsAny(text,
                "email", "e-mail", "mail", "courriel", "write an email", "ecrire un email", "rediger un email");
    }

    private boolean isConceptRequest(String text) {
        return containsAny(text,
                "explain", "what is", "definition", "concept", "explique", "c'est quoi", "definir", "definition");
    }

    private boolean isMovieRequest(String text) {
        return containsAny(text,
                "movie", "film", "serie", "series", "comedie", "comedia", "comedy", "netflix", "prime video");
    }

    private String chocolateCakeReply(String languageCode) {
        if ("fr-FR".equals(languageCode)) {
            return "Recette simple gateau au chocolat:\n- Ingredients: 200g chocolat noir, 120g beurre, 120g sucre, 4 oeufs, 80g farine.\n- Fais fondre chocolat + beurre.\n- Ajoute sucre, puis oeufs un par un.\n- Ajoute farine, melange doucement.\n- Four 180C, 22 a 28 min.\n- Laisse tiedir 10 min avant de servir.";
        }
        if ("ar-TN".equals(languageCode) || "ar-SA".equals(languageCode)) {
            return "Recette sahlat chocolate cake:\n- Ingredients: 200g chocolat, 120g zebda, 120g sokkar, 4 bidh, 80g farina.\n- Dhoueb chocolat m3a zebda.\n- Zid sokkar, ba3d kol mara bidha.\n- Zid farina b chwaya.\n- Four 180C moddet 22-28 d9i9a.\n- Khallih 10 d9aye9 ybared ba3d okhrojou.";
        }
        return "Simple chocolate cake recipe:\n- Ingredients: 200g dark chocolate, 120g butter, 120g sugar, 4 eggs, 80g flour.\n- Melt chocolate and butter.\n- Mix in sugar, then eggs one by one.\n- Fold in flour.\n- Bake at 180C for 22-28 minutes.\n- Let it rest 10 minutes before serving.";
    }

    private String emailHelpReply(String languageCode) {
        if ("fr-FR".equals(languageCode)) {
            return "Modele email rapide:\nObjet: [Sujet]\nBonjour [Nom],\nJe vous contacte concernant [contexte].\nJe souhaite [demande claire].\nMerci d'avance pour votre retour.\nCordialement,\n[Votre nom]";
        }
        if ("ar-TN".equals(languageCode) || "ar-SA".equals(languageCode)) {
            return "Template email srii3:\nSubject: [Sujet]\nBonjour [Nom],\nNekteblek b khas [contexte].\nNheb [demande claire].\nMerci 3la radd.\nCordialement,\n[Ismek]";
        }
        return "Quick email template:\nSubject: [Topic]\nHello [Name],\nI am writing regarding [context].\nI would like to [clear request].\nThank you for your time.\nBest regards,\n[Your name]";
    }

    private String conceptHelpReply(String languageCode) {
        if ("fr-FR".equals(languageCode)) {
            return "Donne-moi le concept exact et ton niveau (debutant/intermediaire). Je te repondrai avec:\n- Definition simple\n- Exemple concret\n- Mini resume en 3 points";
        }
        if ("ar-TN".equals(languageCode) || "ar-SA".equals(languageCode)) {
            return "Aatini esm el concept w niveau mte3ek (debutant/intermediaire). Taw njawbik b:\n- Ta3rif basit\n- Mithal wa9i3i\n- Kholasa fi 3 points";
        }
        return "Send the exact concept and your level (beginner/intermediate). I will reply with:\n- A simple definition\n- One concrete example\n- A 3-point summary";
    }

    private String movieRecommendationReply(String languageCode, String text) {
        boolean comedy = containsAny(text, "comedie", "comedia", "comedy");
        if ("fr-FR".equals(languageCode)) {
            if (comedy) {
                return "Top comedies a voir:\n- The Intouchables\n- Superbad\n- The Grand Budapest Hotel\n- Palm Springs\n- Jojo Rabbit\nDis-moi ton style (romcom, absurd, famille) et je te donne une liste plus precise.";
            }
            return "Suggestions films/series:\n- Inception\n- The Social Network\n- Interstellar\n- Whiplash\n- The Bear (serie)\nDis-moi le genre que tu veux et je te fais une liste ciblee.";
        }
        if ("ar-TN".equals(languageCode) || "ar-SA".equals(languageCode)) {
            if (comedy) {
                return "Comedy films behyin:\n- The Intouchables\n- Superbad\n- The Grand Budapest Hotel\n- Palm Springs\n- Jojo Rabbit\n9olli style elli t7eb (romcom/famille/black comedy) w na3tik liste ad9a.";
            }
            return "Film/series suggestions:\n- Inception\n- The Social Network\n- Interstellar\n- Whiplash\n- The Bear\n9olli genre mte3ek w na3tik recommandations ala 9addak.";
        }
        if (comedy) {
            return "Great comedy picks:\n- The Intouchables\n- Superbad\n- The Grand Budapest Hotel\n- Palm Springs\n- Jojo Rabbit\nTell me your style (romcom, dark, family) and I will narrow it down.";
        }
        return "Movie/series suggestions:\n- Inception\n- The Social Network\n- Interstellar\n- Whiplash\n- The Bear (series)\nTell me your genre and I will give a targeted list.";
    }

    private String detectGeneralIntent(String text) {
        if (containsAny(text, "recommend", "recommande", "donne moi", "suggest", "propose")) {
            return "recommendation";
        }
        if (containsAny(text, "comment", "how to", "how do i", "how can i", "faire", "build", "create")) {
            return "how_to";
        }
        if (containsAny(text, "write", "ecris", "redige", "message", "letter", "lettre", "post")) {
            return "writing";
        }
        if (containsAny(text, "translate", "traduit", "traduire", "translate to")) {
            return "translation";
        }
        if (containsAny(text, "code", "bug", "java", "python", "sql", "api", "algorithm")) {
            return "coding";
        }
        if (containsAny(text, "resume", "summarize", "summary", "explain this text", "simplify")) {
            return "summarize";
        }
        if (containsAny(text, "plan", "planning", "organize", "organise", "schedule", "emploi du temps")) {
            return "planning";
        }
        return "unknown";
    }

    private String generalIntentReply(String languageCode, String intent, String topic) {
        if ("fr-FR".equals(languageCode)) {
            return switch (intent) {
                case "recommendation" -> "Voici une reponse directe pour: \"" + topic + "\".\n- Je peux te donner 5 recommandations tout de suite.\n- Dis-moi juste ton style/budget/duree preferee.";
                case "how_to" -> "Pour \"" + topic + "\" je peux te guider pas a pas:\n1. Preparation\n2. Execution\n3. Verification finale\nDonne-moi ton niveau pour adapter les etapes.";
                case "writing" -> "Je peux rediger le texte complet pour: \"" + topic + "\".\nEnvoie: destinataire + ton (pro/poli/simple) + longueur, et je te donne la version finale.";
                case "translation" -> "Je peux traduire directement.\nColle le texte et precise la langue cible (ex: FR -> EN).";
                case "coding" -> "Je peux aider sur code/debug pour: \"" + topic + "\".\nEnvoie le code + erreur exacte, et je te propose une correction complete.";
                case "summarize" -> "Je peux resumer ce contenu en 3 niveaux (court/moyen/detaille).\nColle le texte et je te fais le resume.";
                case "planning" -> "Je peux construire un planning concret.\nDonne: objectif, deadline, heures dispo/jour, et je te fais un plan final.";
                default -> "Je peux traiter cette demande. Donne un detail en plus et je reponds directement.";
            };
        }
        if ("ar-TN".equals(languageCode) || "ar-SA".equals(languageCode)) {
            return switch (intent) {
                case "recommendation" -> "Najem na3tik recommandations direct l: \"" + topic + "\".\n9olli style/budget/wa9t bach n3tik liste madbouuta.";
                case "how_to" -> "Najem nfassarlek \"" + topic + "\" marhala b marhala.\n9olli niveau mte3ek bach na3tik steps mlaymin.";
                case "writing" -> "Najem nekteblek texte kamel.\nAatini destinataire + tone + tool, w na3tik version final.";
                case "translation" -> "Najem nترجم direct.\nAb3ath texte w 9olli men ena l ena (ex FR->EN).";
                case "coding" -> "Najem n3awnek fi code/debug.\nAb3ath code + erreur exacte w na3tik correction kamla.";
                case "summarize" -> "Najem n3mllek resume (sghir/moyen/kbir).\nAb3ath texte w nabda.";
                case "planning" -> "Najem na3mllek planning wadha7.\nAatini objectif + deadline + wa9t dispo fi nhar.";
                default -> "Najem n3awnek. Zidni b detail wa7ed w njawbik direct.";
            };
        }
        return switch (intent) {
            case "recommendation" -> "I can give direct recommendations for: \"" + topic + "\".\nShare your style/budget/time and I will return a targeted list.";
            case "how_to" -> "I can guide you step by step for: \"" + topic + "\".\nTell me your level and constraints, and I will provide exact steps.";
            case "writing" -> "I can write the full text for: \"" + topic + "\".\nSend recipient + tone + length, and I will draft it.";
            case "translation" -> "I can translate directly.\nPaste the text and target language (for example FR -> EN).";
            case "coding" -> "I can help with coding/debugging for: \"" + topic + "\".\nSend code + exact error and I will provide a fix.";
            case "summarize" -> "I can summarize your text in short/medium/detailed formats.\nPaste the content and I will do it.";
            case "planning" -> "I can build a concrete plan.\nShare goal, deadline, and available hours per day.";
            default -> "I can handle this request. Give one extra detail and I will answer directly.";
        };
    }

    private String summarizeMessage(String message) {
        String text = limit(message, 180).replace("\n", " ").replace("\r", " ").trim();
        if (text.isBlank()) {
            return "your situation";
        }
        return text;
    }

    private String avoidRepetition(String reply, String message, String languageCode, List<ChatTurn> history) {
        String lastAssistant = lastAssistantReply(history);
        if (lastAssistant == null) {
            return reply;
        }
        if (!normalizeForCompare(lastAssistant).equals(normalizeForCompare(reply))) {
            return reply;
        }

        String topic = summarizeMessage(message);
        if ("fr-FR".equals(languageCode)) {
            return reply + "\nJe m'adapte a ton message: \"" + topic + "\".";
        }
        if ("ar-TN".equals(languageCode) || "ar-SA".equals(languageCode)) {
            return reply + "\nNetsarref m3a klamek: \"" + topic + "\".";
        }
        return reply + "\nI am adapting to your latest message: \"" + topic + "\".";
    }

    private String lastAssistantReply(List<ChatTurn> history) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatTurn turn = history.get(i);
            if (turn == null || turn.content() == null || turn.content().isBlank()) {
                continue;
            }
            if ("assistant".equals(turn.role())) {
                return turn.content();
            }
        }
        return null;
    }

    private boolean endsWithSameUserMessage(List<ChatTurn> history, String safeMessage) {
        if (history == null || history.isEmpty()) {
            return false;
        }
        ChatTurn last = history.get(history.size() - 1);
        if (last == null || last.content() == null) {
            return false;
        }
        if (!"user".equals(last.role())) {
            return false;
        }
        return safeMessage.equals(last.content().trim());
    }

    private String normalizeForCompare(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String limit(String value, int max) {
        if (value == null) {
            return "";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private List<RecommendationItem> parseRecommendationLines(String response) {
        if (response == null || response.isBlank()) {
            return List.of();
        }
        List<RecommendationItem> items = new ArrayList<>();
        String[] lines = response.split("\\r?\\n");
        for (String line : lines) {
            String value = line == null ? "" : line.trim();
            if (value.isBlank()) {
                continue;
            }
            String[] parts = value.split("\\|", 2);
            if (parts.length < 2) {
                continue;
            }
            String title = parts[0].trim();
            String description = parts[1].trim();
            if (title.isBlank() || description.isBlank()) {
                continue;
            }
            items.add(new RecommendationItem(title, description));
            if (items.size() >= 3) {
                break;
            }
        }
        return items;
    }

    private List<RecommendationItem> fallbackRecommendations(int stressLevel10, String mood) {
        List<RecommendationItem> high = List.of(
                new RecommendationItem("Breathing Reset", "Try 4-7-8 breathing for 3 rounds before your next task."),
                new RecommendationItem("Mini Meditation", "Do a 5-minute body scan to release physical tension."),
                new RecommendationItem("Sleep Recovery", "Set a fixed sleep hour and reduce screens 45 minutes before bed."),
                new RecommendationItem("Healthy Boundaries", "Say no to one non-essential task and protect one calm study block.")
        );
        List<RecommendationItem> medium = List.of(
                new RecommendationItem("Focus Sprint", "Use 25 min focus + 5 min break for 3 cycles today."),
                new RecommendationItem("Active Break", "Stand up, stretch neck/shoulders, and walk 3 minutes between blocks."),
                new RecommendationItem("Hydration Boost", "Drink one glass of water at every study break."),
                new RecommendationItem("Pressure Reset", "Pick one 10-minute action to reduce overwhelm immediately.")
        );
        List<RecommendationItem> low = List.of(
                new RecommendationItem("Gratitude Note", "Write one win from today before ending your study session."),
                new RecommendationItem("Consistency Plan", "Keep your current routine and lock tomorrow's first task now."),
                new RecommendationItem("Sleep Hygiene", "Maintain stable sleep timing to preserve focus and mood."),
                new RecommendationItem("Energy Check", "Take a short movement break every 60 minutes of work.")
        );

        List<RecommendationItem> source;
        if (stressLevel10 >= 8 || "stressed".equals(mood) || "tired".equals(mood)) {
            source = new ArrayList<>(high);
        } else if (stressLevel10 >= 5) {
            source = new ArrayList<>(medium);
        } else {
            source = new ArrayList<>(low);
        }

        for (int i = source.size() - 1; i > 0; i--) {
            int j = ThreadLocalRandom.current().nextInt(i + 1);
            RecommendationItem tmp = source.get(i);
            source.set(i, source.get(j));
            source.set(j, tmp);
        }

        int count = Math.min(3, source.size());
        return new ArrayList<>(source.subList(0, count));
    }

    private String normalizeQuoteType(String value) {
        String type = value == null ? "motivation" : value.trim().toLowerCase(Locale.ROOT);
        if (!containsAny(type, "motivation", "focus", "calm", "funny")) {
            return "motivation";
        }
        return type;
    }

    private String fallbackQuote(String type) {
        List<String> quotes = switch (type) {
            case "focus" -> List.of(
                    "One clear task beats ten vague intentions.",
                    "Protect your focus before you protect your speed.",
                    "Start with the next step, not the whole mountain."
            );
            case "calm" -> List.of(
                    "Slow breathing is still progress.",
                    "Calm decisions usually create better days.",
                    "You do not need to solve everything tonight."
            );
            case "funny" -> List.of(
                    "Your to-do list is loud, but you are still in charge.",
                    "Surviving one tab at a time still counts as productivity.",
                    "Even your deadlines want you to drink water."
            );
            default -> List.of(
                    "Small progress every day beats perfect plans.",
                    "Start before you feel fully ready.",
                    "Consistency makes hard things look easy later."
            );
        };
        return quotes.get(ThreadLocalRandom.current().nextInt(quotes.size()));
    }
}
