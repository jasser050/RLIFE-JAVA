package com.studyflow.services;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  AntiFraudEngine — Moteur anti-fraude complet pour quiz JavaFX
 * ═══════════════════════════════════════════════════════════════════
 *
 *  ✔ Détection perte de focus (Alt+Tab, autre fenêtre)
 *  ✔ Détection sortie fullscreen + remise automatique
 *  ✔ Score de triche progressif (warning → pénalité → fin)
 *  ✔ Timer d'inactivité (30s sans action = suspect)
 *  ✔ Détection réponse ultra-rapide (<2s = suspect)
 *  ✔ Blocage Ctrl+C / Ctrl+V / Ctrl+X / clic droit
 *  ✔ Alertes overlay non-bloquantes (auto-close 3s)
 *  ✔ Journal complet via FraudLogger
 *  ✔ Callbacks vers le controller (onWarning, onTerminate…)
 *
 *  UTILISATION dans CoursesController :
 *
 *    private final AntiFraudEngine antiFraud = new AntiFraudEngine();
 *
 *    // Dans initialize() :
 *    antiFraud.setOnTerminate(() -> finishQuiz());
 *    antiFraud.setOnPenalty(n -> quizScore--);
 *
 *    // Quand le quiz commence :
 *    antiFraud.attach(stage);
 *    antiFraud.blockSystemActions(scene);
 *    antiFraud.startMonitoring();
 *
 *    // Quand le quiz se termine :
 *    antiFraud.stopMonitoring();
 *    double finalScore = antiFraud.applyPenaltiesToScore(rawScore);
 */
public class AntiFraudEngine {

    // ── Seuils (modifiables selon rigueur souhaitée) ─────────────
    public static final int THRESHOLD_WARNING   = 5;   // → alerte jaune
    public static final int THRESHOLD_PENALTY   = 10;  // → -1 point
    public static final int THRESHOLD_TERMINATE = 15;  // → fin forcée

    // Points par type d'événement
    private static final int PTS_FOCUS_LOST   = 3;
    private static final int PTS_FULLSCREEN   = 4;
    private static final int PTS_INACTIVITY   = 2;
    private static final int PTS_FAST_ANSWER  = 1;
    private static final int PTS_COPY_PASTE   = 1;
    private static final int PTS_RIGHT_CLICK  = 1;

    // Inactivité : 30 secondes sans action
    private static final int INACTIVITY_SEC   = 30;

    // ── État ──────────────────────────────────────────────────────
    private Stage   monitoredStage;
    private int     fraudScore          = 0;
    private int     focusLostCount      = 0;
    private int     fullscreenExitCount = 0;
    private int     penaltyCount        = 0;
    private boolean isActive            = false;
    private boolean quizTerminated      = false;
    private long    questionStartTime   = 0;

    private final List<FraudEvent> eventLog = new ArrayList<>();
    private final FraudLogger      logger   = new FraudLogger();

    // ── Callbacks vers le controller ──────────────────────────────
    private Consumer<FraudEvent> onFraudDetected;
    private Consumer<String>     onWarning;
    private Runnable             onTerminate;
    private Consumer<Integer>    onPenalty;
    private Consumer<Integer>    onFraudScoreUpdated;

    // ── Timers ────────────────────────────────────────────────────
    private Timeline inactivityTimer;
    private Timeline questionTimer;

    // ── Overlay d'alerte ──────────────────────────────────────────
    private Stage warningOverlay;

