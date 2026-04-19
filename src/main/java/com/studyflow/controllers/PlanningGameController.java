package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.PlanningEntry;
import com.studyflow.services.ServicePlanning;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class PlanningGameController implements Initializable {

    @FXML
    private Label totalXpLabel;

    @FXML
    private Label levelLabel;

    @FXML
    private Label progressLabel;

    @FXML
    private Label streakLabel;

    @FXML
    private Label dailyMissionLabel;

    @FXML
    private Label challengeStatusLabel;

    @FXML
    private VBox missionListBox;

    private final ServicePlanning planningService = new ServicePlanning();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadGameDashboard();
    }

    @FXML
    private void handleBackToPlanning() {
        MainController mainController = MainController.getInstance();
        if (mainController != null) {
            mainController.showPlanning();
            return;
        }
        try {
            App.setRoot("views/Planning");
        } catch (IOException ignored) {
            // Keep fallback silent to avoid noisy popups in navigation-only flow.
        }
    }

    @FXML
    private void handleRefreshGame() {
        loadGameDashboard();
    }

    private void loadGameDashboard() {
        List<PlanningEntry> entries = planningService.getAll();
        entries.sort(Comparator.comparing(PlanningEntry::getPlanningDate).thenComparing(PlanningEntry::getStartTime));

        long totalSessions = entries.size();
        long weeklySessions = entries.stream()
                .filter(e -> e.getPlanningDate() != null)
                .filter(e -> !e.getPlanningDate().isBefore(LocalDate.now().minusDays(6)))
                .count();

        long feedbackCount = entries.stream()
                .map(PlanningEntry::getFeedback)
                .filter(value -> value != null && !value.isBlank())
                .count();

        long totalMinutes = entries.stream()
                .filter(e -> e.getStartTime() != null && e.getEndTime() != null)
                .mapToLong(e -> {
                    long minutes = ChronoUnit.MINUTES.between(e.getStartTime(), e.getEndTime());
                    return Math.max(minutes, 0);
                })
                .sum();

        int streak = computeStreak(entries);

        int xp = (int) (totalSessions * 20 + weeklySessions * 10 + feedbackCount * 8 + streak * 15 + (totalMinutes / 30));
        int level = (xp / 100) + 1;
        int progress = xp % 100;

        totalXpLabel.setText(xp + " XP");
        levelLabel.setText("Level " + level);
        progressLabel.setText(progress + "/100 XP to next level");
        streakLabel.setText(streak + " day streak");

        String dailyMission = buildDailyMission(entries);
        dailyMissionLabel.setText(dailyMission);

        boolean missionDone = isDailyMissionDone(entries, dailyMission);
        challengeStatusLabel.setText(missionDone ? "Completed today ✅" : "Not completed yet ⏳");

        renderMissions(entries);
    }

    private int computeStreak(List<PlanningEntry> entries) {
        List<LocalDate> uniqueDates = entries.stream()
                .map(PlanningEntry::getPlanningDate)
                .filter(date -> date != null && !date.isAfter(LocalDate.now()))
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();

        if (uniqueDates.isEmpty()) {
            return 0;
        }

        int streak = 0;
        LocalDate expected = LocalDate.now();
        for (LocalDate date : uniqueDates) {
            if (date.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (date.isBefore(expected)) {
                break;
            }
        }
        return streak;
    }

    private String buildDailyMission(List<PlanningEntry> entries) {
        int selector = LocalDate.now().getDayOfYear() % 3;
        long todaySessions = entries.stream()
                .filter(e -> LocalDate.now().equals(e.getPlanningDate()))
                .count();

        return switch (selector) {
            case 0 -> "Mission: Plan at least 2 sessions today";
            case 1 -> "Mission: Add one session with feedback-ready goal";
            default -> todaySessions > 0
                    ? "Mission: Keep your streak alive tomorrow"
                    : "Mission: Plan your first session of the day";
        };
    }

    private boolean isDailyMissionDone(List<PlanningEntry> entries, String mission) {
        long todaySessions = entries.stream()
                .filter(e -> LocalDate.now().equals(e.getPlanningDate()))
                .count();
        if (mission.contains("2 sessions")) {
            return todaySessions >= 2;
        }
        if (mission.contains("feedback-ready")) {
            return entries.stream().anyMatch(e -> LocalDate.now().equals(e.getPlanningDate()));
        }
        return todaySessions > 0;
    }

    private void renderMissions(List<PlanningEntry> entries) {
        missionListBox.getChildren().clear();

        long todaySessions = entries.stream()
                .filter(e -> LocalDate.now().equals(e.getPlanningDate()))
                .count();
        long thisWeekSessions = entries.stream()
                .filter(e -> e.getPlanningDate() != null)
                .filter(e -> !e.getPlanningDate().isBefore(LocalDate.now().minusDays(6)))
                .count();
        long feedbackCount = entries.stream()
                .map(PlanningEntry::getFeedback)
                .filter(value -> value != null && !value.isBlank())
                .count();

        List<String> missions = new ArrayList<>();
        missions.add((todaySessions >= 1 ? "✅" : "⬜") + " Plan 1 session today");
        missions.add((thisWeekSessions >= 5 ? "✅" : "⬜") + " Reach 5 sessions this week");
        missions.add((feedbackCount >= 3 ? "✅" : "⬜") + " Complete 3 feedback entries");

        for (String mission : missions) {
            Label missionLabel = new Label(mission);
            missionLabel.getStyleClass().add("planning-game-mission-item");
            missionListBox.getChildren().add(missionLabel);
        }
    }
}

