package com.studyflow.controllers;

import com.studyflow.models.Assignment;
import com.studyflow.models.AssignmentDependency;
import com.studyflow.models.Project;
import com.studyflow.models.User;
import com.studyflow.services.AiProjectInsightsService;
import com.studyflow.services.AssignmentService;
import com.studyflow.services.ProjectService;
import com.studyflow.utils.CrudViewContext;
import com.studyflow.utils.PdfExportUtil;
import com.studyflow.utils.UserSession;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ProjectAiWorkspaceController implements Initializable {
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @FXML private StackPane workspaceRoot;
    @FXML private VBox loadingOverlay;
    @FXML private Label loadingLabel;
    @FXML private Label loadingSubtitleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label feedbackLabel;
    @FXML private Label projectTitleLabel;
    @FXML private Label projectMetaLabel;
    @FXML private Label totalAssignmentsLabel;
    @FXML private Label dependencyCountLabel;
    @FXML private VBox healthScoreCard;
    @FXML private Label healthScoreLabel;
    @FXML private Label healthStatusBadge;
    @FXML private ProgressBar progressScoreBar;
    @FXML private Label progressScoreLabel;
    @FXML private ProgressBar activityScoreBar;
    @FXML private Label activityScoreLabel;
    @FXML private ProgressBar deadlinesScoreBar;
    @FXML private Label deadlinesScoreLabel;
    @FXML private ProgressBar balanceScoreBar;
    @FXML private Label balanceScoreLabel;
    @FXML private Label healthSummaryLabel;
    @FXML private VBox dependencyList;
    @FXML private Label executiveSummaryLabel;
    @FXML private VBox recommendationsList;
    @FXML private Label exportPathLabel;
    @FXML private Button analyzeDependenciesButton;
    @FXML private Button generateReportButton;

    private final ProjectService projectService = new ProjectService();
    private final AssignmentService assignmentService = new AssignmentService();
    private final AiProjectInsightsService aiProjectInsightsService = new AiProjectInsightsService();

    private Project project;
    private final List<Assignment> assignments = new ArrayList<>();
    private final List<AssignmentDependency> dependencies = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        project = CrudViewContext.consumeProject();
        if (project == null) {
            showFeedback("No project selected for the AI workspace.", true);
            analyzeDependenciesButton.setDisable(true);
            generateReportButton.setDisable(true);
            return;
        }

        projectTitleLabel.setText(project.getTitle());
        subtitleLabel.setText("Project intelligence for " + project.getTitle() + ".");
        projectMetaLabel.setText(project.getStatus() + " | " + (project.isOwnedByCurrentUser() ? "Owned by you" : "Shared by " + project.getOwnerName()));
        exportPathLabel.setText(PdfExportUtil.defaultExportFile(defaultFilename()).getAbsolutePath());
        setLoadingState(true, "Analyzing project health...");
        refreshWorkspaceAsync("Analyzing project health...");
    }

    @FXML
    private void handleAnalyzeDependencies() {
        if (!isReady()) {
            showFeedback("Project data is unavailable.", true);
            return;
        }
        analyzeDependenciesButton.setDisable(true);
        setLoadingState(true, "Analyzing project dependencies...");
        showFeedback("Analyzing project dependencies...", false);

        Task<List<AssignmentDependency>> task = new Task<>() {
            @Override
            protected List<AssignmentDependency> call() {
                return aiProjectInsightsService.analyzeDependencies(project, assignments);
            }
        };

        task.setOnSucceeded(event -> {
            int saved = assignmentService.replaceProjectDependencies(project.getId(), getCurrentUser().getId(), task.getValue());
            analyzeDependenciesButton.setDisable(false);
            refreshWorkspaceAsync("Refreshing AI workspace...", () -> showFeedback(saved + " dependency link(s) saved.", false));
        });

        task.setOnFailed(event -> {
            analyzeDependenciesButton.setDisable(false);
            setLoadingState(false, null);
            Throwable error = task.getException();
            showFeedback("Dependency analysis failed: " + (error == null ? "unknown error" : error.getMessage()), true);
        });

        Thread thread = new Thread(task, "project-ai-dependencies");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleGenerateReport() {
        if (!isReady()) {
            showFeedback("Project data is unavailable.", true);
            return;
        }
        generateReportButton.setDisable(true);
        setLoadingState(true, "Generating AI PDF report...");
        showFeedback("Generating AI PDF report...", false);

        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                AiProjectInsightsService.ProjectReportInsights insights =
                        aiProjectInsightsService.buildProjectReportInsights(project, assignments, dependencies);
                File file = PdfExportUtil.defaultExportFile(defaultFilename());
                PdfExportUtil.exportProjectIntelligenceReport(
                        file,
                        project,
                        assignments,
                        dependencies,
                        insights.executiveSummary(),
                        insights.recommendations()
                );
                return file;
            }
        };

        task.setOnSucceeded(event -> {
            generateReportButton.setDisable(false);
            File file = task.getValue();
            exportPathLabel.setText(file.getAbsolutePath());
            setLoadingState(false, null);
            showFeedback("AI PDF report exported to " + file.getAbsolutePath(), false);
        });

        task.setOnFailed(event -> {
            generateReportButton.setDisable(false);
            setLoadingState(false, null);
            Throwable error = task.getException();
            showFeedback("Report generation failed: " + (error == null ? "unknown error" : error.getMessage()), true);
        });

        Thread thread = new Thread(task, "project-ai-report");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleBack() {
        CrudViewContext.setProjectContext(project);
        CrudViewContext.rememberProjectSelection(project == null ? null : project.getId());
        MainController.loadContentInMainArea("views/Projects.fxml");
    }

    private void refreshWorkspaceAsync(String loadingMessage) {
        refreshWorkspaceAsync(loadingMessage, null);
    }

    private void refreshWorkspaceAsync(String loadingMessage, Runnable onSuccess) {
        if (!isReady()) {
            setLoadingState(false, null);
            showFeedback("Project data is unavailable.", true);
            return;
        }

        setLoadingState(true, loadingMessage);
        Task<WorkspaceSnapshot> task = new Task<>() {
            @Override
            protected WorkspaceSnapshot call() {
                List<Assignment> nextAssignments = assignmentService.getByProjectId(project.getId(), getCurrentUser().getId());
                List<AssignmentDependency> nextDependencies = assignmentService.getDependenciesForProject(project.getId(), getCurrentUser().getId());
                AiProjectInsightsService.ProjectHealthInsights health =
                        aiProjectInsightsService.buildProjectHealthInsights(project, nextAssignments);
                AiProjectInsightsService.ProjectReportInsights insights =
                        aiProjectInsightsService.buildProjectReportInsights(project, nextAssignments, nextDependencies);
                return new WorkspaceSnapshot(nextAssignments, nextDependencies, health, insights);
            }
        };

        task.setOnSucceeded(event -> {
            applyWorkspaceSnapshot(task.getValue());
            setLoadingState(false, null);
            if (onSuccess != null) {
                onSuccess.run();
            }
        });

        task.setOnFailed(event -> {
            setLoadingState(false, null);
            Throwable error = task.getException();
            showFeedback("Unable to load project AI insights: " + (error == null ? "unknown error" : error.getMessage()), true);
        });

        Thread thread = new Thread(task, "project-ai-workspace-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void applyWorkspaceSnapshot(WorkspaceSnapshot snapshot) {
        assignments.clear();
        assignments.addAll(snapshot.assignments());
        dependencies.clear();
        dependencies.addAll(snapshot.dependencies());

        totalAssignmentsLabel.setText(String.valueOf(assignments.size()));
        dependencyCountLabel.setText(String.valueOf(dependencies.size()));
        renderHealthDashboard(snapshot.health());
        renderDependencies();
        renderReportPreview(snapshot.insights());
    }

    private void renderHealthDashboard(AiProjectInsightsService.ProjectHealthInsights health) {
        healthScoreLabel.setText(String.format("%.1f", health.overallScore()));
        healthSummaryLabel.setText(health.issuesSummary());
        healthStatusBadge.getStyleClass().setAll("badge", health.statusStyleClass());
        healthStatusBadge.setText(health.statusLabel());

        updateScoreBar(progressScoreBar, progressScoreLabel, health.progressScore(), health.statusStyleClass());
        updateScoreBar(activityScoreBar, activityScoreLabel, health.activityScore(), health.statusStyleClass());
        updateScoreBar(deadlinesScoreBar, deadlinesScoreLabel, health.deadlinesScore(), health.statusStyleClass());
        updateScoreBar(balanceScoreBar, balanceScoreLabel, health.balanceScore(), health.statusStyleClass());

        if (healthScoreCard != null) {
            healthScoreCard.getStyleClass().removeAll("mini-stat-green", "mini-stat-amber", "mini-stat-rose");
            if ("success".equals(health.statusStyleClass())) {
                healthScoreCard.getStyleClass().add("mini-stat-green");
            } else if ("warning".equals(health.statusStyleClass())) {
                healthScoreCard.getStyleClass().add("mini-stat-amber");
            } else {
                healthScoreCard.getStyleClass().add("mini-stat-rose");
            }
        }
    }

    private void renderDependencies() {
        dependencyList.getChildren().clear();
        if (dependencies.isEmpty()) {
            dependencyList.getChildren().add(createEmptyState("No dependency links yet. Run AI analysis to build the sequence map."));
            return;
        }

        for (AssignmentDependency dependency : dependencies) {
            dependencyList.getChildren().add(createDependencyRow(dependency));
        }
    }

    private void renderReportPreview(AiProjectInsightsService.ProjectReportInsights insights) {
        executiveSummaryLabel.setText(insights.executiveSummary());
        recommendationsList.getChildren().clear();
        for (String recommendation : insights.recommendations()) {
            Label label = new Label("- " + recommendation);
            label.setWrapText(true);
            label.getStyleClass().add("item-desc");
            recommendationsList.getChildren().add(label);
        }
    }

    private HBox createDependencyRow(AssignmentDependency dependency) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(14));
        row.getStyleClass().addAll("detail-row", "project-assignment-row");

        FontIcon icon = new FontIcon("fth-git-merge");
        icon.getStyleClass().addAll("detail-row-icon", "project-assignment-icon");

        VBox body = new VBox(5);
        HBox.setHgrow(body, Priority.ALWAYS);

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(dependency.getAssignmentTitle());
        title.getStyleClass().addAll("item-title", "project-assignment-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label badge = new Label("Depends On");
        badge.getStyleClass().addAll("badge", "warning");
        top.getChildren().addAll(title, spacer, badge);

        Label meta = new Label("Requires " + dependency.getDependsOnTitle());
        meta.getStyleClass().addAll("item-meta", "project-assignment-meta");

        Label rationale = new Label(dependency.getRationale() == null || dependency.getRationale().isBlank()
                ? "AI detected this as a logical prerequisite."
                : dependency.getRationale());
        rationale.setWrapText(true);
        rationale.getStyleClass().addAll("item-desc", "project-card-description");

        body.getChildren().addAll(top, meta, rationale);
        row.getChildren().addAll(icon, body);
        return row;
    }

    private VBox createEmptyState(String message) {
        VBox box = new VBox(10);
        box.getStyleClass().addAll("card", "empty-state-card");
        box.setPadding(new Insets(28));
        box.setAlignment(Pos.CENTER);

        FontIcon icon = new FontIcon("fth-git-branch");
        icon.getStyleClass().add("empty-state-icon");

        Label title = new Label("Nothing mapped yet");
        title.getStyleClass().add("item-title");

        Label text = new Label(message);
        text.getStyleClass().add("text-muted");
        text.setWrapText(true);

        box.getChildren().addAll(icon, title, text);
        return box;
    }

    private void showFeedback(String message, boolean error) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("inline-alert-success", "inline-alert-error");
        feedbackLabel.getStyleClass().add(error ? "inline-alert-error" : "inline-alert-success");
        feedbackLabel.setVisible(true);
        feedbackLabel.setManaged(true);
    }

    private void setLoadingState(boolean loading, String message) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(loading);
            loadingOverlay.setManaged(loading);
        }
        if (loadingLabel != null && message != null && !message.isBlank()) {
            loadingLabel.setText(message);
        }
        if (loadingSubtitleLabel != null && message != null && !message.isBlank()) {
            loadingSubtitleLabel.setText("This usually takes a few seconds while AI analyzes the project.");
        }
        if (analyzeDependenciesButton != null && loading) {
            analyzeDependenciesButton.setDisable(true);
        }
        if (generateReportButton != null && loading) {
            generateReportButton.setDisable(true);
        }
        if (!loading) {
            if (analyzeDependenciesButton != null) {
                analyzeDependenciesButton.setDisable(false);
            }
            if (generateReportButton != null) {
                generateReportButton.setDisable(false);
            }
        }
    }

    private void updateScoreBar(ProgressBar bar, Label label, double score, String statusStyleClass) {
        if (bar != null) {
            bar.setProgress(Math.max(0, Math.min(1, score / 10.0)));
            bar.getStyleClass().removeAll("success", "warning", "danger");
            bar.getStyleClass().add(score >= 7.5 ? "success" : score >= 5.0 ? "warning" : "danger");
        }
        if (label != null) {
            label.setText(String.format("%.1f", score));
        }
    }

    private boolean isReady() {
        return projectService.isDatabaseAvailable() && assignmentService.isDatabaseAvailable() && getCurrentUser() != null;
    }

    private User getCurrentUser() {
        return UserSession.getInstance().getCurrentUser();
    }

    private String defaultFilename() {
        return "project-ai-report-" + FILE_DATE_FORMATTER.format(LocalDateTime.now()) + ".pdf";
    }

    private record WorkspaceSnapshot(
            List<Assignment> assignments,
            List<AssignmentDependency> dependencies,
            AiProjectInsightsService.ProjectHealthInsights health,
            AiProjectInsightsService.ProjectReportInsights insights
    ) {
    }
}
