package com.studyflow.models;

import java.time.LocalDateTime;

public class AssignmentDependency {
    private int id;
    private int projectId;
    private int assignmentId;
    private int dependsOnAssignmentId;
    private String assignmentTitle;
    private String dependsOnTitle;
    private String rationale;
    private String source;
    private LocalDateTime createdAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(int assignmentId) {
        this.assignmentId = assignmentId;
    }

    public int getDependsOnAssignmentId() {
        return dependsOnAssignmentId;
    }

    public void setDependsOnAssignmentId(int dependsOnAssignmentId) {
        this.dependsOnAssignmentId = dependsOnAssignmentId;
    }

    public String getAssignmentTitle() {
        return assignmentTitle;
    }

    public void setAssignmentTitle(String assignmentTitle) {
        this.assignmentTitle = assignmentTitle;
    }

    public String getDependsOnTitle() {
        return dependsOnTitle;
    }

    public void setDependsOnTitle(String dependsOnTitle) {
        this.dependsOnTitle = dependsOnTitle;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
