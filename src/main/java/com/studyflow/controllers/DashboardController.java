package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.models.EvaluationMatiere;
import com.studyflow.models.Pet;
import com.studyflow.models.Matiere;
import com.studyflow.models.User;
import com.studyflow.services.EvaluationMatiereService;
import com.studyflow.services.MatiereService;
import com.studyflow.services.PetService;
import com.studyflow.utils.PetUiSupport;
import com.studyflow.utils.UserSession;
import com.studyflow.utils.PetPreviewSupport;
import com.studyflow.pets.CatGlbPreview;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label streakLabel;
    @FXML private Label assignmentsDueLabel;
    @FXML private Label completedLabel;
    @FXML private Label studyHoursLabel;
    @FXML private Label cardsReviewedLabel;

    @FXML private AreaChart<String, Number> studyTimeChart;
    @FXML private BarChart<String, Number>  weeklyProgressChart;

    @FXML private VBox scheduleList;
    @FXML private VBox tasksList;
    @FXML private VBox activityList;
    @FXML private VBox courseProgressList;
    @FXML private VBox petHeroCard;
    @FXML private VBox petEmptyCard;
    @FXML private StackPane petHeroContainer;
    @FXML private Label petHeroSubLabel;
    @FXML private Label petHeroNameLabel;
    @FXML private Label petHeroRarityBadge;
    @FXML private Label petHeroMetaLabel;
    @FXML private Label petHeroLevelLabel;
    @FXML private Label petHeroCoinsLabel;
    @FXML private Label petHeroTypeLabel;
    @FXML private Label petHeroMoodLabel;
    @FXML private Label petHeroHungerLabel;
    @FXML private ProgressBar petHeroHungerBar;
    @FXML private Button petManageButton;
    @FXML private Button petMetaverseButton;
    @FXML private Button petCreateButton;

    @FXML private VBox quickCardDecks;
    @FXML private VBox quickCardAssignment;
    @FXML private VBox quickCardSchedule;
    @FXML private VBox quickCardWellness;

    private final EvaluationMatiereService evalService = new EvaluationMatiereService();
    private final MatiereService           matService  = new MatiereService();
    private PetService petService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            petService = new PetService();
        } catch (RuntimeException ignored) {
            petService = null;
        }
        setupWelcomeMessage();
        setupPetHero();
        setupStats();
        setupCourseProgress();
        setupStudyTimeChart();
        setupWeeklyProgressChart();
        setupScheduleList();
        setupTasksList();
        setupActivityList();
        setupQuickActions();
    }

    // Welcome
    private void setupWelcomeMessage() {
        int hour = LocalTime.now().getHour();
        String greeting = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
        User currentUser = UserSession.getInstance().getCurrentUser();
        String userName = (currentUser != null) ? currentUser.getFirstName() : "Student";
        welcomeLabel.setText(greeting + ", " + userName);
    }

    private void setupPetHero() {
        if (petManageButton != null) {
            petManageButton.setOnAction(event -> navigateTo("views/Notes.fxml"));
        }
        if (petMetaverseButton != null) {
            petMetaverseButton.setOnAction(event -> navigateTo("views/PetMetaverse.fxml"));
        }
        if (petCreateButton != null) {
            petCreateButton.setOnAction(event -> navigateTo("views/Notes.fxml"));
        }

        User currentUser = UserSession.getInstance().getCurrentUser();
        Pet pet = null;
        if (currentUser != null && petService != null && petService.isDatabaseAvailable()) {
            try {
                pet = petService.findByUserId(currentUser.getId()).map(petService::syncPetState).orElse(null);
            } catch (RuntimeException ignored) {
                pet = null;
            }
        }

        boolean hasPet = pet != null;
        if (petHeroCard != null) {
            petHeroCard.setVisible(hasPet);
            petHeroCard.setManaged(hasPet);
        }
        if (petEmptyCard != null) {
            petEmptyCard.setVisible(!hasPet);
            petEmptyCard.setManaged(!hasPet);
        }
        if (!hasPet || currentUser == null) {
            return;
        }

        renderPetHero3D(pet.getType());
        petHeroNameLabel.setText(pet.getName());
        petHeroRarityBadge.setText(pet.getRarity());
        petHeroMetaLabel.setText("Level " + pet.getLevel() + " - " + pet.getMood() + " - " + pet.getEvolutionStage());
        petHeroSubLabel.setText("Same companion flow as the web dashboard, with direct access to care and the metaverse.");
        petHeroLevelLabel.setText("Lv. " + pet.getLevel());
        petHeroCoinsLabel.setText(String.valueOf(currentUser.getCoins()));
        petHeroTypeLabel.setText(capitalize(pet.getType()));
        petHeroMoodLabel.setText(PetUiSupport.moodFromHunger(pet.getHunger()));
        petHeroHungerLabel.setText(pet.getHunger() + "/100");
        petHeroHungerBar.setProgress(Math.max(0, Math.min(1, (100 - pet.getHunger()) / 100d)));
    }

    private void renderPetHero3D(String type) {
        if (petHeroContainer == null) return;
        petHeroContainer.getChildren().setAll(PetPreviewSupport.createPreview(type, 224, 224));
    }

    // Stats using evaluation data
    private void setupStats() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            streakLabel.setText("0");
            assignmentsDueLabel.setText("0");
            completedLabel.setText("0");
            studyHoursLabel.setText("0");
            cardsReviewedLabel.setText("0");
            return;
        }
        try {
            List<EvaluationMatiere> evals = evalService.findByUser(currentUser.getId());

            // Total assessments
            assignmentsDueLabel.setText(String.valueOf(evals.size()));

            // Passed assessments (score >= 10)
            long completed = evals.stream().filter(e -> e.getScoreEval() >= 10).count();
            completedLabel.setText(String.valueOf(completed));

            // Study hours = total minutes / 60
            long totalMin = evals.stream().mapToLong(EvaluationMatiere::getDureeEvaluation).sum();
            studyHoursLabel.setText(String.valueOf(totalMin / 60));

            cardsReviewedLabel.setText(String.valueOf(evals.size() * 4));

            // Streak
            streakLabel.setText(String.valueOf(computeStreak(evals)));

        } catch (Exception e) {
            e.printStackTrace();
            streakLabel.setText("0");
            assignmentsDueLabel.setText("0");
            completedLabel.setText("0");
            studyHoursLabel.setText("0");
            cardsReviewedLabel.setText("0");
        }
    }

    private int computeStreak(List<EvaluationMatiere> evals) {
        if (evals.isEmpty()) return 0;
        java.util.Set<LocalDate> dates = evals.stream()
                .filter(e -> e.getDateEvaluation() != null)
                .map(EvaluationMatiere::getDateEvaluation)
                .collect(Collectors.toSet());
        int streak = 0;
        LocalDate cursor = LocalDate.now();
        while (dates.contains(cursor)) { streak++; cursor = cursor.minusDays(1); }
        return streak;
    }

    // Course progress from subject averages
    private void setupCourseProgress() {
        if (courseProgressList == null) return;
        courseProgressList.getChildren().clear();

        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return;

        try {
            List<EvaluationMatiere> evals = evalService.findByUser(currentUser.getId());

            if (evals.isEmpty()) {
                Label empty = new Label("No assessments yet - add one in My Assessments.");
                empty.setStyle("-fx-text-fill:#64748B;-fx-font-size:13px;");
                courseProgressList.getChildren().add(empty);
                return;
            }

            // Average score per subject
            Map<Integer, Double> avgBySubject = evals.stream().collect(
                    Collectors.groupingBy(EvaluationMatiere::getMatiereId,
                            Collectors.averagingDouble(EvaluationMatiere::getScoreEval)));

            // Count of assessments per subject
            Map<Integer, Long> countBySubject = evals.stream().collect(
                    Collectors.groupingBy(EvaluationMatiere::getMatiereId, Collectors.counting()));

            // Best score per subject
            Map<Integer, Double> bestBySubject = evals.stream().collect(
                    Collectors.groupingBy(EvaluationMatiere::getMatiereId,
                            Collectors.collectingAndThen(
                                    Collectors.maxBy(java.util.Comparator.comparingDouble(EvaluationMatiere::getScoreEval)),
                                    opt -> opt.map(EvaluationMatiere::getScoreEval).orElse(0.0))));

            avgBySubject.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                    .forEach(entry -> courseProgressList.getChildren().add(
                            buildCourseProgressRow(
                                    entry.getKey(),
                                    entry.getValue(),
                                    countBySubject.getOrDefault(entry.getKey(), 0L),
                                    bestBySubject.getOrDefault(entry.getKey(), 0.0)
                            )));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox buildCourseProgressRow(int matiereId, double avg, long count, double best) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 0, 10, 0));
        row.setStyle(
                "-fx-border-color:transparent transparent #1E293B transparent;" +
                        "-fx-border-width:0 0 1 0;"
        );

        // Subject name
        String name;
        try {
            Matiere m = matService.findById(matiereId);
            name = m != null ? m.getNomMatiere() : "Subject #" + matiereId;
        } catch (Exception e) {
            name = "Subject #" + matiereId;
        }

        // Color based on avg
        String color = avg >= 14 ? "#10B981" : avg >= 10 ? "#F59E0B" : "#F43F5E";
        String bgColor = avg >= 14 ? "rgba(16,185,129,0.15)" : avg >= 10 ? "rgba(245,158,11,0.15)" : "rgba(244,63,94,0.15)";

        // Icon box
        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(40, 40);
        iconBox.setMinSize(40, 40);
        iconBox.setMaxSize(40, 40);
        iconBox.setStyle("-fx-background-color:" + bgColor + ";-fx-background-radius:10;");
        FontIcon icon = new FontIcon("fth-book-open");
        icon.setIconSize(16);
        icon.setIconColor(Color.web(color));
        iconBox.getChildren().add(icon);

        // Subject info
        VBox info = new VBox(3);
        info.setMinWidth(150);
        info.setMaxWidth(150);

        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-text-fill:#F8FAFC;-fx-font-size:13px;-fx-font-weight:700;");
        nameLbl.setWrapText(true);

        Label subLbl = new Label(count + " assessment" + (count > 1 ? "s" : "") + "  |  Best: " + String.format("%.1f", best) + "/20");
        subLbl.setStyle("-fx-text-fill:#64748B;-fx-font-size:11px;");

        info.getChildren().addAll(nameLbl, subLbl);

        // Progress bar section
        VBox progressSection = new VBox(4);
        HBox.setHgrow(progressSection, Priority.ALWAYS);

        HBox progressHeader = new HBox();
        progressHeader.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label pct = new Label((int)((avg / 20.0) * 100) + "%");
        pct.setStyle("-fx-text-fill:" + color + ";-fx-font-size:12px;-fx-font-weight:700;");
        progressHeader.getChildren().addAll(spacer, pct);

        ProgressBar bar = new ProgressBar(avg / 20.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(8);
        bar.setStyle(
                "-fx-accent:" + color + ";" +
                        "-fx-background-color:#1E293B;" +
                        "-fx-background-radius:4;" +
                        "-fx-border-radius:4;"
        );
        progressSection.getChildren().addAll(progressHeader, bar);

        // Score badge
        Label scoreLbl = new Label(String.format("%.1f/20", avg));
        scoreLbl.setMinWidth(55);
        scoreLbl.setStyle(
                "-fx-text-fill:" + color + ";" +
                        "-fx-font-size:13px;" +
                        "-fx-font-weight:700;" +
                        "-fx-background-color:" + bgColor + ";" +
                        "-fx-padding:4 8;" +
                        "-fx-background-radius:8;"
        );

        row.getChildren().addAll(iconBox, info, progressSection, scoreLbl);
        return row;
    }

    // Charts
    private void setupStudyTimeChart() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Study Hours");

        try {
            if (currentUser != null) {
                List<EvaluationMatiere> evals = evalService.findByUser(currentUser.getId());
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");

                // Group total duration per day (last 7 entries)
                Map<LocalDate, Long> minByDay = evals.stream()
                        .filter(e -> e.getDateEvaluation() != null)
                        .collect(Collectors.groupingBy(
                                EvaluationMatiere::getDateEvaluation,
                                Collectors.summingLong(EvaluationMatiere::getDureeEvaluation)));

                minByDay.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .limit(7)
                        .forEach(entry -> series.getData().add(
                                new XYChart.Data<>(
                                        entry.getKey().format(fmt),
                                        entry.getValue() / 60.0
                                )));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fallback if no data
        if (series.getData().isEmpty()) {
            series.getData().add(new XYChart.Data<>("Mon", 2.5));
            series.getData().add(new XYChart.Data<>("Tue", 3.2));
            series.getData().add(new XYChart.Data<>("Wed", 4.1));
            series.getData().add(new XYChart.Data<>("Thu", 2.8));
            series.getData().add(new XYChart.Data<>("Fri", 3.5));
            series.getData().add(new XYChart.Data<>("Sat", 5.0));
            series.getData().add(new XYChart.Data<>("Sun", 4.2));
        }

        studyTimeChart.getData().add(series);
        studyTimeChart.setAnimated(true);
    }

    private void setupWeeklyProgressChart() {
        User currentUser = UserSession.getInstance().getCurrentUser();

        XYChart.Series<String, Number> passedSeries = new XYChart.Series<>();
        passedSeries.setName("Passed (>=10)");

        XYChart.Series<String, Number> failedSeries = new XYChart.Series<>();
        failedSeries.setName("Failed (<10)");

        try {
            if (currentUser != null) {
                List<EvaluationMatiere> evals = evalService.findByUser(currentUser.getId());
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");

                Map<LocalDate, List<EvaluationMatiere>> byDay = evals.stream()
                        .filter(e -> e.getDateEvaluation() != null)
                        .collect(Collectors.groupingBy(EvaluationMatiere::getDateEvaluation));

                byDay.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .limit(7)
                        .forEach(entry -> {
                            String label = entry.getKey().format(fmt);
                            long passed = entry.getValue().stream().filter(e -> e.getScoreEval() >= 10).count();
                            long failed = entry.getValue().stream().filter(e -> e.getScoreEval() < 10).count();
                            passedSeries.getData().add(new XYChart.Data<>(label, passed));
                            failedSeries.getData().add(new XYChart.Data<>(label, failed));
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fallback if no data
        if (passedSeries.getData().isEmpty()) {
            String[] days = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
            int[] passed  = {3, 5, 4, 6, 2, 4, 3};
            int[] failed  = {2, 1, 3, 1, 4, 2, 2};
            for (int i = 0; i < days.length; i++) {
                passedSeries.getData().add(new XYChart.Data<>(days[i], passed[i]));
                failedSeries.getData().add(new XYChart.Data<>(days[i], failed[i]));
            }
        }

        weeklyProgressChart.getData().addAll(passedSeries, failedSeries);
        weeklyProgressChart.setAnimated(true);
    }

    // Quick actions
    private void setupQuickActions() {
        if (quickCardAssignment != null)
            quickCardAssignment.setOnMouseClicked(e -> navigateTo("views/Courses.fxml"));
        if (quickCardDecks != null) {
            quickCardDecks.setOnMouseClicked(e -> navigateTo("views/Flashcards.fxml"));
            quickCardDecks.setStyle(quickCardDecks.getStyle() + "; -fx-cursor: hand;");
        }
        if (quickCardSchedule != null)
            quickCardSchedule.setOnMouseClicked(e -> navigateTo("views/Planning.fxml"));
        if (quickCardWellness != null)
            quickCardWellness.setOnMouseClicked(e -> navigateTo("views/Wellbeing.fxml"));
    }

    private void navigateTo(String fxmlPath) {
        MainController.loadContentInMainArea(fxmlPath);
    }

    // Schedule
    private void setupScheduleList() {
        scheduleList.getChildren().clear();
        addScheduleItem("Data Structures Lecture", "09:00 AM", "Room 301", "primary");
        addScheduleItem("Algorithm Study Group",   "11:30 AM", "Library",  "success");
        addScheduleItem("Database Lab",            "02:00 PM", "Lab 205",  "warning");
        addScheduleItem("Project Meeting",         "04:30 PM", "Online",   "accent");
    }

    private void addScheduleItem(String title, String time, String location, String color) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12));
        item.setStyle("-fx-background-color: transparent; -fx-background-radius: 12; -fx-cursor: hand;");

        Region colorBar = new Region();
        colorBar.setPrefWidth(4);
        colorBar.setPrefHeight(48);
        colorBar.setStyle("-fx-background-color: " + getColorHex(color) + "; -fx-background-radius: 2;");

        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #F8FAFC;");

        HBox details = new HBox(8);
        details.setAlignment(Pos.CENTER_LEFT);

        FontIcon clockIcon = new FontIcon("fth-clock");
        clockIcon.setIconSize(12);
        clockIcon.setIconColor(Color.web("#94A3B8"));

        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:12px;");

        Label separator = new Label("|");
        separator.setStyle("-fx-text-fill: #475569;");

        FontIcon locationIcon = new FontIcon("fth-map-pin");
        locationIcon.setIconSize(12);
        locationIcon.setIconColor(Color.web("#94A3B8"));

        Label locationLabel = new Label(location);
        locationLabel.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:12px;");

        details.getChildren().addAll(clockIcon, timeLabel, separator, locationIcon, locationLabel);
        content.getChildren().addAll(titleLabel, details);

        Label badge = new Label(color.toUpperCase().substring(0, 3));
        badge.getStyleClass().addAll("badge", color);
        badge.setStyle("-fx-font-size: 10px;");

        item.getChildren().addAll(colorBar, content, badge);
        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 12; -fx-cursor: hand;"));
        item.setOnMouseExited (e -> item.setStyle("-fx-background-color: transparent; -fx-background-radius: 12; -fx-cursor: hand;"));

        scheduleList.getChildren().add(item);
    }

    // Tasks
    private void setupTasksList() {
        tasksList.getChildren().clear();

        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            try {
                List<EvaluationMatiere> evals = evalService.findByUser(currentUser.getId());
                // Show the 4 most recent assessments as tasks
                evals.stream()
                        .filter(e -> e.getDateEvaluation() != null)
                        .sorted((a, b) -> b.getDateEvaluation().compareTo(a.getDateEvaluation()))
                        .limit(4)
                        .forEach(e -> {
                            String name;
                            try {
                                Matiere m = matService.findById(e.getMatiereId());
                                name = m != null ? m.getNomMatiere() : "Subject";
                            } catch (Exception ex) {
                                name = "Subject";
                            }
                            String priority = e.getPrioriteE() != null ? e.getPrioriteE().toLowerCase() : "medium";
                            String date = e.getDateEvaluation().format(DateTimeFormatter.ofPattern("MMM dd"));
                            addTaskItem(name + " - " + String.format("%.1f/%.0f", e.getScoreEval(), e.getNoteMaximaleEval()),
                                    "Score: " + String.format("%.1f", e.getScoreEval()), priority, date);
                        });
                if (!evals.isEmpty()) return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Fallback static tasks
        addTaskItem("Complete Algorithm Assignment", "CS301", "high",   "Jan 26");
        addTaskItem("Database ER Diagram",           "CS305", "medium", "Jan 27");
        addTaskItem("Read Chapter 5",                "CS202", "low",    "Jan 28");
        addTaskItem("Submit Lab Report",             "CS310", "high",   "Jan 29");
    }

    private void addTaskItem(String title, String course, String priority, String dueDate) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12));
        item.setStyle("-fx-background-color: transparent; -fx-background-radius: 12; -fx-cursor: hand;");

        Region checkbox = new Region();
        checkbox.setMinSize(20, 20);
        checkbox.setPrefSize(20, 20);
        checkbox.setMaxSize(20, 20);
        checkbox.setStyle("-fx-background-color: transparent; -fx-border-color: #475569; -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10;");

        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: 500; -fx-text-fill: #F8FAFC;-fx-font-size:13px;");
        titleLabel.setWrapText(true);

        Label courseLabel = new Label(course);
        courseLabel.setStyle("-fx-text-fill:#64748B;-fx-font-size:11px;");
        content.getChildren().addAll(titleLabel, courseLabel);

        VBox right = new VBox(4);
        right.setAlignment(Pos.CENTER_RIGHT);

        String priorityColor = "high".equalsIgnoreCase(priority) ? "danger"
                : "medium".equalsIgnoreCase(priority) ? "warning" : "success";
        Label priorityBadge = new Label(priority.toUpperCase());
        priorityBadge.getStyleClass().addAll("badge", priorityColor);
        priorityBadge.setStyle("-fx-font-size: 10px;");

        Label dueDateLabel = new Label(dueDate);
        dueDateLabel.setStyle("-fx-text-fill:#64748B;-fx-font-size:11px;");
        right.getChildren().addAll(priorityBadge, dueDateLabel);

        item.getChildren().addAll(checkbox, content, right);
        item.setOnMouseEntered(e -> {
            item.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 12; -fx-cursor: hand;");
            checkbox.setStyle("-fx-background-color: transparent; -fx-border-color: #A78BFA; -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10;");
        });
        item.setOnMouseExited(e -> {
            item.setStyle("-fx-background-color: transparent; -fx-background-radius: 12; -fx-cursor: hand;");
            checkbox.setStyle("-fx-background-color: transparent; -fx-border-color: #475569; -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10;");
        });

        tasksList.getChildren().add(item);
    }

    // Activity
    private void setupActivityList() {
        activityList.getChildren().clear();

        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            try {
                List<EvaluationMatiere> evals = evalService.findByUser(currentUser.getId());
                if (!evals.isEmpty()) {
                    evals.stream()
                            .filter(e -> e.getDateEvaluation() != null)
                            .sorted((a, b) -> b.getDateEvaluation().compareTo(a.getDateEvaluation()))
                            .limit(4)
                            .forEach(e -> {
                                String name;
                                try {
                                    Matiere m = matService.findById(e.getMatiereId());
                                    name = m != null ? m.getNomMatiere() : "Subject";
                                } catch (Exception ex) { name = "Subject"; }

                                boolean passed = e.getScoreEval() >= 10;
                                String iconLiteral = passed ? "fth-check-circle" : "fth-alert-circle";
                                String color      = passed ? "success" : "danger";
                                String msg = "Assessment in " + name + " - " +
                                        String.format("%.1f/%.0f", e.getScoreEval(), e.getNoteMaximaleEval());
                                String time = e.getDateEvaluation().format(DateTimeFormatter.ofPattern("MMM dd"));
                                addActivityItem(iconLiteral, color, msg, time);
                            });
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Fallback static activity
        addActivityItem("fth-check",     "primary", "Completed Data Structures Assignment", "2 hours ago");
        addActivityItem("fth-layers",    "warning",  "Reviewed 15 flashcards in Algorithms", "4 hours ago");
        addActivityItem("fth-zap",       "accent",   "7 day study streak achieved!",          "Yesterday");
        addActivityItem("fth-file-text", "success",  "Added new notes for Database Systems",  "Yesterday");
    }

    private void addActivityItem(String icon, String color, String message, String time) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.TOP_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(32, 32);
        iconBox.setMinSize(32, 32);
        iconBox.setMaxSize(32, 32);
        iconBox.getStyleClass().addAll("stat-icon-box", color);

        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(14);
        fontIcon.setIconColor(Color.web(getColorHex(color)));
        iconBox.getChildren().add(fontIcon);

        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 13px;");
        messageLabel.setWrapText(true);

        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-text-fill: #64748B;-fx-font-size:11px;");

        content.getChildren().addAll(messageLabel, timeLabel);
        item.getChildren().addAll(iconBox, content);
        activityList.getChildren().add(item);
    }

    // Helpers
    private String getColorHex(String color) {
        return switch (color) {
            case "primary" -> "#A78BFA";
            case "success" -> "#34D399";
            case "warning" -> "#FBBF24";
            case "danger"  -> "#FB7185";
            case "accent"  -> "#FB923C";
            default        -> "#94A3B8";
        };
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }
}
