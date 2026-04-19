package com.studyflow.services;

import com.studyflow.models.EvaluationMatiere;
import com.studyflow.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EvaluationMatiereService {

    private final Connection conn;

    public EvaluationMatiereService() {
        this.conn = MyDataBase.getInstance().getConnection();
    }

    private static final Set<String> COLONNES_TRI = Set.of(
            "score_eval", "date_evaluation", "duree_evaluation", "priorite_e"
    );

    // ───────────────────────── CREATE ─────────────────────────
    public void create(EvaluationMatiere e, int matiereId) {
        String sql = """
                INSERT INTO evaluation_matiere
                    (user_id, score_eval, note_maximale_eval,
                     date_evaluation, duree_evaluation, priorite_e, matiere_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, e.getUserId());
            ps.setDouble(2, e.getScoreEval());
            ps.setDouble(3, e.getNoteMaximaleEval());
            ps.setDate(4, Date.valueOf(e.getDateEvaluation()));
            ps.setInt(5, e.getDureeEvaluation());
            ps.setString(6, e.getPrioriteE());
            ps.setInt(7, matiereId);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) e.setIdEval(keys.getInt(1));
            }

        } catch (SQLException ex) {
            System.out.println("Erreur CREATE: " + ex.getMessage());
        }
    }

    // ───────────────────────── READ ─────────────────────────
    public List<EvaluationMatiere> findAll() {
        return execQuery("SELECT * FROM evaluation_matiere ORDER BY date_evaluation DESC");
    }

    public List<EvaluationMatiere> findByUser(int userId) {
        String sql = """
                SELECT * FROM evaluation_matiere
                WHERE user_id = ?
                ORDER BY date_evaluation DESC
                """;

        List<EvaluationMatiere> liste = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) liste.add(mapRow(rs));

        } catch (SQLException ex) {
            System.out.println("Erreur findByUser: " + ex.getMessage());
        }

        return liste;
    }

    public EvaluationMatiere findById(int id) {
        String sql = "SELECT * FROM evaluation_matiere WHERE id_eval = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) return mapRow(rs);

        } catch (SQLException ex) {
            System.out.println("Erreur findById: " + ex.getMessage());
        }

        return null;
    }

    public List<EvaluationMatiere> findByMatiere(int matiereId) {
        String sql = """
                SELECT * FROM evaluation_matiere
                WHERE matiere_id = ?
                ORDER BY date_evaluation DESC
                """;

        List<EvaluationMatiere> liste = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, matiereId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) liste.add(mapRow(rs));

        } catch (SQLException ex) {
            System.out.println("Erreur findByMatiere: " + ex.getMessage());
        }

        return liste;
    }

    // ───────────────────────── SEARCH ─────────────────────────
    public List<EvaluationMatiere> search(String terme) {
        String sql = """
                SELECT * FROM evaluation_matiere
                WHERE priorite_e LIKE ?
                   OR CAST(score_eval AS CHAR) LIKE ?
                ORDER BY date_evaluation DESC
                """;

        List<EvaluationMatiere> liste = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String p = "%" + terme + "%";
            ps.setString(1, p);
            ps.setString(2, p);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) liste.add(mapRow(rs));

        } catch (SQLException ex) {
            System.out.println("Erreur search: " + ex.getMessage());
        }

        return liste;
    }

    // ───────────────────────── SORT ─────────────────────────
    public List<EvaluationMatiere> findAllSorted(String colonne, String ordre) {

        if (!COLONNES_TRI.contains(colonne)) {
            throw new IllegalArgumentException("Colonne non autorisée !");
        }

        String dir = "ASC".equalsIgnoreCase(ordre) ? "ASC" : "DESC";
        return execQuery("SELECT * FROM evaluation_matiere ORDER BY " + colonne + " " + dir);
    }

    // ───────────────────────── UPDATE ─────────────────────────
    public void update(EvaluationMatiere e) {
        String sql = """
                UPDATE evaluation_matiere
                SET score_eval = ?, note_maximale_eval = ?, date_evaluation = ?,
                    duree_evaluation = ?, priorite_e = ?, matiere_id = ?
                WHERE id_eval = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, e.getScoreEval());
            ps.setDouble(2, e.getNoteMaximaleEval());
            ps.setDate(3, Date.valueOf(e.getDateEvaluation()));
            ps.setInt(4, e.getDureeEvaluation());
            ps.setString(5, e.getPrioriteE());
            ps.setInt(6, e.getMatiereId());
            ps.setInt(7, e.getIdEval());

            ps.executeUpdate();

        } catch (SQLException ex) {
            System.out.println("Erreur UPDATE: " + ex.getMessage());
        }
    }

    // ───────────────────────── DELETE ─────────────────────────
    public void delete(int id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM evaluation_matiere WHERE id_eval = ?")) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException ex) {
            System.out.println("Erreur DELETE: " + ex.getMessage());
        }
    }

    // ───────────────────────── STATS ─────────────────────────
    public int countTotal() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM evaluation_matiere")) {

            return rs.next() ? rs.getInt(1) : 0;

        } catch (SQLException e) {
            return 0;
        }
    }

    public double getMoyenne() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT AVG(score_eval) FROM evaluation_matiere")) {

            return rs.next() ? Math.round(rs.getDouble(1) * 100.0) / 100.0 : 0.0;

        } catch (SQLException e) {
            return 0.0;
        }
    }

    public int countLacunes() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) FROM evaluation_matiere WHERE score_eval < 10")) {

            return rs.next() ? rs.getInt(1) : 0;

        } catch (SQLException e) {
            return 0;
        }
    }

    // ───────────────────────── HELPERS ─────────────────────────
    private List<EvaluationMatiere> execQuery(String sql) {
        List<EvaluationMatiere> liste = new ArrayList<>();

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) liste.add(mapRow(rs));

        } catch (SQLException e) {
            System.out.println("Erreur execQuery: " + e.getMessage());
        }

        return liste;
    }

    private EvaluationMatiere mapRow(ResultSet rs) throws SQLException {
        EvaluationMatiere e = new EvaluationMatiere();

        e.setIdEval(rs.getInt("id_eval"));
        e.setScoreEval(rs.getDouble("score_eval"));
        e.setNoteMaximaleEval(rs.getDouble("note_maximale_eval"));

        Date d = rs.getDate("date_evaluation");
        if (d != null) e.setDateEvaluation(d.toLocalDate());

        e.setDureeEvaluation(rs.getInt("duree_evaluation"));
        e.setPrioriteE(rs.getString("priorite_e"));
        e.setMatiereId(rs.getInt("matiere_id"));

        try { e.setUserId(rs.getInt("user_id")); }
        catch (SQLException ignored) {}

        e.updatePerformance();

        return e;

    }
}