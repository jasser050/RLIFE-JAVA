package com.studyflow.controllers;

import com.studyflow.models.PlanningEntry;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
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

    private static final List<String> COLOR_PALETTE = List.of(
            "#8B5CF6", "#10B981", "#F59E0B", "#F43F5E", "#F97316", "#3B82F6", "#14B8A6", "#EAB308"
    );
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

    @FXML private Label currentMonthLabel;
    @FXML private Label todayDateLabel;
    @FXML private Label pageMessageLabel;
    @FXML private Label formMessageLabel;
    @FXML private Label planningFormTitleLabel;
    @FXML private GridPane calendarGrid;
    @FXML private HBox planningBoardView;
    @FXML private VBox planningFormPage;
    @FXML private ComboBox<Seance> sessionComboBox;
    @FXML private DatePicker planningDatePicker;
    @FXML private ComboBox<LocalTime> startTimeComboBox;
    @FXML private ComboBox<LocalTime> endTimeComboBox;
    @FXML private ColorPicker colorPicker;
    @FXML private Button savePlanningButton;
    @FXML private ListView<PlanningEntry> todayEventsListView;
    @FXML private ListView<PlanningEntry> upcomingEventsListView;
    @FXML private TextField upcomingSearchField;
    @FXML private ComboBox<String> upcomingSearchScopeComboBox;
    @FXML private ComboBox<String> upcomingFilterComboBox;
    @FXML private ComboBox<String> upcomingSortComboBox;

    private final ServicePlanning servicePlanning = new ServicePlanning();
    private final ServiceSeance serviceSeance = new ServiceSeance();
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
    private final DateTimeFormatter fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter calendarEntryFormatter = DateTimeFormatter.ofPattern("HH:mm");

    private YearMonth currentYearMonth;
    private PlanningEntry editingPlanning;
    private List<PlanningEntry> allPlanningEntries = new ArrayList<>();
    private Map<LocalDate, List<PlanningEntry>> planningByDate = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentYearMonth = YearMonth.now();
        configureSessionComboBox();
        configureTimeComboBoxes();
        configureListViews();
        configureFormInteractions();
        configureUpcomingControls();
        loadSessions();
        refreshPlanningData();
        resetForm();
        showForm(false);
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

        if (selectedSeance == null) {
            showError("A session must be selected.");
            return;
        }
        if (planningDate == null) {
            showError("A planning date is required.");
            return;
        }
        if (startTime == null || endTime == null) {
            showError("Start and end time are required.");
            return;
        }
        if (!endTime.isAfter(startTime)) {
            showError("End time must be after start time.");
            return;
        }
        if (servicePlanning.isColorUsedOnDate(currentUser.getId(), planningDate, selectedColor, editingPlanning == null ? null : editingPlanning.getId())) {
            showError("This color is already used on that day. Choose another one.");
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
    }

    private void configureFormInteractions() {
        planningDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && editingPlanning == null) {
                colorPicker.setValue(Color.web(findAvailableColor(newValue, null)));
            }
        });
    }

    private void configureUpcomingControls() {
        upcomingSearchScopeComboBox.setItems(FXCollections.observableArrayList(
                SEARCH_SCOPE_ALL,
                SEARCH_SCOPE_TITLE,
                SEARCH_SCOPE_DATE,
                SEARCH_SCOPE_TIME,
                SEARCH_SCOPE_COLOR,
                SEARCH_SCOPE_FEEDBACK
        ));
        upcomingSearchScopeComboBox.setValue(SEARCH_SCOPE_ALL);

        upcomingFilterComboBox.setItems(FXCollections.observableArrayList(
                UPCOMING_FILTER_ALL,
                UPCOMING_FILTER_TODAY,
                UPCOMING_FILTER_WEEK,
                UPCOMING_FILTER_MONTH
        ));
        upcomingFilterComboBox.setValue(UPCOMING_FILTER_ALL);

        upcomingSortComboBox.setItems(FXCollections.observableArrayList(
                UPCOMING_SORT_NEAREST,
                UPCOMING_SORT_LATEST,
                UPCOMING_SORT_START_TIME,
                UPCOMING_SORT_TITLE_AZ,
                UPCOMING_SORT_TITLE_ZA
        ));
        upcomingSortComboBox.setValue(UPCOMING_SORT_NEAREST);

        upcomingSearchField.textProperty().addListener((observable, oldValue, newValue) -> updateUpcomingPanel());
        upcomingSearchScopeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateUpcomingPanel());
        upcomingFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateUpcomingPanel());
        upcomingSortComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateUpcomingPanel());
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

        for (int i = 0; i < 6; i++) {
            RowConstraints row = new RowConstraints();
            row.setPercentHeight(100.0 / 6.0);
            row.setVgrow(Priority.ALWAYS);
            row.setFillHeight(true);
            calendarGrid.getRowConstraints().add(row);
        }

        currentMonthLabel.setText(currentYearMonth.format(monthFormatter));

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;
        LocalDate today = LocalDate.now();

        LocalDate gridStart = firstOfMonth.minusDays(dayOfWeek);
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
        int maxEntries = Math.min(entries.size(), 2);
        for (int i = 0; i < maxEntries; i++) {
            PlanningEntry entry = entries.get(i);
            String sessionTitle = entry.getSeanceTitle() == null ? "Session" : entry.getSeanceTitle();
            if (sessionTitle.length() > 14) {
                sessionTitle = sessionTitle.substring(0, 14) + "...";
            }

            Label eventLabel = new Label(entry.getStartTime().format(calendarEntryFormatter) + "  " + sessionTitle);
            eventLabel.setMaxWidth(Double.MAX_VALUE);
            eventLabel.getStyleClass().add("planning-calendar-entry");
            eventLabel.setStyle("-fx-background-color: " + toRgba(entry.getColorHex(), 0.18) +
                    "; -fx-border-color: transparent transparent transparent " + entry.getColorHex() +
                    "; -fx-border-width: 0 0 0 3;");
            cell.getChildren().add(eventLabel);
        }
        if (entries.size() > 2) {
            Label moreLabel = new Label("+" + (entries.size() - 2) + " more");
            moreLabel.getStyleClass().add("planning-calendar-more");
            cell.getChildren().add(moreLabel);
        }

        cell.setOnMouseEntered(event -> {
            if (!cell.getStyleClass().contains("planning-calendar-cell-hover")) {
                cell.getStyleClass().add("planning-calendar-cell-hover");
            }
        });
        cell.setOnMouseExited(event -> cell.getStyleClass().remove("planning-calendar-cell-hover"));
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

    private void updateTodayPanel() {
        LocalDate today = LocalDate.now();
        todayDateLabel.setText(today.format(fullDateFormatter));
        List<PlanningEntry> todayEntries = planningByDate.getOrDefault(today, List.of());
        todayEventsListView.setItems(FXCollections.observableArrayList(todayEntries));
    }

    private void updateUpcomingPanel() {
        LocalDate today = LocalDate.now();
        String query = normalize(upcomingSearchField.getText());
        String searchScope = upcomingSearchScopeComboBox.getValue();
        String periodFilter = upcomingFilterComboBox.getValue();
        String sortOption = upcomingSortComboBox.getValue();

        List<PlanningEntry> upcomingEntries = allPlanningEntries.stream()
                .filter(entry -> !entry.getPlanningDate().isBefore(today))
                .filter(entry -> matchesPeriodFilter(entry, today, periodFilter))
                .filter(entry -> matchesSearch(entry, query, searchScope))
                .sorted(resolveUpcomingComparator(sortOption))
                .toList();
        upcomingEventsListView.setItems(FXCollections.observableArrayList(upcomingEntries));
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
        hideMessage();
    }

    private void showForm(boolean visible) {
        planningBoardView.setManaged(!visible);
        planningBoardView.setVisible(!visible);
        planningFormPage.setManaged(visible);
        planningFormPage.setVisible(visible);
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

            titleLabel.setStyle("-fx-text-fill: #F8FAFC; -fx-font-size: 14px; -fx-font-weight: 700;");
            dateTimeLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");

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
            root.setStyle("-fx-background-color: #0F172A; -fx-border-color: #1E293B; -fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14;");
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
}
