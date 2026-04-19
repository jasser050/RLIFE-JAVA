package com.studyflow.models;

import java.time.LocalDate;

public class EvaluationMatiere {


    private int       idEval;
    private int       userId;
    private double    scoreEval;
    private double    noteMaximaleEval;
    private LocalDate dateEvaluation;
    private int       dureeEvaluation;
    private String    prioriteE;
    private int       matiereId;

    // Attributs supplémentaires gérés côté application
    private String    typeEvaluation;
    private String    commentaire;
    private String    performance;

    public EvaluationMatiere() {
        this.dateEvaluation = LocalDate.now();
    }

    public EvaluationMatiere(int idEval, int userId, double scoreEval, double noteMaximaleEval, LocalDate dateEvaluation,
                             int dureeEvaluation, String prioriteE, int matiereId) {
        this.idEval = idEval;
        this.userId = userId;
        this.scoreEval = scoreEval;
        this.noteMaximaleEval = noteMaximaleEval;
        this.dateEvaluation = dateEvaluation;
        this.dureeEvaluation = dureeEvaluation;
        this.prioriteE = prioriteE;
        this.matiereId = matiereId;
        updatePerformance();
    }

    // Getters & Setters

    public int getIdEval() { return idEval; }
    public void setIdEval(int idEval) { this.idEval = idEval; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public double getScoreEval() { return scoreEval; }
    public void setScoreEval(double scoreEval) { this.scoreEval = scoreEval; updatePerformance(); }

    public double getNoteMaximaleEval() { return noteMaximaleEval; }
    public void setNoteMaximaleEval(double noteMaximaleEval) { this.noteMaximaleEval = noteMaximaleEval; }

    public LocalDate getDateEvaluation() { return dateEvaluation; }
    public void setDateEvaluation(LocalDate dateEvaluation) { this.dateEvaluation = dateEvaluation; }

    public int getDureeEvaluation() { return dureeEvaluation; }
    public void setDureeEvaluation(int dureeEvaluation) { this.dureeEvaluation = dureeEvaluation; }

    public String getPrioriteE() { return prioriteE; }
    public void setPrioriteE(String prioriteE) { this.prioriteE = prioriteE; }

    public int getMatiereId() { return matiereId; }
    public void setMatiereId(int matiereId) { this.matiereId = matiereId; }

    public String getTypeEvaluation() { return typeEvaluation; }
    public void setTypeEvaluation(String typeEvaluation) { this.typeEvaluation = typeEvaluation; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public String getPerformance() { return performance; }

    // Méthode calcul performance selon scoreEval
    public void updatePerformance() {
        if (scoreEval < 8) {
            this.performance = "Faible";
        } else if (scoreEval < 13) {
            this.performance = "Moyenne";
        } else {
            this.performance = "Bonne";
        }
    }

    // Validation simple côté serveur
    public String valider() {
        if (scoreEval < 0 || scoreEval > 20)
            return "Le score doit être entre 0 et 20.";
        if (dateEvaluation == null)
            return "La date est obligatoire.";
        if (dateEvaluation.isAfter(LocalDate.now()))
            return "La date ne peut pas être dans le futur.";
        if (dureeEvaluation < 0)
            return "Le temps de révision doit être positif.";
        if (matiereId <= 0)
            return "Veuillez sélectionner une matière.";
        return null;
    }

    @Override
    public String toString() {
        return "Evaluation n°" + idEval + " - Score: " + scoreEval + "/20";
    }


}
