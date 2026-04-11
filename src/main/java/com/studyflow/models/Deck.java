package com.studyflow.models;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

public class Deck {

    // ── Constantes de validation ──────────────────────────────────────────
    public static final int    TITRE_MIN_LENGTH    = 3;
    public static final int    TITRE_MAX_LENGTH    = 100;
    public static final int    MATIERE_MIN_LENGTH  = 3;
    public static final int    MATIERE_MAX_LENGTH  = 100;
    public static final int    DESC_MAX_LENGTH     = 500;
    public static final long   FILE_MAX_SIZE_MB    = 10;
    public static final long   FILE_MAX_BYTES      = FILE_MAX_SIZE_MB * 1024 * 1024;
    public static final List<String> NIVEAUX_VALIDES    = List.of("Beginner", "Intermediate", "Advanced");
    public static final List<String> IMAGE_EXTENSIONS   = List.of("png","jpg","jpeg","gif","bmp","webp");
    public static final List<String> PDF_EXTENSIONS     = List.of("pdf");

    // ── Champs ────────────────────────────────────────────────────────────
    private int           idDeck;
    private int           userId;
    private String        titre;
    private String        matiere;
    private String        niveau;
    private String        description;
    private String        image;
    private String        pdf;
    private LocalDateTime dateCreation;

    // ── Constructeurs ─────────────────────────────────────────────────────
    public Deck() {}

    public Deck(int userId, String titre, String matiere, String niveau,
                String description, String image, String pdf) {
        this.userId      = userId;
        this.titre       = titre;
        this.matiere     = matiere;
        this.niveau      = niveau;
        this.description = description;
        this.image       = image;
        this.pdf         = pdf;
        this.dateCreation = LocalDateTime.now();
    }

    // ── Getters / Setters simples (pas de validation — pour mapRow BD) ────
    public int    getIdDeck()    { return idDeck; }
    public void   setIdDeck(int idDeck) { this.idDeck = idDeck; }

    public int    getUserId()    { return userId; }
    public void   setUserId(int userId) { this.userId = userId; }

    public String getTitre()     { return titre; }
    public void   setTitre(String titre) { this.titre = titre; }

    public String getMatiere()   { return matiere; }
    public void   setMatiere(String matiere) { this.matiere = matiere; }

    public String getNiveau()    { return niveau; }
    public void   setNiveau(String niveau) { this.niveau = niveau; }

    public String getDescription() { return description; }
    public void   setDescription(String description) { this.description = description; }

    public String getImage()     { return image; }
    public void   setImage(String image) { this.image = image; }

    public String getPdf()       { return pdf; }
    public void   setPdf(String pdf) { this.pdf = pdf; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    // ── Validation métier (appelée UNIQUEMENT avant add/update, jamais en mapRow) ──
    /**
     * Valide toutes les règles métier.
     * Lance IllegalArgumentException avec un message clair si une règle est violée.
     */
    public void validate() {
        // Titre
        if (titre == null || titre.trim().isEmpty())
            throw new IllegalArgumentException("Le titre est obligatoire.");
        if (titre.trim().length() < TITRE_MIN_LENGTH)
            throw new IllegalArgumentException(
                    "Le titre doit contenir au moins " + TITRE_MIN_LENGTH + " caractères.");
        if (titre.trim().length() > TITRE_MAX_LENGTH)
            throw new IllegalArgumentException(
                    "Le titre ne peut pas dépasser " + TITRE_MAX_LENGTH + " caractères.");

        // Matière
        if (matiere == null || matiere.trim().isEmpty())
            throw new IllegalArgumentException("La matière est obligatoire.");
        if (matiere.trim().length() < MATIERE_MIN_LENGTH)
            throw new IllegalArgumentException(
                    "La matière doit contenir au moins " + MATIERE_MIN_LENGTH + " caractères.");
        if (matiere.trim().length() > MATIERE_MAX_LENGTH)
            throw new IllegalArgumentException(
                    "La matière ne peut pas dépasser " + MATIERE_MAX_LENGTH + " caractères.");

        // Niveau
        if (niveau == null || niveau.trim().isEmpty())
            throw new IllegalArgumentException("Le niveau est obligatoire.");
        if (!NIVEAUX_VALIDES.contains(niveau))
            throw new IllegalArgumentException(
                    "Niveau invalide. Valeurs acceptées : " + NIVEAUX_VALIDES);

        // Description (optionnelle)
        if (description != null && description.trim().length() > DESC_MAX_LENGTH)
            throw new IllegalArgumentException(
                    "La description ne peut pas dépasser " + DESC_MAX_LENGTH + " caractères.");

        // Image (optionnelle)
        if (image != null && !image.trim().isEmpty())
            validateFile(image.trim(), IMAGE_EXTENSIONS, "image");

        // PDF (optionnel)
        if (pdf != null && !pdf.trim().isEmpty())
            validateFile(pdf.trim(), PDF_EXTENSIONS, "PDF");
    }

    // ── Validation fichier ────────────────────────────────────────────────
    private void validateFile(String path, List<String> allowedExts, String type) {
        String name = new File(path).getName().toLowerCase();
        boolean extOk = allowedExts.stream().anyMatch(ext -> name.endsWith("." + ext));
        if (!extOk)
            throw new IllegalArgumentException(
                    "Le fichier " + type + " doit avoir l'une de ces extensions : " + allowedExts);
        File file = new File(path);
        if (file.exists() && file.length() > FILE_MAX_BYTES)
            throw new IllegalArgumentException(
                    "Le fichier " + type + " dépasse la taille maximale de " + FILE_MAX_SIZE_MB + " Mo.");
    }

    @Override
    public String toString() { return titre; }
}