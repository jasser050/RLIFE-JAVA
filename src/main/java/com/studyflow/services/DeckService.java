package com.studyflow.services;

import com.studyflow.interfaces.IService;
import com.studyflow.models.Deck;
import com.studyflow.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DeckService implements IService<Deck> {

    // ── Use the singleton connection ──────────────────────────────────────
    private Connection getConnection() {
        return MyDataBase.getInstance().getConnection();
    }

    // ── ADD ───────────────────────────────────────────────────────────────
    @Override
    public void add(Deck deck) {
        // 1. Validation métier (règles de saisie)
        deck.validate();
        // 2. Doublon titre
        if (existsByTitre(deck.getTitre(), -1))
            throw new IllegalArgumentException(
                    "Un deck avec le titre \"" + deck.getTitre() + "\" existe déjà.");

        String sql = "INSERT INTO deck (user_id, titre, matiere, niveau, description, image, pdf, date_creation) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, deck.getUserId());
            ps.setString(2, deck.getTitre());
            ps.setString(3, deck.getMatiere());
            ps.setString(4, deck.getNiveau());
            // NULL-safe: store empty string as NULL to respect DB constraints
            ps.setString(5, deck.getDescription() == null || deck.getDescription().isEmpty() ? null : deck.getDescription());
            ps.setString(6, deck.getImage()       == null || deck.getImage().isEmpty()       ? null : deck.getImage());
            ps.setString(7, deck.getPdf()         == null || deck.getPdf().isEmpty()         ? null : deck.getPdf());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) deck.setIdDeck(rs.getInt(1));
            }
            System.out.println("✅ Deck added: " + deck.getTitre() + " (id=" + deck.getIdDeck() + ")");
        } catch (SQLException e) {
            System.err.println("❌ Error adding deck: " + e.getMessage());
            e.printStackTrace();
            // Re-throw so the controller can show the real error to the user
            throw new RuntimeException("SQL error: " + e.getMessage(), e);
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────
    @Override
    public void update(Deck deck) {
        // 1. Validation métier
        deck.validate();
        // 2. Doublon titre (exclure le deck lui-même)
        if (existsByTitre(deck.getTitre(), deck.getIdDeck()))
            throw new IllegalArgumentException(
                    "Un autre deck avec le titre \"" + deck.getTitre() + "\" existe déjà.");

        String sql = "UPDATE deck SET user_id=?, titre=?, matiere=?, niveau=?, description=?, image=?, pdf=? "
                + "WHERE id_deck=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt   (1, deck.getUserId());
            ps.setString(2, deck.getTitre());
            ps.setString(3, deck.getMatiere());
            ps.setString(4, deck.getNiveau());
            ps.setString(5, deck.getDescription());
            ps.setString(6, deck.getImage());
            ps.setString(7, deck.getPdf());
            ps.setInt   (8, deck.getIdDeck());
            int rows = ps.executeUpdate();
            System.out.println("✅ Deck updated: " + rows + " row(s) affected.");
        } catch (SQLException e) {
            System.err.println("❌ Error updating deck: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("SQL error: " + e.getMessage(), e);
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────
    @Override
    public void delete(Deck deck) {
        String sql = "DELETE FROM deck WHERE id_deck=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, deck.getIdDeck());
            int rows = ps.executeUpdate();
            System.out.println("✅ Deck deleted: " + rows + " row(s) affected.");
        } catch (SQLException e) {
            System.err.println("❌ Error deleting deck: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── GET ALL ───────────────────────────────────────────────────────────
    @Override
    public List<Deck> getAll() {
        List<Deck> list = new ArrayList<>();
        String sql = "SELECT * FROM deck ORDER BY date_creation DESC";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("❌ Error fetching decks: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // ── SEARCH ────────────────────────────────────────────────────────────
    public List<Deck> search(String keyword) {
        List<Deck> list = new ArrayList<>();
        String sql = "SELECT * FROM deck WHERE titre LIKE ? OR matiere LIKE ? OR niveau LIKE ? "
                + "ORDER BY date_creation DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            ps.setString(3, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error searching decks: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // ── DOUBLON TITRE ─────────────────────────────────────────────────────
    /**
     * @param excludeId  id du deck à exclure (pour update) ; passer -1 pour add
     */
    private boolean existsByTitre(String titre, int excludeId) {
        String sql = "SELECT COUNT(*) FROM deck WHERE LOWER(titre) = LOWER(?) AND id_deck != ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, titre);
            ps.setInt   (2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error checking duplicate: " + e.getMessage());
            return false;
        }
    }

    // ── MAPPING ───────────────────────────────────────────────────────────
    private Deck mapRow(ResultSet rs) throws SQLException {
        Deck d = new Deck();
        d.setIdDeck     (rs.getInt      ("id_deck"));
        d.setUserId     (rs.getInt      ("user_id"));
        d.setTitre      (rs.getString   ("titre"));
        d.setMatiere    (rs.getString   ("matiere"));
        d.setNiveau     (rs.getString   ("niveau"));
        d.setDescription(rs.getString   ("description"));
        d.setImage      (rs.getString   ("image"));
        d.setPdf        (rs.getString   ("pdf"));
        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) d.setDateCreation(ts.toLocalDateTime());
        return d;
    }
}