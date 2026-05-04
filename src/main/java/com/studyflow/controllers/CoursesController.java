package com.studyflow.controllers;

import com.studyflow.game.Building;
import com.studyflow.game.GamePointsService;
import com.studyflow.services.EvaluationMatiereService;
import com.studyflow.services.MatiereService;
import com.studyflow.models.EvaluationMatiere;
import com.studyflow.models.Matiere;
import com.studyflow.models.User;
import com.studyflow.utils.UserSession;
import com.studyflow.services.AIQuizService;
import com.studyflow.services.AntiFraudEngine;
import com.studyflow.services.FraudEvent;
import com.studyflow.services.VoiceAssistantService;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.control.Tooltip;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.net.URL;
import com.studyflow.utils.PdfExporter;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.chart.*;

public class CoursesController implements Initializable {

    // ════════════════════════════════════════════════════════════
    //  FXML — Stats cards
    // ════════════════════════════════════════════════════════════
    @FXML private Label statTotal;
    @FXML private Label statMoyenne;
    @FXML private Label statLacunes;
    @FXML private Label statMeilleur;
    @FXML private HBox  statCards;
    @FXML private VBox  smartQuizCard;
    @FXML private HBox  flamyBookCard;
    @FXML private StackPane flameMascotPane;
    @FXML private Label flamyTitleLabel;
    @FXML private Label flamyMoodLabel;
    @FXML private Label flamyLevelLabel;
    @FXML private Label flamyMessageLabel;
    @FXML private Label flamyProgressLabel;
    @FXML private Label flamyCoinsLabel;
    @FXML private ProgressBar flamyProgressBar;
    @FXML private HBox flameEffectsRow;
    @FXML private FlowPane themeShopFlow;

    // ── Header ─────────────────────────────────────────────────
    @FXML private Label            pageTitle;
    @FXML private Label            pageSubtitle;
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button           btnHeaderAction;
    @FXML private FontIcon         btnHeaderIcon;
    @FXML private Button           btnVoiceAssistant;
    @FXML private ComboBox<String> cmbLanguage;

    // ── Voice Assistant UI ─────────────────────────────────────
    @FXML private VBox              voiceBar;
    @FXML private Label             voiceStatusLabel;
    @FXML private Label             voiceTranscriptionLabel;
    @FXML private Label             voiceResponseLabel;
    @FXML private ProgressIndicator voiceProgress;
    @FXML private Button            btnVoiceMic;
    @FXML private Button            btnVoiceCancel;
    @FXML private Button            btnVoiceStopAudio;
    @FXML private Button            btnVoicePlayAudio;
    @FXML private CheckBox          voiceReadAloudToggle;

    // ── Vues ───────────────────────────────────────────────────
    @FXML private VBox     listView;
    @FXML private VBox     formPanel;
    @FXML private VBox     statsView;
    @FXML private VBox     quizMainView;
    @FXML private VBox     cityView;
    @FXML private FlowPane courseGrid;

    // ── Formulaire ─────────────────────────────────────────────
    @FXML private Label              formTitle;
    @FXML private ComboBox<Matiere>  cmbMatiere;
    @FXML private TextField          fldScore;
    @FXML private TextField          fldNoteMax;
    @FXML private DatePicker         dpDate;
    @FXML private TextField          fldDuree;
    @FXML private ComboBox<String>   cmbPriorite;
    @FXML private Button             btnSave;
    @FXML private Label              formError;
    @FXML private Button             btnPrioHaute;
    @FXML private Button             btnPrioMoyenne;
    @FXML private Button             btnPrioBasse;
    @FXML private Label              errMatiere, errScore, errNoteMax;
    @FXML private Label              errDuree, errPriorite, errDate, errSection;
    @FXML private ComboBox<String>   cmbSection;

    // ── Stats ──────────────────────────────────────────────────
    @FXML private BarChart<String,Number>  scoreBySubjectChart;
    @FXML private PieChart                 priorityPieChart;
    @FXML private LineChart<String,Number> progressLineChart;
    @FXML private HBox                     insightBox;

    // ── Quiz setup ─────────────────────────────────────────────
    @FXML private VBox        quizSetupView;
    @FXML private VBox        quizLoadingView;
    @FXML private VBox        quizInProgressView;
    @FXML private ScrollPane  quizResultsView;
    @FXML private VBox        quizVboxDetailedResults;
    @FXML private ComboBox<String>  cmbQuizSection;
    @FXML private ComboBox<Matiere> cmbQuizMatiere;
    @FXML private ComboBox<String>  cmbQuizDifficulty;
    @FXML private ComboBox<Integer> cmbQuizCount;
    @FXML private Label             lblQuizSetupError;
    @FXML private Label             lblQuizLoadingStatus;
    @FXML private ProgressIndicator quizLoadingSpinner;
    @FXML private Button            btnGenerateAIQuiz;

    // ── Quiz in progress ───────────────────────────────────────
    @FXML private Label       lblQuizQuestionNum;
    @FXML private Label       lblQuizDiffBadge;
    @FXML private Label       lblQuizCategoryBadge;
    @FXML private Label       lblQuizQuestionText;
    @FXML private VBox        quizVboxOptions;
    @FXML private ProgressBar quizProgressBar;
    @FXML private Label       lblQuizTimer;
    @FXML private Label       lblQuizProgress;
    @FXML private Label       lblQuizAnsweredCount;
    @FXML private Button      btnQuizNext;

    // ── Quiz results ───────────────────────────────────────────
    @FXML private Label lblQuizFinalScore;
    @FXML private Label lblQuizFinalPercent;
    @FXML private Label lblQuizFinalLevel;
    @FXML private Label lblQuizCorrectCount;
    @FXML private Label lblQuizWrongCount;
    @FXML private Label lblQuizTotalTime;

    // ════════════════════════════════════════════════════════════
    //  SERVICES
    // ════════════════════════════════════════════════════════════
    private final EvaluationMatiereService evalService  = new EvaluationMatiereService();
    private final MatiereService           matService   = new MatiereService();
    private final VoiceAssistantService    voiceService = new VoiceAssistantService();

    // ════════════════════════════════════════════════════════════
    //  CITY GAME — controller reference
    // ════════════════════════════════════════════════════════════
    private CityViewController cityViewController;
    private boolean cityViewLoading = false;

    // ════════════════════════════════════════════════════════════
    //  ANTI-FRAUD ENGINE
    // ════════════════════════════════════════════════════════════
    private final AntiFraudEngine antiFraud = new AntiFraudEngine();

    private Label   lblFraudScore;
    private Label   lblFraudWarning;
    private HBox    fraudStatusBar;
    private boolean antiFraudAttached = false;

    // ════════════════════════════════════════════════════════════
    //  STATE
    // ════════════════════════════════════════════════════════════
    private ObservableList<EvaluationMatiere> allEvals = FXCollections.observableArrayList();
    private EvaluationMatiere editTarget  = null;
    private String selectedPriorite       = null;

    private List<QuizQuestion> quizQuestions    = new ArrayList<>();
    private int     quizCurrentIndex  = 0;
    private int     quizScore         = 0;
    private int     quizAnsweredCount = 0;
    private String  quizSelectedOpt   = null;
    private Timeline quizTimer;
    private int     quizSecondsElapsed = 0;

    private static final String[] COLORS = {"primary","success","accent","warning","danger"};
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final PseudoClass SELECTED   = PseudoClass.getPseudoClass("selected");

    private boolean isRecordingVoice = false;
    private String lastVoiceResponse = "";
    private String selectedFlamyTheme = "Cozy Desk";
    private long currentFlamyTodayCount = 0;
    private int currentFlamyLevel = 1;
    private int currentFlamyStreak = 0;
    private String previewFlamyEffect = "";
    private static final List<FlamyTheme> FLAMY_THEMES = List.of(
            new FlamyTheme("Cozy Desk", 0, "#1F1306", "#3B1D08", "#FB923C", "Warm desk"),
            new FlamyTheme("Magic Library", 90, "#20123B", "#4C1D95", "#A78BFA", "Floating books"),
            new FlamyTheme("Night Study", 140, "#07111F", "#0F3460", "#38BDF8", "Quiet stars"),
            new FlamyTheme("Candy Notes", 190, "#351526", "#9D174D", "#F9A8D4", "Pastel glow"),
            new FlamyTheme("Fire Academy", 240, "#2D0A0A", "#7C2D12", "#F97316", "Big flame"),
            new FlamyTheme("Forest Focus", 300, "#071A12", "#14532D", "#34D399", "Calm leaves")
    );

    private record FlamyTheme(String name, int price, String from, String to, String accent, String perk) {}

    // ════════════════════════════════════════════════════════════
    //  QUIZ MODEL
    // ════════════════════════════════════════════════════════════
    private static class QuizQuestion {
        String question, correctAnswer, userAnswer, difficulty, category, explanation;
        List<String> options;
        QuizQuestion(String q, List<String> opts, String correct,
                     String diff, String cat, String expl) {
            question = q; options = opts; correctAnswer = correct;
            difficulty = diff; category = cat; explanation = expl;
        }
    }

    // ════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        cmbPriorite.setItems(FXCollections.observableArrayList("High", "Medium", "Low"));
        sortCombo.setItems(FXCollections.observableArrayList(
                "Date ↓","Date ↑","Score ↓","Score ↑","Durée ↓","Priority"));
        sortCombo.getSelectionModel().selectFirst();
        sortCombo.setOnAction(e -> applySort());

        fldScore.textProperty().addListener((o,old,n)  -> { if(!n.matches("\\d*(\\.\\d*)?")) fldScore.setText(old); });
        fldNoteMax.textProperty().addListener((o,old,n) -> { if(!n.matches("\\d*(\\.\\d*)?")) fldNoteMax.setText(old); });
        fldDuree.textProperty().addListener((o,old,n)  -> { if(!n.matches("\\d*")) fldDuree.setText(old); });

        try {
            List<String> secs = matService.findAll().stream()
                    .map(Matiere::getSectionMatiere)
                    .filter(Objects::nonNull).distinct().sorted()
                    .collect(Collectors.toList());
            cmbSection.setItems(FXCollections.observableArrayList(secs));
            cmbMatiere.setItems(FXCollections.observableArrayList());
        } catch (Exception e) { e.printStackTrace(); }

        searchField.textProperty().addListener((o,old,val) -> filterAndRender(val));

        dpDate.setDayCellFactory(p -> new DateCell() {
            @Override public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                if (d.isAfter(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color:#1E293B;-fx-text-fill:#475569;");
                } else {
                    setStyle("-fx-background-color:#0F172A;-fx-text-fill:#F8FAFC;");
                }
            }
        });

        cmbQuizDifficulty.setItems(FXCollections.observableArrayList("Easy","Medium","Hard"));
        cmbQuizDifficulty.getSelectionModel().selectFirst();
        cmbQuizCount.setItems(FXCollections.observableArrayList(5,10,15,20));
        cmbQuizCount.getSelectionModel().select(1);

        try {
            List<String> secs = matService.findAll().stream()
                    .map(Matiere::getSectionMatiere)
                    .filter(Objects::nonNull).distinct().sorted()
                    .collect(Collectors.toList());
            cmbQuizSection.setItems(FXCollections.observableArrayList(secs));
        } catch (Exception e) { e.printStackTrace(); }

        setupVoiceAssistant();
        setupVoiceLanguageSelector();
        setupVoiceAudioControls();
        setupAntiFraud();
        setupFlamyThemeShop();

