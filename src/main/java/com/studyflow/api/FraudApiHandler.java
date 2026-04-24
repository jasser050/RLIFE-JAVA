package com.studyflow.api;

import com.studyflow.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * ═══════════════════════════════════════════════════════════════
 *  FraudApiHandler — Logique métier des endpoints anti-fraude
 * ═══════════════════════════════════════════════════════════════
 */
public class FraudApiHandler {

    private static final int TERMINATE_THRESHOLD = 15;

    // Raccourci pour obtenir la connexion via MyDataBase
    private Connection getConn() {
        return MyDataBase.getInstance().getConnection();
    }

    // ═══════════════════════════════════════════════════════════
    //  START SESSION
    // ═══════════════════════════════════════════════════════════

    public String startSession(String sessionId) {
        String sql = "INSERT INTO fraud_sessions " +
                "(session_id, fraud_score, focus_lost_count, fullscreen_exits, " +
                " penalty_count, terminated, raw_score, adjusted_score, started_at) " +
                "VALUES (?, 0, 0, 0, 0, FALSE, 0, 0, NOW()) " +
                "ON DUPLICATE KEY UPDATE started_at = NOW(), terminated = FALSE";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.executeUpdate();
            return json("status", "started", "sessionId", sessionId);
        } catch (Exception e) {
            System.err.println("[FraudAPI] startSession error: " + e.getMessage());
            return jsonError("Cannot create session: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  LOG EVENT
    // ═══════════════════════════════════════════════════════════

    public String logEvent(String sessionId, String eventType,
                           String details, int fraudScore, String severity) {

        String insertEvent = "INSERT INTO fraud_events " +
                "(session_id, event_type, details, fraud_score, severity, created_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW())";

        String updateSession = "UPDATE fraud_sessions SET " +
                "fraud_score = ?, " +
                "terminated  = ?, " +
                "focus_lost_count  = focus_lost_count  + ?, " +
                "fullscreen_exits  = fullscreen_exits  + ? " +
                "WHERE session_id = ?";

        boolean terminate = fraudScore >= TERMINATE_THRESHOLD;

        try {
            Connection con = getConn();
            con.setAutoCommit(false);

            try (PreparedStatement psEvent = con.prepareStatement(insertEvent)) {
                psEvent.setString(1, sessionId);
                psEvent.setString(2, eventType);
                psEvent.setString(3, details);
                psEvent.setInt(4, fraudScore);
                psEvent.setString(5, severity);
                psEvent.executeUpdate();
            }

            int focusDelta      = "FOCUS_LOST".equals(eventType)     ? 1 : 0;
            int fullscreenDelta = "FULLSCREEN_EXIT".equals(eventType) ? 1 : 0;

            try (PreparedStatement psUpdate = con.prepareStatement(updateSession)) {
                psUpdate.setInt(1, fraudScore);
                psUpdate.setBoolean(2, terminate);
                psUpdate.setInt(3, focusDelta);
                psUpdate.setInt(4, fullscreenDelta);
                psUpdate.setString(5, sessionId);
                psUpdate.executeUpdate();
            }

            con.commit();
            con.setAutoCommit(true); // remettre pour les autres requêtes du projet

            return "{\"decision\":\"" + (terminate ? "TERMINATE" : "CONTINUE") + "\"," +
                    "\"fraudScore\":" + fraudScore + "}";

        } catch (Exception e) {
            System.err.println("[FraudAPI] logEvent error: " + e.getMessage());
            // En cas d'erreur BDD → ne pas bloquer le quiz
            return "{\"decision\":\"CONTINUE\",\"fraudScore\":" + fraudScore + "}";
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  END SESSION
    // ═══════════════════════════════════════════════════════════

    public String endSession(String sessionId, double rawScore, int penaltyCount) {
        double adjustedScore = Math.max(0, rawScore - penaltyCount);

        String sql = "UPDATE fraud_sessions SET " +
                "ended_at       = NOW(), " +
                "penalty_count  = ?, " +
                "raw_score      = ?, " +
                "adjusted_score = ? " +
                "WHERE session_id = ?";

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, penaltyCount);
            ps.setDouble(2, rawScore);
            ps.setDouble(3, adjustedScore);
            ps.setString(4, sessionId);
            ps.executeUpdate();
            return "{\"adjustedScore\":" + adjustedScore +
                    ",\"sessionId\":\"" + sessionId + "\"}";
        } catch (Exception e) {
            System.err.println("[FraudAPI] endSession error: " + e.getMessage());
            return "{\"adjustedScore\":" + adjustedScore + "}";
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  GET SESSION REPORT
    // ═══════════════════════════════════════════════════════════

    public String getSession(String sessionId) {
        String sqlSession = "SELECT * FROM fraud_sessions WHERE session_id = ?";
        String sqlEvents  = "SELECT * FROM fraud_events  WHERE session_id = ? ORDER BY created_at";

        try {
            Connection con = getConn();
            StringBuilder sb = new StringBuilder();

            // Session
            try (PreparedStatement ps = con.prepareStatement(sqlSession)) {
                ps.setString(1, sessionId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return jsonError("Session not found");

                sb.append("{\"session\":{");
                sb.append("\"sessionId\":\"").append(rs.getString("session_id")).append("\",");
                sb.append("\"fraudScore\":").append(rs.getInt("fraud_score")).append(",");
                sb.append("\"focusLostCount\":").append(rs.getInt("focus_lost_count")).append(",");
                sb.append("\"fullscreenExits\":").append(rs.getInt("fullscreen_exits")).append(",");
                sb.append("\"penaltyCount\":").append(rs.getInt("penalty_count")).append(",");
                sb.append("\"terminated\":").append(rs.getBoolean("terminated")).append(",");
                sb.append("\"rawScore\":").append(rs.getDouble("raw_score")).append(",");
                sb.append("\"adjustedScore\":").append(rs.getDouble("adjusted_score")).append(",");
                sb.append("\"startedAt\":\"").append(rs.getString("started_at")).append("\",");
                sb.append("\"endedAt\":\"").append(rs.getString("ended_at")).append("\"");
                sb.append("},\"events\":[");
            }

            // Events
            try (PreparedStatement ps = con.prepareStatement(sqlEvents)) {
                ps.setString(1, sessionId);
                ResultSet rs = ps.executeQuery();
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    sb.append("{");
                    sb.append("\"id\":").append(rs.getInt("id")).append(",");
                    sb.append("\"eventType\":\"").append(rs.getString("event_type")).append("\",");
                    sb.append("\"details\":\"").append(escape(rs.getString("details"))).append("\",");
                    sb.append("\"fraudScore\":").append(rs.getInt("fraud_score")).append(",");
                    sb.append("\"severity\":\"").append(rs.getString("severity")).append("\",");
                    sb.append("\"createdAt\":\"").append(rs.getString("created_at")).append("\"");
                    sb.append("}");
                    first = false;
                }
            }

            sb.append("]}");
            return sb.toString();

        } catch (Exception e) {
            return jsonError("Error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  HEALTH CHECK
    // ═══════════════════════════════════════════════════════════

    public String health() {
        try {
            Connection con = getConn();
            boolean dbOk = con != null && !con.isClosed();
            return "{\"status\":\"ok\"," +
                    "\"db\":\"" + (dbOk ? "connected" : "disconnected") + "\"," +
                    "\"timestamp\":\"" + LocalDateTime.now() + "\"," +
                    "\"service\":\"AntiFraud API\"}";
        } catch (Exception e) {
            return "{\"status\":\"ok\",\"db\":\"unknown\"," +
                    "\"timestamp\":\"" + LocalDateTime.now() + "\"," +
                    "\"service\":\"AntiFraud API\"}";
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  UTILS
    // ═══════════════════════════════════════════════════════════

    private String json(String k1, String v1, String k2, String v2) {
        return "{\"" + k1 + "\":\"" + v1 + "\",\"" + k2 + "\":\"" + v2 + "\"}";
    }

    private String jsonError(String msg) {
        return "{\"error\":\"" + escape(msg) + "\"}";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}