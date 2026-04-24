package com.studyflow.services;

import com.studyflow.models.Matiere;
import com.studyflow.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MatiereService {

    private final Connection conn;

    public MatiereService() {
        this.conn = MyDataBase.getInstance().getConnection();
    }

    // ───────────────────────── CREATE ─────────────────────────
    public void create(Matiere m) {
        String sql = """
                INSERT INTO matiere
                (user_id, nom_matiere, coefficient_matiere, section_matiere,
                 type_matiere, heure_matiere, code, description)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, m.getUserId());
            ps.setString(2, m.getNomMatiere());
            ps.setDouble(3, m.getCoefficientMatiere());
            ps.setString(4, m.getSectionMatiere());
            ps.setString(5, m.getTypeMatiere());
            ps.setDouble(6, m.getHeureMatiere());
            ps.setString(7, m.getCode());
            ps.setString(8, m.getDescription());

            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Erreur CREATE: " + e.getMessage());
        }
    }

    // ───────────────────────── READ ─────────────────────────
    public List<Matiere> findAll() {
        return findAllSorted("nom_matiere", "ASC");
    }

    public List<Matiere> findAllSorted(String colonne, String ordre) {

        List<String> colonnesOk = Arrays.asList(
                "nom_matiere", "coefficient_matiere",
                "section_matiere", "type_matiere", "heure_matiere"
        );

        if (!colonnesOk.contains(colonne)) colonne = "nom_matiere";
        if (!"DESC".equalsIgnoreCase(ordre)) ordre = "ASC";

        return execQuery("SELECT * FROM matiere ORDER BY " + colonne + " " + ordre);
    }

    // ───────────────────────── SEARCH ─────────────────────────
    public List<Matiere> search(String terme) {
        String sql = """
                SELECT * FROM matiere
                WHERE nom_matiere LIKE ?
                   OR section_matiere LIKE ?
                   OR type_matiere LIKE ?
                   OR code LIKE ?
                ORDER BY nom_matiere ASC
                """;

        List<Matiere> liste = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            String p = "%" + terme + "%";

            ps.setString(1, p);
            ps.setString(2, p);
            ps.setString(3, p);
            ps.setString(4, p);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) liste.add(mapRow(rs));

        } catch (SQLException e) {
            System.out.println("Erreur SEARCH: " + e.getMessage());
        }

        return liste;
    }

    public Matiere findById(int id) {
        String sql = "SELECT * FROM matiere WHERE id_matiere = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) return mapRow(rs);

        } catch (SQLException e) {
            System.out.println("Erreur findById: " + e.getMessage());
        }

        return null;
    }

    // ───────────────────────── UPDATE ─────────────────────────
    public void update(Matiere m) {
        String sql = """
                UPDATE matiere SET
                    user_id=?, nom_matiere=?, coefficient_matiere=?, section_matiere=?,
                    type_matiere=?, heure_matiere=?, code=?, description=?, updated_at=NOW()
                WHERE id_matiere=?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, m.getUserId());
            ps.setString(2, m.getNomMatiere());
            ps.setDouble(3, m.getCoefficientMatiere());
            ps.setString(4, m.getSectionMatiere());
            ps.setString(5, m.getTypeMatiere());
            ps.setDouble(6, m.getHeureMatiere());
            ps.setString(7, m.getCode());
            ps.setString(8, m.getDescription());
            ps.setInt(9, m.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Erreur UPDATE: " + e.getMessage());
        }
    }

    // ───────────────────────── DELETE ─────────────────────────
    public void delete(int id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM matiere WHERE id_matiere = ?")) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Erreur DELETE: " + e.getMessage());
        }
    }

    // ───────────────────────── COUNT ─────────────────────────
    public int countTotal() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM matiere")) {

            return rs.next() ? rs.getInt(1) : 0;

        } catch (SQLException e) {
            return 0;
        }
    }

    // ───────────────────────── HELPERS ─────────────────────────
    private List<Matiere> execQuery(String sql) {
        List<Matiere> liste = new ArrayList<>();

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) liste.add(mapRow(rs));

        } catch (SQLException e) {
            System.out.println("Erreur execQuery: " + e.getMessage());
        }

        return liste;
    }

    private Matiere mapRow(ResultSet rs) throws SQLException {
        Matiere m = new Matiere();

        m.setId(rs.getInt("id_matiere"));
        m.setUserId(rs.getInt("user_id"));
        m.setNomMatiere(rs.getString("nom_matiere"));
        m.setCoefficientMatiere(rs.getDouble("coefficient_matiere"));
        m.setSectionMatiere(rs.getString("section_matiere"));
        m.setTypeMatiere(rs.getString("type_matiere"));
        m.setHeureMatiere(rs.getDouble("heure_matiere"));
        m.setCode(rs.getString("code"));
        m.setDescription(rs.getString("description"));
        m.setCreatedAt(rs.getTimestamp("created_at"));
        m.setUpdatedAt(rs.getTimestamp("updated_at"));

        return m;
    }
}