    // ═══════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Attache le moteur à la fenêtre JavaFX du quiz.
     * À appeler UNE FOIS quand la scène est disponible.
     */
    public void attach(Stage stage) {
        this.monitoredStage = stage;

        // Détection perte de focus
        stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isActive) return;
            if (!isFocused) handleFocusLost();
            else            resetInactivityTimer();
        });

        // Détection sortie fullscreen
        stage.fullScreenProperty().addListener((obs, wasFS, isFS) -> {
            if (isActive && !isFS) handleFullscreenExit(stage);
        });

        log("Engine attached — stage: " + stage.getTitle());
    }

    /**
     * Démarre la surveillance. Appeler au début du quiz.
     */
    public void startMonitoring() {
        isActive            = true;
        quizTerminated      = false;
        fraudScore          = 0;
        focusLostCount      = 0;
        fullscreenExitCount = 0;
        penaltyCount        = 0;
        questionStartTime   = System.currentTimeMillis();
        eventLog.clear();
        startInactivityTimer();
        log("Monitoring STARTED");
    }

    /**
     * Arrête la surveillance. Appeler en fin de quiz.
     */
    public void stopMonitoring() {
        isActive = false;
        stopInactivityTimer();
        stopQuestionTimer();
        closeOverlay();
        log("Monitoring STOPPED — fraud score final: " + fraudScore);
    }

    /**
     * Notifie un changement de question.
     * Détecte les réponses ultra-rapides.
     */
    public void onQuestionChanged(int questionIndex) {
        long elapsedSec = (System.currentTimeMillis() - questionStartTime) / 1000;
        if (elapsedSec < 2 && questionIndex > 0) {
            addFraud(PTS_FAST_ANSWER, FraudEvent.Type.FAST_ANSWER,
                    "Question answered in " + elapsedSec + "s (suspiciously fast)");
        }
        questionStartTime = System.currentTimeMillis();
        resetInactivityTimer();
    }

    /**
     * Démarre un chronomètre par question.
     *
     * @param limitSec  Durée max en secondes
     * @param onTick    Appelé chaque seconde avec les secondes restantes
     * @param onTimeUp  Appelé quand le temps est écoulé
     */
    public void startQuestionTimer(int limitSec, Consumer<Integer> onTick, Runnable onTimeUp) {
        stopQuestionTimer();
        int[] remaining = {limitSec};
        questionTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remaining[0]--;
            if (onTick != null) onTick.accept(remaining[0]);
            if (remaining[0] <= 0) {
                stopQuestionTimer();
                if (onTimeUp != null) onTimeUp.run();
            }
        }));
        questionTimer.setCycleCount(limitSec);
        questionTimer.play();
    }

    public void stopQuestionTimer() {
        if (questionTimer != null) { questionTimer.stop(); questionTimer = null; }
    }

    // ═══════════════════════════════════════════════════════════════
    //  BLOCAGE ACTIONS SYSTÈME
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bloque Ctrl+C/V/X, clic droit, et réinitialise le timer
     * d'inactivité à chaque interaction utilisateur.
     * À appeler après que la scene est disponible.
     */
    public void blockSystemActions(Scene scene) {

        // Bloquer copier/coller/couper
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case C, V, X, A -> {
                        event.consume();
                        addFraud(PTS_COPY_PASTE, FraudEvent.Type.COPY_PASTE,
                                "Keyboard shortcut blocked: Ctrl+" + event.getCode());
                    }
                    default -> {}
                }
            }
        });

        // Bloquer clic droit
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            if (event.isSecondaryButtonDown()) {
                event.consume();
                addFraud(PTS_RIGHT_CLICK, FraudEvent.Type.RIGHT_CLICK, "Right-click blocked");
            }
        });

        // Réinitialiser inactivité sur toute interaction
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED,
                e -> { if (isActive) resetInactivityTimer(); });
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED,
                e -> { if (isActive) resetInactivityTimer(); });
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                e -> { if (isActive) resetInactivityTimer(); });
    }

    // ═══════════════════════════════════════════════════════════════
    //  HANDLERS INTERNES
    // ═══════════════════════════════════════════════════════════════

    private void handleFocusLost() {
        focusLostCount++;
        addFraud(PTS_FOCUS_LOST, FraudEvent.Type.FOCUS_LOST,
                "Window lost focus — attempt #" + focusLostCount);
        Platform.runLater(() ->
                showOverlay("⚠ Focus lost!\nPlease stay in the exam window.", "#F59E0B"));
    }

    private void handleFullscreenExit(Stage stage) {
        fullscreenExitCount++;
        addFraud(PTS_FULLSCREEN, FraudEvent.Type.FULLSCREEN_EXIT,
                "Fullscreen exited — attempt #" + fullscreenExitCount);
        Platform.runLater(() -> {
            if (!quizTerminated) {
                stage.setFullScreen(true); // Remise en fullscreen automatique
                showOverlay("⚠ Fullscreen exit detected!\nWindow has been restored.", "#EF4444");
            }
        });
    }

    private void handleInactivity() {
        addFraud(PTS_INACTIVITY, FraudEvent.Type.INACTIVITY,
                "No activity detected for " + INACTIVITY_SEC + "+ seconds");
    }

    // ═══════════════════════════════════════════════════════════════
    //  SYSTÈME DE SCORE DE TRICHE
    // ═══════════════════════════════════════════════════════════════

    private void addFraud(int points, FraudEvent.Type type, String description) {
        if (quizTerminated || !isActive) return;

        fraudScore += points;
        FraudEvent event = new FraudEvent(type, description, fraudScore);
        eventLog.add(event);
        logger.log(event);

        if (onFraudDetected     != null) onFraudDetected.accept(event);
        if (onFraudScoreUpdated != null) onFraudScoreUpdated.accept(fraudScore);

        evaluateThresholds();
    }

    private void evaluateThresholds() {
        if (fraudScore >= THRESHOLD_TERMINATE && !quizTerminated) {
            quizTerminated = true;
            stopMonitoring();
            Platform.runLater(() -> {
                showTerminationAlert();
                if (onTerminate != null) onTerminate.run();
            });
        } else if (fraudScore >= THRESHOLD_PENALTY) {
            int newPenalties = fraudScore / THRESHOLD_PENALTY;
            if (newPenalties > penaltyCount) {
                penaltyCount = newPenalties;
                Platform.runLater(() -> {
                    showOverlay("⚠ Penalty applied!\n-1 point deducted.", "#EF4444");
                    if (onPenalty != null) onPenalty.accept(penaltyCount);
                });
            }
        } else if (fraudScore >= THRESHOLD_WARNING) {
            if (onWarning != null)
                onWarning.accept("Suspicious activity — fraud score: "
                        + fraudScore + "/" + THRESHOLD_TERMINATE);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  TIMERS
    // ═══════════════════════════════════════════════════════════════

    private void startInactivityTimer() {
        stopInactivityTimer();
        inactivityTimer = new Timeline(
                new KeyFrame(Duration.seconds(INACTIVITY_SEC), e -> {
                    if (isActive) handleInactivity();
                })
        );
        inactivityTimer.setCycleCount(Timeline.INDEFINITE);
        inactivityTimer.play();
    }

    private void stopInactivityTimer() {
        if (inactivityTimer != null) { inactivityTimer.stop(); inactivityTimer = null; }
    }

    public void resetInactivityTimer() {
        startInactivityTimer(); // repart de zéro
    }

    // ═══════════════════════════════════════════════════════════════
    //  OVERLAYS D'ALERTE
    // ═══════════════════════════════════════════════════════════════

    private void showOverlay(String message, String bgColor) {
        closeOverlay();

        Stage overlay = new Stage(StageStyle.TRANSPARENT);
        overlay.setAlwaysOnTop(true);

        Label lbl = new Label(message);
        lbl.setWrapText(true);
        lbl.setStyle(
                "-fx-text-fill:white;" +
                        "-fx-font-size:13px;" +
                        "-fx-font-weight:700;" +
                        "-fx-text-alignment:center;"
        );

        VBox box = new VBox(lbl);
        box.setAlignment(Pos.CENTER);
        box.setStyle(
                "-fx-background-color:" + bgColor + ";" +
                        "-fx-background-radius:12;" +
                        "-fx-padding:18 24;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.7),20,0,0,6);"
        );
        box.setMaxWidth(280);

        javafx.scene.Scene scene = new javafx.scene.Scene(box);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        overlay.setScene(scene);

        // Positionner en haut à droite de la fenêtre surveillée
        if (monitoredStage != null) {
            overlay.setX(monitoredStage.getX() + monitoredStage.getWidth() - 310);
            overlay.setY(monitoredStage.getY() + 20);
        }
        overlay.show();
        warningOverlay = overlay;

        // Auto-fermeture après 3 secondes
        new Timeline(new KeyFrame(Duration.seconds(3),
                e -> { if (overlay.isShowing()) overlay.close(); }
        )).play();
    }

    private void closeOverlay() {
        if (warningOverlay != null && warningOverlay.isShowing()) warningOverlay.close();
    }

    private void showTerminationAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("🚫 Quiz Terminated");
        alert.setHeaderText("Suspicious activity detected");
        alert.setContentText(
                "Your quiz has been terminated due to repeated suspicious behavior.\n\n" +
                        "• Window focus lost:    " + focusLostCount + " time(s)\n" +
                        "• Fullscreen exits:     " + fullscreenExitCount + " time(s)\n" +
                        "• Fraud score:          " + fraudScore + "/" + THRESHOLD_TERMINATE + "\n\n" +
                        "Your answers have been recorded."
        );
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════════
    //  GETTERS ET CALLBACKS
    // ═══════════════════════════════════════════════════════════════

    public int     getFraudScore()          { return fraudScore; }
    public int     getFocusLostCount()      { return focusLostCount; }
    public int     getFullscreenExitCount() { return fullscreenExitCount; }
    public int     getPenaltyCount()        { return penaltyCount; }
    public boolean isTerminated()           { return quizTerminated; }
    public boolean isActive()               { return isActive; }
    public List<FraudEvent> getEventLog()   { return Collections.unmodifiableList(eventLog); }

    /** Résumé lisible pour l'affichage dans les résultats */
    public String getSummary() {
        return String.format(
                "Fraud Score: %d/%d | Focus Lost: %d | Fullscreen Exits: %d | Penalties: %d",
                fraudScore, THRESHOLD_TERMINATE, focusLostCount, fullscreenExitCount, penaltyCount
        );
    }

    /** Applique les pénalités au score brut du quiz */
    public double applyPenaltiesToScore(double rawScore) {
        return Math.max(0, rawScore - penaltyCount);
    }

    // Setters callbacks
    public void setOnFraudDetected(Consumer<FraudEvent> cb)   { this.onFraudDetected = cb; }
    public void setOnWarning(Consumer<String> cb)              { this.onWarning = cb; }
    public void setOnTerminate(Runnable cb)                    { this.onTerminate = cb; }
    public void setOnPenalty(Consumer<Integer> cb)             { this.onPenalty = cb; }
    public void setOnFraudScoreUpdated(Consumer<Integer> cb)   { this.onFraudScoreUpdated = cb; }

    private void log(String msg) {
        System.out.println("[AntiFraud] " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " — " + msg);
    }
}