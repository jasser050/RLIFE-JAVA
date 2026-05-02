package com.studyflow.game;

import com.studyflow.models.EvaluationMatiere;
import java.util.List;

/**
 * Converts EvaluationMatiere scores into game points.
 *
 * Formula:
 *   points = (score / noteMax) × 100 × priorityMultiplier
 *
 * Priority multipliers:
 *   High   → ×2.0
 *   Medium → ×1.5
 *   Low    → ×1.0
 */
public class GamePointsService {

    /**
     * Computes the game points for a single evaluation.
     */
    public static int computePoints(EvaluationMatiere ev) {
        if (ev == null || ev.getNoteMaximaleEval() <= 0) return 0;
        double ratio  = ev.getScoreEval() / ev.getNoteMaximaleEval();
        double base   = ratio * 100.0;
        double multi  = priorityMultiplier(ev.getPrioriteE());
        return (int) Math.round(base * multi);
    }

    /**
     * Computes the total game points across all evaluations.
     */
    public static int totalPoints(List<EvaluationMatiere> evals) {
        if (evals == null) return 0;
        return evals.stream().mapToInt(GamePointsService::computePoints).sum();
    }

    /**
     * Computes the total game points for one specific subject.
     */
    public static int subjectPoints(List<EvaluationMatiere> evals, int matiereId) {
        if (evals == null) return 0;
        return evals.stream()
                .filter(e -> e.getMatiereId() == matiereId)
                .mapToInt(GamePointsService::computePoints)
                .sum();
    }

    /**
     * Returns the priority multiplier for a given priority string.
     */
    private static double priorityMultiplier(String priority) {
        if (priority == null) return 1.0;
        return switch (priority) {
            case "High",   "Haute"   -> 2.0;
            case "Medium", "Moyenne" -> 1.5;
            default                  -> 1.0;
        };
    }
}