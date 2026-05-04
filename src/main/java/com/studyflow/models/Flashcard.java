package com.studyflow.models;

import java.time.LocalDateTime;

public class Flashcard {

    // ── Validation constants ───────────────────────────────────────────────
    public static final int    TITRE_MIN         = 3;
    public static final int    TITRE_MAX         = 80;
    public static final int    QUESTION_MIN      = 3;
    public static final int    QUESTION_MAX      = 500;
    public static final int    REPONSE_MIN       = 3;
    public static final int    REPONSE_MAX       = 500;
    public static final int    DESCRIPTION_MAX   = 300;
    public static final long   FILE_MAX_BYTES    = 10L * 1024 * 1024; // 10 Mo
    public static final String FILE_MAX_LABEL    = "10 Mo";
    public static final java.util.List<String> IMAGE_EXTENSIONS =
            java.util.List.of("png", "jpg", "jpeg", "gif", "webp");

    // ── Fields ────────────────────────────────────────────────────────────
    private int idFlashcard;
    private int idDeck;
    private String titre;
    private String question;
    private String reponse;
    private String description;
    private int niveauDifficulte;   // 1 = Easy, 2 = Medium, 3 = Hard
    private String etat;            // "new" | "learning" | "mastered"
    private String image;
    private String pdf;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private Integer createdBy;

    // ── Constructors ──────────────────────────────────────────────────────
    public Flashcard() {
        this.etat = "new";
        this.niveauDifficulte = 1;
        this.dateCreation = LocalDateTime.now();
    }

    public Flashcard(int idDeck, String titre, String question, String reponse,
                     String description, int niveauDifficulte, int createdBy) {
        this();
        this.idDeck = idDeck;
        this.titre = titre;
        this.question = question;
        this.reponse = reponse;
        this.description = description;
        this.niveauDifficulte = niveauDifficulte;
        this.createdBy = createdBy;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────
    public int getIdFlashcard()                       { return idFlashcard; }
    public void setIdFlashcard(int idFlashcard)       { this.idFlashcard = idFlashcard; }

    public int getIdDeck()                            { return idDeck; }
    public void setIdDeck(int idDeck)                 { this.idDeck = idDeck; }

    public String getTitre()                          { return titre; }
    public void setTitre(String titre)                { this.titre = titre; }

    public String getQuestion()                       { return question; }
    public void setQuestion(String question)          { this.question = question; }

    public String getReponse()                        { return reponse; }
    public void setReponse(String reponse)            { this.reponse = reponse; }

    public String getDescription()                    { return description; }
    public void setDescription(String description)    { this.description = description; }

    public int getNiveauDifficulte()                  { return niveauDifficulte; }
    public void setNiveauDifficulte(int n)            { this.niveauDifficulte = n; }

    public String getEtat()                           { return etat; }
    public void setEtat(String etat)                  { this.etat = etat; }

    public String getImage()                          { return image; }
    public void setImage(String image)                { this.image = image; }

    public String getPdf()                            { return pdf; }
    public void setPdf(String pdf)                    { this.pdf = pdf; }

    public LocalDateTime getDateCreation()            { return dateCreation; }
    public void setDateCreation(LocalDateTime d)      { this.dateCreation = d; }

    public LocalDateTime getDateModification()        { return dateModification; }
    public void setDateModification(LocalDateTime d)  { this.dateModification = d; }

    public Integer getCreatedBy()                     { return createdBy; }
    public void setCreatedBy(Integer createdBy)       { this.createdBy = createdBy; }

    public String getDifficultyLabel() {
        return switch (niveauDifficulte) {
            case 1  -> "Easy";
            case 2  -> "Medium";
            case 3  -> "Hard";
            default -> "Easy";
        };
    }

    @Override
    public String toString() { return titre != null ? titre : "Flashcard #" + idFlashcard; }
}