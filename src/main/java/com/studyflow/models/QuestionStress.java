package com.studyflow.models;

import java.time.LocalDateTime;

public class QuestionStress {

    private int id;
    private int position;
    private String questionText;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public QuestionStress() {
    }

    public QuestionStress(int id, int position, String questionText, boolean active,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.position = position;
        this.questionText = questionText;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    // Compatibility for existing code that still uses "questionNumber"
    public int getQuestionNumber() {
        return position;
    }

    public void setQuestionNumber(int number) {
        this.position = number;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    @Override
    public String toString() {
        return "QuestionStress{id=" + id + ", position=" + position + ", active=" + active + "}";
    }
}
