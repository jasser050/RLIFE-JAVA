package com.studyflow.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.models.Assignment;
import com.studyflow.models.Project;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AiAssignmentGeneratorService {
    private final ClaudeApiService claudeApiService;
    private final ObjectMapper mapper;
    private final AiProjectInsightsService aiProjectInsightsService;

    public AiAssignmentGeneratorService() {
        this.claudeApiService = new ClaudeApiService();
        this.mapper = new ObjectMapper();
        this.aiProjectInsightsService = new AiProjectInsightsService();
    }

    public boolean isConfigured() {
        return claudeApiService.isConfigured();
    }

    public List<Assignment> generateSuggestionsForProject(Project project, int userId) {
        if (project == null) {
            throw new IllegalArgumentException("Project is required.");
        }
        String systemPrompt = """
                You are a project management assistant.
                Always respond ONLY with a valid JSON array. No explanation, no markdown, no extra text.
                """;

        String prompt = String.format("""
                Generate exactly 5 realistic assignments for this project.

                Project title: %s
                Project description: %s
                Project start date: %s
                Project end date: %s

                Return ONLY a JSON array in this exact shape:
                [
                  {
                    "title": "Task title",
                    "description": "Detailed description",
                    "priority": "Medium",
                    "status": "To Do"
                  }
                ]

                Rules:
                - Return exactly 5 items
                - priority must be one of: Low, Medium, High
                - status must always be: To Do
                - Titles must be concrete and actionable
                - Descriptions must be useful and concise
                - Keep the tasks relevant to the project title and description
                """,
                safe(project.getTitle()),
                safe(project.getDescription()),
                project.getStartDate() == null ? "unknown" : project.getStartDate(),
                project.getEndDate() == null ? "unknown" : project.getEndDate()
        );

        String response = claudeApiService.ask(prompt, systemPrompt);
        String cleanJson = response.replace("```json", "").replace("```", "").trim();

        try {
            List<Map<String, String>> taskMaps = mapper.readValue(
                    cleanJson,
                    new TypeReference<List<Map<String, String>>>() {
                    }
            );
            if (taskMaps.isEmpty()) {
                throw new IllegalStateException("AI provider returned an empty assignment list.");
            }

            List<Assignment> assignments = new ArrayList<>();
            for (int index = 0; index < Math.min(5, taskMaps.size()); index++) {
                Map<String, String> task = taskMaps.get(index);
                Assignment assignment = new Assignment();
                assignment.setUserId(userId);
                assignment.setProjectId(project.getId());
                assignment.setProjectTitle(project.getTitle());
                assignment.setTitle(fallback(task.get("title"), "AI assignment " + (index + 1)));
                assignment.setDescription(fallback(task.get("description"), "Generated from project context."));
                assignment.setPriority(normalizePriority(task.get("priority")));
                assignment.setStatus("To Do");
                assignment.setStartDate(resolveStartDate(project, index));
                assignment.setEndDate(resolveEndDate(project, assignment.getStartDate(), index));
                assignment.setStartTime(LocalTime.of(9, 0));
                assignment.setEndTime(LocalTime.of(13, 0));
                assignments.add(assignment);
            }

            while (assignments.size() < 5) {
                int index = assignments.size();
                Assignment assignment = new Assignment();
                assignment.setUserId(userId);
                assignment.setProjectId(project.getId());
                assignment.setProjectTitle(project.getTitle());
                assignment.setTitle("AI assignment " + (index + 1));
                assignment.setDescription("Generated fallback assignment for " + safe(project.getTitle()) + ".");
                assignment.setPriority("Medium");
                assignment.setStatus("To Do");
                assignment.setStartDate(resolveStartDate(project, index));
                assignment.setEndDate(resolveEndDate(project, assignment.getStartDate(), index));
                assignment.setStartTime(LocalTime.of(9, 0));
                assignment.setEndTime(LocalTime.of(13, 0));
                assignments.add(assignment);
            }

            return assignments;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse AI-generated assignments: " + e.getMessage(), e);
        }
    }

    public List<Assignment> saveSuggestions(List<Assignment> suggestions) {
        AssignmentService assignmentService = new AssignmentService();
        List<Assignment> saved = new ArrayList<>();
        if (suggestions == null) {
            return saved;
        }
        for (Assignment assignment : suggestions) {
            AiProjectInsightsService.AssignmentPlanningEstimate estimate = aiProjectInsightsService.estimateAssignmentPlan(assignment);
            assignment.setEstimatedMinDays(estimate.minDays());
            assignment.setEstimatedMaxDays(estimate.maxDays());
            assignment.setComplexityLevel(estimate.complexityLevel());
            assignment.setAiSuggestedDueDate(estimate.suggestedDueDate());
            assignmentService.add(assignment);
            if (assignment.getId() > 0) {
                saved.add(assignment);
            }
        }
        return saved;
    }

    private LocalDate resolveStartDate(Project project, int index) {
        LocalDate projectStart = project.getStartDate() == null ? LocalDate.now() : project.getStartDate();
        LocalDate projectEnd = project.getEndDate() == null ? projectStart.plusDays(6) : project.getEndDate();
        if (projectEnd.isBefore(projectStart)) {
            return projectStart;
        }
        long span = Math.max(1, ChronoUnit.DAYS.between(projectStart, projectEnd));
        long step = Math.max(1, span / 5);
        LocalDate candidate = projectStart.plusDays(step * index);
        return candidate.isAfter(projectEnd) ? projectEnd : candidate;
    }

    private LocalDate resolveEndDate(Project project, LocalDate startDate, int index) {
        LocalDate projectEnd = project.getEndDate() == null ? startDate.plusDays(2) : project.getEndDate();
        if (projectEnd.isBefore(startDate)) {
            return startDate;
        }
        LocalDate candidate = startDate.plusDays(Math.max(1, index % 3));
        return candidate.isAfter(projectEnd) ? projectEnd : candidate;
    }

    private String normalizePriority(String priority) {
        if (priority == null) {
            return "Medium";
        }
        String normalized = priority.trim().toLowerCase();
        if (normalized.contains("high") || normalized.contains("critical")) {
            return "High";
        }
        if (normalized.contains("low")) {
            return "Low";
        }
        return "Medium";
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
