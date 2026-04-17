package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.Seance;
import com.studyflow.models.User;
import com.studyflow.services.ServicePlanning;
import com.studyflow.services.ServiceSeance;
import com.studyflow.utils.UserSession;
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
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
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
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class PlanningController implements Initializable {

    private static final double BASE_CALENDAR_ROW_HEIGHT = 108;
    private static final double CALENDAR_ENTRY_HEIGHT_STEP = 30;

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
    @FXML private VBox feedbackPendingBox;
    @FXML private Label feedbackPendingLabel;
    @FXML private Button feedbackOpenQueueButton;
    @FXML private VBox feedbackPage;
    @FXML private VBox feedbackPickerPage;
    @FXML private VBox feedbackPendingListContainer;
    @FXML private ListView<PlanningEntry> feedbackPendingListView;
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
    @FXML private ToggleButton feedbackVeryBadButton;
    @FXML private ToggleButton feedbackBadButton;
    @FXML private ToggleButton feedbackMediumButton;
    @FXML private ToggleButton feedbackGoodButton;
    @FXML private ToggleButton feedbackExcellentButton;
    @FXML private Label feedbackErrorLabel;
    @FXML private Button saveFeedbackButton;
    @FXML private Button savePlanningButton;
    @FXML private ListView<PlanningEntry> todayEventsListView;
    @FXML private ListView<PlanningEntry> upcomingEventsListView;
    @FXML private TextField upcomingSearchField;
    @FXML private ComboBox<String> upcomingUnifiedFilterComboBox;

    private final ServicePlanning servicePlanning = new ServicePlanning();
    private final ServiceSeance serviceSeance = new ServiceSeance();
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
    private final DateTimeFormatter fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter calendarEntryFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter shortDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d");

    private YearMonth currentYearMonth;
    private final ToggleGroup feedbackToggleGroup = new ToggleGroup();
    private PlanningEntry selectedFeedbackEntry;
    private PlanningEntry editingPlanning;
    private List<PlanningEntry> allPlanningEntries = new ArrayList<>();
    private Map<LocalDate, List<PlanningEntry>> planningByDate = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentYearMonth = YearMonth.now();
        configureSessionComboBox();
        configureTimeComboBoxes();
        configurePlanningDatePickerReadability();
        configureListViews();
        configureFormInteractions();
        configureFeedbackControls();
        configureUpcomingControls();
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
        LocalTime startTime = startTimeComboBox.getValue();
        LocalTime endTime = endTimeComboBox.getValue();
        String selectedColor = toHex(colorPicker.getValue());

        clearPlanningValidationErrors();

        boolean hasError = !validateSession(selectedSeance, true)
                | !validateDate(planningDate, true)
                | !validateTimeRange(startTime, endTime, true)
                | !validateTimeOverlap(currentUser.getId(), planningDate, startTime, endTime, true)
                | !validateColorForDate(planningDate, selectedColor, true);

        if (hasError) {
            showError("Please fix the highlighted fields.");
            return;
        }

        PlanningEntry planningEntry = editingPlanning == null ? new PlanningEntry() : editingPlanning;
        planningEntry.setUserId(currentUser.getId());
        planningEntry.setSeanceId(selectedSeance.getId());
        planningEntry.setSeanceTitle(selectedSeance.getTitre());
        planningEntry.setPlanningDate(planningDate);
        planningEntry.setStartTime(startTime);
        planningEntry.setEndTime(endTime);
        planningEntry.setColorHex(selectedColor);

        boolean isCreation = editingPlanning == null;
        if (isCreation) {
            servicePlanning.add(planningEntry);
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

    private void openFeedbackPickerFor(PlanningEntry entry) {
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

    private void loadSessions() {
        sessionComboBox.setItems(FXCollections.observableArrayList(serviceSeance.getAll()));
    }

    private void refreshPlanningData() {
        allPlanningEntries = servicePlanning.getAll();
        planningByDate = new HashMap<>();
        for (PlanningEntry entry : allPlanningEntries) {
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
        List<PlanningEntry> periodEntries = allPlanningEntries.stream()
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
                .map(PlanningEntry::getFeedback)
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
                .map(PlanningEntry::getPlanningDate)
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

    private double durationHours(PlanningEntry entry) {
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

        List<PlanningEntry> entries = planningByDate.getOrDefault(date, List.of()).stream()
                .sorted(Comparator.comparing(PlanningEntry::getStartTime))
                .toList();
        for (PlanningEntry entry : entries) {
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

    private HBox createCalendarEntryCard(PlanningEntry entry) {
        HBox card = new HBox(6);
        card.getStyleClass().add("planning-calendar-entry-card");
        card.setMaxWidth(Double.MAX_VALUE);

        Region colorStrip = new Region();
        colorStrip.getStyleClass().add("planning-calendar-entry-color");
        colorStrip.setStyle("-fx-background-color: " + entry.getColorHex() + ";");

        VBox infoBox = new VBox(1);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        String sessionTitle = entry.getSeanceTitle() == null ? "Session" : entry.getSeanceTitle();
        if (sessionTitle.length() > 15) {
            sessionTitle = sessionTitle.substring(0, 15) + "...";
        }

        Label titleLabel = new Label(sessionTitle);
        titleLabel.getStyleClass().add("planning-calendar-entry-title");
        Label timeLabel = new Label(entry.getStartTime().format(calendarEntryFormatter) + " - " + entry.getEndTime().format(calendarEntryFormatter));
        timeLabel.getStyleClass().add("planning-calendar-entry-time");
        infoBox.getChildren().addAll(titleLabel, timeLabel);

        card.getChildren().addAll(colorStrip, infoBox);

        card.setOnMouseClicked(event -> {
            PlanningController.this.startEdit(entry);
            event.consume();
        });

        card.setOnDragDetected(event -> {
            Dragboard dragboard = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(DRAG_ENTRY_PREFIX + entry.getId());
            dragboard.setContent(content);
            event.consume();
        });

        return card;
    }

    private void configureCalendarCellDragAndDrop(VBox cell, LocalDate targetDate) {
        cell.setOnDragOver(event -> {
            Integer draggedId = extractDraggedPlanningId(event.getDragboard());
            if (draggedId != null) {
                PlanningEntry draggedEntry = findPlanningEntryById(draggedId);
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
                PlanningEntry draggedEntry = findPlanningEntryById(draggedId);
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

    private PlanningEntry findPlanningEntryById(int planningId) {
        for (PlanningEntry entry : allPlanningEntries) {
            if (entry.getId() == planningId) {
                return entry;
            }
        }
        return null;
    }

    private void movePlanningEntryToDate(PlanningEntry entry, LocalDate targetDate) {
        if (servicePlanning.hasTimeOverlap(entry.getUserId(), targetDate, entry.getStartTime(), entry.getEndTime(), entry.getId())) {
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
        List<PlanningEntry> todayEntries = planningByDate.getOrDefault(today, List.of());
        todayEventsListView.setItems(FXCollections.observableArrayList(todayEntries));
    }

    private void updateUpcomingPanel() {
        LocalDate today = LocalDate.now();
        String query = normalize(upcomingSearchField.getText());
        String selectedMode = upcomingUnifiedFilterComboBox.getValue();
        String searchScope = resolveSearchScopeFromMode(selectedMode);
        String periodFilter = resolvePeriodFilterFromMode(selectedMode);
        String sortOption = resolveSortFromMode(selectedMode);

        List<PlanningEntry> upcomingEntries = allPlanningEntries.stream()
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

    private boolean matchesPeriodFilter(PlanningEntry entry, LocalDate today, String selectedFilter) {
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

    private boolean matchesSearch(PlanningEntry entry, String query, String selectedScope) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String titleValue = normalize(entry.getSeanceTitle());
        String dateValue = normalize(entry.getPlanningDate().toString() + " " + entry.getPlanningDate().format(DateTimeFormatter.ofPattern("EEE, MMM d")));
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

    private Comparator<PlanningEntry> resolveUpcomingComparator(String sortOption) {
        Comparator<PlanningEntry> byDateAsc = Comparator.comparing(PlanningEntry::getPlanningDate)
                .thenComparing(PlanningEntry::getStartTime);
        if (UPCOMING_SORT_LATEST.equals(sortOption)) {
            return byDateAsc.reversed();
        }
        if (UPCOMING_SORT_START_TIME.equals(sortOption)) {
            return Comparator.comparing(PlanningEntry::getStartTime)
                    .thenComparing(PlanningEntry::getPlanningDate)
                    .thenComparing(entry -> normalize(entry.getSeanceTitle()));
        }
        if (UPCOMING_SORT_TITLE_AZ.equals(sortOption)) {
            return Comparator.comparing((PlanningEntry entry) -> normalize(entry.getSeanceTitle()))
                    .thenComparing(PlanningEntry::getPlanningDate)
                    .thenComparing(PlanningEntry::getStartTime);
        }
        if (UPCOMING_SORT_TITLE_ZA.equals(sortOption)) {
            return Comparator.comparing((PlanningEntry entry) -> normalize(entry.getSeanceTitle()), Comparator.reverseOrder())
                    .thenComparing(PlanningEntry::getPlanningDate)
                    .thenComparing(PlanningEntry::getStartTime);
        }
        return byDateAsc;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase().trim();
    }

    private void startEdit(PlanningEntry planningEntry) {
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
        clearPlanningValidationErrors();
        showForm(true);
        hideMessage();
    }

    private void deletePlanning(PlanningEntry planningEntry) {
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
        colorPicker.setValue(Color.web(COLOR_PALETTE.get(0)));
        clearPlanningValidationErrors();
        hideMessage();
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

    private boolean validateTimeOverlap(int userId, LocalDate planningDate, LocalTime startTime, LocalTime endTime, boolean showMessage) {
        if (planningDate == null || startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            return true;
        }

        boolean hasOverlap = servicePlanning.hasTimeOverlap(
                userId,
                planningDate,
                startTime,
                endTime,
                editingPlanning == null ? null : editingPlanning.getId()
        );
        if (hasOverlap) {
            if (showMessage) {
                markFieldInvalid(startTimeComboBox, startTimeErrorLabel, "This time range overlaps another session on this day.");
                markFieldInvalid(endTimeComboBox, endTimeErrorLabel, "This time range overlaps another session on this day.");
            }
            return false;
        }

        return true;
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
        long pendingCount = allPlanningEntries.stream()
                .filter(entry -> isSessionCompleted(entry.getPlanningDate(), entry.getEndTime()))
                .filter(this::isAwaitingFeedback)
                .count();

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
        updateFeedbackPendingCta(pendingCount);
    }

    private void refreshFeedbackQueue() {
        if (feedbackPendingListView == null) {
            return;
        }

        List<PlanningEntry> completedEntries = allPlanningEntries.stream()
                .filter(entry -> isSessionCompleted(entry.getPlanningDate(), entry.getEndTime()))
                .sorted(Comparator.comparing(PlanningEntry::getPlanningDate).reversed()
                        .thenComparing(PlanningEntry::getStartTime).reversed())
                .toList();

        List<PlanningEntry> pendingEntries = completedEntries.stream()
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
            for (PlanningEntry entry : pendingEntries) {
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

    private boolean isAwaitingFeedback(PlanningEntry entry) {
        return !hasFeedback(entry);
    }

    private boolean hasFeedback(PlanningEntry entry) {
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

    private String formatEntryTime(PlanningEntry entry) {
        return entry.getStartTime().format(timeFormatter) + " - " + entry.getEndTime().format(timeFormatter);
    }

    private String formatEntryDateTime(PlanningEntry entry) {
        return entry.getPlanningDate().format(shortDateFormatter) + " • " + formatEntryTime(entry);
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
    }

    private void showStyledMessage(Label label, String message, String style) {
        label.setText(message);
        label.getStyleClass().clear();
        label.setStyle(style);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideLabelMessage(Label label) {
        if (label == null) {
            return;
        }
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
        label.setStyle("");
    }

    private final class PlanningEntryCell extends ListCell<PlanningEntry> {
        private final VBox root = new VBox(8);
        private final HBox topRow = new HBox(10);
        private final Region colorBar = new Region();
        private final VBox content = new VBox(4);
        private final Label titleLabel = new Label();
        private final Label dateTimeLabel = new Label();
        private final HBox actionBox = new HBox(8);
        private final Button editButton = new Button("Edit");
        private final Button deleteButton = new Button("Delete");

        private PlanningEntryCell() {
            colorBar.setPrefWidth(5);
            colorBar.setMinWidth(5);
            colorBar.setMaxWidth(5);
            colorBar.setPrefHeight(44);

            titleLabel.getStyleClass().add("planning-upcoming-item-title");
            dateTimeLabel.getStyleClass().add("planning-upcoming-item-time");

            editButton.getStyleClass().add("btn-secondary");
            deleteButton.getStyleClass().add("btn-danger");
            editButton.setOnAction(event -> {
                PlanningEntry entry = getItem();
                if (entry != null) {
                    PlanningController.this.startEdit(entry);
                }
            });
            deleteButton.setOnAction(event -> {
                PlanningEntry entry = getItem();
                if (entry != null) {
                    PlanningController.this.deletePlanning(entry);
                }
            });

            actionBox.getChildren().addAll(editButton, deleteButton);
            actionBox.setAlignment(Pos.CENTER_RIGHT);

            content.getChildren().addAll(titleLabel, dateTimeLabel);
            HBox.setHgrow(content, Priority.ALWAYS);
            topRow.getChildren().addAll(colorBar, content, actionBox);

            root.setPadding(new Insets(14));
            root.getStyleClass().add("planning-upcoming-item-card");
            root.getChildren().add(topRow);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(PlanningEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            colorBar.setStyle("-fx-background-color: " + item.getColorHex() + "; -fx-background-radius: 4;");
            titleLabel.setText(item.getSeanceTitle());
            dateTimeLabel.setText(item.getPlanningDate().format(DateTimeFormatter.ofPattern("EEE, MMM d")) + "  •  " + formatEntryTime(item));
            setGraphic(root);
        }
    }

    private final class FeedbackPendingCell extends ListCell<PlanningEntry> {
        private final VBox root = new VBox(4);
        private final Label titleLabel = new Label();
        private final Label detailLabel = new Label();

        private FeedbackPendingCell() {
            root.getStyleClass().add("planning-feedback-item-card");
            root.setPadding(new Insets(10, 12, 10, 12));
            root.setOnMouseClicked(event -> {
                PlanningEntry entry = getItem();
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
        protected void updateItem(PlanningEntry item, boolean empty) {
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
}
