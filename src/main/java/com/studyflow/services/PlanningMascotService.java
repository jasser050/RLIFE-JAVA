package com.studyflow.services;

import com.studyflow.models.Planning;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public final class PlanningMascotService {

    public MascotState computeState(
            int streak,
            List<Planning> entries,
            LocalDate startInclusive,
            LocalDate endInclusive,
            Predicate<Planning> isDayOffEntry,
            BiPredicate<LocalDate, LocalTime> isSessionCompleted
    ) {
        if (entries == null || startInclusive == null || endInclusive == null) {
            return new MascotState("🐣", "Baby Planner", "Just starting out!", "Neutral 😐", 1, 0, 0, "Let's start planning! 🎯");
        }

        double moodAverage = entries.stream()
                .filter(entry -> entry != null && entry.getPlanningDate() != null)
                .filter(entry -> !isDayOffEntry.test(entry))
                .filter(entry -> !entry.getPlanningDate().isBefore(startInclusive) && !entry.getPlanningDate().isAfter(endInclusive))
                .filter(entry -> isSessionCompleted.test(entry.getPlanningDate(), entry.getEndTime()))
                .map(Planning::getFeedback)
                .map(this::toFeedbackCode)
                .filter(code -> code != null)
                .mapToInt(Integer::parseInt)
                .average()
                .orElse(3.0);

        String moodLabel = moodAverage >= 4.5 ? "Excellent"
                : moodAverage >= 3.5 ? "Good"
                : moodAverage >= 2.5 ? "Neutral"
                : moodAverage >= 1.5 ? "Low"
                : "Very Low";

        String moodEmoji = moodAverage >= 4.5 ? "🤩"
                : moodAverage >= 3.5 ? "🙂"
                : moodAverage >= 2.5 ? "😐"
                : moodAverage >= 1.5 ? "😕"
                : "😞";

        String mascotEmoji;
        String mascotName;
        String subtitle;
        int level;
        int nextGoal;
        if (streak <= 0) {
            mascotEmoji = "🥚";
            mascotName = "Baby Planner";
            subtitle = "Just starting out!";
            level = 1;
            nextGoal = 2;
        } else if (streak <= 2) {
            mascotEmoji = "🐣";
            mascotName = "Baby Planner";
            subtitle = "Just starting out!";
            level = 1;
            nextGoal = 3;
        } else if (streak <= 6) {
            mascotEmoji = "🐤";
            mascotName = "Growing Planner";
            subtitle = "Getting momentum";
            level = 2;
            nextGoal = 7;
        } else {
            mascotEmoji = "🐥";
            mascotName = "Shining Planner";
            subtitle = "Strong consistency";
            level = 3;
            nextGoal = 10;
        }

        int progress = Math.min(100, (int) Math.round((Math.max(0, streak) * 100.0) / Math.max(1, nextGoal)));
        String message = moodAverage >= 4.0
                ? "You're doing amazing! Keep it up ✨"
                : moodAverage >= 3.0
                ? "Great rhythm. Stay consistent 🎯"
                : "One step at a time. You got this 💪";

        return new MascotState(
                mascotEmoji,
                mascotName,
                subtitle,
                moodLabel + " " + moodEmoji,
                level,
                Math.max(0, streak),
                progress,
                message
        );
    }

    private String toFeedbackCode(String rawFeedback) {
        if (rawFeedback == null || rawFeedback.isBlank()) {
            return null;
        }
        String value = rawFeedback.trim().toLowerCase();
        return switch (value) {
            case "1", "very bad" -> "1";
            case "2", "bad" -> "2";
            case "3", "medium" -> "3";
            case "4", "good" -> "4";
            case "5", "excellent" -> "5";
            default -> null;
        };
    }

    public record MascotState(
            String emoji,
            String name,
            String subtitle,
            String moodDisplay,
            int level,
            int streak,
            int progressPercent,
            String message
    ) {
    }
}
