package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.Planning;
import com.studyflow.models.Seance;
import com.studyflow.models.TypeSeance;
import com.studyflow.models.User;
import com.studyflow.services.ServicePlanning;
import com.studyflow.services.ServiceSeance;
import com.studyflow.services.AIPlanningService;
import com.studyflow.services.FootballDataService;
import com.studyflow.services.PlanningEmailNotificationService;
import com.studyflow.services.ServiceTypeSeance;
import com.studyflow.utils.UserSession;
import javafx.application.Platform;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DateCell;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlanningController implements Initializable {

    private static final double BASE_CALENDAR_ROW_HEIGHT = 136;
    private static final double CALENDAR_ENTRY_HEIGHT_STEP = 62;

    private static final List<String> COLOR_PALETTE = List.of(
            "#8B5CF6", "#10B981", "#F59E0B", "#F43F5E", "#F97316", "#3B82F6", "#14B8A6", "#EAB308"
    );
    private static final String DRAG_ENTRY_PREFIX = "planning-entry:";
    private static final String SEARCH_SCOPE_ALL = "All fields";
    private static final String SEARCH_SCOPE_TITLE = "Session title";
    private static final String SEARCH_SCOPE_DATE = "Date";
    private static final String SEARCH_SCOPE_TIME = "Time";
    private static final String SEARCH_SCOPE_COLOR = "Color";
    private static final String SEARCH_SCOPE_FEEDBACK = "Feedback";

    private static final String UPCOMING_FILTER_ALL = "All upcoming";
    private static final String UPCOMING_FILTER_TODAY = "Today";
    private static final String UPCOMING_FILTER_WEEK = "This week";
    private static final String UPCOMING_FILTER_MONTH = "This month";

    private static final String UPCOMING_SORT_NEAREST = "Date (nearest first)";
    private static final String UPCOMING_SORT_LATEST = "Date (latest first)";
    private static final String UPCOMING_SORT_START_TIME = "Start time";
    private static final String UPCOMING_SORT_TITLE_AZ = "Session title (A-Z)";
    private static final String UPCOMING_SORT_TITLE_ZA = "Session title (Z-A)";

    private static final String UPCOMING_MODE_ALL_NEAREST = "All upcoming - nearest";
    private static final String UPCOMING_MODE_ALL_LATEST = "All upcoming - latest";
    private static final String UPCOMING_MODE_TODAY = "Today - nearest";
    private static final String UPCOMING_MODE_WEEK = "This week - nearest";
    private static final String UPCOMING_MODE_MONTH = "This month - nearest";
    private static final String UPCOMING_MODE_TITLE_AZ = "Title search - A to Z";
    private static final String UPCOMING_MODE_TITLE_ZA = "Title search - Z to A";
    private static final String UPCOMING_MODE_TIME = "Time search - earliest";
    private static final String UPCOMING_MODE_COLOR = "Color search - nearest";
    private static final String UPCOMING_MODE_FEEDBACK = "Feedback search - nearest";
    private static final String SESSION_TYPE_MATCH = "match";
    private static final String SESSION_TYPE_DAY_OFF = "day off";
    private static final String SESSION_TYPE_DAYOFF = "dayoff";
    private static final Map<String, String> KNOWN_TEAM_ACRONYMS = Map.ofEntries(
            Map.entry("atletico madrid", "ATM"),
            Map.entry("bayern munich", "FCB"),
            Map.entry("bayern munchen", "FCB"),
            Map.entry("fc barcelona", "FCB"),
            Map.entry("barcelona", "FCB"),
            Map.entry("real madrid", "RMA"),
            Map.entry("manchester city", "MCI"),
            Map.entry("manchester united", "MUN"),
            Map.entry("paris saint germain", "PSG"),
            Map.entry("paris saint-germain", "PSG"),
            Map.entry("juventus", "JUV"),
            Map.entry("arsenal", "ARS"),
            Map.entry("liverpool", "LIV"),
            Map.entry("chelsea", "CHE"),
            Map.entry("inter", "INT"),
            Map.entry("ac milan", "MIL")
    );
    private static final LocalTime DAY_OFF_START_TIME = LocalTime.of(0, 0);
    private static final LocalTime DAY_OFF_END_TIME = LocalTime.of(23, 59);
    private static final String DAY_OFF_COLOR_HEX = "#10B981";

    private static final String ASSISTANT_MODE_CHAT = "Chat mode";
    private static final String ASSISTANT_MODE_TERMINAL = "Terminal mode";
    private static final String ANTHROPIC_API_KEY_ENV = "ANTHROPIC_API_KEY";
    private static final Pattern ASSISTANT_TIME_PATTERN = Pattern.compile("(?<!\\d)([01]?\\d|2[0-3])(?:[:h]([0-5]\\d))?(?!\\d)");
    private static final Pattern ASSISTANT_ISO_DATE_PATTERN = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
    private static final Pattern ASSISTANT_SHORT_DATE_PATTERN = Pattern.compile("(?<!\\d)(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?(?!\\d)");
    private static final Pattern ASSISTANT_TIME_RANGE_PATTERN = Pattern.compile(
            "(?i)\\b(?:from\\s+)?([0-2]?\\d(?:[:h.]\\d{1,2})?\\s*(?:am|pm)?)\\s*(?:to|-|until|jusqu(?:'| )a|a|à)\\s*([0-2]?\\d(?:[:h.]\\d{1,2})?\\s*(?:am|pm)?)\\b"
    );
    private static final LocalTime ASSISTANT_MIN_TIME = LocalTime.of(6, 0);
    private static final LocalTime ASSISTANT_MAX_TIME = LocalTime.of(23, 30);
    private static final int ASSISTANT_TIME_STEP_MINUTES = 30;

    @FXML private Label currentMonthLabel;
    @FXML private Label todayDateLabel;
    @FXML private Label pageMessageLabel;
    @FXML private Label planningHoursValueLabel;
    @FXML private Label planningHoursTrendLabel;
    @FXML private Label planningCompletionValueLabel;
    @FXML private Label planningCompletionTrendLabel;
    @FXML private Label planningFeedbackScoreValueLabel;
    @FXML private Label planningFeedbackScoreTrendLabel;
    @FXML private Label planningStreakValueLabel;
    @FXML private Label planningStreakTrendLabel;
    @FXML private Label liveClockLabel;
    @FXML private Label liveClockDateLabel;
    @FXML private VBox feedbackPendingBox;
    @FXML private Label feedbackPendingLabel;
    @FXML private Button feedbackOpenQueueButton;
    @FXML private VBox feedbackPage;
    @FXML private VBox feedbackPickerPage;
    @FXML private VBox feedbackPendingListContainer;
    @FXML private ListView<Planning> feedbackPendingListView;
    @FXML private Label feedbackSelectedSessionLabel;
    @FXML private Label formMessageLabel;
    @FXML private Label planningFormTitleLabel;
    @FXML private GridPane calendarGrid;
    @FXML private HBox planningBoardView;
    @FXML private VBox planningFormPage;
    @FXML private ComboBox<Seance> sessionComboBox;
    @FXML private Label sessionErrorLabel;
    @FXML private DatePicker planningDatePicker;
    @FXML private Label dateErrorLabel;
    @FXML private ComboBox<LocalTime> startTimeComboBox;
    @FXML private Label startTimeErrorLabel;
    @FXML private ComboBox<LocalTime> endTimeComboBox;
    @FXML private Label endTimeErrorLabel;
    @FXML private ColorPicker colorPicker;
    @FXML private Label colorErrorLabel;
    @FXML private VBox planningEditFeedbackBox;
    @FXML private ComboBox<String> planningEditFeedbackComboBox;
    @FXML private ToggleButton feedbackVeryBadButton;
    @FXML private ToggleButton feedbackBadButton;
    @FXML private ToggleButton feedbackMediumButton;
    @FXML private ToggleButton feedbackGoodButton;
    @FXML private ToggleButton feedbackExcellentButton;
    @FXML private Label feedbackErrorLabel;
    @FXML private Button saveFeedbackButton;
    @FXML private Button savePlanningButton;
    @FXML private ListView<Planning> todayEventsListView;
    @FXML private ListView<Planning> upcomingEventsListView;
    @FXML private TextField upcomingSearchField;
    @FXML private ComboBox<String> upcomingUnifiedFilterComboBox;
    @FXML private ComboBox<FootballDataService.TeamOption> matchTeamComboBox;
    @FXML private Label matchStatusLabel;
    @FXML private Label matchSummaryLabel;
    @FXML private Label matchCompetitionLabel;
    @FXML private Label matchKickoffLabel;
    @FXML private ImageView matchHomeLogoImageView;
    @FXML private ImageView matchAwayLogoImageView;
    @FXML private Label matchHomeTeamLabel;
    @FXML private Label matchAwayTeamLabel;
    @FXML private Button matchPrefillButton;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatBox;
    @FXML private TextField assistantInputField;
    @FXML private Label assistantHintLabel;

    private final ServicePlanning servicePlanning = new ServicePlanning();
    private final ServiceSeance serviceSeance = new ServiceSeance();
    private final ServiceTypeSeance serviceTypeSeance = new ServiceTypeSeance();
    private final PlanningEmailNotificationService planningEmailNotificationService = new PlanningEmailNotificationService();
    private final AIPlanningService claudePlanningService = new AIPlanningService();
    private final FootballDataService footballDataService = new FootballDataService();
    private final Preferences planningPreferences = Preferences.userNodeForPackage(PlanningController.class);
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private final DateTimeFormatter fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH);
    private final DateTimeFormatter liveClockTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH);
    private final DateTimeFormatter liveClockDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d yyyy", Locale.ENGLISH);
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter calendarEntryFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter shortDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);

    private YearMonth currentYearMonth;
    private final ToggleGroup feedbackToggleGroup = new ToggleGroup();
    private Planning selectedFeedbackEntry;
    private Planning editingPlanning;
    private List<Planning> allPlanningEntries = new ArrayList<>();
    private Map<LocalDate, List<Planning>> planningByDate = new HashMap<>();
    private final Map<Label, PauseTransition> messageTimers = new HashMap<>();
    private final List<String> assistantUserHistory = new ArrayList<>();
    private final Set<Integer> aiGeneratedPlanningIds = new HashSet<>();
    private final Random assistantRandom = new Random();
    private int assistantHistoryIndex = 0;
    private String assistantDraftInput = "";
    private String assistantSessionResolutionError;
    private HBox assistantTypingRow;
    private AssistantCommand pendingAssistantCommand;
    private FootballDataService.NextMatch selectedNextMatch;
    private Timeline liveClockTimeline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentYearMonth = YearMonth.now();
        configureSessionComboBox();
        configureTimeComboBoxes();
        configurePlanningEditFeedbackField();
        configurePlanningDatePickerReadability();
        configureListViews();
        configureFormInteractions();
        configureFeedbackControls();
        configureUpcomingControls();
        configureMatchPlannerControls();
        configureAssistantControls();
        configureLiveClock();
        loadSessions();
        refreshPlanningData();
        resetForm();
        showForm(false);
        if (feedbackPage != null) {
            feedbackPage.setManaged(false);
            feedbackPage.setVisible(false);
        }
        if (feedbackPickerPage != null) {
            feedbackPickerPage.setManaged(false);
            feedbackPickerPage.setVisible(false);
        }
        addMessage("Hello! Ask me to plan or remove sessions. Example: \"Plan a math session tomorrow at 14:00\".", false);
    }

    private void configureLiveClock() {
        if (liveClockLabel == null || liveClockDateLabel == null) {
            return;
        }

        Runnable refreshClock = () -> {
            LocalDateTime now = LocalDateTime.now();
            liveClockLabel.setText(now.format(liveClockTimeFormatter));
            liveClockDateLabel.setText(now.format(liveClockDateFormatter));
        };
        refreshClock.run();

        liveClockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> refreshClock.run()));
        liveClockTimeline.setCycleCount(Animation.INDEFINITE);
        liveClockTimeline.play();
    }

    // Force explicit day-number colors to keep DatePicker readable in both themes.
    private void configurePlanningDatePickerReadability() {
        if (planningDatePicker == null) {
            return;
        }

        planningDatePicker.setDayCellFactory(datePicker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setStyle("");
                    return;
                }

                boolean isLightTheme = isLightThemeActive();
                String normalColor = isLightTheme ? "#0F172A" : "#E2E8F0";
                String outsideMonthColor = isLightTheme ? "#94A3B8" : "#64748B";
                Paint normalPaint = Color.web(normalColor);
                Paint outsidePaint = Color.web(outsideMonthColor);

                boolean outsideMonth = getStyleClass().contains("previous-month")
                        || getStyleClass().contains("next-month");

                if (isSelected()) {
                    setTextFill(Color.WHITE);
                    setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-opacity: 1;");
                } else {
                    setTextFill(outsideMonth ? outsidePaint : normalPaint);
                    String cellColor = outsideMonth ? outsideMonthColor : normalColor;
                    setStyle("-fx-text-fill: " + cellColor + "; -fx-font-weight: 600; -fx-opacity: 1;");
                }
            }
        });
    }

    private boolean isLightThemeActive() {
        Scene scene = App.getScene();
        if (scene == null) {
            return App.getCurrentTheme() == App.Theme.LIGHT;
        }
        return scene.getStylesheets().stream().anyMatch(css -> css != null && css.contains("light-theme.css"));
    }

    @FXML
    private void handlePreviousMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
        updateCalendar();
    }

    @FXML
    private void handleNextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
        updateCalendar();
    }

    @FXML
    private void handleViewSessions() {
        MainController mainController = MainController.getInstance();
        if (mainController != null) {
            mainController.showSessions();
            return;
        }
        try {
            App.setRoot("views/Sessions");
        } catch (IOException exception) {
            showErrorOn(pageMessageLabel, "Unable to open sessions page: " + exception.getMessage());
        }
    }

    @FXML
    private void handleManageSessionTypes() {
        MainController mainController = MainController.getInstance();
        if (mainController != null) {
            mainController.showSessionTypes();
            return;
        }
        try {
            App.setRoot("views/TypeSeance");
        } catch (IOException exception) {
            showErrorOn(pageMessageLabel, "Unable to open session types page: " + exception.getMessage());
        }
    }

    @FXML
    private void handleAddPlanning() {
        resetForm();
        planningDatePicker.setValue(LocalDate.now());
        colorPicker.setValue(Color.web(findAvailableColor(LocalDate.now(), null)));
        planningFormTitleLabel.setText("Add Planning");
        showForm(true);
        showInfo("Choose one session, one date, one time range and one unique color for that day.");
    }

    @FXML
    private void handleCancelPlanning() {
        resetForm();
        showForm(false);
    }

    @FXML
    private void handleSavePlanning() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            showError("No logged-in user found.");
            return;
        }

        Seance selectedSeance = sessionComboBox.getValue();
        LocalDate planningDate = planningDatePicker.getValue();
        boolean dayOffSelection = isDayOffSeance(selectedSeance);
        LocalTime startTime = dayOffSelection ? DAY_OFF_START_TIME : startTimeComboBox.getValue();
        LocalTime endTime = dayOffSelection ? DAY_OFF_END_TIME : endTimeComboBox.getValue();
        String selectedColor = dayOffSelection ? DAY_OFF_COLOR_HEX : toHex(colorPicker.getValue());

        clearPlanningValidationErrors();

        boolean hasError = !validateSession(selectedSeance, true)
                | !validateDate(planningDate, true)
                | (!dayOffSelection && !validateTimeRange(startTime, endTime, true))
                | !validateTimeOverlap(currentUser.getId(), planningDate, startTime, endTime, selectedSeance, true)
                | !validateColorForDate(planningDate, selectedColor, true);

        if (hasError) {
            showError("Please fix the highlighted fields.");
            return;
        }

        Planning planningEntry = editingPlanning == null ? new Planning() : editingPlanning;
        planningEntry.setUserId(currentUser.getId());
        planningEntry.setSeanceId(selectedSeance.getId());
        planningEntry.setSeanceTitle(selectedSeance.getTitre());
        planningEntry.setPlanningDate(planningDate);
        planningEntry.setStartTime(startTime);
        planningEntry.setEndTime(endTime);
        planningEntry.setColorHex(selectedColor);
        if (editingPlanning != null && planningEditFeedbackBox != null && planningEditFeedbackBox.isManaged()) {
            planningEntry.setFeedback(toFeedbackCode(planningEditFeedbackComboBox == null ? null : planningEditFeedbackComboBox.getValue()));
        }

        boolean isCreation = editingPlanning == null;
        if (isCreation) {
            servicePlanning.add(planningEntry);
            sendPlanningCreationEmailAsync(currentUser, planningEntry);
        } else {
            servicePlanning.update(planningEntry);
        }

        refreshPlanningData();
        resetForm();
        showForm(false);
        showInfo(isCreation ? "Planning saved successfully." : "Planning updated successfully.");
    }

    private void configureSessionComboBox() {
        sessionComboBox.setCellFactory(combo -> new ListCell<>() {
            @Override
            protected void updateItem(Seance item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitre());
            }
        });
        sessionComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Seance item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitre());
            }
        });
    }

    private void configureTimeComboBoxes() {
        List<LocalTime> timeSlots = new ArrayList<>();
        int startMinutes = 6 * 60;
        int endMinutes = 23 * 60 + 30;
        for (int minutes = startMinutes; minutes <= endMinutes; minutes += 30) {
            timeSlots.add(LocalTime.of(minutes / 60, minutes % 60));
        }
        startTimeComboBox.setItems(FXCollections.observableArrayList(timeSlots));
        endTimeComboBox.setItems(FXCollections.observableArrayList(timeSlots));
        configureTimeCell(startTimeComboBox);
        configureTimeCell(endTimeComboBox);
    }

    private void configurePlanningEditFeedbackField() {
        if (planningEditFeedbackComboBox == null) {
            return;
        }
        planningEditFeedbackComboBox.setItems(FXCollections.observableArrayList(
                "Very Bad",
                "Bad",
                "Medium",
                "Good",
                "Excellent"
        ));
        showPlanningEditFeedbackEditor(false);
    }

    @FXML
    private void handleClearPlanningFeedback() {
        if (planningEditFeedbackComboBox != null) {
            planningEditFeedbackComboBox.setValue(null);
        }
    }

    private void configureTimeCell(ComboBox<LocalTime> comboBox) {
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalTime object) {
                return object == null ? "" : object.format(timeFormatter);
            }

            @Override
            public LocalTime fromString(String string) {
                if (string == null || string.isBlank()) {
                    return null;
                }
                return LocalTime.parse(string, timeFormatter);
            }
        });
        comboBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(LocalTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(timeFormatter));
            }
        });
        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(LocalTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(timeFormatter));
            }
        });
    }

    private void configureListViews() {
        todayEventsListView.setCellFactory(list -> new PlanningEntryCell());
        upcomingEventsListView.setCellFactory(list -> new PlanningEntryCell());
        upcomingEventsListView.setFixedCellSize(72);
        if (feedbackPendingListView != null) {
            feedbackPendingListView.setCellFactory(list -> new FeedbackPendingCell());
        }
    }

    private void configureFormInteractions() {
        planningDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && editingPlanning == null) {
                colorPicker.setValue(Color.web(findAvailableColor(newValue, null)));
            }
            if (dateErrorLabel != null && dateErrorLabel.isVisible()) {
                validateDate(newValue, true);
            }
            if (colorErrorLabel != null && colorErrorLabel.isVisible()) {
                validateColorForDate(newValue, colorPicker.getValue() == null ? null : toHex(colorPicker.getValue()), true);
            }
        });

        sessionComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateDayOffTimeMode(newValue);
            if (sessionErrorLabel != null && sessionErrorLabel.isVisible()) {
                validateSession(newValue, true);
            }
        });

        startTimeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (startTimeErrorLabel != null && startTimeErrorLabel.isVisible()) {
                validateTimeRange(startTimeComboBox.getValue(), endTimeComboBox.getValue(), true);
            }
        });

        endTimeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (endTimeErrorLabel != null && endTimeErrorLabel.isVisible()) {
                validateTimeRange(startTimeComboBox.getValue(), endTimeComboBox.getValue(), true);
            }
        });

        colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (colorErrorLabel != null && colorErrorLabel.isVisible()) {
                validateColorForDate(planningDatePicker.getValue(), newValue == null ? null : toHex(newValue), true);
            }
        });
    }

    private void configureFeedbackControls() {
        configureFeedbackButton(feedbackVeryBadButton, "1");
        configureFeedbackButton(feedbackBadButton, "2");
        configureFeedbackButton(feedbackMediumButton, "3");
        configureFeedbackButton(feedbackGoodButton, "4");
        configureFeedbackButton(feedbackExcellentButton, "5");

        feedbackToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (feedbackErrorLabel != null && feedbackErrorLabel.isVisible()) {
                validateFeedbackSelection(true);
            }
        });

        if (feedbackPendingListView != null) {
            feedbackPendingListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && feedbackPage != null && feedbackPage.isVisible()) {
                    openFeedbackPickerFor(newValue);
                }
            });
        }

    }

    @FXML
    private void handleOpenFeedbackQueue() {
        refreshFeedbackQueue();
        showFeedbackPage(true);
        hideMessage();
    }

    @FXML
    private void handleBackFromFeedback() {
        showFeedbackPage(false);
    }

    @FXML
    private void handleBackFromFeedbackPicker() {
        showFeedbackPickerPage(false);
        showFeedbackPage(true);
    }

    @FXML
    private void handleSaveFeedback() {
        if (selectedFeedbackEntry == null) {
            markFeedbackInvalid("Select a finished session first.");
            return;
        }
        if (!validateFeedbackSelection(true)) {
            return;
        }

        selectedFeedbackEntry.setFeedback(blankToNull(getSelectedFeedback()));
        servicePlanning.update(selectedFeedbackEntry);
        if (feedbackPendingListView != null) {
            feedbackPendingListView.getItems().removeIf(entry -> entry.getId() == selectedFeedbackEntry.getId());
        }
        refreshPlanningData();
        refreshFeedbackQueue();
        selectedFeedbackEntry = null;
        feedbackToggleGroup.selectToggle(null);
        showFeedbackPickerPage(false);
        if (hasPendingFeedbackEntries()) {
            showFeedbackPage(true);
            showInfo("Feedback saved successfully.");
        } else {
            showFeedbackPage(false);
            showInfo("Feedback saved successfully. All finished sessions are now rated.");
        }
    }

    private boolean hasPendingFeedbackEntries() {
        return allPlanningEntries.stream()
                .filter(entry -> isSessionCompleted(entry.getPlanningDate(), entry.getEndTime()))
                .anyMatch(this::isAwaitingFeedback);
    }

    private void openFeedbackPickerFor(Planning entry) {
        selectedFeedbackEntry = entry;
        feedbackSelectedSessionLabel.setText(entry.getSeanceTitle() + " - " + formatEntryDateTime(entry));
        selectFeedback(entry.getFeedback());
        clearFeedbackValidation();
        showFeedbackPickerPage(true);
    }

    private void configureFeedbackButton(ToggleButton button, String feedbackValue) {
        if (button == null) {
            return;
        }
        button.setToggleGroup(feedbackToggleGroup);
        button.setUserData(feedbackValue);
        if (!button.getStyleClass().contains("planning-feedback-chip")) {
            button.getStyleClass().add("planning-feedback-chip");
        }
    }

    private void configureUpcomingControls() {
        upcomingUnifiedFilterComboBox.setItems(FXCollections.observableArrayList(
                UPCOMING_MODE_ALL_NEAREST,
                UPCOMING_MODE_ALL_LATEST,
                UPCOMING_MODE_TODAY,
                UPCOMING_MODE_WEEK,
                UPCOMING_MODE_MONTH,
                UPCOMING_MODE_TITLE_AZ,
                UPCOMING_MODE_TITLE_ZA,
                UPCOMING_MODE_TIME,
                UPCOMING_MODE_COLOR,
                UPCOMING_MODE_FEEDBACK
        ));
        upcomingUnifiedFilterComboBox.setValue(UPCOMING_MODE_ALL_NEAREST);

        upcomingSearchField.textProperty().addListener((observable, oldValue, newValue) -> updateUpcomingPanel());
        upcomingUnifiedFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateUpcomingPanel());
    }

    private void configureMatchPlannerControls() {
        if (matchTeamComboBox == null) {
            return;
        }

        matchTeamComboBox.setItems(FXCollections.observableArrayList(footballDataService.getSuggestedTeams()));
        matchTeamComboBox.setCellFactory(combo -> new ListCell<>() {
            @Override
            protected void updateItem(FootballDataService.TeamOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });
        matchTeamComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(FootballDataService.TeamOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });

        FootballDataService.TeamOption savedFavorite = resolveFavoriteTeam();
        if (savedFavorite != null) {
            matchTeamComboBox.getSelectionModel().select(savedFavorite);
            setMatchStatus("Loading next match for your saved favorite team...", false);
            handleLoadNextMatch();
        } else {
            setMatchStatus("Select a team to load its next match.", false);
        }

        if (matchPrefillButton != null) {
            matchPrefillButton.setDisable(true);
        }
        setMatchTeams("Home", "Away", "", "");
        selectedNextMatch = null;

        // Auto-load next match when user changes selected team.
        matchTeamComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            if (oldValue != null && oldValue.id() == newValue.id()) {
                return;
            }
            handleLoadNextMatch();
        });
    }

    @FXML
    private void handleLoadNextMatch() {
        if (matchTeamComboBox == null) {
            return;
        }

        FootballDataService.TeamOption selectedTeam = matchTeamComboBox.getValue();
        if (selectedTeam == null) {
            setMatchStatus("Please choose a team first.", true);
            return;
        }

        saveFavoriteTeam(selectedTeam);
        setMatchStatus("Loading next match for " + selectedTeam.name() + "...", false);
        setMatchDetails("No match loaded yet", "Competition: -", "Kickoff: -");
        setMatchTeams("Home", "Away", "", "");
        setMatchPrefillEnabled(false);

        CompletableFuture.supplyAsync(() -> {
            try {
                return footballDataService.fetchNextMatch(selectedTeam);
            } catch (Exception exception) {
                throw new RuntimeException(exception.getMessage(), exception);
            }
        }).thenAccept(nextMatch -> Platform.runLater(() -> {
            selectedNextMatch = nextMatch;
            String localKickoff = formatMatchKickoff(nextMatch.kickoffUtc());
            setMatchDetails(
                    nextMatch.summary(),
                    "Competition: " + nextMatch.competition(),
                    "Kickoff: " + localKickoff
            );
            setMatchTeams(nextMatch.homeTeam(), nextMatch.awayTeam(), nextMatch.homeLogoUrl(), nextMatch.awayLogoUrl());
            setMatchStatus("Next match loaded successfully.", false);
            setMatchPrefillEnabled(true);
        })).exceptionally(error -> {
            Platform.runLater(() -> {
                selectedNextMatch = null;
                String message = error == null || error.getCause() == null
                        ? "Unable to load the next match."
                        : error.getCause().getMessage();
                setMatchStatus(message, true);
                setMatchTeams("Home", "Away", "", "");
                setMatchPrefillEnabled(false);
            });
            return null;
        });
    }

    @FXML
    private void handlePrefillNextMatch() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            showError("No logged-in user found.");
            return;
        }
        if (selectedNextMatch == null) {
            showError("Load a match first.");
            return;
        }

        TypeSeance matchType = resolveAssistantType(SESSION_TYPE_MATCH);
        if (matchType == null) {
            showError("Type 'Match' was not found. Please create it in session types.");
            return;
        }

        Seance matchSession = resolveOrCreateMatchSession(currentUser.getId(), selectedNextMatch, matchType);

        LocalDate matchDate = selectedNextMatch.kickoffUtc().atZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
        LocalTime matchStart = selectedNextMatch.kickoffUtc().atZoneSameInstant(ZoneId.systemDefault()).toLocalTime();
        LocalTime startSlot = normalizeAssistantTime(matchStart, false);
        if (startSlot == null) {
            startSlot = LocalTime.of(18, 0);
        }
        LocalTime endSlot = normalizeAssistantTime(startSlot.plusHours(2), true);
        if (endSlot == null || !endSlot.isAfter(startSlot)) {
            endSlot = startSlot.plusHours(1);
        }

        if (!canApplyPlanningTime(currentUser.getId(), matchDate, startSlot, endSlot, null, matchSession, true)) {
            showError("This match time overlaps another planning on the same day.");
            return;
        }

        Planning planningEntry = new Planning();
        planningEntry.setUserId(currentUser.getId());
        planningEntry.setSeanceId(matchSession.getId());
        planningEntry.setSeanceTitle(matchSession.getTitre());
        planningEntry.setPlanningDate(matchDate);
        planningEntry.setStartTime(startSlot);
        planningEntry.setEndTime(endSlot);
        planningEntry.setColorHex(findAvailableColor(matchDate, null));

        servicePlanning.add(planningEntry);
        sendPlanningCreationEmailAsync(currentUser, planningEntry);
        refreshPlanningData();
        showForm(false);
        showInfo("Match planned directly in your calendar.");
    }

    private Seance resolveOrCreateMatchSession(int userId,
                                               FootballDataService.NextMatch nextMatch,
                                               TypeSeance matchType) {
        String matchTitle = buildMatchAcronymTitle(nextMatch.homeTeam(), nextMatch.awayTeam());
        String legacyTitle = nextMatch.homeTeam() + " vs " + nextMatch.awayTeam();
        String normalizedTitle = normalize(matchTitle);
        String normalizedLegacyTitle = normalize(legacyTitle);
        for (Seance seance : sessionComboBox.getItems()) {
            if (seance != null
                    && seance.getTypeSeanceId() != null
                    && seance.getTypeSeanceId() == matchType.getId()
                    && (normalize(seance.getTitre()).equals(normalizedTitle)
                    || normalize(seance.getTitre()).equals(normalizedLegacyTitle))) {
                if (!normalize(seance.getTitre()).equals(normalizedTitle)) {
                    seance.setTitre(matchTitle);
                    serviceSeance.update(seance);
                    loadSessions();
                    for (Seance reloaded : sessionComboBox.getItems()) {
                        if (reloaded != null && reloaded.getId() == seance.getId()) {
                            return reloaded;
                        }
                    }
                }
                return seance;
            }
        }

        Seance created = new Seance();
        created.setUserId(userId);
        created.setTitre(matchTitle);
        created.setTypeSeanceId(matchType.getId());
        created.setTypeSeanceName(matchType.getName());
        created.setTypeSeance(matchType.getName());
        created.setDescription("Auto-created from football next match card: " + legacyTitle);
        serviceSeance.add(created);
        loadSessions();

        for (Seance seance : sessionComboBox.getItems()) {
            if (seance.getId() == created.getId()) {
                return seance;
            }
        }
        return created;
    }

    private String buildMatchAcronymTitle(String homeTeam, String awayTeam) {
        return toTeamAcronym(homeTeam) + " VS " + toTeamAcronym(awayTeam);
    }

    private String toTeamAcronym(String teamName) {
        if (teamName == null || teamName.isBlank()) {
            return "TEAM";
        }
        String cleaned = teamName.trim().replaceAll("[^A-Za-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.isBlank()) {
            return "TEAM";
        }

        String normalizedName = normalizeAssistant(cleaned);
        String knownAcronym = KNOWN_TEAM_ACRONYMS.get(normalizedName);
        if (knownAcronym != null && !knownAcronym.isBlank()) {
            return knownAcronym;
        }

        String[] words = cleaned.split(" ");
        StringBuilder acronymBuilder = new StringBuilder();
        for (String word : words) {
            if (word == null || word.isBlank()) {
                continue;
            }
            acronymBuilder.append(Character.toUpperCase(word.charAt(0)));
            if (acronymBuilder.length() >= 3) {
                break;
            }
        }

        if (acronymBuilder.length() < 3) {
            String compact = cleaned.replace(" ", "").toUpperCase(Locale.ENGLISH);
            int index = 0;
            while (acronymBuilder.length() < 3 && index < compact.length()) {
                char candidate = compact.charAt(index++);
                if (Character.isLetterOrDigit(candidate)) {
                    acronymBuilder.append(candidate);
                }
            }
        }

        while (acronymBuilder.length() < 3) {
            acronymBuilder.append('X');
        }
        return acronymBuilder.substring(0, 3);
    }

    private FootballDataService.TeamOption resolveFavoriteTeam() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null || matchTeamComboBox == null) {
            return null;
        }

        int savedTeamId = planningPreferences.getInt(favoriteTeamKey(currentUser.getId()), -1);
        if (savedTeamId <= 0) {
            return null;
        }

        for (FootballDataService.TeamOption team : matchTeamComboBox.getItems()) {
            if (team.id() == savedTeamId) {
                return team;
            }
        }

        return null;
    }

    private void saveFavoriteTeam(FootballDataService.TeamOption team) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null || team == null) {
            return;
        }
        planningPreferences.putInt(favoriteTeamKey(currentUser.getId()), team.id());
    }

    private String favoriteTeamKey(int userId) {
        return "planning.favorite.team." + userId;
    }

    private String formatMatchKickoff(OffsetDateTime kickoffUtc) {
        if (kickoffUtc == null) {
            return "-";
        }
        var localKickoff = kickoffUtc.atZoneSameInstant(ZoneId.systemDefault());
        String datePart = localKickoff.format(DateTimeFormatter.ofPattern("EEE, MMM d yyyy", Locale.ENGLISH));
        String timePart = localKickoff.format(timeFormatter);
        return "Date: " + datePart + " | Time: " + timePart;
    }

    private void setMatchStatus(String message, boolean error) {
        if (matchStatusLabel == null) {
            return;
        }
        matchStatusLabel.setText(message == null ? "" : message);
        matchStatusLabel.getStyleClass().removeAll("session-field-error", "text-small");
        matchStatusLabel.getStyleClass().add(error ? "session-field-error" : "text-small");
    }

    private void setMatchDetails(String summary, String competition, String kickoff) {
        if (matchSummaryLabel != null) {
            matchSummaryLabel.setText(summary == null ? "No match loaded yet" : summary);
        }
        if (matchCompetitionLabel != null) {
            matchCompetitionLabel.setText(competition == null ? "Competition: -" : competition);
        }
        if (matchKickoffLabel != null) {
            matchKickoffLabel.setText(kickoff == null ? "Kickoff: -" : kickoff);
        }
    }

    private void setMatchTeams(String homeName, String awayName, String homeLogoUrl, String awayLogoUrl) {
        if (matchHomeTeamLabel != null) {
            matchHomeTeamLabel.setText((homeName == null || homeName.isBlank()) ? "Home" : homeName);
        }
        if (matchAwayTeamLabel != null) {
            matchAwayTeamLabel.setText((awayName == null || awayName.isBlank()) ? "Away" : awayName);
        }
        loadTeamLogo(matchHomeLogoImageView, homeLogoUrl);
        loadTeamLogo(matchAwayLogoImageView, awayLogoUrl);
    }

    private void loadTeamLogo(ImageView imageView, String logoUrl) {
        if (imageView == null) {
            return;
        }
        if (logoUrl == null || logoUrl.isBlank()) {
            imageView.setImage(null);
            return;
        }
        try {
            imageView.setImage(new Image(logoUrl, true));
        } catch (Exception exception) {
            imageView.setImage(null);
        }
    }

    private void setMatchPrefillEnabled(boolean enabled) {
        if (matchPrefillButton != null) {
            matchPrefillButton.setDisable(!enabled);
        }
    }

    private void configureAssistantControls() {
        if (chatScrollPane != null) {
            chatScrollPane.setFitToWidth(true);
            chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        }

        if (assistantInputField != null) {
            assistantInputField.setOnAction(event -> handleAssistantSend());
            assistantInputField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.UP) {
                    navigateAssistantInputHistory(-1);
                    event.consume();
                } else if (event.getCode() == KeyCode.DOWN) {
                    navigateAssistantInputHistory(1);
                    event.consume();
                }
            });
        }

        updateAssistantHint();
    }

    @FXML
    private void handleAssistantSend() {
        if (assistantInputField == null) {
            return;
        }
        String rawInput = assistantInputField.getText();
        String input = rawInput == null ? "" : rawInput.trim();
        if (input.isEmpty()) {
            addMessage("Type a command first.", false);
            return;
        }

        addMessage(input, true);
        if (assistantUserHistory.isEmpty() || !assistantUserHistory.get(assistantUserHistory.size() - 1).equals(input)) {
            assistantUserHistory.add(input);
        }
        assistantHistoryIndex = assistantUserHistory.size();
        assistantDraftInput = "";
        assistantInputField.clear();
        showAssistantTypingIndicator();

        try {
            if (pendingAssistantCommand != null) {
                if (containsAny(normalizeAssistant(input), "cancel", "annule", "stop")) {
                    pendingAssistantCommand = null;
                    addMessage("Okay, I cancelled the pending planning request.", false);
                    return;
                }

                AssistantCommand completedCommand = completePendingAssistantCommand(input);
                if (completedCommand == null) {
                    addMessage("I still need a valid time. Example: from 14:00 to 16:00.", false);
                    return;
                }

                String response = executeAssistantCommand(completedCommand);
                addMessage(response, false);
                return;
            }

            AssistantCommand command;
            if (input.startsWith("/")) {
                command = parseTerminalCommand(input);
            } else {
                command = parseAssistantCommandWithClaude(input);
            }
            String response = executeAssistantCommand(command);
            addMessage(response, false);
        } catch (Exception exception) {
            addMessage("I could not process that command: " + exception.getMessage(), false);
        } finally {
            hideAssistantTypingIndicator();
        }
    }

    private AssistantCommand completePendingAssistantCommand(String input) {
        if (pendingAssistantCommand == null) {
            return null;
        }

        String normalizedInput = normalizeAssistant(input);
        LocalTime start = extractAssistantStartTime(normalizedInput);
        if (start == null) {
            return null;
        }

        LocalTime end = extractAssistantEndTime(normalizedInput, start);
        if (end == null) {
            end = start.plusHours(1);
        }

        return AssistantCommand.create(
                pendingAssistantCommand.title,
                pendingAssistantCommand.sessionType,
                pendingAssistantCommand.date,
                start,
                end
        );
    }

    @FXML
    private void handleAssistantQuickPlan() {
        insertAssistantPrompt("Plan a focus session for today from 14:00 to 16:00");
    }

    @FXML
    private void handleAssistantQuickRevision() {
        insertAssistantPrompt("Plan a revision Java session tomorrow from 10:00 to 12:00");
    }

    @FXML
    private void handleAssistantQuickDelete() {
        insertAssistantPrompt("Delete the Java session on 2026-04-21");
    }

    private void insertAssistantPrompt(String prompt) {
        if (assistantInputField == null) {
            return;
        }
        assistantInputField.setText(prompt == null ? "" : prompt);
        assistantInputField.positionCaret(assistantInputField.getText().length());
        assistantInputField.requestFocus();
    }

    private void navigateAssistantInputHistory(int direction) {
        if (assistantInputField == null || assistantUserHistory.isEmpty()) {
            return;
        }

        if (assistantHistoryIndex < 0 || assistantHistoryIndex > assistantUserHistory.size()) {
            assistantHistoryIndex = assistantUserHistory.size();
        }

        if (direction < 0 && assistantHistoryIndex == assistantUserHistory.size()) {
            assistantDraftInput = assistantInputField.getText() == null ? "" : assistantInputField.getText();
        }

        int nextIndex = assistantHistoryIndex + direction;
        if (nextIndex < 0) {
            nextIndex = 0;
        }
        if (nextIndex > assistantUserHistory.size()) {
            nextIndex = assistantUserHistory.size();
        }
        assistantHistoryIndex = nextIndex;

        String textToShow = assistantHistoryIndex == assistantUserHistory.size()
                ? assistantDraftInput
                : assistantUserHistory.get(assistantHistoryIndex);
        assistantInputField.setText(textToShow == null ? "" : textToShow);
        assistantInputField.positionCaret(assistantInputField.getText().length());
    }

    private AssistantCommand parseAssistantCommandWithClaude(String input) {
        String apiKey = System.getenv(ANTHROPIC_API_KEY_ENV);
        if (apiKey == null || apiKey.isBlank()) {
            return parseNaturalLanguageCommand(input);
        }

        AIPlanningService.ClaudeResult result = claudePlanningService.interpretPlanningCommand(
                apiKey,
                input,
                LocalDate.now(),
                getKnownSessionTitles(),
                getKnownSessionTypes()
        );

        if (!result.isSuccess() || result.getCommand() == null) {
            return parseNaturalLanguageCommand(input);
        }

        AssistantCommand mapped = mapClaudeToAssistantCommand(result.getCommand());
        if (mapped.type == AssistantCommandType.ERROR) {
            return parseNaturalLanguageCommand(input);
        }

        AssistantCommand localParsed = parseNaturalLanguageCommand(input);
        if (mapped.type == AssistantCommandType.CREATE && localParsed.type == AssistantCommandType.CREATE) {
            String mergedTitle = isSuspiciousAssistantTitle(mapped.title) ? localParsed.title : mapped.title;
            LocalDate mergedDate = localParsed.date != null ? localParsed.date : mapped.date;
            LocalTime mergedStart = localParsed.startTime != null ? localParsed.startTime : mapped.startTime;
            LocalTime mergedEnd = localParsed.endTime != null ? localParsed.endTime : mapped.endTime;
            String mergedType = mapped.sessionType != null ? mapped.sessionType : localParsed.sessionType;

            if (mergedStart != null && mergedEnd != null && !mergedEnd.isAfter(mergedStart)) {
                mergedEnd = mergedStart.plusHours(1);
            }

            return AssistantCommand.create(mergedTitle, mergedType, mergedDate, mergedStart, mergedEnd);
        }
        return mapped;
    }

    private boolean isSuspiciousAssistantTitle(String title) {
        if (title == null || title.isBlank()) {
            return true;
        }
        String normalizedTitle = normalizeAssistant(title);
        return normalizedTitle.matches(".*\\d{1,2}[/-]\\d{1,2}([/-]\\d{2,4})?.*")
                || normalizedTitle.startsWith("in ")
                || normalizedTitle.startsWith("on ")
                || normalizedTitle.startsWith("at ")
                || normalizedTitle.length() < 2;
    }

    private AssistantCommand mapClaudeToAssistantCommand(AIPlanningService.ClaudeCommand command) {
        if (command == null) {
            return AssistantCommand.error("Claude returned no command.");
        }

        String action = normalizeAssistant(command.action());
        if ("help".equals(action)) {
            return AssistantCommand.help();
        }
        if ("unknown".equals(action)) {
            return AssistantCommand.error(command.message() == null
                    ? "I did not understand the request."
                    : command.message());
        }

        if ("create".equals(action)) {
            String title = command.title();
            String sessionType = command.sessionType();
            LocalDate date = parseIsoDate(command.date());
            LocalTime start = parseClock(command.startTime());
            LocalTime end = command.endTime() == null ? (start == null ? null : start.plusHours(1)) : parseClock(command.endTime());

            if (title == null || title.isBlank()) {
                return AssistantCommand.error("Claude did not provide a session title.");
            }
            if (date == null) {
                return AssistantCommand.error("Claude did not provide a valid date.");
            }
            if (start == null) {
                return AssistantCommand.awaitingTime(title, sessionType, date);
            }
            if (end == null) {
                return AssistantCommand.error("Claude did not provide a valid end time.");
            }
            return AssistantCommand.create(title, sessionType, date, start, end);
        }

        if ("delete".equals(action)) {
            LocalDate date = parseIsoDate(command.date());
            if (date == null) {
                return AssistantCommand.error("Claude did not provide a valid date for deletion.");
            }
            return AssistantCommand.delete(command.title(), date);
        }

        if ("update".equals(action) || "modify".equals(action) || "change".equals(action)) {
            LocalDate targetDate = parseIsoDate(command.targetDate());
            LocalDate newDate = parseIsoDate(command.date());
            LocalTime start = parseClock(command.startTime());
            LocalTime end = command.endTime() == null ? null : parseClock(command.endTime());
            boolean keepTime = containsAny(normalizeAssistant(command.message()), "same time", "same hour", "same schedule", "meme heure");
            return AssistantCommand.update(command.title(), command.sessionType(), targetDate, newDate, start, end, keepTime);
        }

        return AssistantCommand.error("Unsupported Claude action.");
    }

    private List<String> getKnownSessionTitles() {
        List<String> titles = new ArrayList<>();
        if (sessionComboBox == null || sessionComboBox.getItems() == null) {
            return titles;
        }
        for (Seance seance : sessionComboBox.getItems()) {
            if (seance != null && seance.getTitre() != null && !seance.getTitre().isBlank()) {
                titles.add(seance.getTitre());
            }
        }
        return titles;
    }

    private List<String> getKnownSessionTypes() {
        List<String> types = new ArrayList<>();
        for (TypeSeance typeSeance : serviceTypeSeance.getAvailableTypes()) {
            if (typeSeance != null && typeSeance.getName() != null && !typeSeance.getName().isBlank()) {
                types.add(typeSeance.getName());
            }
        }
        return types;
    }

    @FXML
    private void handleAssistantClear() {
        if (chatBox != null) {
            chatBox.getChildren().clear();
        }
        assistantTypingRow = null;
        pendingAssistantCommand = null;
        addMessage("Conversation cleared.", false);
    }

    private void updateAssistantHint() {
        if (assistantHintLabel == null) {
            return;
        }
        assistantHintLabel.setText("");
        assistantHintLabel.setVisible(false);
        assistantHintLabel.setManaged(false);
    }

    private void addMessage(String text, boolean isUser) {
        if (chatBox == null) {
            return;
        }

        Label bubble = new Label(text == null ? "" : text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(305);
        bubble.getStyleClass().addAll("assistant-bubble", isUser ? "assistant-user" : "assistant-bot");

        Label avatarLabel = new Label(isUser ? "ME" : "🤖");
        avatarLabel.getStyleClass().addAll("assistant-avatar", isUser ? "assistant-avatar-user" : "assistant-avatar-bot");
        StackPane avatar = new StackPane(avatarLabel);
        avatar.getStyleClass().add("assistant-avatar-wrap");
        if (!isUser) {
            animateAssistantAvatar(avatar);
        }

        HBox row = isUser ? new HBox(bubble, avatar) : new HBox(avatar, bubble);
        row.getStyleClass().add("assistant-message-row");
        row.setSpacing(8);
        row.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setFillHeight(false);

        chatBox.getChildren().add(row);
        scrollChatToBottom();
    }

    private void animateAssistantAvatar(StackPane avatar) {
        if (avatar == null) {
            return;
        }
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(1.35), avatar);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.08);
        pulse.setToY(1.08);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();
    }

    private void showAssistantTypingIndicator() {
        if (chatBox == null || assistantTypingRow != null) {
            return;
        }

        Label bubble = new Label("AI is thinking...");
        bubble.setWrapText(true);
        bubble.setMaxWidth(320);
        bubble.getStyleClass().addAll("assistant-bubble", "assistant-bot", "assistant-typing");

        assistantTypingRow = new HBox(bubble);
        assistantTypingRow.setAlignment(Pos.CENTER_LEFT);
        assistantTypingRow.setFillHeight(false);
        chatBox.getChildren().add(assistantTypingRow);
        scrollChatToBottom();
    }

    private void hideAssistantTypingIndicator() {
        if (chatBox == null || assistantTypingRow == null) {
            return;
        }
        chatBox.getChildren().remove(assistantTypingRow);
        assistantTypingRow = null;
        scrollChatToBottom();
    }

    private void scrollChatToBottom() {
        if (chatScrollPane == null || chatBox == null) {
            return;
        }

        // Two UI cycles ensure the ScrollPane has measured the new bubble height.
        Platform.runLater(() -> {
            chatBox.applyCss();
            chatBox.layout();
            chatScrollPane.layout();
            chatScrollPane.setVvalue(1.0);
            Platform.runLater(() -> {
                chatScrollPane.layout();
                chatScrollPane.setVvalue(1.0);
            });
        });
    }

    private AssistantCommand parseAssistantCommand(String input) {
        if (input == null || input.isBlank()) {
            return AssistantCommand.help();
        }
        String trimmed = input.trim();
        if (trimmed.startsWith("/")) {
            return parseTerminalCommand(trimmed);
        }
        return parseNaturalLanguageCommand(trimmed);
    }

    private AssistantCommand parseTerminalCommand(String input) {
        String normalized = input.trim();
        if ("/help".equalsIgnoreCase(normalized)) {
            return AssistantCommand.help();
        }

        if (normalized.toLowerCase(Locale.ROOT).startsWith("/plan ")) {
            String payload = normalized.substring(6).trim();
            String[] parts = payload.split("\\|");
            if (parts.length < 3) {
                return AssistantCommand.error("Use: /plan title|yyyy-MM-dd|HH:mm|HH:mm (end optional)");
            }
            String title = parts[0].trim();
            String sessionType = parts.length >= 5 ? parts[1].trim() : null;
            int dateIndex = parts.length >= 5 ? 2 : 1;
            int startIndex = parts.length >= 5 ? 3 : 2;
            int endIndex = parts.length >= 5 ? 4 : 3;
            LocalDate date = parseIsoDate(parts[dateIndex].trim());
            LocalTime start = parseClock(parts[startIndex].trim());
            LocalTime end = parts.length > endIndex ? parseClock(parts[endIndex].trim()) : (start == null ? null : start.plusHours(1));
            if (title.isBlank() || date == null || start == null || end == null) {
                return AssistantCommand.error("Invalid /plan values. Expected title, valid date and time.");
            }
            return AssistantCommand.create(title, sessionType, date, start, end);
        }

        if (normalized.toLowerCase(Locale.ROOT).startsWith("/delete ")) {
            String payload = normalized.substring(8).trim();
            String[] parts = payload.split("\\|");
            LocalDate date = parseIsoDate(parts[0].trim());
            if (date == null) {
                return AssistantCommand.error("Use: /delete yyyy-MM-dd [|title]");
            }
            String title = parts.length >= 2 ? parts[1].trim() : null;
            return AssistantCommand.delete(title, date);
        }

        if (normalized.toLowerCase(Locale.ROOT).startsWith("/update ")) {
            String payload = normalized.substring(8).trim();
            String[] parts = payload.split("\\|");
            if (parts.length < 2) {
                return AssistantCommand.error("Use: /update yyyy-MM-dd|title [|newDate] [|start] [|end]");
            }
            LocalDate targetDate = parseIsoDate(parts[0].trim());
            String title = parts[1].trim();
            LocalDate newDate = parts.length >= 3 ? parseIsoDate(parts[2].trim()) : null;
            LocalTime start = parts.length >= 4 ? parseClock(parts[3].trim()) : null;
            LocalTime end = parts.length >= 5 ? parseClock(parts[4].trim()) : null;
            if (targetDate == null) {
                return AssistantCommand.error("/update requires a valid target date (yyyy-MM-dd).");
            }
            return AssistantCommand.update(title, null, targetDate, newDate, start, end, false);
        }

        return AssistantCommand.error("Unknown command. Use /help.");
    }

    private AssistantCommand parseNaturalLanguageCommand(String input) {
        String lowered = normalizeAssistant(input);
        if (lowered.contains("help") || lowered.contains("aide")) {
            return AssistantCommand.help();
        }

        boolean isDelete = containsAny(lowered, "delete", "remove", "supprime", "annule");
        boolean isUpdate = containsAny(lowered,
                "modify", "change", "update", "reschedule", "move", "shift", "edit",
                "modifier", "changer", "deplacer", "decal");
        boolean isCreate = containsAny(lowered, "add", "create", "plan", "schedule", "ajoute", "planifie", "planifier");

        LocalDate date = extractAssistantDate(lowered);
        List<LocalDate> detectedDates = extractAssistantDates(lowered);
        LocalTime start = extractAssistantStartTime(lowered);
        LocalTime end = extractAssistantEndTime(lowered, start);
        String sessionType = extractAssistantSessionType(lowered);
        String title = extractAssistantTitle(lowered, input, sessionType);

        if (isDelete) {
            if (date == null) {
                return AssistantCommand.error("I need a date to delete planning. Example: delete Tuesday session.");
            }
            return AssistantCommand.delete(title, date);
        }

        if (isUpdate) {
            String updateTitle = extractAssistantUpdateTitle(lowered, input, sessionType);
            boolean keepTime = containsAny(lowered, "same time", "same hour", "same schedule", "meme heure", "meme horaire");
            LocalDate targetDate = detectedDates.isEmpty() ? null : detectedDates.get(0);
            LocalDate newDate = null;
            if (detectedDates.size() >= 2) {
                newDate = detectedDates.get(1);
            }

            boolean changesTime = start != null || end != null || containsAny(lowered, "change the time", "set it from", "from", "at");
            boolean changesDate = newDate != null || (detectedDates.size() == 1 && containsAny(lowered, "to ", "into ", "for "));
            if (changesDate && newDate == null && detectedDates.size() == 1 && !containsAny(lowered, "today", "tomorrow", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")) {
                newDate = detectedDates.get(0);
                targetDate = null;
            }

            if (updateTitle == null && targetDate == null) {
                return AssistantCommand.error("I need at least a session title or date to modify planning.");
            }
            if (!changesTime && newDate == null) {
                return AssistantCommand.error("Tell me what to change: date, time, or both.");
            }
            return AssistantCommand.update(updateTitle, sessionType, targetDate, newDate, start, end, keepTime);
        }

        if (isCreate) {
            if (title == null || title.isBlank()) {
                return AssistantCommand.error("I need a session title. Example: add a math session tomorrow at 14:00.");
            }
            if (date == null) {
                return AssistantCommand.error("I need a date. Example: tomorrow or 2026-04-20.");
            }
            if (start == null) {
                return AssistantCommand.awaitingTime(title, sessionType, date);
            }
            if (end == null) {
                end = start.plusHours(1);
            }
            return AssistantCommand.create(title, sessionType, date, start, end);
        }

        return AssistantCommand.error("I did not understand. Ask me to add/plan/delete, or use /help.");
    }

    private String executeAssistantCommand(AssistantCommand command) {
        if (command.type == AssistantCommandType.AWAITING_TIME) {
            pendingAssistantCommand = command;
            return "Got it. Please provide the time for " + command.title + " on "
                    + command.date + ". Example: from 14:00 to 16:00.";
        }
        if (command.type == AssistantCommandType.HELP) {
            return "Commands: /plan title|yyyy-MM-dd|HH:mm|HH:mm, /delete yyyy-MM-dd|title. In chat mode, you can say: Add a math session tomorrow at 14:00.";
        }
        if (command.type == AssistantCommandType.ERROR) {
            return command.errorMessage;
        }

        if (command.type == AssistantCommandType.CREATE) {
            return executeAssistantCreate(command);
        }

        if (command.type == AssistantCommandType.DELETE) {
            return executeAssistantDelete(command);
        }

        if (command.type == AssistantCommandType.UPDATE) {
            return executeAssistantUpdate(command);
        }

        return "Unsupported command.";
    }

    private String executeAssistantCreate(AssistantCommand command) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            return "No logged-in user found.";
        }

        LocalTime normalizedStart = normalizeAssistantTime(command.startTime, false);
        if (normalizedStart == null) {
            return "Invalid start time. Use a time between 06:00 and 23:30.";
        }

        LocalTime rawEnd = command.endTime == null ? command.startTime.plusHours(1) : command.endTime;
        LocalTime normalizedEnd = normalizeAssistantTime(rawEnd, true);
        if (normalizedEnd == null || !normalizedEnd.isAfter(normalizedStart)) {
            normalizedEnd = normalizeAssistantTime(normalizedStart.plusHours(1), true);
        }

        if (normalizedEnd == null || !normalizedEnd.isAfter(normalizedStart)) {
            return "Invalid time range. Planning hours are from 06:00 to 23:30.";
        }

        Seance seance = resolveSessionForAssistant(currentUser.getId(), command.title, command.sessionType);
        if (seance == null) {
            return assistantSessionResolutionError == null
                    ? "Unable to resolve a session title."
                    : assistantSessionResolutionError;
        }

        if (!canApplyPlanningTime(currentUser.getId(), command.date, normalizedStart, normalizedEnd, null, seance, true)) {
            return "Cannot create planning: this time overlaps another session on that day.";
        }

        Planning entry = new Planning();
        entry.setUserId(currentUser.getId());
        entry.setSeanceId(seance.getId());
        entry.setSeanceTitle(seance.getTitre());
        entry.setPlanningDate(command.date);
        entry.setStartTime(normalizedStart);
        entry.setEndTime(normalizedEnd);
        entry.setColorHex(pickRandomAssistantColor(currentUser.getId(), command.date));

        servicePlanning.add(entry);
        sendPlanningCreationEmailAsync(currentUser, entry);
        refreshPlanningData();
        trackAssistantGeneratedEntry(entry);
        pendingAssistantCommand = null;

        boolean timeAdjusted = !normalizedStart.equals(command.startTime)
                || (command.endTime != null && !normalizedEnd.equals(command.endTime));
        String adjustmentNote = timeAdjusted ? " (time adjusted to planning slots)" : "";
        return "Planning created: " + seance.getTitre() + " on " + command.date.format(shortDateFormatter) + " at "
                + normalizedStart.format(timeFormatter) + " - " + normalizedEnd.format(timeFormatter) + adjustmentNote + ".";
    }

    private String executeAssistantDelete(AssistantCommand command) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            return "No logged-in user found.";
        }

        String titleFilter = normalize(command.title);
        List<Planning> matching = allPlanningEntries.stream()
                .filter(entry -> entry.getUserId() == currentUser.getId())
                .filter(entry -> command.date.equals(entry.getPlanningDate()))
                .filter(entry -> titleFilter.isBlank() || normalize(entry.getSeanceTitle()).contains(titleFilter))
                .toList();

        if (matching.isEmpty()) {
            return "No matching planning found for that date.";
        }

        for (Planning entry : matching) {
            servicePlanning.delete(entry);
        }
        refreshPlanningData();

        return "Deleted " + matching.size() + " planning entr" + (matching.size() == 1 ? "y" : "ies")
                + " for " + command.date.format(shortDateFormatter) + ".";
    }

    private String executeAssistantUpdate(AssistantCommand command) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            return "No logged-in user found.";
        }

        Planning selected = resolvePlanningEntryForUpdate(currentUser.getId(), command);
        if (selected == null) {
            return assistantSessionResolutionError == null
                    ? "I could not find a matching planning to modify."
                    : assistantSessionResolutionError;
        }

        LocalDate targetDate = command.newDate != null ? command.newDate : selected.getPlanningDate();
        LocalTime targetStart = command.startTime == null
                ? selected.getStartTime()
                : normalizeAssistantTime(command.startTime, false);

        LocalTime targetEnd;
        if (command.endTime != null) {
            targetEnd = normalizeAssistantTime(command.endTime, true);
        } else if (command.startTime != null) {
            int durationMinutes = Math.max(30, (selected.getEndTime().toSecondOfDay() - selected.getStartTime().toSecondOfDay()) / 60);
            targetEnd = normalizeAssistantTime(targetStart.plusMinutes(durationMinutes), true);
        } else {
            targetEnd = selected.getEndTime();
        }

        if (targetStart == null || targetEnd == null || !targetEnd.isAfter(targetStart)) {
            return "Invalid updated time range. Use HH:mm from 06:00 to 23:30.";
        }

        Seance selectedSeance = findSeanceById(selected.getSeanceId());
        if (!canApplyPlanningTime(currentUser.getId(), targetDate, targetStart, targetEnd, selected.getId(), selectedSeance, true)) {
            return "Cannot update planning: this time overlaps another session on that day.";
        }

        String adjustedColorNote = "";
        if (servicePlanning.isColorUsedOnDate(currentUser.getId(), targetDate, selected.getColorHex(), selected.getId())) {
            selected.setColorHex(findAvailableColor(targetDate, selected.getId()));
            adjustedColorNote = " Color adjusted to keep uniqueness on that day.";
        }

        selected.setPlanningDate(targetDate);
        selected.setStartTime(targetStart);
        selected.setEndTime(targetEnd);
        servicePlanning.update(selected);
        refreshPlanningData();

        return "Planning updated: " + selected.getSeanceTitle() + " -> "
                + selected.getPlanningDate().format(shortDateFormatter) + " "
                + selected.getStartTime().format(timeFormatter) + " - "
                + selected.getEndTime().format(timeFormatter) + "." + adjustedColorNote;
    }

    private Planning resolvePlanningEntryForUpdate(int userId, AssistantCommand command) {
        assistantSessionResolutionError = null;
        String titleFilter = normalize(command.title);
        List<Planning> candidates = allPlanningEntries.stream()
                .filter(entry -> entry.getUserId() == userId)
                .filter(entry -> command.date == null || command.date.equals(entry.getPlanningDate()))
                .filter(entry -> titleFilter.isBlank() || normalize(entry.getSeanceTitle()).contains(titleFilter))
                .sorted(Comparator.comparing(Planning::getPlanningDate).thenComparing(Planning::getStartTime))
                .toList();

        if (candidates.isEmpty() && !titleFilter.isBlank()) {
            candidates = allPlanningEntries.stream()
                    .filter(entry -> entry.getUserId() == userId)
                    .filter(entry -> normalize(entry.getSeanceTitle()).contains(titleFilter))
                    .sorted(Comparator.comparing(Planning::getPlanningDate).thenComparing(Planning::getStartTime))
                    .toList();
        }

        if (candidates.isEmpty()) {
            assistantSessionResolutionError = "No matching planning found. Try adding a more specific date or title.";
            return null;
        }

        if (candidates.size() > 1) {
            List<Planning> exactTitle = candidates.stream()
                    .filter(entry -> !titleFilter.isBlank() && normalize(entry.getSeanceTitle()).equals(titleFilter))
                    .toList();
            if (exactTitle.size() == 1) {
                return exactTitle.get(0);
            }
            assistantSessionResolutionError = buildAmbiguousPlanningMessage(candidates);
            return null;
        }

        return candidates.get(0);
    }

    private String buildAmbiguousPlanningMessage(List<Planning> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "Multiple sessions found. Please be more specific.";
        }
        StringBuilder builder = new StringBuilder("I found multiple matching sessions. Please specify date or title. Examples: ");
        int limit = Math.min(3, candidates.size());
        for (int i = 0; i < limit; i++) {
            Planning entry = candidates.get(i);
            if (i > 0) {
                builder.append(" | ");
            }
            builder.append(entry.getSeanceTitle())
                    .append(" on ")
                    .append(entry.getPlanningDate())
                    .append(" at ")
                    .append(entry.getStartTime().format(timeFormatter));
        }
        return builder.toString();
    }

    private Seance resolveSessionForAssistant(int userId, String requestedTitle, String requestedType) {
        assistantSessionResolutionError = null;
        String normalizedRequested = normalize(requestedTitle);
        TypeSeance resolvedType = resolveAssistantType(requestedType);
        if (resolvedType == null) {
            assistantSessionResolutionError = "Please specify a valid session type (for example: revision).";
            return null;
        }

        for (Seance seance : sessionComboBox.getItems()) {
            if (normalize(seance.getTitre()).equals(normalizedRequested) && isSessionTypeValid(seance)) {
                return seance;
            }
        }
        for (Seance seance : sessionComboBox.getItems()) {
            if ((normalize(seance.getTitre()).contains(normalizedRequested)
                    || normalizedRequested.contains(normalize(seance.getTitre())))
                    && isSessionTypeValid(seance)) {
                return seance;
            }
        }

        String cleanTitle = toTitleCase(requestedTitle);
        if (cleanTitle.isBlank()) {
            assistantSessionResolutionError = "Please provide a valid session title.";
            return null;
        }

        Seance created = new Seance();
        created.setUserId(userId);
        created.setTitre(cleanTitle);
        created.setTypeSeanceId(resolvedType.getId());
        created.setTypeSeanceName(resolvedType.getName());
        created.setTypeSeance(resolvedType.getName());
        created.setDescription("Created from planning assistant");
        serviceSeance.add(created);
        loadSessions();

        for (Seance seance : sessionComboBox.getItems()) {
            if (seance.getId() == created.getId()) {
                return seance;
            }
        }
        return created;
    }

    private TypeSeance resolveAssistantType(String requestedType) {
        String normalizedRequestedType = normalizeAssistantTypeForSearch(requestedType);
        if (normalizedRequestedType.isBlank()) {
            return null;
        }
        List<TypeSeance> availableTypes = serviceTypeSeance.getAvailableTypes();
        for (TypeSeance typeSeance : availableTypes) {
            String normalizedTypeName = normalizeAssistantTypeForSearch(typeSeance.getName());
            if (normalizedTypeName.equals(normalizedRequestedType)) {
                return typeSeance;
            }
        }
        for (TypeSeance typeSeance : availableTypes) {
            String normalizedName = normalizeAssistantTypeForSearch(typeSeance.getName());
            if (normalizedName.contains(normalizedRequestedType) || normalizedRequestedType.contains(normalizedName)) {
                return typeSeance;
            }
        }
        return null;
    }

    private String extractAssistantSessionType(String normalizedInput) {
        String bestMatch = null;
        int bestLength = -1;
        String normalizedText = normalizeAssistantTypeForSearch(normalizedInput);
        for (TypeSeance typeSeance : serviceTypeSeance.getAvailableTypes()) {
            String typeName = typeSeance == null ? null : typeSeance.getName();
            String normalizedType = normalizeAssistantTypeForSearch(typeName);
            if (normalizedType.isBlank()) {
                continue;
            }
            if (normalizedText.contains(normalizedType) && normalizedType.length() > bestLength) {
                bestMatch = typeName;
                bestLength = normalizedType.length();
            }
        }
        return bestMatch;
    }

    private boolean isSessionTypeValid(Seance seance) {
        return seance != null
                && seance.getTypeSeanceId() != null
                && seance.getTypeSeanceId() > 0;
    }

    private String toTitleCase(String input) {
        if (input == null || input.isBlank()) {
            return "Session";
        }
        String[] words = input.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.toString();
    }

    private LocalDate extractAssistantDate(String normalizedInput) {
        List<LocalDate> dates = extractAssistantDates(normalizedInput);
        if (!dates.isEmpty()) {
            return dates.get(0);
        }

        LocalDate today = LocalDate.now();
        Map<String, DayOfWeek> dayAliases = Map.ofEntries(
                Map.entry("monday", DayOfWeek.MONDAY),
                Map.entry("lundi", DayOfWeek.MONDAY),
                Map.entry("tuesday", DayOfWeek.TUESDAY),
                Map.entry("mardi", DayOfWeek.TUESDAY),
                Map.entry("wednesday", DayOfWeek.WEDNESDAY),
                Map.entry("mercredi", DayOfWeek.WEDNESDAY),
                Map.entry("thursday", DayOfWeek.THURSDAY),
                Map.entry("jeudi", DayOfWeek.THURSDAY),
                Map.entry("friday", DayOfWeek.FRIDAY),
                Map.entry("vendredi", DayOfWeek.FRIDAY),
                Map.entry("saturday", DayOfWeek.SATURDAY),
                Map.entry("samedi", DayOfWeek.SATURDAY),
                Map.entry("sunday", DayOfWeek.SUNDAY),
                Map.entry("dimanche", DayOfWeek.SUNDAY)
        );

        for (Map.Entry<String, DayOfWeek> alias : dayAliases.entrySet()) {
            if (normalizedInput.contains(alias.getKey())) {
                return today.with(TemporalAdjusters.nextOrSame(alias.getValue()));
            }
        }
        return null;
    }

    private List<LocalDate> extractAssistantDates(String normalizedInput) {
        List<LocalDate> dates = new ArrayList<>();

        Matcher isoMatcher = ASSISTANT_ISO_DATE_PATTERN.matcher(normalizedInput);
        while (isoMatcher.find()) {
            LocalDate parsedIso = parseIsoDate(isoMatcher.group());
            if (parsedIso != null && !dates.contains(parsedIso)) {
                dates.add(parsedIso);
            }
        }

        Matcher shortMatcher = ASSISTANT_SHORT_DATE_PATTERN.matcher(normalizedInput);
        while (shortMatcher.find()) {
            LocalDate parsedShort = parseIsoDate(shortMatcher.group());
            if (parsedShort != null && !dates.contains(parsedShort)) {
                dates.add(parsedShort);
            }
        }

        LocalDate today = LocalDate.now();
        if (containsAny(normalizedInput, "today", "aujourd", "auj")) {
            if (!dates.contains(today)) {
                dates.add(today);
            }
        }
        if (containsAny(normalizedInput, "tomorrow", "demain")) {
            LocalDate tomorrow = today.plusDays(1);
            if (!dates.contains(tomorrow)) {
                dates.add(tomorrow);
            }
        }

        Map<String, DayOfWeek> dayAliases = Map.ofEntries(
                Map.entry("monday", DayOfWeek.MONDAY),
                Map.entry("lundi", DayOfWeek.MONDAY),
                Map.entry("tuesday", DayOfWeek.TUESDAY),
                Map.entry("mardi", DayOfWeek.TUESDAY),
                Map.entry("wednesday", DayOfWeek.WEDNESDAY),
                Map.entry("mercredi", DayOfWeek.WEDNESDAY),
                Map.entry("thursday", DayOfWeek.THURSDAY),
                Map.entry("jeudi", DayOfWeek.THURSDAY),
                Map.entry("friday", DayOfWeek.FRIDAY),
                Map.entry("vendredi", DayOfWeek.FRIDAY),
                Map.entry("saturday", DayOfWeek.SATURDAY),
                Map.entry("samedi", DayOfWeek.SATURDAY),
                Map.entry("sunday", DayOfWeek.SUNDAY),
                Map.entry("dimanche", DayOfWeek.SUNDAY)
        );

        for (Map.Entry<String, DayOfWeek> alias : dayAliases.entrySet()) {
            if (normalizedInput.contains(alias.getKey())) {
                LocalDate weekdayDate = today.with(TemporalAdjusters.nextOrSame(alias.getValue()));
                if (!dates.contains(weekdayDate)) {
                    dates.add(weekdayDate);
                }
            }
        }
        return dates;
    }

    private String extractAssistantUpdateTitle(String normalizedInput, String rawInput, String sessionType) {
        String knownTitle = findKnownSessionTitleMention(normalizedInput);
        if (knownTitle != null && !knownTitle.isBlank()) {
            return knownTitle;
        }

        Pattern changePattern = Pattern.compile(
                "(?i)(?:modify|change|update|reschedule|move|edit|modifier|changer)\\s+(?:the\\s+)?(?:session\\s+)?(?:of\\s+)?(.+)$"
        );
        Matcher matcher = changePattern.matcher(rawInput == null ? "" : rawInput);
        if (matcher.find()) {
            String candidate = sanitizeAssistantTitle(matcher.group(1));
            candidate = removeLeadingSessionType(candidate, sessionType);
            candidate = stripLeadingKnownSessionType(candidate);
            if (!candidate.isBlank()) {
                return candidate;
            }
        }

        return extractAssistantTitle(normalizedInput, rawInput, sessionType);
    }

    private LocalTime extractAssistantStartTime(String normalizedInput) {
        String safeInput = removeDateTokensForTimeParsing(normalizedInput);

        Matcher rangeMatcher = ASSISTANT_TIME_RANGE_PATTERN.matcher(safeInput);
        if (rangeMatcher.find()) {
            LocalTime startFromRange = parseClock(rangeMatcher.group(1));
            if (startFromRange != null) {
                return startFromRange;
            }
        }

        Matcher matcher = ASSISTANT_TIME_PATTERN.matcher(safeInput);
        while (matcher.find()) {
            LocalTime parsed = parseClock(matcher.group());
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private LocalTime extractAssistantEndTime(String normalizedInput, LocalTime start) {
        String safeInput = removeDateTokensForTimeParsing(normalizedInput);

        Matcher rangeMatcher = ASSISTANT_TIME_RANGE_PATTERN.matcher(safeInput);
        if (rangeMatcher.find()) {
            LocalTime endFromRange = parseClock(rangeMatcher.group(2));
            if (endFromRange != null) {
                return endFromRange;
            }
        }

        Matcher matcher = ASSISTANT_TIME_PATTERN.matcher(safeInput);
        LocalTime first = null;
        LocalTime second = null;
        while (matcher.find()) {
            LocalTime parsed = parseClock(matcher.group());
            if (parsed == null) {
                continue;
            }
            if (first == null) {
                first = parsed;
            } else {
                second = parsed;
                break;
            }
        }
        if (second != null) {
            return second;
        }
        if (start != null) {
            return start.plusHours(1);
        }
        return null;
    }

    private String removeDateTokensForTimeParsing(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String withoutIso = ASSISTANT_ISO_DATE_PATTERN.matcher(input).replaceAll(" ");
        return ASSISTANT_SHORT_DATE_PATTERN.matcher(withoutIso).replaceAll(" ");
    }

    private String extractAssistantTitle(String normalizedInput, String rawInput, String sessionType) {
        Pattern nounBeforeSessionPattern = Pattern.compile(
                "(?i)(?:add|create|plan|schedule|ajoute|planifie|planifier)\\s+(?:a|an|the|une|un)?\\s*([a-zA-Z][a-zA-Z0-9 _-]{1,40}?)\\s+session\\b"
        );
        Matcher nounBeforeSessionMatcher = nounBeforeSessionPattern.matcher(rawInput);
        if (nounBeforeSessionMatcher.find()) {
            String candidate = sanitizeAssistantTitle(nounBeforeSessionMatcher.group(1));
            candidate = removeLeadingSessionType(candidate, sessionType);
            candidate = stripLeadingKnownSessionType(candidate);
            if (!candidate.isBlank()) {
                return candidate;
            }
        }

        String[] startTokens = {"session of", "session", "seance de", "seance d", "seance", "cours de", "cours d"};
        int start = -1;
        int startTokenLength = 0;
        for (String token : startTokens) {
            int index = normalizedInput.indexOf(token);
            if (index >= 0) {
                start = index;
                startTokenLength = token.length();
                break;
            }
        }

        if (start >= 0) {
            int from = Math.min(rawInput.length(), start + startTokenLength);
            String candidate = sanitizeAssistantTitle(rawInput.substring(from));
            candidate = removeLeadingSessionType(candidate, sessionType);
            candidate = stripLeadingKnownSessionType(candidate);
            if (!candidate.isBlank()) {
                return candidate;
            }
        }

        Pattern genericActionTailPattern = Pattern.compile(
                "(?i)(?:add|create|plan|schedule|ajoute|planifie|planifier)\\s+(?:a|an|the|une|un)?\\s*(.+)$"
        );
        Matcher genericActionTailMatcher = genericActionTailPattern.matcher(rawInput);
        if (genericActionTailMatcher.find()) {
            String fallbackCandidate = sanitizeAssistantTitle(genericActionTailMatcher.group(1));
            fallbackCandidate = removeLeadingSessionType(fallbackCandidate, sessionType);
            fallbackCandidate = stripLeadingKnownSessionType(fallbackCandidate);
            if (!fallbackCandidate.isBlank()) {
                return fallbackCandidate;
            }
        }

        return findKnownSessionTitleMention(normalizedInput);
    }

    private String removeLeadingSessionType(String titleCandidate, String sessionType) {
        if (titleCandidate == null || titleCandidate.isBlank() || sessionType == null || sessionType.isBlank()) {
            return titleCandidate == null ? "" : titleCandidate;
        }

        String trimmed = titleCandidate.trim();
        String normalizedTitle = normalizeAssistantTypeToken(trimmed);
        String normalizedType = normalizeAssistantTypeToken(sessionType);

        if (normalizedTitle.equals(normalizedType)) {
            return "";
        }

        String[] words = trimmed.split("\\s+");
        if (words.length > 0) {
            String firstWordNormalized = normalizeAssistantTypeToken(words[0]);
            if (firstWordNormalized.equals(normalizedType) && words.length > 1) {
                StringBuilder remainder = new StringBuilder();
                for (int i = 1; i < words.length; i++) {
                    if (remainder.length() > 0) {
                        remainder.append(' ');
                    }
                    remainder.append(words[i]);
                }
                return stripLeadingSessionKeyword(remainder.toString().trim());
            }
        }

        if (normalizedTitle.startsWith(normalizedType + " ")) {
            int typeWordCount = sessionType.trim().split("\\s+").length;
            if (words.length > typeWordCount) {
                StringBuilder remainder = new StringBuilder();
                for (int i = typeWordCount; i < words.length; i++) {
                    if (remainder.length() > 0) {
                        remainder.append(' ');
                    }
                    remainder.append(words[i]);
                }
                return stripLeadingSessionKeyword(remainder.toString().trim());
            }
            return "";
        }

        return stripLeadingSessionKeyword(trimmed);
    }

    private String stripLeadingKnownSessionType(String titleCandidate) {
        if (titleCandidate == null || titleCandidate.isBlank()) {
            return "";
        }

        String current = titleCandidate.trim();
        for (TypeSeance typeSeance : serviceTypeSeance.getAvailableTypes()) {
            String typeName = typeSeance == null ? null : typeSeance.getName();
            if (typeName == null || typeName.isBlank()) {
                continue;
            }
            String stripped = removeLeadingSessionType(current, typeName);
            if (!normalizeAssistant(stripped).equals(normalizeAssistant(current))) {
                current = stripped;
                break;
            }
        }
        return current.trim();
    }

    private String sanitizeAssistantTitle(String rawTitle) {
        if (rawTitle == null) {
            return "";
        }
        String candidate = rawTitle.trim();
        candidate = candidate.replaceAll("(?i)\\b(today|tomorrow|monday|tuesday|wednesday|thursday|friday|saturday|sunday|lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche|at|to|from|on|in|le)\\b.*$", "").trim();
        candidate = candidate.replaceAll("(?i)\\b\\d{1,2}[:h.]?\\d{0,2}\\b.*$", "").trim();
        candidate = candidate.replaceAll("(?i)\\b\\d{1,2}[/-]\\d{1,2}([/-]\\d{2,4})?\\b.*$", "").trim();
        candidate = candidate.replaceAll("^[^a-zA-Z]+", "").trim();
        candidate = stripLeadingSessionKeyword(candidate);
        candidate = candidate.replaceAll("[.,;:]+$", "").trim();
        return candidate;
    }

    private String stripLeadingSessionKeyword(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceFirst("(?i)^(session|seance|séance)\\s+", "").trim();
    }

    private String normalizeAssistantTypeToken(String value) {
        String normalized = normalizeAssistant(value);
        if (normalized.isBlank()) {
            return "";
        }
        return switch (normalized) {
            case "review", "revise", "revisions", "rev" -> "revision";
            case "course", "cours", "class" -> "course";
            case "tp", "practical" -> "tp";
            default -> normalized;
        };
    }

    private String normalizeAssistantTypeForSearch(String value) {
        String normalized = normalizeAssistant(value);
        if (normalized.isBlank()) {
            return "";
        }

        String[] words = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word == null || word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(normalizeAssistantTypeToken(word));
        }
        return builder.toString().trim();
    }

    private String findKnownSessionTitleMention(String normalizedInput) {
        Seance bestMatch = null;
        int bestLength = -1;
        for (Seance seance : sessionComboBox.getItems()) {
            String title = normalizeAssistant(seance.getTitre());
            if (title.isBlank()) {
                continue;
            }
            if (normalizedInput.contains(title) && title.length() > bestLength) {
                bestMatch = seance;
                bestLength = title.length();
            }
        }
        return bestMatch == null ? null : bestMatch.getTitre();
    }

    private boolean containsAny(String source, String... tokens) {
        if (source == null) {
            return false;
        }
        for (String token : tokens) {
            if (source.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeAssistant(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.toLowerCase(Locale.ROOT).trim();
    }

    private LocalDate parseIsoDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String raw = value.trim();

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ofPattern("dd/MM/uuuu"),
                DateTimeFormatter.ofPattern("d-M-uuuu"),
                DateTimeFormatter.ofPattern("dd-MM-uuuu"),
                DateTimeFormatter.ofPattern("d/M/uu"),
                DateTimeFormatter.ofPattern("d-M-uu")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDate parsed = LocalDate.parse(raw, formatter);
                if (parsed.getYear() < 100) {
                    parsed = parsed.plusYears(2000);
                }
                return parsed;
            } catch (DateTimeParseException ignored) {
                // Try next accepted format.
            }
        }

        try {
            return LocalDate.parse(raw);
        } catch (Exception exception) {
            return null;
        }
    }

    private LocalTime parseClock(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String raw = value.trim().toLowerCase(Locale.ROOT).replace('h', ':').replace('.', ':');
        boolean pm = raw.endsWith("pm");
        boolean am = raw.endsWith("am");
        if (pm || am) {
            raw = raw.replace("pm", "").replace("am", "").trim();
        }
        String[] parts = raw.split(":");
        try {
            int hour = Integer.parseInt(parts[0]);
            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            if (pm && hour < 12) {
                hour += 12;
            } else if (am && hour == 12) {
                hour = 0;
            }
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null;
            }
            return LocalTime.of(hour, minute);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private LocalTime normalizeAssistantTime(LocalTime time, boolean isEnd) {
        if (time == null) {
            return null;
        }

        int totalMinutes = time.getHour() * 60 + time.getMinute();
        int minMinutes = ASSISTANT_MIN_TIME.getHour() * 60 + ASSISTANT_MIN_TIME.getMinute();
        int maxMinutes = ASSISTANT_MAX_TIME.getHour() * 60 + ASSISTANT_MAX_TIME.getMinute();

        if (isEnd) {
            totalMinutes = Math.min(maxMinutes, totalMinutes);
        }
        if (totalMinutes < minMinutes) {
            totalMinutes = minMinutes;
        }
        if (totalMinutes > maxMinutes) {
            totalMinutes = maxMinutes;
        }

        int remainder = totalMinutes % ASSISTANT_TIME_STEP_MINUTES;
        if (remainder != 0) {
            totalMinutes += (ASSISTANT_TIME_STEP_MINUTES - remainder);
            if (totalMinutes > maxMinutes) {
                totalMinutes = maxMinutes;
            }
        }

        return LocalTime.of(totalMinutes / 60, totalMinutes % 60);
    }

    private void loadSessions() {
        sessionComboBox.setItems(FXCollections.observableArrayList(serviceSeance.getAll()));
    }

    private void refreshPlanningData() {
        allPlanningEntries = servicePlanning.getAll();
        planningByDate = new HashMap<>();
        for (Planning entry : allPlanningEntries) {
            planningByDate.computeIfAbsent(entry.getPlanningDate(), key -> new ArrayList<>()).add(entry);
        }
        updateCalendar();
        updateTodayPanel();
        updateUpcomingPanel();
        updatePlanningStatistics();
        updateFeedbackPendingBanner();
        refreshFeedbackQueue();
    }

    private void updatePlanningStatistics() {
        LocalDate today = LocalDate.now();
        LocalDate currentStart = today.minusDays(6);
        LocalDate previousStart = today.minusDays(13);
        LocalDate previousEnd = today.minusDays(7);

        double hoursCurrent = computePlannedHoursBetween(currentStart, today);
        double hoursPrevious = computePlannedHoursBetween(previousStart, previousEnd);
        setKpiValue(planningHoursValueLabel, formatHours(hoursCurrent));
        setTrendChip(planningHoursTrendLabel, hoursCurrent - hoursPrevious, "h vs last 7d", false);

        double completionCurrent = computeCompletionRateBetween(currentStart, today);
        double completionPrevious = computeCompletionRateBetween(previousStart, previousEnd);
        setKpiValue(planningCompletionValueLabel, formatPercent(completionCurrent));
        setTrendChip(planningCompletionTrendLabel, completionCurrent - completionPrevious, "pts vs last 7d", false);

        double feedbackCurrent = computeFeedbackAverageBetween(currentStart, today);
        double feedbackPrevious = computeFeedbackAverageBetween(previousStart, previousEnd);
        setKpiValue(planningFeedbackScoreValueLabel, formatScore(feedbackCurrent));
        setTrendChip(planningFeedbackScoreTrendLabel, feedbackCurrent - feedbackPrevious, "vs last 7d", false);

        int streak = computeCurrentPlanningStreak(today);
        int bestStreak = computeBestPlanningStreak();
        setKpiValue(planningStreakValueLabel, streak + " days");
        if (planningStreakTrendLabel != null) {
            planningStreakTrendLabel.setText("Best " + bestStreak + " days");
            planningStreakTrendLabel.getStyleClass().setAll("kpi-trend-chip", "neutral");
        }
    }

    private double computePlannedHoursBetween(LocalDate startInclusive, LocalDate endInclusive) {
        return allPlanningEntries.stream()
                .filter(entry -> isDateBetween(entry.getPlanningDate(), startInclusive, endInclusive))
                .mapToDouble(this::durationHours)
                .sum();
    }

    private double computeCompletionRateBetween(LocalDate startInclusive, LocalDate endInclusive) {
        List<Planning> periodEntries = allPlanningEntries.stream()
                .filter(entry -> isDateBetween(entry.getPlanningDate(), startInclusive, endInclusive))
                .toList();
        if (periodEntries.isEmpty()) {
            return 0;
        }
        long completed = periodEntries.stream()
                .filter(entry -> isSessionCompleted(entry.getPlanningDate(), entry.getEndTime()))
                .count();
        return (completed * 100.0) / periodEntries.size();
    }

    private double computeFeedbackAverageBetween(LocalDate startInclusive, LocalDate endInclusive) {
        List<Integer> scores = allPlanningEntries.stream()
                .filter(entry -> isDateBetween(entry.getPlanningDate(), startInclusive, endInclusive))
                .map(Planning::getFeedback)
                .map(this::toFeedbackCode)
                .filter(code -> code != null && !code.isBlank())
                .map(Integer::parseInt)
                .toList();
        if (scores.isEmpty()) {
            return 0;
        }
        return scores.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    private int computeCurrentPlanningStreak(LocalDate anchorDate) {
        int streak = 0;
        LocalDate cursor = anchorDate;
        while (hasPlanningOnDate(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private int computeBestPlanningStreak() {
        List<LocalDate> dates = allPlanningEntries.stream()
                .map(Planning::getPlanningDate)
                .distinct()
                .sorted()
                .toList();
        int best = 0;
        int current = 0;
        LocalDate previous = null;
        for (LocalDate date : dates) {
            if (previous == null || date.equals(previous.plusDays(1))) {
                current++;
            } else {
                current = 1;
            }
            best = Math.max(best, current);
            previous = date;
        }
        return best;
    }

    private boolean hasPlanningOnDate(LocalDate date) {
        return allPlanningEntries.stream().anyMatch(entry -> date.equals(entry.getPlanningDate()));
    }

    private boolean isDateBetween(LocalDate value, LocalDate startInclusive, LocalDate endInclusive) {
        if (value == null || startInclusive == null || endInclusive == null) {
            return false;
        }
        return !value.isBefore(startInclusive) && !value.isAfter(endInclusive);
    }

    private double durationHours(Planning entry) {
        if (entry == null || entry.getStartTime() == null || entry.getEndTime() == null || !entry.getEndTime().isAfter(entry.getStartTime())) {
            return 0;
        }
        return (entry.getEndTime().toSecondOfDay() - entry.getStartTime().toSecondOfDay()) / 3600.0;
    }

    private void setKpiValue(Label label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }

    private void setTrendChip(Label label, double delta, String suffix, boolean invertGoodSignal) {
        if (label == null) {
            return;
        }
        String sign = delta > 0.01 ? "+" : (delta < -0.01 ? "-" : "");
        double absolute = Math.abs(delta);
        label.setText(sign + formatNumber(absolute) + " " + suffix);

        String trendClass = "neutral";
        if (delta > 0.01) {
            trendClass = invertGoodSignal ? "down" : "up";
        } else if (delta < -0.01) {
            trendClass = invertGoodSignal ? "up" : "down";
        }
        label.getStyleClass().setAll("kpi-trend-chip", trendClass);
    }

    private String formatHours(double value) {
        return formatNumber(value) + " h";
    }

    private String formatPercent(double value) {
        return Math.round(value) + "%";
    }

    private String formatScore(double value) {
        if (value <= 0) {
            return "- / 5";
        }
        return formatNumber(value) + " / 5";
    }

    private String formatNumber(double value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    private void updateCalendar() {
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();

        for (int i = 0; i < 7; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / 7.0);
            column.setHgrow(Priority.ALWAYS);
            column.setFillWidth(true);
            calendarGrid.getColumnConstraints().add(column);
        }

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;
        LocalDate gridStart = firstOfMonth.minusDays(dayOfWeek);

        double calendarMinHeight = 0;
        for (int i = 0; i < 6; i++) {
            RowConstraints row = new RowConstraints();
            double rowHeight = computeCalendarRowHeight(gridStart, i);
            row.setPrefHeight(rowHeight);
            row.setVgrow(Priority.NEVER);
            row.setFillHeight(true);
            calendarGrid.getRowConstraints().add(row);
            calendarMinHeight += rowHeight;
        }
        calendarGrid.setPrefHeight(calendarMinHeight + (calendarGrid.getVgap() * 5));

        currentMonthLabel.setText(currentYearMonth.format(monthFormatter));
        LocalDate today = LocalDate.now();

        for (int index = 0; index < 42; index++) {
            LocalDate date = gridStart.plusDays(index);
            boolean isCurrentMonth = date.getMonth() == currentYearMonth.getMonth() && date.getYear() == currentYearMonth.getYear();
            boolean isToday = date.equals(today);
            int row = index / 7;
            int col = index % 7;
            calendarGrid.add(createCalendarCell(date, isCurrentMonth, isToday), col, row);
        }
    }

    private VBox createCalendarCell(LocalDate date, boolean isCurrentMonth, boolean isToday) {
        VBox cell = new VBox(4);
        cell.setFillWidth(true);
        cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        cell.setMinHeight(98);
        cell.setPadding(new Insets(8));
        cell.getStyleClass().add("planning-calendar-cell");
        if (!isCurrentMonth) {
            cell.getStyleClass().add("planning-calendar-cell-outside");
        }
        if (isToday) {
            cell.getStyleClass().add("planning-calendar-cell-today");
        }

        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.getStyleClass().add("planning-calendar-day-label");
        if (!isCurrentMonth) {
            dayLabel.getStyleClass().add("planning-calendar-day-label-outside");
        }
        if (isToday) {
            dayLabel.getStyleClass().add("planning-calendar-day-label-today");
        }
        cell.getChildren().add(dayLabel);

        List<Planning> entries = planningByDate.getOrDefault(date, List.of()).stream()
                .sorted(Comparator.comparing(Planning::getStartTime))
                .toList();
        boolean hasDayOffEntry = entries.stream().anyMatch(this::isDayOffPlanningEntry);
        if (hasDayOffEntry) {
            cell.getStyleClass().add("planning-calendar-cell-dayoff");
            StackPane dayOffMarker = new StackPane();
            dayOffMarker.getStyleClass().add("planning-dayoff-calendar-cell-marker");
            Label dayOffLabel = new Label("XX");
            dayOffLabel.getStyleClass().add("planning-dayoff-calendar-cell-marker-text");
            dayOffMarker.getChildren().add(dayOffLabel);
            VBox.setVgrow(dayOffMarker, Priority.ALWAYS);
            cell.getChildren().add(dayOffMarker);
        }
        for (Planning entry : entries) {
            if (hasDayOffEntry && isDayOffPlanningEntry(entry)) {
                continue;
            }
            cell.getChildren().add(createCalendarEntryCard(entry));
        }

        cell.setOnMouseEntered(event -> {
            if (!cell.getStyleClass().contains("planning-calendar-cell-hover")) {
                cell.getStyleClass().add("planning-calendar-cell-hover");
            }
        });
        cell.setOnMouseExited(event -> cell.getStyleClass().remove("planning-calendar-cell-hover"));
        configureCalendarCellDragAndDrop(cell, date);
        cell.setOnMouseClicked(event -> {
            if (!isCurrentMonth) {
                currentYearMonth = YearMonth.from(date);
                updateCalendar();
            }
            if (hasDayOffEntry) {
                showInfo("This day has a day off. Only Match planning is allowed on this date.");
            }
            resetForm();
            planningDatePicker.setValue(date);
            colorPicker.setValue(Color.web(findAvailableColor(date, null)));
            planningFormTitleLabel.setText("Add Planning");
            showForm(true);
            hideMessage();
        });
        GridPane.setHgrow(cell, Priority.ALWAYS);
        GridPane.setVgrow(cell, Priority.ALWAYS);
        return cell;
    }

    private double computeCalendarRowHeight(LocalDate gridStart, int rowIndex) {
        int rowMaxEntries = 0;
        int rowStartIndex = rowIndex * 7;
        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate date = gridStart.plusDays(rowStartIndex + dayOffset);
            int dayEntries = planningByDate.getOrDefault(date, List.of()).size();
            rowMaxEntries = Math.max(rowMaxEntries, dayEntries);
        }

        if (rowMaxEntries <= 0) {
            return BASE_CALENDAR_ROW_HEIGHT;
        }
        return BASE_CALENDAR_ROW_HEIGHT + (rowMaxEntries * CALENDAR_ENTRY_HEIGHT_STEP);
    }

    private HBox createCalendarEntryCard(Planning entry) {
        boolean matchEntry = isMatchPlanningEntry(entry);
        boolean dayOffEntry = !matchEntry && isDayOffPlanningEntry(entry);
        HBox card = new HBox(matchEntry ? 8 : 6);
        card.getStyleClass().add("planning-calendar-entry-card");
        if (matchEntry) {
            card.getStyleClass().add("planning-match-calendar-entry-card");
            card.setMinHeight(52);
        } else if (dayOffEntry) {
            card.getStyleClass().add("planning-dayoff-calendar-entry-card");
            card.setMinHeight(40);
        }
        if (isAssistantGeneratedEntry(entry)) {
            card.getStyleClass().add("planning-ai-calendar-entry-card");
        }
        card.setMaxWidth(Double.MAX_VALUE);

        Region colorStrip = new Region();
        colorStrip.getStyleClass().add("planning-calendar-entry-color");
        colorStrip.setStyle("-fx-background-color: " + entry.getColorHex() + "; -fx-background-radius: 4;");

        Label matchChip = null;

        VBox infoBox = new VBox(matchEntry ? 2 : 1);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        if (dayOffEntry) {
            infoBox.setAlignment(Pos.CENTER);
        }

        String sessionTitle = entry.getSeanceTitle() == null ? "Session" : entry.getSeanceTitle();
        if (matchEntry) {
            sessionTitle = "⚽ " + sessionTitle;
        } else if (dayOffEntry) {
            sessionTitle = "XX";
        }
        int maxTitleLength = matchEntry ? Integer.MAX_VALUE : 15;
        if (!matchEntry && sessionTitle.length() > maxTitleLength) {
            sessionTitle = sessionTitle.substring(0, maxTitleLength) + "...";
        }

        Label titleLabel = new Label(sessionTitle);
        titleLabel.setWrapText(matchEntry);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.getStyleClass().add("planning-calendar-entry-title");
        if (matchEntry) {
            titleLabel.getStyleClass().add("planning-match-calendar-entry-title");
            titleLabel.setWrapText(true);
            titleLabel.setTooltip(new Tooltip(sessionTitle));
        } else if (dayOffEntry) {
            titleLabel.getStyleClass().add("planning-dayoff-calendar-entry-title");
        }
        if (isAssistantGeneratedEntry(entry)) {
            titleLabel.getStyleClass().add("planning-ai-calendar-entry-title");
        }
        HBox titleRow = new HBox(matchEntry ? 8 : 4);
        titleRow.setAlignment(dayOffEntry ? Pos.CENTER : Pos.CENTER_LEFT);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        titleRow.getChildren().add(titleLabel);
        Label timeLabel = new Label(entry.getStartTime().format(calendarEntryFormatter) + " - " + entry.getEndTime().format(calendarEntryFormatter));
        timeLabel.getStyleClass().add("planning-calendar-entry-time");
        if (matchEntry) {
            timeLabel.getStyleClass().add("planning-match-calendar-entry-time");
            // Keep full start/end time on one line for match entries.
            timeLabel.setText(entry.getStartTime().format(calendarEntryFormatter) + " - " + entry.getEndTime().format(calendarEntryFormatter));
            timeLabel.setWrapText(false);
            timeLabel.setMaxWidth(Double.MAX_VALUE);
        } else if (dayOffEntry) {
            timeLabel.setText("");
            timeLabel.setManaged(false);
            timeLabel.setVisible(false);
        }
        infoBox.getChildren().addAll(titleRow, timeLabel);

        if (matchChip != null) {
            card.getChildren().addAll(colorStrip, matchChip, infoBox);
        } else if (dayOffEntry) {
            card.getChildren().add(infoBox);
        } else {
            card.getChildren().addAll(colorStrip, infoBox);
        }

        card.setOnMouseClicked(event -> {
            if (dayOffEntry) {
                showInfo("Day off is locked for this full day (00:00 - 23:59).");
                event.consume();
                return;
            }
            PlanningController.this.startEdit(entry);
            event.consume();
        });

        card.setOnDragDetected(event -> {
            if (dayOffEntry) {
                event.consume();
                return;
            }
            Dragboard dragboard = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(DRAG_ENTRY_PREFIX + entry.getId());
            dragboard.setContent(content);
            event.consume();
        });

        return card;
    }

    private boolean isAssistantGeneratedEntry(Planning entry) {
        if (entry == null) {
            return false;
        }

        if (aiGeneratedPlanningIds.contains(entry.getId())) {
            return true;
        }

        if (sessionComboBox == null || sessionComboBox.getItems() == null) {
            return false;
        }
        for (Seance seance : sessionComboBox.getItems()) {
            if (seance == null || seance.getId() != entry.getSeanceId()) {
                continue;
            }
            String description = seance.getDescription();
            return description != null && normalizeAssistant(description).contains("created from planning assistant");
        }
        return false;
    }

    private boolean isMatchPlanningEntry(Planning entry) {
        if (entry == null) {
            return false;
        }

        String title = normalizeAssistant(entry.getSeanceTitle());
        if (title.contains(" vs ")) {
            return true;
        }

        if (sessionComboBox == null || sessionComboBox.getItems() == null) {
            return false;
        }

        for (Seance seance : sessionComboBox.getItems()) {
            if (seance == null || seance.getId() != entry.getSeanceId()) {
                continue;
            }
            String typeName = normalizeAssistant(seance.getTypeSeanceName());
            String type = normalizeAssistant(seance.getTypeSeance());
            return SESSION_TYPE_MATCH.equals(typeName) || SESSION_TYPE_MATCH.equals(type);
        }

        return false;
    }

    private boolean isDayOffPlanningEntry(Planning entry) {
        if (entry == null) {
            return false;
        }

        String title = normalizeAssistant(entry.getSeanceTitle());
        if (title.contains(SESSION_TYPE_DAY_OFF) || title.contains(SESSION_TYPE_DAYOFF)) {
            return true;
        }

        if (sessionComboBox == null || sessionComboBox.getItems() == null) {
            return false;
        }

        for (Seance seance : sessionComboBox.getItems()) {
            if (seance == null || seance.getId() != entry.getSeanceId()) {
                continue;
            }
            String typeName = normalizeAssistant(seance.getTypeSeanceName());
            String type = normalizeAssistant(seance.getTypeSeance());
            return typeName.contains(SESSION_TYPE_DAY_OFF)
                    || type.contains(SESSION_TYPE_DAY_OFF)
                    || typeName.contains(SESSION_TYPE_DAYOFF)
                    || type.contains(SESSION_TYPE_DAYOFF);
        }

        return false;
    }

    private boolean isDayOffSeance(Seance seance) {
        if (seance == null) {
            return false;
        }
        String typeName = normalizeAssistant(seance.getTypeSeanceName());
        String type = normalizeAssistant(seance.getTypeSeance());
        return typeName.contains(SESSION_TYPE_DAY_OFF)
                || type.contains(SESSION_TYPE_DAY_OFF)
                || typeName.contains(SESSION_TYPE_DAYOFF)
                || type.contains(SESSION_TYPE_DAYOFF);
    }

    private void updateDayOffTimeMode(Seance selectedSeance) {
        boolean dayOffSelection = isDayOffSeance(selectedSeance);
        if (dayOffSelection) {
            startTimeComboBox.setValue(DAY_OFF_START_TIME);
            endTimeComboBox.setValue(DAY_OFF_END_TIME);
            if (colorPicker != null) {
                colorPicker.setValue(Color.web(DAY_OFF_COLOR_HEX));
            }
            showInfo("Day off uses full day automatically: 00:00 - 23:59 with a fixed color.");
        }
        setTimeFieldsDisabled(dayOffSelection);
        if (colorPicker != null) {
            colorPicker.setDisable(dayOffSelection);
        }
    }

    private void setTimeFieldsDisabled(boolean disabled) {
        if (startTimeComboBox != null) {
            startTimeComboBox.setDisable(disabled);
        }
        if (endTimeComboBox != null) {
            endTimeComboBox.setDisable(disabled);
        }
    }

    private String toCompactMatchTitle(String title) {
        if (title == null || title.isBlank()) {
            return "Match";
        }
        String lowered = normalizeAssistant(title);
        if (!lowered.contains(" vs ")) {
            return title;
        }

        String[] parts = title.split("(?i)\\s+vs\\s+", 2);
        if (parts.length < 2) {
            return title;
        }
        return toShortTeamName(parts[0]) + " vs " + toShortTeamName(parts[1]);
    }

    private String toCalendarMatchDisplayTitle(String title) {
        if (title == null || title.isBlank()) {
            return "MATCH";
        }

        String normalized = normalizeAssistant(title);
        if (normalized.contains(" vs ")) {
            String[] parts = title.split("(?i)\\s+vs\\s+", 2);
            if (parts.length == 2) {
                return toTeamAcronym(parts[0]) + " VS " + toTeamAcronym(parts[1]);
            }
        }

        return title;
    }

    private String toElegantMatchTitle(String title) {
        if (title == null || title.isBlank()) {
            return "Match";
        }
        String lowered = normalizeAssistant(title);
        if (!lowered.contains(" vs ")) {
            return shortenTeamName(cleanTeamPrefix(title));
        }

        String[] parts = title.split("(?i)\\s+vs\\s+", 2);
        if (parts.length < 2) {
            return shortenTeamName(cleanTeamPrefix(title));
        }
        String home = shortenTeamName(cleanTeamPrefix(parts[0]));
        String away = shortenTeamName(cleanTeamPrefix(parts[1]));
        return home + " vs " + away;
    }

    private String cleanTeamPrefix(String teamName) {
        if (teamName == null) {
            return "";
        }
        String cleaned = teamName.trim();
        String normalized = normalizeAssistant(cleaned);
        List<String> prefixes = List.of("fc ", "cf ", "ac ", "sc ", "rc ", "real ", "club ");
        for (String prefix : prefixes) {
            if (normalized.startsWith(prefix)) {
                return cleaned.substring(prefix.length()).trim();
            }
        }
        return cleaned;
    }

    private String shortenTeamName(String teamName) {
        if (teamName == null || teamName.isBlank()) {
            return "Team";
        }
        String cleaned = teamName.trim();
        if (cleaned.length() <= 24) {
            return cleaned;
        }
        return cleaned.substring(0, 24);
    }

    private String toShortTeamName(String teamName) {
        if (teamName == null || teamName.isBlank()) {
            return "Team";
        }
        String cleaned = teamName.trim();
        String[] tokens = cleaned.split("\\s+");
        if (tokens.length >= 2) {
            String first = tokens[0];
            String second = tokens[1];
            String combined = first + " " + second;
            return combined.length() <= 12 ? combined : first;
        }
        if (cleaned.length() <= 12) {
            return cleaned;
        }
        return cleaned.substring(0, 12);
    }

    private String pickRandomAssistantColor(int userId, LocalDate date) {
        List<String> availableColors = new ArrayList<>();
        for (String colorHex : COLOR_PALETTE) {
            if (!servicePlanning.isColorUsedOnDate(userId, date, colorHex, null)) {
                availableColors.add(colorHex);
            }
        }

        if (!availableColors.isEmpty()) {
            return availableColors.get(assistantRandom.nextInt(availableColors.size()));
        }

        // If palette is exhausted for the day, generate a random unique color.
        for (int attempt = 0; attempt < 60; attempt++) {
            String randomColor = String.format("#%02X%02X%02X",
                    assistantRandom.nextInt(256),
                    assistantRandom.nextInt(256),
                    assistantRandom.nextInt(256));
            if (!servicePlanning.isColorUsedOnDate(userId, date, randomColor, null)) {
                return randomColor;
            }
        }

        return COLOR_PALETTE.get(assistantRandom.nextInt(COLOR_PALETTE.size()));
    }

    private void trackAssistantGeneratedEntry(Planning createdEntry) {
        if (createdEntry == null) {
            return;
        }

        if (createdEntry.getId() > 0) {
            aiGeneratedPlanningIds.add(createdEntry.getId());
            return;
        }

        Planning bestMatch = null;
        for (Planning entry : allPlanningEntries) {
            if (entry == null) {
                continue;
            }
            if (entry.getSeanceId() == createdEntry.getSeanceId()
                    && createdEntry.getPlanningDate() != null
                    && createdEntry.getPlanningDate().equals(entry.getPlanningDate())
                    && createdEntry.getStartTime() != null
                    && createdEntry.getStartTime().equals(entry.getStartTime())
                    && createdEntry.getEndTime() != null
                    && createdEntry.getEndTime().equals(entry.getEndTime())) {
                if (bestMatch == null || entry.getId() > bestMatch.getId()) {
                    bestMatch = entry;
                }
            }
        }

        if (bestMatch != null && bestMatch.getId() > 0) {
            aiGeneratedPlanningIds.add(bestMatch.getId());
        }
    }

    private void configureCalendarCellDragAndDrop(VBox cell, LocalDate targetDate) {
        cell.setOnDragOver(event -> {
            Integer draggedId = extractDraggedPlanningId(event.getDragboard());
            if (draggedId != null) {
                Planning draggedEntry = findPlanningEntryById(draggedId);
                if (draggedEntry != null && !targetDate.equals(draggedEntry.getPlanningDate())) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
            }
            event.consume();
        });

        cell.setOnDragEntered(event -> {
            if (extractDraggedPlanningId(event.getDragboard()) != null
                    && !cell.getStyleClass().contains("planning-calendar-drop-target")) {
                cell.getStyleClass().add("planning-calendar-drop-target");
            }
            event.consume();
        });

        cell.setOnDragExited(event -> {
            cell.getStyleClass().remove("planning-calendar-drop-target");
            event.consume();
        });

        cell.setOnDragDropped(event -> {
            boolean success = false;
            Integer draggedId = extractDraggedPlanningId(event.getDragboard());
            if (draggedId != null) {
                Planning draggedEntry = findPlanningEntryById(draggedId);
                if (draggedEntry != null && !targetDate.equals(draggedEntry.getPlanningDate())) {
                    movePlanningEntryToDate(draggedEntry, targetDate);
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private Integer extractDraggedPlanningId(Dragboard dragboard) {
        if (dragboard == null || !dragboard.hasString()) {
            return null;
        }
        String payload = dragboard.getString();
        if (payload == null || !payload.startsWith(DRAG_ENTRY_PREFIX)) {
            return null;
        }
        try {
            return Integer.parseInt(payload.substring(DRAG_ENTRY_PREFIX.length()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Planning findPlanningEntryById(int planningId) {
        for (Planning entry : allPlanningEntries) {
            if (entry.getId() == planningId) {
                return entry;
            }
        }
        return null;
    }

    private void movePlanningEntryToDate(Planning entry, LocalDate targetDate) {
        Seance movingSeance = findSeanceById(entry.getSeanceId());
        if (!canApplyPlanningTime(entry.getUserId(), targetDate, entry.getStartTime(), entry.getEndTime(), entry.getId(), movingSeance, true)) {
            showErrorOn(pageMessageLabel,
                    "Move cancelled: another session already uses this time range on " + targetDate.format(shortDateFormatter) + ".");
            return;
        }

        entry.setPlanningDate(targetDate);
        boolean colorAlreadyUsed = servicePlanning.isColorUsedOnDate(
                entry.getUserId(),
                targetDate,
                entry.getColorHex(),
                entry.getId()
        );
        if (colorAlreadyUsed) {
            showErrorOn(pageMessageLabel,
                    "Move cancelled: this color is already used on " + targetDate.format(shortDateFormatter) + ".");
            return;
        }

        servicePlanning.update(entry);
        refreshPlanningData();

        String message = "Planning moved to " + targetDate.format(shortDateFormatter) + ".";
        showInfo(message);
    }

    private void updateTodayPanel() {
        LocalDate today = LocalDate.now();
        todayDateLabel.setText(today.format(fullDateFormatter));
        List<Planning> todayEntries = planningByDate.getOrDefault(today, List.of());
        todayEventsListView.setItems(FXCollections.observableArrayList(todayEntries));
    }

    private void updateUpcomingPanel() {
        LocalDate today = LocalDate.now();
        String query = normalize(upcomingSearchField.getText());
        String selectedMode = upcomingUnifiedFilterComboBox.getValue();
        String searchScope = resolveSearchScopeFromMode(selectedMode);
        String periodFilter = resolvePeriodFilterFromMode(selectedMode);
        String sortOption = resolveSortFromMode(selectedMode);

        List<Planning> upcomingEntries = allPlanningEntries.stream()
                .filter(entry -> !entry.getPlanningDate().isBefore(today))
                .filter(entry -> matchesPeriodFilter(entry, today, periodFilter))
                .filter(entry -> matchesSearch(entry, query, searchScope))
                .sorted(resolveUpcomingComparator(sortOption))
                .toList();
        upcomingEventsListView.setItems(FXCollections.observableArrayList(upcomingEntries));
    }

    private String resolveSearchScopeFromMode(String selectedMode) {
        if (UPCOMING_MODE_TITLE_AZ.equals(selectedMode) || UPCOMING_MODE_TITLE_ZA.equals(selectedMode)) {
            return SEARCH_SCOPE_TITLE;
        }
        if (UPCOMING_MODE_TIME.equals(selectedMode)) {
            return SEARCH_SCOPE_TIME;
        }
        if (UPCOMING_MODE_COLOR.equals(selectedMode)) {
            return SEARCH_SCOPE_COLOR;
        }
        if (UPCOMING_MODE_FEEDBACK.equals(selectedMode)) {
            return SEARCH_SCOPE_FEEDBACK;
        }
        return SEARCH_SCOPE_ALL;
    }

    private String resolvePeriodFilterFromMode(String selectedMode) {
        if (UPCOMING_MODE_TODAY.equals(selectedMode)) {
            return UPCOMING_FILTER_TODAY;
        }
        if (UPCOMING_MODE_WEEK.equals(selectedMode)) {
            return UPCOMING_FILTER_WEEK;
        }
        if (UPCOMING_MODE_MONTH.equals(selectedMode)) {
            return UPCOMING_FILTER_MONTH;
        }
        return UPCOMING_FILTER_ALL;
    }

    private String resolveSortFromMode(String selectedMode) {
        if (UPCOMING_MODE_ALL_LATEST.equals(selectedMode)) {
            return UPCOMING_SORT_LATEST;
        }
        if (UPCOMING_MODE_TITLE_AZ.equals(selectedMode)) {
            return UPCOMING_SORT_TITLE_AZ;
        }
        if (UPCOMING_MODE_TITLE_ZA.equals(selectedMode)) {
            return UPCOMING_SORT_TITLE_ZA;
        }
        if (UPCOMING_MODE_TIME.equals(selectedMode)) {
            return UPCOMING_SORT_START_TIME;
        }
        return UPCOMING_SORT_NEAREST;
    }

    private boolean matchesPeriodFilter(Planning entry, LocalDate today, String selectedFilter) {
        if (selectedFilter == null || UPCOMING_FILTER_ALL.equals(selectedFilter)) {
            return true;
        }
        LocalDate entryDate = entry.getPlanningDate();
        if (UPCOMING_FILTER_TODAY.equals(selectedFilter)) {
            return entryDate.equals(today);
        }
        if (UPCOMING_FILTER_WEEK.equals(selectedFilter)) {
            LocalDate weekStart = today.with(DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);
            return !entryDate.isBefore(weekStart) && !entryDate.isAfter(weekEnd);
        }
        if (UPCOMING_FILTER_MONTH.equals(selectedFilter)) {
            return entryDate.getYear() == today.getYear() && entryDate.getMonth() == today.getMonth();
        }
        return true;
    }

    private boolean matchesSearch(Planning entry, String query, String selectedScope) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String titleValue = normalize(entry.getSeanceTitle());
        String dateValue = normalize(entry.getPlanningDate().toString() + " " + entry.getPlanningDate().format(shortDateFormatter));
        String timeValue = normalize(formatEntryTime(entry));
        String colorValue = normalize(entry.getColorHex());
        String feedbackValue = normalize(entry.getFeedback());

        if (SEARCH_SCOPE_TITLE.equals(selectedScope)) {
            return titleValue.contains(query);
        }
        if (SEARCH_SCOPE_DATE.equals(selectedScope)) {
            return dateValue.contains(query);
        }
        if (SEARCH_SCOPE_TIME.equals(selectedScope)) {
            return timeValue.contains(query);
        }
        if (SEARCH_SCOPE_COLOR.equals(selectedScope)) {
            return colorValue.contains(query);
        }
        if (SEARCH_SCOPE_FEEDBACK.equals(selectedScope)) {
            return feedbackValue.contains(query);
        }

        return titleValue.contains(query)
                || dateValue.contains(query)
                || timeValue.contains(query)
                || colorValue.contains(query)
                || feedbackValue.contains(query);
    }

    private Comparator<Planning> resolveUpcomingComparator(String sortOption) {
        Comparator<Planning> byDateAsc = Comparator.comparing(Planning::getPlanningDate)
                .thenComparing(Planning::getStartTime);
        if (UPCOMING_SORT_LATEST.equals(sortOption)) {
            return byDateAsc.reversed();
        }
        if (UPCOMING_SORT_START_TIME.equals(sortOption)) {
            return Comparator.comparing(Planning::getStartTime)
                    .thenComparing(Planning::getPlanningDate)
                    .thenComparing(entry -> normalize(entry.getSeanceTitle()));
        }
        if (UPCOMING_SORT_TITLE_AZ.equals(sortOption)) {
            return Comparator.comparing((Planning entry) -> normalize(entry.getSeanceTitle()))
                    .thenComparing(Planning::getPlanningDate)
                    .thenComparing(Planning::getStartTime);
        }
        if (UPCOMING_SORT_TITLE_ZA.equals(sortOption)) {
            return Comparator.comparing((Planning entry) -> normalize(entry.getSeanceTitle()), Comparator.reverseOrder())
                    .thenComparing(Planning::getPlanningDate)
                    .thenComparing(Planning::getStartTime);
        }
        return byDateAsc;
    }

    private String normalize(String value) {
        return normalizeAssistant(value);
    }

    private void startEdit(Planning planningEntry) {
        editingPlanning = planningEntry;
        planningFormTitleLabel.setText("Edit Planning");
        for (Seance seance : sessionComboBox.getItems()) {
            if (seance.getId() == planningEntry.getSeanceId()) {
                sessionComboBox.setValue(seance);
                break;
            }
        }
        planningDatePicker.setValue(planningEntry.getPlanningDate());
        startTimeComboBox.setValue(planningEntry.getStartTime());
        endTimeComboBox.setValue(planningEntry.getEndTime());
        colorPicker.setValue(Color.web(planningEntry.getColorHex()));
        updateDayOffTimeMode(sessionComboBox.getValue());
        boolean hasEditableFeedback = hasFeedback(planningEntry);
        showPlanningEditFeedbackEditor(hasEditableFeedback);
        if (planningEditFeedbackComboBox != null) {
            String feedbackCode = toFeedbackCode(planningEntry.getFeedback());
            planningEditFeedbackComboBox.setValue(feedbackCode == null ? null : toFeedbackLabel(feedbackCode));
        }
        clearPlanningValidationErrors();
        showForm(true);
        hideMessage();
    }

    private void deletePlanning(Planning planningEntry) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Planning");
        alert.setHeaderText("Delete planned session?");
        alert.setContentText("You are about to delete the planning for \"" + planningEntry.getSeanceTitle() + "\".");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            showInfo("Planning deletion cancelled.");
            return;
        }
        servicePlanning.delete(planningEntry);
        if (editingPlanning != null && editingPlanning.getId() == planningEntry.getId()) {
            resetForm();
            showForm(false);
        }
        refreshPlanningData();
        showInfo("Planning deleted successfully.");
    }

    private void resetForm() {
        editingPlanning = null;
        planningFormTitleLabel.setText("Add Planning");
        sessionComboBox.setValue(null);
        planningDatePicker.setValue(null);
        startTimeComboBox.setValue(null);
        endTimeComboBox.setValue(null);
        setTimeFieldsDisabled(false);
        if (colorPicker != null) {
            colorPicker.setDisable(false);
        }
        colorPicker.setValue(Color.web(COLOR_PALETTE.get(0)));
        if (planningEditFeedbackComboBox != null) {
            planningEditFeedbackComboBox.setValue(null);
        }
        showPlanningEditFeedbackEditor(false);
        clearPlanningValidationErrors();
        hideMessage();
    }

    private void showPlanningEditFeedbackEditor(boolean visible) {
        if (planningEditFeedbackBox == null) {
            return;
        }
        planningEditFeedbackBox.setVisible(visible);
        planningEditFeedbackBox.setManaged(visible);
    }


    private boolean validateSession(Seance selectedSeance, boolean showMessage) {
        if (selectedSeance == null) {
            if (showMessage) {
                markFieldInvalid(sessionComboBox, sessionErrorLabel, "A session must be selected.");
            }
            return false;
        }
        clearFieldValidation(sessionComboBox, sessionErrorLabel);
        return true;
    }

    private boolean validateDate(LocalDate planningDate, boolean showMessage) {
        if (planningDate == null) {
            if (showMessage) {
                markFieldInvalid(planningDatePicker, dateErrorLabel, "A planning date is required.");
            }
            return false;
        }
        clearFieldValidation(planningDatePicker, dateErrorLabel);
        return true;
    }

    private boolean validateTimeRange(LocalTime startTime, LocalTime endTime, boolean showMessage) {
        boolean valid = true;

        if (startTime == null) {
            valid = false;
            if (showMessage) {
                markFieldInvalid(startTimeComboBox, startTimeErrorLabel, "Start time is required.");
            }
        } else {
            clearFieldValidation(startTimeComboBox, startTimeErrorLabel);
        }

        if (endTime == null) {
            valid = false;
            if (showMessage) {
                markFieldInvalid(endTimeComboBox, endTimeErrorLabel, "End time is required.");
            }
        } else {
            clearFieldValidation(endTimeComboBox, endTimeErrorLabel);
        }

        if (valid && !endTime.isAfter(startTime)) {
            valid = false;
            if (showMessage) {
                markFieldInvalid(endTimeComboBox, endTimeErrorLabel, "End time must be after start time.");
            }
        }

        return valid;
    }

    private boolean validateTimeOverlap(int userId, LocalDate planningDate, LocalTime startTime, LocalTime endTime, Seance selectedSeance, boolean showValidation) {
        if (planningDate == null || startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            return true;
        }

        boolean allowed = canApplyPlanningTime(
                userId,
                planningDate,
                startTime,
                endTime,
                editingPlanning == null ? null : editingPlanning.getId(),
                selectedSeance,
                showValidation
        );
        if (!allowed) {
            if (showValidation) {
                markFieldInvalid(startTimeComboBox, startTimeErrorLabel, "This time range overlaps another session on this day.");
                markFieldInvalid(endTimeComboBox, endTimeErrorLabel, "This time range overlaps another session on this day.");
            }
            return false;
        }

        return true;
    }

    private boolean canApplyPlanningTime(int userId,
                                         LocalDate planningDate,
                                         LocalTime startTime,
                                         LocalTime endTime,
                                         Integer excludedPlanningId,
                                         Seance selectedSeance,
                                         boolean enforceDayOffRuleMessage) {
        boolean hasOverlap = servicePlanning.hasTimeOverlap(userId, planningDate, startTime, endTime, excludedPlanningId);
        if (!hasOverlap) {
            return true;
        }

        boolean selectedIsMatch = isMatchSeance(selectedSeance);
        boolean selectedIsDayOff = isDayOffSeance(selectedSeance);
        if (!selectedIsMatch && !selectedIsDayOff) {
            return false;
        }

        for (Planning existing : allPlanningEntries) {
            if (existing == null || existing.getUserId() != userId) {
                continue;
            }
            if (excludedPlanningId != null && existing.getId() == excludedPlanningId) {
                continue;
            }
            if (!planningDate.equals(existing.getPlanningDate())) {
                continue;
            }
            if (!timeRangesOverlap(startTime, endTime, existing.getStartTime(), existing.getEndTime())) {
                continue;
            }

            boolean existingIsMatch = isMatchPlanningEntry(existing);
            boolean existingIsDayOff = isDayOffPlanningEntry(existing);
            boolean allowedPair = (selectedIsMatch && existingIsDayOff) || (selectedIsDayOff && existingIsMatch);
            if (!allowedPair) {
                return false;
            }
        }

        if (enforceDayOffRuleMessage && selectedIsDayOff && !selectedIsMatch) {
            showInfo("Day off can only coexist with Match on the same day.");
        }
        return true;
    }

    private boolean timeRangesOverlap(LocalTime startA, LocalTime endA, LocalTime startB, LocalTime endB) {
        if (startA == null || endA == null || startB == null || endB == null) {
            return false;
        }
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    private Seance findSeanceById(int seanceId) {
        if (sessionComboBox == null || sessionComboBox.getItems() == null) {
            return null;
        }
        for (Seance seance : sessionComboBox.getItems()) {
            if (seance != null && seance.getId() == seanceId) {
                return seance;
            }
        }
        return null;
    }

    private boolean isMatchSeance(Seance seance) {
        if (seance == null) {
            return false;
        }
        String typeName = normalizeAssistant(seance.getTypeSeanceName());
        String type = normalizeAssistant(seance.getTypeSeance());
        return SESSION_TYPE_MATCH.equals(typeName) || SESSION_TYPE_MATCH.equals(type);
    }

    private boolean validateColorForDate(LocalDate planningDate, String selectedColor, boolean showMessage) {
        if (planningDate == null || selectedColor == null || selectedColor.isBlank()) {
            clearFieldValidation(colorPicker, colorErrorLabel);
            return true;
        }

        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            return true;
        }

        boolean colorUsed = servicePlanning.isColorUsedOnDate(
                currentUser.getId(),
                planningDate,
                selectedColor,
                editingPlanning == null ? null : editingPlanning.getId()
        );
        if (colorUsed) {
            if (showMessage) {
                markFieldInvalid(colorPicker, colorErrorLabel, "This color is already used on that day.");
            }
            return false;
        }

        clearFieldValidation(colorPicker, colorErrorLabel);
        return true;
    }

    private boolean validateFeedbackSelection(boolean showMessage) {
        String feedback = getSelectedFeedback();
        if (feedback == null || feedback.isBlank()) {
            if (showMessage) {
                markFeedbackInvalid("Choose one feedback option.");
            }
            return false;
        }

        clearFeedbackValidation();
        return true;
    }

    private void clearPlanningValidationErrors() {
        clearFieldValidation(sessionComboBox, sessionErrorLabel);
        clearFieldValidation(planningDatePicker, dateErrorLabel);
        clearFieldValidation(startTimeComboBox, startTimeErrorLabel);
        clearFieldValidation(endTimeComboBox, endTimeErrorLabel);
        clearFieldValidation(colorPicker, colorErrorLabel);
    }

    private void markFeedbackInvalid(String message) {
        applyFeedbackErrorStyle(true);
        if (feedbackErrorLabel != null) {
            feedbackErrorLabel.setText(message);
            feedbackErrorLabel.setVisible(true);
            feedbackErrorLabel.setManaged(true);
        }
    }

    private void clearFeedbackValidation() {
        applyFeedbackErrorStyle(false);
        if (feedbackErrorLabel != null) {
            feedbackErrorLabel.setText("");
            feedbackErrorLabel.setVisible(false);
            feedbackErrorLabel.setManaged(false);
        }
    }

    private void applyFeedbackErrorStyle(boolean invalid) {
        for (Toggle toggle : feedbackToggleGroup.getToggles()) {
            if (!(toggle instanceof ToggleButton button)) {
                continue;
            }
            if (invalid) {
                if (!button.getStyleClass().contains("field-invalid")) {
                    button.getStyleClass().add("field-invalid");
                }
            } else {
                button.getStyleClass().remove("field-invalid");
            }
        }
    }

    private String getSelectedFeedback() {
        Toggle selectedToggle = feedbackToggleGroup.getSelectedToggle();
        if (selectedToggle == null || selectedToggle.getUserData() == null) {
            return null;
        }
        return toFeedbackCode(selectedToggle.getUserData().toString());
    }

    private void selectFeedback(String feedbackValue) {
        String normalizedCode = toFeedbackCode(feedbackValue);
        if (normalizedCode == null || normalizedCode.isBlank()) {
            feedbackToggleGroup.selectToggle(null);
            return;
        }
        for (Toggle toggle : feedbackToggleGroup.getToggles()) {
            if (normalizedCode.equals(String.valueOf(toggle.getUserData()))) {
                feedbackToggleGroup.selectToggle(toggle);
                return;
            }
        }
        feedbackToggleGroup.selectToggle(null);
    }

    private String toFeedbackCode(String rawFeedback) {
        if (rawFeedback == null || rawFeedback.isBlank()) {
            return null;
        }
        String value = rawFeedback.trim();
        return switch (value.toLowerCase()) {
            case "1", "very bad" -> "1";
            case "2", "bad" -> "2";
            case "3", "medium" -> "3";
            case "4", "good" -> "4";
            case "5", "excellent" -> "5";
            default -> null;
        };
    }

    private String toFeedbackLabel(String feedbackCode) {
        return switch (feedbackCode) {
            case "1" -> "Very Bad";
            case "2" -> "Bad";
            case "3" -> "Medium";
            case "4" -> "Good";
            case "5" -> "Excellent";
            default -> "Unknown";
        };
    }

    private boolean isSessionCompleted(LocalDate planningDate, LocalTime endTime) {
        if (planningDate == null || endTime == null) {
            return false;
        }
        LocalDateTime endDateTime = LocalDateTime.of(planningDate, endTime);
        return !endDateTime.isAfter(LocalDateTime.now());
    }

    private void updateFeedbackPendingBanner() {
        List<Planning> pendingEntries = allPlanningEntries.stream()
                 .filter(entry -> isSessionCompleted(entry.getPlanningDate(), entry.getEndTime()))
                 .filter(this::isAwaitingFeedback)
                 .toList();
        long pendingCount = pendingEntries.size();

        if (feedbackPendingBox == null || feedbackPendingLabel == null) {
            return;
        }

        if (pendingCount <= 0) {
            feedbackPendingBox.setVisible(false);
            feedbackPendingBox.setManaged(false);
            feedbackPendingLabel.setText("");
            updateFeedbackPendingCta(0);
            return;
        }

        String suffix = pendingCount == 1 ? " session awaiting feedback" : " sessions awaiting feedback";
        feedbackPendingLabel.setText(pendingCount + suffix);
        feedbackPendingBox.setVisible(true);
        feedbackPendingBox.setManaged(true);
        updateFeedbackPendingCta(pendingEntries.size());
        if (pendingCount == 0) {
            selectedFeedbackEntry = null;
            if (feedbackSelectedSessionLabel != null) {
                feedbackSelectedSessionLabel.setText("No sessions are awaiting feedback.");
            }
            feedbackToggleGroup.selectToggle(null);
            return;
        }

        if (selectedFeedbackEntry != null) {
            for (Planning entry : pendingEntries) {
                if (entry.getId() == selectedFeedbackEntry.getId()) {
                    feedbackPendingListView.getSelectionModel().select(entry);
                    return;
                }
            }
        }
        feedbackPendingListView.getSelectionModel().select(pendingEntries.get(0));
    }

    private void refreshFeedbackQueue() {
        if (feedbackPendingListView == null) {
            return;
        }

        List<Planning> completedEntries = allPlanningEntries.stream()
                .filter(entry -> isSessionCompleted(entry.getPlanningDate(), entry.getEndTime()))
                .sorted(Comparator.comparing(Planning::getPlanningDate).reversed()
                        .thenComparing(Planning::getStartTime).reversed())
                .toList();

        List<Planning> pendingEntries = completedEntries.stream()
                .filter(this::isAwaitingFeedback)
                .toList();

        boolean hasPendingEntries = !pendingEntries.isEmpty();
        if (feedbackPendingListContainer != null) {
            feedbackPendingListContainer.setManaged(hasPendingEntries);
            feedbackPendingListContainer.setVisible(hasPendingEntries);
        }

        feedbackPendingListView.setItems(FXCollections.observableArrayList(pendingEntries));
        updateFeedbackPendingCta(pendingEntries.size());
        if (pendingEntries.isEmpty()) {
            selectedFeedbackEntry = null;
            if (feedbackSelectedSessionLabel != null) {
                feedbackSelectedSessionLabel.setText("No sessions are awaiting feedback.");
            }
            feedbackToggleGroup.selectToggle(null);
            return;
        }

        if (selectedFeedbackEntry != null) {
            for (Planning entry : pendingEntries) {
                if (entry.getId() == selectedFeedbackEntry.getId()) {
                    feedbackPendingListView.getSelectionModel().select(entry);
                    return;
                }
            }
        }
        feedbackPendingListView.getSelectionModel().select(pendingEntries.get(0));
    }

    private void updateFeedbackPendingCta(long pendingCount) {
        if (feedbackOpenQueueButton == null) {
            return;
        }

        if (pendingCount <= 0) {
            feedbackOpenQueueButton.setVisible(false);
            feedbackOpenQueueButton.setManaged(false);
            feedbackOpenQueueButton.setDisable(true);
            return;
        }

        feedbackOpenQueueButton.setText("Open feedback queue (" + pendingCount + ")");
        feedbackOpenQueueButton.setDisable(false);
        feedbackOpenQueueButton.setVisible(true);
        feedbackOpenQueueButton.setManaged(true);
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isAwaitingFeedback(Planning entry) {
        return !hasFeedback(entry);
    }

    private boolean hasFeedback(Planning entry) {
        if (entry == null) {
            return false;
        }
        return toFeedbackCode(entry.getFeedback()) != null;
    }

    private void markFieldInvalid(Control field, Label errorLabel, String message) {
        if (field != null && !field.getStyleClass().contains("field-invalid")) {
            field.getStyleClass().add("field-invalid");
        }
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void clearFieldValidation(Control field, Label errorLabel) {
        if (field != null) {
            field.getStyleClass().remove("field-invalid");
        }
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    private void showForm(boolean visible) {
        planningBoardView.setManaged(!visible);
        planningBoardView.setVisible(!visible);
        planningFormPage.setManaged(visible);
        planningFormPage.setVisible(visible);
        if (feedbackPage != null && visible) {
            feedbackPage.setManaged(false);
            feedbackPage.setVisible(false);
        }
        if (feedbackPickerPage != null && visible) {
            feedbackPickerPage.setManaged(false);
            feedbackPickerPage.setVisible(false);
        }
    }

    private void showFeedbackPage(boolean visible) {
        if (feedbackPage == null) {
            return;
        }
        feedbackPage.setManaged(visible);
        feedbackPage.setVisible(visible);
        if (visible) {
            planningBoardView.setManaged(false);
            planningBoardView.setVisible(false);
            planningFormPage.setManaged(false);
            planningFormPage.setVisible(false);
            if (feedbackPickerPage != null) {
                feedbackPickerPage.setManaged(false);
                feedbackPickerPage.setVisible(false);
            }
            clearFeedbackValidation();
        } else {
            planningBoardView.setManaged(true);
            planningBoardView.setVisible(true);
            if (feedbackPickerPage != null) {
                feedbackPickerPage.setManaged(false);
                feedbackPickerPage.setVisible(false);
            }
        }
    }

    private void showFeedbackPickerPage(boolean visible) {
        if (feedbackPickerPage == null) {
            return;
        }
        feedbackPickerPage.setManaged(visible);
        feedbackPickerPage.setVisible(visible);

        if (visible) {
            planningBoardView.setManaged(false);
            planningBoardView.setVisible(false);
            planningFormPage.setManaged(false);
            planningFormPage.setVisible(false);
            if (feedbackPage != null) {
                feedbackPage.setManaged(false);
                feedbackPage.setVisible(false);
            }
        }
    }

    private String findAvailableColor(LocalDate date, Integer excludedPlanningId) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            return COLOR_PALETTE.get(0);
        }
        for (String colorHex : COLOR_PALETTE) {
            if (!servicePlanning.isColorUsedOnDate(currentUser.getId(), date, colorHex, excludedPlanningId)) {
                return colorHex;
            }
        }
        return COLOR_PALETTE.get(0);
    }

    private String toHex(Color color) {
        int red = (int) Math.round(color.getRed() * 255);
        int green = (int) Math.round(color.getGreen() * 255);
        int blue = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02X%02X%02X", red, green, blue);
    }

    private String toRgba(String hexColor, double alpha) {
        Color color = Color.web(hexColor);
        return String.format("rgba(%d, %d, %d, %.2f)",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255),
                alpha);
    }

    private String formatEntryTime(Planning entry) {
        return entry.getStartTime().format(timeFormatter) + " - " + entry.getEndTime().format(timeFormatter);
    }

    private String formatEntryDateTime(Planning entry) {
        return entry.getPlanningDate().format(shortDateFormatter) + " • " + formatEntryTime(entry);
    }

    private void sendPlanningCreationEmailAsync(User user, Planning planningEntry) {
        CompletableFuture
                .supplyAsync(() -> planningEmailNotificationService.sendPlanningCreatedEmail(user, planningEntry))
                .thenAccept(error -> {
                    if (error == null || error.isBlank()) {
                        return;
                    }
                    Platform.runLater(() -> showInfo("Planning saved, but email was not sent: " + error));
                });
    }

    private void showError(String message) {
        showErrorOn(formMessageLabel, message);
    }

    private void showInfo(String message) {
        Label target = planningFormPage.isVisible() ? formMessageLabel : pageMessageLabel;
        showStyledMessage(target, message,
                "-fx-background-color: rgba(139,92,246,0.12); -fx-border-color: rgba(139,92,246,0.35); -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #C4B5FD; -fx-padding: 12 16; -fx-font-size: 13px;");
    }

    private void hideMessage() {
        hideLabelMessage(pageMessageLabel);
        hideLabelMessage(formMessageLabel);
    }

    private void showErrorOn(Label label, String message) {
        label.setText(message);
        label.getStyleClass().setAll("auth-error");
        label.setStyle("");
        label.setVisible(true);
        label.setManaged(true);
        scheduleMessageHide(label, 6.0);
    }

    private void showStyledMessage(Label label, String message, String style) {
        label.setText(message);
        label.getStyleClass().clear();
        label.setStyle(style);
        label.setVisible(true);
        label.setManaged(true);
        scheduleMessageHide(label, 4.5);
    }

    private void hideLabelMessage(Label label) {
        if (label == null) {
            return;
        }
        PauseTransition transition = messageTimers.remove(label);
        if (transition != null) {
            transition.stop();
        }
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
        label.setStyle("");
    }

    private void scheduleMessageHide(Label label, double seconds) {
        if (label == null) {
            return;
        }
        PauseTransition previous = messageTimers.remove(label);
        if (previous != null) {
            previous.stop();
        }
        PauseTransition timer = new PauseTransition(Duration.seconds(seconds));
        timer.setOnFinished(event -> hideLabelMessage(label));
        messageTimers.put(label, timer);
        timer.play();
    }

    private final class PlanningEntryCell extends ListCell<Planning> {
        private final VBox root = new VBox(3);
        private final HBox topRow = new HBox(8);
        private final Region colorBar = new Region();
        private final VBox content = new VBox(4);
        private final Label titleLabel = new Label();
        private final Label dateTimeLabel = new Label();
        private final Label aiBadgeLabel = new Label("AI");
        private final Label matchBadgeLabel = new Label("⚽");
        private final HBox actionBox = new HBox(6);
        private final Button editButton = new Button();
        private final Button deleteButton = new Button();

        private PlanningEntryCell() {
            colorBar.setPrefWidth(5);
            colorBar.setMinWidth(5);
            colorBar.setMaxWidth(5);
            colorBar.setPrefHeight(44);

            titleLabel.getStyleClass().add("planning-upcoming-item-title");
            titleLabel.setWrapText(false);
            titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            titleLabel.setMaxWidth(Double.MAX_VALUE);
            dateTimeLabel.getStyleClass().add("planning-upcoming-item-time");
            dateTimeLabel.setWrapText(false);
            dateTimeLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            dateTimeLabel.setMaxWidth(Double.MAX_VALUE);
            aiBadgeLabel.getStyleClass().add("planning-ai-badge");
            aiBadgeLabel.setManaged(false);
            aiBadgeLabel.setVisible(false);
            matchBadgeLabel.getStyleClass().add("planning-match-badge");
            matchBadgeLabel.setManaged(false);
            matchBadgeLabel.setVisible(false);

            editButton.getStyleClass().addAll("btn-secondary", "planning-upcoming-action-btn", "planning-upcoming-action-icon");
            deleteButton.getStyleClass().addAll("btn-danger", "planning-upcoming-action-btn", "planning-upcoming-action-delete", "planning-upcoming-action-icon");
            editButton.setText(null);
            deleteButton.setText(null);
            editButton.setGraphic(new FontIcon("fth-edit-3:12"));
            deleteButton.setGraphic(new FontIcon("fth-trash-2:12"));
            editButton.setTooltip(new Tooltip("Edit"));
            deleteButton.setTooltip(new Tooltip("Delete"));
            editButton.setOnAction(event -> {
                Planning entry = getItem();
                if (entry != null) {
                    PlanningController.this.startEdit(entry);
                }
            });
            deleteButton.setOnAction(event -> {
                Planning entry = getItem();
                if (entry != null) {
                    PlanningController.this.deletePlanning(entry);
                }
            });

            actionBox.getChildren().addAll(matchBadgeLabel, aiBadgeLabel, editButton, deleteButton);
            actionBox.setAlignment(Pos.CENTER_RIGHT);
            actionBox.setMinWidth(Region.USE_PREF_SIZE);

            content.getChildren().addAll(titleLabel, dateTimeLabel);
            content.setFillWidth(true);
            content.setMinWidth(0);
            HBox.setHgrow(content, Priority.ALWAYS);
            topRow.getChildren().addAll(colorBar, content, actionBox);
            topRow.setAlignment(Pos.TOP_LEFT);

            root.setPadding(new Insets(8, 10, 8, 10));
            root.setFillWidth(true);
            root.getStyleClass().add("planning-upcoming-item-card");
            root.getChildren().add(topRow);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(Planning item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            colorBar.setStyle("-fx-background-color: " + item.getColorHex() + "; -fx-background-radius: 4;");
            titleLabel.setText(item.getSeanceTitle());
            dateTimeLabel.setText(item.getPlanningDate().format(shortDateFormatter) + "  •  " + formatEntryTime(item));

            boolean aiGenerated = isAssistantGeneratedEntry(item);
            boolean matchEntry = isMatchPlanningEntry(item);
            aiBadgeLabel.setVisible(aiGenerated);
            aiBadgeLabel.setManaged(aiGenerated);
            matchBadgeLabel.setVisible(matchEntry);
            matchBadgeLabel.setManaged(matchEntry);
            root.getStyleClass().remove("planning-ai-upcoming-item-card");
            root.getStyleClass().remove("planning-match-upcoming-item-card");
            if (aiGenerated) {
                root.getStyleClass().add("planning-ai-upcoming-item-card");
            }
            if (matchEntry) {
                root.getStyleClass().add("planning-match-upcoming-item-card");
            }
            setGraphic(root);
        }
    }

    private final class FeedbackPendingCell extends ListCell<Planning> {
        private final VBox root = new VBox(4);
        private final Label titleLabel = new Label();
        private final Label detailLabel = new Label();

        private FeedbackPendingCell() {
            root.getStyleClass().add("planning-feedback-item-card");
            root.setPadding(new Insets(10, 12, 10, 12));
            root.setOnMouseClicked(event -> {
                Planning entry = getItem();
                if (entry == null) {
                    return;
                }
                if (feedbackPendingListView != null) {
                    feedbackPendingListView.getSelectionModel().select(entry);
                }
                if (feedbackPage != null && feedbackPage.isVisible()) {
                    openFeedbackPickerFor(entry);
                }
                event.consume();
            });

            titleLabel.getStyleClass().add("planning-feedback-item-title");
            detailLabel.getStyleClass().add("planning-feedback-item-detail");
            root.getChildren().addAll(titleLabel, detailLabel);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(Planning item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            String status = isAwaitingFeedback(item)
                    ? "Awaiting feedback"
                    : "Feedback: " + toFeedbackCode(item.getFeedback()) + " (" + toFeedbackLabel(toFeedbackCode(item.getFeedback())) + ")";
            titleLabel.setText(item.getSeanceTitle());
            detailLabel.setText(item.getPlanningDate().format(shortDateFormatter) + " • " + formatEntryTime(item) + " • " + status);
            setGraphic(root);
        }
    }

    private enum AssistantCommandType {
        CREATE,
        DELETE,
        UPDATE,
        AWAITING_TIME,
        HELP,
        ERROR
    }

    private static final class AssistantCommand {
        private final AssistantCommandType type;
        private final String title;
        private final String sessionType;
        private final LocalDate date;
        private final LocalDate newDate;
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final boolean keepTime;
        private final String errorMessage;

        private AssistantCommand(AssistantCommandType type, String title, String sessionType, LocalDate date,
                                 LocalDate newDate, LocalTime startTime, LocalTime endTime,
                                 boolean keepTime, String errorMessage) {
            this.type = type;
            this.title = title;
            this.sessionType = sessionType;
            this.date = date;
            this.newDate = newDate;
            this.startTime = startTime;
            this.endTime = endTime;
            this.keepTime = keepTime;
            this.errorMessage = errorMessage;
        }

        private static AssistantCommand create(String title, String sessionType, LocalDate date, LocalTime startTime, LocalTime endTime) {
            return new AssistantCommand(AssistantCommandType.CREATE, title, sessionType, date, null, startTime, endTime, false, null);
        }

        private static AssistantCommand delete(String title, LocalDate date) {
            return new AssistantCommand(AssistantCommandType.DELETE, title, null, date, null, null, null, false, null);
        }

        private static AssistantCommand update(String title, String sessionType, LocalDate targetDate, LocalDate newDate,
                                               LocalTime startTime, LocalTime endTime, boolean keepTime) {
            return new AssistantCommand(AssistantCommandType.UPDATE, title, sessionType, targetDate, newDate, startTime, endTime, keepTime, null);
        }

        private static AssistantCommand awaitingTime(String title, String sessionType, LocalDate date) {
            return new AssistantCommand(AssistantCommandType.AWAITING_TIME, title, sessionType, date, null, null, null, false, null);
        }

        private static AssistantCommand help() {
            return new AssistantCommand(AssistantCommandType.HELP, null, null, null, null, null, null, false, null);
        }

        private static AssistantCommand error(String message) {
            return new AssistantCommand(AssistantCommandType.ERROR, null, null, null, null, null, null, false, message);
        }
    }

    // Assistant chat now uses simple ScrollPane + VBox bubbles.
}
