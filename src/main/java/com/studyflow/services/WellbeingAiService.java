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
                    You are RLIFE Wellbeing AI Coach for students.
                    Rules:
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
        String topic = summarizeMessage(message);
        boolean wellbeingRelated = isWellbeingRelated(message);

        if ("fr-FR".equals(languageCode)) {
            if (!wellbeingRelated) {
                return "J'ai bien lu: \"" + topic + "\".\nJe suis surtout un coach bien-etre (stress, sommeil, focus, motivation).\nSi tu veux, je peux t'aider a transformer ta demande en plan anti-stress ou plan d'organisation.";
            }
            return "Tu as dit: \"" + topic + "\".\nPlan rapide:\n- Definis ton objectif des 20 prochaines minutes.\n- Fais une seule action courte maintenant.\n- Note ce qui te bloque pour que je t'aide a le debloquer.";
        }

        if ("ar-TN".equals(languageCode) || "ar-SA".equals(languageCode)) {
            if (!wellbeingRelated) {
                return "Fhemtk: \"" + topic + "\".\nEna akther coach wellbeing (stress, noum, focus, motivation).\nNajem n3awnek n7awlou talbek l plan sghir y9allek stress w y7assen tanthimk.";
            }
            return "Fhemt mni7: \"" + topic + "\".\nPlan sghir taw:\n- 7added objectif l 20 minute jeyin.\n- A3mel awel khotwa sghira taw.\n- 9olli chnoua li y3atlek besh nfasskhou m3a ba3dhna.";
        }

        if (!wellbeingRelated) {
            return "I read your message: \"" + topic + "\".\nI am mainly a wellbeing coach (stress, sleep, focus, motivation).\nIf you want, I can turn your request into a short wellbeing/action plan.";
        }
        return "I got your message: \"" + topic + "\".\nQuick plan:\n- Define one goal for the next 20 minutes.\n- Do one small action now.\n- Tell me what blocks you most so I can help you unblock it.";
    }

    private boolean isWellbeingRelated(String message) {
        String text = message == null ? "" : message.toLowerCase(Locale.ROOT);
        return containsAny(text,
                "stress", "stressed", "anxiety", "panic", "overwhelm",
                "sleep", "insomnia", "dormir", "sommeil", "noum", "n3ass",
                "focus", "concentr", "study", "exam", "revision", "motivation",
                "sad", "depressed", "hopeless", "triste", "burnout", "fatigue");
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
}
