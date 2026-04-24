package com.studyflow.models;

import java.time.LocalDateTime;

/**
 * Représente une session de quiz dans la BDD anti-fraude.
 * Table : fraud_sessions
 *
 * SQL de création :
 * CREATE TABLE fraud_sessions (
 *   session_id          VARCHAR(36) PRIMARY KEY,
 *   fraud_score         INT DEFAULT 0,
 *   focus_lost_count    INT DEFAULT 0,
 *   fullscreen_exits    INT DEFAULT 0,
 *   penalty_count       INT DEFAULT 0,
 *   terminated          BOOLEAN DEFAULT FALSE,
 *   raw_score           DOUBLE DEFAULT 0,
 *   adjusted_score      DOUBLE DEFAULT 0,
 *   started_at          DATETIME,
 *   ended_at            DATETIME
 * );
 *
 * CREATE TABLE fraud_events (
 *   id               INT AUTO_INCREMENT PRIMARY KEY,
 *   session_id       VARCHAR(36),
 *   event_type       VARCHAR(50),
 *   details          TEXT,
 *   fraud_score      INT,
 *   severity         VARCHAR(20),
 *   created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
 *   FOREIGN KEY (session_id) REFERENCES fraud_sessions(session_id)
 * );
 */
public class FraudSessionRecord {

    private String        sessionId;
    private int           fraudScore;
    private int           focusLostCount;
    private int           fullscreenExits;
    private int           penaltyCount;
    private boolean       terminated;
    private double        rawScore;
    private double        adjustedScore;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    public FraudSessionRecord() {}

    public FraudSessionRecord(String sessionId) {
        this.sessionId  = sessionId;
        this.startedAt  = LocalDateTime.now();
        this.terminated = false;
    }

    // ── Getters / Setters ────────────────────────────────────────
    public String        getSessionId()       { return sessionId; }
    public void          setSessionId(String v){ this.sessionId = v; }

    public int           getFraudScore()      { return fraudScore; }
    public void          setFraudScore(int v) { this.fraudScore = v; }

    public int           getFocusLostCount()       { return focusLostCount; }
    public void          setFocusLostCount(int v)  { this.focusLostCount = v; }

    public int           getFullscreenExits()      { return fullscreenExits; }
    public void          setFullscreenExits(int v) { this.fullscreenExits = v; }

    public int           getPenaltyCount()    { return penaltyCount; }
    public void          setPenaltyCount(int v){ this.penaltyCount = v; }

    public boolean       isTerminated()       { return terminated; }
    public void          setTerminated(boolean v){ this.terminated = v; }

    public double        getRawScore()        { return rawScore; }
    public void          setRawScore(double v){ this.rawScore = v; }

    public double        getAdjustedScore()        { return adjustedScore; }
    public void          setAdjustedScore(double v){ this.adjustedScore = v; }

    public LocalDateTime getStartedAt()       { return startedAt; }
    public void          setStartedAt(LocalDateTime v){ this.startedAt = v; }

    public LocalDateTime getEndedAt()         { return endedAt; }
    public void          setEndedAt(LocalDateTime v)  { this.endedAt = v; }

    @Override
    public String toString() {
        return String.format(
                "FraudSession[id=%s, score=%d, penalties=%d, terminated=%b]",
                sessionId, fraudScore, penaltyCount, terminated
        );
    }
}