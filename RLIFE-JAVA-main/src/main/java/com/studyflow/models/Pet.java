package com.studyflow.models;

import java.time.LocalDateTime;

public class Pet {
    private int id;
    private int userId;
    private String name;
    private String type;
    private String rarity;
    private String personality;
    private int level;
    private int xp;
    private int hunger;
    private int happiness;
    private int energy;
    private int health;
    private int coinsSpent;
    private LocalDateTime lastInteractionAt;
    private LocalDateTime lastHungerAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRarity() { return rarity; }
    public void setRarity(String rarity) { this.rarity = rarity; }
    public String getPersonality() { return personality; }
    public void setPersonality(String personality) { this.personality = personality; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }
    public int getHunger() { return hunger; }
    public void setHunger(int hunger) { this.hunger = hunger; }
    public int getHappiness() { return happiness; }
    public void setHappiness(int happiness) { this.happiness = happiness; }
    public int getEnergy() { return energy; }
    public void setEnergy(int energy) { this.energy = energy; }
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    public int getCoinsSpent() { return coinsSpent; }
    public void setCoinsSpent(int coinsSpent) { this.coinsSpent = coinsSpent; }
    public LocalDateTime getLastInteractionAt() { return lastInteractionAt; }
    public void setLastInteractionAt(LocalDateTime lastInteractionAt) { this.lastInteractionAt = lastInteractionAt; }
    public LocalDateTime getLastHungerAt() { return lastHungerAt; }
    public void setLastHungerAt(LocalDateTime lastHungerAt) { this.lastHungerAt = lastHungerAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getXpToNextLevel() {
        return 100 + Math.max(0, level - 1) * 45;
    }

    public String getMood() {
        if (health <= 40 || energy <= 30) {
            return "Tired";
        }
        if (hunger >= 80) {
            return "Hungry";
        }
        if (happiness >= 80 && energy >= 60) {
            return "Great";
        }
        if (happiness >= 60) {
            return "Happy";
        }
        if (happiness >= 40) {
            return "Okay";
        }
        return "Needs care";
    }

    public String getEvolutionStage() {
        if (level >= 8) {
            return "Legendary";
        }
        if (level >= 5) {
            return "Advanced";
        }
        if (level >= 3) {
            return "Growing";
        }
        return "Starter";
    }
}