        showView("list");
        loadData();
    }

    // ════════════════════════════════════════════════════════════
    //  CITY GAME — NAVIGATION (CORRECTED)
    // ════════════════════════════════════════════════════════════
    @FXML
    public void handleShowCity() {
        try {
            if (cityViewController == null) {
                showCityLoadingPlaceholder();
            }
            showView("city");

            if (cityViewController != null) {
                Platform.runLater(cityViewController::refresh);
                return;
            }

            if (cityViewLoading) return;
            cityViewLoading = true;

            PauseTransition delay = new PauseTransition(Duration.millis(120));
            delay.setOnFinished(e -> {
                try {
                    loadCityView();
                    cityViewLoading = false;
                    if (cityViewController != null) {
                        cityViewController.refresh();
                    }
                } catch (Exception ex) {
                    cityViewLoading = false;
                    ex.printStackTrace();
                    System.err.println("[CityGame] Erreur: " + ex.getMessage());
                    showCityErrorAlert(ex.getMessage());
                }
            });
            delay.play();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[CityGame] Erreur: " + e.getMessage());
            showCityErrorAlert(e.getMessage());
        }
    }

    @FXML
    public void handleShowCityMouse(javafx.scene.input.MouseEvent event) {
        handleShowCity();
    }

    private void loadCityView() throws java.io.IOException {
        if (cityView == null) {
            throw new IllegalStateException("cityView container is not injected from Courses.fxml");
        }

        if (cityViewController == null || cityView.getChildren().isEmpty()) {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/studyflow/views/Cityview.fxml"));
            Parent cityRoot = loader.load();
            cityViewController = loader.getController();
            cityView.getChildren().setAll(cityRoot);
        }

    }

    private void showCityLoadingPlaceholder() {
        if (cityView == null || cityViewLoading || cityViewController != null) return;

        VBox placeholder = new VBox(10);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setMinHeight(520);
        placeholder.setStyle("-fx-background-color:#060D1A;-fx-padding:36;");

        Label title = new Label("Loading My City...");
        title.setStyle("-fx-text-fill:#F8FAFC;-fx-font-size:22px;-fx-font-weight:800;");

        Label subtitle = new Label("Preparing the game map");
        subtitle.setStyle("-fx-text-fill:#64748B;-fx-font-size:13px;");

        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(42, 42);
        progress.setStyle("-fx-accent:#7C3AED;");

        placeholder.getChildren().setAll(progress, title, subtitle);
        cityView.getChildren().setAll(placeholder);
    }

    private void showCityErrorAlert(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Impossible de charger My City");
        alert.setContentText("Erreur: " + error + "\n\nVerifiez que Cityview.fxml existe dans:\n" +
                "src/main/resources/com/studyflow/views/Cityview.fxml");
        alert.showAndWait();
    }

    // ════════════════════════════════════════════════════════════
    //  CITY GAME — BUILDING UNLOCK NOTIFICATION
    // ════════════════════════════════════════════════════════════
    private void checkAndNotifyBuildingUnlock(int pointsBeforeSave) {
        try {
            List<EvaluationMatiere> fresh = evalService.findByUser(
                    UserSession.getInstance().getCurrentUser().getId());
            int newTotal = GamePointsService.totalPoints(fresh);
            Building.Type before = Building.getBuildingForPoints(pointsBeforeSave);
            Building.Type after  = Building.getBuildingForPoints(newTotal);
            if (!before.name.equals(after.name)) {
                showUnlockAlert(after, newTotal);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showUnlockAlert(Building.Type bt, int totalPts) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("🏙️ New Building Unlocked!");
        alert.setHeaderText(bt.emoji + "  " + bt.name + " is now yours!");
        alert.setContentText(
                bt.description +
                        "\n\n🏆 Total city points: " + totalPts +
                        "\n\nClick on 'My City 🏙️' to admire your progress!");
        alert.showAndWait();
    }

    // ════════════════════════════════════════════════════════════
    //  VOICE ASSISTANT SETUP
    // ════════════════════════════════════════════════════════════
    private void setupVoiceAssistant() {
        if (!voiceService.hasApiKey()) {
            Platform.runLater(() -> {
                voiceStatusLabel.setText("Click Start Speaking to configure your Groq API key");
                voiceProgress.setVisible(false);
            });
        }

        voiceService.onTranscription = text -> Platform.runLater(() -> {
            voiceTranscriptionLabel.setText("🎤 You said: " + text);
            voiceStatusLabel.setText("✍️ Transcribed: "
                    + (text.length() > 40 ? text.substring(0, 40) + "…" : text));
        });

        voiceService.onAIResponse = response -> Platform.runLater(() -> {
            lastVoiceResponse = response == null ? "" : response;
            voiceResponseLabel.setText("🤖 Assistant: " + response);
            voiceStatusLabel.setText("💬 Assistant responded");
            if (btnVoicePlayAudio != null) {
                btnVoicePlayAudio.setDisable(lastVoiceResponse.isBlank());
            }
        });

        voiceService.onStatus = status -> Platform.runLater(() -> {
            voiceStatusLabel.setText(status);
            boolean idle = status.contains("Ready") || status.contains("ready")
                    || status.contains("Prêt") || status.contains("prêt")
                    || status.contains("click");
            voiceProgress.setVisible(!idle);
        });

        voiceService.onError = error -> Platform.runLater(() -> {
            voiceStatusLabel.setText("❌ Error: " + error);
            voiceTranscriptionLabel.setText("");
            voiceResponseLabel.setText("");
            voiceProgress.setVisible(false);
            btnVoiceMic.setDisable(false);
            btnVoiceMic.setText("Start Speaking");
            btnVoiceMic.setStyle("-fx-background-color:#7C3AED;-fx-text-fill:white;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:12 24;-fx-font-size:14px;-fx-font-weight:900;-fx-cursor:hand;");
            isRecordingVoice = false;
        });

        voiceService.onAudioStart = () -> Platform.runLater(() -> {
            voiceStatusLabel.setText("🔊 Speaking…");
            voiceProgress.setVisible(true);
            if (btnVoiceStopAudio != null) {
                btnVoiceStopAudio.setDisable(false);
            }
        });

        voiceService.onAudioDone = () -> Platform.runLater(() -> {
            voiceStatusLabel.setText("🎤 Ready — click mic to ask");
            btnVoiceMic.setDisable(false);
            btnVoiceMic.setText("Start Speaking");
            btnVoiceMic.setStyle("-fx-background-color:#7C3AED;-fx-text-fill:white;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:12 24;-fx-font-size:14px;-fx-font-weight:900;-fx-cursor:hand;");
            voiceProgress.setVisible(false);
            if (btnVoiceStopAudio != null) {
                btnVoiceStopAudio.setDisable(true);
            }
            isRecordingVoice = false;
        });
    }

    @FXML
    private void handleShowVoicePage() {
        showView("voice");
        if (!voiceService.hasApiKey()) {
            voiceStatusLabel.setText("Click Start Speaking to configure your Groq API key");
            voiceProgress.setVisible(false);
        } else if (!isRecordingVoice) {
            voiceStatusLabel.setText("Ready with " + voiceService.getConfiguredProviderLabel() + " - click Start Speaking");
            voiceProgress.setVisible(false);
        }
    }

    @FXML
    private void handleVoiceAssistant() {
        if (!ensureGroqConfigured()) {
            return;
        }
        if (isRecordingVoice) stopVoiceRecording();
        else startVoiceRecording();
    }

    private void setupVoiceLanguageSelector() {
        if (cmbLanguage == null) return;
        cmbLanguage.setItems(FXCollections.observableArrayList(
                "Auto", "Francais", "English", "Arabic", "Spanish", "German",
                "Italian", "Portuguese", "Turkish"
        ));
        cmbLanguage.getSelectionModel().select("Auto");
        voiceService.setLanguage("auto");
        cmbLanguage.setOnAction(event -> {
            String selected = cmbLanguage.getValue();
            voiceService.setLanguage(selected);
            if (voiceStatusLabel != null && voiceBar != null && voiceBar.isVisible() && !isRecordingVoice) {
                voiceStatusLabel.setText("Language set to " + selected);
            }
        });
    }

    private void setupVoiceAudioControls() {
        if (voiceReadAloudToggle != null) {
            voiceReadAloudToggle.setSelected(true);
            voiceService.setSpeakResponses(true);
            voiceReadAloudToggle.selectedProperty().addListener((obs, oldValue, selected) ->
                    voiceService.setSpeakResponses(selected));
        }
        if (btnVoiceStopAudio != null) {
            btnVoiceStopAudio.setDisable(true);
        }
        if (btnVoicePlayAudio != null) {
            btnVoicePlayAudio.setDisable(true);
        }
    }

    private boolean ensureGroqConfigured() {
        if (voiceService.hasApiKey()) {
            return true;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Groq API Key");
        dialog.setHeaderText("Configure Groq for the voice assistant");
        dialog.setContentText("GROQ_API_KEY:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().trim().isBlank()) {
            showVoiceError("Groq API key is required before recording");
            return false;
        }

        if (!voiceService.configureGroqApiKey(result.get(), true)) {
            showVoiceError("Invalid Groq API key");
            return false;
        }

        voiceStatusLabel.setText("Groq configured - click Start Speaking");
        voiceProgress.setVisible(false);
        return true;
    }

    private void startVoiceRecording() {
        try {
            voiceService.startRecording();
            isRecordingVoice = true;
            btnVoiceMic.setText("Stop Recording");
            btnVoiceMic.setStyle("-fx-background-color:#EF4444;-fx-text-fill:white;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:12 24;-fx-font-size:14px;-fx-font-weight:900;-fx-cursor:hand;");
            showVoiceBar(true);
            voiceStatusLabel.setText("🎙️ Recording… click Stop when done");
            voiceTranscriptionLabel.setText("");
            voiceResponseLabel.setText("");
            lastVoiceResponse = "";
            if (btnVoicePlayAudio != null) {
                btnVoicePlayAudio.setDisable(true);
            }
            voiceProgress.setVisible(true);
            new Thread(() -> {
                try { Thread.sleep(30_000); } catch (InterruptedException ignored) {}
                if (isRecordingVoice) Platform.runLater(this::stopVoiceRecording);
            }).start();
        } catch (Exception e) {
            showVoiceError("Microphone error: " + e.getMessage());
            isRecordingVoice = false;
            btnVoiceMic.setText("Start Speaking");
            btnVoiceMic.setStyle("-fx-background-color:#7C3AED;-fx-text-fill:white;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:12 24;-fx-font-size:14px;-fx-font-weight:900;-fx-cursor:hand;");
        }
    }

    private void stopVoiceRecording() {
        if (!isRecordingVoice) return;
        isRecordingVoice = false;
        btnVoiceMic.setDisable(true);
        btnVoiceMic.setText("Processing...");
        voiceStatusLabel.setText("⏳ Processing your request…");
        voiceProgress.setVisible(true);
        voiceService.stopAndProcess();
    }

    @FXML
    private void handleCancelVoice() {
        if (isRecordingVoice) {
            voiceService.cancel();
            isRecordingVoice = false;
        }
        showView("voice");
        btnVoiceMic.setDisable(false);
        btnVoiceMic.setText("Start Speaking");
        btnVoiceMic.setStyle("-fx-background-color:#7C3AED;-fx-text-fill:white;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:12 24;-fx-font-size:14px;-fx-font-weight:900;-fx-cursor:hand;");
        voiceStatusLabel.setText("🎤 Voice assistant ready");
    }

    @FXML
    private void handleStopVoiceReading() {
        voiceService.stopSpeaking();
        if (btnVoiceStopAudio != null) {
            btnVoiceStopAudio.setDisable(true);
        }
        voiceProgress.setVisible(false);
        voiceStatusLabel.setText("Audio stopped");
    }

    @FXML
    private void handlePlayVoiceResponse() {
        if (lastVoiceResponse == null || lastVoiceResponse.isBlank()) {
            showVoiceError("No assistant answer to read yet");
            return;
        }
        voiceService.speakAsync(lastVoiceResponse);
    }

    private void showVoiceBar(boolean show) {
        if (show) showView("voice");
    }

    private void showVoiceError(String message) {
        Platform.runLater(() -> {
            voiceStatusLabel.setText("❌ " + message);
            voiceTranscriptionLabel.setText("");
            voiceResponseLabel.setText("");
            voiceBar.setVisible(true);
            voiceBar.setManaged(true);
            voiceProgress.setVisible(false);
            btnVoiceMic.setDisable(false);
            btnVoiceMic.setText("Start Speaking");
            btnVoiceMic.setStyle("-fx-background-color:#7C3AED;-fx-text-fill:white;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:12 24;-fx-font-size:14px;-fx-font-weight:900;-fx-cursor:hand;");
            isRecordingVoice = false;
        });
    }

    // ════════════════════════════════════════════════════════════
    //  ANTI-FRAUD ENGINE
    // ════════════════════════════════════════════════════════════
    private void setupAntiFraud() {
        antiFraud.setOnFraudDetected(event -> Platform.runLater(() -> {
            if (fraudStatusBar  != null) fraudStatusBar.setVisible(true);
            if (lblFraudWarning != null) {
                lblFraudWarning.setText(event.getType().label + ": " + event.getDetails());
                lblFraudWarning.setStyle("-fx-text-fill:" + event.getSeverityColor() + ";-fx-font-size:12px;");
            }
        }));

        antiFraud.setOnFraudScoreUpdated(score -> Platform.runLater(() -> {
            if (lblFraudScore == null) return;
            lblFraudScore.setText("⚠ " + score + "/" + AntiFraudEngine.THRESHOLD_TERMINATE);
            String col = score >= AntiFraudEngine.THRESHOLD_PENALTY ? "#EF4444"
                    : score >= AntiFraudEngine.THRESHOLD_WARNING  ? "#F59E0B" : "#34D399";
            lblFraudScore.setStyle("-fx-text-fill:" + col + ";-fx-font-size:12px;-fx-font-weight:700;");
        }));

        antiFraud.setOnPenalty(penaltyCount -> Platform.runLater(() -> {
            quizScore = Math.max(0, quizScore - 1);
        }));

        antiFraud.setOnWarning(msg -> Platform.runLater(() -> {
            if (lblFraudWarning != null) {
                lblFraudWarning.setText("⚠ " + msg);
                lblFraudWarning.setStyle("-fx-text-fill:#F59E0B;-fx-font-size:12px;");
            }
        }));

        antiFraud.setOnTerminate(() -> Platform.runLater(() -> {
            stopQuizTimer();
            finishQuiz();
        }));
    }

    private void attachAntiFraudOnce() {
        if (antiFraudAttached) return;
        antiFraudAttached = true;
        Platform.runLater(() -> {
            if (courseGrid.getScene() != null
                    && courseGrid.getScene().getWindow() instanceof Stage stage) {
                antiFraud.attach(stage);
                antiFraud.blockSystemActions(courseGrid.getScene());
            }
        });
    }

    private HBox buildFraudBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color:#1A0808;-fx-border-color:#EF4444;" +
                "-fx-border-width:0 0 0 4;-fx-padding:8 16;-fx-background-radius:8;");
        bar.setVisible(false);

        Label icon = new Label("🔒");
        icon.setStyle("-fx-font-size:13px;");

        lblFraudWarning = new Label("Secure exam mode active");
        lblFraudWarning.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:12px;");
        HBox.setHgrow(lblFraudWarning, Priority.ALWAYS);

        lblFraudScore = new Label("⚠ 0/" + AntiFraudEngine.THRESHOLD_TERMINATE);
        lblFraudScore.setStyle("-fx-text-fill:#34D399;-fx-font-size:12px;-fx-font-weight:700;");

        ProgressBar fraudBar = new ProgressBar(0);
        fraudBar.setPrefWidth(80);
        fraudBar.setPrefHeight(6);
        fraudBar.setStyle("-fx-accent:#EF4444;-fx-background-color:#1E293B;");

        antiFraud.setOnFraudScoreUpdated(score -> Platform.runLater(() -> {
            double prog = (double) score / AntiFraudEngine.THRESHOLD_TERMINATE;
            fraudBar.setProgress(Math.min(1.0, prog));
            if (score > 0) bar.setVisible(true);
            if (lblFraudScore != null) {
                lblFraudScore.setText("⚠ " + score + "/" + AntiFraudEngine.THRESHOLD_TERMINATE);
                String col = score >= AntiFraudEngine.THRESHOLD_PENALTY ? "#EF4444"
                        : score >= AntiFraudEngine.THRESHOLD_WARNING ? "#F59E0B" : "#34D399";
                lblFraudScore.setStyle("-fx-text-fill:" + col + ";-fx-font-size:12px;-fx-font-weight:700;");
            }
        }));

        bar.getChildren().addAll(icon, lblFraudWarning, fraudBar, lblFraudScore);
        fraudStatusBar = bar;
        return bar;
    }

    // ════════════════════════════════════════════════════════════
    //  VIEW SWITCHER
    // ════════════════════════════════════════════════════════════
    private void showView(String view) {
        boolean isList  = "list".equals(view);
        boolean isForm  = "form".equals(view);
        boolean isStats = "stats".equals(view);
        boolean isQuiz  = "quiz".equals(view);
        boolean isCity  = "city".equals(view);
        boolean isVoice = "voice".equals(view);

        listView.setVisible(isList);     listView.setManaged(isList);
        formPanel.setVisible(isForm);    formPanel.setManaged(isForm);
        statsView.setVisible(isStats);   statsView.setManaged(isStats);
        quizMainView.setVisible(isQuiz); quizMainView.setManaged(isQuiz);
        if (voiceBar != null) {
            voiceBar.setVisible(isVoice);
            voiceBar.setManaged(isVoice);
        }
        if (cityView != null) {
            cityView.setVisible(isCity);
            cityView.setManaged(isCity);
        }

        boolean showCards = isList || isStats;
        statCards.setVisible(showCards); statCards.setManaged(showCards);
        if (flamyBookCard != null) {
            flamyBookCard.setVisible(isList);
            flamyBookCard.setManaged(isList);
        }
        if (smartQuizCard != null) { smartQuizCard.setVisible(isList); smartQuizCard.setManaged(isList); }
        sortCombo.setVisible(isList);   sortCombo.setManaged(isList);
        searchField.setVisible(isList); searchField.setManaged(isList);

        if (isList) {
            btnHeaderAction.setText("New Assessment");
            if (btnHeaderIcon != null) btnHeaderIcon.setIconLiteral("fth-plus");
            pageTitle.setText("My Assessments");
            pageSubtitle.setText("Track your results and manage your assessments by subject");

        } else if (isForm) {
            btnHeaderAction.setText("← Back");
            if (btnHeaderIcon != null) btnHeaderIcon.setIconLiteral("fth-arrow-left");
            pageTitle.setText(editTarget == null ? "New Assessment" : "Edit Assessment");
            pageSubtitle.setText("Fill in all fields below and save");

        } else if (isStats) {
            btnHeaderAction.setText("← Back");
            if (btnHeaderIcon != null) btnHeaderIcon.setIconLiteral("fth-arrow-left");
            pageTitle.setText("My Statistics");
            pageSubtitle.setText("Analysis of your " + allEvals.size() + " assessment(s)");

        } else if (isQuiz) {
            btnHeaderAction.setText("← Back");
            if (btnHeaderIcon != null) btnHeaderIcon.setIconLiteral("fth-arrow-left");
            pageTitle.setText("Smart Quiz");
            pageSubtitle.setText("Test your knowledge — Secure exam mode");
            showQuizSubView("setup");
            attachAntiFraudOnce();

        } else if (isCity) {
            btnHeaderAction.setText("← Back");
            if (btnHeaderIcon != null) btnHeaderIcon.setIconLiteral("fth-arrow-left");
            pageTitle.setText("My City 🏙️");
            pageSubtitle.setText("Your academic progress as a growing city");
        } else if (isVoice) {
            btnHeaderAction.setText("Back");
            if (btnHeaderIcon != null) btnHeaderIcon.setIconLiteral("fth-arrow-left");
            pageTitle.setText("Voice Assistant");
            pageSubtitle.setText("Speech to text assistant for study questions");
        }
    }

    private void showQuizSubView(String sub) {
        boolean setup   = "setup".equals(sub);
        boolean loading = "loading".equals(sub);
        boolean quiz    = "quiz".equals(sub);
        boolean results = "results".equals(sub);

        quizSetupView.setVisible(setup);     quizSetupView.setManaged(setup);
        quizLoadingView.setVisible(loading); quizLoadingView.setManaged(loading);
        quizInProgressView.setVisible(quiz); quizInProgressView.setManaged(quiz);
        quizResultsView.setVisible(results); quizResultsView.setManaged(results);
    }

    // ════════════════════════════════════════════════════════════
    //  DATA
    // ════════════════════════════════════════════════════════════
    private void loadData() {
        User u = UserSession.getInstance().getCurrentUser();
        if (u == null) {
            allEvals.clear();
            courseGrid.getChildren().clear();
            statTotal.setText("0"); statMoyenne.setText("0");
            statLacunes.setText("0"); statMeilleur.setText("0");
            updateFlamyBook();
            return;
        }
        try {
            allEvals.setAll(evalService.findByUser(u.getId()));
            updateStats();
            updateFlamyBook();
            renderCards(allEvals);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateFlamyBook() {
        if (flamyBookCard == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        long todayCount = allEvals.stream()
                .filter(e -> today.equals(e.getDateEvaluation()))
                .count();
        double todayAvg = allEvals.stream()
                .filter(e -> today.equals(e.getDateEvaluation()))
                .mapToDouble(EvaluationMatiere::getScoreEval)
                .average()
                .orElse(0);
        int streak = computeEvaluationStreak();
        int total = allEvals.size();
        int level = Math.max(1, Math.min(20, 1 + total / 3 + streak / 4));
        int flameCoins = total * 35 + streak * 20 + (int) todayCount * 15;
        double dailyProgress = Math.min(1.0, todayCount / 4.0);
        currentFlamyTodayCount = todayCount;
        currentFlamyLevel = level;
        currentFlamyStreak = streak;

        if (FLAMY_THEMES.stream().filter(t -> t.name().equals(selectedFlamyTheme)).findFirst().map(FlamyTheme::price).orElse(0) > flameCoins) {
            selectedFlamyTheme = "Cozy Desk";
        }

        String mood;
        String message;
        if (todayCount == 0) {
            mood = "Sad little flame";
            message = "Flamy is quiet today. Add one assessment to relight the face and warm up the camp logs.";
        } else if (todayAvg > 0 && todayAvg < 10) {
            mood = "Worried but brave";
            message = "One tough result still gives energy. Review the weak subject and Flamy will keep glowing.";
        } else if (todayCount == 1) {
            mood = "Awake sparkle";
            message = "Nice start. One more assessment today will unlock a stronger flame effect.";
        } else if (todayCount < 4) {
            mood = "Happy study flame";
            message = "The pages are open and the flame is growing. Keep the daily streak alive.";
        } else {
            mood = "Radiant level rush";
            message = "Full daily flame. Garland lights, runner sparks, and star glow are active.";
        }

        flamyTitleLabel.setText("FlamyBook");
        flamyMoodLabel.setText(mood + " | " + streak + "-day streak");
        flamyLevelLabel.setText("Level " + level);
        flamyMessageLabel.setText(message);
        flamyProgressLabel.setText(todayCount + " / 4 today");
        flamyProgressBar.setProgress(dailyProgress);
        flamyCoinsLabel.setText(flameCoins + " flame coins");

        renderFlamyMascot(todayCount, level);
        renderFlamyEffects(todayCount, streak, level);
        renderThemeShop(flameCoins);
        applyFlamyTheme(selectedFlamyTheme);
    }

    private int computeEvaluationStreak() {
        Set<LocalDate> days = allEvals.stream()
                .map(EvaluationMatiere::getDateEvaluation)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        int streak = 0;
        LocalDate cursor = LocalDate.now();
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private void setupFlamyThemeShop() {
        renderThemeShop(0);
        applyFlamyTheme(selectedFlamyTheme);
        renderFlamyMascot(0, 1);
    }

    private void renderThemeShop(int flameCoins) {
        if (themeShopFlow == null) {
            return;
        }
        themeShopFlow.getChildren().clear();
        for (FlamyTheme theme : FLAMY_THEMES) {
            boolean selected = theme.name().equals(selectedFlamyTheme);
            boolean unlocked = flameCoins >= theme.price();
            Button button = new Button((selected ? "Selected " : unlocked ? "Use " : "Locked ") + theme.name());
            button.setTooltip(new Tooltip(theme.perk() + " | " + theme.price() + " coins"));
            button.setDisable(!unlocked);
            button.setStyle("-fx-background-color:" + (selected ? theme.accent() : "rgba(255,255,255,0.07)") + ";"
                    + "-fx-text-fill:" + (selected ? "#111827" : theme.accent()) + ";"
                    + "-fx-border-color:" + theme.accent() + ";"
                    + "-fx-border-width:1; -fx-border-radius:10; -fx-background-radius:10;"
                    + "-fx-padding:7 10; -fx-font-size:11px; -fx-font-weight:800; -fx-cursor:hand;");
            button.setOnAction(e -> {
                selectedFlamyTheme = theme.name();
                applyFlamyTheme(theme.name());
                renderThemeShop(flameCoins);
            });
            themeShopFlow.getChildren().add(button);
        }
    }

    private void applyFlamyTheme(String themeName) {
        if (flamyBookCard == null) {
            return;
        }
        FlamyTheme theme = FLAMY_THEMES.stream()
                .filter(t -> t.name().equals(themeName))
                .findFirst()
                .orElse(FLAMY_THEMES.get(0));
        flamyBookCard.setStyle("-fx-background-color:linear-gradient(to right," + theme.from() + "," + theme.to() + ");"
                + "-fx-border-color:" + theme.accent() + "; -fx-border-width:1.2;"
                + "-fx-border-radius:18; -fx-background-radius:18; -fx-padding:18 20;"
                + "-fx-effect:dropshadow(gaussian," + toRgba(theme.accent(), 0.28) + ",18,0,0,5);");
        flamyProgressBar.setStyle("-fx-accent:" + theme.accent() + "; -fx-background-color:rgba(0,0,0,0.25);"
                + "-fx-background-radius:999; -fx-border-radius:999;");
    }

    private void renderFlamyEffects(long todayCount, int streak, int level) {
        if (flameEffectsRow == null) {
            return;
        }
        flameEffectsRow.getChildren().clear();
        addEffectChip("Garland", "garland", streak >= 3, "needs 3-day streak", "#FBBF24");
        addEffectChip("Runner sparks", "runner", todayCount >= 2, "needs 2 assessments today", "#38BDF8");
        addEffectChip("Star glow", "star", level >= 5, "needs level 5", "#A78BFA");
    }

    private void addEffectChip(String text, String effect, boolean active, String lockedReason, String color) {
        Button chip = new Button((active ? "Play " : "Locked ") + text);
        chip.setTooltip(new Tooltip(active ? "Click to preview " + text : lockedReason));
        chip.setStyle("-fx-text-fill:" + (active ? color : "#64748B") + ";"
                + "-fx-background-color:" + (active ? toRgba(color, 0.16) : "rgba(100,116,139,0.12)") + ";"
                + "-fx-border-color:" + (active ? color : "#334155") + ";"
                + "-fx-border-width:1; -fx-background-radius:999; -fx-border-radius:999;"
                + "-fx-padding:4 9; -fx-font-size:10px; -fx-font-weight:800; -fx-cursor:hand;");
        chip.setOnAction(e -> {
            if (!active) {
                flamyMessageLabel.setText(text + " is locked: " + lockedReason + ".");
                renderFlamyMascot(currentFlamyTodayCount, currentFlamyLevel);
                return;
            }
            playFlamyEffect(effect, text);
        });
        flameEffectsRow.getChildren().add(chip);
    }

    private void playFlamyEffect(String effect, String label) {
        previewFlamyEffect = effect;
        flamyMessageLabel.setText(label + " effect preview is playing.");
        renderFlamyMascot(currentFlamyTodayCount, currentFlamyLevel);
        PauseTransition reset = new PauseTransition(Duration.seconds(1.8));
        reset.setOnFinished(e -> {
            previewFlamyEffect = "";
            updateFlamyBook();
        });
        reset.play();
    }

    private void renderFlamyMascot(long todayCount, int level) {
        if (flameMascotPane == null) {
            return;
        }
        Pane art = new Pane();
        art.setPrefSize(150, 136);

        boolean garlandActive = currentFlamyStreak >= 3 || "garland".equals(previewFlamyEffect);
        boolean runnerActive = todayCount >= 2 || "runner".equals(previewFlamyEffect);
        boolean starActive = level >= 5 || "star".equals(previewFlamyEffect);

        if (starActive) {
            Circle aura = new Circle(75, 62, "star".equals(previewFlamyEffect) ? 61 : 50, Color.web("#FDE68A", 0.11));
            aura.setStroke(Color.web("#FBBF24", 0.34));
            aura.setStrokeWidth(2);
            art.getChildren().add(aura);
            addStar(20, 31, 5, "#FDE68A", art);
            addStar(132, 39, 4, "#FDE68A", art);
            addStar(116, 16, 3.5, "#FDE68A", art);
        }

        if (garlandActive) {
            QuadCurve cord = new QuadCurve(12, 20, 75, "garland".equals(previewFlamyEffect) ? 42 : 31, 138, 20);
            cord.setFill(Color.TRANSPARENT);
            cord.setStroke(Color.web("#FBBF24"));
            cord.setStrokeWidth(1.5);
            art.getChildren().add(cord);
            for (int i = 0; i < 6; i++) {
                double x = 19 + i * 22;
                double y = 24 + Math.sin(i * 0.95) * 7;
                Circle bulb = new Circle(x, y, "garland".equals(previewFlamyEffect) ? 4.5 : 3.4,
                        Color.web(i % 2 == 0 ? "#FDE68A" : "#FB923C"));
                bulb.setStroke(Color.web("#7C2D12"));
                bulb.setStrokeWidth(1);
                art.getChildren().add(bulb);
            }
        }

        SVGPath outerFlame = new SVGPath();
        outerFlame.setContent("M75 5 C95 30 85 42 101 58 C111 35 128 55 119 78 C136 69 139 101 113 113 C92 124 59 124 38 113 C13 99 22 70 35 77 C29 54 45 35 52 57 C53 37 61 21 75 5 Z");
        outerFlame.setFill(Color.web(todayCount == 0 ? "#F59E0B" : "#F97316"));
        outerFlame.setStroke(Color.web("#B45309"));
        outerFlame.setStrokeWidth(2.4);

        SVGPath midFlame = new SVGPath();
        midFlame.setContent("M76 26 C90 45 82 57 94 70 C101 56 110 70 104 88 C115 89 108 106 91 112 C78 117 58 114 49 103 C37 88 47 72 55 84 C54 66 64 45 76 26 Z");
        midFlame.setFill(Color.web(todayCount >= 4 ? "#FEF3C7" : "#FBBF24"));
        midFlame.setOpacity(todayCount == 0 ? 0.62 : 1);

        SVGPath innerFlame = new SVGPath();
        innerFlame.setContent("M77 43 C86 58 78 69 88 80 C94 72 99 85 94 96 C88 106 68 109 59 96 C51 84 62 76 66 84 C65 68 70 54 77 43 Z");
        innerFlame.setFill(Color.web(todayCount >= 4 ? "#FFFFFF" : "#FDBA74"));
        innerFlame.setOpacity(todayCount == 0 ? 0.35 : 0.78);

        art.getChildren().addAll(outerFlame, midFlame, innerFlame);

        Circle leftEye = new Circle(58, 80, todayCount == 0 ? 5 : 7, Color.web("#3B1F16"));
        Circle rightEye = new Circle(91, 80, todayCount == 0 ? 5 : 7, Color.web("#3B1F16"));
        Circle leftEyeSpark = new Circle(55, 77, 2.1, Color.WHITE);
        Circle rightEyeSpark = new Circle(88, 77, 2.1, Color.WHITE);
        Circle blushL = new Circle(48, 91, 5.2, Color.web("#FB7185", 0.72));
        Circle blushR = new Circle(102, 91, 5.2, Color.web("#FB7185", 0.72));

        Arc mouth = new Arc(75, todayCount == 0 ? 96 : 91, todayCount == 0 ? 8 : 7, 5,
                todayCount == 0 ? 20 : 200, 140);
        mouth.setType(ArcType.OPEN);
        mouth.setFill(Color.TRANSPARENT);
        mouth.setStroke(Color.web("#3B1F16"));
        mouth.setStrokeWidth(2);

        Line browL = new Line(52, 69, 61, todayCount == 0 ? 67 : 68);
        Line browR = new Line(88, todayCount == 0 ? 67 : 68, 98, 69);
        browL.setStroke(Color.web("#3B1F16"));
        browR.setStroke(Color.web("#3B1F16"));
        browL.setStrokeWidth(2);
        browR.setStrokeWidth(2);
        browL.setStrokeLineCap(StrokeLineCap.ROUND);
        browR.setStrokeLineCap(StrokeLineCap.ROUND);

        art.getChildren().addAll(blushL, blushR, leftEye, rightEye, leftEyeSpark, rightEyeSpark, browL, browR, mouth);

        if (todayCount == 0) {
            Circle tear = new Circle(98, 86, 3, Color.web("#38BDF8"));
            art.getChildren().add(tear);
        }

        addLog(34, 112, -16, art);
        addLog(75, 116, 0, art);
        addLog(116, 112, 16, art);

        if (runnerActive) {
            for (int i = 0; i < ("runner".equals(previewFlamyEffect) ? 7 : 4); i++) {
                double x = 18 + i * 16;
                double y = 122 - (i % 2) * 10;
                Polygon spark = new Polygon(x, y - 7, x + 5, y, x, y + 7, x - 5, y);
                spark.setFill(Color.web(i % 2 == 0 ? "#FDBA74" : "#38BDF8"));
                spark.setStroke(Color.web("#7C2D12", 0.5));
                spark.setStrokeWidth(0.8);
                art.getChildren().add(spark);
            }
        }

        flameMascotPane.getChildren().setAll(art);
    }

    private void addLog(double x, double y, double rotate, Pane art) {
        Rectangle log = new Rectangle(x - 19, y - 6, 38, 12);
        log.setArcWidth(10);
        log.setArcHeight(10);
        log.setFill(Color.web("#8B4513"));
        log.setStroke(Color.web("#4A250B"));
        log.setStrokeWidth(2);
        log.setRotate(rotate);
        Circle ring = new Circle(x + 13, y, 5, Color.web("#A16207"));
        ring.setStroke(Color.web("#4A250B"));
        ring.setStrokeWidth(1.5);
        ring.setRotate(rotate);
        art.getChildren().addAll(log, ring);
    }

    private void addStar(double x, double y, double size, String color, Pane art) {
        Polygon star = new Polygon(
                x, y - size,
                x + size * 0.35, y - size * 0.35,
                x + size, y,
                x + size * 0.35, y + size * 0.35,
                x, y + size,
                x - size * 0.35, y + size * 0.35,
                x - size, y,
                x - size * 0.35, y - size * 0.35
        );
        star.setFill(Color.web(color));
        star.setStroke(Color.web("#7C2D12", 0.45));
        star.setStrokeWidth(0.8);
        art.getChildren().add(star);
    }

    private String toRgba(String hex, double alpha) {
        Color color = Color.web(hex);
        return String.format(Locale.US, "rgba(%d,%d,%d,%.2f)",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255),
                alpha);
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(allEvals.size()));
        double avg = allEvals.stream().mapToDouble(EvaluationMatiere::getScoreEval).average().orElse(0);
        statMoyenne.setText(String.format("%.1f", avg));
        statLacunes.setText(String.valueOf(allEvals.stream().mapToLong(EvaluationMatiere::getDureeEvaluation).sum()));
        double best = allEvals.stream().mapToDouble(EvaluationMatiere::getScoreEval).max().orElse(0);
        statMeilleur.setText(String.format("%.1f", best));
    }

    private void renderCards(List<EvaluationMatiere> list) {
        courseGrid.getChildren().clear();
        for (int i = 0; i < list.size(); i++)
            courseGrid.getChildren().add(buildCard(list.get(i), COLORS[i % COLORS.length]));
    }

    // ════════════════════════════════════════════════════════════
    //  FORM HANDLERS
    // ════════════════════════════════════════════════════════════
    @FXML private void handleShowForm() {
        editTarget = null; clearForm();
        formTitle.setText("New Assessment");
        btnSave.setText("➕  Add Assessment");
        showView("form");
    }

    @FXML private void handleSave() {
        boolean ok = true;
        clearErrors();

        if (cmbSection.getValue() == null || cmbSection.getValue().isBlank()) {
            errSection.setText("⚠ Please select a section."); ok = false;
        }
        if (cmbMatiere.getValue() == null) {
            errMatiere.setText("⚠ Please select a subject."); ok = false;
        }

        double score = 0, noteMax = 0;
        try {
            if (fldScore.getText().isBlank()) throw new NumberFormatException();
            score = Double.parseDouble(fldScore.getText().trim());
            if (score < 0) { errScore.setText("⚠ Score cannot be negative."); ok = false; }
        } catch (NumberFormatException ex) { errScore.setText("⚠ Enter a valid number."); ok = false; }

        try {
            if (fldNoteMax.getText().isBlank()) throw new NumberFormatException();
            noteMax = Double.parseDouble(fldNoteMax.getText().trim());
            if (noteMax <= 0)  { errNoteMax.setText("⚠ Must be > 0.");   ok = false; }
            else if (noteMax > 20) { errNoteMax.setText("⚠ Max is 20."); ok = false; }
        } catch (NumberFormatException ex) { errNoteMax.setText("⚠ Enter a valid number."); ok = false; }

        if (ok && score > noteMax) { errScore.setText("⚠ Score cannot exceed max."); ok = false; }

        int duree = 0;
        try {
            if (fldDuree.getText().isBlank()) throw new NumberFormatException();
            duree = Integer.parseInt(fldDuree.getText().trim());
            if (duree <= 0) { errDuree.setText("⚠ Must be > 0."); ok = false; }
        } catch (NumberFormatException ex) { errDuree.setText("⚠ Enter a whole number."); ok = false; }

        if (selectedPriorite == null) { errPriorite.setText("⚠ Please select a priority."); ok = false; }

        if (dpDate.getValue() == null) {
            errDate.setText("⚠ Please select a date."); ok = false;
        } else if (dpDate.getValue().isAfter(LocalDate.now())) {
            errDate.setText("⚠ Date cannot be in the future."); ok = false;
        }

        User u = UserSession.getInstance().getCurrentUser();
        if (u == null) { showGlobalError("You must be logged in."); return; }
        if (!ok) return;

        // Snapshot points BEFORE saving (for building unlock check)
        int pointsBeforeSave = GamePointsService.totalPoints(new ArrayList<>(allEvals));

        try {
            EvaluationMatiere e = editTarget != null ? editTarget : new EvaluationMatiere();
            e.setScoreEval(score);
            e.setNoteMaximaleEval(noteMax);
            e.setDateEvaluation(dpDate.getValue());
            e.setDureeEvaluation(duree);
            e.setPrioriteE(selectedPriorite);
            e.setMatiereId(cmbMatiere.getValue().getId());
            e.setUserId(u.getId());
            if (editTarget == null) evalService.create(e, cmbMatiere.getValue().getId());
            else                    evalService.update(e);

            clearForm();
            showView("list");
            loadData();

            // Check if a new building was unlocked
            checkAndNotifyBuildingUnlock(pointsBeforeSave);

        } catch (Exception ex) { showGlobalError("Database error: " + ex.getMessage()); }
    }

    @FXML private void handleCancel() { clearForm(); showView("list"); }

    private void startEdit(EvaluationMatiere ev) {
        editTarget = ev;
        try {
            Matiere mat = matService.findById(ev.getMatiereId());
            if (mat != null && mat.getSectionMatiere() != null) {
                cmbSection.setValue(mat.getSectionMatiere());
                handleSectionChange();
            }
            cmbMatiere.getItems().stream()
                    .filter(m -> m.getId() == ev.getMatiereId())
                    .findFirst().ifPresent(cmbMatiere::setValue);
        } catch (Exception e) { e.printStackTrace(); }

        fldScore.setText(String.valueOf(ev.getScoreEval()));
        fldNoteMax.setText(String.valueOf(ev.getNoteMaximaleEval()));
        dpDate.setValue(ev.getDateEvaluation());
        fldDuree.setText(String.valueOf(ev.getDureeEvaluation()));
        if (ev.getPrioriteE() != null) setPriorite(ev.getPrioriteE());
        formTitle.setText("Edit Assessment");
        btnSave.setText("💾  Update");
        showView("form");
    }

    private void deleteItem(EvaluationMatiere ev) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this assessment?", ButtonType.YES, ButtonType.CANCEL);
        a.setTitle("Confirm deletion");
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                try { evalService.delete(ev.getIdEval()); loadData(); }
                catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════════════════════════
    @FXML private void handleHeaderAction() {
        if (formPanel.isVisible() || statsView.isVisible() || quizMainView.isVisible()
                || (cityView != null && cityView.isVisible())
                || (voiceBar != null && voiceBar.isVisible())) {
            stopQuizTimer();
            antiFraud.stopMonitoring();
            showView("list");
        } else {
            handleShowForm();
        }
    }

    @FXML public void handleShowQuiz()  { showView("quiz"); }

    @FXML public void handleShowStats() {
        if (allEvals.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "No assessments yet.", ButtonType.OK).showAndWait();
            return;
        }
        populateStats(); showView("stats");
    }

    // ════════════════════════════════════════════════════════════
    //  QUIZ — SECTION CHANGE
    // ════════════════════════════════════════════════════════════
    @FXML private void handleQuizSectionChange() {
        String sec = cmbQuizSection.getValue();
        if (sec == null) return;
        try {
            List<Matiere> filtered = matService.findAll().stream()
                    .filter(m -> sec.equals(m.getSectionMatiere()))
                    .collect(Collectors.toList());
            cmbQuizMatiere.setItems(FXCollections.observableArrayList(filtered));
            cmbQuizMatiere.setPromptText("Choose a subject…");
            cmbQuizMatiere.getSelectionModel().clearSelection();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ════════════════════════════════════════════════════════════
    //  QUIZ — AI GENERATION
    // ════════════════════════════════════════════════════════════
    @FXML private void handleGenerateAIQuiz() {
        lblQuizSetupError.setText("");
        if (cmbQuizSection.getValue() == null) {
            lblQuizSetupError.setText("⚠ Please select a section."); return;
        }
        Matiere mat = cmbQuizMatiere.getValue();
        if (mat == null) {
            lblQuizSetupError.setText("⚠ Please select a subject."); return;
        }

        String matiere    = mat.getNomMatiere();
        String section    = cmbQuizSection.getValue();
        String difficulty = cmbQuizDifficulty.getValue() != null ? cmbQuizDifficulty.getValue() : "Medium";
        int    count      = cmbQuizCount.getValue() != null ? cmbQuizCount.getValue() : 10;
        String level      = detectLevel(getAvgForSubject(mat.getId()));

        showQuizLoading(true);
        lblQuizLoadingStatus.setText("Claude AI is generating your questions…");

        new Thread(() -> {
            try {
                AIQuizService service = new AIQuizService();
                List<AIQuizService.ParsedQuestion> parsed =
                        service.generateQuizQuestions(matiere, section, level, count);
                Platform.runLater(() -> {
                    showQuizLoading(false);
                    if (parsed.isEmpty()) {
                        lblQuizSetupError.setText("⚠ AI returned no questions. Check your API key.");
                        showQuizSubView("setup"); return;
                    }
                    quizQuestions.clear();
                    for (AIQuizService.ParsedQuestion pq : parsed) {
                        quizQuestions.add(new QuizQuestion(
                                pq.question, pq.options, pq.correct,
                                pq.difficulty, pq.category, pq.explanation));
                    }
                    quizCurrentIndex = 0; quizScore = 0; quizAnsweredCount = 0;
                    startQuizSession();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showQuizLoading(false);
                    String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    if      (msg.contains("401")) lblQuizSetupError.setText("⚠ Invalid API key.");
                    else if (msg.contains("429")) lblQuizSetupError.setText("⚠ Rate limit. Retry later.");
                    else                          lblQuizSetupError.setText("⚠ AI error: " + msg);
                    showQuizSubView("setup");
                });
            }
        }).start();
    }

    private void showQuizLoading(boolean show) {
        if (btnGenerateAIQuiz != null) {
            btnGenerateAIQuiz.setDisable(show);
            btnGenerateAIQuiz.setText(show ? "⏳ Generating…" : "🤖  Generate AI Quiz with Claude");
        }
        if (show) showQuizSubView("loading");
    }

    private String detectLevel(double avg) {
        if (avg < 10) return "easy";
        if (avg < 14) return "medium";
        return "hard";
    }

    private double getAvgForSubject(int id) {
        return allEvals.stream().filter(e -> e.getMatiereId() == id)
                .mapToDouble(EvaluationMatiere::getScoreEval).average().orElse(10.0);
    }

    // ════════════════════════════════════════════════════════════
    //  QUIZ SESSION START
    // ════════════════════════════════════════════════════════════
    private void startQuizSession() {
        if (fraudStatusBar == null) {
            HBox bar = buildFraudBar();
            quizInProgressView.getChildren().add(0, bar);
        }
        antiFraud.startMonitoring();
        Platform.runLater(() -> {
            if (courseGrid.getScene() != null
                    && courseGrid.getScene().getWindow() instanceof Stage stage) {
                stage.setFullScreen(true);
            }
        });
        showQuizSubView("quiz");
        startQuizTimer();
        showQuizQuestion();
    }

    // ════════════════════════════════════════════════════════════
    //  QUIZ TIMER
    // ════════════════════════════════════════════════════════════
    private void startQuizTimer() {
        stopQuizTimer();
        quizSecondsElapsed = 0;
        quizTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            quizSecondsElapsed++;
            lblQuizTimer.setText(
                    String.format("%d:%02d", quizSecondsElapsed / 60, quizSecondsElapsed % 60));
        }));
        quizTimer.setCycleCount(Timeline.INDEFINITE);
        quizTimer.play();
    }

    private void stopQuizTimer() {
        if (quizTimer != null) { quizTimer.stop(); quizTimer = null; }
    }

    // ════════════════════════════════════════════════════════════
    //  SHOW QUESTION
    // ════════════════════════════════════════════════════════════
    private void showQuizQuestion() {
        if (quizCurrentIndex >= quizQuestions.size()) { finishQuiz(); return; }

        QuizQuestion q = quizQuestions.get(quizCurrentIndex);
        quizSelectedOpt = null;
        btnQuizNext.setDisable(true);

        int total = quizQuestions.size();
        lblQuizQuestionNum.setText("Question " + (quizCurrentIndex + 1));
        lblQuizDiffBadge.setText(q.difficulty != null ? q.difficulty : "Medium");
        lblQuizCategoryBadge.setText(q.category != null ? q.category : "General");
        lblQuizQuestionText.setText(q.question);
        lblQuizProgress.setText((quizCurrentIndex + 1) + " / " + total);
        lblQuizAnsweredCount.setText(quizAnsweredCount + " answered");
        quizProgressBar.setProgress((double) quizCurrentIndex / total);

        quizVboxOptions.getChildren().clear();
        String[] letters = {"A","B","C","D"};
        for (int i = 0; i < q.options.size(); i++) {
            String opt    = q.options.get(i);
            String letter = i < letters.length ? letters[i] : String.valueOf(i + 1);
            Button btn = new Button(letter + ".  " + opt);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setUserData(opt);
            btn.setStyle(styleOptionDefault());
            btn.setOnAction(e -> handleOptionSelected(btn, opt));
            quizVboxOptions.getChildren().add(btn);
        }
        antiFraud.onQuestionChanged(quizCurrentIndex);
    }

    private void handleOptionSelected(Button clicked, String opt) {
        quizSelectedOpt = opt;
        btnQuizNext.setDisable(false);
        quizVboxOptions.getChildren().forEach(n -> {
            if (n instanceof Button b) b.setStyle(styleOptionDefault());
        });
        clicked.setStyle(styleOptionSelected());
    }

    @FXML private void handleQuizNextQuestion() {
        QuizQuestion q = quizQuestions.get(quizCurrentIndex);
        q.userAnswer = quizSelectedOpt;
        if (quizSelectedOpt != null && quizSelectedOpt.equals(q.correctAnswer)) quizScore++;
        if (quizSelectedOpt != null) quizAnsweredCount++;
        quizCurrentIndex++;
        showQuizQuestion();
    }

    // ════════════════════════════════════════════════════════════
    //  FINISH QUIZ
    // ════════════════════════════════════════════════════════════
    private void finishQuiz() {
        stopQuizTimer();
        antiFraud.stopMonitoring();

        double rawScore      = quizScore;
        double adjustedScore = antiFraud.applyPenaltiesToScore(rawScore);
        int    displayScore  = (int) adjustedScore;
        int    total         = quizQuestions.size();
        int    percent       = total > 0 ? (int)((adjustedScore * 100.0) / total) : 0;

        lblQuizFinalScore.setText(displayScore + " / " + total);
        lblQuizFinalPercent.setText(percent + "%");
        lblQuizCorrectCount.setText(String.valueOf(quizScore));
        lblQuizWrongCount.setText(String.valueOf(total - quizScore));
        lblQuizTotalTime.setText(
                String.format("%d:%02d", quizSecondsElapsed / 60, quizSecondsElapsed % 60));

        if (percent >= 80) {
            lblQuizFinalLevel.setText("Excellent 🎉");
            lblQuizFinalLevel.setStyle("-fx-text-fill:#34D399;-fx-font-size:20px;-fx-font-weight:700;");
        } else if (percent >= 60) {
            lblQuizFinalLevel.setText("Good 👍");
            lblQuizFinalLevel.setStyle("-fx-text-fill:#60A5FA;-fx-font-size:20px;-fx-font-weight:700;");
        } else {
            lblQuizFinalLevel.setText("Need Improvement 📚");
            lblQuizFinalLevel.setStyle("-fx-text-fill:#FB7185;-fx-font-size:20px;-fx-font-weight:700;");
        }

        quizVboxDetailedResults.getChildren().clear();
        if (antiFraud.getFraudScore() > 0)
            quizVboxDetailedResults.getChildren().add(buildFraudSummaryCard());

        for (int i = 0; i < quizQuestions.size(); i++) {
            QuizQuestion q = quizQuestions.get(i);
            boolean ok    = q.correctAnswer.equals(q.userAnswer);
            String accentColor = ok ? "#34D399" : "#FB7185";
            String bgColor     = ok ? "#064E3B" : "#4C0519";
            String borderColor = ok ? "#34D399" : "#FB7185";

            VBox card = new VBox(10);
            card.setStyle("-fx-background-color:" + bgColor + ";-fx-border-color:" + borderColor +
                    ";-fx-border-width:1.5;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:14 16;");

            HBox qHeader = new HBox(10); qHeader.setAlignment(Pos.TOP_LEFT);
            Label numBadge = new Label(String.valueOf(i + 1));
            numBadge.setStyle("-fx-background-color:" + accentColor + ";-fx-text-fill:#0F172A;" +
                    "-fx-font-size:11px;-fx-font-weight:800;-fx-padding:3 8;-fx-background-radius:20;");
            Label qText = new Label(q.question); qText.setWrapText(true);
            qText.setStyle("-fx-text-fill:#F8FAFC;-fx-font-size:13px;-fx-font-weight:600;-fx-line-spacing:2;");
            HBox.setHgrow(qText, Priority.ALWAYS);
            Label resultIcon = new Label(ok ? "✅" : "❌");
            resultIcon.setStyle("-fx-font-size:18px;");
            qHeader.getChildren().addAll(numBadge, qText, resultIcon);

            HBox userRow = new HBox(8); userRow.setAlignment(Pos.CENTER_LEFT);
            Label userLbl = new Label("Your answer:");
            userLbl.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:12px;");
            Label userVal = new Label(q.userAnswer != null ? q.userAnswer : "Not answered");
            userVal.setWrapText(true);
            userVal.setStyle("-fx-text-fill:" + accentColor + ";-fx-font-size:12px;-fx-font-weight:600;");
            HBox.setHgrow(userVal, Priority.ALWAYS);
            userRow.getChildren().addAll(userLbl, userVal);
            card.getChildren().addAll(qHeader, userRow);

            if (!ok) {
                HBox corrRow = new HBox(8); corrRow.setAlignment(Pos.CENTER_LEFT);
                Label corrLbl = new Label("✓  Correct answer:");
                corrLbl.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:12px;");
                Label corrVal = new Label(q.correctAnswer); corrVal.setWrapText(true);
                corrVal.setStyle("-fx-text-fill:#34D399;-fx-font-size:12px;-fx-font-weight:700;");
                HBox.setHgrow(corrVal, Priority.ALWAYS);
                corrRow.getChildren().addAll(corrLbl, corrVal);
                card.getChildren().add(corrRow);
            }

            if (q.explanation != null && !q.explanation.isBlank()) {
                Region sep = new Region(); sep.setPrefHeight(1);
                sep.setStyle("-fx-background-color:rgba(255,255,255,0.08);");
                VBox explBox = new VBox(6);
                explBox.setStyle("-fx-background-color:rgba(124,58,237,0.12);" +
                        "-fx-border-color:rgba(167,139,250,0.4);-fx-border-width:1;" +
                        "-fx-border-radius:8;-fx-background-radius:8;-fx-padding:10 12;");
                Label explTitle = new Label("💡 AI Explanation");
                explTitle.setStyle("-fx-text-fill:#A78BFA;-fx-font-size:11px;-fx-font-weight:700;");
                Label explText = new Label(q.explanation); explText.setWrapText(true);
                explText.setStyle("-fx-text-fill:#CBD5E1;-fx-font-size:12px;-fx-line-spacing:3;");
                explBox.getChildren().addAll(explTitle, explText);
                card.getChildren().addAll(sep, explBox);
            }
            quizVboxDetailedResults.getChildren().add(card);
        }

        showQuizSubView("results");
        Platform.runLater(() -> {
            if (courseGrid.getScene() != null
                    && courseGrid.getScene().getWindow() instanceof Stage stage) {
                stage.setFullScreen(false);
            }
        });
    }

    private VBox buildFraudSummaryCard() {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color:#1A0A0A;-fx-border-color:#EF4444;-fx-border-width:2;" +
                "-fx-border-radius:12;-fx-background-radius:12;-fx-padding:16;");
        Label title = new Label("🔒 Exam Security Report");
        title.setStyle("-fx-text-fill:#F87171;-fx-font-size:14px;-fx-font-weight:700;");
        Label summary = new Label(antiFraud.getSummary());
        summary.setStyle("-fx-text-fill:#CBD5E1;-fx-font-size:12px;");
        summary.setWrapText(true);
        if (antiFraud.getPenaltyCount() > 0) {
            Label penalty = new Label("⚠ " + antiFraud.getPenaltyCount() + " penalty point(s) deducted.");
            penalty.setStyle("-fx-text-fill:#FCA5A5;-fx-font-size:12px;-fx-font-weight:600;");
            card.getChildren().addAll(title, summary, penalty);
        } else {
            card.getChildren().addAll(title, summary);
        }
        FlowPane eventFlow = new FlowPane(8, 6);
        for (FraudEvent e : antiFraud.getEventLog()) {
            Label badge = new Label(e.getFormattedTimestamp() + " — " + e.getType().label);
            badge.setStyle("-fx-background-color:#2D0A0A;-fx-text-fill:" + e.getSeverityColor() +
                    ";-fx-font-size:11px;-fx-padding:4 10;-fx-background-radius:20;");
            eventFlow.getChildren().add(badge);
        }
        if (!antiFraud.getEventLog().isEmpty()) card.getChildren().add(eventFlow);
        return card;
    }

    @FXML private void handleQuizRetake() {
        stopQuizTimer();
        quizQuestions.forEach(q -> q.userAnswer = null);
        quizCurrentIndex = 0; quizScore = 0; quizAnsweredCount = 0;
        startQuizSession();
    }

    @FXML private void handleQuizBackToSetup() {
        stopQuizTimer();
        antiFraud.stopMonitoring();
        showQuizSubView("setup");
    }

    // ════════════════════════════════════════════════════════════
    //  OPTION STYLES
    // ════════════════════════════════════════════════════════════
    private String styleOptionDefault() {
        return "-fx-background-color:#0F172A;-fx-border-color:#334155;-fx-border-width:1.5;" +
                "-fx-border-radius:10;-fx-background-radius:10;-fx-padding:12 16;" +
                "-fx-font-size:14px;-fx-text-fill:#CBD5E1;-fx-alignment:CENTER_LEFT;-fx-cursor:hand;";
    }
    private String styleOptionSelected() {
        return "-fx-background-color:#1E1B4B;-fx-border-color:#7C3AED;-fx-border-width:2;" +
                "-fx-border-radius:10;-fx-background-radius:10;-fx-padding:12 16;" +
                "-fx-font-size:14px;-fx-text-fill:white;-fx-alignment:CENTER_LEFT;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.3),8,0,0,2);";
    }

    // ════════════════════════════════════════════════════════════
    //  PRIORITY
    // ════════════════════════════════════════════════════════════
    @FXML private void selectPrioHaute()   { setPriorite("High"); }
    @FXML private void selectPrioMoyenne() { setPriorite("Medium"); }
    @FXML private void selectPrioBasse()   { setPriorite("Low"); }

    @FXML private void handleSectionChange() {
        String sec = cmbSection.getValue();
        cmbMatiere.setValue(null); errSection.setText("");
        if (sec == null || sec.isBlank()) {
            cmbMatiere.setItems(FXCollections.observableArrayList());
            cmbMatiere.setPromptText("Select a section first…"); return;
        }
        try {
            List<Matiere> filtered = matService.findAll().stream()
                    .filter(m -> sec.equals(m.getSectionMatiere()))
                    .collect(Collectors.toList());
            cmbMatiere.setItems(FXCollections.observableArrayList(filtered));
            cmbMatiere.setPromptText(filtered.isEmpty()
                    ? "No subjects for this section" : "Choose a subject…");
        } catch (Exception e) {
            e.printStackTrace(); errSection.setText("⚠ Error loading subjects.");
        }
    }

    private void setPriorite(String val) {
        selectedPriorite = val; cmbPriorite.setValue(val); errPriorite.setText("");
        String bH = "-fx-background-color:#1E293B;-fx-text-fill:#FB7185;-fx-border-color:#FB7185;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 20;-fx-font-size:13px;-fx-cursor:hand;";
        String bM = "-fx-background-color:#1E293B;-fx-text-fill:#FBBF24;-fx-border-color:#FBBF24;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 20;-fx-font-size:13px;-fx-cursor:hand;";
        String bL = "-fx-background-color:#1E293B;-fx-text-fill:#34D399;-fx-border-color:#34D399;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 20;-fx-font-size:13px;-fx-cursor:hand;";
        btnPrioHaute.setStyle(bH); btnPrioMoyenne.setStyle(bM); btnPrioBasse.setStyle(bL);
        switch (val) {
            case "High"   -> btnPrioHaute  .setStyle("-fx-background-color:#F43F5E;-fx-text-fill:white;-fx-border-color:#F43F5E;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 20;-fx-font-size:13px;-fx-cursor:hand;-fx-font-weight:700;");
            case "Medium" -> btnPrioMoyenne.setStyle("-fx-background-color:#F59E0B;-fx-text-fill:white;-fx-border-color:#F59E0B;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 20;-fx-font-size:13px;-fx-cursor:hand;-fx-font-weight:700;");
            case "Low"    -> btnPrioBasse  .setStyle("-fx-background-color:#10B981;-fx-text-fill:white;-fx-border-color:#10B981;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 20;-fx-font-size:13px;-fx-cursor:hand;-fx-font-weight:700;");
        }
    }

    // ════════════════════════════════════════════════════════════
    //  FILTERS & SORT
    // ════════════════════════════════════════════════════════════
    @FXML private void filterAll()     { renderCards(allEvals); }
    @FXML private void filterBonne()   { renderCards(allEvals.filtered(e -> e.getScoreEval() >= 14)); }
    @FXML private void filterMoyenne() { renderCards(allEvals.filtered(e -> e.getScoreEval() >= 10 && e.getScoreEval() < 14)); }
    @FXML private void filterFaible()  { renderCards(allEvals.filtered(e -> e.getScoreEval() < 10)); }
    @FXML private void filterHaute()   { renderCards(allEvals.filtered(e -> "High".equalsIgnoreCase(e.getPrioriteE()))); }

    private void filterAndRender(String term) {
        if (term == null || term.isBlank()) { renderCards(allEvals); return; }
        String low = term.toLowerCase();
        renderCards(allEvals.filtered(e ->
                String.valueOf(e.getScoreEval()).contains(low) ||
                        (e.getPrioriteE() != null && e.getPrioriteE().toLowerCase().contains(low)) ||
                        getNomMatiere(e.getMatiereId()).toLowerCase().contains(low)));
    }

    private void applySort() {
        String sel = sortCombo.getValue(); if (sel == null) return;
        List<EvaluationMatiere> sorted = new ArrayList<>(allEvals);
        switch (sel) {
            case "Date ↓"   -> sorted.sort(Comparator.comparing(EvaluationMatiere::getDateEvaluation, Comparator.nullsLast(Comparator.reverseOrder())));
            case "Date ↑"   -> sorted.sort(Comparator.comparing(EvaluationMatiere::getDateEvaluation, Comparator.nullsLast(Comparator.naturalOrder())));
            case "Score ↓"  -> sorted.sort(Comparator.comparingDouble(EvaluationMatiere::getScoreEval).reversed());
            case "Score ↑"  -> sorted.sort(Comparator.comparingDouble(EvaluationMatiere::getScoreEval));
            case "Durée ↓"  -> sorted.sort(Comparator.comparingInt(EvaluationMatiere::getDureeEvaluation).reversed());
            case "Priority" -> sorted.sort(Comparator.comparing(e -> prioOrder(e.getPrioriteE())));
        }
        renderCards(sorted);
    }

    private int prioOrder(String p) {
        if (p == null) return 99;
        return switch (p) {
            case "High","Haute"     -> 0;
            case "Medium","Moyenne" -> 1;
            case "Low","Basse"      -> 2;
            default -> 99;
        };
    }

    // ════════════════════════════════════════════════════════════
    //  STATISTICS
    // ════════════════════════════════════════════════════════════
    private void populateStats() {
        statsView.getChildren().clear();
        statsView.setSpacing(16);
        statsView.setPadding(new Insets(0,0,24,0));
        statsView.getChildren().add(buildKpiRow());

        HBox row2 = new HBox(16);
        VBox subjectCard  = buildSubjectBarsCard();
        VBox priorityCard = buildPriorityRingsCard();
        HBox.setHgrow(subjectCard,  Priority.ALWAYS);
        HBox.setHgrow(priorityCard, Priority.ALWAYS);
        row2.getChildren().addAll(subjectCard, priorityCard);
        statsView.getChildren().add(row2);

        HBox row3 = new HBox(16);
        VBox lineCard = buildLineChartCard();
        VBox barCard  = buildBarChartCard();
        HBox.setHgrow(lineCard, Priority.ALWAYS);
        HBox.setHgrow(barCard,  Priority.ALWAYS);
        row3.getChildren().addAll(lineCard, barCard);
        statsView.getChildren().add(row3);

        statsView.getChildren().add(buildInsightsCard());
    }

    private VBox buildLineChartCard() {
        VBox card = cardBox("Score trend");
        CategoryAxis xA = new CategoryAxis(); NumberAxis yA = new NumberAxis(0,20,2);
        xA.setTickLabelFill(Color.web("#64748B")); yA.setTickLabelFill(Color.web("#64748B"));
        LineChart<String,Number> chart = new LineChart<>(xA, yA);
        chart.setLegendVisible(false); chart.setAnimated(true); chart.setPrefHeight(240);
        chart.setStyle("-fx-background-color:transparent;-fx-plot-background-color:#0F172A;");
        XYChart.Series<String,Number> series = new XYChart.Series<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        allEvals.stream().filter(e -> e.getDateEvaluation() != null)
                .sorted(Comparator.comparing(EvaluationMatiere::getDateEvaluation))
                .forEach(e -> series.getData().add(
                        new XYChart.Data<>(e.getDateEvaluation().format(fmt), e.getScoreEval())));
        chart.getData().add(series);
        card.getChildren().add(chart); return card;
    }

    private VBox buildBarChartCard() {
        VBox card = cardBox("Avg by subject");
        CategoryAxis xA = new CategoryAxis(); NumberAxis yA = new NumberAxis(0,20,2);
        xA.setTickLabelFill(Color.web("#64748B")); yA.setTickLabelFill(Color.web("#64748B"));
        BarChart<String,Number> chart = new BarChart<>(xA, yA);
        chart.setLegendVisible(false); chart.setAnimated(true); chart.setPrefHeight(240);
        chart.setStyle("-fx-background-color:transparent;-fx-plot-background-color:#0F172A;");
        XYChart.Series<String,Number> series = new XYChart.Series<>();
        Map<Integer,Double> avg = allEvals.stream().collect(
                Collectors.groupingBy(EvaluationMatiere::getMatiereId,
                        Collectors.averagingDouble(EvaluationMatiere::getScoreEval)));
        avg.entrySet().stream().sorted(Map.Entry.<Integer,Double>comparingByValue().reversed()).forEach(en -> {
            String name = getNomMatiere(en.getKey());
            if (name.length() > 10) name = name.substring(0, 9) + "…";
            series.getData().add(new XYChart.Data<>(name, en.getValue()));
        });
        chart.getData().add(series); card.getChildren().add(chart); return card;
    }

    private HBox buildKpiRow() {
        HBox row = new HBox(12);
        double avg = allEvals.stream().mapToDouble(EvaluationMatiere::getScoreEval).average().orElse(0);
        long totalMin = allEvals.stream().mapToLong(EvaluationMatiere::getDureeEvaluation).sum();
        long above14  = allEvals.stream().filter(e -> e.getScoreEval() >= 14).count();
        Map<Integer,Double> avgBySub = allEvals.stream().collect(
                Collectors.groupingBy(EvaluationMatiere::getMatiereId,
                        Collectors.averagingDouble(EvaluationMatiere::getScoreEval)));
        String best  = avgBySub.entrySet().stream().max(Map.Entry.comparingByValue()).map(e -> getNomMatiere(e.getKey())).orElse("—");
        String worst = avgBySub.entrySet().stream().min(Map.Entry.comparingByValue()).map(e -> getNomMatiere(e.getKey())).orElse("—");
        double bestA  = avgBySub.values().stream().mapToDouble(d -> d).max().orElse(0);
        double worstA = avgBySub.values().stream().mapToDouble(d -> d).min().orElse(0);

        // Add total city points KPI
        int cityPts = GamePointsService.totalPoints(new ArrayList<>(allEvals));
        Building.Type bt = Building.getBuildingForPoints(cityPts);

        row.getChildren().addAll(
                buildKpi("Total",        String.valueOf(allEvals.size()),                   "assessments",          true,  "#A78BFA"),
                buildKpi("Average",      String.format("%.1f/20", avg),                    avg >= 10 ? "Passing" : "Below passing", avg >= 10, "#34D399"),
                buildKpi("Best subject", best,                                              String.format("avg %.1f", bestA), true, "#38BDF8"),
                buildKpi("Weakest",      worst,                                             String.format("avg %.1f — focus here", worstA), false, "#FB7185"),
                buildKpi("City 🏙️",      bt.emoji + " " + bt.name,                        cityPts + " pts", true, "#F59E0B")
        );
        row.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        return row;
    }

    private VBox buildKpi(String label, String value, String delta, boolean pos, String color) {
        VBox card = new VBox(6); card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color:#0F172A;-fx-border-color:#1E293B;-fx-border-width:1;-fx-border-radius:14;-fx-background-radius:14;");
        Region bar = new Region(); bar.setPrefHeight(3); bar.setMaxWidth(40);
        bar.setStyle("-fx-background-color:"+color+";-fx-background-radius:2;");
        Label lbl = new Label(label); lbl.setStyle("-fx-text-fill:#64748B;-fx-font-size:11px;");
        Label val = new Label(value); val.setStyle("-fx-text-fill:#F8FAFC;-fx-font-size:20px;-fx-font-weight:700;"); val.setWrapText(true);
        Label dlt = new Label(delta); dlt.setStyle("-fx-text-fill:"+(pos?"#34D399":"#FB7185")+";-fx-font-size:11px;-fx-padding:3 8;-fx-background-radius:20;-fx-background-color:"+(pos?"#0D2C1F":"#2D0A0A")+";");
        card.getChildren().addAll(bar, lbl, val, dlt); return card;
    }

    private VBox buildSubjectBarsCard() {
        VBox card = cardBox("Score by subject (avg / 20)");
        VBox body = new VBox(10);
        Map<Integer,Double> avg = allEvals.stream().collect(
                Collectors.groupingBy(EvaluationMatiere::getMatiereId,
                        Collectors.averagingDouble(EvaluationMatiere::getScoreEval)));
        avg.entrySet().stream().sorted(Map.Entry.<Integer,Double>comparingByValue().reversed()).forEach(en -> {
            double a = en.getValue(); double pct = (a / 20.0) * 100;
            String col = a >= 14 ? "#10B981" : a >= 10 ? "#F59E0B" : "#F43F5E";
            String nm  = getNomMatiere(en.getKey()); if (nm.length() > 12) nm = nm.substring(0,11) + "…";
            HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
            Label nl = new Label(nm); nl.setMinWidth(90); nl.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:12px;");
            StackPane track = new StackPane(); track.setMaxHeight(8); track.setPrefHeight(8);
            HBox.setHgrow(track, Priority.ALWAYS);
            Region bg   = new Region(); bg.setPrefHeight(8); bg.setMaxWidth(Double.MAX_VALUE); bg.setStyle("-fx-background-color:#1E293B;-fx-background-radius:4;");
            Region fill = new Region(); fill.setPrefHeight(8); fill.setStyle("-fx-background-color:"+col+";-fx-background-radius:4;");
            track.setMaxWidth(Double.MAX_VALUE);
            bg.prefWidthProperty().bind(track.widthProperty());
            fill.prefWidthProperty().bind(track.widthProperty().multiply(pct / 100.0));
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            track.getChildren().addAll(bg, fill);
            Label vl = new Label(String.format("%.1f", a)); vl.setMinWidth(32);
            vl.setStyle("-fx-text-fill:"+col+";-fx-font-size:12px;-fx-font-weight:700;");
            row.getChildren().addAll(nl, track, vl); body.getChildren().add(row);
        });
        if (body.getChildren().isEmpty()) body.getChildren().add(new Label("No data"));
        card.getChildren().add(body); return card;
    }

    private VBox buildPriorityRingsCard() {
        VBox card = cardBox("Score by priority");
        Map<String,Double> avg = allEvals.stream().filter(e -> e.getPrioriteE() != null)
                .collect(Collectors.groupingBy(EvaluationMatiere::getPrioriteE,
                        Collectors.averagingDouble(EvaluationMatiere::getScoreEval)));
        HBox row = new HBox(24); row.setAlignment(Pos.CENTER); row.setPadding(new Insets(12,0,0,0));
        row.getChildren().addAll(
                buildRing("HIGH",   avg.getOrDefault("High",   avg.getOrDefault("Haute",   0.0)), "#E24B4A"),
                buildRing("MEDIUM", avg.getOrDefault("Medium", avg.getOrDefault("Moyenne", 0.0)), "#EF9F27"),
                buildRing("LOW",    avg.getOrDefault("Low",    avg.getOrDefault("Basse",   0.0)), "#1D9E75")
        );
        card.getChildren().add(row); return card;
    }

    private VBox buildRing(String label, double avg, String color) {
        VBox box = new VBox(8); box.setAlignment(Pos.CENTER);
        StackPane stack = new StackPane(); stack.setPrefSize(90, 90);
        ProgressIndicator ring = new ProgressIndicator(avg / 20.0); ring.setPrefSize(80, 80);
        ring.setStyle("-fx-accent:" + color + ";");
        Label val = new Label(String.format("%.1f", avg));
        val.setStyle("-fx-text-fill:#F8FAFC;-fx-font-size:14px;-fx-font-weight:700;");
        stack.getChildren().addAll(ring, val);
        Label lbl = new Label(label); lbl.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:11px;-fx-font-weight:600;");
        box.getChildren().addAll(stack, lbl); return box;
    }

    private VBox buildInsightsCard() {
        VBox card = cardBox("Smart insights");
        FlowPane flow = new FlowPane(10, 10);
        double avg = allEvals.stream().mapToDouble(EvaluationMatiere::getScoreEval).average().orElse(0);
        long above14  = allEvals.stream().filter(e -> e.getScoreEval() >= 14).count();
        long below10  = allEvals.stream().filter(e -> e.getScoreEval() < 10).count();
        long totalMin = allEvals.stream().mapToLong(EvaluationMatiere::getDureeEvaluation).sum();

        int cityPts = GamePointsService.totalPoints(new ArrayList<>(allEvals));
        Building.Type bt = Building.getBuildingForPoints(cityPts);

        if      (avg >= 16) addBadge(flow,"⭐  Excellent avg ≥ 16","#10B981","#0D2C1F");
        else if (avg >= 14) addBadge(flow,"✅  Good avg ≥ 14","#34D399","#0D2C1F");
        else if (avg >= 10) addBadge(flow,"🟡  Average 10–14","#F59E0B","#2C1E00");
        else                addBadge(flow,"🔴  Below avg — keep going!","#F43F5E","#2D0A0A");
        if (above14  > 0)   addBadge(flow, above14+" assessment(s) ≥ 14","#A78BFA","#1E1B4B");
        if (below10  > 0)   addBadge(flow, below10+" to revise (< 10)","#FB7185","#2D0A0A");
        if (totalMin >= 60) addBadge(flow, String.format("⏱  %.1fh studied", totalMin/60.0),"#38BDF8","#0C2233");

        // City badge
        addBadge(flow, bt.emoji + "  " + bt.name + " — " + cityPts + " pts", "#F59E0B", "#1C1300");

        card.getChildren().add(flow); return card;
    }

    // ════════════════════════════════════════════════════════════
    //  EXPORT PDF
    // ════════════════════════════════════════════════════════════
    @FXML private void handleExportPdf() {
        if (allEvals.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Add at least one assessment first.", ButtonType.OK).showAndWait(); return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Save PDF Report");
        fc.setInitialFileName("StudyFlow_"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files","*.pdf"));
        Stage stage = (Stage) courseGrid.getScene().getWindow();
        java.io.File file = fc.showSaveDialog(stage);
        if (file == null) return;
        try {
            PdfExporter.export(new ArrayList<>(allEvals), this::getNomMatiere, file);
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Export successful"); ok.setHeaderText("PDF saved!");
            ok.setContentText(file.getAbsolutePath()); ok.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Export failed: " + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    // ════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════
    private void clearForm() {
        editTarget = null; selectedPriorite = null;
        cmbMatiere.setValue(null); fldScore.clear(); fldNoteMax.clear(); fldDuree.clear();
        dpDate.setValue(null); cmbPriorite.setValue(null);
        String bH = "-fx-background-color:#1E293B;-fx-text-fill:#FB7185;-fx-border-color:#FB7185;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 20;-fx-font-size:13px;-fx-cursor:hand;";
        String bM = "-fx-background-color:#1E293B;-fx-text-fill:#FBBF24;-fx-border-color:#FBBF24;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 20;-fx-font-size:13px;-fx-cursor:hand;";
        String bL = "-fx-background-color:#1E293B;-fx-text-fill:#34D399;-fx-border-color:#34D399;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 20;-fx-font-size:13px;-fx-cursor:hand;";
        btnPrioHaute.setStyle(bH); btnPrioMoyenne.setStyle(bM); btnPrioBasse.setStyle(bL);
        cmbSection.setValue(null); errSection.setText("");
        cmbMatiere.setItems(FXCollections.observableArrayList());
        cmbMatiere.setPromptText("Select a section first…");
        clearErrors();
    }

    private void clearErrors() {
        formError.setVisible(false);
        errMatiere.setText(""); errScore.setText(""); errNoteMax.setText("");
        errDuree.setText(""); errPriorite.setText(""); errDate.setText(""); errSection.setText("");
    }

    private void showGlobalError(String msg) {
        formError.setText("⚠ " + msg); formError.setVisible(true);
    }

    private String getNomMatiere(int id) {
        try { Matiere m = matService.findById(id); return m != null ? m.getNomMatiere() : "Subject #" + id; }
        catch (Exception e) { return "Subject #" + id; }
    }

    private VBox cardBox(String title) {
        VBox card = new VBox(12); card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color:#0F172A;-fx-border-color:#1E293B;-fx-border-width:1;-fx-border-radius:16;-fx-background-radius:16;");
        Label t = new Label(title); t.setStyle("-fx-text-fill:#F8FAFC;-fx-font-size:14px;-fx-font-weight:700;");
        card.getChildren().add(t); return card;
    }

    private void addBadge(FlowPane fp, String text, String fg, String bg) {
        Label b = new Label(text);
        b.setStyle("-fx-text-fill:"+fg+";-fx-background-color:"+bg+";-fx-font-size:12px;-fx-padding:6 12;-fx-background-radius:20;");
        fp.getChildren().add(b);
    }

    private HBox buildChip(String icon, String text, String hex) {
        HBox chip = new HBox(6); chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle("-fx-background-color:rgba(255,255,255,0.05);-fx-padding:4 10;-fx-background-radius:20;");
        FontIcon ic = new FontIcon(icon); ic.setIconSize(12); ic.setIconColor(Color.web(hex));
        Label lbl = new Label(text); lbl.setStyle("-fx-text-fill:"+hex+";-fx-font-size:12px;-fx-font-weight:500;");
        chip.getChildren().addAll(ic, lbl); return chip;
    }

    private String getPrioColor(String p) {
        if (p == null) return "#94A3B8";
        return switch (p) {
            case "High","Haute"     -> "#FB7185";
            case "Medium","Moyenne" -> "#FBBF24";
            case "Low","Basse"      -> "#34D399";
            default -> "#94A3B8";
        };
    }

    private String getGradient(String c) {
        return switch (c) {
            case "primary" -> "linear-gradient(to right,#6D28D9,#8B5CF6)";
            case "success" -> "linear-gradient(to right,#047857,#10B981)";
            case "warning" -> "linear-gradient(to right,#D97706,#F59E0B)";
            case "accent"  -> "linear-gradient(to right,#EA580C,#F97316)";
            case "danger"  -> "linear-gradient(to right,#E11D48,#F43F5E)";
            default        -> "linear-gradient(to right,#475569,#64748B)";
        };
    }

    private String getIconBg(String c) {
        return switch (c) {
            case "primary" -> "rgba(109,40,217,0.25)"; case "success" -> "rgba(4,120,87,0.25)";
            case "warning" -> "rgba(217,119,6,0.25)";  case "accent"  -> "rgba(234,88,12,0.25)";
            case "danger"  -> "rgba(225,29,72,0.25)";  default        -> "rgba(71,85,105,0.25)";
        };
    }

    private String getHex(String c) {
        return switch (c) {
            case "primary" -> "#8B5CF6"; case "success" -> "#10B981";
            case "warning" -> "#F59E0B"; case "accent"  -> "#F97316";
            case "danger"  -> "#F43F5E"; default        -> "#64748B";
        };
    }

    private VBox buildCard(EvaluationMatiere ev, String color) {
        VBox card = new VBox(0);
        card.getStyleClass().add("card");
        card.setPrefWidth(380); card.setMaxWidth(380);
        card.setStyle("-fx-background-color:#0F172A;-fx-border-color:#1E293B;-fx-border-width:1.5;" +
                "-fx-border-radius:16;-fx-background-radius:16;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),12,0,0,4);");

        Region bar = new Region(); bar.setPrefHeight(4);
        bar.setStyle("-fx-background-color:" + getGradient(color) + ";-fx-background-radius:16 16 0 0;");

        VBox content = new VBox(14); content.setPadding(new Insets(18,20,18,20));

        HBox header = new HBox(12); header.setAlignment(Pos.CENTER_LEFT);
        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(46,46); iconBox.setMinSize(46,46); iconBox.setMaxSize(46,46);
        iconBox.setStyle("-fx-background-color:" + getIconBg(color) + ";-fx-background-radius:12;");
        FontIcon icon = new FontIcon("fth-clipboard");
        icon.setIconSize(20); icon.setIconColor(Color.web(getHex(color)));
        iconBox.getChildren().add(icon);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Label badge = new Label(String.format("%.1f / %.0f", ev.getScoreEval(), ev.getNoteMaximaleEval()));
        badge.setStyle("-fx-background-color:" + getHex(color) + ";-fx-text-fill:white;" +
                "-fx-font-size:13px;-fx-font-weight:700;-fx-padding:5 12;-fx-background-radius:20;");
        header.getChildren().addAll(iconBox, sp, badge);

        Label nom = new Label(getNomMatiere(ev.getMatiereId()));
        nom.setStyle("-fx-text-fill:#F8FAFC;-fx-font-size:17px;-fx-font-weight:700;");
        nom.setWrapText(true);

        HBox dateRow = new HBox(6); dateRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon cal = new FontIcon("fth-calendar"); cal.setIconSize(12); cal.setIconColor(Color.web("#94A3B8"));
        Label dateL = new Label(ev.getDateEvaluation() != null ? ev.getDateEvaluation().format(FMT) : "—");
        dateL.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:12px;");
        dateRow.getChildren().addAll(cal, dateL);

        HBox chips = new HBox(10); chips.setAlignment(Pos.CENTER_LEFT);

        // City points chip
        int evPts = GamePointsService.computePoints(ev);
        Building.Type evBuilding = Building.getBuildingForPoints(evPts);
        Label cityChip = new Label(evBuilding.emoji + " +" + evPts + " pts");
        cityChip.setStyle("-fx-text-fill:#A78BFA;-fx-font-size:11px;-fx-font-weight:600;" +
                "-fx-background-color:rgba(124,58,237,0.15);-fx-padding:3 8;-fx-background-radius:12;");

        chips.getChildren().addAll(
                buildChip("fth-clock", ev.getDureeEvaluation() + " min", "#94A3B8"),
                buildChip("fth-alert-circle", ev.getPrioriteE() != null ? ev.getPrioriteE() : "—",
                        getPrioColor(ev.getPrioriteE())),
                cityChip
        );

        double progress = ev.getNoteMaximaleEval() > 0
                ? ev.getScoreEval() / ev.getNoteMaximaleEval() : 0;
        VBox progSection = new VBox(6);
        HBox ph = new HBox(); ph.setAlignment(Pos.CENTER_LEFT);
        Label pl = new Label("Score"); pl.setStyle("-fx-text-fill:#64748B;-fx-font-size:12px;");
        Region ps = new Region(); HBox.setHgrow(ps, Priority.ALWAYS);
        Label pv = new Label((int)(progress*100) + "%");
        pv.setStyle("-fx-text-fill:" + getHex(color) + ";-fx-font-size:13px;-fx-font-weight:700;");
        ph.getChildren().addAll(pl, ps, pv);
        ProgressBar pb = new ProgressBar(progress); pb.setMaxWidth(Double.MAX_VALUE);
        pb.setStyle("-fx-accent:" + getHex(color) + ";-fx-background-color:#1E293B;" +
                "-fx-background-radius:4;-fx-border-radius:4;");
        progSection.getChildren().addAll(ph, pb);

        Region div = new Region(); div.setPrefHeight(1);
        div.setStyle("-fx-background-color:#1E293B;");

        HBox actions = new HBox(8); actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnEdit = new Button("✏  Edit");
        btnEdit.setStyle("-fx-background-color:transparent;-fx-text-fill:" + getHex(color) + ";" +
                "-fx-border-color:" + getHex(color) + ";-fx-border-width:1.5;" +
                "-fx-border-radius:8;-fx-background-radius:8;" +
                "-fx-font-size:12px;-fx-font-weight:600;-fx-padding:6 16;-fx-cursor:hand;");
        btnEdit.setOnAction(e -> startEdit(ev));

        Button btnDel = new Button("🗑  Delete");
        btnDel.setStyle("-fx-background-color:transparent;-fx-text-fill:#FB7185;" +
                "-fx-border-color:#FB7185;-fx-border-width:1.5;" +
                "-fx-border-radius:8;-fx-background-radius:8;" +
                "-fx-font-size:12px;-fx-font-weight:600;-fx-padding:6 16;-fx-cursor:hand;");
        btnDel.setOnAction(e -> deleteItem(ev));
        actions.getChildren().addAll(btnEdit, btnDel);

        content.getChildren().addAll(header, nom, dateRow, chips, progSection, div, actions);
        card.getChildren().addAll(bar, content);
        return card;
    }
}
