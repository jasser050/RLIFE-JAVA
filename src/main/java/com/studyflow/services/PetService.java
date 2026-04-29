package com.studyflow.services;

import com.studyflow.models.Pet;
import com.studyflow.models.PetEvent;
import com.studyflow.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class PetService {
    private final Connection cnx;

    public PetService() {
        this.cnx = MyDataBase.getInstance().getConnection();
        if (this.cnx != null) {
            ensureSchema();
        }
    }

    public boolean isDatabaseAvailable() {
        return cnx != null && MyDataBase.getInstance().isConnected();
    }

    public Optional<Pet> findByUserId(int userId) {
        requireDatabase();
        String sql = """
                SELECT id, user_id, name, type, rarity, personality, level, xp, hunger, happiness,
                       energy, health, coins_spent, last_interaction_at, last_hunger_at, created_at, last_event_at, state_flags
                FROM pet
                WHERE user_id = ?
                ORDER BY id DESC
                LIMIT 1
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapPet(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch pet data.", e);
        }
    }

    public Pet createPet(int userId, String type, String name) {
        requireDatabase();
        validateType(type);
        if (name == null || name.trim().length() < 2 || name.trim().length() > 25) {
            throw new RuntimeException("Pet name must be between 2 and 25 characters.");
        }
        if (findByUserId(userId).isPresent()) {
            throw new RuntimeException("You already have a companion.");
        }
        LocalDateTime now = LocalDateTime.now();
        String sql = """
                INSERT INTO pet
                (user_id, name, type, rarity, personality, level, xp, hunger, happiness, energy, health,
                 coins_spent, last_interaction_at, last_hunger_at, created_at, last_event_at, state_flags)
                VALUES (?, ?, ?, ?, ?, 1, 0, 24, 72, 80, 100, 0, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, name.trim());
            ps.setString(3, type);
            ps.setString(4, randomRarity());
            ps.setString(5, personalityFor(type));
            ps.setTimestamp(6, Timestamp.valueOf(now));
            ps.setTimestamp(7, Timestamp.valueOf(now));
            ps.setTimestamp(8, Timestamp.valueOf(now));
            ps.setTimestamp(9, Timestamp.valueOf(now));
            ps.setString(10, "[]");
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (!keys.next()) {
                throw new RuntimeException("Pet could not be created.");
            }
            int petId = keys.getInt(1);
            createEvent(petId, "adopt", "epic", "Companion adopted", name.trim() + " joined your RLIFE journey.", "{\"xp\":0}");
            return findById(petId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create pet.", e);
        }
    }

    public Pet renamePet(int userId, int petId, String newName) {
        requireDatabase();
        if (newName == null || newName.trim().length() < 2 || newName.trim().length() > 25) {
            throw new RuntimeException("Pet name must be between 2 and 25 characters.");
        }
        String sql = "UPDATE pet SET name = ?, last_interaction_at = ?, last_event_at = ? WHERE id = ? AND user_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, newName.trim());
            ps.setTimestamp(2, Timestamp.valueOf(now));
            ps.setTimestamp(3, Timestamp.valueOf(now));
            ps.setInt(4, petId);
            ps.setInt(5, userId);
            if (ps.executeUpdate() == 0) {
                throw new RuntimeException("Companion not found.");
            }
            createEvent(petId, "rename", "common", "Name updated", "Your companion is now called " + newName.trim() + ".", "{\"xp\":0}");
            return findById(petId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to rename pet.", e);
        }
    }

    public Pet changeType(int userId, int petId, String newType) {
        requireDatabase();
        validateType(newType);
        Pet pet = syncPetState(findById(petId));
        if (pet.getUserId() != userId) {
            throw new RuntimeException("Companion not found.");
        }
        if (pet.getType().equalsIgnoreCase(newType)) {
            throw new RuntimeException("This is already your current companion type.");
        }
        int cost = 300;
        int coins = getUserCoins(userId);
        if (coins < cost) {
            throw new RuntimeException("You need 300 coins to change companion type.");
        }
        updateUserCoins(userId, -cost);
        String sql = """
                UPDATE pet
                SET type = ?, personality = ?, coins_spent = coins_spent + ?, happiness = LEAST(100, happiness + 4),
                    last_interaction_at = ?, last_event_at = ?
                WHERE id = ? AND user_id = ?
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, newType);
            ps.setString(2, personalityFor(newType));
            ps.setInt(3, cost);
            LocalDateTime now = LocalDateTime.now();
            ps.setTimestamp(4, Timestamp.valueOf(now));
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.setInt(6, petId);
            ps.setInt(7, userId);
            ps.executeUpdate();
            createEvent(petId, "change_type", "rare", "Type changed", "Your companion transformed into a " + newType + ".", "{\"coins\":-300}");
            return findById(petId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to change pet type.", e);
        }
    }

    public Pet feedPet(int userId, int petId, String foodType) {
        requireDatabase();
        Pet pet = syncPetState(findById(petId));
        if (pet.getUserId() != userId) {
            throw new RuntimeException("Companion not found.");
        }
        Map<String, FoodDef> foods = foodCatalog();
        FoodDef food = foods.get(foodType);
        if (food == null) {
            throw new RuntimeException("Unknown food type.");
        }
        if (getUserCoins(userId) < food.cost) {
            throw new RuntimeException("Not enough coins for " + food.name + ".");
        }
        updateUserCoins(userId, -food.cost);
        pet.setHunger(clamp(pet.getHunger() - food.hungerReduction));
        pet.setHappiness(clamp(pet.getHappiness() + 5));
        pet.setEnergy(clamp(pet.getEnergy() + 7));
        pet.setCoinsSpent(pet.getCoinsSpent() + food.cost);
        gainXp(pet, 10);
        persistPet(pet);
        createEvent(petId, "feed", "common", food.name, pet.getName() + " enjoyed a " + food.name.toLowerCase() + ".", "{\"hunger\":-" + food.hungerReduction + "}");
        return findById(petId);
    }

    public Pet performAction(int userId, int petId, String action) {
        requireDatabase();
        Pet pet = syncPetState(findById(petId));
        if (pet.getUserId() != userId) {
            throw new RuntimeException("Companion not found.");
        }
        switch ((action == null ? "" : action.trim().toLowerCase())) {
            case "study" -> {
                pet.setEnergy(clamp(pet.getEnergy() - 10));
                pet.setHappiness(clamp(pet.getHappiness() + 3));
                gainXp(pet, 18);
                updateUserCoins(userId, 35);
                createEvent(petId, "study", "rare", "Study session", pet.getName() + " focused with you and earned 35 coins.", "{\"coins\":35,\"xp\":18}");
            }
            case "play" -> {
                pet.setEnergy(clamp(pet.getEnergy() - 12));
                pet.setHappiness(clamp(pet.getHappiness() + 14));
                pet.setHunger(clamp(pet.getHunger() + 6));
                gainXp(pet, 12);
                createEvent(petId, "play", "common", "Play time", pet.getName() + " had a playful moment.", "{\"happiness\":14,\"xp\":12}");
            }
            case "rest" -> {
                pet.setEnergy(clamp(pet.getEnergy() + 20));
                pet.setHealth(clamp(pet.getHealth() + 8));
                pet.setHunger(clamp(pet.getHunger() + 4));
                gainXp(pet, 8);
                createEvent(petId, "rest", "common", "Recovery", pet.getName() + " recovered energy.", "{\"energy\":20,\"health\":8,\"xp\":8}");
            }
            case "clean" -> {
                pet.setHealth(clamp(pet.getHealth() + 12));
                pet.setHappiness(clamp(pet.getHappiness() + 6));
                gainXp(pet, 9);
                createEvent(petId, "clean", "common", "Care routine", pet.getName() + " looks fresh and healthy.", "{\"health\":12,\"happiness\":6,\"xp\":9}");
            }
            default -> throw new RuntimeException("Unknown action.");
        }
        persistPet(pet);
        return findById(petId);
    }

    public Pet syncPetState(Pet pet) {
        requireDatabase();
        if (pet == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastTick = pet.getLastHungerAt() == null ? now : pet.getLastHungerAt();
        long minutes = Math.max(0, Duration.between(lastTick, now).toMinutes());
        if (minutes < 30) {
            return pet;
        }
        long hungerSteps = minutes / 45;
        long moodSteps = minutes / 90;
        if (hungerSteps <= 0 && moodSteps <= 0) {
            return pet;
        }
        pet.setHunger(clamp(pet.getHunger() + (int) hungerSteps * 3));
        pet.setEnergy(clamp(pet.getEnergy() - (int) Math.max(1, moodSteps) * 2));
        pet.setHappiness(clamp(pet.getHappiness() - (int) Math.max(1, moodSteps)));
        if (pet.getHunger() >= 92) {
            pet.setHealth(clamp(pet.getHealth() - 4));
        }
        pet.setLastHungerAt(now);
        pet.setUpdatedAt(now);
        persistPet(pet);
        return findById(pet.getId());
    }

    public List<PetEvent> findRecentEvents(int petId, int limit) {
        requireDatabase();
        String sql = """
                SELECT id, pet_id, event_type, title, description, rarity, created_at
                FROM pet_event
                WHERE pet_id = ?
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """;
        List<PetEvent> items = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, petId);
            ps.setInt(2, Math.max(1, limit));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                items.add(mapEvent(rs));
            }
            return items;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load pet history.", e);
        }
    }

    public int getUserCoins(int userId) {
        requireDatabase();
        try (PreparedStatement ps = cnx.prepareStatement("SELECT COALESCE(coins, 0) FROM `user` WHERE id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load coins.", e);
        }
    }

    private void updateUserCoins(int userId, int delta) {
        requireDatabase();
        String sql = "UPDATE `user` SET coins = GREATEST(0, COALESCE(coins, 0) + ?) WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user coins.", e);
        }
    }

    private Pet findById(int petId) {
        requireDatabase();
        try (PreparedStatement ps = cnx.prepareStatement("""
                SELECT id, user_id, name, type, rarity, personality, level, xp, hunger, happiness,
                       energy, health, coins_spent, last_interaction_at, last_hunger_at, created_at, last_event_at, state_flags
                FROM pet
                WHERE id = ?
                LIMIT 1
                """)) {
            ps.setInt(1, petId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapPet(rs);
            }
            throw new RuntimeException("Companion not found.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load companion.", e);
        }
    }

    private void persistPet(Pet pet) {
        requireDatabase();
        String sql = """
                UPDATE pet
                SET level = ?, xp = ?, hunger = ?, happiness = ?, energy = ?, health = ?,
                    coins_spent = ?, last_interaction_at = ?, last_hunger_at = ?, last_event_at = ?, state_flags = ?
                WHERE id = ?
                """;
        LocalDateTime now = LocalDateTime.now();
        if (pet.getLastInteractionAt() == null) {
            pet.setLastInteractionAt(now);
        }
        pet.setUpdatedAt(now);
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, pet.getLevel());
            ps.setInt(2, pet.getXp());
            ps.setInt(3, pet.getHunger());
            ps.setInt(4, pet.getHappiness());
            ps.setInt(5, pet.getEnergy());
            ps.setInt(6, pet.getHealth());
            ps.setInt(7, pet.getCoinsSpent());
            ps.setTimestamp(8, Timestamp.valueOf(pet.getLastInteractionAt()));
            ps.setTimestamp(9, Timestamp.valueOf(pet.getLastHungerAt() == null ? now : pet.getLastHungerAt()));
            ps.setTimestamp(10, Timestamp.valueOf(now));
            ps.setString(11, "[]");
            ps.setInt(12, pet.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update companion.", e);
        }
    }

    private void gainXp(Pet pet, int amount) {
        pet.setXp(Math.max(0, pet.getXp() + amount));
        pet.setLastInteractionAt(LocalDateTime.now());
        while (pet.getXp() >= pet.getXpToNextLevel()) {
            pet.setXp(pet.getXp() - pet.getXpToNextLevel());
            pet.setLevel(pet.getLevel() + 1);
            pet.setHealth(clamp(pet.getHealth() + 6));
            pet.setEnergy(clamp(pet.getEnergy() + 5));
            pet.setHappiness(clamp(pet.getHappiness() + 5));
            createEvent(pet.getId(), "level_up", "epic", "Level up", pet.getName() + " reached level " + pet.getLevel() + ".", "{\"level\":" + pet.getLevel() + "}");
            unlockAchievementIfMissing(pet.getId(), "pet_level_" + pet.getLevel(), "Level " + pet.getLevel(), "Reach level " + pet.getLevel() + ".", 40, 15);
        }
    }

    private void createEvent(int petId, String type, String rarity, String title, String description, String effectsJson) {
        requireDatabase();
        String sql = """
                INSERT INTO pet_event (pet_id, event_type, rarity, title, description, effects, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, petId);
            ps.setString(2, type);
            ps.setString(3, rarity);
            ps.setString(4, title);
            ps.setString(5, description);
            ps.setString(6, effectsJson == null ? "{}" : effectsJson);
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create pet event.", e);
        }
    }

    private Pet mapPet(ResultSet rs) throws SQLException {
        Pet pet = new Pet();
        pet.setId(rs.getInt("id"));
        pet.setUserId(rs.getInt("user_id"));
        pet.setName(rs.getString("name"));
        pet.setType(rs.getString("type"));
        pet.setRarity(rs.getString("rarity"));
        pet.setPersonality(rs.getString("personality"));
        pet.setLevel(rs.getInt("level"));
        pet.setXp(rs.getInt("xp"));
        pet.setHunger(rs.getInt("hunger"));
        pet.setHappiness(rs.getInt("happiness"));
        pet.setEnergy(rs.getInt("energy"));
        pet.setHealth(rs.getInt("health"));
        pet.setCoinsSpent(rs.getInt("coins_spent"));
        pet.setLastInteractionAt(readTimestamp(rs, "last_interaction_at"));
        pet.setLastHungerAt(readTimestamp(rs, "last_hunger_at"));
        pet.setCreatedAt(readTimestamp(rs, "created_at"));
        pet.setUpdatedAt(readTimestamp(rs, "last_event_at"));
        return pet;
    }

    private PetEvent mapEvent(ResultSet rs) throws SQLException {
        PetEvent event = new PetEvent();
        event.setId(rs.getInt("id"));
        event.setPetId(rs.getInt("pet_id"));
        event.setEventType(rs.getString("event_type"));
        event.setTitle(rs.getString("title"));
        event.setDescription(rs.getString("description"));
        event.setRarity(rs.getString("rarity"));
        event.setCreatedAt(readTimestamp(rs, "created_at"));
        return event;
    }

    private LocalDateTime readTimestamp(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private void validateType(String type) {
        if (!typeCatalog().containsKey(type)) {
            throw new RuntimeException("Invalid companion type.");
        }
    }

    private String personalityFor(String type) {
        return switch (type) {
            case "dog", "fox", "bird" -> "Playful";
            case "dragon" -> "Bold";
            case "hamster", "panda", "rabbit" -> "Calm";
            default -> "Curious";
        };
    }

    private String randomRarity() {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 5) return "Legendary";
        if (roll < 20) return "Epic";
        if (roll < 45) return "Rare";
        return "Common";
    }

    private Map<String, String> typeCatalog() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("cat", "Cat");
        map.put("dog", "Dog");
        map.put("dragon", "Dragon");
        map.put("fox", "Fox");
        map.put("bird", "Bird");
        map.put("hamster", "Hamster");
        map.put("panda", "Panda");
        map.put("rabbit", "Rabbit");
        return map;
    }

    private Map<String, FoodDef> foodCatalog() {
        Map<String, FoodDef> map = new LinkedHashMap<>();
        map.put("basic", new FoodDef("Basic food", 50, 20));
        map.put("premium", new FoodDef("Premium meal", 120, 45));
        map.put("deluxe", new FoodDef("Deluxe feast", 200, 75));
        return map;
    }

    private void ensureSchema() {
        try (Statement stm = cnx.createStatement()) {
            stm.execute("""
                    CREATE TABLE IF NOT EXISTS pet (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        type VARCHAR(50) NOT NULL,
                        name VARCHAR(100) NOT NULL,
                        level INT NOT NULL DEFAULT 1,
                        xp INT NOT NULL DEFAULT 0,
                        hunger INT NOT NULL DEFAULT 24,
                        coins_spent INT NOT NULL DEFAULT 0,
                        created_at DATETIME NOT NULL,
                        last_hunger_at DATETIME NOT NULL,
                        personality VARCHAR(30) NOT NULL DEFAULT 'calm',
                        rarity VARCHAR(20) NOT NULL DEFAULT 'common',
                        happiness INT NOT NULL DEFAULT 72,
                        energy INT NOT NULL DEFAULT 80,
                        health INT NOT NULL DEFAULT 100,
                        state_flags LONGTEXT NULL,
                        last_interaction_at DATETIME NULL,
                        last_event_at DATETIME NULL,
                        INDEX idx_pet_user (user_id),
                        CONSTRAINT fk_pet_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
                    )
                    """);
            stm.execute("""
                    CREATE TABLE IF NOT EXISTS pet_event (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        pet_id INT NOT NULL,
                        event_type VARCHAR(40) NOT NULL,
                        rarity VARCHAR(30) NOT NULL,
                        title VARCHAR(255) NOT NULL,
                        description VARCHAR(255) NOT NULL,
                        effects LONGTEXT NULL,
                        created_at DATETIME NOT NULL,
                        INDEX idx_pet_event_pet (pet_id),
                        CONSTRAINT fk_pet_event_pet FOREIGN KEY (pet_id) REFERENCES pet(id) ON DELETE CASCADE
                    )
                    """);
            stm.execute("""
                    CREATE TABLE IF NOT EXISTS pet_achievement (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        pet_id INT NOT NULL,
                        code VARCHAR(100) NOT NULL,
                        title VARCHAR(160) NOT NULL,
                        description LONGTEXT NOT NULL,
                        reward_coins INT NOT NULL DEFAULT 0,
                        reward_xp INT NOT NULL DEFAULT 0,
                        unlocked_at DATETIME NOT NULL,
                        INDEX idx_pet_achievement_pet (pet_id),
                        CONSTRAINT fk_pet_achievement_pet FOREIGN KEY (pet_id) REFERENCES pet(id) ON DELETE CASCADE
                    )
                    """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to prepare pet storage.", e);
        }
    }

    private void unlockAchievementIfMissing(int petId, String code, String title, String description, int rewardCoins, int rewardXp) {
        try (PreparedStatement check = cnx.prepareStatement("SELECT id FROM pet_achievement WHERE pet_id = ? AND code = ? LIMIT 1")) {
            check.setInt(1, petId);
            check.setString(2, code);
            ResultSet rs = check.executeQuery();
            if (rs.next()) {
                return;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check pet achievement.", e);
        }

        try (PreparedStatement insert = cnx.prepareStatement("""
                INSERT INTO pet_achievement (pet_id, code, title, description, reward_coins, reward_xp, unlocked_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            insert.setInt(1, petId);
            insert.setString(2, code);
            insert.setString(3, title);
            insert.setString(4, description);
            insert.setInt(5, rewardCoins);
            insert.setInt(6, rewardXp);
            insert.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save pet achievement.", e);
        }
    }

    private void requireDatabase() {
        if (!isDatabaseAvailable()) {
            throw new RuntimeException("Pet database is unavailable. Check the MySQL connection.");
        }
    }

    private record FoodDef(String name, int cost, int hungerReduction) {}
}
