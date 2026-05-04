package com.studyflow.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ═══════════════════════════════════════════════════
 *  FraudEvent — Modèle d'un événement suspect
 * ═══════════════════════════════════════════════════
 */
public class FraudEvent {

    public enum Type {
        FOCUS_LOST        ("🔴 Focus Lost",        3),
        FULLSCREEN_EXIT   ("🔴 Fullscreen Exit",    4),
        INACTIVITY        ("🟡 Inactivity",          2),
        FAST_ANSWER       ("🟡 Fast Answer",         1),
        COPY_PASTE        ("🟡 Copy/Paste Attempt",  1),
        RIGHT_CLICK       ("🟡 Right Click",         1),
        SUSPICIOUS_PATTERN("🔴 Suspicious Pattern",  3),
        MANUAL            ("⚪ Manual Flag",          0);

        public final String label;
        public final int    points;

        Type(String label, int points) {
            this.label  = label;
            this.points = points;
        }
    }

    private final Type          type;
    private final String        details;
    private final int           fraudScoreAtTime;
    private final LocalDateTime timestamp;

    public FraudEvent(Type type, String details, int fraudScoreAtTime) {
        this.type             = type;
        this.details          = details;
        this.fraudScoreAtTime = fraudScoreAtTime;
        this.timestamp        = LocalDateTime.now();
    }

    public Type          getType()             { return type; }
    public String        getDetails()          { return details; }
    public int           getFraudScoreAtTime() { return fraudScoreAtTime; }
    public LocalDateTime getTimestamp()        { return timestamp; }

    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public boolean isCritical() {
        return type == Type.FOCUS_LOST
                || type == Type.FULLSCREEN_EXIT
                || type == Type.SUSPICIOUS_PATTERN;
    }

    public String getSeverityColor() {
        return switch (type) {
            case FOCUS_LOST, FULLSCREEN_EXIT, SUSPICIOUS_PATTERN -> "#EF4444";
            case INACTIVITY, FAST_ANSWER, COPY_PASTE, RIGHT_CLICK -> "#F59E0B";
            default -> "#64748B";
        };
    }

    @Override
    public String toString() {
        return String.format("[%s] %s — %s (fraud score: %d)",
                getFormattedTimestamp(), type.label, details, fraudScoreAtTime);
    }
}