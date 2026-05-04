package com.studyflow.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminNotification {

    private int id;
    private int studentId;
    private String studentName;
    private String urgencyLevel;
    private String adminMessage;
    private String weakDecksJson;
    private String deckSuggestionJson;
    private String status;
    private Integer generatedDeckId;
    private String generatedDeckTitle;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    private String suggestionTitle;
    private String suggestionSubject;
    private String suggestionDifficulty;
    private String suggestionReason;
    private final List<String> focusTopics = new ArrayList<>();
    private final List<String> weakDeckNames = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(String urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    public String getAdminMessage() {
        return adminMessage;
    }

    public void setAdminMessage(String adminMessage) {
        this.adminMessage = adminMessage;
    }

    public String getWeakDecksJson() {
        return weakDecksJson;
    }

    public void setWeakDecksJson(String weakDecksJson) {
        this.weakDecksJson = weakDecksJson;
    }

    public String getDeckSuggestionJson() {
        return deckSuggestionJson;
    }

    public void setDeckSuggestionJson(String deckSuggestionJson) {
        this.deckSuggestionJson = deckSuggestionJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getGeneratedDeckId() {
        return generatedDeckId;
    }

    public void setGeneratedDeckId(Integer generatedDeckId) {
        this.generatedDeckId = generatedDeckId;
    }

    public String getGeneratedDeckTitle() {
        return generatedDeckTitle;
    }

    public void setGeneratedDeckTitle(String generatedDeckTitle) {
        this.generatedDeckTitle = generatedDeckTitle;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getSuggestionTitle() {
        return suggestionTitle;
    }

    public void setSuggestionTitle(String suggestionTitle) {
        this.suggestionTitle = suggestionTitle;
    }

    public String getSuggestionSubject() {
        return suggestionSubject;
    }

    public void setSuggestionSubject(String suggestionSubject) {
        this.suggestionSubject = suggestionSubject;
    }

    public String getSuggestionDifficulty() {
        return suggestionDifficulty;
    }

    public void setSuggestionDifficulty(String suggestionDifficulty) {
        this.suggestionDifficulty = suggestionDifficulty;
    }

    public String getSuggestionReason() {
        return suggestionReason;
    }

    public void setSuggestionReason(String suggestionReason) {
        this.suggestionReason = suggestionReason;
    }

    public List<String> getFocusTopics() {
        return focusTopics;
    }

    public List<String> getWeakDeckNames() {
        return weakDeckNames;
    }

    public boolean isPending() {
        return status == null || status.isBlank() || "pending".equalsIgnoreCase(status);
    }

    public boolean matchesDeckName(String deckName) {
        if (deckName == null || deckName.isBlank()) {
            return false;
        }
        String normalized = deckName.trim().toLowerCase(Locale.ROOT);
        return weakDeckNames.stream().anyMatch(name -> name.trim().toLowerCase(Locale.ROOT).equals(normalized));
    }

    public String getUrgencyBadgeLabel() {
        return "critical".equalsIgnoreCase(urgencyLevel) ? "Critical Alert" : "Support Alert";
    }

    public String getUrgencyColor() {
        return "critical".equalsIgnoreCase(urgencyLevel) ? "#FB7185" : "#FBBF24";
    }

    public String getFocusTopicsLabel() {
        return focusTopics.isEmpty() ? "core concepts, guided practice" : String.join(", ", focusTopics);
    }

    public String getWeakDecksLabel() {
        return weakDeckNames.isEmpty() ? "No weak decks listed" : String.join(", ", weakDeckNames);
    }
}
