package com.studyflow.models;

import java.time.LocalDateTime;

/**
 * Entité QuestionStress — équivalent de App\Entity\QuestionStress (Symfony).
 *
 * Attributs mappés depuis la table question_stress :
 *   id                  → id
 *   question_number_ques → questionNumber
 *   question_text_ques   → questionText
 *   is_active_ques       → isActive
 *   created_at_ques      → createdAt
 *   updated_at_ques      → updatedAt
 */
public class QuestionStress {

    private int           id;
    private int           questionNumber;
    private String        questionText;
    private boolean       isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Constructeurs ────────────────────────────────────────────────────────

    public QuestionStress() {}

    public QuestionStress(int id, int questionNumber, String questionText,
                          boolean isActive, LocalDateTime createdAt,
                          LocalDateTime updatedAt) {
        this.id             = id;
        this.questionNumber = questionNumber;
        this.questionText   = questionText;
        this.isActive       = isActive;
        this.createdAt      = createdAt;
        this.updatedAt      = updatedAt;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getQuestionNumber()              { return questionNumber; }
    public void setQuestionNumber(int n)        { this.questionNumber = n; }

    public String getQuestionText()             { return questionText; }
    public void setQuestionText(String t)       { this.questionText = t; }

    public boolean isActive()                   { return isActive; }
    public void setActive(boolean active)       { this.isActive = active; }

    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime d)   { this.createdAt = d; }

    public LocalDateTime getUpdatedAt()         { return updatedAt; }
    public void setUpdatedAt(LocalDateTime d)   { this.updatedAt = d; }

    @Override
    public String toString() {
        return "QuestionStress{id=" + id + ", number=" + questionNumber +
                ", active=" + isActive + "}";
    }
}