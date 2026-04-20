package com.studyflow.services;

import com.studyflow.interfaces.IService;
import com.studyflow.models.Flashcard;
import com.studyflow.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FlashcardService implements IService<Flashcard> {

    private Connection getConnection() {
        return MyDataBase.getInstance().getConnection();
    }

    // ── ADD ───────────────────────────────────────────────────────────────
    @Override
    public void add(Flashcard fc) {
        String sql = "INSERT INTO flashcard (id_deck, titre, question, reponse, description, " +
                     "niveau_difficulte, etat, image, pdf, date_creation, created_by) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, fc.getIdDeck());
            ps.setString(2, fc.getTitre());
            ps.setString(3, fc.getQuestion());
            ps.setString(4, fc.getReponse());
            ps.setString(5, nullIfEmpty(fc.getDescription()));
            ps.setInt   (6, fc.getNiveauDifficulte());
            ps.setString(7, fc.getEtat() != null ? fc.getEtat() : "new");
            ps.setString(8, nullIfEmpty(fc.getImage()));
            ps.setString(9, nullIfEmpty(fc.getPdf()));
            if (fc.getCreatedBy() != null) ps.setInt(10, fc.getCreatedBy());
            else ps.setNull(10, Types.INTEGER);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) fc.setIdFlashcard(rs.getInt(1));
            }
            System.out.println("✅ Flashcard added: " + fc.getTitre() + " (id=" + fc.getIdFlashcard() + ")");
        } catch (SQLException e) {
            System.err.println("❌ Error adding flashcard: " + e.getMessage());
            throw new RuntimeException("SQL error: " + e.getMessage(), e);
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────
    @Override
    public void update(Flashcard fc) {
        String sql = "UPDATE flashcard SET titre=?, question=?, reponse=?, description=?, " +
                     "niveau_difficulte=?, etat=?, image=?, pdf=?, date_modification=NOW() " +
                     "WHERE id_flashcard=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, fc.getTitre());
            ps.setString(2, fc.getQuestion());
            ps.setString(3, fc.getReponse());
            ps.setString(4, nullIfEmpty(fc.getDescription()));
            ps.setInt   (5, fc.getNiveauDifficulte());
            ps.setString(6, fc.getEtat());
            ps.setString(7, nullIfEmpty(fc.getImage()));
            ps.setString(8, nullIfEmpty(fc.getPdf()));
            ps.setInt   (9, fc.getIdFlashcard());
            int rows = ps.executeUpdate();
            System.out.println("✅ Flashcard updated: " + rows + " row(s).");
        } catch (SQLException e) {
            System.err.println("❌ Error updating flashcard: " + e.getMessage());
            throw new RuntimeException("SQL error: " + e.getMessage(), e);
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────
    @Override
    public void delete(Flashcard fc) {
        String sql = "DELETE FROM flashcard WHERE id_flashcard=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, fc.getIdFlashcard());
            ps.executeUpdate();
            System.out.println("✅ Flashcard deleted: id=" + fc.getIdFlashcard());
        } catch (SQLException e) {
            System.err.println("❌ Error deleting flashcard: " + e.getMessage());
        }
    }

    // ── GET ALL ───────────────────────────────────────────────────────────
    @Override
    public List<Flashcard> getAll() {
        List<Flashcard> list = new ArrayList<>();
        String sql = "SELECT * FROM flashcard ORDER BY date_creation DESC";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("❌ Error fetching flashcards: " + e.getMessage());
        }
        return list;
    }

    // ── GET BY DECK ───────────────────────────────────────────────────────
    public List<Flashcard> getByDeck(int deckId) {
        List<Flashcard> list = new ArrayList<>();
        String sql = "SELECT * FROM flashcard WHERE id_deck=? ORDER BY date_creation DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error fetching flashcards by deck: " + e.getMessage());
        }
        return list;
    }

    // ── GET BY DECK + USER ────────────────────────────────────────────────
    public List<Flashcard> getByDeckAndUser(int deckId, int userId) {
        List<Flashcard> list = new ArrayList<>();
        String sql = "SELECT * FROM flashcard WHERE id_deck=? AND created_by=? ORDER BY date_creation DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, deckId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error fetching flashcards: " + e.getMessage());
        }
        return list;
    }

    // ── UPDATE ETAT ONLY ──────────────────────────────────────────────────
    public void updateEtat(int flashcardId, String etat) {
        String sql = "UPDATE flashcard SET etat=?, date_modification=NOW() WHERE id_flashcard=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, etat);
            ps.setInt   (2, flashcardId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Error updating etat: " + e.getMessage());
        }
    }

    // ── COUNT BY ETAT ─────────────────────────────────────────────────────
    public int countByEtat(int userId, String etat) {
        String sql = "SELECT COUNT(*) FROM flashcard WHERE created_by=? AND etat=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt   (1, userId);
            ps.setString(2, etat);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error counting flashcards: " + e.getMessage());
        }
        return 0;
    }

    // ── SEARCH ────────────────────────────────────────────────────────────
    public List<Flashcard> search(int deckId, String keyword) {
        List<Flashcard> list = new ArrayList<>();
        String sql = "SELECT * FROM flashcard WHERE id_deck=? " +
                     "AND (titre LIKE ? OR question LIKE ? OR reponse LIKE ?) " +
                     "ORDER BY date_creation DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            ps.setInt   (1, deckId);
            ps.setString(2, kw);
            ps.setString(3, kw);
            ps.setString(4, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error searching flashcards: " + e.getMessage());
        }
        return list;
    }

    // ── ROW MAPPER ────────────────────────────────────────────────────────
    private Flashcard mapRow(ResultSet rs) throws SQLException {
        Flashcard fc = new Flashcard();
        fc.setIdFlashcard    (rs.getInt      ("id_flashcard"));
        fc.setIdDeck         (rs.getInt      ("id_deck"));
        fc.setTitre          (rs.getString   ("titre"));
        fc.setQuestion       (rs.getString   ("question"));
        fc.setReponse        (rs.getString   ("reponse"));
        fc.setDescription    (rs.getString   ("description"));
        fc.setNiveauDifficulte(rs.getInt     ("niveau_difficulte"));
        fc.setEtat           (rs.getString   ("etat"));
        fc.setImage          (rs.getString   ("image"));
        fc.setPdf            (rs.getString   ("pdf"));
        Timestamp tc = rs.getTimestamp("date_creation");
        if (tc != null) fc.setDateCreation(tc.toLocalDateTime());
        Timestamp tm = rs.getTimestamp("date_modification");
        if (tm != null) fc.setDateModification(tm.toLocalDateTime());
        int cb = rs.getInt("created_by");
        if (!rs.wasNull()) fc.setCreatedBy(cb);
        return fc;
    }

    // ── UTIL ──────────────────────────────────────────────────────────────
    private String nullIfEmpty(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }
}
