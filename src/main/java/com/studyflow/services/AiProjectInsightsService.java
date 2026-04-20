package com.studyflow.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.models.Assignment;
import com.studyflow.models.AssignmentDependency;
import com.studyflow.models.Project;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AiProjectInsightsService {
    public record AssignmentPlanningEstimate(Integer minDays, Integer maxDays, String complexityLevel, LocalDate suggestedDueDate) {
    }

    public record ProjectReportInsights(String executiveSummary, List<String> recommendations) {
    }

    public record ProjectHealthInsights(
            double overallScore,
            double progressScore,
            double activityScore,
            double deadlinesScore,
            double balanceScore,
            String statusLabel,
            String statusStyleClass,
            String issuesSummary
    ) {
    }

    private final ClaudeApiService aiService;
    private final ObjectMapper mapper;

    public AiProjectInsightsService() {
        this.aiService = new ClaudeApiService();
        this.mapper = new ObjectMapper();
    }

    public boolean isConfigured() {
        return aiService.isConfigured();
    }

    public AssignmentPlanningEstimate estimateAssignmentPlan(Assignment assignment) {
        if (assignment == null) {
            return new AssignmentPlanningEstimate(1, 3, "Low", LocalDate.now().plusDays(2));
        }

        try {
            if (!isConfigured()) {
                return heuristicEstimate(assignment);
            }

            String prompt = String.format("""
                    Analyze this assignment and respond only as JSON.

                    Today: %s
                    Title: %s
                    Description: %s

                    Return exactly this JSON object:
                    {
                      "minDays": 1,
                      "maxDays": 3,
                      "complexityLevel": "Medium",
                      "suggestedDueDate": "2026-04-21"
                    }

                    Rules:
                    - complexityLevel must be Low, Medium, or High
                    - suggestedDueDate must be a valid ISO date
                    - minDays and maxDays must be positive integers
                    - maxDays must be >= minDays
                    """,
                    LocalDate.now(),
                    safe(assignment.getTitle()),
                    safe(assignment.getDescription())
            );

            String raw = cleanJson(aiService.ask(prompt, null));
            Map<String, Object> parsed = mapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
            Integer minDays = coercePositiveInt(parsed.get("minDays"), 1);
            Integer maxDays = coercePositiveInt(parsed.get("maxDays"), Math.max(minDays, 2));
            if (maxDays < minDays) {
                maxDays = minDays;
            }
            String complexity = normalizeComplexity(String.valueOf(parsed.get("complexityLevel")));
            LocalDate suggestedDueDate = parseDate(String.valueOf(parsed.get("suggestedDueDate")));
            if (suggestedDueDate == null) {
                suggestedDueDate = LocalDate.now().plusDays(maxDays);
            }
            return new AssignmentPlanningEstimate(minDays, maxDays, complexity, suggestedDueDate);
        } catch (Exception ignored) {
            return heuristicEstimate(assignment);
        }
    }

    public List<AssignmentDependency> analyzeDependencies(Project project, List<Assignment> assignments) {
        if (project == null || assignments == null || assignments.size() < 2) {
            return List.of();
        }

        try {
            if (!isConfigured()) {
                return heuristicDependencies(project, assignments);
            }

            String prompt = String.format("""
                    Analyze task dependencies for this project and return only a JSON array.

                    Project: %s
                    Description: %s
                    Assignments:
                    %s

                    Return a JSON array like:
                    [
                      {
                        "assignmentTitle": "Testing",
                        "dependsOnTitle": "Development",
                        "rationale": "Testing should follow implementation."
                      }
                    ]

                    Rules:
                    - only include logical prerequisites
                    - do not create circular dependencies
                    - keep rationale under 120 characters
                    - do not include duplicates
                    """,
                    safe(project.getTitle()),
                    safe(project.getDescription()),
                    assignments.stream()
                            .map(assignment -> "- " + safe(assignment.getTitle()) + ": " + safe(assignment.getDescription()))
                            .collect(Collectors.joining("\n"))
            );

            String raw = cleanJson(aiService.ask(prompt, null));
            List<Map<String, String>> parsed = mapper.readValue(raw, new TypeReference<List<Map<String, String>>>() {});
            Map<String, Assignment> byTitle = assignments.stream()
                    .collect(Collectors.toMap(a -> safe(a.getTitle()).toLowerCase(Locale.ROOT), a -> a, (left, right) -> left, LinkedHashMap::new));

            List<AssignmentDependency> dependencies = new ArrayList<>();
            for (Map<String, String> item : parsed) {
                Assignment child = byTitle.get(safe(item.get("assignmentTitle")).toLowerCase(Locale.ROOT));
                Assignment parent = byTitle.get(safe(item.get("dependsOnTitle")).toLowerCase(Locale.ROOT));
                if (child == null || parent == null || child.getId() <= 0 || parent.getId() <= 0 || child.getId() == parent.getId()) {
                    continue;
                }
                AssignmentDependency dependency = new AssignmentDependency();
                dependency.setProjectId(project.getId());
                dependency.setAssignmentId(child.getId());
                dependency.setDependsOnAssignmentId(parent.getId());
                dependency.setAssignmentTitle(child.getTitle());
                dependency.setDependsOnTitle(parent.getTitle());
                dependency.setRationale(trimRationale(item.get("rationale")));
                dependency.setSource("ai");
                if (isDuplicate(dependencies, dependency)) {
                    continue;
                }
                dependencies.add(dependency);
            }

            return dependencies.isEmpty() ? heuristicDependencies(project, assignments) : dependencies;
        } catch (Exception ignored) {
            return heuristicDependencies(project, assignments);
        }
    }

    public ProjectReportInsights buildProjectReportInsights(Project project, List<Assignment> assignments, List<AssignmentDependency> dependencies) {
        if (project == null) {
            return new ProjectReportInsights("No project context was available.", List.of("Review the selected project before exporting."));
        }

        try {
            if (!isConfigured()) {
                return heuristicReport(project, assignments, dependencies);
            }

            String prompt = String.format("""
                    Create an executive summary and recommendations for a project health report.
                    Respond only as JSON.

                    Project: %s
                    Description: %s
                    Status: %s
                    Assignments count: %d
                    Status breakdown: %s
                    Dependency count: %d

                    JSON format:
                    {
                      "executiveSummary": "Short professional summary.",
                      "recommendations": [
                        "Recommendation 1",
                        "Recommendation 2",
                        "Recommendation 3"
                      ]
                    }

                    Rules:
                    - executiveSummary must be 2 to 4 concise sentences
                    - recommendations must contain exactly 3 items
                    - recommendations must be practical
                    """,
                    safe(project.getTitle()),
                    safe(project.getDescription()),
                    safe(project.getStatus()),
                    assignments == null ? 0 : assignments.size(),
                    buildStatusBreakdown(assignments),
                    dependencies == null ? 0 : dependencies.size()
            );

            String raw = cleanJson(aiService.ask(prompt, null));
            Map<String, Object> parsed = mapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
            String executiveSummary = safe(String.valueOf(parsed.get("executiveSummary")));
            List<String> recommendations = mapper.convertValue(parsed.get("recommendations"), new TypeReference<List<String>>() {});
            recommendations = recommendations == null ? List.of() : recommendations.stream()
                    .map(this::safe)
                    .filter(value -> !value.isBlank())
                    .limit(3)
                    .collect(Collectors.toList());
            if (executiveSummary.isBlank() || recommendations.isEmpty()) {
                return heuristicReport(project, assignments, dependencies);
            }
            while (recommendations.size() < 3) {
                recommendations = new ArrayList<>(recommendations);
                recommendations.add(defaultRecommendation(recommendations.size()));
            }
            return new ProjectReportInsights(executiveSummary, recommendations);
        } catch (Exception ignored) {
            return heuristicReport(project, assignments, dependencies);
        }
    }

    public ProjectHealthInsights buildProjectHealthInsights(Project project, List<Assignment> assignments) {
        List<Assignment> safeAssignments = assignments == null ? List.of() : assignments;

        double progressScore = computeProgressScore(safeAssignments);
        double activityScore = computeActivityScore(project, safeAssignments);
        double deadlinesScore = computeDeadlinesScore(safeAssignments);
        double balanceScore = computeBalanceScore(safeAssignments);
        double overallScore = roundOneDecimal((progressScore + activityScore + deadlinesScore + balanceScore) / 4.0);
        String statusLabel = overallScore >= 7.5 ? "Good" : overallScore >= 5.0 ? "Warning" : "Critical";
        String statusStyleClass = overallScore >= 7.5 ? "success" : overallScore >= 5.0 ? "warning" : "danger";

        String issuesSummary = buildHealthSummaryWithAiFallback(project, safeAssignments, progressScore, activityScore, deadlinesScore, balanceScore, overallScore);
        return new ProjectHealthInsights(
                overallScore,
                roundOneDecimal(progressScore),
                roundOneDecimal(activityScore),
                roundOneDecimal(deadlinesScore),
                roundOneDecimal(balanceScore),
                statusLabel,
                statusStyleClass,
                issuesSummary
        );
    }

    private AssignmentPlanningEstimate heuristicEstimate(Assignment assignment) {
        String text = (safe(assignment.getTitle()) + " " + safe(assignment.getDescription())).toLowerCase(Locale.ROOT);
        int score = 1;
        for (String keyword : List.of("integration", "deploy", "architecture", "security", "testing", "analysis", "documentation", "review")) {
            if (text.contains(keyword)) {
                score++;
            }
        }
        if (text.length() > 120) {
            score++;
        }
        int minDays = Math.max(1, Math.min(5, score));
        int maxDays = Math.max(minDays + 1, Math.min(10, minDays + 2));
        String complexity = score >= 4 ? "High" : score >= 3 ? "Medium" : "Low";
        return new AssignmentPlanningEstimate(minDays, maxDays, complexity, LocalDate.now().plusDays(maxDays));
    }

    private List<AssignmentDependency> heuristicDependencies(Project project, List<Assignment> assignments) {
        List<Assignment> sorted = assignments.stream()
                .sorted(Comparator.comparing(a -> safe(a.getTitle()).toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        List<AssignmentDependency> dependencies = new ArrayList<>();

        Assignment implementation = findByKeywords(sorted, "develop", "implementation", "build", "code", "create");
        Assignment testing = findByKeywords(sorted, "test", "qa", "validation");
        Assignment deployment = findByKeywords(sorted, "deploy", "release", "launch");
        Assignment documentation = findByKeywords(sorted, "document", "guide");
        Assignment design = findByKeywords(sorted, "design", "spec", "plan", "analysis");

        addDependencyIfPresent(dependencies, project, implementation, design, "Implementation should follow design decisions.");
        addDependencyIfPresent(dependencies, project, testing, implementation, "Testing should follow implementation.");
        addDependencyIfPresent(dependencies, project, deployment, testing == null ? implementation : testing, "Deployment should follow readiness checks.");
        addDependencyIfPresent(dependencies, project, documentation, implementation, "Documentation depends on the implemented solution.");
        return dependencies;
    }

    private ProjectReportInsights heuristicReport(Project project, List<Assignment> assignments, List<AssignmentDependency> dependencies) {
        int total = assignments == null ? 0 : assignments.size();
        long completed = assignments == null ? 0 : assignments.stream().filter(Assignment::isCompleted).count();
        long inProgress = assignments == null ? 0 : assignments.stream().filter(Assignment::isInProgress).count();
        long todo = assignments == null ? 0 : assignments.stream().filter(Assignment::isTodo).count();
        int dependencyCount = dependencies == null ? 0 : dependencies.size();

        String summary = String.format(
                "%s currently tracks %d assignment(s), with %d completed, %d in progress, and %d still in the backlog. " +
                        "The project status is %s, and %d dependency link(s) have been identified to clarify sequencing. " +
                        "Focus should stay on moving active work through review while protecting the critical path.",
                safe(project.getTitle()),
                total,
                completed,
                inProgress,
                todo,
                safe(project.getStatus()).isBlank() ? "unspecified" : safe(project.getStatus()),
                dependencyCount
        );

        List<String> recommendations = new ArrayList<>();
        recommendations.add(todo > inProgress
                ? "Convert the highest-priority backlog items into active work only when the current in-progress load is stable."
                : "Keep the current in-progress set small and push items to review faster to avoid bottlenecks.");
        recommendations.add(dependencyCount == 0
                ? "Review assignment ordering manually and define explicit prerequisites for critical work streams."
                : "Use the detected dependencies to sequence work and avoid starting downstream tasks too early.");
        recommendations.add(completed == 0
                ? "Identify one short assignment that can be finished quickly to create momentum and validate delivery flow."
                : "Use completed work as a reference point to estimate remaining assignments more accurately.");
        return new ProjectReportInsights(summary, recommendations);
    }

    private String buildHealthSummaryWithAiFallback(
            Project project,
            List<Assignment> assignments,
            double progressScore,
            double activityScore,
            double deadlinesScore,
            double balanceScore,
            double overallScore
    ) {
        try {
            if (!isConfigured()) {
                return heuristicHealthSummary(project, assignments, progressScore, activityScore, deadlinesScore, balanceScore, overallScore);
            }

            String prompt = String.format("""
                    Write a short project health summary based on these metrics.
                    Respond only as JSON.

                    Project: %s
                    Status: %s
                    Overall score: %.1f/10
                    Progress score: %.1f/10
                    Activity score: %.1f/10
                    Deadlines score: %.1f/10
                    Balance score: %.1f/10
                    Assignment count: %d

                    JSON format:
                    {
                      "issuesSummary": "2 to 4 sentences highlighting the main issues and strengths."
                    }
                    """,
                    safe(project == null ? null : project.getTitle()),
                    safe(project == null ? null : project.getStatus()),
                    overallScore,
                    progressScore,
                    activityScore,
                    deadlinesScore,
                    balanceScore,
                    assignments.size()
            );

            String raw = cleanJson(aiService.ask(prompt, null));
            Map<String, Object> parsed = mapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
            String summary = safe(String.valueOf(parsed.get("issuesSummary")));
            return summary.isBlank()
                    ? heuristicHealthSummary(project, assignments, progressScore, activityScore, deadlinesScore, balanceScore, overallScore)
                    : summary;
        } catch (Exception ignored) {
            return heuristicHealthSummary(project, assignments, progressScore, activityScore, deadlinesScore, balanceScore, overallScore);
        }
    }

    private void addDependencyIfPresent(List<AssignmentDependency> dependencies, Project project, Assignment child, Assignment parent, String rationale) {
        if (child == null || parent == null || child.getId() == parent.getId()) {
            return;
        }
        AssignmentDependency dependency = new AssignmentDependency();
        dependency.setProjectId(project.getId());
        dependency.setAssignmentId(child.getId());
        dependency.setDependsOnAssignmentId(parent.getId());
        dependency.setAssignmentTitle(child.getTitle());
        dependency.setDependsOnTitle(parent.getTitle());
        dependency.setRationale(rationale);
        dependency.setSource("heuristic");
        if (!isDuplicate(dependencies, dependency)) {
            dependencies.add(dependency);
        }
    }

    private Assignment findByKeywords(List<Assignment> assignments, String... keywords) {
        return assignments.stream()
                .filter(assignment -> {
                    String text = (safe(assignment.getTitle()) + " " + safe(assignment.getDescription())).toLowerCase(Locale.ROOT);
                    for (String keyword : keywords) {
                        if (text.contains(keyword)) {
                            return true;
                        }
                    }
                    return false;
                })
                .findFirst()
                .orElse(null);
    }

    private boolean isDuplicate(List<AssignmentDependency> dependencies, AssignmentDependency candidate) {
        return dependencies.stream().anyMatch(existing ->
                existing.getAssignmentId() == candidate.getAssignmentId()
                        && existing.getDependsOnAssignmentId() == candidate.getDependsOnAssignmentId());
    }

    private String buildStatusBreakdown(List<Assignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return "none";
        }
        Map<String, Long> counts = assignments.stream()
                .collect(Collectors.groupingBy(a -> safe(a.getStatus()), LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    private String defaultRecommendation(int index) {
        if (index == 0) {
            return "Keep the active workload focused on the most time-sensitive assignments first.";
        }
        if (index == 1) {
            return "Review project sequencing weekly and adjust ownership or priorities when blockers appear.";
        }
        return "Use the generated report as a checkpoint for progress and risk review.";
    }

    private double computeProgressScore(List<Assignment> assignments) {
        if (assignments.isEmpty()) {
            return 2.5;
        }
        long completed = assignments.stream().filter(Assignment::isCompleted).count();
        long inProgress = assignments.stream().filter(Assignment::isInProgress).count();
        double completionRatio = completed / (double) assignments.size();
        double momentumRatio = (completed + (inProgress * 0.5)) / assignments.size();
        return clamp((completionRatio * 7.0) + (momentumRatio * 3.0), 0, 10);
    }

    private double computeActivityScore(Project project, List<Assignment> assignments) {
        if (assignments.isEmpty()) {
            return 2.0;
        }
        long recentlyUpdated = assignments.stream()
                .filter(assignment -> assignment.getUpdatedAt() != null && !assignment.getUpdatedAt().isBefore(java.time.LocalDateTime.now().minusDays(7)))
                .count();
        long recentlyCreated = assignments.stream()
                .filter(assignment -> assignment.getCreatedAt() != null && !assignment.getCreatedAt().isBefore(java.time.LocalDateTime.now().minusDays(7)))
                .count();
        double ratio = Math.min(1.0, (recentlyUpdated + recentlyCreated) / (double) Math.max(1, assignments.size()));
        double projectBonus = project != null && "In Progress".equalsIgnoreCase(project.getStatus()) ? 1.0 : 0.0;
        return clamp((ratio * 9.0) + projectBonus, 0, 10);
    }

    private double computeDeadlinesScore(List<Assignment> assignments) {
        if (assignments.isEmpty()) {
            return 5.0;
        }
        long overdue = assignments.stream().filter(Assignment::isOverdue).count();
        long dueSoon = assignments.stream()
                .filter(assignment -> assignment.getEndDate() != null
                        && !assignment.isCompleted()
                        && !assignment.getEndDate().isBefore(LocalDate.now())
                        && !assignment.getEndDate().isAfter(LocalDate.now().plusDays(2)))
                .count();
        double overduePenalty = (overdue / (double) assignments.size()) * 7.0;
        double dueSoonPenalty = (dueSoon / (double) assignments.size()) * 2.0;
        return clamp(10.0 - overduePenalty - dueSoonPenalty, 0, 10);
    }

    private double computeBalanceScore(List<Assignment> assignments) {
        if (assignments.isEmpty()) {
            return 5.0;
        }
        long todo = assignments.stream().filter(Assignment::isTodo).count();
        long inProgress = assignments.stream().filter(Assignment::isInProgress).count();
        long completed = assignments.stream().filter(Assignment::isCompleted).count();
        double total = assignments.size();
        double spreadPenalty = Math.abs((todo / total) - 0.4) + Math.abs((inProgress / total) - 0.3) + Math.abs((completed / total) - 0.3);
        return clamp(10.0 - (spreadPenalty * 6.5), 0, 10);
    }

    private String heuristicHealthSummary(
            Project project,
            List<Assignment> assignments,
            double progressScore,
            double activityScore,
            double deadlinesScore,
            double balanceScore,
            double overallScore
    ) {
        StringBuilder summary = new StringBuilder();
        summary.append(safe(project == null ? null : project.getTitle())).append(" has an overall health score of ")
                .append(String.format(Locale.US, "%.1f", overallScore)).append("/10. ");
        if (deadlinesScore < 5.0) {
            summary.append("Deadline risk is the main issue, with overdue or near-due assignments reducing stability. ");
        } else if (progressScore < 5.0) {
            summary.append("Progress is lagging, which suggests the project is not converting backlog into completed work fast enough. ");
        } else {
            summary.append("The project is moving with acceptable momentum, though there is still room to tighten execution. ");
        }
        if (activityScore < 5.0) {
            summary.append("Recent assignment activity is low, so the board may not reflect active ownership clearly. ");
        }
        if (balanceScore < 5.0) {
            summary.append("Workload balance across backlog, active work, and completed items is uneven.");
        } else {
            summary.append("Task distribution is reasonably balanced across the workflow.");
        }
        return summary.toString().trim();
    }

    private String cleanJson(String value) {
        return value == null ? "" : value.replace("```json", "").replace("```", "").trim();
    }

    private Integer coercePositiveInt(Object value, int fallback) {
        try {
            int parsed = Integer.parseInt(String.valueOf(value).replaceAll("[^0-9-]", ""));
            return parsed > 0 ? parsed : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String normalizeComplexity(String value) {
        String normalized = safe(value).toLowerCase(Locale.ROOT);
        if (normalized.contains("high") || normalized.contains("complex")) {
            return "High";
        }
        if (normalized.contains("medium") || normalized.contains("moderate")) {
            return "Medium";
        }
        return "Low";
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(safe(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String trimRationale(String value) {
        String text = safe(value);
        if (text.length() <= 140) {
            return text;
        }
        return text.substring(0, 137) + "...";
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
