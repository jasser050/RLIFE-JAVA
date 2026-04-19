package com.studyflow.controllers;

import com.studyflow.App;
import com.studyflow.services.EvaluationMatiereService;
import com.studyflow.services.MatiereService;
import com.studyflow.models.EvaluationMatiere;
import com.studyflow.models.Matiere;
import com.studyflow.models.User;
import com.studyflow.utils.UserSession;
import com.studyflow.services.AIQuizService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.css.PseudoClass;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
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

    // ── Stats cards ───────────────────────────────────────────────────────────
    @FXML private Label statTotal;
    @FXML private Label statMoyenne;
    @FXML private Label statLacunes;
    @FXML private Label statMeilleur;
    @FXML private HBox  statCards;
    @FXML private VBox smartQuizCard;

    // ── Header ────────────────────────────────────────────────────────────────
    @FXML private Label            pageTitle;
    @FXML private Label            pageSubtitle;
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button           btnHeaderAction;
    @FXML private FontIcon         btnHeaderIcon;
    @FXML private Button           btnSmartQuiz;

    // ── Vues principales ─────────────────────────────────────────────────────
    @FXML private VBox listView;
    @FXML private VBox formPanel;
    @FXML private VBox statsView;
    @FXML private VBox quizMainView;

    // ── Grid ──────────────────────────────────────────────────────────────────
    @FXML private FlowPane courseGrid;

    // ── Formulaire ────────────────────────────────────────────────────────────
    @FXML private Label              formTitle;
    @FXML private ComboBox<Matiere>  cmbMatiere;
    @FXML private TextField          fldScore;
    @FXML private TextField          fldNoteMax;
    @FXML private DatePicker         dpDate;
    @FXML private TextField          fldDuree;
    @FXML private ComboBox<String>   cmbPriorite;
    @FXML private Button             btnSave;
    @FXML private Label              formError;

    @FXML private Button btnPrioHaute;
    @FXML private Button btnPrioMoyenne;
    @FXML private Button btnPrioBasse;

    @FXML private Label errMatiere;
    @FXML private Label errScore;
    @FXML private Label errNoteMax;
    @FXML private Label errDuree;
    @FXML private Label errPriorite;
    @FXML private Label errDate;
    @FXML private ComboBox<String> cmbSection;
    @FXML private Label            errSection;

    // ── Statistics View ───────────────────────────────────────────────────────
    @FXML private BarChart<String, Number>  scoreBySubjectChart;
    @FXML private PieChart                  priorityPieChart;
    @FXML private LineChart<String, Number> progressLineChart;
    @FXML private HBox                      insightBox;

    // ══════════════════════════════════════════════════════════════════════════
    //  QUIZ — FXML bindings
    // ══════════════════════════════════════════════════════════════════════════
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

    @FXML private Label lblQuizFinalScore;
    @FXML private Label lblQuizFinalPercent;
    @FXML private Label lblQuizFinalLevel;
    @FXML private Label lblQuizCorrectCount;
    @FXML private Label lblQuizWrongCount;
    @FXML private Label lblQuizTotalTime;

    @FXML private Button btnGenerateAIQuiz;

    // ── Services ──────────────────────────────────────────────────────────────
    private final EvaluationMatiereService evalService = new EvaluationMatiereService();
    private final MatiereService matService = new MatiereService();

    // ── État assessments ──────────────────────────────────────────────────────
    private ObservableList<EvaluationMatiere> allEvals = FXCollections.observableArrayList();
    private EvaluationMatiere editTarget = null;
    private String selectedPriorite = null;

    // ── État quiz ─────────────────────────────────────────────────────────────
    private List<QuizQuestion> quizQuestions = new ArrayList<>();
    private int quizCurrentIndex = 0;
    private int quizScore = 0;
    private int quizAnsweredCount = 0;
    private String quizSelectedOpt = null;
    private Timeline quizTimer;
    private int quizSecondsElapsed = 0;

    private static final String[] COLORS = {"primary", "success", "accent", "warning", "danger"};
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    // ══════════════════════════════════════════════════════════════════════════
    //  QUIZ MODEL
    // ══════════════════════════════════════════════════════════════════════════
    private static class QuizQuestion {
        String question, correctAnswer, userAnswer, difficulty, category;
        List<String> options;
        QuizQuestion(String q, List<String> opts, String correct, String diff, String cat) {
            question = q; options = opts; correctAnswer = correct;
            difficulty = diff; category = cat;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ── Assessments setup ────────────────────────────────────────────────
        cmbPriorite.setItems(FXCollections.observableArrayList("High", "Medium", "Low"));
        sortCombo.setItems(FXCollections.observableArrayList(
                "Date ↓", "Date ↑", "Score ↓", "Score ↑", "Durée ↓", "Priority"));
        sortCombo.getSelectionModel().selectFirst();
        sortCombo.setOnAction(e -> applySort());

        fldScore.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*(\\.\\d*)?")) fldScore.setText(o);
        });
        fldNoteMax.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*(\\.\\d*)?")) fldNoteMax.setText(o);
        });
        fldDuree.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) fldDuree.setText(o);
        });

        try {
            List<String> sections = matService.findAll().stream()
                    .map(Matiere::getSectionMatiere)
                    .filter(Objects::nonNull).distinct().sorted()
                    .collect(Collectors.toList());
            cmbSection.setItems(FXCollections.observableArrayList(sections));
            cmbMatiere.setItems(FXCollections.observableArrayList());
        } catch (Exception e) { e.printStackTrace(); }

        searchField.textProperty().addListener((o, old, val) -> filterAndRender(val));

        dpDate.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isAfter(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color:#1E293B; -fx-text-fill:#475569;");
                } else {
                    setStyle("-fx-background-color:#0F172A; -fx-text-fill:#F8FAFC;");
                }
            }
        });

        // ── Quiz setup ───────────────────────────────────────────────────────
        cmbQuizDifficulty.setItems(FXCollections.observableArrayList("Easy", "Medium", "Hard"));
        cmbQuizDifficulty.getSelectionModel().selectFirst();
        cmbQuizCount.setItems(FXCollections.observableArrayList(5, 10, 15, 20));
        cmbQuizCount.getSelectionModel().select(1);

        try {
            List<String> sections = matService.findAll().stream()
                    .map(Matiere::getSectionMatiere)
                    .filter(Objects::nonNull).distinct().sorted()
                    .collect(Collectors.toList());
            cmbQuizSection.setItems(FXCollections.observableArrayList(sections));
        } catch (Exception e) { e.printStackTrace(); }

        showView("list");
        loadData();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  VIEW SWITCHER
    // ══════════════════════════════════════════════════════════════════════════
    private void showView(String view) {
        boolean isList = "list".equals(view);
        boolean isForm = "form".equals(view);
        boolean isStats = "stats".equals(view);
        boolean isQuiz = "quiz".equals(view);

        listView.setVisible(isList);       listView.setManaged(isList);
        formPanel.setVisible(isForm);      formPanel.setManaged(isForm);
        statsView.setVisible(isStats);     statsView.setManaged(isStats);
        quizMainView.setVisible(isQuiz);   quizMainView.setManaged(isQuiz);

        boolean showCards = isList || isStats;
        statCards.setVisible(showCards);
        statCards.setManaged(showCards);

        if (smartQuizCard != null) {
            smartQuizCard.setVisible(isList);
            smartQuizCard.setManaged(isList);
        }

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
            pageSubtitle.setText("Test your knowledge by section and subject");
            showQuizSubView("setup");
        }
    }

    private void showQuizSubView(String sub) {
        quizSetupView.setVisible("setup".equals(sub));
        quizSetupView.setManaged("setup".equals(sub));
        quizLoadingView.setVisible("loading".equals(sub));
        quizLoadingView.setManaged("loading".equals(sub));
        quizInProgressView.setVisible("quiz".equals(sub));
        quizInProgressView.setManaged("quiz".equals(sub));
        quizResultsView.setVisible("results".equals(sub));
        quizResultsView.setManaged("results".equals(sub));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ASSESSMENTS — Données & CRUD avec SERVICES
    // ══════════════════════════════════════════════════════════════════════════
    private void loadData() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            courseGrid.getChildren().clear();
            statTotal.setText("0");
            statMoyenne.setText("0");
            statLacunes.setText("0");
            statMeilleur.setText("0");
            return;
        }
        try {
            List<EvaluationMatiere> list = evalService.findByUser(currentUser.getId());
            allEvals.setAll(list);
            updateStats();
            renderCards(allEvals);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(allEvals.size()));
        double moy = allEvals.stream().mapToDouble(EvaluationMatiere::getScoreEval).average().orElse(0);
        statMoyenne.setText(String.format("%.1f", moy));
        long totalDuree = allEvals.stream().mapToLong(EvaluationMatiere::getDureeEvaluation).sum();
        statLacunes.setText(String.valueOf(totalDuree));
        double best = allEvals.stream().mapToDouble(EvaluationMatiere::getScoreEval).max().orElse(0);
        statMeilleur.setText(String.format("%.1f", best));
    }

    private void renderCards(List<EvaluationMatiere> list) {
        courseGrid.getChildren().clear();
        for (int i = 0; i < list.size(); i++) {
            courseGrid.getChildren().add(buildCard(list.get(i), COLORS[i % COLORS.length]));
        }
    }

    private VBox buildCard(EvaluationMatiere ev, String color) {
        VBox card = new VBox(0);
        card.getStyleClass().add("card");
        card.setPrefWidth(380);
        card.setStyle("-fx-cursor: hand;");

        Region colorBar = new Region();
        colorBar.setPrefHeight(4);
        colorBar.setStyle("-fx-background-color:" + getGradientForColor(color) + "; -fx-background-radius: 16 16 0 0;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(48, 48);
        iconBox.getStyleClass().addAll("stat-icon-box", color);
        FontIcon icon = new FontIcon("fth-clipboard");
        icon.setIconSize(22);
        icon.setIconColor(Color.web(getColorHex(color)));
        iconBox.getChildren().add(icon);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label scoreBadge = new Label(String.format("%.1f / %.0f", ev.getScoreEval(), ev.getNoteMaximaleEval()));
        scoreBadge.getStyleClass().addAll("badge", color);

        header.getChildren().addAll(iconBox, spacer, scoreBadge);

        Label nomLabel = new Label(getNomMatiere(ev.getMatiereId()));
        nomLabel.getStyleClass().add("courses-card-title");

        HBox dateRow = new HBox(6);
        dateRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon calIcon = new FontIcon("fth-calendar");
        calIcon.setIconSize(12);
        calIcon.setIconColor(Color.web("#94A3B8"));
        Label dateLabel = new Label(ev.getDateEvaluation() != null ? ev.getDateEvaluation().format(FMT) : "—");
        dateLabel.getStyleClass().add("text-small");
        dateRow.getChildren().addAll(calIcon, dateLabel);

        HBox stats = new HBox(12);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
                buildChip("fth-clock", ev.getDureeEvaluation() + " min", "#94A3B8"),
                buildChip("fth-alert-circle", ev.getPrioriteE() != null ? ev.getPrioriteE() : "—", getPriorityColor(ev.getPrioriteE())));

        double progress = ev.getNoteMaximaleEval() > 0 ? ev.getScoreEval() / ev.getNoteMaximaleEval() : 0;
        VBox progressSection = new VBox(8);
        HBox progressHeader = new HBox();
        progressHeader.setAlignment(Pos.CENTER_LEFT);
        Label pt = new Label("Score");
        pt.getStyleClass().add("text-small");
        Region ps = new Region();
        HBox.setHgrow(ps, Priority.ALWAYS);
        Label pv = new Label((int) (progress * 100) + "%");
        pv.getStyleClass().add("courses-progress-value");
        progressHeader.getChildren().addAll(pt, ps, pv);

        ProgressBar progressBar = new ProgressBar(progress);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add(color);
        progressSection.getChildren().addAll(progressHeader, progressBar);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(4, 0, 0, 0));

        Button btnEdit = new Button("Edit");
        styleBtn(btnEdit, getColorHex(color));
        btnEdit.setOnAction(e -> startEdit(ev));

        Button btnDel = new Button("Delete");
        styleBtn(btnDel, "#FB7185");
        btnDel.setOnAction(e -> deleteItem(ev));

        actions.getChildren().addAll(btnEdit, btnDel);
        content.getChildren().addAll(header, nomLabel, dateRow, stats, progressSection, actions);
        card.getChildren().addAll(colorBar, content);

        return card;
    }

    @FXML
    private void handleShowForm() {
        editTarget = null;
        clearForm();
        formTitle.setText("New Assessment");
        btnSave.setText("➕ Add Assessment");
        showView("form");
    }

    @FXML
    private void handleSave() {
        boolean ok = true;
        clearErrors();

        if (cmbSection.getValue() == null || cmbSection.getValue().isBlank()) {
            errSection.setText("⚠ Please select a section.");
            ok = false;
        }
        if (cmbMatiere.getValue() == null) {
            errMatiere.setText("⚠ Please select a subject.");
            ok = false;
        }

        double score = 0, noteMax = 0;
        try {
            if (fldScore.getText().isBlank()) throw new NumberFormatException();
            score = Double.parseDouble(fldScore.getText().trim());
            if (score < 0) {
                errScore.setText("⚠ Score cannot be negative.");
                ok = false;
            }
        } catch (NumberFormatException ex) {
            errScore.setText("⚠ Enter a valid number (e.g. 14.5).");
            ok = false;
        }

        try {
            if (fldNoteMax.getText().isBlank()) throw new NumberFormatException();
            noteMax = Double.parseDouble(fldNoteMax.getText().trim());
            if (noteMax <= 0) {
                errNoteMax.setText("⚠ Must be greater than 0.");
                ok = false;
            } else if (noteMax > 20) {
                errNoteMax.setText("⚠ Maximum grade cannot exceed 20.");
                ok = false;
            }
        } catch (NumberFormatException ex) {
            errNoteMax.setText("⚠ Enter a valid number (e.g. 20).");
            ok = false;
        }

        if (ok && score > noteMax) {
            errScore.setText("⚠ Score cannot exceed the maximum grade (" + noteMax + ").");
            ok = false;
        }

        int duree = 0;
        try {
            if (fldDuree.getText().isBlank()) throw new NumberFormatException();
            duree = Integer.parseInt(fldDuree.getText().trim());
            if (duree <= 0) {
                errDuree.setText("⚠ Duration must be greater than 0.");
                ok = false;
            }
        } catch (NumberFormatException ex) {
            errDuree.setText("⚠ Enter a whole number (e.g. 90).");
            ok = false;
        }

        if (selectedPriorite == null) {
            errPriorite.setText("⚠ Please select a priority.");
            ok = false;
        }

        if (dpDate.getValue() == null) {
            errDate.setText("⚠ Please select a date.");
            ok = false;
        } else if (dpDate.getValue().isAfter(LocalDate.now())) {
            errDate.setText("⚠ Date cannot be in the future.");
            ok = false;
        }

        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            showGlobalError("You must be logged in.");
            return;
        }

        if (!ok) return;

        try {
            EvaluationMatiere e = editTarget != null ? editTarget : new EvaluationMatiere();
            e.setScoreEval(score);
            e.setNoteMaximaleEval(noteMax);
            e.setDateEvaluation(dpDate.getValue());
            e.setDureeEvaluation(duree);
            e.setPrioriteE(selectedPriorite);
            e.setMatiereId(cmbMatiere.getValue().getId());
            e.setUserId(currentUser.getId());

            if (editTarget == null) {
                evalService.create(e, cmbMatiere.getValue().getId());
            } else {
                evalService.update(e);
            }

            clearForm();
            showView("list");
            loadData();
        } catch (Exception ex) {
            showGlobalError("Database error: " + ex.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        clearForm();
        showView("list");
    }

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
        } catch (Exception e) {
            e.printStackTrace();
        }
        fldScore.setText(String.valueOf(ev.getScoreEval()));
        fldNoteMax.setText(String.valueOf(ev.getNoteMaximaleEval()));
        dpDate.setValue(ev.getDateEvaluation());
        fldDuree.setText(String.valueOf(ev.getDureeEvaluation()));
        if (ev.getPrioriteE() != null) setPriorite(ev.getPrioriteE());
        formTitle.setText("Edit Assessment");
        btnSave.setText("💾 Update");
        showView("form");
    }

    private void deleteItem(EvaluationMatiere ev) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this assessment?", ButtonType.YES, ButtonType.CANCEL);
        a.setTitle("Confirm deletion");
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                try {
                    evalService.delete(ev.getIdEval());
                    loadData();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════════════════════════
    @FXML
    private void handleHeaderAction() {
        if (formPanel.isVisible() || statsView.isVisible() || quizMainView.isVisible()) {
            stopQuizTimer();
            showView("list");
        } else {
            handleShowForm();
        }
    }

    @FXML
    public void handleShowQuiz() {
        showView("quiz");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  QUIZ METHODS
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void handleQuizSectionChange() {
        String section = cmbQuizSection.getValue();
        if (section == null) return;
        try {
            List<Matiere> filtered = matService.findAll().stream()
                    .filter(m -> section.equals(m.getSectionMatiere()))
                    .collect(Collectors.toList());
            cmbQuizMatiere.setItems(FXCollections.observableArrayList(filtered));
            cmbQuizMatiere.setPromptText("Choose a subject…");
            cmbQuizMatiere.getSelectionModel().clearSelection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleStartQuiz() {
        lblQuizSetupError.setText("");
        if (cmbQuizSection.getValue() == null) {
            lblQuizSetupError.setText("⚠ Please select a section.");
            return;
        }
        if (cmbQuizMatiere.getValue() == null) {
            lblQuizSetupError.setText("⚠ Please select a subject.");
            return;
        }

        String subject = cmbQuizMatiere.getValue().getNomMatiere();
        String difficulty = cmbQuizDifficulty.getValue();
        int count = cmbQuizCount.getValue() != null ? cmbQuizCount.getValue() : 10;

        showQuizSubView("loading");
        lblQuizLoadingStatus.setText("Generating questions…");

        new Thread(() -> {
            try {
                Thread.sleep(700);
                List<QuizQuestion> generated = generateFallbackQuizQuestions(subject, difficulty, count);
                Platform.runLater(() -> {
                    quizQuestions = generated;
                    quizCurrentIndex = 0;
                    quizScore = 0;
                    quizAnsweredCount = 0;
                    startQuizSession();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showQuizSubView("setup"));
            }
        }).start();
    }

    private List<QuizQuestion> generateFallbackQuizQuestions(String subject, String difficulty, int count) {
        List<QuizQuestion> questions = new ArrayList<>();
        String lowerSubject = subject.toLowerCase();

        if (lowerSubject.contains("math") || lowerSubject.contains("mathématiques")) {
            questions.add(new QuizQuestion(
                    "What is the derivative of x²?",
                    Arrays.asList("x", "2x", "x²", "2"),
                    "2x", difficulty, "Mathematics"));
            questions.add(new QuizQuestion(
                    "Solve: 2x + 5 = 13",
                    Arrays.asList("x = 4", "x = 6", "x = 8", "x = 10"),
                    "x = 4", difficulty, "Mathematics"));
            questions.add(new QuizQuestion(
                    "What is the value of π (pi) approximately?",
                    Arrays.asList("2.14", "3.14", "4.14", "5.14"),
                    "3.14", difficulty, "Mathematics"));
        } else if (lowerSubject.contains("phys") || lowerSubject.contains("physics")) {
            questions.add(new QuizQuestion(
                    "What is Newton's second law?",
                    Arrays.asList("F = ma", "E = mc²", "V = IR", "P = VI"),
                    "F = ma", difficulty, "Physics"));
            questions.add(new QuizQuestion(
                    "What is the unit of force?",
                    Arrays.asList("Joule", "Watt", "Newton", "Pascal"),
                    "Newton", difficulty, "Physics"));
        } else if (lowerSubject.contains("prog") || lowerSubject.contains("programming") || lowerSubject.contains("java")) {
            questions.add(new QuizQuestion(
                    "What does JVM stand for?",
                    Arrays.asList("Java Virtual Machine", "Java Variable Method", "Java Version Manager", "Java Visual Machine"),
                    "Java Virtual Machine", difficulty, "Programming"));
            questions.add(new QuizQuestion(
                    "Which of these is a Java keyword?",
                    Arrays.asList("integer", "String", "static", "float"),
                    "static", difficulty, "Programming"));
        }

        if (questions.size() < count) {
            questions.add(new QuizQuestion(
                    "What is the capital of France?",
                    Arrays.asList("London", "Berlin", "Paris", "Madrid"),
                    "Paris", difficulty, "General Knowledge"));
            questions.add(new QuizQuestion(
                    "Which planet is known as the Red Planet?",
                    Arrays.asList("Mars", "Jupiter", "Venus", "Saturn"),
                    "Mars", difficulty, "General Knowledge"));
            questions.add(new QuizQuestion(
                    "What is the largest ocean on Earth?",
                    Arrays.asList("Atlantic Ocean", "Indian Ocean", "Arctic Ocean", "Pacific Ocean"),
                    "Pacific Ocean", difficulty, "General Knowledge"));
        }

        Collections.shuffle(questions);
        return questions.subList(0, Math.min(count, questions.size()));
    }

    private void startQuizSession() {
        showQuizSubView("quiz");
        startQuizTimer();
        showQuizQuestion();
    }

    private void startQuizTimer() {
        stopQuizTimer();
        quizSecondsElapsed = 0;
        quizTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            quizSecondsElapsed++;
            lblQuizTimer.setText(String.format("%d:%02d", quizSecondsElapsed / 60, quizSecondsElapsed % 60));
        }));
        quizTimer.setCycleCount(Timeline.INDEFINITE);
        quizTimer.play();
    }

    private void stopQuizTimer() {
        if (quizTimer != null) {
            quizTimer.stop();
            quizTimer = null;
        }
    }

    private void showQuizQuestion() {
        if (quizCurrentIndex >= quizQuestions.size()) {
            finishQuiz();
            return;
        }

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
        String[] letters = {"A", "B", "C", "D"};
        for (int i = 0; i < q.options.size(); i++) {
            String opt = q.options.get(i);
            String letter = i < letters.length ? letters[i] : String.valueOf(i + 1);
            Button btn = new Button(letter + ".  " + opt);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setUserData(opt);
            btn.setStyle(quizDefaultOptionStyle());
            btn.setOnAction(e -> handleQuizOptionSelected(btn, opt));
            quizVboxOptions.getChildren().add(btn);
        }
    }

    private void handleQuizOptionSelected(Button clicked, String opt) {
        quizSelectedOpt = opt;
        btnQuizNext.setDisable(false);
        quizVboxOptions.getChildren().forEach(n -> {
            if (n instanceof Button b) b.setStyle(quizDefaultOptionStyle());
        });
        clicked.setStyle(quizSelectedOptionStyle());
    }

    @FXML
    private void handleQuizNextQuestion() {
        QuizQuestion q = quizQuestions.get(quizCurrentIndex);
        q.userAnswer = quizSelectedOpt;
        boolean correct = quizSelectedOpt != null && quizSelectedOpt.equals(q.correctAnswer);
        if (correct) quizScore++;
        if (quizSelectedOpt != null) quizAnsweredCount++;
        showQuizFeedback(correct, q.correctAnswer);
    }

    private void showQuizFeedback(boolean correct, String correctAnswer) {
        btnQuizNext.setDisable(true);
        quizVboxOptions.getChildren().forEach(n -> {
            if (n instanceof Button b) {
                String raw = b.getText().length() > 4 ? b.getText().substring(4) : b.getText();
                if (raw.equals(correctAnswer))
                    b.setStyle(quizCorrectOptionStyle());
                else if (raw.equals(quizSelectedOpt) && !correct)
                    b.setStyle(quizWrongOptionStyle());
            }
        });
        new Timeline(new KeyFrame(Duration.millis(900), e -> {
            quizCurrentIndex++;
            showQuizQuestion();
        })).play();
    }

    private void finishQuiz() {
        stopQuizTimer();
        showQuizSubView("results");

        int total = quizQuestions.size();
        int percent = total > 0 ? (int) ((quizScore * 100.0) / total) : 0;

        lblQuizFinalScore.setText(quizScore + " / " + total);
        lblQuizFinalPercent.setText(percent + "%");
        lblQuizCorrectCount.setText(String.valueOf(quizScore));
        lblQuizWrongCount.setText(String.valueOf(total - quizScore));
        lblQuizTotalTime.setText(String.format("%d:%02d", quizSecondsElapsed / 60, quizSecondsElapsed % 60));

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
        for (int i = 0; i < quizQuestions.size(); i++) {
            QuizQuestion q = quizQuestions.get(i);
            boolean ok = q.correctAnswer.equals(q.userAnswer);
            VBox card = new VBox(6);
            card.setStyle("-fx-background-color:" + (ok ? "#064E3B" : "#4C0519") + ";"
                    + "-fx-border-color:" + (ok ? "#34D399" : "#FB7185") + ";"
                    + "-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:12 16;");
            Label lq = new Label((i + 1) + ".  " + q.question);
            lq.setWrapText(true);
            lq.setStyle("-fx-text-fill:#F8FAFC;-fx-font-size:13px;-fx-font-weight:600;");
            Label la = new Label("Your answer : " + (q.userAnswer != null ? q.userAnswer : "Not answered"));
            la.setStyle("-fx-text-fill:" + (ok ? "#34D399" : "#FB7185") + ";-fx-font-size:12px;");
            Label lc = new Label("✓  Correct : " + q.correctAnswer);
            lc.setStyle("-fx-text-fill:#34D399;-fx-font-size:12px;");
            card.getChildren().addAll(lq, la, lc);
            quizVboxDetailedResults.getChildren().add(card);
        }
    }

    @FXML
    private void handleQuizRetake() {
        stopQuizTimer();
        quizQuestions.forEach(q -> q.userAnswer = null);
        quizCurrentIndex = 0;
        quizScore = 0;
        quizAnsweredCount = 0;
        startQuizSession();
    }

    @FXML
    private void handleQuizBackToSetup() {
        stopQuizTimer();
        showQuizSubView("setup");
    }

    // ── Styles options quiz ────────────────────────────────────────────────
    private String quizDefaultOptionStyle() {
        return "-fx-background-color:#0F172A;-fx-border-color:#334155;-fx-border-width:1.5;"
                + "-fx-border-radius:10;-fx-background-radius:10;-fx-padding:12 16;"
                + "-fx-font-size:14px;-fx-text-fill:#CBD5E1;-fx-alignment:CENTER_LEFT;-fx-cursor:hand;";
    }

    private String quizSelectedOptionStyle() {
        return "-fx-background-color:#1E1B4B;-fx-border-color:#7C3AED;-fx-border-width:1.5;"
                + "-fx-border-radius:10;-fx-background-radius:10;-fx-padding:12 16;"
                + "-fx-font-size:14px;-fx-text-fill:white;-fx-alignment:CENTER_LEFT;-fx-cursor:hand;";
    }

    private String quizCorrectOptionStyle() {
        return "-fx-background-color:#064E3B;-fx-border-color:#34D399;-fx-border-width:1.5;"
                + "-fx-border-radius:10;-fx-background-radius:10;-fx-padding:12 16;"
                + "-fx-font-size:14px;-fx-text-fill:#34D399;-fx-alignment:CENTER_LEFT;";
    }

    private String quizWrongOptionStyle() {
        return "-fx-background-color:#4C0519;-fx-border-color:#FB7185;-fx-border-width:1.5;"
                + "-fx-border-radius:10;-fx-background-radius:10;-fx-padding:12 16;"
                + "-fx-font-size:14px;-fx-text-fill:#FB7185;-fx-alignment:CENTER_LEFT;";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ASSESSMENTS — Priorité et filtres
    // ══════════════════════════════════════════════════════════════════════════
    @FXML
    private void selectPrioHaute() {
        setPriorite("High");
    }

    @FXML
    private void selectPrioMoyenne() {
        setPriorite("Medium");
    }

    @FXML
    private void selectPrioBasse() {
        setPriorite("Low");
    }

    @FXML
    private void handleSectionChange() {
        String section = cmbSection.getValue();
        cmbMatiere.setValue(null);
        errSection.setText("");
        if (section == null || section.isBlank()) {
            cmbMatiere.setItems(FXCollections.observableArrayList());
            cmbMatiere.setPromptText("Select a section first…");
            return;
        }
        try {
            List<Matiere> filtered = matService.findAll().stream()
                    .filter(m -> section.equals(m.getSectionMatiere()))
                    .collect(Collectors.toList());
            cmbMatiere.setItems(FXCollections.observableArrayList(filtered));
            cmbMatiere.setPromptText(filtered.isEmpty() ? "No subjects for this section" : "Choose a subject…");
        } catch (Exception e) {
            e.printStackTrace();
            errSection.setText("⚠ Error loading subjects.");
        }
    }

    private void setPriorite(String valeur) {
        selectedPriorite = valeur;
        cmbPriorite.setValue(valeur);
        errPriorite.setText("");

        String baseH = "-fx-background-color:#1E293B;-fx-text-fill:#FB7185;-fx-border-color:#FB7185;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 20;-fx-font-size:13px;-fx-cursor:hand;";
        String baseM = "-fx-background-color:#1E293B;-fx-text-fill:#FBBF24;-fx-border-color:#FBBF24;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 20;-fx-font-size:13px;-fx-cursor:hand;";
        String baseL = "-fx-background-color:#1E293B;-fx-text-fill:#34D399;-fx-border-color:#34D399;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 20;-fx-font-size:13px;-fx-cursor:hand;";

        btnPrioHaute.setStyle(baseH);
        btnPrioMoyenne.setStyle(baseM);
        btnPrioBasse.setStyle(baseL);

        switch (valeur) {
            case "High":
                btnPrioHaute.setStyle("-fx-background-color:#F43F5E;-fx-text-fill:white;-fx-border-color:#F43F5E;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 20;-fx-font-size:13px;-fx-cursor:hand;-fx-font-weight:700;");
                break;
            case "Medium":
                btnPrioMoyenne.setStyle("-fx-background-color:#F59E0B;-fx-text-fill:white;-fx-border-color:#F59E0B;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 20;-fx-font-size:13px;-fx-cursor:hand;-fx-font-weight:700;");
                break;
            case "Low":
                btnPrioBasse.setStyle("-fx-background-color:#10B981;-fx-text-fill:white;-fx-border-color:#10B981;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 20;-fx-font-size:13px;-fx-cursor:hand;-fx-font-weight:700;");
                break;
        }
        updatePriorityButtons();
    }

    @FXML
    private void filterAll() {
        renderCards(allEvals);
    }

    @FXML
    private void filterBonne() {
        renderCards(allEvals.filtered(e -> e.getScoreEval() >= 14));
    }

    @FXML
    private void filterMoyenne() {
        renderCards(allEvals.filtered(e -> e.getScoreEval() >= 10 && e.getScoreEval() < 14));
    }

    @FXML
    private void filterFaible() {
        renderCards(allEvals.filtered(e -> e.getScoreEval() < 10));
    }

    @FXML
    private void filterHaute() {
        renderCards(allEvals.filtered(e -> "High".equalsIgnoreCase(e.getPrioriteE()) || "Haute".equalsIgnoreCase(e.getPrioriteE())));
    }

    private void filterAndRender(String term) {
        if (term == null || term.isBlank()) {
            renderCards(allEvals);
            return;
        }
        String low = term.toLowerCase();
        renderCards(allEvals.filtered(e ->
                String.valueOf(e.getScoreEval()).contains(low) ||
                        (e.getPrioriteE() != null && e.getPrioriteE().toLowerCase().contains(low)) ||
                        getNomMatiere(e.getMatiereId()).toLowerCase().contains(low)));
    }

    private void applySort() {
        String sel = sortCombo.getValue();
        if (sel == null) return;
        List<EvaluationMatiere> sorted = new ArrayList<>(allEvals);
        switch (sel) {
            case "Date ↓":
                sorted.sort(Comparator.comparing(EvaluationMatiere::getDateEvaluation, Comparator.nullsLast(Comparator.reverseOrder())));
                break;
            case "Date ↑":
                sorted.sort(Comparator.comparing(EvaluationMatiere::getDateEvaluation, Comparator.nullsLast(Comparator.naturalOrder())));
                break;
            case "Score ↓":
                sorted.sort(Comparator.comparingDouble(EvaluationMatiere::getScoreEval).reversed());
                break;
            case "Score ↑":
                sorted.sort(Comparator.comparingDouble(EvaluationMatiere::getScoreEval));
                break;
            case "Durée ↓":
                sorted.sort(Comparator.comparingInt(EvaluationMatiere::getDureeEvaluation).reversed());
                break;
            case "Priority":
                sorted.sort(Comparator.comparing(e -> priorityOrder(e.getPrioriteE())));
                break;
        }
        renderCards(sorted);
    }

    private int priorityOrder(String p) {
        if (p == null) return 99;
        switch (p) {
            case "High": case "Haute": return 0;
            case "Medium": case "Moyenne": return 1;
            case "Low": case "Basse": return 2;
            default: return 99;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STATISTICS
    // ══════════════════════════════════════════════════════════════════════════
    @FXML
    private void handleShowStats() {
        if (allEvals.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "No assessments yet — add one first.", ButtonType.OK).showAndWait();
            return;
        }
        populateStats();
        showView("stats");
    }

    private void populateStats() {
        statsView.getChildren().clear();
        statsView.setSpacing(16);
        statsView.setPadding(new Insets(0, 0, 24, 0));
        statsView.getChildren().add(buildKpiRow());

        HBox row2 = new HBox(16);
        row2.getChildren().addAll(buildSubjectBarsCard(), buildPriorityRingsCard());
        HBox.setHgrow(row2.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(row2.getChildren().get(1), Priority.ALWAYS);
        statsView.getChildren().add(row2);

        HBox row3 = new HBox(16);
        VBox lineCard = buildWebChartCard("Score trend", buildLineChartHtml());
        VBox barCard  = buildWebChartCard("Avg by subject", buildBarChartHtml());
        HBox.setHgrow(lineCard, Priority.ALWAYS);
        HBox.setHgrow(barCard,  Priority.ALWAYS);
        row3.getChildren().addAll(lineCard, barCard);
        statsView.getChildren().add(row3);

        statsView.getChildren().add(buildHeatmapCard());
        statsView.getChildren().add(buildInsightsCard());
    }

    private HBox buildKpiRow() {
        HBox row = new HBox(12);
        double avg      = allEvals.stream().mapToDouble(EvaluationMatiere::getScoreEval).average().orElse(0);
        long   totalMin = allEvals.stream().mapToLong(EvaluationMatiere::getDureeEvaluation).sum();
        long   above14  = allEvals.stream().filter(e -> e.getScoreEval() >= 14).count();

        LocalDate now      = LocalDate.now();
        LocalDate monthAgo = now.minusMonths(1);
        long thisMonth = allEvals.stream().filter(e -> e.getDateEvaluation() != null && e.getDateEvaluation().isAfter(monthAgo)).count();
        long lastMonth = allEvals.stream().filter(e -> e.getDateEvaluation() != null
                && e.getDateEvaluation().isAfter(now.minusMonths(2))
                && !e.getDateEvaluation().isAfter(monthAgo)).count();
        String deltaStr = (thisMonth >= lastMonth) ? "+" + (thisMonth - lastMonth) + " vs last month" : "-" + (lastMonth - thisMonth) + " vs last month";
        boolean deltaUp = thisMonth >= lastMonth;

        Map<Integer, Double> avgBySubject = allEvals.stream().collect(
                Collectors.groupingBy(EvaluationMatiere::getMatiereId, Collectors.averagingDouble(EvaluationMatiere::getScoreEval)));
        String bestSubj  = avgBySubject.entrySet().stream().max(Map.Entry.comparingByValue()).map(e -> getNomMatiere(e.getKey())).orElse("—");
        String worstSubj = avgBySubject.entrySet().stream().min(Map.Entry.comparingByValue()).map(e -> getNomMatiere(e.getKey())).orElse("—");
        double bestSubjAvg  = avgBySubject.values().stream().mapToDouble(d -> d).max().orElse(0);
        double worstSubjAvg = avgBySubject.values().stream().mapToDouble(d -> d).min().orElse(0);

        row.getChildren().addAll(
                buildKpiCard("Total assessments", String.valueOf(allEvals.size()), deltaStr, deltaUp, "#A78BFA"),
                buildKpiCard("Average score", String.format("%.1f / 20", avg), avg >= 10 ? "Passing average" : "Below passing", avg >= 10, "#34D399"),
                buildKpiCard("Best subject", bestSubj, String.format("avg %.1f", bestSubjAvg), true, "#38BDF8"),
                buildKpiCard("Weakest subject", worstSubj, String.format("avg %.1f — needs work", worstSubjAvg), false, "#FB7185"),
                buildKpiCard("Study time", totalMin >= 60 ? String.format("%.1fh", totalMin / 60.0) : totalMin + "min", above14 + " assessments ≥14/20", true, "#FBBF24")
        );
        row.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        return row;
    }

    private VBox buildKpiCard(String label, String value, String delta, boolean positive, String accentColor) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16));

        String bgColor = "#0F172A";
        String borderColor = "#1E293B";
        String textColor = "#F8FAFC";
        String labelColor = "#64748B";

        card.setStyle("-fx-background-color:" + bgColor + "; -fx-border-color:" + borderColor + "; -fx-border-width:1; -fx-border-radius:14; -fx-background-radius:14;");

        Region topBar = new Region();
        topBar.setPrefHeight(3);
        topBar.setMaxWidth(40);
        topBar.setStyle("-fx-background-color:" + accentColor + "; -fx-background-radius:2;");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill:" + labelColor + "; -fx-font-size:11px; -fx-font-weight:500;");

        Label val = new Label(value);
        val.setStyle("-fx-text-fill:" + textColor + "; -fx-font-size:20px; -fx-font-weight:700;");
        val.setWrapText(true);

        Label dlt = new Label(delta);
        String deltaColor = positive ? "#34D399" : "#FB7185";
        String deltaBg = positive ? "#0D2C1F" : "#2D0A0A";
        dlt.setStyle("-fx-text-fill:" + deltaColor + "; -fx-font-size:11px; -fx-padding:3 8; -fx-background-radius:20;"
                + "-fx-background-color:" + deltaBg + ";");

        card.getChildren().addAll(topBar, lbl, val, dlt);
        return card;
    }

    private VBox buildSubjectBarsCard() {
        VBox card = cardContainer("Score by subject (avg / 20)");
        VBox body = new VBox(10);
        Map<Integer, Double> avgBySubject = allEvals.stream().collect(
                Collectors.groupingBy(EvaluationMatiere::getMatiereId, Collectors.averagingDouble(EvaluationMatiere::getScoreEval)));
        avgBySubject.entrySet().stream().sorted(Map.Entry.<Integer, Double>comparingByValue().reversed()).forEach(entry -> {
            String name = getNomMatiere(entry.getKey());
            double avg  = entry.getValue();
            double pct  = (avg / 20.0) * 100;
            String color = avg >= 14 ? "#10B981" : avg >= 10 ? "#F59E0B" : "#F43F5E";
            String textColor = "#94A3B8";

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Label nameLbl = new Label(name.length() > 12 ? name.substring(0, 11) + "…" : name);
            nameLbl.setMinWidth(90);
            nameLbl.setStyle("-fx-text-fill:" + textColor + "; -fx-font-size:12px;");

            StackPane track = new StackPane();
            track.setMaxHeight(8);
            track.setPrefHeight(8);
            HBox.setHgrow(track, Priority.ALWAYS);

            String bgColor = "#1E293B";
            Region bg = new Region();
            bg.setPrefHeight(8);
            bg.setMaxWidth(Double.MAX_VALUE);
            bg.setStyle("-fx-background-color:" + bgColor + "; -fx-background-radius:4;");

            Region fill = new Region();
            fill.setPrefHeight(8);
            fill.setStyle("-fx-background-color:" + color + "; -fx-background-radius:4;");

            track.setMaxWidth(Double.MAX_VALUE);
            bg.prefWidthProperty().bind(track.widthProperty());
            fill.prefWidthProperty().bind(track.widthProperty().multiply(pct / 100.0));
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            track.getChildren().addAll(bg, fill);

            Label valLbl = new Label(String.format("%.1f", avg));
            valLbl.setMinWidth(32);
            valLbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:12px; -fx-font-weight:700;");

            row.getChildren().addAll(nameLbl, track, valLbl);
            body.getChildren().add(row);
        });
        if (body.getChildren().isEmpty()) {
            Label empty = new Label("No data yet");
            String emptyColor = "#475569";
            empty.setStyle("-fx-text-fill:" + emptyColor + "; -fx-font-size:13px;");
            body.getChildren().add(empty);
        }
        card.getChildren().add(body);
        return card;
    }

    private VBox buildPriorityRingsCard() {
        VBox card = cardContainer("Score rings by priority");
        Map<String, Double> avgByPrio = allEvals.stream()
                .filter(e -> e.getPrioriteE() != null)
                .collect(Collectors.groupingBy(EvaluationMatiere::getPrioriteE, Collectors.averagingDouble(EvaluationMatiere::getScoreEval)));
        double high   = avgByPrio.getOrDefault("High",   avgByPrio.getOrDefault("Haute",   0.0));
        double medium = avgByPrio.getOrDefault("Medium", avgByPrio.getOrDefault("Moyenne", 0.0));
        double low    = avgByPrio.getOrDefault("Low",    avgByPrio.getOrDefault("Basse",   0.0));
        String html = buildRingsHtml(high, medium, low);
        WebView wv = new WebView();
        wv.setPrefHeight(160);
        wv.setMinHeight(160);
        wv.setMaxHeight(160);
        wv.getEngine().loadContent(html);
        card.getChildren().add(wv);
        return card;
    }

    private String buildRingsHtml(double high, double medium, double low) {
        String bgColor = "#0F172A";
        String textColor = "#94A3B8";

        return "<!DOCTYPE html><html><head><style>"
                + "body{margin:0;padding:0;background:" + bgColor + ";display:flex;justify-content:space-around;align-items:center;height:160px;font-family:sans-serif;}"
                + ".ring-wrap{display:flex;flex-direction:column;align-items:center;gap:8px;}"
                + ".ring-label{color:" + textColor + ";font-size:11px;font-weight:600;letter-spacing:.05em;}"
                + "</style></head><body>"
                + buildRingSvg("HIGH",   high,   "#E24B4A", 20)
                + buildRingSvg("MEDIUM", medium, "#EF9F27", 20)
                + buildRingSvg("LOW",    low,    "#1D9E75", 20)
                + "</body></html>";
    }

    private String buildRingSvg(String label, double avg, String color, double max) {
        double pct  = max > 0 ? avg / max : 0;
        double r    = 34;
        double circ = 2 * Math.PI * r;
        double dash = pct * circ;
        String valStr = String.format("%.1f", avg);
        String textColor = "#F8FAFC";
        String strokeColor = "#1E293B";

        return "<div class='ring-wrap'>"
                + "<svg width='90' height='90' viewBox='0 0 90 90'>"
                + "<circle cx='45' cy='45' r='" + r + "' fill='none' stroke='" + strokeColor + "' stroke-width='7'/>"
                + "<circle cx='45' cy='45' r='" + r + "' fill='none' stroke='" + color + "' stroke-width='7'"
                + " stroke-dasharray='" + dash + " " + circ + "'"
                + " stroke-dashoffset='" + (circ * 0.25) + "' stroke-linecap='round'/>"
                + "<text x='45' y='50' text-anchor='middle' font-size='14' font-weight='700' fill='" + textColor + "'>" + valStr + "</text>"
                + "</svg><span class='ring-label'>" + label + "</span>"
                + "</div>";
    }

    private VBox buildWebChartCard(String title, String html) {
        VBox card = cardContainer(title);
        WebView wv = new WebView();
        wv.setPrefHeight(240);
        wv.setMinHeight(240);
        wv.getEngine().loadContent(html);
        card.getChildren().add(wv);
        return card;
    }

    private String buildLineChartHtml() {
        DateTimeFormatter shortFmt = DateTimeFormatter.ofPattern("dd/MM");
        List<EvaluationMatiere> sorted = allEvals.stream()
                .filter(e -> e.getDateEvaluation() != null)
                .sorted(Comparator.comparing(EvaluationMatiere::getDateEvaluation))
                .collect(Collectors.toList());
        StringBuilder labels = new StringBuilder();
        StringBuilder data   = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) { labels.append(","); data.append(","); }
            labels.append("'").append(sorted.get(i).getDateEvaluation().format(shortFmt)).append("'");
            data.append(sorted.get(i).getScoreEval());
        }

        String bgColor = "#0F172A";
        String gridColor = "rgba(255,255,255,0.04)";
        String textColor = "#64748B";

        return "<!DOCTYPE html><html><head>"
                + "<script src='https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.js'></script>"
                + "<style>body{margin:0;background:" + bgColor + ";}</style>"
                + "</head><body><div style='position:relative;width:100%;height:220px;'><canvas id='lc'></canvas></div>"
                + "<script>new Chart(document.getElementById('lc'),{type:'line',"
                + "data:{labels:[" + labels + "],datasets:[{label:'Score',data:[" + data + "],"
                + "borderColor:'#A78BFA',backgroundColor:'rgba(167,139,250,0.12)',"
                + "pointBackgroundColor:'#A78BFA',tension:0.4,fill:true,borderWidth:2.5,pointRadius:5}]},"
                + "options:{responsive:true,maintainAspectRatio:false,plugins:{legend:{display:false}},"
                + "scales:{y:{min:0,max:20,ticks:{color:'" + textColor + "',font:{size:10}},grid:{color:'" + gridColor + "'}},"
                + "x:{ticks:{color:'" + textColor + "',font:{size:10},maxRotation:0},grid:{display:false}}}}});"
                + "</script></body></html>";
    }

    private String buildBarChartHtml() {
        Map<Integer, Double> avgBySubject = allEvals.stream().collect(
                Collectors.groupingBy(EvaluationMatiere::getMatiereId, Collectors.averagingDouble(EvaluationMatiere::getScoreEval)));
        List<Map.Entry<Integer, Double>> entries = avgBySubject.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed()).collect(Collectors.toList());
        StringBuilder labels = new StringBuilder();
        StringBuilder data   = new StringBuilder();
        StringBuilder colors = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) { labels.append(","); data.append(","); colors.append(","); }
            String name = getNomMatiere(entries.get(i).getKey());
            if (name.length() > 10) name = name.substring(0, 9) + "…";
            double avg   = entries.get(i).getValue();
            String color = avg >= 14 ? "'#10B981'" : avg >= 10 ? "'#F59E0B'" : "'#F43F5E'";
            labels.append("'").append(name).append("'");
            data.append(String.format("%.2f", avg));
            colors.append(color);
        }

        String bgColor = "#0F172A";
        String gridColor = "rgba(255,255,255,0.04)";
        String textColor = "#64748B";

        return "<!DOCTYPE html><html><head>"
                + "<script src='https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.js'></script>"
                + "<style>body{margin:0;background:" + bgColor + ";}</style>"
                + "</head><body><div style='position:relative;width:100%;height:220px;'><canvas id='bc'></canvas></div>"
                + "<script>new Chart(document.getElementById('bc'),{type:'bar',"
                + "data:{labels:[" + labels + "],datasets:[{data:[" + data + "],"
                + "backgroundColor:[" + colors + "],borderRadius:6,borderSkipped:false}]},"
                + "options:{responsive:true,maintainAspectRatio:false,plugins:{legend:{display:false}},"
                + "scales:{y:{min:0,max:20,ticks:{color:'" + textColor + "',font:{size:10}},grid:{color:'" + gridColor + "'}},"
                + "x:{ticks:{color:'" + textColor + "',font:{size:10}},grid:{display:false}}}}});"
                + "</script></body></html>";
    }

    private VBox buildHeatmapCard() {
        VBox card = cardContainer("Study activity — last 5 weeks");
        Map<LocalDate, Long> countByDay = allEvals.stream()
                .filter(e -> e.getDateEvaluation() != null)
                .collect(Collectors.groupingBy(EvaluationMatiere::getDateEvaluation, Collectors.counting()));
        long maxCount = countByDay.values().stream().mapToLong(l -> l).max().orElse(1);

        HBox legend = new HBox(6);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(0, 0, 8, 0));

        String labelColor = "#475569";
        Label lessLbl = new Label("less");
        lessLbl.setStyle("-fx-text-fill:" + labelColor + "; -fx-font-size:11px;");

        String[] legendColors = new String[]{"#1E293B","#1D4E89","#1E6FBF","#2196F3","#60B4FF"};

        legend.getChildren().add(lessLbl);
        for (String c : legendColors) {
            Region cell = new Region();
            cell.setPrefSize(14, 14);
            cell.setStyle("-fx-background-color:" + c + "; -fx-background-radius:3;");
            legend.getChildren().add(cell);
        }
        Label moreLbl = new Label("more");
        moreLbl.setStyle("-fx-text-fill:" + labelColor + "; -fx-font-size:11px;");
        legend.getChildren().add(moreLbl);

        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusWeeks(4).with(java.time.DayOfWeek.MONDAY);

        for (int week = 0; week < 5; week++) {
            for (int day = 0; day < 7; day++) {
                LocalDate d     = start.plusDays((long) week * 7 + day);
                long count      = countByDay.getOrDefault(d, 0L);
                double ratio    = maxCount > 0 ? (double) count / maxCount : 0;
                String cellColor = count == 0 ? "#1E293B" : ratio <= 0.25 ? "#1D4E89" : ratio <= 0.50 ? "#1E6FBF" : ratio <= 0.75 ? "#2196F3" : "#60B4FF";
                Region cell = new Region();
                cell.setPrefSize(18, 18);
                cell.setStyle("-fx-background-color:" + cellColor + "; -fx-background-radius:3; -fx-cursor:hand;");
                String tooltip = d.format(DateTimeFormatter.ofPattern("dd MMM")) + (count > 0 ? " — " + count + " eval(s)" : "");
                Tooltip tp = new Tooltip(tooltip);
                Tooltip.install(cell, tp);
                grid.add(cell, week, day);
            }
        }

        String[] dayNames = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        VBox dayLabels = new VBox(5);
        for (String dn : dayNames) {
            Label dl = new Label(dn);
            dl.setPrefHeight(18);
            dl.setStyle("-fx-text-fill:" + labelColor + "; -fx-font-size:10px;");
            dayLabels.getChildren().add(dl);
        }

        HBox heatmapRow = new HBox(10);
        heatmapRow.setAlignment(Pos.CENTER_LEFT);
        heatmapRow.getChildren().addAll(dayLabels, grid);
        card.getChildren().addAll(legend, heatmapRow);
        return card;
    }

    private VBox buildInsightsCard() {
        VBox card = cardContainer("Smart insights");
        FlowPane flow = new FlowPane(10, 10);
        double avg      = allEvals.stream().mapToDouble(EvaluationMatiere::getScoreEval).average().orElse(0);
        long   totalMin = allEvals.stream().mapToLong(EvaluationMatiere::getDureeEvaluation).sum();
        long   above14  = allEvals.stream().filter(e -> e.getScoreEval() >= 14).count();
        long   below10  = allEvals.stream().filter(e -> e.getScoreEval() < 10).count();

        if (avg >= 16)      addBadge(flow, "⭐  Excellent avg ≥ 16", "#10B981", "#0D2C1F");
        else if (avg >= 14) addBadge(flow, "✅  Good avg ≥ 14",       "#34D399", "#0D2C1F");
        else if (avg >= 10) addBadge(flow, "🟡  Average 10–14",       "#F59E0B", "#2C1E00");
        else                addBadge(flow, "🔴  Below avg — keep going!", "#F43F5E", "#2D0A0A");

        if (above14 > 0) addBadge(flow, above14 + " assessment" + (above14 > 1 ? "s" : "") + " ≥ 14/20", "#A78BFA", "#1E1B4B");
        if (below10 > 0) addBadge(flow, below10 + " to revise (< 10)", "#FB7185", "#2D0A0A");

        if (totalMin >= 60) addBadge(flow, String.format("⏱  %.1fh studied", totalMin / 60.0), "#38BDF8", "#0C2233");
        else if (totalMin > 0) addBadge(flow, "⏱  " + totalMin + " min studied", "#38BDF8", "#0C2233");

        int streak = computeStreak();
        if (streak > 0) addBadge(flow, "🔥  " + streak + "-day streak", "#FB923C", "#2C1200");

        String improved = findMostImproved();
        if (improved != null) addBadge(flow, "📈  Most improved: " + improved, "#FBBF24", "#2C1E00");

        Map<Integer, Double> avgBySubject = allEvals.stream().collect(
                Collectors.groupingBy(EvaluationMatiere::getMatiereId, Collectors.averagingDouble(EvaluationMatiere::getScoreEval)));
        avgBySubject.entrySet().stream().max(Map.Entry.comparingByValue()).ifPresent(e ->
                addBadge(flow, "🏆  Best: " + getNomMatiere(e.getKey()) + " (" + String.format("%.1f", e.getValue()) + ")", "#34D399", "#0D2C1F"));
        avgBySubject.entrySet().stream().min(Map.Entry.comparingByValue()).ifPresent(e -> {
            if (e.getValue() < 10)
                addBadge(flow, "⚠  Focus on: " + getNomMatiere(e.getKey()) + " (" + String.format("%.1f", e.getValue()) + ")", "#FB7185", "#2D0A0A");
        });
        card.getChildren().add(flow);
        return card;
    }

    private int computeStreak() {
        if (allEvals.isEmpty()) return 0;
        Set<LocalDate> evalDates = allEvals.stream().filter(e -> e.getDateEvaluation() != null)
                .map(EvaluationMatiere::getDateEvaluation).collect(Collectors.toSet());
        int streak = 0;
        LocalDate cursor = LocalDate.now();
        while (evalDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private String findMostImproved() {
        Map<Integer, List<EvaluationMatiere>> bySubject = allEvals.stream()
                .filter(e -> e.getDateEvaluation() != null)
                .collect(Collectors.groupingBy(EvaluationMatiere::getMatiereId));
        int    bestId   = -1;
        double bestDiff = 0;
        for (Map.Entry<Integer, List<EvaluationMatiere>> entry : bySubject.entrySet()) {
            List<EvaluationMatiere> sortedList = entry.getValue().stream()
                    .sorted(Comparator.comparing(EvaluationMatiere::getDateEvaluation))
                    .collect(Collectors.toList());
            if (sortedList.size() < 2) continue;
            double diff = sortedList.get(sortedList.size() - 1).getScoreEval() - sortedList.get(0).getScoreEval();
            if (diff > bestDiff) {
                bestDiff = diff;
                bestId = entry.getKey();
            }
        }
        return bestId >= 0 ? getNomMatiere(bestId) + " (+" + String.format("%.1f", bestDiff) + ")" : null;
    }

    private VBox cardContainer(String title) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        String bgColor = "#0F172A";
        String borderColor = "#1E293B";
        String titleColor = "#F8FAFC";
        card.setStyle("-fx-background-color:" + bgColor + "; -fx-border-color:" + borderColor + "; -fx-border-width:1; -fx-border-radius:16; -fx-background-radius:16;");
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill:" + titleColor + "; -fx-font-size:14px; -fx-font-weight:700;");
        card.getChildren().add(titleLbl);
        return card;
    }

    private void addBadge(FlowPane flow, String text, String textColor, String bgColor) {
        Label badge = new Label(text);
        badge.setStyle("-fx-text-fill:" + textColor + "; -fx-background-color:" + bgColor + "; "
                + "-fx-font-size:12px; -fx-padding:6 12; -fx-background-radius:20;");
        flow.getChildren().add(badge);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  AI QUIZ GENERATION
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void handleGenerateAIQuiz() {
        lblQuizSetupError.setText("");

        if (cmbQuizSection.getValue() == null) {
            lblQuizSetupError.setText("⚠ Please select a section.");
            return;
        }
        Matiere selected = cmbQuizMatiere.getValue();
        if (selected == null) {
            lblQuizSetupError.setText("⚠ Please select a subject.");
            return;
        }

        String matiere   = selected.getNomMatiere();
        String section   = cmbQuizSection.getValue();
        String difficulty = cmbQuizDifficulty.getValue() != null ? cmbQuizDifficulty.getValue() : "Medium";
        int    count     = cmbQuizCount.getValue() != null ? cmbQuizCount.getValue() : 10;
        String level     = detectLevel(getStudentGradeForSubject(selected.getId()));

        showQuizLoading(true);
        lblQuizLoadingStatus.setText("OpenRouter AI is generating your questions...");

        new Thread(() -> {
            try {
                AIQuizService service = new AIQuizService();
                List<AIQuizService.ParsedQuestion> parsed =
                        service.generateQuizQuestions(matiere, section, level, count);

                Platform.runLater(() -> {
                    showQuizLoading(false);

                    if (parsed.isEmpty()) {
                        lblQuizSetupError.setText("⚠ AI returned no questions. Check your OPENROUTER_API_KEY environment variable.");
                        showQuizSubView("setup");
                        return;
                    }

                    quizQuestions.clear();
                    for (AIQuizService.ParsedQuestion pq : parsed) {
                        QuizQuestion q = new QuizQuestion(
                                pq.question,
                                pq.options,
                                pq.correct,
                                pq.difficulty,
                                pq.category
                        );
                        quizQuestions.add(q);
                    }

                    quizCurrentIndex  = 0;
                    quizScore         = 0;
                    quizAnsweredCount = 0;
                    startQuizSession();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showQuizLoading(false);
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("401")) {
                        lblQuizSetupError.setText("⚠ Invalid API key. Set OPENROUTER_API_KEY environment variable.\nGet your key at: https://openrouter.ai/keys");
                    } else if (msg != null && msg.contains("429")) {
                        lblQuizSetupError.setText("⚠ Rate limit. Wait a moment and retry.");
                    } else {
                        lblQuizSetupError.setText("⚠ AI error: " + (msg != null ? msg : "Unknown error"));
                    }
                    showQuizSubView("setup");
                });
            }
        }).start();
    }

    private void showQuizLoading(boolean show) {
        if (show) {
            if (btnGenerateAIQuiz != null) {
                btnGenerateAIQuiz.setDisable(true);
                btnGenerateAIQuiz.setText("⏳ AI is generating...");
            }
            showQuizSubView("loading");
        } else {
            if (btnGenerateAIQuiz != null) {
                btnGenerateAIQuiz.setDisable(false);
                btnGenerateAIQuiz.setText("🤖  Generate AI Quiz");
            }
        }
    }

    private Matiere getSelectedMatiereFromQuiz() {
        return cmbQuizMatiere.getValue();
    }

    private String detectLevel(double note) {
        if (note < 10) return "easy";
        else if (note < 14) return "medium";
        else return "hard";
    }

    private double getStudentGradeForSubject(int matiereId) {
        return allEvals.stream()
                .filter(e -> e.getMatiereId() == matiereId)
                .mapToDouble(EvaluationMatiere::getScoreEval)
                .average()
                .orElse(10.0);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  EXPORT PDF
    // ══════════════════════════════════════════════════════════════════════════
    @FXML
    private void handleExportPdf() {
        if (allEvals.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Add at least one assessment before exporting.", ButtonType.OK).showAndWait();
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save PDF Report");
        chooser.setInitialFileName("StudyFlow_Report_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        Stage stage = (Stage) courseGrid.getScene().getWindow();
        java.io.File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try {
            PdfExporter.export(new ArrayList<>(allEvals), this::getNomMatiere, file);
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Export successful");
            ok.setHeaderText("PDF generated!");
            ok.setContentText("Saved to:\n" + file.getAbsolutePath());
            ok.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Export failed: " + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════════
    private void clearForm() {
        editTarget = null;
        selectedPriorite = null;
        cmbMatiere.setValue(null);
        fldScore.clear();
        fldNoteMax.clear();
        fldDuree.clear();
        dpDate.setValue(null);
        cmbPriorite.setValue(null);

        btnPrioHaute.setStyle("-fx-background-color:#1E293B;-fx-text-fill:#FB7185;-fx-border-color:#FB7185;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 20;-fx-font-size:13px;-fx-cursor:hand;");
        btnPrioMoyenne.setStyle("-fx-background-color:#1E293B;-fx-text-fill:#FBBF24;-fx-border-color:#FBBF24;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 20;-fx-font-size:13px;-fx-cursor:hand;");
        btnPrioBasse.setStyle("-fx-background-color:#1E293B;-fx-text-fill:#34D399;-fx-border-color:#34D399;-fx-border-width:1.5;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 20;-fx-font-size:13px;-fx-cursor:hand;");

        updatePriorityButtons();
        cmbSection.setValue(null);
        errSection.setText("");
        cmbMatiere.setItems(FXCollections.observableArrayList());
        cmbMatiere.setPromptText("Select a section first…");
        clearErrors();
    }

    private void clearErrors() {
        formError.setVisible(false);
        errMatiere.setText("");
        errScore.setText("");
        errNoteMax.setText("");
        errDuree.setText("");
        errPriorite.setText("");
        errDate.setText("");
        errSection.setText("");
        fldScore.setStyle(null);
        fldNoteMax.setStyle(null);
        fldDuree.setStyle(null);
        cmbMatiere.setStyle(null);
        dpDate.setStyle(null);
    }

    private void showGlobalError(String msg) {
        formError.setText("⚠ " + msg);
        formError.setVisible(true);
    }

    private String getNomMatiere(int id) {
        try {
            Matiere m = matService.findById(id);
            return m != null ? m.getNomMatiere() : "Matière #" + id;
        } catch (Exception e) {
            return "Matière #" + id;
        }
    }

    private HBox buildChip(String icon, String text, String hexColor) {
        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("courses-chip");
        FontIcon ic = new FontIcon(icon);
        ic.setIconSize(12);
        ic.setIconColor(Color.web(hexColor));
        Label lbl = new Label(text);
        lbl.getStyleClass().add("courses-chip-label");
        lbl.setTextFill(Color.web(hexColor));
        chip.getChildren().addAll(ic, lbl);
        return chip;
    }

    private void styleBtn(Button btn, String hexColor) {
        btn.setStyle("-fx-background-color:transparent; -fx-text-fill:" + hexColor
                + "; -fx-border-color:" + hexColor
                + "; -fx-border-radius:8; -fx-background-radius:8;"
                + " -fx-font-size:12px; -fx-padding:6 14; -fx-cursor:hand;");
    }

    private String getPriorityColor(String p) {
        if (p == null) return "#94A3B8";
        switch (p) {
            case "High": case "Haute": return "#FB7185";
            case "Medium": case "Moyenne": return "#FBBF24";
            case "Low": case "Basse": return "#34D399";
            default: return "#94A3B8";
        }
    }

    private String getGradientForColor(String color) {
        switch (color) {
            case "primary": return "linear-gradient(to right, #6D28D9, #8B5CF6)";
            case "success": return "linear-gradient(to right, #047857, #10B981)";
            case "warning": return "linear-gradient(to right, #D97706, #F59E0B)";
            case "accent": return "linear-gradient(to right, #EA580C, #F97316)";
            case "danger": return "linear-gradient(to right, #E11D48, #F43F5E)";
            default: return "linear-gradient(to right, #475569, #64748B)";
        }
    }

    private String getColorHex(String color) {
        switch (color) {
            case "primary": return "#8B5CF6";
            case "success": return "#10B981";
            case "warning": return "#F59E0B";
            case "accent": return "#F97316";
            case "danger": return "#F43F5E";
            default: return "#64748B";
        }
    }

    private void updatePriorityButtons() {
        resetPriorityButton(btnPrioHaute, "Haute", "High");
        resetPriorityButton(btnPrioMoyenne, "Moyenne", "Medium");
        resetPriorityButton(btnPrioBasse, "Basse", "Low");
    }

    private void resetPriorityButton(Button button, String frenchValue, String englishValue) {
        button.pseudoClassStateChanged(SELECTED,
                frenchValue.equalsIgnoreCase(selectedPriorite) || englishValue.equalsIgnoreCase(selectedPriorite));
    }
}