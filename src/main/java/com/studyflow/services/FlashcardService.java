package com.studyflow.services;

import com.studyflow.interfaces.IService;
import com.studyflow.models.Flashcard;
import com.studyflow.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class FlashcardService implements IService<Flashcard> {
    private volatile String deckForeignKeyColumn;
    private volatile String createdByColumn;

    private Connection getConnection() {
        return MyDataBase.getInstance().getConnection();
    }

    @Override
    public void add(Flashcard fc) {
        String deckColumn = getDeckForeignKeyColumn();
        String createdByColumn = getCreatedByColumn();
        boolean includeCreatedBy = createdByColumn != null;
        String sql = includeCreatedBy
                ? "INSERT INTO flashcard (" + deckColumn + ", titre, question, reponse, description, "
                + "niveau_difficulte, etat, image, pdf, date_creation, " + createdByColumn + ") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?)"
                : "INSERT INTO flashcard (" + deckColumn + ", titre, question, reponse, description, "
                + "niveau_difficulte, etat, image, pdf, date_creation) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, fc.getIdDeck());
            ps.setString(2, fc.getTitre());
            ps.setString(3, fc.getQuestion());
            ps.setString(4, fc.getReponse());
            ps.setString(5, nullIfEmpty(fc.getDescription()));
            ps.setInt(6, fc.getNiveauDifficulte());
            ps.setString(7, fc.getEtat() != null ? fc.getEtat() : "new");
            ps.setString(8, nullIfEmpty(fc.getImage()));
            ps.setString(9, nullIfEmpty(fc.getPdf()));
            if (includeCreatedBy) {
                if (fc.getCreatedBy() != null) {
                    ps.setInt(10, fc.getCreatedBy());
                } else {
                    ps.setNull(10, Types.INTEGER);
                }
            }

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    fc.setIdFlashcard(rs.getInt(1));
                }
            }
            System.out.println("Flashcard added: " + fc.getTitre() + " (id=" + fc.getIdFlashcard() + ")");
        } catch (SQLException e) {
            System.err.println("Error adding flashcard: " + e.getMessage());
            throw new RuntimeException("SQL error: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Flashcard fc) {
        String sql = "UPDATE flashcard SET titre=?, question=?, reponse=?, description=?, "
                + "niveau_difficulte=?, etat=?, image=?, pdf=?, date_modification=NOW() "
                + "WHERE id_flashcard=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, fc.getTitre());
            ps.setString(2, fc.getQuestion());
            ps.setString(3, fc.getReponse());
            ps.setString(4, nullIfEmpty(fc.getDescription()));
            ps.setInt(5, fc.getNiveauDifficulte());
            ps.setString(6, fc.getEtat());
            ps.setString(7, nullIfEmpty(fc.getImage()));
            ps.setString(8, nullIfEmpty(fc.getPdf()));
            ps.setInt(9, fc.getIdFlashcard());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating flashcard: " + e.getMessage());
            throw new RuntimeException("SQL error: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(Flashcard fc) {
        String sql = "DELETE FROM flashcard WHERE id_flashcard=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, fc.getIdFlashcard());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting flashcard: " + e.getMessage());
        }
    }

    @Override
    public List<Flashcard> getAll() {
        List<Flashcard> list = new ArrayList<>();
        String sql = "SELECT * FROM flashcard ORDER BY date_creation DESC";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching flashcards: " + e.getMessage());
        }
        return list;
    }

    public List<Flashcard> getByDeck(int deckId) {
        List<Flashcard> list = new ArrayList<>();
        String deckColumn = getDeckForeignKeyColumn();
        String sql = "SELECT * FROM flashcard WHERE " + deckColumn + "=? ORDER BY date_creation DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching flashcards by deck: " + e.getMessage());
        }
        return list;
    }

    public List<Flashcard> getByDeckAndUser(int deckId, int userId) {
        List<Flashcard> list = new ArrayList<>();
        String deckColumn = getDeckForeignKeyColumn();
        String createdByColumn = getCreatedByColumn();
        String sql = createdByColumn != null
                ? "SELECT * FROM flashcard WHERE " + deckColumn + "=? AND " + createdByColumn + "=? ORDER BY date_creation DESC"
                : "SELECT f.* FROM flashcard f JOIN deck d ON f." + deckColumn + " = d.id_deck "
                + "WHERE f." + deckColumn + "=? AND d.user_id=? ORDER BY f.date_creation DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, deckId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching flashcards: " + e.getMessage());
        }
        return list;
    }

    public void updateEtat(int flashcardId, String etat) {
        String sql = "UPDATE flashcard SET etat=?, date_modification=NOW() WHERE id_flashcard=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, etat);
            ps.setInt(2, flashcardId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating etat: " + e.getMessage());
        }
    }

    public int countByEtat(int userId, String etat) {
        String createdByColumn = getCreatedByColumn();
        String sql = createdByColumn != null
                ? "SELECT COUNT(*) FROM flashcard WHERE " + createdByColumn + "=? AND etat=?"
                : "SELECT COUNT(*) FROM flashcard f JOIN deck d ON f." + getDeckForeignKeyColumn() + " = d.id_deck "
                + "WHERE d.user_id=? AND f.etat=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, etat);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error counting flashcards: " + e.getMessage());
        }
        return 0;
    }

    public List<Flashcard> search(int deckId, String keyword) {
        List<Flashcard> list = new ArrayList<>();
        String deckColumn = getDeckForeignKeyColumn();
        String sql = "SELECT * FROM flashcard WHERE " + deckColumn + "=? "
                + "AND (titre LIKE ? OR question LIKE ? OR reponse LIKE ?) "
                + "ORDER BY date_creation DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            ps.setInt(1, deckId);
            ps.setString(2, kw);
            ps.setString(3, kw);
            ps.setString(4, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching flashcards: " + e.getMessage());
        }
        return list;
    }

    private Flashcard mapRow(ResultSet rs) throws SQLException {
        Flashcard fc = new Flashcard();
        fc.setIdFlashcard(rs.getInt("id_flashcard"));
        fc.setIdDeck(rs.getInt(getDeckForeignKeyColumn()));
        fc.setTitre(rs.getString("titre"));
        fc.setQuestion(rs.getString("question"));
        fc.setReponse(rs.getString("reponse"));
        fc.setDescription(rs.getString("description"));
        fc.setNiveauDifficulte(rs.getInt("niveau_difficulte"));
        fc.setEtat(rs.getString("etat"));
        fc.setImage(rs.getString("image"));
        fc.setPdf(rs.getString("pdf"));

        Timestamp createdAt = rs.getTimestamp("date_creation");
        if (createdAt != null) {
            fc.setDateCreation(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("date_modification");
        if (updatedAt != null) {
            fc.setDateModification(updatedAt.toLocalDateTime());
        }

        String createdByColumn = getCreatedByColumn();
        if (createdByColumn != null) {
            int createdBy = rs.getInt(createdByColumn);
            if (!rs.wasNull()) {
                fc.setCreatedBy(createdBy);
            }
        }
        return fc;
    }

    private String getDeckForeignKeyColumn() {
        String cached = deckForeignKeyColumn;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (deckForeignKeyColumn == null) {
                deckForeignKeyColumn = firstExistingColumn("flashcard", "id_deck", "deck_id", "id_deck_id");
                if (deckForeignKeyColumn == null) {
                    deckForeignKeyColumn = "id_deck";
                }
            }
            return deckForeignKeyColumn;
        }
    }

    private String getCreatedByColumn() {
        String cached = createdByColumn;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (createdByColumn == null) {
                createdByColumn = firstExistingColumn("flashcard", "created_by", "created_by_id");
            }
            return createdByColumn;
        }
    }

    private String firstExistingColumn(String table, String... candidates) {
        for (String candidate : candidates) {
            if (hasColumn(table, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean hasColumn(String table, String column) {
        String sql = "SELECT 1 FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ? LIMIT 1";
        try {
            Connection connection = getConnection();
            String schema = connection.getCatalog();
            if (schema != null && !schema.isBlank()) {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, schema);
                    ps.setString(2, table);
                    ps.setString(3, column);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return true;
                    }
                }
            }

            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM " + table + " LIMIT 0")) {
                return hasColumn(rs.getMetaData(), column);
            }
        } catch (SQLException e) {
            System.err.println("Error reading schema metadata: " + e.getMessage());
            return false;
        }
    }

    private boolean hasColumn(java.sql.ResultSetMetaData metaData, String expectedColumn) throws SQLException {
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String actualColumn = metaData.getColumnName(i);
            if (actualColumn != null && actualColumn.equalsIgnoreCase(expectedColumn)) {
                return true;
            }
        }
        return false;
    }

    private String nullIfEmpty(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
