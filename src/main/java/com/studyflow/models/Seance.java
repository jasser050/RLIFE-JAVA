package com.studyflow.models;

import java.sql.Timestamp;

public class Seance {
    private int id;
    private int userId;
    private String titre;
    private String typeSeance;
    private String description;
    private String partageAvec;
    private String statut;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Integer typeSeanceId;
    private Integer matiereId;
    private String typeSeanceName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getTypeSeance() {
        return typeSeance;
    }

    public void setTypeSeance(String typeSeance) {
        this.typeSeance = typeSeance;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPartageAvec() {
        return partageAvec;
    }

    public void setPartageAvec(String partageAvec) {
        this.partageAvec = partageAvec;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getTypeSeanceId() {
        return typeSeanceId;
    }

    public void setTypeSeanceId(Integer typeSeanceId) {
        this.typeSeanceId = typeSeanceId;
    }

    public Integer getMatiereId() {
        return matiereId;
    }

    public void setMatiereId(Integer matiereId) {
        this.matiereId = matiereId;
    }

    public String getTypeSeanceName() {
        return typeSeanceName;
    }

    public void setTypeSeanceName(String typeSeanceName) {
        this.typeSeanceName = typeSeanceName;
    }
}
