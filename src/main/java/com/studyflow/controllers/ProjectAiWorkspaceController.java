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
    private AiProjectInsightsService.ProjectHealthInsights currentHealthInsights;
    private AiProjectInsightsService.ProjectReportInsights currentReportInsights;

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
        setLoading(true, "Opening AI workspace", "Preparing the AI workspace for \"" + project.getTitle() + "\".");
        refreshWorkspaceAsync();
    }

    @FXML
    private void handleAnalyzeDependencies() {
        if (!isReady()) {
            showFeedback("Project data is unavailable.", true);
            return;
        }
        analyzeDependenciesButton.setDisable(true);
        setLoading(true, "Analyzing dependencies", "Scanning assignments to build the AI dependency map.");
        showFeedback("Analyzing project dependencies...", false);

        Task<List<AssignmentDependency>> task = new Task<>() {
            @Override
            protected List<AssignmentDependency> call() {
                return aiProjectInsightsService.analyzeDependencies(project, assignments);
            }
        };

        task.setOnSucceeded(event -> {
            setLoading(false, null, null);
            int saved = assignmentService.replaceProjectDependencies(project.getId(), getCurrentUser().getId(), task.getValue());
            analyzeDependenciesButton.setDisable(false);
            showFeedback(saved + " dependency link(s) saved.", false);
            setLoading(true, "Refreshing AI workspace", "Updating dependencies, health insights, and report preview.");
            refreshWorkspaceAsync();
        });

        task.setOnFailed(event -> {
            setLoading(false, null, null);
            analyzeDependenciesButton.setDisable(false);
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
        setLoading(true, "Generating AI report", "Preparing project intelligence and exporting the PDF report.");
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
            setLoading(false, null, null);
            generateReportButton.setDisable(false);
            File file = task.getValue();
            exportPathLabel.setText(file.getAbsolutePath());
            showFeedback("AI PDF report exported to " + file.getAbsolutePath(), false);
        });

        task.setOnFailed(event -> {
            setLoading(false, null, null);
            generateReportButton.setDisable(false);
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

    private void refreshWorkspaceAsync() {
        Task<WorkspaceSnapshot> task = new Task<>() {
            @Override
            protected WorkspaceSnapshot call() {
                List<Assignment> loadedAssignments = assignmentService.getByProjectId(project.getId(), getCurrentUser().getId());
                List<AssignmentDependency> loadedDependencies = assignmentService.getDependenciesForProject(project.getId(), getCurrentUser().getId());
                AiProjectInsightsService.ProjectHealthInsights healthInsights =
                        aiProjectInsightsService.buildProjectHealthInsights(project, loadedAssignments);
                AiProjectInsightsService.ProjectReportInsights reportInsights =
                        aiProjectInsightsService.buildProjectReportInsights(project, loadedAssignments, loadedDependencies);
                return new WorkspaceSnapshot(loadedAssignments, loadedDependencies, healthInsights, reportInsights);
            }
        };

        task.setOnSucceeded(event -> {
            WorkspaceSnapshot snapshot = task.getValue();
            assignments.clear();
            assignments.addAll(snapshot.assignments());
            dependencies.clear();
            dependencies.addAll(snapshot.dependencies());
            currentHealthInsights = snapshot.healthInsights();
            currentReportInsights = snapshot.reportInsights();

            totalAssignmentsLabel.setText(String.valueOf(assignments.size()));
            dependencyCountLabel.setText(String.valueOf(dependencies.size()));
            renderHealthDashboard();
            renderDependencies();
            renderReportPreview();
            setLoading(false, null, null);
        });

        task.setOnFailed(event -> {
            setLoading(false, null, null);
            analyzeDependenciesButton.setDisable(false);
            generateReportButton.setDisable(false);
            Throwable error = task.getException();
            showFeedback("AI workspace loading failed: " + (error == null ? "unknown error" : error.getMessage()), true);
        });

        Thread thread = new Thread(task, "project-ai-workspace-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void renderHealthDashboard() {
        AiProjectInsightsService.ProjectHealthInsights health = currentHealthInsights;
        if (health == null) {
            return;
        }

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

    private void renderReportPreview() {
        AiProjectInsightsService.ProjectReportInsights insights = currentReportInsights;
        if (insights == null) {
            return;
        }
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

    private void setLoading(boolean loading, String title, String subtitle) {
        if (loadingOverlay != null) {
            if (loading) {
                loadingOverlay.toFront();
            }
            loadingOverlay.setVisible(loading);
            loadingOverlay.setManaged(loading);
        }
        if (loadingLabel != null && title != null && !title.isBlank()) {
            loadingLabel.setText(title);
        }
        if (loadingSubtitleLabel != null && subtitle != null && !subtitle.isBlank()) {
            loadingSubtitleLabel.setText(subtitle);
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
            AiProjectInsightsService.ProjectHealthInsights healthInsights,
            AiProjectInsightsService.ProjectReportInsights reportInsights
    ) {
    }
}
