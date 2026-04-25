package com.studyflow.models;

import java.time.LocalDateTime;

/**
 * Rating — modèle pour la table `ratings`
 * Colonnes : id, user_id, deck_id, stars, created_at, updated_at,
 *            comment, tags, clarity, completeness, difficulty, usefulness
 */
public class Rating {

    private int           id;
    private int           userId;
    private int           deckId;
    private int           stars;          // 1-5
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String        comment;
    private String        tags;
    private Integer       clarity;        // 1-5, nullable
    private Integer       completeness;   // 1-5, nullable
    private Integer       difficulty;     // 1-5, nullable
    private Integer       usefulness;     // 1-5, nullable

    // Champs transients (jointure depuis deck) — pas en DB
    private String deckName;
    private String deckSubject;
    private String userName;
    private String userEmail;
    private String username;

    // ─────────────────────────────────────────────────────────────────────
    //  CONSTRUCTORS
    // ─────────────────────────────────────────────────────────────────────

    public Rating() {}

    /** Constructeur minimal pour un nouveau rating */
    public Rating(int userId, int deckId, int stars) {
        this.userId    = userId;
        this.deckId    = deckId;
        this.stars     = stars;
        this.createdAt = LocalDateTime.now();
    }

    /** Constructeur complet (lecture depuis la DB) */
    public Rating(int id, int userId, int deckId, int stars,
                  LocalDateTime createdAt, LocalDateTime updatedAt,
                  String comment, String tags,
                  Integer clarity, Integer completeness,
                  Integer difficulty, Integer usefulness) {
        this.id           = id;
        this.userId       = userId;
        this.deckId       = deckId;
        this.stars        = stars;
        this.createdAt    = createdAt;
        this.updatedAt    = updatedAt;
        this.comment      = comment;
        this.tags         = tags;
        this.clarity      = clarity;
        this.completeness = completeness;
        this.difficulty   = difficulty;
        this.usefulness   = usefulness;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  BUSINESS METHODS
    // ─────────────────────────────────────────────────────────────────────

    /** Retourne true si le deck est considéré comme difficile (note ≤ 3) */
    public boolean isWeak() {
        return stars <= 3;
    }

    /** Retourne true si le deck est en situation critique (note ≤ 2) */
    public boolean isCritical() {
        return stars <= 2;
    }

    /** Libellé lisible de la note */
    public String getStarsLabel() {
        return switch (stars) {
            case 1 -> "Très difficile";
            case 2 -> "Difficile";
            case 3 -> "Moyen";
            case 4 -> "Facile";
            case 5 -> "Très facile";
            default -> "Non noté";
        };
    }

    /** Emoji correspondant à la note */
    public String getStarsEmoji() {
        return switch (stars) {
            case 1 -> "❌";
            case 2 -> "😟";
            case 3 -> "😐";
            case 4 -> "😊";
            case 5 -> "✅";
            default -> "⭐";
        };
    }

    /** Couleur CSS hex associée à la note (compatible avec le design de l'app) */
    public String getStarsColor() {
        return switch (stars) {
            case 1 -> "#FB7185";  // danger
            case 2 -> "#FB923C";  // accent/orange
            case 3 -> "#FBBF24";  // warning
            case 4 -> "#34D399";  // success
            case 5 -> "#A78BFA";  // primary/violet
            default -> "#64748B";
        };
    }

    @Override
    public String toString() {
        return "Rating{id=" + id + ", userId=" + userId + ", deckId=" + deckId +
               ", deckName='" + deckName + "', stars=" + stars +
               ", label='" + getStarsLabel() + "'}";
    }

    // ─────────────────────────────────────────────────────────────────────
    //  GETTERS & SETTERS
    // ─────────────────────────────────────────────────────────────────────

    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }

    public int getUserId()                    { return userId; }
    public void setUserId(int userId)         { this.userId = userId; }

    public int getDeckId()                    { return deckId; }
    public void setDeckId(int deckId)         { this.deckId = deckId; }

    public int getStars()                     { return stars; }
    public void setStars(int stars)           { this.stars = stars; }

    public LocalDateTime getCreatedAt()               { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()               { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getComment()                { return comment; }
    public void setComment(String comment)    { this.comment = comment; }

    public String getTags()                   { return tags; }
    public void setTags(String tags)          { this.tags = tags; }

    public Integer getClarity()               { return clarity; }
    public void setClarity(Integer clarity)   { this.clarity = clarity; }

    public Integer getCompleteness()                    { return completeness; }
    public void setCompleteness(Integer completeness)   { this.completeness = completeness; }

    public Integer getDifficulty()                  { return difficulty; }
    public void setDifficulty(Integer difficulty)   { this.difficulty = difficulty; }

    public Integer getUsefulness()                  { return usefulness; }
    public void setUsefulness(Integer usefulness)   { this.usefulness = usefulness; }

    public String getDeckName()                  { return deckName; }
    public void setDeckName(String deckName)     { this.deckName = deckName; }

    public String getDeckSubject()                   { return deckSubject; }
    public void setDeckSubject(String deckSubject)   { this.deckSubject = deckSubject; }

    public String getUserName()                  { return userName; }
    public void setUserName(String userName)     { this.userName = userName; }

    public String getUserEmail()                 { return userEmail; }
    public void setUserEmail(String userEmail)   { this.userEmail = userEmail; }

    public String getUsername()                  { return username; }
    public void setUsername(String username)     { this.username = username; }
}
