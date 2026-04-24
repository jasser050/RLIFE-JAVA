package com.studyflow.models;



import java.util.Date;

public class Matiere {

    private int id; // id_matiere
    private int userId;
    private String nomMatiere;
    private double coefficientMatiere;
    private String sectionMatiere;
    private String typeMatiere;
    private double heureMatiere;
    private String code;
    private String description;
    private Date createdAt;
    private Date updatedAt;

    // Constructeur complet
    public Matiere(int id, int userId, String nomMatiere, double coefficientMatiere,
                   String sectionMatiere, String typeMatiere, double heureMatiere,
                   String code, String description, Date createdAt, Date updatedAt) {
        this.id = id;
        this.userId = userId;
        this.nomMatiere = nomMatiere;
        this.coefficientMatiere = coefficientMatiere;
        this.sectionMatiere = sectionMatiere;
        this.typeMatiere = typeMatiere;
        this.heureMatiere = heureMatiere;
        this.code = code;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Constructeur sans ID (insertion)
    public Matiere(int userId, String nomMatiere, double coefficientMatiere,
                   String sectionMatiere, String typeMatiere,
                   double heureMatiere, String code, String description) {
        this.userId = userId;
        this.nomMatiere = nomMatiere;
        this.coefficientMatiere = coefficientMatiere;
        this.sectionMatiere = sectionMatiere;
        this.typeMatiere = typeMatiere;
        this.heureMatiere = heureMatiere;
        this.code = code;
        this.description = description;
    }

    public Matiere() {}

    // Validation
    public String valider() {
        if (nomMatiere == null || nomMatiere.trim().isEmpty())
            return "Le nom de la matière est obligatoire.";

        if (coefficientMatiere <= 0)
            return "Le coefficient doit être supérieur à 0.";

        if (heureMatiere < 0)
            return "Le nombre d'heures doit être positif.";

        if (code == null || code.trim().isEmpty())
            return "Le code est obligatoire.";

        return null;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getNomMatiere() { return nomMatiere; }
    public void setNomMatiere(String nomMatiere) { this.nomMatiere = nomMatiere; }

    public double getCoefficientMatiere() { return coefficientMatiere; }
    public void setCoefficientMatiere(double coefficientMatiere) { this.coefficientMatiere = coefficientMatiere; }

    public String getSectionMatiere() { return sectionMatiere; }
    public void setSectionMatiere(String sectionMatiere) { this.sectionMatiere = sectionMatiere; }

    public String getTypeMatiere() { return typeMatiere; }
    public void setTypeMatiere(String typeMatiere) { this.typeMatiere = typeMatiere; }

    public double getHeureMatiere() { return heureMatiere; }
    public void setHeureMatiere(double heureMatiere) { this.heureMatiere = heureMatiere; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return nomMatiere != null ? nomMatiere : "";
    }
}
