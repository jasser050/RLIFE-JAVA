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
import com.studyflow.services.SpeechToTextService;
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
import javafx.scene.control.CheckBox;
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
import javafx.scene.control.ToggleButton;
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
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.prefs.Preferences;

public class WellbeingController implements Initializable {
    private enum QuizMode {
        SIMPLE,
        AI
    }

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
    @FXML private FlowPane copingToolsPreviewPane;
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
    @FXML private VBox inlineToolSection;
    @FXML private Label inlineToolTitleLabel;
    @FXML private StackPane inlineToolHost;
    @FXML private VBox quizModeSection;
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
    @FXML private Button quizLoadAiSuggestionsButton;
    @FXML private Label quizAiSuggestionsMessageLabel;
    @FXML private VBox quizAiSuggestionsBox;
    @FXML private VBox quizSelectedRecommendationBox;
    @FXML private Label quizSelectedRecommendationTitleLabel;
    @FXML private Label quizSelectedRecommendationContentLabel;
    @FXML private Label globalMessageLabel;
    @FXML private Label notesErrorLabel;
    @FXML private ComboBox<String> quoteTypeCombo;
    @FXML private CheckBox quoteEnabledToggle;
    private final ServiceWellBeing serviceWellBeing = new ServiceWellBeing();
    private final ServiceCopingSession serviceCopingSession = new ServiceCopingSession();
    private final ServiceWellbeingJournalEntry serviceJournalEntry = new ServiceWellbeingJournalEntry();
    private final ServiceQuestionStress serviceQuestionStress = new ServiceQuestionStress();
    private final ServiceQuizStress serviceQuizStress = new ServiceQuizStress();
    private final ServiceRecommendationStress serviceRecommendationStress = new ServiceRecommendationStress();
    private final SpeechToTextService speechToTextService = new SpeechToTextService();
    private final WellbeingAiService wellbeingAiService = new WellbeingAiService();
    private final ObservableList<WellBeing> allCheckins = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private WellBeing editingItem;
    private String selectedMood = "good";
    private double selectedSleepHours = 7d;
    private final List<QuestionStress> quizQuestions = new ArrayList<>();
    private final Map<Integer, Integer> quizAnswers = new HashMap<>();
    private final Set<Integer> aiUsedQuestionIds = new HashSet<>();
    private int currentQuizIndex = 0;
    private QuizStress currentQuizResult;
    private String lastAiSuggestionFingerprint = "";
    private QuizMode currentQuizMode = QuizMode.SIMPLE;
    private static final int QUIZ_AI_QUESTION_COUNT = 10;
    private static final int MIN_NOTE_LENGTH = 6;
    private static final int MAX_NOTE_LENGTH = 1000;
    private static final int GLOBAL_MESSAGE_HIDE_SECONDS = 5;
    private static final double VOICE_SILENCE_AVG_THRESHOLD = 85.0;
    private static final int VOICE_SILENCE_PEAK_THRESHOLD = 650;
    private static final String PREF_QUOTE_TYPE = "global.quote.type";
    private static final String PREF_QUOTE_ENABLED = "global.quote.enabled";
    private static final String PREF_QUOTE_DISMISSED_UNTIL = "global.quote.dismissed.until";
    private static final String PREF_QUOTE_POS_X = "global.quote.position.x";
    private static final String PREF_QUOTE_POS_Y = "global.quote.position.y";
    private static final int MOOD_EMOJI_SIZE = 56;
    private static final int MOOD_EMOJI_FALLBACK_FONT_SIZE = 50;
    private final Preferences preferences = Preferences.userNodeForPackage(MainController.class);
    private Runnable activeInlineToolCloser;
    private Timeline globalMessageTimer;
    private String journalAutoCandidateCode = "";
    private int journalAutoCandidateHits = 0;
    private record CopingToolDef(String key, String title, String durationLabel, int durationSeconds, String description) {}
    private record MoodEntry(String day, String mood, String icon, String color) {}
    private record HabitData(String name, String icon, String color, boolean[] weekProgress) {}
    private record MindfulnessSession(String title, String duration, String icon, String color) {}
    private record SleepEntry(String day, double hours, String quality) {}
    private record AiSuggestionItem(String title, String content) {}

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        hideGlobalMessage();
        try {
            setupFilters();
            setupForm();
            setupQuoteControls();
            loadData();
            showOverviewMode();
        } catch (Exception e) {
            allCheckins.clear();
            showOverviewMode();
            showError("Wellbeing data could not be loaded right now. Check the database connection and try again.");
            e.printStackTrace();
        }
    }

    private void setupQuoteControls() {
        if (quoteTypeCombo == null || quoteEnabledToggle == null) {
            return;
        }
        quoteTypeCombo.setItems(FXCollections.observableArrayList("Motivation", "Funny", "Calm", "Focus"));
        String storedType = preferences.get(PREF_QUOTE_TYPE, "motivation").trim().toLowerCase(Locale.ROOT);
        String selected = switch (storedType) {
            case "funny" -> "Funny";
            case "calm" -> "Calm";
            case "focus" -> "Focus";
            default -> "Motivation";
        };
        quoteTypeCombo.setValue(selected);
        quoteEnabledToggle.setSelected(preferences.getBoolean(PREF_QUOTE_ENABLED, true));
    }

    @FXML
    private void handleApplyQuoteSettings() {
        if (quoteTypeCombo == null || quoteEnabledToggle == null) {
            return;
        }
        String type = quoteTypeCombo.getValue() == null ? "motivation" : quoteTypeCombo.getValue().trim().toLowerCase(Locale.ROOT);
        if (!List.of("motivation", "funny", "calm", "focus").contains(type)) {
            type = "motivation";
        }
        preferences.put(PREF_QUOTE_TYPE, type);
        preferences.putBoolean(PREF_QUOTE_ENABLED, quoteEnabledToggle.isSelected());
        preferences.putLong(PREF_QUOTE_DISMISSED_UNTIL, 0L);
        showGlobalMessage("Quote settings updated.", false);
    }

    @FXML
    private void handleResetQuotePosition() {
        preferences.remove(PREF_QUOTE_POS_X);
        preferences.remove(PREF_QUOTE_POS_Y);
        preferences.putLong(PREF_QUOTE_DISMISSED_UNTIL, 0L);
        showGlobalMessage("Quote position reset.", false);
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
        if (stressSlider == null || energySlider == null || stressValueLabel == null || energyValueLabel == null) {
            return;
        }
        stressSlider.valueProperty().addListener((obs, oldVal, newVal) -> stressValueLabel.setText(String.valueOf(newVal.intValue())));
        energySlider.valueProperty().addListener((obs, oldVal, newVal) -> energyValueLabel.setText(String.valueOf(newVal.intValue())));
        stressValueLabel.setText(String.valueOf((int) stressSlider.getValue()));
        energyValueLabel.setText(String.valueOf((int) energySlider.getValue()));
        configureMoodButtons();
        if (cancelEditButton != null) {
            cancelEditButton.setVisible(false);
            cancelEditButton.setManaged(false);
        }
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
            refreshOverview();
            refreshHistoryTable();
            showError("Please log in with a valid account to access your wellbeing data.");
            return;
        }
        try {
            allCheckins.setAll(serviceWellBeing.findAllForUser(currentUserId));
            refreshOverview();
            refreshHistoryTable();
        } catch (RuntimeException e) {
            allCheckins.clear();
            refreshOverview();
            refreshHistoryTable();
            String message = e.getMessage() == null ? "Unable to load wellbeing data." : e.getMessage();
            showError(message);
        }
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
        populateCopingToolsPreview();
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
            String moodKey = item.getMood() == null ? "okay" : item.getMood().toLowerCase().trim();
            String emojiUnicode = EmojiUtils.getMoodEmojiUnicode(moodKey);
            ImageView emojiImage = EmojiUtils.loadMoodEmojiImage(moodKey, MOOD_EMOJI_SIZE);
            if (emojiImage != null) {
                moodBox.getChildren().add(emojiImage);
            } else {
                Label emojiFallback = new Label(emojiUnicode);
                emojiFallback.getStyleClass().add("wellbeing-list-emoji");
                emojiFallback.setStyle("-fx-font-size: " + MOOD_EMOJI_FALLBACK_FONT_SIZE + "px; -fx-font-family: 'Segoe UI Emoji';");
                moodBox.getChildren().add(emojiFallback);
            }

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
            VBox card = new VBox(10);
            card.getStyleClass().add("wellbeing-tool-card");
            card.setPrefWidth(410);
            card.setMinWidth(410);

            StackPane iconBox = new StackPane();
            iconBox.getStyleClass().addAll("wellbeing-tool-icon", "tool-icon-" + tool.key());
            FontIcon icon = new FontIcon(toolIconLiteral(tool.key()));
            icon.setIconSize(18);
            iconBox.getChildren().add(icon);

            Label title = new Label(tool.title());
            title.getStyleClass().add("tools-card-title");

            Label desc = new Label(tool.description());
            desc.getStyleClass().add("tools-card-desc");
            desc.setWrapText(true);

            Label duration = new Label(tool.durationLabel());
            duration.getStyleClass().addAll("tools-card-badge", toolBadgeClass(tool.durationLabel()));

            Button startBtn = new Button("Start");
            startBtn.getStyleClass().addAll("btn-primary", "tools-start-btn");
            startBtn.setOnAction(event -> openCopingTool(tool));

            HBox contentTop = new HBox(12, iconBox, new VBox(4, title, desc));
            HBox.setHgrow(contentTop.getChildren().get(1), Priority.ALWAYS);

            HBox footer = new HBox(10, duration, new Region(), startBtn);
            HBox.setHgrow(footer.getChildren().get(1), Priority.ALWAYS);

            card.getChildren().addAll(contentTop, footer);
            copingToolsPane.getChildren().add(card);
        }
    }

    private void populateCopingToolsPreview() {
        if (copingToolsPreviewPane == null) {
            return;
        }
        copingToolsPreviewPane.getChildren().clear();
        for (CopingToolDef tool : copingToolsCatalog()) {
            HBox card = new HBox(12);
            card.getStyleClass().add("wellbeing-tool-preview-card");
            card.setPrefWidth(495);
            card.setMinWidth(495);

            StackPane iconBox = new StackPane();
            iconBox.getStyleClass().addAll("wellbeing-tool-icon", "tool-icon-" + tool.key());
            FontIcon icon = new FontIcon(toolIconLiteral(tool.key()));
            icon.setIconSize(18);
            iconBox.getChildren().add(icon);

            Label title = new Label(tool.title());
            title.getStyleClass().add("tools-card-title");
            Label desc = new Label(tool.description());
            desc.getStyleClass().add("tools-card-desc");
            desc.setWrapText(true);
            Label duration = new Label(tool.durationLabel());
            duration.getStyleClass().addAll("tools-card-badge", toolBadgeClass(tool.durationLabel()));

            VBox textBox = new VBox(5, title, desc, duration);
            textBox.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(textBox, Priority.ALWAYS);
            card.getChildren().addAll(iconBox, textBox);
            copingToolsPreviewPane.getChildren().add(card);
        }
    }

    private String toolIconLiteral(String key) {
        return switch (key) {
            case "breathing_exercise" -> "fth-activity";
            case "gratitude_journal" -> "fth-book-open";
            case "nature_sounds", "yoga_coach" -> "fth-moon";
            default -> "fth-cpu";
        };
    }

    private String toolBadgeClass(String durationLabel) {
        String value = durationLabel == null ? "" : durationLabel.trim().toLowerCase(Locale.ROOT);
        if (value.contains("ongoing")) {
            return "tools-card-badge-ongoing";
        }
        if (value.contains("anytime")) {
            return "tools-card-badge-anytime";
        }
        return "tools-card-badge-time";
    }

    private List<CopingToolDef> copingToolsCatalog() {
        return List.of(
                new CopingToolDef("breathing_exercise", "Breathing Exercise", "3 min", 180, "4-7-8 breathing technique for quick relaxation"),
                new CopingToolDef("gratitude_journal", "Gratitude Journal", "2 min", 120, "Write three things you're grateful for"),
                new CopingToolDef("nature_sounds", "Nature Sounds", "Ongoing", 300, "Relaxing ambient sounds for focus"),
                new CopingToolDef("yoga_coach", "Yoga Coach", "6 min", 360, "Stress-relief yoga with dynamic demo"),
                new CopingToolDef("ai_chat_coach", "AI Chat Assistant", "Anytime", 300, "Talk with AI for general questions, study help, and wellbeing")
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
            case "nature_sounds" -> openNatureSoundsSpotifyTool(session);
            default -> openBreathingTool(session);
        }
    }

    private Stage createToolStage(String title, Pane body, int width, int height) {
        Stage stage = new Stage();
        stage.initModality(Modality.NONE);
        stage.setTitle(title);
        body.setPadding(new Insets(18));
        body.setOpacity(12);
        if (body.getStyle() == null || body.getStyle().isBlank()) {
            body.setStyle("-fx-background-color: #0F172A;");
        }
        Scene scene = new Scene(body, width, height);
        stage.setScene(scene);
        return stage;
    }

    private void showInlineTool(String title, Node content, Runnable closer) {
        if (inlineToolSection == null || inlineToolHost == null || inlineToolTitleLabel == null) {
            return;
        }
        activeInlineToolCloser = closer;
        inlineToolTitleLabel.setText(title);
        inlineToolHost.getChildren().setAll(content);
        inlineToolSection.setVisible(true);
        inlineToolSection.setManaged(true);
        Platform.runLater(() -> {
            if (inlineToolSection.getScene() != null) {
                Node p = inlineToolSection.getParent();
                while (p != null && !(p instanceof ScrollPane)) {
                    p = p.getParent();
                }
                if (p instanceof ScrollPane scrollPane) {
                    scrollPane.layout();
                    scrollPane.setVvalue(1.0);
                }
            }
        });
    }

    @FXML
    private void handleCloseInlineTool() {
        if (activeInlineToolCloser != null) {
            activeInlineToolCloser.run();
            activeInlineToolCloser = null;
        }
        if (inlineToolHost != null) {
            inlineToolHost.getChildren().clear();
        }
        if (inlineToolSection != null) {
            inlineToolSection.setVisible(false);
            inlineToolSection.setManaged(false);
        }
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
        AtomicBoolean sessionClosed = new AtomicBoolean(false);
        Runnable closer = () -> {
            if (sessionClosed.getAndSet(true)) {
                return;
            }
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
        AtomicBoolean sessionClosed = new AtomicBoolean(false);
        Runnable closer = () -> {
            if (sessionClosed.getAndSet(true)) {
                return;
            }
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
        VBox root = new VBox(16);
        root.setStyle("-fx-background-color: transparent;");

        VBox topBanner = new VBox(6);
        topBanner.setPadding(new Insets(16));
        topBanner.setStyle(
                "-fx-background-color: linear-gradient(to right, #111827, #1E293B);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #334155;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 16;"
        );
        Label focusLabel = new Label("FOCUS SESSION");
        focusLabel.setStyle("-fx-text-fill: #A78BFA; -fx-font-size: 11px; -fx-font-weight: 700;");
        Label titleLabel = new Label("Gratitude Journal");
        titleLabel.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 38px; -fx-font-weight: 800;");
        Label subtitleLabel = new Label("Write three things you're grateful for");
        subtitleLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");
        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_RIGHT);
        Label durationBadge = new Label("2 min");
        durationBadge.setStyle("-fx-background-color: rgba(167,139,250,0.2); -fx-text-fill: #C4B5FD; -fx-padding: 4 10; -fx-background-radius: 10; -fx-font-weight: 700;");
        Label readyBadge = new Label("Ready");
        readyBadge.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #E2E8F0; -fx-padding: 4 10; -fx-background-radius: 10; -fx-font-weight: 700;");
        badges.getChildren().addAll(durationBadge, readyBadge);
        HBox bannerTitleRow = new HBox(10, new VBox(2, focusLabel, titleLabel, subtitleLabel), new Region(), badges);
        HBox.setHgrow(bannerTitleRow.getChildren().get(1), Priority.ALWAYS);
        topBanner.getChildren().add(bannerTitleRow);

        VBox composerCard = new VBox(12);
        composerCard.setPadding(new Insets(20));
        composerCard.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #0F172A, #111827);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #334155;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 16;"
        );
        Label sectionTitle = new Label("Gratitude Journal");
        sectionTitle.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 34px; -fx-font-weight: 800;");
        Label sectionDesc = new Label("Write what you are grateful for. You can also dictate by voice.");
        sectionDesc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        TextArea input = new TextArea();
        input.setPromptText("Example: 1) I am grateful for...");
        input.setWrapText(true);
        input.setPrefRowCount(7);
        input.setStyle(
                "-fx-control-inner-background: #1E293B;" +
                        "-fx-text-fill: #f8fafc;" +
                        "-fx-prompt-text-fill: #94a3b8;" +
                        "-fx-border-color: #475569;" +
                        "-fx-border-width: 1.8;" +
                        "-fx-border-radius: 14;" +
                        "-fx-background-radius: 14;"
        );

        ComboBox<String> languageBox = new ComboBox<>(FXCollections.observableArrayList(
                "Auto (AI detect)", "English", "Francais", "Arabic", "Tunisian Arabic"
        ));
        languageBox.setValue("Auto (AI detect)");
        languageBox.setStyle(
                "-fx-background-color: #1E293B;" +
                        "-fx-text-fill: #e2e8f0;" +
                        "-fx-border-color: #475569;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;"
        );

        Label statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: #A78BFA; -fx-font-size: 12px; -fx-font-weight: 700;");
        Label voiceStatusLabel = new Label("Voice: idle");
        voiceStatusLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: 600;");

        Button startVoiceBtn = new Button("Start Voice Input");
        startVoiceBtn.getStyleClass().add("btn-secondary");
        startVoiceBtn.setStyle(
                "-fx-background-color: #334155;" +
                        "-fx-text-fill: #eff6ff;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 9 16;" +
                        "-fx-font-weight: 700;"
        );
        Button stopVoiceBtn = new Button("Stop");
        stopVoiceBtn.getStyleClass().add("btn-danger");
        stopVoiceBtn.setStyle(
                "-fx-background-color: #dc2626;" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 9 16;" +
                        "-fx-font-weight: 700;"
        );
        stopVoiceBtn.setDisable(true);

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setStyle(
                "-fx-background-color: #7C3AED;" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 9 18;" +
                        "-fx-font-weight: 700;"
        );
        Button newBtn = new Button("New Journal");
        newBtn.getStyleClass().add("btn-secondary");
        newBtn.setStyle(
                "-fx-background-color: #1E293B;" +
                        "-fx-text-fill: #e2e8f0;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 9 16;" +
                        "-fx-font-weight: 700;"
        );
        Button closeBtn = new Button("Back to Coping Tools");
        closeBtn.getStyleClass().add("btn-secondary");
        closeBtn.setStyle(
                "-fx-background-color: #334155;" +
                        "-fx-text-fill: #f1f5f9;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 9 16;" +
                        "-fx-font-weight: 700;"
        );

        HBox voiceRow = new HBox(10, startVoiceBtn, stopVoiceBtn, new Label("Language"), languageBox, voiceStatusLabel);
        voiceRow.setAlignment(Pos.CENTER_LEFT);
        HBox actionRow = new HBox(10, saveBtn, newBtn, closeBtn);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        composerCard.getChildren().addAll(sectionTitle, sectionDesc, input, voiceRow, actionRow, statusLabel);

        VBox entriesCard = new VBox(12);
        entriesCard.setPadding(new Insets(20));
        entriesCard.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #0F172A, #111827);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #334155;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 16;"
        );
        Label entriesTitle = new Label("Recent Journal Entries");
        entriesTitle.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 30px; -fx-font-weight: 800;");
        VBox entriesBox = new VBox(10);
        ScrollPane entriesPane = new ScrollPane(entriesBox);
        entriesPane.setFitToWidth(true);
        entriesPane.setPrefHeight(240);
        entriesPane.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;"
        );
        entriesCard.getChildren().addAll(entriesTitle, entriesPane);

        int[] editingId = {0};
        boolean[] completed = {false};
        boolean[] usedVoiceForEntry = {false};
        AtomicBoolean voiceRunning = new AtomicBoolean(false);
        Thread[] voiceThread = new Thread[1];
        AtomicReference<String> selectedVoiceLanguage = new AtomicReference<>("auto");
        AtomicReference<String> autoDetectedVoiceLanguage = new AtomicReference<>("");
        selectedVoiceLanguage.set(mapJournalLanguageSelection(languageBox.getValue()));
        languageBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            selectedVoiceLanguage.set(mapJournalLanguageSelection(newValue));
            if (!"Auto (AI detect)".equals(newValue)) {
                autoDetectedVoiceLanguage.set("");
            }
        });

        Runnable[] reload = new Runnable[1];
        reload[0] = () -> {
            List<WellbeingJournalEntry> entries = serviceJournalEntry.findByUser(userId, 100);
            entriesBox.getChildren().clear();
            if (entries.isEmpty()) {
                Label emptyLabel = new Label("No journal entries yet.");
                emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
                entriesBox.getChildren().add(emptyLabel);
                return;
            }

            for (WellbeingJournalEntry entry : entries) {
                VBox card = new VBox(8);
                card.setPadding(new Insets(14));
                card.setStyle(
                        "-fx-background-color: rgba(30, 41, 59, 0.75);" +
                                "-fx-border-color: #334155;" +
                                "-fx-border-width: 1;" +
                                "-fx-border-radius: 12;" +
                                "-fx-background-radius: 12;"
                );

                String dateText = entry.getCreatedAt() == null
                        ? "-"
                        : entry.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                Label dateLabel = new Label(dateText);
                dateLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px; -fx-font-weight: 700;");

                Label contentLabel = new Label(entry.getContent() == null ? "" : entry.getContent());
                contentLabel.setWrapText(true);
                contentLabel.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 20px; -fx-font-weight: 600;");

                String meta = "Mode: " + (entry.getInputMode() == null ? "text" : entry.getInputMode())
                        + "   |   Language: " + (entry.getLanguageCode() == null || entry.getLanguageCode().isBlank() ? "auto" : entry.getLanguageCode());
                Label metaLabel = new Label(meta);
                metaLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");

                Button editBtn = new Button("Edit");
                editBtn.getStyleClass().add("btn-secondary");
                editBtn.setStyle(
                        "-fx-background-color: #334155;" +
                                "-fx-text-fill: #E2E8F0;" +
                                "-fx-background-radius: 10;" +
                                "-fx-padding: 7 14;" +
                                "-fx-font-weight: 700;"
                );
                editBtn.setOnAction(e -> {
                    editingId[0] = entry.getId();
                    input.setText(entry.getContent() == null ? "" : entry.getContent());
                    languageBox.setValue(languageLabelFromCode(entry.getLanguageCode()));
                    usedVoiceForEntry[0] = "voice".equalsIgnoreCase(entry.getInputMode());
                    statusLabel.setText("Editing entry #" + entry.getId());
                });

                Button deleteBtn = new Button("Delete");
                deleteBtn.getStyleClass().add("btn-danger");
                deleteBtn.setStyle(
                        "-fx-background-color: #b91c1c;" +
                                "-fx-text-fill: #ffffff;" +
                                "-fx-background-radius: 10;" +
                                "-fx-padding: 7 14;" +
                                "-fx-font-weight: 700;"
                );
                deleteBtn.setOnAction(e -> {
                    try {
                        serviceJournalEntry.delete(entry.getId(), userId);
                        if (editingId[0] == entry.getId()) {
                            editingId[0] = 0;
                            input.clear();
                            usedVoiceForEntry[0] = false;
                        }
                        statusLabel.setText("Entry deleted.");
                        reload[0].run();
                    } catch (RuntimeException ex) {
                        statusLabel.setText("Delete error: " + ex.getMessage());
                    }
                });

                HBox buttonsRow = new HBox(10, editBtn, deleteBtn);
                card.getChildren().addAll(dateLabel, contentLabel, metaLabel, buttonsRow);
                entriesBox.getChildren().add(card);
            }
        };
        reload[0].run();

        startVoiceBtn.setOnAction(e -> {
            if (voiceRunning.get()) {
                return;
            }
            boolean canUseApi = speechToTextService.isConfigured();
            if (!canUseApi) {
                statusLabel.setText("Voice input unavailable: configure OPENAI_API_KEY.");
                return;
            }
            voiceRunning.set(true);
            journalAutoCandidateCode = "";
            journalAutoCandidateHits = 0;
            autoDetectedVoiceLanguage.set("");
            startVoiceBtn.setDisable(true);
            stopVoiceBtn.setDisable(false);
            voiceStatusLabel.setText("Voice: listening...");
            statusLabel.setText("Voice input started (AI transcription: " + speechToTextService.getConfiguredProviderLabel() + ").");

            voiceThread[0] = new Thread(() -> runJournalVoiceLoop(
                    voiceRunning,
                    input,
                    statusLabel,
                    voiceStatusLabel,
                    startVoiceBtn,
                    stopVoiceBtn,
                    selectedVoiceLanguage,
                    usedVoiceForEntry,
                    languageBox,
                    autoDetectedVoiceLanguage
            ), "journal-voice-loop");
            voiceThread[0].setDaemon(true);
            voiceThread[0].start();
        });

        stopVoiceBtn.setOnAction(e -> stopJournalVoiceInput(
                voiceRunning, voiceThread, startVoiceBtn, stopVoiceBtn, voiceStatusLabel, "Voice: stopped"
        ));

        saveBtn.setOnAction(e -> {
            String content = input.getText() == null ? "" : input.getText().trim();
            if (content.isBlank()) {
                statusLabel.setText("Write something first.");
                return;
            }
            try {
                String languageCode = mapJournalLanguageSelection(languageBox.getValue());
                if ("auto".equalsIgnoreCase(languageCode)) {
                    languageCode = normalizeLanguageCode(autoDetectedVoiceLanguage.get());
                }

                if (editingId[0] > 0) {
                    WellbeingJournalEntry item = new WellbeingJournalEntry();
                    item.setId(editingId[0]);
                    item.setContent(content);
                    item.setLanguageCode(languageCode);
                    item.setInputMode(usedVoiceForEntry[0] ? "voice" : "text");
                    item.setUserId(userId);
                    serviceJournalEntry.update(item);
                    statusLabel.setText("Entry updated.");
                } else {
                    WellbeingJournalEntry item = new WellbeingJournalEntry();
                    item.setContent(content);
                    item.setLanguageCode(languageCode);
                    item.setInputMode(usedVoiceForEntry[0] ? "voice" : "text");
                    item.setCreatedAt(LocalDateTime.now());
                    item.setUserId(userId);
                    serviceJournalEntry.add(item);
                    statusLabel.setText("Entry saved.");
                }
                completed[0] = true;
                editingId[0] = 0;
                usedVoiceForEntry[0] = false;
                input.clear();
                reload[0].run();
            } catch (RuntimeException ex) {
                statusLabel.setText("Save error: " + ex.getMessage());
            }
        });

        newBtn.setOnAction(e -> {
            editingId[0] = 0;
            usedVoiceForEntry[0] = false;
            input.clear();
            statusLabel.setText("New journal entry.");
        });

        root.getChildren().addAll(topBanner, composerCard, entriesCard);

        LocalDateTime openedAt = LocalDateTime.now();
        Runnable closer = () -> {
            stopJournalVoiceInput(voiceRunning, voiceThread, startVoiceBtn, stopVoiceBtn, voiceStatusLabel, "Voice: idle");
            closeSession(session, openedAt, completed[0]);
        };
        closeBtn.setOnAction(e -> {
            closer.run();
            if (inlineToolSection != null) {
                inlineToolHost.getChildren().clear();
                inlineToolSection.setVisible(false);
                inlineToolSection.setManaged(false);
                activeInlineToolCloser = null;
            }
        });
        showInlineTool("Gratitude Journal", root, closer);
    }

    private String mapJournalLanguageSelection(String selected) {
        if (selected == null || selected.isBlank()) {
            return "auto";
        }
        return switch (selected) {
            case "English" -> "en-US";
            case "Francais" -> "fr-FR";
            case "Arabic" -> "ar-SA";
            case "Tunisian Arabic" -> "ar-TN";
            default -> "auto";
        };
    }

    private String languageLabelFromCode(String code) {
        if (code == null || code.isBlank()) {
            return "Auto (AI detect)";
        }
        return switch (code) {
            case "en-US" -> "English";
            case "fr-FR" -> "Francais";
            case "ar-SA" -> "Arabic";
            case "ar-TN" -> "Tunisian Arabic";
            default -> "Auto (AI detect)";
        };
    }

    private void stopJournalVoiceInput(
            AtomicBoolean running,
            Thread[] voiceThread,
            Button startVoiceBtn,
            Button stopVoiceBtn,
            Label voiceStatusLabel,
            String statusText
    ) {
        running.set(false);
        if (voiceThread[0] != null) {
            voiceThread[0].interrupt();
        }
        startVoiceBtn.setDisable(false);
        stopVoiceBtn.setDisable(true);
        voiceStatusLabel.setText(statusText);
    }

    private void runJournalVoiceLoop(
            AtomicBoolean running,
            TextArea input,
            Label statusLabel,
            Label voiceStatusLabel,
            Button startVoiceBtn,
            Button stopVoiceBtn,
            AtomicReference<String> languageCodeRef,
            boolean[] usedVoiceForEntry,
            ComboBox<String> languageBox,
            AtomicReference<String> autoDetectedLanguageRef
    ) {
        if (!speechToTextService.isConfigured()) {
            Platform.runLater(() -> {
                statusLabel.setText("Voice input unavailable: configure OPENAI_API_KEY.");
                voiceStatusLabel.setText("Voice: unavailable");
                startVoiceBtn.setDisable(false);
                stopVoiceBtn.setDisable(true);
            });
            running.set(false);
            return;
        }

        AudioFormat format = new AudioFormat(16000f, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        TargetDataLine line = null;
        int transcriptionFailureStreak = 0;

        try {
            if (!AudioSystem.isLineSupported(info)) {
                Platform.runLater(() -> {
                    statusLabel.setText("Voice input not supported on this device.");
                    voiceStatusLabel.setText("Voice: unavailable");
                    startVoiceBtn.setDisable(false);
                    stopVoiceBtn.setDisable(true);
                });
                running.set(false);
                return;
            }

            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            while (running.get()) {
                byte[] wavChunk = captureWavChunk(line, format, 3200, running);
                if (!running.get()) {
                    break;
                }
                if (wavChunk.length < 3000) {
                    continue;
                }
                if (isLikelySilentWav(wavChunk)) {
                    Platform.runLater(() -> voiceStatusLabel.setText("Voice: listening..."));
                    continue;
                }

                String selectedLanguageCode = languageCodeRef.get();
                String sttLanguage = "auto".equalsIgnoreCase(selectedLanguageCode) ? null : selectedLanguageCode;
                SpeechToTextService.TranscriptionResult result = speechToTextService.transcribeWavDetailed(wavChunk, sttLanguage);
                if (result == null || result.text() == null || result.text().isBlank()) {
                    transcriptionFailureStreak++;
                    String detail = speechToTextService.getLastError();
                    if (transcriptionFailureStreak >= 3) {
                        Platform.runLater(() -> statusLabel.setText(
                                detail == null || detail.isBlank()
                                        ? "Voice captured but no text detected."
                                        : "Transcription issue: " + detail
                        ));
                    }
                    continue;
                }
                transcriptionFailureStreak = 0;

                String clean = result.text().trim();
                if (!isMeaningfulSpeechText(clean)) {
                    continue;
                }
                String detectedLanguageCode = result.languageCode();
                Platform.runLater(() -> {
                    appendTranscriptToInput(input, clean);
                    usedVoiceForEntry[0] = true;
                    updateJournalAutoLanguage(
                            languageBox,
                            clean,
                            input.getText(),
                            statusLabel,
                            detectedLanguageCode,
                            autoDetectedLanguageRef
                    );
                    voiceStatusLabel.setText("Voice: writing...");
                });
            }
        } catch (LineUnavailableException e) {
            Platform.runLater(() -> {
                statusLabel.setText("Microphone unavailable.");
                voiceStatusLabel.setText("Voice: unavailable");
            });
        } catch (RuntimeException e) {
            Platform.runLater(() -> {
                statusLabel.setText("Voice input error.");
                voiceStatusLabel.setText("Voice: error");
            });
        } finally {
            if (line != null) {
                line.stop();
                line.close();
            }
            running.set(false);
            Platform.runLater(() -> {
                startVoiceBtn.setDisable(false);
                stopVoiceBtn.setDisable(true);
                if (!"Voice: error".equals(voiceStatusLabel.getText()) && !"Voice: unavailable".equals(voiceStatusLabel.getText())) {
                    voiceStatusLabel.setText("Voice: idle");
                }
            });
        }
    }

    private byte[] captureWavChunk(TargetDataLine line, AudioFormat format, int millis, AtomicBoolean running) {
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int frameSize = Math.max(1, format.getFrameSize());
        int targetBytes = Math.max(frameSize * 32, (int) ((format.getSampleRate() * frameSize * millis) / 1000.0));
        int totalRead = 0;

        while (running.get() && totalRead < targetBytes) {
            int toRead = Math.min(buffer.length, targetBytes - totalRead);
            int read = line.read(buffer, 0, toRead);
            if (read > 0) {
                pcm.write(buffer, 0, read);
                totalRead += read;
            } else if (read < 0) {
                break;
            }
        }

        byte[] raw = pcm.toByteArray();
        if (raw.length == 0) {
            return new byte[0];
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(raw);
             AudioInputStream ais = new AudioInputStream(bais, format, raw.length / format.getFrameSize());
             ByteArrayOutputStream wavOut = new ByteArrayOutputStream()) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavOut);
            return wavOut.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private void appendTranscriptToInput(TextArea input, String transcript) {
        if (!isMeaningfulSpeechText(transcript)) {
            return;
        }
        String existing = input.getText() == null ? "" : input.getText().trim();
        String next = transcript.trim();
        if (existing.isBlank()) {
            input.setText(next);
            input.positionCaret(input.getText().length());
            return;
        }
        String sep = existing.endsWith(".") || existing.endsWith("!") || existing.endsWith("?") ? " " : "\n";
        input.setText(existing + sep + next);
        input.positionCaret(input.getText().length());
    }

    private boolean isMeaningfulSpeechText(String text) {
        if (text == null) {
            return false;
        }
        String clean = text.trim();
        if (clean.isBlank() || clean.length() < 2) {
            return false;
        }
        return clean.chars().anyMatch(Character::isLetterOrDigit);
    }

    private boolean isLikelySilentWav(byte[] wavChunk) {
        if (wavChunk == null || wavChunk.length < 46) {
            return true;
        }
        int start = wavChunk.length > 44 ? 44 : 0;
        long sumAbs = 0L;
        int sampleCount = 0;
        int peak = 0;

        for (int i = start; i + 1 < wavChunk.length; i += 2) {
            int sample = (short) (((wavChunk[i + 1] & 0xFF) << 8) | (wavChunk[i] & 0xFF));
            int abs = Math.abs(sample);
            sumAbs += abs;
            sampleCount++;
            if (abs > peak) {
                peak = abs;
            }
        }

        if (sampleCount == 0) {
            return true;
        }
        double avg = sumAbs / (double) sampleCount;
        return avg < VOICE_SILENCE_AVG_THRESHOLD && peak < VOICE_SILENCE_PEAK_THRESHOLD;
    }

    private void updateJournalAutoLanguage(
            ComboBox<String> languageBox,
            String transcript,
            String fullInputText,
            Label statusLabel,
            String detectedLanguageCode,
            AtomicReference<String> autoDetectedLanguageRef
    ) {
        if (languageBox == null || transcript == null || transcript.isBlank()) {
            return;
        }
        String selection = languageBox.getValue();
        if (!"Auto (AI detect)".equals(selection)) {
            statusLabel.setText("Voice segment captured.");
            return;
        }

        String code = normalizeLanguageCode(detectedLanguageCode);
        if (code == null) {
            code = detectJournalSpeechLanguageCode(transcript, fullInputText);
        }
        String current = normalizeLanguageCode(autoDetectedLanguageRef == null ? null : autoDetectedLanguageRef.get());
        if (current == null) {
            current = "en-US";
        }
        if (code == null || code.isBlank()) {
            statusLabel.setText("Voice segment captured.");
            return;
        }

        // Stabilize auto-switch: require two consecutive detections before switching.
        if (!code.equals(journalAutoCandidateCode)) {
            journalAutoCandidateCode = code;
            journalAutoCandidateHits = 1;
        } else {
            journalAutoCandidateHits++;
        }

        if (!code.equals(current) && journalAutoCandidateHits < 2) {
            statusLabel.setText("Voice segment captured. Detecting language...");
            return;
        }

        if (autoDetectedLanguageRef != null) {
            autoDetectedLanguageRef.set(code);
        }
        statusLabel.setText("Voice segment captured. Auto-detected: " + languageLabelFromCode(code) + " (" + code + ")");
    }

    private String detectJournalSpeechLanguageCode(String transcript, String fullInputText) {
        String segment = normalizeLanguageHeuristicsText(transcript);
        String full = normalizeLanguageHeuristicsText(limitText(fullInputText, 800));
        String merged = (full + " " + segment).trim();
        if (merged.isBlank()) {
            return "en-US";
        }

        if (merged.matches(".*[\\u0600-\\u06FF].*")) {
            int tnArabic = scoreText(
                    merged,
                    "\u0628\u0631\u0634\u0629", // barsha
                    "\u0634\u0646\u0648\u0629", // chnowa
                    "\u062a\u0648\u0629", // tawa
                    "\u064a\u0627\u0633\u0631", // yesser
                    "\u0628\u0631\u0627\u0628\u064a", // brabi
                    "\u0641\u0645\u0627", // famma
                    "\u0644\u0627\u0628\u0627\u0633", // labes
                    "\u0646\u062d\u0628", // nheb
                    "\u062a\u0648\u0646\u0633" // tounes
            );
            return tnArabic >= 1 ? "ar-TN" : "ar-SA";
        }

        int tnLatin = scoreText(
                merged,
                "barsha", "barcha", "brcha",
                "3lech", "alech",
                "chnowa", "chnoua", "shnowa",
                "kifech", "kifach",
                "brabi", "rabi",
                "yesser", "yaser",
                "tawa", "taw",
                "behi", "behy",
                "mouch", "moch",
                "famma", "famech",
                "nheb", "n7eb",
                "labes", "lebes",
                "sa7a", "saha",
                "ya3ni", "3andi", "3andek", "3andou",
                "ma3ndich", "mandich",
                "tounes", "tunis", "sfax", "sousse", "nabeul", "bizerte", "gabes", "djerba"
        );
        if (tnLatin >= 2) {
            return "ar-TN";
        }

        int fr = scoreText(merged, "bonjour", "merci", "je ", "suis", "avec", "pour", "pas", "oui", "francais", "ca", "salut");
        int en = scoreText(merged, "hello", "thanks", "i ", "am", "with", "for", "not", "yes", "english", "please");
        if (tnLatin >= fr + 1 && tnLatin >= en + 1) {
            return "ar-TN";
        }
        if (fr >= en + 1) {
            return "fr-FR";
        }

        return "en-US";
    }

    private String normalizeLanguageHeuristicsText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text
                .toLowerCase(Locale.ROOT)
                .replace('’', '\'')
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\u0600-\\u06FF\\s']", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String detectJournalSpeechLanguageCodeLegacy(String transcript, String fullInputText) {
        String segment = transcript == null ? "" : transcript.trim().toLowerCase(Locale.ROOT);
        String full = fullInputText == null ? "" : limitText(fullInputText, 600).toLowerCase(Locale.ROOT);
        String merged = (full + " " + segment).trim();
        if (merged.isBlank()) {
            return "en-US";
        }

        if (merged.matches(".*[\\u0600-\\u06FF].*")) {
            int tnArabic = scoreText(merged, "برشة", "شنوة", "توا", "ياسر", "برابي", "فما", "معاك", "نحب");
            return tnArabic >= 1 ? "ar-TN" : "ar-SA";
        }

        int tnLatin = scoreText(merged, "barsha", "barcha", "3lech", "chnowa", "brabi", "yesser", "tawa", "behi", "mouch", "famma", "ya3ni", "nheb", "n7eb", "ma3ak");
        if (tnLatin >= 2) {
            return "ar-TN";
        }

        int fr = scoreText(merged, "bonjour", "merci", "je ", "suis", "avec", "pour", "pas", "oui", "francais", "ça", "ca", "salut");
        int en = scoreText(merged, "hello", "thanks", "i ", "am", "with", "for", "not", "yes", "english", "please");
        if (fr >= en + 1) {
            return "fr-FR";
        }

        return "en-US";
    }

    private String normalizeLanguageCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (normalized.startsWith("fr")) {
            return "fr-FR";
        }
        if (normalized.startsWith("ar-tn")) {
            return "ar-TN";
        }
        if (normalized.startsWith("ar")) {
            return "ar-SA";
        }
        if (normalized.startsWith("en")) {
            return "en-US";
        }
        return null;
    }

    private int scoreText(String text, String... tokens) {
        int score = 0;
        for (String token : tokens) {
            if (text.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private String limitText(String value, int max) {
        if (value == null) {
            return "";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(value.length() - max);
    }

    private void openAiChatTool(CopingSession session) {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: transparent;");

        VBox header = new VBox(6);
        header.setPadding(new Insets(16));
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, #111827, #1E293B);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #334155;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 16;"
        );
        Label focusLabel = new Label("FOCUS SESSION");
        focusLabel.setStyle("-fx-text-fill: #A78BFA; -fx-font-size: 11px; -fx-font-weight: 700; -fx-letter-spacing: 1px;");
        Label titleLabel = new Label("AI Chat Assistant");
        titleLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 38px; -fx-font-weight: 800;");
        Label subtitle = new Label("Motivation · Guidance · Support · Any Language");
        subtitle.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");
        header.getChildren().addAll(focusLabel, titleLabel, subtitle);

        Label statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: #A78BFA; -fx-font-size: 12px; -fx-font-weight: 600;");

        VBox messagesBox = new VBox(12);
        messagesBox.setPadding(new Insets(14));
        messagesBox.setStyle("-fx-background-color: #111827;");
        messagesBox.getChildren().add(chatBubble("assistant", "Hello! I can help with general questions, studies, or wellbeing. What do you need right now?"));

        ScrollPane messagesPane = new ScrollPane(messagesBox);
        messagesPane.setFitToWidth(true);
        messagesPane.setPrefHeight(370);
        messagesPane.setStyle(
                "-fx-background: #111827;" +
                        "-fx-background-color: #111827;" +
                        "-fx-border-color: #334155;" +
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
                    "-fx-background-color: #1E293B;" +
                            "-fx-text-fill: #E2E8F0;" +
                            "-fx-border-color: #475569;" +
                            "-fx-border-radius: 9;" +
                            "-fx-background-radius: 9;"
            );
        }
        HBox configRow = new HBox(10, modeBox, styleBox, levelBox, languageBox);

        HBox chipsRow = new HBox(8,
                createChatChip("Explain this concept"),
                createChatChip("Help me write an email"),
                createChatChip("Build a study plan"),
                createChatChip("I feel stressed")
        );

        TextArea input = new TextArea();
        input.setPromptText("Write your message in any language...");
        input.setWrapText(true);
        input.setPrefRowCount(3);
        input.setStyle(
                "-fx-control-inner-background: #1E293B;" +
                        "-fx-text-fill: #e2e8f0;" +
                        "-fx-prompt-text-fill: #94A3B8;" +
                        "-fx-border-color: #475569;" +
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
        StackPane aiLoadingOverlay = createAiLoadingOverlay(
                "AI is thinking...",
                "This usually takes a few seconds while AI prepares your answer."
        );

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
            clearBtn.setDisable(true);
            input.setDisable(true);
            setAiLoadingVisible(aiLoadingOverlay, true);

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
                clearBtn.setDisable(false);
                input.setDisable(false);
                setAiLoadingVisible(aiLoadingOverlay, false);
                Platform.runLater(() -> {
                    messagesPane.layout();
                    messagesPane.setVvalue(1.0);
                });
            });
            task.setOnFailed(evt -> {
                messagesBox.getChildren().add(chatBubble("assistant", "Sorry, I could not generate a reply right now. Please try again."));
                statusLabel.setText("Error while generating response.");
                sendBtn.setDisable(false);
                clearBtn.setDisable(false);
                input.setDisable(false);
                setAiLoadingVisible(aiLoadingOverlay, false);
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
            messagesBox.getChildren().setAll(chatBubble("assistant", "Hello! I can help with general questions, studies, or wellbeing. What do you need right now?"));
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
                "-fx-background-color: linear-gradient(to bottom right, #0F172A, #111827);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #334155;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 16;"
        );

        StackPane chatCardStack = new StackPane(chatCard, aiLoadingOverlay);
        root.getChildren().addAll(header, chatCardStack);

        LocalDateTime openedAt = LocalDateTime.now();
        Runnable closer = () -> closeSession(session, openedAt, completed[0]);
        closeBtn.setOnAction(e -> {
            closer.run();
            if (inlineToolSection != null) {
                inlineToolHost.getChildren().clear();
                inlineToolSection.setVisible(false);
                inlineToolSection.setManaged(false);
                activeInlineToolCloser = null;
            }
        });
        showInlineTool("AI Chat Assistant", root, closer);
    }

    private Button createChatChip(String text) {
        Button chip = new Button(text);
        chip.setStyle(
                "-fx-background-color: #1E293B;" +
                        "-fx-text-fill: #CBD5E1;" +
                        "-fx-background-radius: 100;" +
                        "-fx-border-color: #475569;" +
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
                    "-fx-background-color: #1E293B;" +
                            "-fx-text-fill: #e2e8f0;" +
                            "-fx-background-radius: 14;" +
                            "-fx-border-color: #475569;" +
                            "-fx-border-radius: 14;"
            );
        } else {
            msg.setStyle(
                    "-fx-background-color: rgba(124,58,237,0.25);" +
                            "-fx-text-fill: #EDE9FE;" +
                            "-fx-background-radius: 14;" +
                            "-fx-border-color: #8B5CF6;" +
                            "-fx-border-radius: 14;"
            );
        }
        Label time = new Label("just now");
        time.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        VBox block = new VBox(4, msg, time);
        HBox row = new HBox(block);
        row.setAlignment("user".equals(role) ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return row;
    }

    private StackPane createAiLoadingOverlay(String title, String subtitle) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("ai-loading-overlay");
        overlay.setVisible(false);
        overlay.setManaged(false);
        overlay.setPickOnBounds(true);

        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("ai-loading-card");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("ai-loading-title");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("ai-loading-subtitle");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(320);

        ProgressBar loadingBar = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
        loadingBar.getStyleClass().add("ai-loading-bar");
        loadingBar.setMaxWidth(Double.MAX_VALUE);
        loadingBar.setPrefWidth(280);

        card.getChildren().addAll(titleLabel, subtitleLabel, loadingBar);
        overlay.getChildren().add(card);
        return overlay;
    }

    private void setAiLoadingVisible(StackPane overlay, boolean visible) {
        if (overlay == null) {
            return;
        }
        overlay.setVisible(visible);
        overlay.setManaged(visible);
        overlay.setMouseTransparent(!visible);
    }

    private void openNatureSoundsTool(CopingSession session) {
        VBox root = new VBox(14);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #040b19, #07122a, #0a1633);");

        VBox shell = new VBox(12);
        shell.setPadding(new Insets(12));
        shell.setStyle(
                "-fx-background-color: rgba(8, 12, 24, 0.92);" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #1f2937;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 18;"
        );

        Label eyebrow = new Label("NATURE SOUND DECK");
        eyebrow.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px; -fx-font-weight: 700; -fx-letter-spacing: 1px;");
        Label title = new Label("Nature Sounds");
        title.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 28px; -fx-font-weight: 800;");
        Label subtitle = new Label("Ambient playlists for focus and relaxation");
        subtitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        ToggleGroup sizeGroup = new ToggleGroup();
        ToggleButton sBtn = new ToggleButton("S");
        ToggleButton mBtn = new ToggleButton("M");
        ToggleButton lBtn = new ToggleButton("L");
        ToggleButton xlBtn = new ToggleButton("XL");
        for (ToggleButton b : List.of(sBtn, mBtn, lBtn, xlBtn)) {
            b.setToggleGroup(sizeGroup);
            b.setStyle(
                    "-fx-background-color: #111827;" +
                            "-fx-text-fill: #94a3b8;" +
                            "-fx-background-radius: 8;" +
                            "-fx-border-color: #1f2937;" +
                            "-fx-border-radius: 8;" +
                            "-fx-padding: 4 10;" +
                            "-fx-font-weight: 700;"
            );
        }
        mBtn.setSelected(true);

        Button collapseBtn = new Button("x");
        collapseBtn.setStyle(
                "-fx-background-color: #1f2937;" +
                        "-fx-text-fill: #cbd5e1;" +
                        "-fx-background-radius: 999;" +
                        "-fx-min-width: 24;" +
                        "-fx-min-height: 24;" +
                        "-fx-max-width: 24;" +
                        "-fx-max-height: 24;" +
                        "-fx-font-weight: 800;"
        );
        HBox topRow = new HBox(10, new VBox(2, eyebrow, title, subtitle), new Region(), sBtn, mBtn, lBtn, xlBtn, collapseBtn);
        HBox.setHgrow(topRow.getChildren().get(1), Priority.ALWAYS);
        topRow.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup categoryGroup = new ToggleGroup();
        ToggleButton natureBtn = new ToggleButton("Nature");
        ToggleButton quranBtn = new ToggleButton("Quran");
        ToggleButton relaxBtn = new ToggleButton("Relax");
        ToggleButton motivationBtn = new ToggleButton("Motivation");
        List<ToggleButton> categoryButtons = List.of(natureBtn, quranBtn, relaxBtn, motivationBtn);
        for (ToggleButton b : categoryButtons) {
            b.setToggleGroup(categoryGroup);
            b.setStyle(
                    "-fx-background-color: #161b22;" +
                            "-fx-text-fill: #cbd5e1;" +
                            "-fx-background-radius: 16;" +
                            "-fx-border-color: #334155;" +
                            "-fx-border-radius: 16;" +
                            "-fx-padding: 8 16;" +
                            "-fx-font-weight: 700;"
            );
        }
        natureBtn.setSelected(true);
        HBox categories = new HBox(8, natureBtn, quranBtn, relaxBtn, motivationBtn);

        VBox playlistCard = new VBox(10);
        playlistCard.setPadding(new Insets(14));
        playlistCard.setStyle(
                "-fx-background-color: #111111;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #232323;" +
                        "-fx-border-radius: 14;"
        );

        HBox coverAndTracks = new HBox(12);
        StackPane cover = new StackPane();
        cover.setPrefSize(120, 120);
        cover.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #0f5132, #052e16, #14532d);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #166534;" +
                        "-fx-border-radius: 12;"
        );
        Label coverLabel = new Label("Nature\nSounds");
        coverLabel.setStyle("-fx-text-fill: #d1fae5; -fx-font-size: 24px; -fx-font-weight: 800;");
        cover.getChildren().add(coverLabel);

        ListView<String> tracksList = new ListView<>();
        tracksList.setPrefHeight(120);
        tracksList.setStyle(
                "-fx-control-inner-background: #1f2937;" +
                        "-fx-background-color: #1f2937;" +
                        "-fx-text-fill: #e5e7eb;" +
                        "-fx-border-color: #374151;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;"
        );

        coverAndTracks.getChildren().addAll(cover, tracksList);
        HBox.setHgrow(tracksList, Priority.ALWAYS);

        Label playlistTitle = new Label("Nature Sounds · Spotify");
        playlistTitle.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 26px; -fx-font-weight: 800;");
        Label nowPlaying = new Label("Now playing: Creek Rain With Birds And Thunder");
        nowPlaying.setStyle("-fx-text-fill: #a3a3a3; -fx-font-size: 12px;");

        playlistCard.getChildren().addAll(coverAndTracks, playlistTitle, nowPlaying);

        Label timer = new Label("05:00");
        timer.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 30px; -fx-font-weight: 800;");
        Label status = new Label("Choose a playlist and press Start.");
        status.setStyle("-fx-text-fill: #60a5fa; -fx-font-size: 12px; -fx-font-weight: 700;");

        Map<String, List<String>> playlists = new LinkedHashMap<>();
        playlists.put("Nature", List.of(
                "Creek Rain With Birds And Thunder",
                "Rain in the Andorran Forest",
                "Bird Sounds in Mano de Oso",
                "River Birds (Green Noise)",
                "Ocean Breeze and Leaves"
        ));
        playlists.put("Quran", List.of(
                "Sourat Al-Anbiya - Yasser Al-Dossari",
                "Sourat Al-Rahman - Calm Recitation",
                "Sourat Maryam - Soft Night Mode",
                "Sourat Yasin - Focus Recitation"
        ));
        playlists.put("Relax", List.of(
                "Deep Alpha Relaxation",
                "Floating Clouds Ambience",
                "Moonlight Chill Session",
                "Calm Piano and Water"
        ));
        playlists.put("Motivation", List.of(
                "Present Memory - Kask Srkiner",
                "Hidden Treasure - Starstrum",
                "Velvet - The Dawning",
                "Progress Pulse - Study Edition"
        ));

        Runnable refreshTheme = () -> {
            Toggle selected = categoryGroup.getSelectedToggle();
            String key = selected instanceof ToggleButton tb ? tb.getText() : "Nature";
            List<String> tracks = playlists.getOrDefault(key, List.of());
            tracksList.getItems().setAll(tracks);
            if (!tracks.isEmpty()) {
                tracksList.getSelectionModel().select(0);
                nowPlaying.setText("Now playing: " + tracks.get(0));
            } else {
                nowPlaying.setText("Now playing: -");
            }

            String accent = switch (key) {
                case "Quran" -> "#16a34a";
                case "Relax" -> "#f59e0b";
                case "Motivation" -> "#22c55e";
                default -> "#22c55e";
            };
            for (ToggleButton b : categoryButtons) {
                boolean active = b.isSelected();
                b.setStyle(
                        "-fx-background-color: " + (active ? accent : "#161b22") + ";" +
                                "-fx-text-fill: " + (active ? "#041016" : "#cbd5e1") + ";" +
                                "-fx-background-radius: 16;" +
                                "-fx-border-color: " + (active ? accent : "#334155") + ";" +
                                "-fx-border-radius: 16;" +
                                "-fx-padding: 8 16;" +
                                "-fx-font-weight: 700;"
                );
            }
            playlistTitle.setText(key + " Sounds · Spotify");
            coverLabel.setText(key + "\nSounds");
        };
        refreshTheme.run();

        categoryGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> refreshTheme.run());
        tracksList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.isBlank()) {
                nowPlaying.setText("Now playing: " + newV);
            }
        });

        int[] remaining = {300};
        boolean[] completed = {false};
        Timeline[] timeline = new Timeline[1];
        timeline[0] = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> {
            remaining[0]--;
            int minutes = Math.max(0, remaining[0]) / 60;
            int seconds = Math.max(0, remaining[0]) % 60;
            timer.setText(String.format("%02d:%02d", minutes, seconds));
            if (remaining[0] <= 0) {
                timeline[0].stop();
                completed[0] = true;
                status.setText("Session complete. Great focus.");
            }
        }));
        timeline[0].setCycleCount(Timeline.INDEFINITE);

        Runnable resetFromSize = () -> {
            int value = 300;
            Toggle selected = sizeGroup.getSelectedToggle();
            if (selected instanceof ToggleButton tb) {
                value = switch (tb.getText()) {
                    case "S" -> 120;
                    case "L" -> 900;
                    case "XL" -> 1500;
                    default -> 300;
                };
            }
            remaining[0] = value;
            timer.setText(String.format("%02d:%02d", value / 60, value % 60));
        };
        resetFromSize.run();
        sizeGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (timeline[0].getStatus() != javafx.animation.Animation.Status.RUNNING) {
                resetFromSize.run();
            }
        });

        Button start = new Button("Start");
        start.setStyle(
                "-fx-background-color: #16a34a;" +
                        "-fx-text-fill: #ecfdf5;" +
                        "-fx-background-radius: 24;" +
                        "-fx-padding: 10 22;" +
                        "-fx-font-weight: 800;"
        );
        start.setOnAction(e -> {
            completed[0] = false;
            resetFromSize.run();
            timeline[0].playFromStart();
            status.setText("Playing " + (tracksList.getSelectionModel().getSelectedItem() == null ? "session" : tracksList.getSelectionModel().getSelectedItem()));
        });

        Button pause = new Button("Pause");
        pause.setStyle(
                "-fx-background-color: #334155;" +
                        "-fx-text-fill: #f1f5f9;" +
                        "-fx-background-radius: 24;" +
                        "-fx-padding: 10 22;" +
                        "-fx-font-weight: 700;"
        );
        pause.setOnAction(e -> {
            timeline[0].stop();
            status.setText("Session paused.");
        });

        Button reset = new Button("Reset");
        reset.setStyle(
                "-fx-background-color: #1e293b;" +
                        "-fx-text-fill: #cbd5e1;" +
                        "-fx-background-radius: 24;" +
                        "-fx-padding: 10 22;" +
                        "-fx-font-weight: 700;"
        );
        reset.setOnAction(e -> {
            timeline[0].stop();
            resetFromSize.run();
            status.setText("Session reset.");
        });

        Button close = new Button("Back");
        close.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #94a3b8;" +
                        "-fx-border-color: #334155;" +
                        "-fx-border-radius: 24;" +
                        "-fx-background-radius: 24;" +
                        "-fx-padding: 10 22;" +
                        "-fx-font-weight: 700;"
        );

        HBox controls = new HBox(10, start, pause, reset, close, new Region(), timer);
        HBox.setHgrow(controls.getChildren().get(4), Priority.ALWAYS);
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox footer = new VBox(8, controls, status);
        footer.setPadding(new Insets(8, 2, 2, 2));

        shell.getChildren().addAll(topRow, categories, playlistCard, footer);

        Button bubbleButton = new Button("♫");
        bubbleButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #1ed760, #1db954);" +
                        "-fx-text-fill: #041016;" +
                        "-fx-font-size: 24px;" +
                        "-fx-font-weight: 800;" +
                        "-fx-background-radius: 999;" +
                        "-fx-min-width: 62;" +
                        "-fx-min-height: 62;" +
                        "-fx-max-width: 62;" +
                        "-fx-max-height: 62;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(29,185,84,0.45), 18, 0.35, 0, 6);"
        );
        bubbleButton.setText("SP");
        Label bubbleHint = new Label("Nature Sounds");
        bubbleHint.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-font-weight: 700;");
        VBox bubbleWrap = new VBox(6, bubbleButton, bubbleHint);
        bubbleWrap.setAlignment(Pos.CENTER);

        StackPane widgetArea = new StackPane();
        widgetArea.setPadding(new Insets(8));
        widgetArea.getChildren().addAll(shell, bubbleWrap);
        VBox.setVgrow(widgetArea, Priority.ALWAYS);
        StackPane.setAlignment(shell, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(shell, new Insets(0, 20, 86, 0));
        StackPane.setAlignment(bubbleWrap, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(bubbleWrap, new Insets(0, 20, 14, 0));
        root.getChildren().add(widgetArea);

        Runnable showPanel = () -> {
            shell.setManaged(true);
            shell.setVisible(true);
            bubbleHint.setText("Hide Player");
        };
        Runnable hidePanel = () -> {
            shell.setVisible(false);
            shell.setManaged(false);
            bubbleHint.setText("Nature Sounds");
        };
        bubbleButton.setOnAction(e -> {
            if (shell.isVisible()) {
                hidePanel.run();
            } else {
                showPanel.run();
            }
        });
        collapseBtn.setOnAction(e -> hidePanel.run());
        hidePanel.run();

        Stage stage = createToolStage("Nature Sounds", root, 980, 670);

        LocalDateTime openedAt = LocalDateTime.now();
        AtomicBoolean sessionClosed = new AtomicBoolean(false);
        Runnable closer = () -> {
            if (sessionClosed.getAndSet(true)) {
                return;
            }
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

    private void openNatureSoundsSpotifyTool(CopingSession session) {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #040b19, #07122a, #0a1633);");
        root.setPadding(new Insets(10));

        Map<String, String> spotifyPlaylists = new LinkedHashMap<>();
        spotifyPlaylists.put("nature", "https://open.spotify.com/embed/playlist/37i9dQZF1DX4PP3DA4J0N8?utm_source=generator&theme=0");
        spotifyPlaylists.put("quran", "https://open.spotify.com/embed/playlist/2mIXv4QFfQbcNv89QmT6XQ?utm_source=generator&theme=0");
        spotifyPlaylists.put("motivation", "https://open.spotify.com/embed/playlist/37i9dQZF1DX1s9knjP51Oa?utm_source=generator&theme=0");
        spotifyPlaylists.put("calm", "https://open.spotify.com/embed/playlist/37i9dQZF1DX3Ogo9pFvBkY?utm_source=generator&theme=0");
        spotifyPlaylists.put("focus", "https://open.spotify.com/embed/playlist/37i9dQZF1DWZeKCadgRdKQ?utm_source=generator&theme=0");
        spotifyPlaylists.put("sleep", "https://open.spotify.com/embed/playlist/37i9dQZF1DWStPkKCDmQbL?utm_source=generator&theme=0");

        Map<String, String> spotifyLabels = new LinkedHashMap<>();
        spotifyLabels.put("nature", "Nature Sounds");
        spotifyLabels.put("quran", "Quran");
        spotifyLabels.put("motivation", "Motivation");
        spotifyLabels.put("calm", "Calm");
        spotifyLabels.put("focus", "Focus");
        spotifyLabels.put("sleep", "Sleep");

        Map<String, Integer> spotifyHeights = new LinkedHashMap<>();
        spotifyHeights.put("sm", 152);
        spotifyHeights.put("md", 200);
        spotifyHeights.put("lg", 240);
        spotifyHeights.put("xl", 280);

        VBox widget = new VBox(10);
        widget.setMaxWidth(560);
        widget.setPrefWidth(560);
        widget.setPadding(new Insets(12));
        widget.setStyle(
                "-fx-background-color: rgba(8, 12, 24, 0.94);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: rgba(51, 65, 85, 0.85);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 16;"
        );

        HBox topBar = new HBox(8);
        topBar.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label("Nature Sounds");
        label.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 14px; -fx-font-weight: 800;");
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        ToggleGroup sizeGroup = new ToggleGroup();
        ToggleButton sBtn = new ToggleButton("S");
        ToggleButton mBtn = new ToggleButton("M");
        ToggleButton lBtn = new ToggleButton("L");
        ToggleButton xlBtn = new ToggleButton("XL");
        Map<ToggleButton, String> sizeMap = new LinkedHashMap<>();
        sizeMap.put(sBtn, "sm");
        sizeMap.put(mBtn, "md");
        sizeMap.put(lBtn, "lg");
        sizeMap.put(xlBtn, "xl");
        for (ToggleButton b : sizeMap.keySet()) {
            b.setToggleGroup(sizeGroup);
            b.setStyle(
                    "-fx-background-color: #111827;" +
                            "-fx-text-fill: #cbd5e1;" +
                            "-fx-font-size: 11px;" +
                            "-fx-font-weight: 700;" +
                            "-fx-padding: 4 8;" +
                            "-fx-background-radius: 8;" +
                            "-fx-border-color: #334155;" +
                            "-fx-border-radius: 8;"
            );
        }
        sBtn.setSelected(true);

        Button collapseBtn = new Button("-");
        collapseBtn.setStyle(
                "-fx-background-color: #1f2937;" +
                        "-fx-text-fill: #cbd5e1;" +
                        "-fx-font-weight: 800;" +
                        "-fx-background-radius: 999;" +
                        "-fx-min-width: 24;" +
                        "-fx-min-height: 24;" +
                        "-fx-max-width: 24;" +
                        "-fx-max-height: 24;"
        );
        Button closeBtn = new Button("x");
        closeBtn.setStyle(
                "-fx-background-color: #1f2937;" +
                        "-fx-text-fill: #cbd5e1;" +
                        "-fx-font-weight: 800;" +
                        "-fx-background-radius: 999;" +
                        "-fx-min-width: 24;" +
                        "-fx-min-height: 24;" +
                        "-fx-max-width: 24;" +
                        "-fx-max-height: 24;"
        );
        topBar.getChildren().addAll(label, topSpacer, sBtn, mBtn, lBtn, xlBtn, collapseBtn, closeBtn);

        HBox presetRow = new HBox(8);
        String[] presets = {"nature", "quran", "motivation", "calm", "focus", "sleep"};
        List<ToggleButton> presetButtons = new ArrayList<>();
        ToggleGroup presetGroup = new ToggleGroup();
        for (String preset : presets) {
            ToggleButton b = new ToggleButton(spotifyLabels.getOrDefault(preset, preset));
            b.setUserData(preset);
            b.setToggleGroup(presetGroup);
            b.setStyle(
                    "-fx-background-color: #161b22;" +
                            "-fx-text-fill: #cbd5e1;" +
                            "-fx-font-size: 12px;" +
                            "-fx-font-weight: 700;" +
                            "-fx-padding: 6 12;" +
                            "-fx-background-radius: 999;" +
                            "-fx-border-color: #334155;" +
                            "-fx-border-radius: 999;"
            );
            presetButtons.add(b);
            presetRow.getChildren().add(b);
        }
        if (!presetButtons.isEmpty()) {
            presetButtons.get(0).setSelected(true);
        }

        HBox customUrlRow = new HBox(8);
        customUrlRow.setAlignment(Pos.CENTER_LEFT);
        TextField customUrlField = new TextField();
        customUrlField.setPromptText("Paste Spotify embed URL...");
        customUrlField.setStyle(
                "-fx-background-color: #0f172a;" +
                        "-fx-text-fill: #e2e8f0;" +
                        "-fx-prompt-text-fill: #64748b;" +
                        "-fx-border-color: #334155;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;"
        );
        HBox.setHgrow(customUrlField, Priority.ALWAYS);
        Button loadCustomBtn = new Button("Load URL");
        loadCustomBtn.setStyle(
                "-fx-background-color: #2563eb;" +
                        "-fx-text-fill: #dbeafe;" +
                        "-fx-font-weight: 700;" +
                        "-fx-background-radius: 8;"
        );
        customUrlRow.getChildren().addAll(customUrlField, loadCustomBtn);

        Label loadingLabel = new Label("Loading Spotify...");
        loadingLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        loadingLabel.setVisible(true);
        loadingLabel.setManaged(true);

        WebView spotifyView = new WebView();
        WebEngine spotifyEngine = spotifyView.getEngine();
        spotifyView.setContextMenuEnabled(false);
        spotifyView.setPrefHeight(152);
        spotifyView.setStyle(
                "-fx-background-color: #0b1220;" +
                        "-fx-border-color: #1f2937;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;"
        );

        VBox iframeWrap = new VBox(loadingLabel, spotifyView);
        iframeWrap.setSpacing(6);
        Label footer = new Label("Spotify floating player");
        footer.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        widget.getChildren().addAll(topBar, presetRow, customUrlRow, iframeWrap, footer);

        Button bubbleButton = new Button("SP");
        bubbleButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #1ed760, #1db954);" +
                        "-fx-text-fill: #041016;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: 800;" +
                        "-fx-background-radius: 999;" +
                        "-fx-min-width: 58;" +
                        "-fx-min-height: 58;" +
                        "-fx-max-width: 58;" +
                        "-fx-max-height: 58;" +
                        "-fx-effect: dropshadow(gaussian, rgba(29,185,84,0.45), 16, 0.3, 0, 4);"
        );
        Label bubbleHint = new Label("Nature Sounds");
        bubbleHint.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-font-weight: 700;");
        VBox bubbleWrap = new VBox(6, bubbleButton, bubbleHint);
        bubbleWrap.setAlignment(Pos.CENTER);

        StackPane.setAlignment(widget, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(widget, new Insets(0, 18, 86, 0));
        StackPane.setAlignment(bubbleWrap, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(bubbleWrap, new Insets(0, 18, 14, 0));
        root.getChildren().addAll(widget, bubbleWrap);

        final String[] currentPreset = {"nature"};
        final String[] currentSize = {"sm"};
        final double[] dragAnchor = {0, 0};
        final double[] dragTranslate = {0, 0};

        Runnable refreshPresetStyles = () -> {
            for (ToggleButton b : presetButtons) {
                boolean selected = b.isSelected();
                b.setStyle(
                        "-fx-background-color: " + (selected ? "#1d4ed8" : "#161b22") + ";" +
                                "-fx-text-fill: " + (selected ? "#dbeafe" : "#cbd5e1") + ";" +
                                "-fx-font-size: 12px;" +
                                "-fx-font-weight: 700;" +
                                "-fx-padding: 6 12;" +
                                "-fx-background-radius: 999;" +
                                "-fx-border-color: " + (selected ? "#3b82f6" : "#334155") + ";" +
                                "-fx-border-radius: 999;"
                );
            }
        };

        Runnable refreshSizeStyles = () -> {
            for (Map.Entry<ToggleButton, String> entry : sizeMap.entrySet()) {
                ToggleButton b = entry.getKey();
                boolean selected = b.isSelected();
                b.setStyle(
                        "-fx-background-color: " + (selected ? "#0f3a2a" : "#111827") + ";" +
                                "-fx-text-fill: " + (selected ? "#86efac" : "#cbd5e1") + ";" +
                                "-fx-font-size: 11px;" +
                                "-fx-font-weight: 700;" +
                                "-fx-padding: 4 8;" +
                                "-fx-background-radius: 8;" +
                                "-fx-border-color: " + (selected ? "#16a34a" : "#334155") + ";" +
                                "-fx-border-radius: 8;"
                );
            }
        };

        Runnable applySize = () -> {
            int h = spotifyHeights.getOrDefault(currentSize[0], 152);
            spotifyView.setPrefHeight(h);
            spotifyView.setMinHeight(h);
            spotifyView.setMaxHeight(h);
        };

        java.util.function.BiConsumer<String, String> loadSpotify = (preset, forcedUrl) -> {
            String key = spotifyPlaylists.containsKey(preset) ? preset : "nature";
            currentPreset[0] = key;
            String src = (forcedUrl != null && !forcedUrl.isBlank()) ? forcedUrl.trim() : spotifyPlaylists.get(key);
            label.setText(spotifyLabels.getOrDefault(key, "Nature Sounds"));
            loadingLabel.setVisible(true);
            loadingLabel.setManaged(true);
            spotifyEngine.load(src);
        };

        spotifyEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED
                    || newState == javafx.concurrent.Worker.State.FAILED
                    || newState == javafx.concurrent.Worker.State.CANCELLED) {
                loadingLabel.setVisible(false);
                loadingLabel.setManaged(false);
            }
        });

        sizeGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (newV instanceof ToggleButton tb) {
                currentSize[0] = sizeMap.getOrDefault(tb, "sm");
                applySize.run();
                refreshSizeStyles.run();
            }
        });

        presetGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (newV instanceof ToggleButton tb && tb.getUserData() instanceof String key) {
                loadSpotify.accept(key, "");
            }
            refreshPresetStyles.run();
        });

        loadCustomBtn.setOnAction(e -> loadSpotify.accept(currentPreset[0], customUrlField.getText()));
        customUrlField.setOnAction(e -> loadSpotify.accept(currentPreset[0], customUrlField.getText()));

        widget.setOnMousePressed(e -> {
            dragAnchor[0] = e.getSceneX();
            dragAnchor[1] = e.getSceneY();
            dragTranslate[0] = widget.getTranslateX();
            dragTranslate[1] = widget.getTranslateY();
        });
        widget.setOnMouseDragged(e -> {
            double dx = e.getSceneX() - dragAnchor[0];
            double dy = e.getSceneY() - dragAnchor[1];
            widget.setTranslateX(dragTranslate[0] + dx);
            widget.setTranslateY(dragTranslate[1] + dy);
        });

        Runnable showPanel = () -> {
            widget.setManaged(true);
            widget.setVisible(true);
            bubbleHint.setText("Hide Player");
        };
        Runnable hidePanel = () -> {
            widget.setVisible(false);
            widget.setManaged(false);
            bubbleHint.setText("Nature Sounds");
        };
        bubbleButton.setOnAction(e -> {
            if (widget.isVisible()) {
                hidePanel.run();
            } else {
                showPanel.run();
            }
        });
        collapseBtn.setOnAction(e -> hidePanel.run());

        Stage stage = createToolStage("Nature Sounds", root, 1040, 700);
        LocalDateTime openedAt = LocalDateTime.now();
        AtomicBoolean sessionClosed = new AtomicBoolean(false);
        AtomicBoolean loadedAnyPlaylist = new AtomicBoolean(false);

        java.util.function.Consumer<Boolean> closeAndSave = completed -> {
            if (sessionClosed.getAndSet(true)) {
                return;
            }
            closeSession(session, openedAt, completed);
        };
        closeBtn.setOnAction(e -> {
            closeAndSave.accept(loadedAnyPlaylist.get());
            stage.close();
        });
        stage.setOnCloseRequest(e -> closeAndSave.accept(loadedAnyPlaylist.get()));

        loadSpotify.accept("nature", "");
        loadedAnyPlaylist.set(true);
        refreshPresetStyles.run();
        refreshSizeStyles.run();
        applySize.run();
        showPanel.run();
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

    // Backward-compatibility for older Wellbeing.fxml still wired to #showMoodDialog
    @FXML
    private void showMoodDialog() {
        handleOpenCheckin();
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
        showCopingToolsMode();
    }

    @FXML
    private void handleStartBreathingTool() {
        copingToolsCatalog().stream()
                .filter(tool -> "breathing_exercise".equals(tool.key()))
                .findFirst()
                .ifPresent(this::openCopingTool);
    }

    @FXML
    private void handleOpenQuiz() {
        showQuizModePickerInline();
    }

    @FXML
    private void handleStartSimpleQuiz() {
        startQuiz(QuizMode.SIMPLE);
    }

    @FXML
    private void handleStartAiQuiz() {
        startQuiz(QuizMode.AI);
    }

    @FXML
    private void handleQuizPrevious() {
        hideGlobalMessage();
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
        hideGlobalMessage();
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
        hideGlobalMessage();

        int totalScore = quizAnswers.values().stream().mapToInt(Integer::intValue).sum();
        int answeredCount = Math.max(1, quizAnswers.size());
        double averageScore = (double) totalScore / answeredCount;
        String stressLevel;
        String interpretation;

        if (averageScore <= 15d) {
            stressLevel = "minimal";
            interpretation = "Your stress levels appear to be minimal. Continue practicing good self-care habits and maintain your healthy routines.";
        } else if (averageScore <= 25d) {
            stressLevel = "mild";
            interpretation = "You are experiencing mild stress. Consider incorporating some relaxation techniques and ensure you are taking regular breaks.";
        } else if (averageScore <= 35d) {
            stressLevel = "moderate";
            interpretation = "Your stress levels are moderate. It is important to prioritize self-care and consider using coping tools. If symptoms persist, consider speaking with a counselor.";
        } else {
            stressLevel = "high";
            interpretation = "You are experiencing high stress levels. Please prioritize your mental health and consider reaching out to a mental health professional for support.";
        }

        QuizStress quiz = new QuizStress();
        quiz.setQuizDate(LocalDateTime.now());
        quiz.setAnswers(new LinkedHashMap<>(quizAnswers));
        quiz.setTotalScore(totalScore);
        quiz.setStressLevel(stressLevel);
        quiz.setInterpretation(interpretation);
        quiz.setCreatedWithAi(currentQuizMode == QuizMode.AI);
        if (currentQuizMode == QuizMode.AI) {
            quiz.setAiModel("local-randomized-v1");
            quiz.setAiPromptVersion("wellbeing-quiz-ai-questions-v1");
        }
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
        startQuiz(currentQuizMode);
    }

    @FXML
    private void handleQuizBackToOverview() {
        showOverviewMode();
    }

    @FXML
    private void handleLoadAiSuggestions() {
        if (currentQuizResult == null) {
            return;
        }
        StackPane suggestionsLoadingOverlay = createAiLoadingOverlay(
                "Analyzing your results...",
                "Generating personalized AI suggestions for your current wellbeing status."
        );
        setAiLoadingVisible(suggestionsLoadingOverlay, true);
        if (quizAiSuggestionsBox != null) {
            quizAiSuggestionsBox.getChildren().setAll(suggestionsLoadingOverlay);
        }
        if (quizLoadAiSuggestionsButton != null) {
            quizLoadAiSuggestionsButton.setDisable(true);
            quizLoadAiSuggestionsButton.setText("Loading...");
        }
        if (quizAiSuggestionsMessageLabel != null) {
            quizAiSuggestionsMessageLabel.setText("Generating AI suggestions...");
        }

        QuizStress quiz = currentQuizResult;
        Task<List<AiSuggestionItem>> task = new Task<>() {
            @Override
            protected List<AiSuggestionItem> call() {
                return loadAiSuggestionsForQuiz(quiz);
            }
        };

        task.setOnSucceeded(evt -> {
            List<AiSuggestionItem> items = task.getValue();
            String fingerprint = computeSuggestionFingerprint(items);
            if (!fingerprint.isBlank() && fingerprint.equals(lastAiSuggestionFingerprint) && items.size() > 1) {
                for (int i = items.size() - 1; i > 0; i--) {
                    int j = ThreadLocalRandom.current().nextInt(i + 1);
                    AiSuggestionItem temp = items.get(i);
                    items.set(i, items.get(j));
                    items.set(j, temp);
                }
                fingerprint = computeSuggestionFingerprint(items);
            }
            lastAiSuggestionFingerprint = fingerprint;

            renderAiSuggestions(items);
            if (quizAiSuggestionsMessageLabel != null) {
                quizAiSuggestionsMessageLabel.setText("AI updated your recommendations for your current stress result.");
            }
            if (quizLoadAiSuggestionsButton != null) {
                quizLoadAiSuggestionsButton.setDisable(false);
                quizLoadAiSuggestionsButton.setText("Show AI Suggestions");
            }
        });

        task.setOnFailed(evt -> {
            renderAiSuggestions(buildDefaultAiSuggestionItems(quiz));
            if (quizAiSuggestionsMessageLabel != null) {
                quizAiSuggestionsMessageLabel.setText("Unable to load AI now. Fallback suggestions are shown.");
            }
            if (quizLoadAiSuggestionsButton != null) {
                quizLoadAiSuggestionsButton.setDisable(false);
                quizLoadAiSuggestionsButton.setText("Show AI Suggestions");
            }
        });

        Thread worker = new Thread(task, "wellbeing-ai-quiz-suggestions");
        worker.setDaemon(true);
        worker.start();
    }

    private void openQuizModePicker() {
        Label title = new Label("Choose Quiz Mode");
        title.getStyleClass().add("quiz-mode-title");
        Label subtitle = new Label("Choose between Simple quiz (admin) and AI quiz.");
        subtitle.getStyleClass().add("quiz-mode-subtitle");
        subtitle.setWrapText(true);

        Label simpleTitle = new Label("Simple Quiz");
        simpleTitle.getStyleClass().add("quiz-mode-card-title");
        Label simpleDesc = new Label("Questions created by admin.");
        simpleDesc.getStyleClass().add("quiz-mode-card-desc");
        simpleDesc.setWrapText(true);
        VBox simpleContent = new VBox(4, simpleTitle, simpleDesc);
        Button simpleButton = new Button();
        simpleButton.getStyleClass().addAll("quiz-mode-card", "quiz-mode-card-simple");
        simpleButton.setGraphic(simpleContent);
        simpleButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        simpleButton.setMaxWidth(Double.MAX_VALUE);
        simpleButton.setOnAction(e -> {
            Stage stage = (Stage) simpleButton.getScene().getWindow();
            stage.close();
            startQuiz(QuizMode.SIMPLE);
        });

        Label aiTitle = new Label("AI Quiz");
        aiTitle.getStyleClass().add("quiz-mode-card-title");
        Label aiDesc = new Label("Dynamic AI questions, non-repetitive.");
        aiDesc.getStyleClass().add("quiz-mode-card-desc");
        aiDesc.setWrapText(true);
        VBox aiContent = new VBox(4, aiTitle, aiDesc);
        Button aiButton = new Button();
        aiButton.getStyleClass().addAll("quiz-mode-card", "quiz-mode-card-ai");
        aiButton.setGraphic(aiContent);
        aiButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        aiButton.setMaxWidth(Double.MAX_VALUE);
        aiButton.setOnAction(e -> {
            Stage stage = (Stage) aiButton.getScene().getWindow();
            stage.close();
            startQuiz(QuizMode.AI);
        });

        HBox cards = new HBox(12, simpleButton, aiButton);
        HBox.setHgrow(simpleButton, Priority.ALWAYS);
        HBox.setHgrow(aiButton, Priority.ALWAYS);

        VBox root = new VBox(14, title, subtitle, cards);
        root.setPadding(new Insets(18));
        root.getStyleClass().add("quiz-mode-dialog");

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Quiz Mode");
        Scene scene = new Scene(root, 620, 250);
        Scene parentScene = statsSection != null ? statsSection.getScene() : null;
        if (parentScene != null && parentScene.getStylesheets() != null) {
            scene.getStylesheets().setAll(parentScene.getStylesheets());
            if (parentScene.getWindow() instanceof Stage owner) {
                stage.initOwner(owner);
            }
        }
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void startQuiz(QuizMode mode) {
        hideGlobalMessage();
        currentQuizMode = mode == null ? QuizMode.SIMPLE : mode;
        quizQuestions.clear();
        quizAnswers.clear();
        currentQuizIndex = 0;

        List<QuestionStress> activeQuestions;
        if (currentQuizMode == QuizMode.AI) {
            activeQuestions = generateAiQuizQuestions();
        } else {
            activeQuestions = serviceQuestionStress.findAll().stream()
                    .filter(QuestionStress::isActive)
                    .sorted(Comparator.comparingInt(QuestionStress::getQuestionNumber))
                    .toList();
        }

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

    private List<QuestionStress> generateAiQuizQuestions() {
        List<String> pool = Arrays.asList(
                "In the last 7 days, how often did you feel mentally overwhelmed by study tasks?",
                "How difficult was it to stay focused during your study sessions this week?",
                "How often did you feel physically tense (neck, shoulders, jaw) while studying?",
                "How much did stress affect your sleep quality recently?",
                "How often did you feel emotionally drained after classes or assignments?",
                "How hard was it to calm yourself after a stressful academic moment?",
                "How often did you postpone tasks because stress felt too high?",
                "How supported did you feel by your routine (breaks, food, sleep, movement)?",
                "How often did your thoughts race when you tried to rest?",
                "How much did stress reduce your motivation to study effectively?",
                "How difficult was it to manage stress before deadlines this week?",
                "How often did you feel anxious without a clear reason during the day?",
                "How much did stress affect your confidence in your academic abilities?",
                "How often did you struggle to recover energy after a long day of studying?",
                "How hard was it to keep a healthy balance between study and personal time?",
                "How often did you notice rapid breathing or heartbeat when stressed?",
                "How difficult was it to complete tasks without feeling pressure or panic?",
                "How often did you feel irritable because of accumulated stress?"
        );

        List<QuestionStress> records = new ArrayList<>();
        for (String text : pool) {
            int id = aiQuestionIdFromText(text);
            QuestionStress question = new QuestionStress();
            question.setId(id);
            question.setQuestionText(text);
            question.setActive(true);
            records.add(question);
        }

        List<QuestionStress> available = records.stream()
                .filter(q -> !aiUsedQuestionIds.contains(q.getId()))
                .collect(Collectors.toList());
        if (available.size() < QUIZ_AI_QUESTION_COUNT) {
            aiUsedQuestionIds.clear();
            available = new ArrayList<>(records);
        }

        for (int i = available.size() - 1; i > 0; i--) {
            int j = ThreadLocalRandom.current().nextInt(i + 1);
            QuestionStress temp = available.get(i);
            available.set(i, available.get(j));
            available.set(j, temp);
        }

        int count = Math.min(QUIZ_AI_QUESTION_COUNT, available.size());
        List<QuestionStress> selected = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            QuestionStress q = available.get(i);
            q.setQuestionNumber(i + 1);
            selected.add(q);
            aiUsedQuestionIds.add(q.getId());
        }
        return selected;
    }

    private int aiQuestionIdFromText(String text) {
        int hash = (text == null ? "" : text.trim().toLowerCase(Locale.ROOT)).hashCode();
        if (hash == Integer.MIN_VALUE) {
            hash = 1;
        }
        return -Math.abs(hash);
    }

    private void renderCurrentQuizQuestion() {
        if (quizQuestions.isEmpty()) {
            return;
        }
        hideGlobalMessage();
        QuestionStress question = quizQuestions.get(currentQuizIndex);

        quizCurrentQuestionLabel.setText(String.valueOf(currentQuizIndex + 1));
        quizQuestionTitleLabel.setText("Question " + (currentQuizIndex + 1));
        quizQuestionTextLabel.setText(question.getQuestionText());
        quizOptionsBox.getChildren().clear();

        ToggleGroup group = new ToggleGroup();
        Map<Integer, String> options = Map.of(
                1, "Not at all",
                2, "A little",
                3, "Moderately",
                4, "Very much"
        );

        for (int value = 1; value <= 4; value++) {
            RadioButton option = new RadioButton(options.get(value));
            option.setUserData(value);
            option.setToggleGroup(group);
            option.getStyleClass().addAll("text-body", "quiz-option-radio", "quiz-option-card");
            option.setWrapText(true);
            option.setPrefWidth(Double.MAX_VALUE);
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

        Runnable refreshOptionSelectionStyle = () -> {
            for (Toggle toggle : group.getToggles()) {
                if (!(toggle instanceof RadioButton rb)) {
                    continue;
                }
                rb.getStyleClass().remove("quiz-option-selected");
            }
            Toggle selected = group.getSelectedToggle();
            if (selected instanceof RadioButton selectedButton
                    && !selectedButton.getStyleClass().contains("quiz-option-selected")) {
                selectedButton.getStyleClass().add("quiz-option-selected");
            }
        };

        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null && newToggle.getUserData() instanceof Integer answerValue) {
                quizAnswers.put(question.getId(), answerValue);
                updateQuizProgress();
                hideGlobalMessage();
            }
            refreshOptionSelectionStyle.run();
        });
        refreshOptionSelectionStyle.run();

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
        currentQuizResult = quiz;
        resetAiSuggestionsSection();
        int maxScore = Math.max(40, quizQuestions.size() * 40);
        quizResultScoreLabel.setText(quiz.getTotalScore() + " / " + maxScore);
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

    private void resetAiSuggestionsSection() {
        if (quizAiSuggestionsBox != null) {
            quizAiSuggestionsBox.getChildren().clear();
        }
        if (quizAiSuggestionsMessageLabel != null) {
            quizAiSuggestionsMessageLabel.setText("Click the button to generate extra recommendations adapted to your stress result.");
        }
        if (quizSelectedRecommendationBox != null) {
            quizSelectedRecommendationBox.setVisible(false);
            quizSelectedRecommendationBox.setManaged(false);
        }
        if (quizSelectedRecommendationTitleLabel != null) {
            quizSelectedRecommendationTitleLabel.setText("");
        }
        if (quizSelectedRecommendationContentLabel != null) {
            quizSelectedRecommendationContentLabel.setText("");
        }
        if (quizLoadAiSuggestionsButton != null) {
            quizLoadAiSuggestionsButton.setDisable(false);
            quizLoadAiSuggestionsButton.setText("Show AI Suggestions");
        }
    }

    private List<AiSuggestionItem> loadAiSuggestionsForQuiz(QuizStress quiz) {
        if (quiz == null) {
            return List.of();
        }
        int answersCount = Math.max(1, quiz.getAnswers() == null ? 0 : quiz.getAnswers().size());
        int avgScore = (int) Math.round((double) quiz.getTotalScore() / answersCount);
        int stressLevel10 = Math.max(1, Math.min(10, (int) Math.round(avgScore / 4.0)));
        String moodHint = switch (quiz.getStressLevel()) {
            case "minimal", "mild" -> "okay";
            case "moderate", "high" -> "stressed";
            default -> "okay";
        };

        List<WellbeingAiService.RecommendationItem> generated = wellbeingAiService.generateRecommendations(
                stressLevel10,
                moodHint,
                List.of("quiz_result", "stress_level:" + quiz.getStressLevel(), "score:" + quiz.getTotalScore())
        );

        List<AiSuggestionItem> items = new ArrayList<>();
        for (WellbeingAiService.RecommendationItem item : generated) {
            if (item == null) {
                continue;
            }
            String title = item.title() == null ? "" : item.title().trim();
            String content = item.description() == null ? "" : item.description().trim();
            if (title.isBlank() || content.isBlank()) {
                continue;
            }
            items.add(new AiSuggestionItem(title, content));
        }
        if (items.isEmpty()) {
            items.addAll(buildDefaultAiSuggestionItems(quiz));
        }
        return items;
    }

    private void renderAiSuggestions(List<AiSuggestionItem> items) {
        if (quizAiSuggestionsBox == null) {
            return;
        }
        quizAiSuggestionsBox.getChildren().clear();
        if (items == null || items.isEmpty()) {
            Label empty = new Label("No AI suggestions available right now.");
            empty.getStyleClass().add("text-small");
            quizAiSuggestionsBox.getChildren().add(empty);
            return;
        }

        for (AiSuggestionItem item : items) {
            VBox card = new VBox(6);
            card.getStyleClass().addAll("wellbeing-input-card", "quiz-ai-suggestion-card");
            card.setPadding(new Insets(14));

            Label title = new Label(item.title());
            title.getStyleClass().add("quiz-ai-suggestion-title");
            title.setWrapText(true);

            Label content = new Label(item.content());
            content.getStyleClass().add("quiz-ai-suggestion-content");
            content.setWrapText(true);

            card.getChildren().addAll(title, content);
            card.setOnMouseClicked(evt -> selectAiSuggestion(item, card));
            quizAiSuggestionsBox.getChildren().add(card);
        }
    }

    private void selectAiSuggestion(AiSuggestionItem item, VBox selectedCard) {
        if (quizAiSuggestionsBox != null) {
            for (Node node : quizAiSuggestionsBox.getChildren()) {
                if (node instanceof VBox card) {
                    card.getStyleClass().remove("quiz-ai-suggestion-card-selected");
                }
            }
        }
        if (selectedCard != null && !selectedCard.getStyleClass().contains("quiz-ai-suggestion-card-selected")) {
            selectedCard.getStyleClass().add("quiz-ai-suggestion-card-selected");
        }
        if (quizSelectedRecommendationTitleLabel != null) {
            quizSelectedRecommendationTitleLabel.setText(item.title());
        }
        if (quizSelectedRecommendationContentLabel != null) {
            quizSelectedRecommendationContentLabel.setText(item.content());
        }
        if (quizSelectedRecommendationBox != null) {
            quizSelectedRecommendationBox.setManaged(true);
            quizSelectedRecommendationBox.setVisible(true);
        }
    }

    private String computeSuggestionFingerprint(List<AiSuggestionItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (AiSuggestionItem item : items) {
            sb.append(item.title()).append('|').append(item.content()).append(';');
        }
        return Integer.toHexString(sb.toString().hashCode());
    }

    private List<AiSuggestionItem> buildDefaultAiSuggestionItems(QuizStress quiz) {
        Map<String, String> actionTitles = new HashMap<>();
        actionTitles.put("breathing", "Breathing Reset");
        actionTitles.put("meditation", "Mini Meditation");
        actionTitles.put("sleep", "Sleep Recovery");
        actionTitles.put("boundaries", "Healthy Boundaries");
        actionTitles.put("pomodoro", "Focus Sprint");
        actionTitles.put("hydration", "Hydration Boost");
        actionTitles.put("breaks", "Active Break");
        actionTitles.put("gratitude", "Gratitude Note");

        Map<String, List<String>> actionTips = new HashMap<>();
        actionTips.put("breathing", List.of(
                "Try 4-7-8 breathing for 3 rounds before studying.",
                "Inhale 4s, exhale 6s for 2 minutes to lower tension.",
                "Take one deep breath each time you switch tasks."
        ));
        actionTips.put("meditation", List.of(
                "Do a 5-minute body scan to relax your mind.",
                "Close your eyes and observe breathing for 3 minutes.",
                "Use a short mindfulness break between classes."
        ));
        actionTips.put("sleep", List.of(
                "Set a fixed sleep time tonight and reduce screen light.",
                "Stop caffeine late in the day to improve sleep quality.",
                "Create a calm 20-minute wind-down before bed."
        ));
        actionTips.put("boundaries", List.of(
                "Say no to one non-essential task today.",
                "Protect one uninterrupted study block in your schedule.",
                "Set a clear stop time for work tonight."
        ));
        actionTips.put("pomodoro", List.of(
                "Try 25 minutes focus + 5 minutes break for 3 cycles.",
                "Set one micro-goal per focus block to reduce pressure.",
                "Pause after each cycle to stretch and reset."
        ));
        actionTips.put("hydration", List.of(
                "Drink one glass of water now, then every study break.",
                "Keep a water bottle visible during your sessions.",
                "Pair each completed task with a hydration reminder."
        ));
        actionTips.put("breaks", List.of(
                "Walk for 3-5 minutes between study blocks.",
                "Do neck and shoulder release every hour.",
                "Stand up and stretch before starting a new topic."
        ));
        actionTips.put("gratitude", List.of(
                "Write one thing you handled well today.",
                "List two small wins before ending the day.",
                "Add a short gratitude sentence after each study session."
        ));

        List<String> keys;
        switch (quiz.getStressLevel()) {
            case "high" -> keys = new ArrayList<>(List.of("breathing", "meditation", "sleep", "boundaries"));
            case "moderate" -> keys = new ArrayList<>(List.of("pomodoro", "breaks", "hydration", "breathing"));
            default -> keys = new ArrayList<>(List.of("gratitude", "sleep", "hydration", "breaks"));
        }

        for (int i = keys.size() - 1; i > 0; i--) {
            int j = ThreadLocalRandom.current().nextInt(i + 1);
            String temp = keys.get(i);
            keys.set(i, keys.get(j));
            keys.set(j, temp);
        }

        List<AiSuggestionItem> items = new ArrayList<>();
        int count = Math.min(3, keys.size());
        for (int i = 0; i < count; i++) {
            String key = keys.get(i);
            List<String> tips = actionTips.getOrDefault(key, List.of("Take one small step now and keep it consistent today."));
            String tip = tips.get(ThreadLocalRandom.current().nextInt(tips.size()));
            items.add(new AiSuggestionItem(actionTitles.getOrDefault(key, capitalize(key)), tip));
        }
        return items;
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
        stopGlobalMessageTimer();
        globalMessageLabel.setText(message == null ? "" : message);
        globalMessageLabel.setStyle(
                "-fx-background-color: " + (error ? "#dc2626" : "#16a34a") + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 10 14 10 14;" +
                        "-fx-background-radius: 8;"
        );
        globalMessageLabel.setVisible(true);
        globalMessageLabel.setManaged(true);
        globalMessageTimer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(GLOBAL_MESSAGE_HIDE_SECONDS), event -> hideGlobalMessage()));
        globalMessageTimer.setCycleCount(1);
        globalMessageTimer.play();
    }

    private void hideGlobalMessage() {
        if (globalMessageLabel == null) {
            return;
        }
        stopGlobalMessageTimer();
        globalMessageLabel.setText("");
        globalMessageLabel.setVisible(false);
        globalMessageLabel.setManaged(false);
    }

    private void stopGlobalMessageTimer() {
        if (globalMessageTimer != null) {
            globalMessageTimer.stop();
            globalMessageTimer = null;
        }
    }

    private void showOverviewMode() {
        handleCloseInlineTool();
        hideGlobalMessage();
        setNodeVisibility(statsSection, true);
        setNodeVisibility(overviewSection, true);
        setNodeVisibility(historySection, false);
        setNodeVisibility(toolsSection, false);
        setNodeVisibility(formSection, false);
        setNodeVisibility(quizModeSection, false);
        setNodeVisibility(quizSection, false);
        setNodeVisibility(quizResultsSection, false);
    }

    private void showCopingToolsMode() {
        hideGlobalMessage();
        setNodeVisibility(statsSection, false);
        setNodeVisibility(overviewSection, false);
        setNodeVisibility(historySection, false);
        setNodeVisibility(formSection, false);
        setNodeVisibility(quizModeSection, false);
        setNodeVisibility(quizSection, false);
        setNodeVisibility(quizResultsSection, false);
        setNodeVisibility(toolsSection, true);
    }

    private void showCheckinMode() {
        handleCloseInlineTool();
        hideGlobalMessage();
        setNodeVisibility(statsSection, false);
        setNodeVisibility(overviewSection, false);
        setNodeVisibility(historySection, false);
        setNodeVisibility(toolsSection, false);
        setNodeVisibility(formSection, true);
        setNodeVisibility(quizModeSection, false);
        setNodeVisibility(quizSection, false);
        setNodeVisibility(quizResultsSection, false);
    }

    private void showHistoryMode() {
        handleCloseInlineTool();
        hideGlobalMessage();
        setNodeVisibility(statsSection, false);
        setNodeVisibility(overviewSection, false);
        setNodeVisibility(toolsSection, false);
        setNodeVisibility(formSection, false);
        setNodeVisibility(historySection, true);
        setNodeVisibility(quizModeSection, false);
        setNodeVisibility(quizSection, false);
        setNodeVisibility(quizResultsSection, false);
    }

    private void showQuizMode() {
        handleCloseInlineTool();
        hideGlobalMessage();
        setNodeVisibility(statsSection, false);
        setNodeVisibility(overviewSection, false);
        setNodeVisibility(historySection, false);
        setNodeVisibility(toolsSection, false);
        setNodeVisibility(formSection, false);
        setNodeVisibility(quizModeSection, false);
        setNodeVisibility(quizResultsSection, false);
        setNodeVisibility(quizSection, true);
    }

    private void showQuizResultsMode() {
        handleCloseInlineTool();
        hideGlobalMessage();
        setNodeVisibility(statsSection, false);
        setNodeVisibility(overviewSection, false);
        setNodeVisibility(historySection, false);
        setNodeVisibility(toolsSection, false);
        setNodeVisibility(formSection, false);
        setNodeVisibility(quizModeSection, false);
        setNodeVisibility(quizSection, false);
        setNodeVisibility(quizResultsSection, true);
    }

    private void showQuizModePickerInline() {
        handleCloseInlineTool();
        hideGlobalMessage();
        setNodeVisibility(statsSection, false);
        setNodeVisibility(overviewSection, false);
        setNodeVisibility(historySection, false);
        setNodeVisibility(toolsSection, false);
        setNodeVisibility(formSection, false);
        setNodeVisibility(quizSection, false);
        setNodeVisibility(quizResultsSection, false);
        setNodeVisibility(quizModeSection, true);
    }

    private void setNodeVisibility(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
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
                new MindfulnessSession("Deep Breathing", "5 min", "fth-activity", "primary"),
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
