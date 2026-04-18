package com.studyflow.controllers;

import com.studyflow.models.CopingSession;
import com.studyflow.models.User;
import com.studyflow.models.WellBeing;
import com.studyflow.models.QuestionStress;
import com.studyflow.models.QuizStress;
import com.studyflow.models.RecommendationStress;
import com.studyflow.models.WellbeingJournalEntry;
import com.studyflow.services.ServiceCopingSession;
import com.studyflow.services.ServiceQuestionStress;
import com.studyflow.services.ServiceQuizStress;
import com.studyflow.services.ServiceRecommendationStress;
import com.studyflow.services.ServiceWellBeing;
import com.studyflow.services.ServiceWellbeingJournalEntry;
import com.studyflow.services.WellbeingAiService;
import com.studyflow.utils.UserSession;
import com.studyflow.utils.EmojiUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.HashMap;
import java.util.Locale;
import java.util.stream.Collectors;

public class WellbeingController implements Initializable {

    @FXML private Label moodLabel;
    @FXML private Label sleepLabel;
    @FXML private Label stressLabel;
    @FXML private Label energyLabel;
    @FXML private Label totalCheckinsLabel;
    @FXML private Label avgStressLabel;
    @FXML private Label avgEnergyLabel;
    @FXML private Label avgSleepLabel;
    @FXML private Label historyTotalLabel;
    @FXML private Label historyAvgStressLabel;
    @FXML private Label historyAvgEnergyLabel;
    @FXML private Label historyAvgSleepLabel;
    @FXML private ComboBox<String> overviewSortCombo;
    @FXML private ComboBox<String> historyMoodFilter;
    @FXML private ComboBox<String> historySortCombo;
    @FXML private TextField historySearchField;
    @FXML private BarChart<String, Number> stressChart;
    @FXML private VBox recentCheckinsBox;
    @FXML private FlowPane copingToolsPane;
    @FXML private FlowPane moodDistributionPane;
    @FXML private VBox historyListBox;
    @FXML private VBox moodWeekBox;
    @FXML private VBox habitsBox;
    @FXML private VBox mindfulnessBox;
    @FXML private VBox sleepLogBox;
    @FXML private Slider stressSlider;
    @FXML private Slider energySlider;
    @FXML private HBox moodButtonsBox;
    @FXML private HBox sleepButtonsBox;
    @FXML private TextArea notesArea;
    @FXML private Label stressValueLabel;
    @FXML private Label energyValueLabel;
    @FXML private Label formTitleLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelEditButton;
    @FXML private HBox statsSection;
    @FXML private VBox overviewSection;
    @FXML private VBox formSection;
    @FXML private VBox historySection;
    @FXML private VBox toolsSection;
    @FXML private VBox quizSection;
    @FXML private VBox quizResultsSection;
    @FXML private Label quizCurrentQuestionLabel;
    @FXML private Label quizTotalQuestionsLabel;
    @FXML private Label quizProgressPercentLabel;
    @FXML private ProgressBar quizProgressBar;
    @FXML private Label quizQuestionTitleLabel;
    @FXML private Label quizQuestionTextLabel;
    @FXML private VBox quizOptionsBox;
    @FXML private Button quizPrevButton;
    @FXML private Button quizNextButton;
    @FXML private Button quizSubmitButton;
    @FXML private Label quizResultEmojiLabel;
    @FXML private Label quizResultTitleLabel;
    @FXML private Label quizResultSubtitleLabel;
    @FXML private Label quizResultScoreLabel;
    @FXML private Label quizResultLevelLabel;
    @FXML private Label quizResultInterpretationLabel;
    @FXML private VBox quizRecommendationsBox;
    @FXML private Label globalMessageLabel;
    @FXML private Label notesErrorLabel;
    private final ServiceWellBeing serviceWellBeing = new ServiceWellBeing();
    private final ServiceCopingSession serviceCopingSession = new ServiceCopingSession();
    private final ServiceWellbeingJournalEntry serviceJournalEntry = new ServiceWellbeingJournalEntry();
    private final ServiceQuestionStress serviceQuestionStress = new ServiceQuestionStress();
    private final ServiceQuizStress serviceQuizStress = new ServiceQuizStress();
    private final ServiceRecommendationStress serviceRecommendationStress = new ServiceRecommendationStress();
    private final WellbeingAiService wellbeingAiService = new WellbeingAiService();
    private final ObservableList<WellBeing> allCheckins = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private WellBeing editingItem;
    private String selectedMood = "good";
    private double selectedSleepHours = 7d;
    private final List<QuestionStress> quizQuestions = new ArrayList<>();
    private final Map<Integer, Integer> quizAnswers = new HashMap<>();
    private int currentQuizIndex = 0;
    private static final int MIN_NOTE_LENGTH = 6;
    private static final int MAX_NOTE_LENGTH = 1000;
    private static final int MOOD_EMOJI_SIZE = 56;
    private static final int MOOD_EMOJI_FALLBACK_FONT_SIZE = 50;
    private record CopingToolDef(String key, String title, String durationLabel, int durationSeconds, String description) {}
    private record MoodEntry(String day, String mood, String icon, String color) {}
    private record HabitData(String name, String icon, String color, boolean[] weekProgress) {}
    private record MindfulnessSession(String title, String duration, String icon, String color) {}
    private record SleepEntry(String day, double hours, String quality) {}

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        hideGlobalMessage();
        setupFilters();
        setupForm();
        loadData();
        showOverviewMode();
    }

    private void setupFilters() {
        if (overviewSortCombo != null) {
            overviewSortCombo.setItems(FXCollections.observableArrayList(
                    "Date (Newest)", "Date (Oldest)", "Stress (High -> Low)", "Stress (Low -> High)",
                    "Energy (High -> Low)", "Energy (Low -> High)"
            ));
            overviewSortCombo.setValue("Date (Newest)");
            overviewSortCombo.setOnAction(event -> refreshOverview());
        }

        if (historyMoodFilter != null) {
            historyMoodFilter.setItems(FXCollections.observableArrayList("All moods", "great", "good", "okay", "stressed", "tired"));
            historyMoodFilter.setValue("All moods");
            historyMoodFilter.setOnAction(event -> refreshHistoryTable());
        }

        if (historySortCombo != null) {
            historySortCombo.setItems(FXCollections.observableArrayList(
                    "Date (Newest)", "Date (Oldest)", "Stress (High -> Low)", "Stress (Low -> High)",
                    "Energy (High -> Low)", "Energy (Low -> High)", "Mood (A-Z)"
            ));
            historySortCombo.setValue("Date (Newest)");
            historySortCombo.setOnAction(event -> refreshHistoryTable());
        }

        if (historySearchField != null) {
            historySearchField.textProperty().addListener((obs, oldValue, newValue) -> refreshHistoryTable());
        }
    }

    private void setupForm() {
        stressSlider.valueProperty().addListener((obs, oldVal, newVal) -> stressValueLabel.setText(String.valueOf(newVal.intValue())));
        energySlider.valueProperty().addListener((obs, oldVal, newVal) -> energyValueLabel.setText(String.valueOf(newVal.intValue())));
        stressValueLabel.setText(String.valueOf((int) stressSlider.getValue()));
        energyValueLabel.setText(String.valueOf((int) energySlider.getValue()));
        configureMoodButtons();
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);
        updateChoiceSelection(moodButtonsBox, selectedMood);
        updateChoiceSelection(sleepButtonsBox, String.valueOf((int) selectedSleepHours));
    }

    private void configureMoodButtons() {
        if (moodButtonsBox == null) {
            return;
        }

        for (Node child : moodButtonsBox.getChildren()) {
            if (!(child instanceof Button button) || button.getUserData() == null) {
                continue;
            }
            String moodKey = String.valueOf(button.getUserData()).toLowerCase().trim();
            String label = EmojiUtils.getMoodLabelMap().getOrDefault(moodKey, capitalize(moodKey));
            String emojiUnicode = EmojiUtils.getMoodEmojiUnicode(moodKey);

            button.setText(label);
            button.setContentDisplay(ContentDisplay.TOP);
            button.setGraphicTextGap(12);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setMinWidth(210);

            ImageView emojiImage = EmojiUtils.loadMoodEmojiImage(moodKey, MOOD_EMOJI_SIZE);
            if (emojiImage != null) {
                button.setGraphic(emojiImage);
            } else {
                Label fallbackEmoji = new Label(emojiUnicode);
                fallbackEmoji.setStyle("-fx-font-size: " + MOOD_EMOJI_FALLBACK_FONT_SIZE + "px; -fx-font-family: 'Segoe UI Emoji';");
                button.setGraphic(fallbackEmoji);
            }
        }

        // On applique juste la sélection initiale (le "Good" en vert)
        updateChoiceSelection(moodButtonsBox, selectedMood);
    }

    private void loadData() {
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null || currentUserId <= 0) {
            allCheckins.clear();
            showError("Please log in with a valid account to access your wellbeing data.");
            return;
        }
        allCheckins.setAll(serviceWellBeing.findAllForUser(currentUserId));
        refreshOverview();
        refreshHistoryTable();
    }

    private void refreshOverview() {
        List<WellBeing> sorted = new ArrayList<>(allCheckins);
        sortBySelection(sorted, overviewSortCombo != null ? overviewSortCombo.getValue() : null);

        updateHeroSummary(sorted);
        updateStatsLabels(sorted, historyTotalLabel, historyAvgStressLabel, historyAvgEnergyLabel, historyAvgSleepLabel);
        if (totalCheckinsLabel != null) {
            updateStatsLabels(sorted, totalCheckinsLabel, avgStressLabel, avgEnergyLabel, avgSleepLabel);
        }

        populateStressChart(sorted.stream().limit(10).collect(Collectors.toList()));
        populateRecentCheckins(sorted.stream().limit(6).collect(Collectors.toList()));
        populateCopingTools();
        populateMoodDistribution(sorted);
        loadMoodWeek(sorted);
        loadHabits();
        loadMindfulnessSessions();
        loadSleepLog(sorted);
    }

    private void refreshHistoryTable() {
        if (historyListBox == null) {
            return;
        }

        List<WellBeing> filtered = new ArrayList<>(allCheckins);
        String search = historySearchField == null || historySearchField.getText() == null
                ? ""
                : historySearchField.getText().trim().toLowerCase();
        String selectedMood = historyMoodFilter != null ? historyMoodFilter.getValue() : null;

        if (!search.isBlank()) {
            filtered = filtered.stream()
                    .filter(item -> containsIgnoreCase(item.getMood(), search) || containsIgnoreCase(item.getNote(), search))
                    .collect(Collectors.toList());
        }

        if (selectedMood != null && !"All moods".equals(selectedMood)) {
            filtered = filtered.stream()
                    .filter(item -> selectedMood.equalsIgnoreCase(item.getMood()))
                    .collect(Collectors.toList());
        }

        sortBySelection(filtered, historySortCombo != null ? historySortCombo.getValue() : null);
        populateHistoryList(filtered);
    }

    private void populateStressChart(List<WellBeing> sortedCheckins) {
        if (stressChart == null) {
            return;
        }
        stressChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        List<WellBeing> chronological = new ArrayList<>(sortedCheckins);
        chronological.sort(Comparator.comparing(WellBeing::getEntryDate));
        for (WellBeing item : chronological) {
            XYChart.Data<String, Number> point = new XYChart.Data<>(
                    item.getEntryDate().format(DateTimeFormatter.ofPattern("MMM d")),
                    item.getStressLevel()
            );
            point.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode == null) {
                    return;
                }
                int value = point.getYValue().intValue();
                if (value <= 4) {
                    newNode.setStyle("-fx-bar-fill: #22C55E;");
                } else if (value <= 6) {
                    newNode.setStyle("-fx-bar-fill: #F59E0B;");
                } else {
                    newNode.setStyle("-fx-bar-fill: #EF4444;");
                }
            });
            series.getData().add(point);
        }
        stressChart.getData().add(series);
    }

    private void populateRecentCheckins(List<WellBeing> checkins) {
        if (recentCheckinsBox == null) {
            return;
        }
        recentCheckinsBox.getChildren().clear();
        if (checkins.isEmpty()) {
            recentCheckinsBox.getChildren().add(createEmptyCard("No check-ins yet", "Start with your first daily check-in."));
            return;
        }

        for (WellBeing item : checkins) {
            HBox row = new HBox(14);
            row.getStyleClass().add("wellbeing-list-item");

            String moodKey = item.getMood() == null ? "okay" : item.getMood().toLowerCase().trim();
            String emojiUnicode = EmojiUtils.getMoodEmojiUnicode(moodKey);
            ImageView emojiImage = EmojiUtils.loadMoodEmojiImage(moodKey, MOOD_EMOJI_SIZE);
            Node emojiNode;
            if (emojiImage != null) {
                emojiNode = emojiImage;
            } else {
                Label emojiFallback = new Label(emojiUnicode);
                emojiFallback.getStyleClass().add("wellbeing-list-emoji");
                emojiFallback.setStyle("-fx-font-size: " + MOOD_EMOJI_FALLBACK_FONT_SIZE + "px; -fx-font-family: 'Segoe UI Emoji';");
                emojiNode = emojiFallback;
            }

            VBox content = new VBox(4);
            Label mood = new Label(capitalize(item.getMood()));
            mood.getStyleClass().add("text-body");
            mood.setStyle("-fx-font-weight: 700; -fx-text-fill: " + moodColor(item.getMood()) + ";");
            Label date = new Label(formatDate(item.getEntryDate()));
            date.getStyleClass().add("text-small");
            date.setStyle("-fx-text-fill: #14B8A6;");
            content.getChildren().addAll(mood, date);
            HBox.setHgrow(content, Priority.ALWAYS);

            VBox metrics = new VBox(3);
            metrics.setAlignment(Pos.CENTER_RIGHT);
            Label stress = new Label("Stress: " + item.getStressLevel() + "/10");
            stress.getStyleClass().add("text-small");
            stress.setStyle("-fx-text-fill: " + stressColor(item.getStressLevel()) + "; -fx-font-weight: 700;");
            Label sleep = new Label("Sleep: " + item.getFormattedSleep());
            sleep.getStyleClass().add("text-small");
            sleep.setStyle("-fx-text-fill: #93C5FD; -fx-font-weight: 700;");
            metrics.getChildren().addAll(stress, sleep);

            row.getChildren().addAll(emojiNode, content, metrics);
            recentCheckinsBox.getChildren().add(row);
        }
    }

    private void populateHistoryList(List<WellBeing> checkins) {
        if (historyListBox == null) {
            return;
        }

        historyListBox.getChildren().clear();
        if (checkins.isEmpty()) {
            historyListBox.getChildren().add(createEmptyCard("No check-ins yet", "Start tracking your wellbeing by creating your first check-in."));
            return;
        }

        for (WellBeing item : checkins) {
            HBox row = new HBox(14);
            row.getStyleClass().add("wellbeing-history-item");
            row.setAlignment(Pos.CENTER_LEFT);

            VBox moodBox = new VBox(4);
            moodBox.setAlignment(Pos.CENTER);
            moodBox.getStyleClass().add("wellbeing-history-mood-box");
            Label emoji = new Label(item.getMoodEmoji());
            emoji.getStyleClass().add("wellbeing-list-emoji");
            moodBox.getChildren().add(emoji);

            VBox main = new VBox(4);
            HBox.setHgrow(main, Priority.ALWAYS);
            Label title = new Label(capitalize(item.getMood()) + "  •  " + formatDate(item.getEntryDate()));
            title.getStyleClass().add("text-body");
            title.setStyle("-fx-font-weight: 700; -fx-text-fill: " + moodColor(item.getMood()) + ";");
            Label note = new Label(item.getNote() == null || item.getNote().isBlank() ? "No notes" : item.getNote());
            note.getStyleClass().add("text-small");
            note.setWrapText(true);
            main.getChildren().addAll(title, note);

            VBox metrics = new VBox(4);
            metrics.setAlignment(Pos.CENTER_RIGHT);
            Label stress = new Label("Stress: " + item.getStressLevel() + "/10");
            stress.getStyleClass().add("text-small");
            Label energy = new Label("Energy: " + item.getEnergyLevel() + "/10");
            energy.getStyleClass().add("text-small");
            Label sleep = new Label("Sleep: " + item.getFormattedSleep());
            sleep.getStyleClass().add("text-small");
            metrics.getChildren().addAll(stress, energy, sleep);

            Button editBtn = new Button("Edit");
            editBtn.getStyleClass().add("btn-secondary");
            editBtn.setOnAction(event -> startEdit(item));

            Button deleteBtn = new Button("Delete");
            deleteBtn.getStyleClass().add("btn-danger");
            deleteBtn.setOnAction(event -> deleteItem(item));

            VBox actions = new VBox(8, editBtn, deleteBtn);
            actions.setAlignment(Pos.CENTER_RIGHT);

            row.getChildren().addAll(moodBox, main, metrics, actions);
            historyListBox.getChildren().add(row);
        }
    }

    private void populateMoodDistribution(List<WellBeing> checkins) {
        if (moodDistributionPane == null) {
            return;
        }
        moodDistributionPane.getChildren().clear();
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("great", 0L);
        counts.put("good", 0L);
        counts.put("okay", 0L);
        counts.put("stressed", 0L);
        counts.put("tired", 0L);
        checkins.forEach(item -> counts.computeIfPresent(item.getMood(), (key, value) -> value + 1));

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            VBox card = new VBox(8);
            card.getStyleClass().add("wellbeing-mood-card");
            card.setAlignment(Pos.CENTER);
            Label emoji = new Label(emojiForMood(entry.getKey()));
            emoji.getStyleClass().add("wellbeing-mood-emoji");
            Label count = new Label(String.valueOf(entry.getValue()));
            count.getStyleClass().add("stat-value");
            count.setStyle("-fx-font-size: 24px;");
            Label mood = new Label(capitalize(entry.getKey()));
            mood.getStyleClass().add("text-small");
            card.getChildren().addAll(emoji, count, mood);
            moodDistributionPane.getChildren().add(card);
        }
    }

    private void populateCopingTools() {
        if (copingToolsPane == null) {
            return;
        }
        copingToolsPane.getChildren().clear();
        for (CopingToolDef tool : copingToolsCatalog()) {
            VBox card = new VBox(8);
            card.getStyleClass().add("wellbeing-tool-card");
            Label title = new Label(tool.title());
            title.getStyleClass().add("text-body");
            title.setStyle("-fx-font-weight: 600;");
            Label desc = new Label(tool.description());
            desc.getStyleClass().add("text-small");
            desc.setWrapText(true);
            Label duration = new Label(tool.durationLabel());
            duration.getStyleClass().addAll("badge", "accent");

            Button startBtn = new Button("Start");
            startBtn.getStyleClass().add("btn-primary");
            startBtn.setOnAction(event -> openCopingTool(tool));
            card.getChildren().addAll(title, desc, duration, startBtn);
            copingToolsPane.getChildren().add(card);
        }
    }

    private List<CopingToolDef> copingToolsCatalog() {
        return List.of(
                new CopingToolDef("breathing_exercise", "Breathing Exercise", "3 min", 180, "Guided 4-7-8 breathing session."),
                new CopingToolDef("gratitude_journal", "Gratitude Journal", "2 min", 120, "Write what you are grateful for and save entries."),
                new CopingToolDef("yoga_coach", "Yoga Coach", "3 min", 180, "Follow short yoga stretches with automatic timer."),
                new CopingToolDef("ai_chat_coach", "AI Chat Coach", "5 min", 300, "Chat with a supportive wellbeing assistant."),
                new CopingToolDef("nature_sounds", "Nature Sounds", "5 min", 300, "Quiet focus timer inspired by ambient nature sessions.")
        );
    }

    private void openCopingTool(CopingToolDef tool) {
        Integer userId = getCurrentUserId();
        if (userId == null || userId <= 0) {
            showError("Please log in with a valid account to start coping tools.");
            return;
        }

        final CopingSession session;
        try {
            session = serviceCopingSession.startSession(userId, tool.key(), tool.title(), tool.durationSeconds());
        } catch (RuntimeException e) {
            showError(e.getMessage());
            return;
        }

        switch (tool.key()) {
            case "gratitude_journal" -> openJournalTool(session);
            case "yoga_coach" -> openYogaTool(session);
            case "ai_chat_coach" -> openAiChatTool(session);
            case "nature_sounds" -> openNatureSoundsTool(session);
            default -> openBreathingTool(session);
        }
    }

    private Stage createToolStage(String title, VBox body, int width, int height) {
        Stage stage = new Stage();
        stage.initModality(Modality.NONE);
        stage.setTitle(title);
        body.setPadding(new Insets(18));
        body.setSpacing(12);
        if (body.getStyle() == null || body.getStyle().isBlank()) {
            body.setStyle("-fx-background-color: #0F172A;");
        }
        Scene scene = new Scene(body, width, height);
        stage.setScene(scene);
        return stage;
    }

    private void openBreathingTool(CopingSession session) {
        Label phaseLabel = new Label("Ready to breathe");
        phaseLabel.getStyleClass().add("text-heading");
        phaseLabel.setStyle("-fx-font-size: 22px;");

        Label statusLabel = new Label("Pattern: Inhale 4s - Hold 7s - Exhale 8s");
        statusLabel.getStyleClass().add("text-small");
        Label roundLabel = new Label("Round 0/3");
        roundLabel.getStyleClass().add("text-body");

        int[] phaseIndex = {0};
        int[] phaseSecondsLeft = {4};
        int[] round = {0};
        String[] phases = {"Inhale slowly...", "Hold gently...", "Exhale softly..."};
        int[] phaseDurations = {4, 7, 8};
        boolean[] completed = {false};

        Timeline[] timeline = new Timeline[1];
        timeline[0] = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> {
            if (round[0] >= 3) {
                return;
            }

            phaseSecondsLeft[0]--;
            if (phaseSecondsLeft[0] > 0) {
                phaseLabel.setText(phases[phaseIndex[0]] + " (" + phaseSecondsLeft[0] + "s)");
                return;
            }

            phaseIndex[0]++;
            if (phaseIndex[0] >= phases.length) {
                phaseIndex[0] = 0;
                round[0]++;
                roundLabel.setText("Round " + Math.min(round[0], 3) + "/3");
                if (round[0] >= 3) {
                    completed[0] = true;
                    timeline[0].stop();
                    phaseLabel.setText("Session complete");
                    statusLabel.setText("Great job. You completed all breathing rounds.");
                    return;
                }
            }
            phaseSecondsLeft[0] = phaseDurations[phaseIndex[0]];
            phaseLabel.setText(phases[phaseIndex[0]] + " (" + phaseSecondsLeft[0] + "s)");
        }));
        timeline[0].setCycleCount(Timeline.INDEFINITE);

        Button startBtn = new Button("Start");
        startBtn.getStyleClass().add("btn-primary");
        startBtn.setOnAction(e -> {
            round[0] = 0;
            phaseIndex[0] = 0;
            phaseSecondsLeft[0] = phaseDurations[0];
            completed[0] = false;
            roundLabel.setText("Round 0/3");
            phaseLabel.setText(phases[0] + " (" + phaseSecondsLeft[0] + "s)");
            statusLabel.setText("Breathing session running...");
            timeline[0].playFromStart();
        });

        Button stopBtn = new Button("Stop");
        stopBtn.getStyleClass().add("btn-danger");
        stopBtn.setOnAction(e -> {
            timeline[0].stop();
            phaseLabel.setText("Session stopped");
            statusLabel.setText("You can restart anytime.");
        });

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-secondary");

        HBox controls = new HBox(10, startBtn, stopBtn, closeBtn);
        VBox root = new VBox(12, phaseLabel, statusLabel, roundLabel, controls);
        Stage stage = createToolStage("Breathing Exercise", root, 520, 280);

        LocalDateTime openedAt = LocalDateTime.now();
        Runnable closer = () -> {
            timeline[0].stop();
            closeSession(session, openedAt, completed[0]);
        };
        closeBtn.setOnAction(e -> {
            closer.run();
            stage.close();
        });
        stage.setOnCloseRequest(e -> closer.run());
        stage.show();
    }

    private void openYogaTool(CopingSession session) {
        List<String[]> exercises = List.of(
                new String[]{"Neck Release", "Tilt head gently left and right. Keep shoulders relaxed."},
                new String[]{"Shoulder Rolls", "Roll shoulders forward then backward with slow breaths."},
                new String[]{"Seated Twist", "Sit tall and twist softly to each side, no force."},
                new String[]{"Cat-Cow Stretch", "Alternate arching and rounding your back with your breath."},
                new String[]{"Child Pose", "Kneel, stretch your arms forward, and breathe deeply."}
        );

        Label titleLabel = new Label(exercises.get(0)[0]);
        titleLabel.getStyleClass().add("text-heading");
        titleLabel.setStyle("-fx-font-size: 22px;");
        Label descLabel = new Label(exercises.get(0)[1]);
        descLabel.getStyleClass().add("text-body");
        descLabel.setWrapText(true);
        Label timerLabel = new Label("20s");
        timerLabel.getStyleClass().add("text-subheading");
        Label stepLabel = new Label("1/" + exercises.size());
        stepLabel.getStyleClass().add("text-small");
        Label statusLabel = new Label("Click Start Yoga to begin.");
        statusLabel.getStyleClass().add("text-small");

        int[] index = {0};
        int[] seconds = {20};
        boolean[] running = {false};
        boolean[] completed = {false};

        Timeline[] timeline = new Timeline[1];
        timeline[0] = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> {
            if (!running[0]) {
                return;
            }
            seconds[0]--;
            timerLabel.setText(Math.max(0, seconds[0]) + "s");
            if (seconds[0] > 0) {
                return;
            }
            if (index[0] >= exercises.size() - 1) {
                running[0] = false;
                completed[0] = true;
                timeline[0].stop();
                titleLabel.setText("Session Complete");
                descLabel.setText("Great job. You completed all yoga exercises.");
                statusLabel.setText("Yoga Coach: all exercises completed.");
                return;
            }
            index[0]++;
            seconds[0] = 20;
            titleLabel.setText(exercises.get(index[0])[0]);
            descLabel.setText(exercises.get(index[0])[1]);
            stepLabel.setText((index[0] + 1) + "/" + exercises.size());
            timerLabel.setText(seconds[0] + "s");
            statusLabel.setText("Exercise " + (index[0] + 1) + " of " + exercises.size());
        }));
        timeline[0].setCycleCount(Timeline.INDEFINITE);

        Button startBtn = new Button("Start Yoga");
        startBtn.getStyleClass().add("btn-primary");
        startBtn.setOnAction(e -> {
            if (completed[0]) {
                index[0] = 0;
                seconds[0] = 20;
                completed[0] = false;
                titleLabel.setText(exercises.get(0)[0]);
                descLabel.setText(exercises.get(0)[1]);
                stepLabel.setText("1/" + exercises.size());
            }
            running[0] = true;
            timerLabel.setText(seconds[0] + "s");
            statusLabel.setText("Exercise " + (index[0] + 1) + " of " + exercises.size());
            timeline[0].play();
        });

        Button nextBtn = new Button("Next");
        nextBtn.getStyleClass().add("btn-secondary");
        nextBtn.setOnAction(e -> {
            if (index[0] >= exercises.size() - 1) {
                return;
            }
            index[0]++;
            seconds[0] = 20;
            titleLabel.setText(exercises.get(index[0])[0]);
            descLabel.setText(exercises.get(index[0])[1]);
            stepLabel.setText((index[0] + 1) + "/" + exercises.size());
            timerLabel.setText(seconds[0] + "s");
            statusLabel.setText("Exercise " + (index[0] + 1) + " of " + exercises.size());
        });

        Button closeBtn = new Button("Back to Coping Tools");
        closeBtn.getStyleClass().add("btn-secondary");
        HBox controls = new HBox(10, startBtn, nextBtn, closeBtn);
        VBox root = new VBox(10, titleLabel, descLabel, timerLabel, stepLabel, statusLabel, controls);
        Stage stage = createToolStage("Yoga Coach", root, 650, 320);

        LocalDateTime openedAt = LocalDateTime.now();
        Runnable closer = () -> {
            timeline[0].stop();
            closeSession(session, openedAt, completed[0]);
        };
        closeBtn.setOnAction(e -> {
            closer.run();
            stage.close();
        });
        stage.setOnCloseRequest(e -> closer.run());
        stage.show();
    }

    private void openJournalTool(CopingSession session) {
        Integer userId = getCurrentUserId();
        if (userId == null || userId <= 0) {
            showError("Please log in to use journal.");
            return;
        }

        Label titleLabel = new Label("Gratitude Journal");
        titleLabel.getStyleClass().add("text-heading");
        titleLabel.setStyle("-fx-font-size: 22px;");
        Label statusLabel = new Label("Write and save your entry.");
        statusLabel.getStyleClass().add("text-small");

        TextArea input = new TextArea();
        input.setPromptText("Write three things you're grateful for...");
        input.setWrapText(true);
        input.setPrefRowCount(5);

        ComboBox<String> langBox = new ComboBox<>(FXCollections.observableArrayList(
                "Auto", "en-US", "fr-FR", "ar-SA", "ar-TN"
        ));
        langBox.setValue("Auto");

        ListView<WellbeingJournalEntry> listView = new ListView<>();
        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(WellbeingJournalEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                String date = item.getCreatedAt() == null
                        ? "-"
                        : item.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                setText(date + "  |  " + item.getContent());
            }
        });

        int[] editingId = {0};
        boolean[] completed = {false};
        Runnable reload = () -> listView.getItems().setAll(serviceJournalEntry.findByUser(userId, 100));
        reload.run();

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            String content = input.getText() == null ? "" : input.getText().trim();
            if (content.isBlank()) {
                statusLabel.setText("Write something first.");
                return;
            }
            try {
                String languageCode = "Auto".equalsIgnoreCase(langBox.getValue()) ? null : langBox.getValue();
                if (editingId[0] > 0) {
                    WellbeingJournalEntry item = new WellbeingJournalEntry();
                    item.setId(editingId[0]);
                    item.setContent(content);
                    item.setLanguageCode(languageCode);
                    item.setInputMode("text");
                    item.setUserId(userId);
                    serviceJournalEntry.update(item);
                    statusLabel.setText("Entry updated.");
                } else {
                    WellbeingJournalEntry item = new WellbeingJournalEntry();
                    item.setContent(content);
                    item.setLanguageCode(languageCode);
                    item.setInputMode("text");
                    item.setCreatedAt(LocalDateTime.now());
                    item.setUserId(userId);
                    serviceJournalEntry.add(item);
                    statusLabel.setText("Entry saved.");
                }
                completed[0] = true;
                editingId[0] = 0;
                input.clear();
                reload.run();
            } catch (RuntimeException ex) {
                statusLabel.setText("Save error: " + ex.getMessage());
            }
        });

        Button newBtn = new Button("New");
        newBtn.getStyleClass().add("btn-secondary");
        newBtn.setOnAction(e -> {
            editingId[0] = 0;
            input.clear();
            statusLabel.setText("New journal entry.");
        });

        Button editBtn = new Button("Edit Selected");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setOnAction(e -> {
            WellbeingJournalEntry selected = listView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                statusLabel.setText("Select an entry first.");
                return;
            }
            editingId[0] = selected.getId();
            input.setText(selected.getContent());
            if (selected.getLanguageCode() != null && !selected.getLanguageCode().isBlank()) {
                langBox.setValue(selected.getLanguageCode());
            } else {
                langBox.setValue("Auto");
            }
            statusLabel.setText("Editing entry #" + selected.getId());
        });

        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> {
            WellbeingJournalEntry selected = listView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                statusLabel.setText("Select an entry first.");
                return;
            }
            try {
                serviceJournalEntry.delete(selected.getId(), userId);
                if (editingId[0] == selected.getId()) {
                    editingId[0] = 0;
                    input.clear();
                }
                statusLabel.setText("Entry deleted.");
                reload.run();
            } catch (RuntimeException ex) {
                statusLabel.setText("Delete error: " + ex.getMessage());
            }
        });

        Button closeBtn = new Button("Back to Coping Tools");
        closeBtn.getStyleClass().add("btn-secondary");

        HBox topRow = new HBox(10, saveBtn, newBtn, editBtn, deleteBtn, closeBtn);
        VBox root = new VBox(10, titleLabel, statusLabel, langBox, input, topRow, listView);
        Stage stage = createToolStage("Gratitude Journal", root, 780, 600);

        LocalDateTime openedAt = LocalDateTime.now();
        Runnable closer = () -> closeSession(session, openedAt, completed[0]);
        closeBtn.setOnAction(e -> {
            closer.run();
            stage.close();
        });
        stage.setOnCloseRequest(e -> closer.run());
        stage.show();
    }

    private void openAiChatTool(CopingSession session) {
        VBox root = new VBox(14);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #040b19, #07122a, #0a1633);");

        VBox header = new VBox(6);
        header.setPadding(new Insets(16));
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, #10224c, #0a1f45, #103b5f);" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: #1f9fe3;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 16;"
        );
        Label focusLabel = new Label("FOCUS SESSION");
        focusLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: 700; -fx-letter-spacing: 1px;");
        Label titleLabel = new Label("AI Chat Coach");
        titleLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 38px; -fx-font-weight: 800;");
        Label subtitle = new Label("Motivation · Guidance · Support · Any Language");
        subtitle.setStyle("-fx-text-fill: #93c5fd; -fx-font-size: 13px;");
        header.getChildren().addAll(focusLabel, titleLabel, subtitle);

        Label statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 12px; -fx-font-weight: 600;");

        VBox messagesBox = new VBox(12);
        messagesBox.setPadding(new Insets(14));
        messagesBox.setStyle("-fx-background-color: #061433;");
        messagesBox.getChildren().add(chatBubble("assistant", "Hello! I am here to help. What do you need right now?"));

        ScrollPane messagesPane = new ScrollPane(messagesBox);
        messagesPane.setFitToWidth(true);
        messagesPane.setPrefHeight(370);
        messagesPane.setStyle(
                "-fx-background: #061433;" +
                "-fx-background-color: #061433;" +
                "-fx-border-color: #155e99;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 14;" +
                "-fx-background-radius: 14;"
        );

        ComboBox<String> modeBox = new ComboBox<>(FXCollections.observableArrayList("General", "Supportive", "Practical"));
        modeBox.setValue("General");
        ComboBox<String> styleBox = new ComboBox<>(FXCollections.observableArrayList("Direct", "Warm", "Concise"));
        styleBox.setValue("Direct");
        ComboBox<String> levelBox = new ComboBox<>(FXCollections.observableArrayList("Professional", "Friendly", "Simple"));
        levelBox.setValue("Professional");
        ComboBox<String> languageBox = new ComboBox<>(FXCollections.observableArrayList("Auto", "English", "Francais", "Arabic", "Tunisian Arabic"));
        languageBox.setValue("Auto");
        for (ComboBox<String> box : List.of(modeBox, styleBox, levelBox, languageBox)) {
            box.setPrefWidth(170);
            box.setStyle(
                    "-fx-background-color: #142847;" +
                    "-fx-text-fill: #dbeafe;" +
                    "-fx-border-color: #1f5d93;" +
                    "-fx-border-radius: 9;" +
                    "-fx-background-radius: 9;"
            );
        }
        HBox configRow = new HBox(10, modeBox, styleBox, levelBox, languageBox);

        HBox chipsRow = new HBox(8,
                createChatChip("I feel stressed"),
                createChatChip("Motivation boost"),
                createChatChip("Focus tips"),
                createChatChip("Help me relax")
        );

        TextArea input = new TextArea();
        input.setPromptText("Write your message in any language...");
        input.setWrapText(true);
        input.setPrefRowCount(3);
        input.setStyle(
                "-fx-control-inner-background: #0d203e;" +
                "-fx-text-fill: #e2e8f0;" +
                "-fx-prompt-text-fill: #6b8ab3;" +
                "-fx-border-color: #1e5a93;" +
                "-fx-border-width: 1.4;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;"
        );

        List<WellbeingAiService.ChatTurn> chatHistory = new ArrayList<>();
        boolean[] completed = {false};

        Button sendBtn = new Button("Send");
        sendBtn.getStyleClass().add("btn-primary");
        Button clearBtn = new Button("New");
        clearBtn.getStyleClass().add("btn-secondary");
        Button closeBtn = new Button("Back");
        closeBtn.getStyleClass().add("btn-secondary");

        Runnable sendAction = () -> {
            String text = input.getText() == null ? "" : input.getText().trim();
            if (text.isBlank()) {
                statusLabel.setText("Write a message first.");
                return;
            }

            messagesBox.getChildren().add(chatBubble("user", text));
            chatHistory.add(new WellbeingAiService.ChatTurn("user", text));
            input.clear();
            statusLabel.setText("AI is typing...");
            sendBtn.setDisable(true);

            String selectedLanguageCode = switch (languageBox.getValue() == null ? "Auto" : languageBox.getValue()) {
                case "Francais" -> "fr-FR";
                case "Arabic" -> "ar-SA";
                case "Tunisian Arabic" -> "ar-TN";
                case "English" -> "en-US";
                default -> "auto";
            };

            Task<WellbeingAiService.CoachReply> task = new Task<>() {
                @Override
                protected WellbeingAiService.CoachReply call() {
                    return wellbeingAiService.coachReply(
                            text,
                            new ArrayList<>(chatHistory),
                            selectedLanguageCode,
                            modeBox.getValue(),
                            styleBox.getValue(),
                            levelBox.getValue()
                    );
                }
            };
            task.setOnSucceeded(evt -> {
                WellbeingAiService.CoachReply result = task.getValue();
                String reply = result == null ? "" : result.reply();
                if (reply == null || reply.isBlank()) {
                    reply = "Sorry, I could not generate a reply right now. Please try again.";
                }
                messagesBox.getChildren().add(chatBubble("assistant", reply));
                chatHistory.add(new WellbeingAiService.ChatTurn("assistant", reply));
                statusLabel.setText(result != null && "ai".equals(result.source()) ? "Answered by AI" : "Fallback response");
                completed[0] = true;
                sendBtn.setDisable(false);
                Platform.runLater(() -> {
                    messagesPane.layout();
                    messagesPane.setVvalue(1.0);
                });
            });
            task.setOnFailed(evt -> {
                messagesBox.getChildren().add(chatBubble("assistant", "Sorry, I could not generate a reply right now. Please try again."));
                statusLabel.setText("Error while generating response.");
                sendBtn.setDisable(false);
            });
            Thread worker = new Thread(task, "wellbeing-ai-chat");
            worker.setDaemon(true);
            worker.start();
        };

        sendBtn.setOnAction(e -> sendAction.run());
        input.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER && !e.isShiftDown()) {
                e.consume();
                sendAction.run();
            }
        });

        clearBtn.setOnAction(e -> {
            chatHistory.clear();
            messagesBox.getChildren().setAll(chatBubble("assistant", "Hello! I am here to help. What do you need right now?"));
            statusLabel.setText("Ready");
        });

        chipsRow.getChildren().forEach(node -> {
            if (node instanceof Button chip) {
                chip.setOnAction(e -> {
                    input.setText(chip.getText());
                    input.requestFocus();
                    input.positionCaret(input.getText().length());
                });
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10, statusLabel, spacer, clearBtn, closeBtn, sendBtn);

        VBox chatCard = new VBox(10, messagesPane, configRow, chipsRow, input, actions);
        chatCard.setPadding(new Insets(14));
        chatCard.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #071833, #05112a);" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: #155e99;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 16;"
        );

        root.getChildren().addAll(header, chatCard);
        Stage stage = createToolStage("AI Chat Coach", root, 1120, 760);

        LocalDateTime openedAt = LocalDateTime.now();
        Runnable closer = () -> closeSession(session, openedAt, completed[0]);
        closeBtn.setOnAction(e -> {
            closer.run();
            stage.close();
        });
        stage.setOnCloseRequest(e -> closer.run());
        stage.show();
    }

    private Button createChatChip(String text) {
        Button chip = new Button(text);
        chip.setStyle(
                "-fx-background-color: #0f274a;" +
                "-fx-text-fill: #93c5fd;" +
                "-fx-background-radius: 100;" +
                "-fx-border-color: #1f5d93;" +
                "-fx-border-radius: 100;" +
                "-fx-padding: 5 12;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: 600;"
        );
        return chip;
    }

    private Node chatBubble(String role, String text) {
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setMaxWidth(760);
        msg.setPadding(new Insets(10, 14, 10, 14));
        msg.getStyleClass().add("text-body");
        if ("user".equals(role)) {
            msg.setStyle(
                    "-fx-background-color: #1f2d46;" +
                    "-fx-text-fill: #e2e8f0;" +
                    "-fx-background-radius: 14;" +
                    "-fx-border-color: #334b6c;" +
                    "-fx-border-radius: 14;"
            );
        } else {
            msg.setStyle(
                    "-fx-background-color: #15346b;" +
                    "-fx-text-fill: #dbeafe;" +
                    "-fx-background-radius: 14;" +
                    "-fx-border-color: #1d4ed8;" +
                    "-fx-border-radius: 14;"
            );
        }
        Label time = new Label("just now");
        time.setStyle("-fx-text-fill: #6b8ab3; -fx-font-size: 11px;");
        VBox block = new VBox(4, msg, time);
        HBox row = new HBox(block);
        row.setAlignment("user".equals(role) ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return row;
    }

    private void openNatureSoundsTool(CopingSession session) {
        Label title = new Label("Nature Sounds Focus");
        title.getStyleClass().add("text-heading");
        title.setStyle("-fx-font-size: 22px;");

        Label info = new Label("A quiet 5-minute focus timer inspired by nature ambience.");
        info.getStyleClass().add("text-body");
        Label timer = new Label("05:00");
        timer.getStyleClass().add("text-heading");
        timer.setStyle("-fx-font-size: 38px;");
        Label status = new Label("Press Start to begin.");
        status.getStyleClass().add("text-small");

        int[] total = {300};
        boolean[] completed = {false};
        Timeline[] timeline = new Timeline[1];
        timeline[0] = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> {
            total[0]--;
            int minutes = Math.max(0, total[0]) / 60;
            int seconds = Math.max(0, total[0]) % 60;
            timer.setText(String.format("%02d:%02d", minutes, seconds));
            if (total[0] <= 0) {
                timeline[0].stop();
                completed[0] = true;
                status.setText("Session complete. Well done.");
            }
        }));
        timeline[0].setCycleCount(Timeline.INDEFINITE);

        Button start = new Button("Start");
        start.getStyleClass().add("btn-primary");
        start.setOnAction(e -> {
            total[0] = 300;
            timer.setText("05:00");
            status.setText("Focus session running...");
            completed[0] = false;
            timeline[0].playFromStart();
        });

        Button stop = new Button("Stop");
        stop.getStyleClass().add("btn-danger");
        stop.setOnAction(e -> {
            timeline[0].stop();
            status.setText("Session stopped.");
        });

        Button close = new Button("Close");
        close.getStyleClass().add("btn-secondary");
        HBox actions = new HBox(10, start, stop, close);

        VBox root = new VBox(12, title, info, timer, status, actions);
        Stage stage = createToolStage("Nature Sounds", root, 520, 300);

        LocalDateTime openedAt = LocalDateTime.now();
        Runnable closer = () -> {
            timeline[0].stop();
            closeSession(session, openedAt, completed[0]);
        };
        close.setOnAction(e -> {
            closer.run();
            stage.close();
        });
        stage.setOnCloseRequest(e -> closer.run());
        stage.show();
    }

    private void closeSession(CopingSession session, LocalDateTime openedAt, boolean completed) {
        Integer userId = getCurrentUserId();
        if (session == null || userId == null || userId <= 0) {
            return;
        }
        int actual = (int) Math.max(1, Duration.between(openedAt, LocalDateTime.now()).getSeconds());
        try {
            serviceCopingSession.finishSession(session.getId(), userId, completed ? "finished" : "cancelled", actual);
            loadData();
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    private String buildLocalCoachReply(String message) {
        String text = message == null ? "" : message.trim().toLowerCase();
        if (text.isBlank()) {
            return "Tell me what you are feeling, and we will take it step by step.";
        }
        if (isHighRiskMessage(text)) {
            return "I am really sorry you are going through this. Your safety comes first. Please call local emergency services now and contact someone you trust nearby.";
        }
        if (containsAny(text, "sleep", "insomnia", "dormir", "sommeil", "noum", "n3ass")) {
            return "For tonight: avoid screens 30 minutes before bed, do slow breathing for 3 minutes, and keep your room cool and dark.";
        }
        if (containsAny(text, "stress", "anxiety", "overwhelmed", "angoisse")) {
            return "Let us reduce stress now: breathe in 4 seconds, hold 4, exhale 6 for 2 minutes, then choose one small action for the next 10 minutes.";
        }
        if (containsAny(text, "focus", "concentr", "study", "exam", "revision")) {
            return "Use a 25-minute focus block with phone away, then a 5-minute break. Start with one clear task only.";
        }
        if (containsAny(text, "motivation", "demotiv", "discouraged")) {
            return "Set a tiny goal for 5 minutes, start now, and reward yourself after this first step.";
        }
        return "I am with you. Tell me your top challenge right now, and I will give you a short practical plan.";
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean isHighRiskMessage(String text) {
        return containsAny(text,
                "suicide", "kill myself", "self harm", "hurt myself",
                "je veux mourir", "je veux me tuer", "mourir",
                "nheb nmout", "nmout");
    }

    @FXML
    private void handleSaveCheckin() {
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null || currentUserId <= 0) {
            showError("Please log in with a valid account to save check-ins.");
            return;
        }

        String note = notesArea.getText() == null ? "" : notesArea.getText().trim();

        // Réinitialiser l'erreur avant chaque vérification
        resetNotesError();

        if (note.isBlank()) {
            showNotesError("Note is required.");
            return;
        }
        if (note.length() < MIN_NOTE_LENGTH) {
            showNotesError("Note must be more than 5 characters.");
            return;
        }
        if (note.length() > MAX_NOTE_LENGTH) {
            showNotesError("Note must be 1000 characters or less.");
            return;
        }

        // Si tout est correct → on sauvegarde
        WellBeing item = editingItem != null ? editingItem : new WellBeing();
        item.setEntryDate(LocalDateTime.now());
        item.setMood(selectedMood);
        item.setStressLevel((int) Math.round(stressSlider.getValue()));
        item.setEnergyLevel((int) Math.round(energySlider.getValue()));
        item.setSleepHours(selectedSleepHours);
        item.setNote(note);
        item.setUserId(currentUserId);

        try {
            if (editingItem == null) {
                item.setCreatedAt(LocalDateTime.now());
                serviceWellBeing.add(item);
            } else {
                item.setUpdatedAt(LocalDateTime.now());
                serviceWellBeing.update(item);
            }

            resetForm();
            loadData();
            showOverviewMode();

        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    private void resetNotesError() {
        if (notesErrorLabel != null) {
            notesErrorLabel.setVisible(false);
            notesErrorLabel.setManaged(false);
        }

        if (notesArea != null) {
            notesArea.setStyle("-fx-background-color: #1E293B; " +
                    "-fx-control-inner-background: #1E293B; " +
                    "-fx-text-fill: #F1F5F9; " +
                    "-fx-prompt-text-fill: #64748B; " +
                    "-fx-border-color: #334155; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 12; " +
                    "-fx-background-radius: 12; " +
                    "-fx-padding: 14; " +
                    "-fx-font-size: 14px;");
        }
    }

    private void showNotesError(String message) {
        if (notesErrorLabel != null) {
            notesErrorLabel.setText(message);
            notesErrorLabel.setVisible(true);
            notesErrorLabel.setManaged(true);
        }

        if (notesArea != null) {
            notesArea.setStyle("-fx-background-color: #1E293B; " +
                    "-fx-control-inner-background: #1E293B; " +
                    "-fx-text-fill: #F1F5F9; " +
                    "-fx-prompt-text-fill: #64748B; " +
                    "-fx-border-color: #EF4444; " +
                    "-fx-border-width: 2.5; " +
                    "-fx-border-radius: 12; " +
                    "-fx-background-radius: 12; " +
                    "-fx-padding: 14; " +
                    "-fx-font-size: 14px;");

            notesArea.requestFocus();
        }
    }
    @FXML
    private void handleCancelEdit() {
        resetForm();
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    @FXML
    private void handleOpenCheckin() {
        resetForm();
        showCheckinMode();
    }

    @FXML
    private void handleBackToOverview() {
        resetForm();
        showOverviewMode();
    }

    @FXML
    private void handleViewAllHistory() {
        refreshHistoryTable();
        showHistoryMode();
    }

    @FXML
    private void handleOpenCopingTools() {
        showOverviewMode();
    }

    @FXML
    private void handleOpenQuiz() {
        startQuiz();
    }

    @FXML
    private void handleQuizPrevious() {
        if (currentQuizIndex > 0) {
            currentQuizIndex--;
            renderCurrentQuizQuestion();
        }
    }

    @FXML
    private void handleQuizNext() {
        if (!isCurrentQuestionAnswered()) {
            showError("Please select an answer before continuing.");
            return;
        }
        if (currentQuizIndex < quizQuestions.size() - 1) {
            currentQuizIndex++;
            renderCurrentQuizQuestion();
        }
    }

    @FXML
    private void handleQuizSubmit() {
        if (!isCurrentQuestionAnswered()) {
            showError("Please select an answer before submitting.");
            return;
        }
        if (quizAnswers.size() < quizQuestions.size()) {
            showError("Please answer all questions before submitting.");
            return;
        }

        int totalScore = quizAnswers.values().stream().mapToInt(Integer::intValue).sum();
        String stressLevel;
        String interpretation;

        if (totalScore <= 10) {
            stressLevel = "minimal";
            interpretation = "Your stress levels appear minimal. Keep your healthy habits and routine.";
        } else if (totalScore <= 20) {
            stressLevel = "mild";
            interpretation = "You are experiencing mild stress. Add breaks and relaxation techniques to your day.";
        } else if (totalScore <= 30) {
            stressLevel = "moderate";
            interpretation = "Your stress is moderate. Prioritize self-care and use coping tools regularly.";
        } else {
            stressLevel = "high";
            interpretation = "You are experiencing high stress. Please prioritize your mental health and seek support.";
        }

        QuizStress quiz = new QuizStress();
        quiz.setQuizDate(LocalDateTime.now());
        quiz.setAnswers(new LinkedHashMap<>(quizAnswers));
        quiz.setTotalScore(totalScore);
        quiz.setStressLevel(stressLevel);
        quiz.setInterpretation(interpretation);
        quiz.setCreatedWithAi(false);
        quiz.setCreatedAt(LocalDateTime.now());
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null || currentUserId <= 0) {
            showError("Please log in with a valid account to save quiz results.");
            return;
        }
        quiz.setUserId(currentUserId);

        try {
            serviceQuizStress.add(quiz);
            showQuizResults(quiz);
        } catch (RuntimeException e) {
            showError(e.getMessage() == null ? "Failed to save quiz result." : e.getMessage());
        }
    }

    @FXML
    private void handleRetakeQuiz() {
        startQuiz();
    }

    @FXML
    private void handleQuizBackToOverview() {
        showOverviewMode();
    }

    private void startQuiz() {
        quizQuestions.clear();
        quizAnswers.clear();
        currentQuizIndex = 0;

        List<QuestionStress> activeQuestions = serviceQuestionStress.findAll().stream()
                .filter(QuestionStress::isActive)
                .sorted(Comparator.comparingInt(QuestionStress::getQuestionNumber))
                .toList();

        quizQuestions.addAll(activeQuestions);
        if (quizQuestions.isEmpty()) {
            showError("No active quiz questions available. Please ask admin to activate questions.");
            return;
        }

        if (quizTotalQuestionsLabel != null) {
            quizTotalQuestionsLabel.setText(String.valueOf(quizQuestions.size()));
        }
        renderCurrentQuizQuestion();
        showQuizMode();
    }

    private void renderCurrentQuizQuestion() {
        if (quizQuestions.isEmpty()) {
            return;
        }
        QuestionStress question = quizQuestions.get(currentQuizIndex);

        quizCurrentQuestionLabel.setText(String.valueOf(currentQuizIndex + 1));
        quizQuestionTitleLabel.setText("Question " + (currentQuizIndex + 1));
        quizQuestionTextLabel.setText(question.getQuestionText());
        quizOptionsBox.getChildren().clear();

        ToggleGroup group = new ToggleGroup();
        Map<Integer, String> options = Map.of(
                0, "Not at all",
                1, "Several days",
                2, "More than half the days",
                3, "Nearly every day"
        );

        for (int value = 0; value <= 3; value++) {
            RadioButton option = new RadioButton(options.get(value));
            option.setUserData(value);
            option.setToggleGroup(group);
            option.getStyleClass().add("text-body");
            option.setWrapText(true);
            option.setPrefWidth(Double.MAX_VALUE);
            option.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 12; -fx-padding: 12 14; -fx-border-color: #334155; -fx-border-radius: 12;");
            VBox.setVgrow(option, Priority.NEVER);
            quizOptionsBox.getChildren().add(option);
        }

        Integer existingAnswer = quizAnswers.get(question.getId());
        if (existingAnswer != null) {
            for (Toggle toggle : group.getToggles()) {
                if (toggle.getUserData() instanceof Integer answerValue && answerValue.equals(existingAnswer)) {
                    group.selectToggle(toggle);
                    break;
                }
            }
        }

        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null && newToggle.getUserData() instanceof Integer answerValue) {
                quizAnswers.put(question.getId(), answerValue);
                updateQuizProgress();
            }
        });

        quizPrevButton.setVisible(currentQuizIndex > 0);
        quizPrevButton.setManaged(currentQuizIndex > 0);

        boolean isLast = currentQuizIndex == quizQuestions.size() - 1;
        quizNextButton.setVisible(!isLast);
        quizNextButton.setManaged(!isLast);
        quizSubmitButton.setVisible(isLast);
        quizSubmitButton.setManaged(isLast);

        updateQuizProgress();
    }

    private void updateQuizProgress() {
        if (quizQuestions.isEmpty()) {
            return;
        }
        double ratio = (double) quizAnswers.size() / quizQuestions.size();
        int percent = (int) Math.round(ratio * 100d);
        quizProgressBar.setProgress(ratio);
        quizProgressPercentLabel.setText(percent + "%");
    }

     @SuppressWarnings({"unused", "all"})
     private boolean isCurrentQuestionAnswered() {
         if (quizQuestions.isEmpty()) {
             return false;
         }
         QuestionStress question = quizQuestions.get(currentQuizIndex);
         return quizAnswers.containsKey(question.getId());
     }

    private void showQuizResults(QuizStress quiz) {
        quizResultScoreLabel.setText(quiz.getTotalScore() + " / " + (quizQuestions.size() * 3));
        quizResultLevelLabel.setText(capitalize(quiz.getStressLevel()));
        quizResultInterpretationLabel.setText(quiz.getInterpretation());

        String emoji;
        String title;
        String subtitle;
        switch (quiz.getStressLevel()) {
            case "minimal" -> {
                emoji = "😊";
                title = "Great Job!";
                subtitle = "Your stress levels are well managed.";
            }
            case "mild" -> {
                emoji = "🙂";
                title = "Good Results";
                subtitle = "Your stress is mild and manageable.";
            }
            case "moderate" -> {
                emoji = "😐";
                title = "Time to Focus";
                subtitle = "Your stress is moderate, take care of yourself.";
            }
            default -> {
                emoji = "😰";
                title = "We Are Here to Help";
                subtitle = "Your stress is high, support is recommended.";
            }
        }

        quizResultEmojiLabel.setText(emoji);
        quizResultTitleLabel.setText(title);
        quizResultSubtitleLabel.setText(subtitle);

        String recLevel = switch (quiz.getStressLevel()) {
            case "minimal", "mild" -> "low";
            case "moderate" -> "medium";
            default -> "high";
        };

        List<RecommendationStress> recommendations = serviceRecommendationStress.findByLevel(recLevel);
        renderRecommendations(recommendations, recLevel);
        showQuizResultsMode();
    }

    private void renderRecommendations(List<RecommendationStress> recommendations, String recLevel) {
        quizRecommendationsBox.getChildren().clear();
        if (recommendations == null || recommendations.isEmpty()) {
            Label empty = new Label("No recommendations found for level: " + recLevel);
            empty.getStyleClass().add("text-small");
            quizRecommendationsBox.getChildren().add(empty);
            return;
        }

        for (RecommendationStress recommendation : recommendations) {
            VBox card = new VBox(6);
            card.getStyleClass().add("wellbeing-input-card");
            card.setPadding(new Insets(14));
            Label title = new Label(recommendation.getTitle());
            title.getStyleClass().add("text-subheading");
            title.setStyle("-fx-font-size: 15px;");
            Label content = new Label(recommendation.getContent());
            content.getStyleClass().add("text-body");
            content.setWrapText(true);
            card.getChildren().addAll(title, content);
            quizRecommendationsBox.getChildren().add(card);
        }
    }

    private void startEdit(WellBeing item) {
        editingItem = item;
        formTitleLabel.setText("Edit Check-in");
        saveButton.setText("Update Check-in");
        cancelEditButton.setVisible(true);
        cancelEditButton.setManaged(true);
        selectedMood = item.getMood();
        updateChoiceSelection(moodButtonsBox, selectedMood);
        stressSlider.setValue(item.getStressLevel());
        energySlider.setValue(item.getEnergyLevel());
        selectedSleepHours = item.getSleepHours();
        updateChoiceSelection(sleepButtonsBox, String.valueOf((int) selectedSleepHours));
        notesArea.setText(item.getNote());
        showCheckinMode();
    }

    private void deleteItem(WellBeing item) {
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null || currentUserId <= 0 || item.getUserId() == null || !item.getUserId().equals(currentUserId)) {
            showError("You can only delete your own check-ins.");
            return;
        }
        try {
            serviceWellBeing.delete(item);
            if (editingItem != null && editingItem.getId() == item.getId()) {
                resetForm();
            }
            loadData();
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    private void resetForm() {
        editingItem = null;
        formTitleLabel.setText("Daily Check-in");
        saveButton.setText("Save Check-in");
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);
        selectedMood = "good";
        updateChoiceSelection(moodButtonsBox, selectedMood);
        stressSlider.setValue(5);
        energySlider.setValue(7);
        selectedSleepHours = 7d;
        updateChoiceSelection(sleepButtonsBox, "7");
        notesArea.clear();
    }

    @FXML
    private void handleMoodSelection(ActionEvent event) {
        Object source = event.getSource();
        if (source instanceof Control control && control.getUserData() != null) {
            selectedMood = String.valueOf(control.getUserData());
            updateChoiceSelection(moodButtonsBox, selectedMood);
        }
    }

    @FXML
    private void handleSleepSelection(ActionEvent event) {
        Object source = event.getSource();
        if (source instanceof Control control && control.getUserData() != null) {
            selectedSleepHours = Double.parseDouble(String.valueOf(control.getUserData()));
            updateChoiceSelection(sleepButtonsBox, String.valueOf((int) selectedSleepHours));
        }
    }

    private Integer getCurrentUserId() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getId() : null;
    }

    private void sortBySelection(List<WellBeing> items, String sortValue) {
        Comparator<WellBeing> comparator = switch (sortValue == null ? "" : sortValue) {
            case "Date (Oldest)" -> Comparator.comparing(WellBeing::getEntryDate);
            case "Stress (High -> Low)" -> Comparator.comparingInt(WellBeing::getStressLevel).reversed();
            case "Stress (Low -> High)" -> Comparator.comparingInt(WellBeing::getStressLevel);
            case "Energy (High -> Low)" -> Comparator.comparingInt(WellBeing::getEnergyLevel).reversed();
            case "Energy (Low -> High)" -> Comparator.comparingInt(WellBeing::getEnergyLevel);
            case "Mood (A-Z)" -> Comparator.comparing(WellBeing::getMood, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(WellBeing::getEntryDate).reversed();
        };
        items.sort(comparator);
    }

     @SuppressWarnings("SameParameterValue")
     private Node createEmptyCard(String titleText, String subtitleText) {
         VBox box = new VBox(8);
         box.getStyleClass().add("wellbeing-empty-card");
         Label title = new Label(titleText);
         title.getStyleClass().add("text-body");
         title.setStyle("-fx-font-weight: 600;");
         Label subtitle = new Label(subtitleText);
         subtitle.getStyleClass().add("text-small");
         subtitle.setWrapText(true);
         box.getChildren().addAll(title, subtitle);
         return box;
     }

    private boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private String formatAverage(double value) {
        return String.format("%.1f", value);
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "-" : value.format(dateFormatter);
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }

    private String emojiForMood(String mood) {
        return switch (mood) {
            case "great" -> "\uD83D\uDE0A";
            case "good" -> "\uD83D\uDE42";
            case "okay" -> "\uD83D\uDE10";
            case "stressed" -> "\uD83D\uDE1F";
            default -> "\uD83D\uDE34";
        };
    }

    private void updateChoiceSelection(HBox container, String selectedValue) {
        if (container == null || selectedValue == null) return;

        if (container != moodButtonsBox) {
            for (Node child : container.getChildren()) {
                if (child instanceof Control control) {
                    control.getStyleClass().remove("selected");
                    Object userData = control.getUserData();
                    if (userData != null && selectedValue.equalsIgnoreCase(String.valueOf(userData))) {
                        if (!control.getStyleClass().contains("selected")) {
                            control.getStyleClass().add("selected");
                        }
                    }
                }
            }
            return;
        }

        for (Node child : container.getChildren()) {
            if (child instanceof Button btn) {
                Object userData = btn.getUserData();
                if (userData == null) continue;

                String btnMood = userData.toString().toLowerCase().trim();

                if (btnMood.equals(selectedValue.toLowerCase().trim())) {
                    // Bouton sélectionné (vert)
                    btn.setStyle("-fx-background-color: #10B981; " +
                            "-fx-background-radius: 18; " +
                            "-fx-border-radius: 18; " +
                            "-fx-border-color: #34D399; " +
                            "-fx-padding: 20 10 16 10; " +
                            "-fx-cursor: hand;");
                } else {
                    // Bouton normal (sombre)
                    btn.setStyle("-fx-background-color: #1E293B; " +
                            "-fx-background-radius: 18; " +
                            "-fx-border-radius: 18; " +
                            "-fx-border-color: transparent; " +
                            "-fx-padding: 20 10 16 10; " +
                            "-fx-cursor: hand;");
                }
            }
        }
    }

    private void showError(String message) {
        showGlobalMessage(message, true);
    }

    private void showGlobalMessage(String message, boolean error) {
        if (globalMessageLabel == null) {
            return;
        }
        globalMessageLabel.setText(message == null ? "" : message);
        globalMessageLabel.setStyle(
                "-fx-background-color: " + (error ? "#dc2626" : "#16a34a") + ";" +
                "-fx-text-fill: white;" +
                "-fx-padding: 10 14 10 14;" +
                "-fx-background-radius: 8;"
        );
        globalMessageLabel.setVisible(true);
        globalMessageLabel.setManaged(true);
    }

    private void hideGlobalMessage() {
        if (globalMessageLabel == null) {
            return;
        }
        globalMessageLabel.setVisible(false);
        globalMessageLabel.setManaged(false);
    }

    private void showOverviewMode() {
        statsSection.setVisible(true);
        statsSection.setManaged(true);
        overviewSection.setVisible(true);
        overviewSection.setManaged(true);
        historySection.setVisible(false);
        historySection.setManaged(false);
        toolsSection.setVisible(true);
        toolsSection.setManaged(true);
        formSection.setVisible(false);
        formSection.setManaged(false);
        quizSection.setVisible(false);
        quizSection.setManaged(false);
        quizResultsSection.setVisible(false);
        quizResultsSection.setManaged(false);
    }

    private void showCheckinMode() {
        statsSection.setVisible(false);
        statsSection.setManaged(false);
        overviewSection.setVisible(false);
        overviewSection.setManaged(false);
        historySection.setVisible(false);
        historySection.setManaged(false);
        toolsSection.setVisible(false);
        toolsSection.setManaged(false);
        formSection.setVisible(true);
        formSection.setManaged(true);
        quizSection.setVisible(false);
        quizSection.setManaged(false);
        quizResultsSection.setVisible(false);
        quizResultsSection.setManaged(false);
    }

    private void showHistoryMode() {
        statsSection.setVisible(false);
        statsSection.setManaged(false);
        overviewSection.setVisible(false);
        overviewSection.setManaged(false);
        toolsSection.setVisible(false);
        toolsSection.setManaged(false);
        formSection.setVisible(false);
        formSection.setManaged(false);
        historySection.setVisible(true);
        historySection.setManaged(true);
        quizSection.setVisible(false);
        quizSection.setManaged(false);
        quizResultsSection.setVisible(false);
        quizResultsSection.setManaged(false);
    }

    private void showQuizMode() {
        statsSection.setVisible(false);
        statsSection.setManaged(false);
        overviewSection.setVisible(false);
        overviewSection.setManaged(false);
        historySection.setVisible(false);
        historySection.setManaged(false);
        toolsSection.setVisible(false);
        toolsSection.setManaged(false);
        formSection.setVisible(false);
        formSection.setManaged(false);
        quizResultsSection.setVisible(false);
        quizResultsSection.setManaged(false);
        quizSection.setVisible(true);
        quizSection.setManaged(true);
    }

    private void showQuizResultsMode() {
        statsSection.setVisible(false);
        statsSection.setManaged(false);
        overviewSection.setVisible(false);
        overviewSection.setManaged(false);
        historySection.setVisible(false);
        historySection.setManaged(false);
        toolsSection.setVisible(false);
        toolsSection.setManaged(false);
        formSection.setVisible(false);
        formSection.setManaged(false);
        quizSection.setVisible(false);
        quizSection.setManaged(false);
        quizResultsSection.setVisible(true);
        quizResultsSection.setManaged(true);
    }

    private void updateStatsLabels(List<WellBeing> items, Label total, Label stress, Label energy, Label sleep) {
        if (total == null || stress == null || energy == null || sleep == null) {
            return;
        }

        total.setText(String.valueOf(items.size()));
        stress.setText(formatAverage(items.stream().mapToInt(WellBeing::getStressLevel).average().orElse(0)) + "/10");
        energy.setText(formatAverage(items.stream().mapToInt(WellBeing::getEnergyLevel).average().orElse(0)) + "/10");
        sleep.setText(formatAverage(items.stream().mapToDouble(WellBeing::getSleepHours).average().orElse(0)) + "h");
    }

    private void updateHeroSummary(List<WellBeing> items) {
        if (moodLabel == null || sleepLabel == null || stressLabel == null || energyLabel == null) {
            return;
        }

        if (items.isEmpty()) {
            moodLabel.setText("Good");
            sleepLabel.setText("7.5h");
            stressLabel.setText("Low");
            energyLabel.setText("High");
            return;
        }

        WellBeing latest = items.get(0);
        moodLabel.setText(capitalize(latest.getMood()));
        sleepLabel.setText(latest.getFormattedSleep());
        stressLabel.setText(levelLabel(latest.getStressLevel(), true));
        energyLabel.setText(levelLabel(latest.getEnergyLevel(), false));
    }

    private String levelLabel(int value, boolean inverse) {
        if (inverse) {
            if (value <= 3) {
                return "Low";
            }
            if (value <= 6) {
                return "Medium";
            }
            return "High";
        }

        if (value <= 3) {
            return "Low";
        }
        if (value <= 6) {
            return "Medium";
        }
        return "High";
    }

    private void loadMoodWeek(List<WellBeing> items) {
        if (moodWeekBox == null) {
            return;
        }

        moodWeekBox.getChildren().clear();
        List<WellBeing> recent = new ArrayList<>(items.stream().limit(7).toList());
        recent.sort(Comparator.comparing(WellBeing::getEntryDate));

        List<MoodEntry> moods;
        if (recent.isEmpty()) {
            moods = List.of(
                    new MoodEntry("Mon", "Great", "fth-smile", "success"),
                    new MoodEntry("Tue", "Good", "fth-smile", "success"),
                    new MoodEntry("Wed", "Okay", "fth-meh", "warning"),
                    new MoodEntry("Thu", "Stressed", "fth-frown", "danger"),
                    new MoodEntry("Fri", "Good", "fth-smile", "success"),
                    new MoodEntry("Sat", "Great", "fth-smile", "success"),
                    new MoodEntry("Sun", "Good", "😊", "success")
            );
        } else {
            moods = recent.stream()
                    .map(item -> new MoodEntry(
                            item.getEntryDate().format(DateTimeFormatter.ofPattern("EEE")),
                            capitalize(item.getMood()),
                            iconForMood(item.getMood()),
                            colorForMood(item.getMood())
                    ))
                    .toList();
        }

        HBox moodRow = new HBox(12);
        moodRow.setAlignment(Pos.CENTER_LEFT);

        for (MoodEntry mood : moods) {
            VBox dayBox = createMoodDayBox(mood);
            HBox.setHgrow(dayBox, Priority.ALWAYS);
            moodRow.getChildren().add(dayBox);
        }

        moodWeekBox.getChildren().add(moodRow);
    }

    private VBox createMoodDayBox(MoodEntry mood) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(12));
        box.getStyleClass().add("wellbeing-day-box");

        Label dayText = new Label(mood.day());
        dayText.getStyleClass().add("wellbeing-day-label");

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(40, 40);
        iconBox.setStyle("-fx-background-color: " + getColorWithAlpha(mood.color()) + "; -fx-background-radius: 100;");

        FontIcon icon = new FontIcon(mood.icon());
        icon.setIconSize(20);
        icon.setIconColor(Color.web(getColorHex(mood.color())));
        iconBox.getChildren().add(icon);

        Label moodText = new Label(mood.mood());
        moodText.getStyleClass().add("wellbeing-day-mood");

        box.getChildren().addAll(dayText, iconBox, moodText);
        return box;
    }

    private void loadHabits() {
        if (habitsBox == null) {
            return;
        }

        habitsBox.getChildren().clear();
        List<HabitData> habits = List.of(
                new HabitData("Morning Meditation", "fth-sun", "warning", new boolean[]{true, true, false, true, true, true, false}),
                new HabitData("Exercise", "fth-activity", "success", new boolean[]{true, false, true, false, true, true, false}),
                new HabitData("Read 30 mins", "fth-book", "primary", new boolean[]{true, true, true, true, true, false, true}),
                new HabitData("No Social Media", "fth-smartphone", "danger", new boolean[]{false, true, false, true, true, false, false}),
                new HabitData("8 Hours Sleep", "fth-moon", "accent", new boolean[]{true, true, true, false, true, true, true}),
                new HabitData("Drink Water", "fth-droplet", "primary", new boolean[]{true, true, true, true, true, true, true})
        );

        for (HabitData habit : habits) {
            habitsBox.getChildren().add(createHabitItem(habit));
        }
    }

    private HBox createHabitItem(HabitData habit) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(14, 16, 14, 16));
        item.getStyleClass().add("wellbeing-habit-item");

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(32, 32);
        iconBox.setStyle("-fx-background-color: " + getColorWithAlpha(habit.color()) + "; -fx-background-radius: 8;");
        FontIcon icon = new FontIcon(habit.icon());
        icon.setIconSize(14);
        icon.setIconColor(Color.web(getColorHex(habit.color())));
        iconBox.getChildren().add(icon);

        Label nameLabel = new Label(habit.name());
        nameLabel.getStyleClass().add("text-body");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        HBox dotsBox = new HBox(6);
        dotsBox.setAlignment(Pos.CENTER_RIGHT);
        String[] days = {"M", "T", "W", "T", "F", "S", "S"};
        for (int i = 0; i < 7; i++) {
            VBox dayDot = new VBox(2);
            dayDot.setAlignment(Pos.CENTER);

            Label day = new Label(days[i]);
            day.getStyleClass().add("wellbeing-habit-day");

            Region dot = new Region();
            dot.setPrefSize(18, 18);
            dot.setStyle("-fx-background-color: " +
                    (habit.weekProgress()[i] ? getColorHex(habit.color()) : "#334155") +
                    "; -fx-background-radius: 100;");

            dayDot.getChildren().addAll(day, dot);
            dotsBox.getChildren().add(dayDot);
        }

        long completed = 0;
        for (boolean value : habit.weekProgress()) {
            if (value) {
                completed++;
            }
        }
        int percentage = (int) ((completed / 7.0) * 100);

        Label percentLabel = new Label(percentage + "%");
        percentLabel.setStyle("-fx-text-fill: " + getColorHex(habit.color()) + "; -fx-font-size: 12px; -fx-font-weight: 600;");
        percentLabel.setMinWidth(40);
        percentLabel.setAlignment(Pos.CENTER_RIGHT);

        item.getChildren().addAll(iconBox, nameLabel, dotsBox, percentLabel);
        return item;
    }

    private void loadMindfulnessSessions() {
        if (mindfulnessBox == null) {
            return;
        }

        mindfulnessBox.getChildren().clear();
        List<MindfulnessSession> sessions = List.of(
                new MindfulnessSession("Deep Breathing", "5 min", "fth-wind", "primary"),
                new MindfulnessSession("Body Scan", "10 min", "fth-user", "success"),
                new MindfulnessSession("Focus Meditation", "15 min", "fth-target", "warning"),
                new MindfulnessSession("Sleep Meditation", "20 min", "fth-moon", "accent")
        );

        for (MindfulnessSession session : sessions) {
            mindfulnessBox.getChildren().add(createMindfulnessItem(session));
        }
    }

    private HBox createMindfulnessItem(MindfulnessSession session) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12));
        item.getStyleClass().add("wellbeing-session-item");

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(40, 40);
        iconBox.setStyle("-fx-background-color: " + getColorWithAlpha(session.color()) + "; -fx-background-radius: 10;");

        FontIcon icon = new FontIcon(session.icon());
        icon.setIconSize(18);
        icon.setIconColor(Color.web(getColorHex(session.color())));
        iconBox.getChildren().add(icon);

        VBox content = new VBox(2);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label title = new Label(session.title());
        title.setStyle("-fx-text-fill: #F8FAFC; -fx-font-size: 13px; -fx-font-weight: 500;");

        Label duration = new Label(session.duration());
        duration.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        content.getChildren().addAll(title, duration);

        StackPane playBtn = new StackPane();
        playBtn.setPrefSize(32, 32);
        playBtn.setStyle("-fx-background-color: " + getColorHex(session.color()) + "; -fx-background-radius: 100;");
        FontIcon playIcon = new FontIcon("fth-play");
        playIcon.setIconSize(14);
        playIcon.setIconColor(Color.WHITE);
        playBtn.getChildren().add(playIcon);

        item.getChildren().addAll(iconBox, content, playBtn);
        return item;
    }

    private void loadSleepLog(List<WellBeing> items) {
        if (sleepLogBox == null) {
            return;
        }

        sleepLogBox.getChildren().clear();
        List<WellBeing> recent = new ArrayList<>(items.stream().limit(7).toList());

        List<SleepEntry> sleepData;
        if (recent.isEmpty()) {
            sleepData = List.of(
                    new SleepEntry("Today", 7.5, "good"),
                    new SleepEntry("Yesterday", 6.0, "fair"),
                    new SleepEntry("Mon", 8.0, "great"),
                    new SleepEntry("Sun", 7.0, "good"),
                    new SleepEntry("Sat", 9.0, "great"),
                    new SleepEntry("Fri", 5.5, "poor"),
                    new SleepEntry("Thu", 7.5, "good")
            );
        } else {
            sleepData = recent.stream()
                    .map(item -> new SleepEntry(
                            item.getEntryDate().format(DateTimeFormatter.ofPattern("EEE")),
                            item.getSleepHours(),
                            sleepQuality(item.getSleepHours())
                    ))
                    .toList();
        }

        for (SleepEntry entry : sleepData) {
            sleepLogBox.getChildren().add(createSleepItem(entry));
        }
    }

    private HBox createSleepItem(SleepEntry entry) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 0, 10, 0));
        item.getStyleClass().add("wellbeing-sleep-item");

        Label dayLabel = new Label(entry.day());
        dayLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        dayLabel.setMinWidth(70);

        VBox barBox = new VBox(4);
        HBox.setHgrow(barBox, Priority.ALWAYS);

        ProgressBar bar = new ProgressBar(entry.hours() / 10.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(8);
        String barColor = switch (entry.quality()) {
            case "great" -> "success";
            case "good" -> "primary";
            case "fair" -> "warning";
            default -> "danger";
        };
        bar.getStyleClass().add(barColor);
        barBox.getChildren().add(bar);

        Label hoursLabel = new Label(String.format("%.1fh", entry.hours()));
        hoursLabel.setStyle("-fx-text-fill: #F8FAFC; -fx-font-size: 13px; -fx-font-weight: 600;");
        hoursLabel.setMinWidth(40);
        hoursLabel.setAlignment(Pos.CENTER_RIGHT);

        String qualityColor = switch (entry.quality()) {
            case "great" -> "#10B981";
            case "good" -> "#8B5CF6";
            case "fair" -> "#F59E0B";
            default -> "#F43F5E";
        };

        Label qualityLabel = new Label(capitalize(entry.quality()));
        qualityLabel.setStyle("-fx-background-color: " + qualityColor + "33; -fx-text-fill: " + qualityColor
                + "; -fx-padding: 2 8; -fx-background-radius: 100; -fx-font-size: 10px; -fx-font-weight: 600;");

        item.getChildren().addAll(dayLabel, barBox, hoursLabel, qualityLabel);
        return item;
    }

    private String sleepQuality(double hours) {
        if (hours >= 8) {
            return "great";
        }
        if (hours >= 7) {
            return "good";
        }
        if (hours >= 6) {
            return "fair";
        }
        return "poor";
    }

    private String iconForMood(String mood) {
        return switch (mood) {
            case "great", "good" -> "fth-smile";
            case "okay" -> "fth-meh";
            case "stressed" -> "fth-frown";
            default -> "fth-moon";
        };
    }

    private String colorForMood(String mood) {
        return switch (mood) {
            case "great", "good" -> "success";
            case "okay" -> "warning";
            case "stressed" -> "danger";
            default -> "primary";
        };
    }

    private String getColorHex(String color) {
        return switch (color) {
            case "primary" -> "#A78BFA";
            case "success" -> "#34D399";
            case "warning" -> "#FBBF24";
            case "danger" -> "#FB7185";
            case "accent" -> "#FB923C";
            default -> "#94A3B8";
        };
    }

    private String getColorWithAlpha(String color) {
        return switch (color) {
            case "primary" -> "rgba(139, 92, 246, 0.2)";
            case "success" -> "rgba(16, 185, 129, 0.2)";
            case "warning" -> "rgba(245, 158, 11, 0.2)";
            case "danger" -> "rgba(244, 63, 94, 0.2)";
            case "accent" -> "rgba(249, 115, 22, 0.2)";
            default -> "rgba(148, 163, 184, 0.2)";
        };
    }

    private String moodColor(String mood) {
        return switch (mood == null ? "" : mood.toLowerCase()) {
            case "great" -> "#22C55E";
            case "good" -> "#3B82F6";
            case "okay" -> "#F59E0B";
            case "stressed" -> "#EF4444";
            default -> "#A78BFA";
        };
    }

    private String stressColor(int stressLevel) {
        if (stressLevel <= 4) {
            return "#22C55E";
        }
        if (stressLevel <= 6) {
            return "#F59E0B";
        }
        return "#EF4444";
    }
}
