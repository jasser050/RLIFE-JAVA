package com.studyflow.models;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class QuizStress {
    private int id;
    private LocalDateTime quizDate;
    private Map<Integer, Integer> answers = new LinkedHashMap<>();
    private int totalScore;
    private String stressLevel;
    private String interpretation;
    private boolean createdWithAi;
    private String aiModel;
    private String aiPromptVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer userId;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getQuizDate() { return quizDate; }
    public void setQuizDate(LocalDateTime quizDate) { this.quizDate = quizDate; }

    public Map<Integer, Integer> getAnswers() { return answers; }
    public void setAnswers(Map<Integer, Integer> answers) { this.answers = answers; }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }

    public String getStressLevel() { return stressLevel; }
    public void setStressLevel(String stressLevel) { this.stressLevel = stressLevel; }

    public String getInterpretation() { return interpretation; }
    public void setInterpretation(String interpretation) { this.interpretation = interpretation; }

    public boolean isCreatedWithAi() { return createdWithAi; }
    public void setCreatedWithAi(boolean createdWithAi) { this.createdWithAi = createdWithAi; }

    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }

    public String getAiPromptVersion() { return aiPromptVersion; }
    public void setAiPromptVersion(String aiPromptVersion) { this.aiPromptVersion = aiPromptVersion; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
}
