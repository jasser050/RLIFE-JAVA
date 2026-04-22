package com.studyflow.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Assignment {
    private int id;
    private int userId;
    private int ownerUserId;
    private int projectId;
    private String projectTitle;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String priority;
    private String status;
    private Integer estimatedMinDays;
    private Integer estimatedMaxDays;
    private String complexityLevel;
    private LocalDate aiSuggestedDueDate;
    private boolean ownedByCurrentUser;
    private String ownerName;
    private String gitCommitMessage;
    private String gitCommitPathspec;
    private String gitLastCommitHash;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime gitLastCommitAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(int ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getEstimatedMinDays() {
        return estimatedMinDays;
    }

    public void setEstimatedMinDays(Integer estimatedMinDays) {
        this.estimatedMinDays = estimatedMinDays;
    }

    public Integer getEstimatedMaxDays() {
        return estimatedMaxDays;
    }

    public void setEstimatedMaxDays(Integer estimatedMaxDays) {
        this.estimatedMaxDays = estimatedMaxDays;
    }

    public String getComplexityLevel() {
        return complexityLevel;
    }

    public void setComplexityLevel(String complexityLevel) {
        this.complexityLevel = complexityLevel;
    }

    public LocalDate getAiSuggestedDueDate() {
        return aiSuggestedDueDate;
    }

    public void setAiSuggestedDueDate(LocalDate aiSuggestedDueDate) {
        this.aiSuggestedDueDate = aiSuggestedDueDate;
    }

    public boolean isOwnedByCurrentUser() {
        return ownedByCurrentUser;
    }

    public void setOwnedByCurrentUser(boolean ownedByCurrentUser) {
        this.ownedByCurrentUser = ownedByCurrentUser;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getGitCommitMessage() {
        return gitCommitMessage;
    }

    public void setGitCommitMessage(String gitCommitMessage) {
        this.gitCommitMessage = gitCommitMessage;
    }

    public String getGitCommitPathspec() {
        return gitCommitPathspec;
    }

    public void setGitCommitPathspec(String gitCommitPathspec) {
        this.gitCommitPathspec = gitCommitPathspec;
    }

    public String getGitLastCommitHash() {
        return gitLastCommitHash;
    }

    public void setGitLastCommitHash(String gitLastCommitHash) {
        this.gitLastCommitHash = gitLastCommitHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getGitLastCommitAt() {
        return gitLastCommitAt;
    }

    public void setGitLastCommitAt(LocalDateTime gitLastCommitAt) {
        this.gitLastCommitAt = gitLastCommitAt;
    }

    public boolean isCompleted() {
        return "Completed".equalsIgnoreCase(status);
    }

    public boolean isInProgress() {
        return "In Progress".equalsIgnoreCase(status);
    }

    public boolean isTodo() {
        return "To Do".equalsIgnoreCase(status);
    }

    public boolean isOverdue() {
        return endDate != null && endDate.isBefore(LocalDate.now()) && !isCompleted();
    }

    public String getPriorityStyleClass() {
        if ("High".equalsIgnoreCase(priority)) {
            return "danger";
        }
        if ("Medium".equalsIgnoreCase(priority)) {
            return "warning";
        }
        return "success";
    }

    public String getComplexityStyleClass() {
        if ("High".equalsIgnoreCase(complexityLevel) || "Complex".equalsIgnoreCase(complexityLevel)) {
            return "danger";
        }
        if ("Medium".equalsIgnoreCase(complexityLevel) || "Moderate".equalsIgnoreCase(complexityLevel)) {
            return "warning";
        }
        return "success";
    }
}
