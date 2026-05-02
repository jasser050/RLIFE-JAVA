package com.studyflow.controllers;

import com.studyflow.game.Building;
import com.studyflow.game.GamePointsService;
import com.studyflow.models.EvaluationMatiere;
import com.studyflow.models.Matiere;
import com.studyflow.models.User;
import com.studyflow.services.EvaluationMatiereService;
import com.studyflow.services.MatiereService;
import com.studyflow.utils.UserSession;
import com.google.gson.Gson;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class CityViewController implements Initializable {

    // ════════════════════════════════════════════════════════════
    //  FXML FIELDS
    // ════════════════════════════════════════════════════════════
    @FXML private Label      lblCityName;
    @FXML private Label      lblTotalPoints;
    @FXML private Label      lblRank;
    @FXML private Label      lblBuildingEmoji;
    @FXML private Label      lblCurrentBuilding;
    @FXML private Label      lblBuildingDesc;
    @FXML private Label      lblNextBuilding;
    @FXML private Label      lblProgressPct;
    @FXML private ProgressBar pbProgress;
    @FXML private Label      lblPointsBreakdown;
    @FXML private FlowPane   buildingsGrid;
    @FXML private VBox       subjectCityList;
    @FXML private HBox       achievementRow;
    @FXML private WebView    cityMapWebView;

    // ════════════════════════════════════════════════════════════
    //  SERVICES
    // ════════════════════════════════════════════════════════════
    private final EvaluationMatiereService evalService = new EvaluationMatiereService();
    private final MatiereService           matService  = new MatiereService();

    private List<EvaluationMatiere> allEvals = new ArrayList<>();

    // ════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadAndRender();
    }

    /** Called from CoursesController whenever the city view is shown. */
    public void refresh() {
        loadAndRender();
    }

    // ════════════════════════════════════════════════════════════
    //  LOAD & RENDER
    // ════════════════════════════════════════════════════════════
    private void loadAndRender() {
        User u = UserSession.getInstance().getCurrentUser();
        if (u == null) return;

        try {
            allEvals = evalService.findByUser(u.getId());
        } catch (Exception e) {
            e.printStackTrace();
            allEvals = new ArrayList<>();
        }

        int total            = GamePointsService.totalPoints(allEvals);
        Building.Type current = Building.getBuildingForPoints(total);
        Building.Type next    = Building.getNextBuilding(total);
        int progress          = Building.getProgressToNext(total);

        // ── City name & rank ──────────────────────────────────
        if (lblCityName != null)
            lblCityName.setText(
                    u.getFirstName() != null ? u.getFirstName() + "'s City" : "My City"
            );

        if (lblRank != null)
            lblRank.setText(computeRankTitle(total));

        // ── Animated total points counter ─────────────────────
        animateCounter(lblTotalPoints, 0, total, 1200);

        // ── Current building ──────────────────────────────────
        if (lblBuildingEmoji != null) {
            lblBuildingEmoji.setText(current.emoji);
            bounceIn(lblBuildingEmoji);
        }
        if (lblCurrentBuilding != null) lblCurrentBuilding.setText(current.name);
        if (lblBuildingDesc    != null) lblBuildingDesc.setText(current.description);

        // ── Next building & progress ──────────────────────────
        if (next != null) {
            if (lblNextBuilding != null)
                lblNextBuilding.setText("Next: " + next.emoji + " " + next.name
                        + "  (" + (next.pointsRequired - total) + " pts away)");
            if (lblProgressPct != null) lblProgressPct.setText(progress + "%");
            if (pbProgress != null) {
                pbProgress.setProgress(0);
                new Timeline(new KeyFrame(Duration.millis(1200),
                        new KeyValue(pbProgress.progressProperty(),
                                progress / 100.0, Interpolator.EASE_OUT))).play();
            }
        } else {
            if (lblNextBuilding != null) lblNextBuilding.setText("🎉 Maximum level reached!");
            if (lblProgressPct  != null) lblProgressPct.setText("MAX");
            if (pbProgress      != null) pbProgress.setProgress(1.0);
        }

        // ── Points breakdown ──────────────────────────────────
        if (lblPointsBreakdown != null) {
            long high   = allEvals.stream().filter(e -> "High"  .equalsIgnoreCase(e.getPrioriteE())).count();
            long medium = allEvals.stream().filter(e -> "Medium".equalsIgnoreCase(e.getPrioriteE())).count();
            long low    = allEvals.stream().filter(e -> "Low"   .equalsIgnoreCase(e.getPrioriteE())).count();
            lblPointsBreakdown.setText(
                    "🔴 High ×2.0: " + high + " eval(s)     " +
                            "🟡 Medium ×1.5: " + medium + " eval(s)     " +
                            "🟢 Low ×1.0: " + low + " eval(s)");
        }

        // ── Buildings grid ────────────────────────────────────
        if (buildingsGrid != null) renderBuildingsGrid(total);

        // ── Per-subject mini cities ────────────────────────────
        if (subjectCityList != null) renderSubjectCities();

        // ── Achievements ──────────────────────────────────────
        if (achievementRow != null) renderAchievements(total);
        if (cityMapWebView != null) renderInteractiveMap(u, total);
    }

    // ════════════════════════════════════════════════════════════
    //  BUILDINGS GRID
    // ════════════════════════════════════════════════════════════
    private void renderBuildingsGrid(int total) {
        buildingsGrid.getChildren().clear();
        Building.Type[] types = Building.Type.values();
        for (int i = 0; i < types.length; i++) {
            Building.Type t       = types[i];
            boolean       unlocked = total >= t.pointsRequired;
            buildingsGrid.getChildren().add(buildBuildingCard(t, unlocked, i * 70L));
        }
    }

    private VBox buildBuildingCard(Building.Type type, boolean unlocked, long delayMs) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(110);
        card.setPadding(new Insets(14, 10, 14, 10));
        card.setStyle(unlocked
                ? "-fx-background-color:#0F172A;-fx-border-color:#7C3AED;-fx-border-width:2;" +
                "-fx-border-radius:16;-fx-background-radius:16;" +
                "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.4),12,0,0,4);"
                : "-fx-background-color:#0A0F1E;-fx-border-color:#1E293B;-fx-border-width:1.5;" +
                "-fx-border-radius:16;-fx-background-radius:16;");

        Label emojiLbl = new Label(unlocked ? type.emoji : "🔒");
        emojiLbl.setStyle("-fx-font-size:34px;");

        Label nameLbl = new Label(type.name);
        nameLbl.setStyle("-fx-text-fill:" + (unlocked ? "#F8FAFC" : "#334155") +
                ";-fx-font-size:11px;-fx-font-weight:700;");
        nameLbl.setWrapText(true);
        nameLbl.setAlignment(Pos.CENTER);

        Label ptsLbl = new Label(type.pointsRequired + " pts");
        ptsLbl.setStyle("-fx-text-fill:" + (unlocked ? "#A78BFA" : "#334155") +
                ";-fx-font-size:10px;");

        card.getChildren().addAll(emojiLbl, nameLbl, ptsLbl);

        // Staggered fade-in animation
        card.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(400), card);
        ft.setToValue(1.0);
        ft.setDelay(Duration.millis(delayMs));
        ft.play();

        if (unlocked) {
            card.setOnMouseEntered(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(140), card);
                st.setToX(1.07); st.setToY(1.07); st.play();
            });
            card.setOnMouseExited(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(140), card);
                st.setToX(1.0); st.setToY(1.0); st.play();
            });
            Tooltip tip = new Tooltip(type.description + "\nUnlocked at " + type.pointsRequired + " pts");
            tip.setStyle("-fx-background-color:#1E293B;-fx-text-fill:#F8FAFC;-fx-font-size:12px;");
            Tooltip.install(card, tip);
        }
        return card;
    }

    // ════════════════════════════════════════════════════════════
    //  PER-SUBJECT MINI CITIES
    // ════════════════════════════════════════════════════════════
    private void renderSubjectCities() {
        subjectCityList.getChildren().clear();

        Map<Integer, List<EvaluationMatiere>> grouped = allEvals.stream()
                .collect(Collectors.groupingBy(EvaluationMatiere::getMatiereId));

        if (grouped.isEmpty()) {
            Label empty = new Label("No evaluations yet — add some to grow your city!");
            empty.setStyle("-fx-text-fill:#475569;-fx-font-size:13px;-fx-padding:20;");
            subjectCityList.getChildren().add(empty);
            return;
        }

        grouped.entrySet().stream()
                .sorted((a, b) -> Integer.compare(
                        GamePointsService.subjectPoints(b.getValue(), b.getKey()),
                        GamePointsService.subjectPoints(a.getValue(), a.getKey())))
                .forEach(entry -> {
                    int matiereId = entry.getKey();
                    int pts = GamePointsService.subjectPoints(entry.getValue(), matiereId);
                    Building.Type bt = Building.getBuildingForPoints(pts);
                    subjectCityList.getChildren().add(
                            buildSubjectRow(getNomMatiere(matiereId), pts, bt));
                });
    }

    private HBox buildSubjectRow(String subjectName, int pts, Building.Type building) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 18, 14, 18));
        row.setStyle("-fx-background-color:#0F172A;-fx-border-color:#1E293B;" +
                "-fx-border-width:1;-fx-border-radius:14;-fx-background-radius:14;");

        Label emoji = new Label(building.emoji);
        emoji.setStyle("-fx-font-size:28px;");

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nm = new Label(subjectName);
        nm.setStyle("-fx-text-fill:#F8FAFC;-fx-font-size:14px;-fx-font-weight:700;");
        Label lvl = new Label(building.name + "  •  " + pts + " pts");
        lvl.setStyle("-fx-text-fill:#64748B;-fx-font-size:12px;");
        info.getChildren().addAll(nm, lvl);

        Building.Type next = Building.getNextBuilding(pts);
        int pct = Building.getProgressToNext(pts);

        VBox barBox = new VBox(4);
        barBox.setAlignment(Pos.CENTER_RIGHT);
        barBox.setMinWidth(130);

        Label nextLbl = new Label(next != null ? "→ " + next.emoji + " " + next.name : "MAX 🏆");
        nextLbl.setStyle("-fx-text-fill:#7C3AED;-fx-font-size:10px;-fx-font-weight:600;");

        ProgressBar pb = new ProgressBar((double) pct / 100);
        pb.setPrefWidth(130);
        pb.setPrefHeight(6);
        pb.setStyle("-fx-accent:#7C3AED;-fx-background-color:#1E293B;" +
                "-fx-background-radius:3;-fx-border-radius:3;");

        Label pctLbl = new Label(pct + "%");
        pctLbl.setStyle("-fx-text-fill:#A78BFA;-fx-font-size:10px;");

        barBox.getChildren().addAll(nextLbl, pb, pctLbl);
        row.getChildren().addAll(emoji, info, barBox);
        return row;
    }

    // ════════════════════════════════════════════════════════════
    //  ACHIEVEMENTS
    // ════════════════════════════════════════════════════════════
    private void renderAchievements(int total) {
        achievementRow.getChildren().clear();

        if (total >= 50)   addAchievement("🌱", "First Steps",  "#34D399");
        if (total >= 200)  addAchievement("📖", "Reader",       "#60A5FA");
        if (total >= 500)  addAchievement("⭐", "Scholar",      "#FBBF24");
        if (total >= 1000) addAchievement("🏆", "Champion",     "#F97316");
        if (total >= 2000) addAchievement("👑", "Master",       "#A78BFA");
        if (total >= 5000) addAchievement("🌈", "Grandmaster",  "#F43F5E");

        long perfect = allEvals.stream()
                .filter(e -> e.getNoteMaximaleEval() > 0
                        && e.getScoreEval() == e.getNoteMaximaleEval()).count();
        if (perfect >= 1)  addAchievement("💯", "Perfect!",    "#FB7185");
        if (allEvals.size() >= 5)  addAchievement("📊", "5 Evals",  "#38BDF8");
        if (allEvals.size() >= 10) addAchievement("🔥", "10 Evals", "#FB923C");

        if (achievementRow.getChildren().isEmpty()) {
            Label none = new Label("Complete evaluations to earn achievements!");
            none.setStyle("-fx-text-fill:#475569;-fx-font-size:12px;");
            achievementRow.getChildren().add(none);
        }
    }

    private void addAchievement(String emoji, String label, String color) {
        VBox badge = new VBox(4);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(10, 14, 10, 14));
        badge.setStyle("-fx-background-color:#0F172A;-fx-border-color:" + color +
                ";-fx-border-width:1.5;-fx-border-radius:12;-fx-background-radius:12;");

        Label em  = new Label(emoji);
        em.setStyle("-fx-font-size:22px;");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill:" + color +
                ";-fx-font-size:10px;-fx-font-weight:700;");

        badge.getChildren().addAll(em, lbl);

        // Pop-in animation
        badge.setScaleX(0); badge.setScaleY(0);
        ScaleTransition st = new ScaleTransition(Duration.millis(300), badge);
        st.setToX(1); st.setToY(1);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.setDelay(Duration.millis(achievementRow.getChildren().size() * 80L));
        st.play();

        achievementRow.getChildren().add(badge);
    }

    // ════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════
    private void renderInteractiveMap(User user, int totalPoints) {
        try {
            URL mapUrl = getClass().getResource("/com/studyflow/views/city-map-game.html");
            if (mapUrl == null) return;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("cityName", user.getFirstName() != null ? user.getFirstName() + "'s City" : "My City");
            payload.put("totalPoints", totalPoints);
            payload.put("subjects", buildMapSubjects());

            String json = new Gson().toJson(payload);
            String encoded = URLEncoder.encode(json, StandardCharsets.UTF_8);
            WebEngine engine = cityMapWebView.getEngine();
            engine.setJavaScriptEnabled(true);
            engine.load(mapUrl.toExternalForm() + "?data=" + encoded);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Map<String, Object>> buildMapSubjects() {
        Map<Integer, List<EvaluationMatiere>> grouped = allEvals.stream()
                .collect(Collectors.groupingBy(EvaluationMatiere::getMatiereId));

        if (grouped.isEmpty()) {
            return List.of(
                    mapSubject("Mathematics", 0, "Tent", "MATH"),
                    mapSubject("Physics", 0, "Tent", "PHYS"),
                    mapSubject("English", 0, "Tent", "ENG")
            );
        }

        return grouped.entrySet().stream()
                .sorted((a, b) -> Integer.compare(
                        GamePointsService.subjectPoints(b.getValue(), b.getKey()),
                        GamePointsService.subjectPoints(a.getValue(), a.getKey())))
                .limit(12)
                .map(entry -> {
                    int matiereId = entry.getKey();
                    int points = GamePointsService.subjectPoints(entry.getValue(), matiereId);
                    Building.Type building = Building.getBuildingForPoints(points);
                    return mapSubject(getNomMatiere(matiereId), points, building.name, building.emoji);
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> mapSubject(String name, int points, String building, String emoji) {
        Map<String, Object> subject = new LinkedHashMap<>();
        subject.put("name", name);
        subject.put("points", points);
        subject.put("building", building);
        subject.put("emoji", emoji);
        return subject;
    }

    private String computeRankTitle(int pts) {
        if (pts >= 5000) return "👑 Grand Scholar";
        if (pts >= 2000) return "🎓 Master";
        if (pts >= 1000) return "🏆 Expert";
        if (pts >= 500)  return "⭐ Advanced";
        if (pts >= 100)  return "📗 Learner";
        return "🌱 Beginner";
    }

    private void animateCounter(Label lbl, int from, int to, int durationMs) {
        if (lbl == null) return;
        int steps = Math.max(1, Math.min(60, Math.abs(to - from)));
        Timeline t = new Timeline();
        for (int i = 1; i <= steps; i++) {
            int val = from + (int)((to - from) * ((double) i / steps));
            long ms = (long)(durationMs * ((double) i / steps));
            t.getKeyFrames().add(new KeyFrame(Duration.millis(ms),
                    ev -> lbl.setText(String.valueOf(val))));
        }
        t.getKeyFrames().add(new KeyFrame(Duration.millis(durationMs),
                ev -> lbl.setText(String.valueOf(to))));
        t.play();
    }

    private void bounceIn(Label lbl) {
        ScaleTransition st = new ScaleTransition(Duration.millis(500), lbl);
        st.setFromX(0.3); st.setFromY(0.3);
        st.setToX(1.0);   st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.play();
    }

    private String getNomMatiere(int id) {
        try {
            Matiere m = matService.findById(id);
            return m != null ? m.getNomMatiere() : "Subject #" + id;
        } catch (Exception e) { return "Subject #" + id; }
    }
}
