package com.studyflow.services;

import com.studyflow.api.FraudApiClient;
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
 * ═══════════════════════════════════════════════════════════════
 *  AntiFraudEngine — Moteur anti-fraude avec API REST intégrée
 * ═══════════════════════════════════════════════════════════════
 *
 *  Chaque événement suspect est envoyé à l'API locale (port 8085)
 *  qui le persiste en BDD et retourne une décision (CONTINUE/TERMINATE).
 *
 *  Si l'API est indisponible → mode local automatique (comportement
 *  identique à l'ancienne version sans API).
 *
 *  UTILISATION dans CoursesController (inchangée) :
 *
 *    private final AntiFraudEngine antiFraud = new AntiFraudEngine();
 *    antiFraud.setOnTerminate(() -> finishQuiz());
 *    antiFraud.setOnPenalty(n -> quizScore--);
 *    antiFraud.attach(stage);
 *    antiFraud.blockSystemActions(scene);
 *    antiFraud.startMonitoring();   // ← démarre aussi la session API
 *    antiFraud.stopMonitoring();    // ← clôture la session API
 *    double score = antiFraud.applyPenaltiesToScore(rawScore);
 */
public class AntiFraudEngine {

    // ── Seuils ───────────────────────────────────────────────────
    public static final int THRESHOLD_WARNING   = 5;
    public static final int THRESHOLD_PENALTY   = 10;
    public static final int THRESHOLD_TERMINATE = 15;

    private static final int PTS_FOCUS_LOST  = 3;
    private static final int PTS_FULLSCREEN  = 4;
    private static final int PTS_INACTIVITY  = 2;
    private static final int PTS_FAST_ANSWER = 1;
    private static final int PTS_COPY_PASTE  = 1;
    private static final int PTS_RIGHT_CLICK = 1;
    private static final int INACTIVITY_SEC  = 30;

    // ── État ─────────────────────────────────────────────────────
    private Stage   monitoredStage;
    private int     fraudScore          = 0;
    private int     focusLostCount      = 0;
    private int     fullscreenExitCount = 0;
    private int     penaltyCount        = 0;
    private boolean isActive            = false;
    private boolean quizTerminated      = false;
    private long    questionStartTime   = 0;
    private String  sessionId           = null; // UUID de la session API

    private final List<FraudEvent> eventLog = new ArrayList<>();
    private final FraudLogger      logger   = new FraudLogger();

    // ── API CLIENT ← NOUVEAU ─────────────────────────────────────
    private final FraudApiClient apiClient = new FraudApiClient();

    // ── Callbacks vers le controller ─────────────────────────────
    private Consumer<FraudEvent> onFraudDetected;
    private Consumer<String>     onWarning;
    private Runnable             onTerminate;
    private Consumer<Integer>    onPenalty;
    private Consumer<Integer>    onFraudScoreUpdated;

    // ── Timers ───────────────────────────────────────────────────
    private Timeline inactivityTimer;
    private Timeline questionTimer;

    // ── Overlay ──────────────────────────────────────────────────
    private Stage warningOverlay;

    // ═══════════════════════════════════════════════════════════
    //  INITIALISATION
    // ═══════════════════════════════════════════════════════════

