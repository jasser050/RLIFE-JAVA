package com.studyflow.models;

import java.time.LocalDateTime;

public class WellBeing {
    private int id;
    private LocalDateTime entryDate;
    private String mood;
    private int stressLevel;
    private int energyLevel;
    private double sleepHours;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer userId;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDateTime entryDate) { this.entryDate = entryDate; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public int getStressLevel() { return stressLevel; }
    public void setStressLevel(int stressLevel) { this.stressLevel = stressLevel; }

    public int getEnergyLevel() { return energyLevel; }
    public void setEnergyLevel(int energyLevel) { this.energyLevel = energyLevel; }

    public double getSleepHours() { return sleepHours; }
    public void setSleepHours(double sleepHours) { this.sleepHours = sleepHours; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getMoodEmoji() {
        return switch (mood == null ? "" : mood.toLowerCase()) {
            case "great" -> "\uD83D\uDE0A";
            case "good" -> "\uD83D\uDE0A";
            case "okay" -> "\uD83D\uDE10";
            case "stressed" -> "\uD83D\uDE1F";
            default -> "\uD83D\uDE34";
        };
    }

    public String getFormattedSleep() {
        return sleepHours == Math.rint(sleepHours)
                ? String.format("%.0fh", sleepHours)
                : String.format("%.1fh", sleepHours);
    }
}
