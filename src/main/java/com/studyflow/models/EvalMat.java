package com.studyflow.models;

public class EvalMat {
    private int id;
    private int evaluationId;
    private int matiereId;

    public EvalMat() {}

    public EvalMat(int evaluationId, int matiereId) {
        this.evaluationId = evaluationId;
        this.matiereId = matiereId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEvaluationId() { return evaluationId; }
    public void setEvaluationId(int evaluationId) { this.evaluationId = evaluationId; }

    public int getMatiereId() { return matiereId; }
    public void setMatiereId(int matiereId) { this.matiereId = matiereId; }
}