    public void attach(Stage stage) {
        this.monitoredStage = stage;

        stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isActive) return;
            if (!isFocused) handleFocusLost();
            else            resetInactivityTimer();
        });

        stage.fullScreenProperty().addListener((obs, wasFS, isFS) -> {
            if (isActive && !isFS) handleFullscreenExit(stage);
        });

        log("Engine attached — stage: " + stage.getTitle());
    }

    /**
     * Démarre la surveillance + crée une session dans l'API.
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

        // Générer un UUID de session
        sessionId = java.util.UUID.randomUUID().toString();
        log("Session ID: " + sessionId);

        // Vérifier que l'API est disponible, puis démarrer la session
        new Thread(() -> {
            boolean available = apiClient.checkHealth();
            if (available) {
                apiClient.startSession(sessionId);
                log("API mode active — session registered");
            } else {
                log("API unavailable — local mode active");
            }
        }, "FraudInit").start();

        startInactivityTimer();
        log("Monitoring STARTED — session: " + sessionId);
    }

    /**
     * Arrête la surveillance + clôture la session dans l'API.
     */
    public void stopMonitoring() {
        isActive = false;
        stopInactivityTimer();
        stopQuestionTimer();
        closeOverlay();

        // Clôturer la session via l'API
        if (sessionId != null) {
            apiClient.endSession(sessionId, fraudScore, penaltyCount, adjustedScore -> {
                log("Session closed — adjustedScore from API: " + adjustedScore);
            });
        }

        log("Monitoring STOPPED — fraud score: " + fraudScore);
    }

    public void onQuestionChanged(int questionIndex) {
        long elapsedSec = (System.currentTimeMillis() - questionStartTime) / 1000;
        if (elapsedSec < 2 && questionIndex > 0) {
            addFraud(PTS_FAST_ANSWER, FraudEvent.Type.FAST_ANSWER,
                    "Question answered in " + elapsedSec + "s");
        }
        questionStartTime = System.currentTimeMillis();
        resetInactivityTimer();
    }

    public void startQuestionTimer(int limitSec, Consumer<Integer> onTick, Runnable onTimeUp) {
        stopQuestionTimer();
        int[] remaining = {limitSec};
        questionTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remaining[0]--;
            if (onTick != null) onTick.accept(remaining[0]);
            if (remaining[0] <= 0) { stopQuestionTimer(); if (onTimeUp != null) onTimeUp.run(); }
        }));
        questionTimer.setCycleCount(limitSec);
        questionTimer.play();
    }

    public void stopQuestionTimer() {
        if (questionTimer != null) { questionTimer.stop(); questionTimer = null; }
    }

    // ═══════════════════════════════════════════════════════════
    //  BLOCAGE ACTIONS SYSTÈME
    // ═══════════════════════════════════════════════════════════

    public void blockSystemActions(javafx.scene.Scene scene) {

        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case C, V, X, A -> {
                        event.consume();
                        addFraud(PTS_COPY_PASTE, FraudEvent.Type.COPY_PASTE,
                                "Ctrl+" + event.getCode() + " blocked");
                    }
                    default -> {}
                }
            }
        });

        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            if (event.isSecondaryButtonDown()) {
                event.consume();
                addFraud(PTS_RIGHT_CLICK, FraudEvent.Type.RIGHT_CLICK, "Right-click blocked");
            }
        });

        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED,
                e -> { if (isActive) resetInactivityTimer(); });
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED,
                e -> { if (isActive) resetInactivityTimer(); });
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                e -> { if (isActive) resetInactivityTimer(); });
    }

    // ═══════════════════════════════════════════════════════════
    //  HANDLERS INTERNES
    // ═══════════════════════════════════════════════════════════

    private void handleFocusLost() {
        focusLostCount++;
        addFraud(PTS_FOCUS_LOST, FraudEvent.Type.FOCUS_LOST,
                "Window lost focus — #" + focusLostCount);
        Platform.runLater(() ->
                showOverlay("⚠ Focus lost!\nStay in the exam window.", "#F59E0B"));
    }

    private void handleFullscreenExit(Stage stage) {
        fullscreenExitCount++;
        addFraud(PTS_FULLSCREEN, FraudEvent.Type.FULLSCREEN_EXIT,
                "Fullscreen exited — #" + fullscreenExitCount);
        Platform.runLater(() -> {
            if (!quizTerminated) {
                stage.setFullScreen(true);
                showOverlay("⚠ Fullscreen exit detected!\nWindow restored.", "#EF4444");
            }
        });
    }

    private void handleInactivity() {
        addFraud(PTS_INACTIVITY, FraudEvent.Type.INACTIVITY,
                "No activity for " + INACTIVITY_SEC + "s");
    }

    // ═══════════════════════════════════════════════════════════
    //  SYSTÈME DE SCORE — ENVOIE À L'API
    // ═══════════════════════════════════════════════════════════

    private void addFraud(int points, FraudEvent.Type type, String description) {
        if (quizTerminated || !isActive) return;

        fraudScore += points;
        FraudEvent event = new FraudEvent(type, description, fraudScore);
        eventLog.add(event);
        logger.log(event); // log local conservé

        if (onFraudDetected     != null) onFraudDetected.accept(event);
        if (onFraudScoreUpdated != null) onFraudScoreUpdated.accept(fraudScore);

        // ── Envoyer à l'API → la décision revient via callback ───
        if (sessionId != null && apiClient.isApiAvailable()) {
            String severity = getSeverity(type);
            apiClient.logEvent(
                    sessionId,
                    type.name(),
                    description,
                    fraudScore,
                    severity,
                    decision -> {
                        // L'API retourne "TERMINATE" ou "CONTINUE"
                        if ("TERMINATE".equals(decision) && !quizTerminated) {
                            quizTerminated = true;
                            stopMonitoring();
                            Platform.runLater(() -> {
                                showTerminationAlert();
                                if (onTerminate != null) onTerminate.run();
                            });
                        }
                        // Si CONTINUE → évaluation locale aussi
                        else {
                            evaluateThresholdsLocal();
                        }
                    }
            );
        } else {
            // Mode local (API indisponible)
            evaluateThresholdsLocal();
        }
    }

    /**
     * Évaluation locale — utilisée en fallback si l'API est down.
     */
    private void evaluateThresholdsLocal() {
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
                onWarning.accept("Suspicious activity — score: "
                        + fraudScore + "/" + THRESHOLD_TERMINATE);
        }
    }

    private String getSeverity(FraudEvent.Type type) {
        return switch (type) {
            case FOCUS_LOST, FULLSCREEN_EXIT, SUSPICIOUS_PATTERN -> "HIGH";
            case INACTIVITY, FAST_ANSWER                         -> "MEDIUM";
            default                                               -> "LOW";
        };
    }

    // ═══════════════════════════════════════════════════════════
    //  TIMERS
    // ═══════════════════════════════════════════════════════════

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

    public void resetInactivityTimer() { startInactivityTimer(); }

    // ═══════════════════════════════════════════════════════════
    //  OVERLAYS
    // ═══════════════════════════════════════════════════════════

    private void showOverlay(String message, String bgColor) {
        closeOverlay();
        Stage overlay = new Stage(StageStyle.TRANSPARENT);
        overlay.setAlwaysOnTop(true);

        Label lbl = new Label(message);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:700;-fx-text-alignment:center;");

        VBox box = new VBox(lbl);
        box.setAlignment(Pos.CENTER);
        box.setStyle(
                "-fx-background-color:" + bgColor + ";-fx-background-radius:12;" +
                        "-fx-padding:18 24;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.7),20,0,0,6);"
        );
        box.setMaxWidth(280);

        Scene scene = new Scene(box);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        overlay.setScene(scene);

        if (monitoredStage != null) {
            overlay.setX(monitoredStage.getX() + monitoredStage.getWidth() - 310);
            overlay.setY(monitoredStage.getY() + 20);
        }
        overlay.show();
        warningOverlay = overlay;

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
                "Your quiz has been terminated.\n\n" +
                        "• Focus lost:       " + focusLostCount + " time(s)\n" +
                        "• Fullscreen exits: " + fullscreenExitCount + " time(s)\n" +
                        "• Fraud score:      " + fraudScore + "/" + THRESHOLD_TERMINATE + "\n\n" +
                        "Session ID: " + sessionId + "\n" +
                        "Your answers have been recorded."
        );
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════
    //  GETTERS / CALLBACKS
    // ═══════════════════════════════════════════════════════════

    public int     getFraudScore()          { return fraudScore; }
    public int     getFocusLostCount()      { return focusLostCount; }
    public int     getFullscreenExitCount() { return fullscreenExitCount; }
    public int     getPenaltyCount()        { return penaltyCount; }
    public boolean isTerminated()           { return quizTerminated; }
    public boolean isActive()               { return isActive; }
    public String  getSessionId()           { return sessionId; }
    public boolean isApiMode()              { return apiClient.isApiAvailable(); }

    public List<FraudEvent> getEventLog()   { return Collections.unmodifiableList(eventLog); }

    public String getSummary() {
        return String.format(
                "Fraud Score: %d/%d | Focus Lost: %d | Fullscreen: %d | Penalties: %d | Mode: %s",
                fraudScore, THRESHOLD_TERMINATE, focusLostCount, fullscreenExitCount, penaltyCount,
                apiClient.isApiAvailable() ? "API" : "Local"
        );
    }

    public double applyPenaltiesToScore(double rawScore) {
        return Math.max(0, rawScore - penaltyCount);
    }

    public void setOnFraudDetected(Consumer<FraudEvent> cb)  { this.onFraudDetected = cb; }
    public void setOnWarning(Consumer<String> cb)             { this.onWarning = cb; }
    public void setOnTerminate(Runnable cb)                   { this.onTerminate = cb; }
    public void setOnPenalty(Consumer<Integer> cb)            { this.onPenalty = cb; }
    public void setOnFraudScoreUpdated(Consumer<Integer> cb)  { this.onFraudScoreUpdated = cb; }

    private void log(String msg) {
        System.out.println("[AntiFraud] " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " — " + msg);
    }

}