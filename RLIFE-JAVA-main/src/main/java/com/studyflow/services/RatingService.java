package com.studyflow.services;

import com.studyflow.models.Rating;
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

public class RatingService {

    private Connection getConn() throws SQLException {
        return MyDataBase.getInstance().getConnection();
    }

    public void addRating(Rating rating) {
        String sql = """
            INSERT INTO rating
              (user_id, deck_id, stars, comment, tags, clarity,
               completeness, difficulty, usefulness, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """;
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillRatingStatement(ps, rating);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    rating.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("[RatingService] addRating error: " + e.getMessage());
        }
    }

    public void updateRating(Rating rating) {
        String sql = """
            UPDATE rating
               SET stars=?, comment=?, tags=?, clarity=?,
                   completeness=?, difficulty=?, usefulness=?,
                   updated_at=NOW()
             WHERE user_id=? AND deck_id=?
            """;
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, rating.getStars());
            setNullableString(ps, 2, rating.getComment());
            setNullableString(ps, 3, rating.getTags());
            setNullableInt(ps, 4, rating.getClarity());
            setNullableInt(ps, 5, rating.getCompleteness());
            setNullableInt(ps, 6, rating.getDifficulty());
            setNullableInt(ps, 7, rating.getUsefulness());
            ps.setInt(8, rating.getUserId());
            ps.setInt(9, rating.getDeckId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[RatingService] updateRating error: " + e.getMessage());
        }
    }

    public void upsertRating(Rating rating) {
        Rating existing = getRatingByUserAndDeck(rating.getUserId(), rating.getDeckId());
        if (existing == null) {
            addRating(rating);
        } else {
            rating.setId(existing.getId());
            updateRating(rating);
        }
    }

    public Rating getRatingByUserAndDeck(int userId, int deckId) {
        String sql = "SELECT * FROM rating WHERE user_id=? AND deck_id=?";
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[RatingService] getRatingByUserAndDeck error: " + e.getMessage());
        }
        return null;
    }

    public List<Rating> getAllRatingsByUser(int userId) {
        String sql = """
            SELECT r.*, d.titre AS deck_name, d.matiere AS deck_subject
              FROM rating r
              JOIN deck d ON r.deck_id = d.id_deck
             WHERE r.user_id = ?
             ORDER BY COALESCE(r.updated_at, r.created_at) DESC
            """;
        return fetchRatingsWithDeckInfo(sql, userId);
    }

    public List<Rating> getWeakRatingsByUser(int userId) {
        String sql = """
            SELECT r.*, d.titre AS deck_name, d.matiere AS deck_subject
              FROM rating r
              JOIN deck d ON r.deck_id = d.id_deck
             WHERE r.user_id = ? AND r.stars <= 3
             ORDER BY r.stars ASC, COALESCE(r.updated_at, r.created_at) DESC
            """;
        return fetchRatingsWithDeckInfo(sql, userId);
    }

    public List<Rating> getAllRatingsForAdmin() {
        String sql = """
            SELECT r.*,
                   d.titre AS deck_name,
                   d.matiere AS deck_subject,
                   COALESCE(NULLIF(TRIM(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, ''))), ''),
                            NULLIF(u.username, ''),
                            NULLIF(u.email, ''),
                            CONCAT('User #', u.id)) AS user_name,
                   u.email AS user_email,
                   u.username AS username
              FROM `rating` r
              JOIN `deck` d ON r.deck_id = d.id_deck
              JOIN `user` u ON r.user_id = u.id
             ORDER BY COALESCE(r.updated_at, r.created_at) DESC, r.id DESC
            """;

        List<Rating> list = new ArrayList<>();
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Rating rating = mapRow(rs);
                attachDeckInfo(rs, rating);
                attachUserInfo(rs, rating);
                list.add(rating);
            }
        } catch (SQLException e) {
            System.err.println("[RatingService] getAllRatingsForAdmin error: " + e.getMessage());
        }
        return list;
    }

    public List<Rating> getRatingsForDeckAdmin(int deckId) {
        String sql = """
            SELECT r.*,
                   d.titre AS deck_name,
                   d.matiere AS deck_subject,
                   COALESCE(NULLIF(TRIM(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, ''))), ''),
                            NULLIF(u.username, ''),
                            NULLIF(u.email, ''),
                            CONCAT('User #', u.id)) AS user_name,
                   u.email AS user_email,
                   u.username AS username
              FROM `rating` r
              JOIN `deck` d ON r.deck_id = d.id_deck
              JOIN `user` u ON r.user_id = u.id
             WHERE r.deck_id = ?
             ORDER BY COALESCE(r.updated_at, r.created_at) DESC, r.id DESC
            """;

        List<Rating> list = new ArrayList<>();
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Rating rating = mapRow(rs);
                    attachDeckInfo(rs, rating);
                    attachUserInfo(rs, rating);
                    list.add(rating);
                }
            }
        } catch (SQLException e) {
            System.err.println("[RatingService] getRatingsForDeckAdmin error: " + e.getMessage());
        }
        return list;
    }

    public double getAverageStarsForDeck(int deckId) {
        String sql = "SELECT AVG(stars) FROM rating WHERE deck_id=?";
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[RatingService] getAverageStarsForDeck error: " + e.getMessage());
        }
        return 0.0;
    }

    public int getRatingCountForDeck(int deckId) {
        String sql = "SELECT COUNT(*) FROM rating WHERE deck_id=?";
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, deckId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[RatingService] getRatingCountForDeck error: " + e.getMessage());
        }
        return 0;
    }

    public void deleteRating(int userId, int deckId) {
        String sql = "DELETE FROM rating WHERE user_id=? AND deck_id=?";
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, deckId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[RatingService] deleteRating error: " + e.getMessage());
        }
    }

    private List<Rating> fetchRatingsWithDeckInfo(String sql, int userId) {
        List<Rating> list = new ArrayList<>();
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Rating rating = mapRow(rs);
                    attachDeckInfo(rs, rating);
                    attachUserInfo(rs, rating);
                    list.add(rating);
                }
            }
        } catch (SQLException e) {
            System.err.println("[RatingService] fetchRatings error: " + e.getMessage());
        }
        return list;
    }

    private Rating mapRow(ResultSet rs) throws SQLException {
        Rating rating = new Rating();
        rating.setId(rs.getInt("id"));
        rating.setUserId(rs.getInt("user_id"));
        rating.setDeckId(rs.getInt("deck_id"));
        rating.setStars(rs.getInt("stars"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            rating.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            rating.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        rating.setComment(rs.getString("comment"));
        rating.setTags(rs.getString("tags"));

        int clarity = rs.getInt("clarity");
        if (!rs.wasNull()) {
            rating.setClarity(clarity);
        }

        int completeness = rs.getInt("completeness");
        if (!rs.wasNull()) {
            rating.setCompleteness(completeness);
        }

        int difficulty = rs.getInt("difficulty");
        if (!rs.wasNull()) {
            rating.setDifficulty(difficulty);
        }

        int usefulness = rs.getInt("usefulness");
        if (!rs.wasNull()) {
            rating.setUsefulness(usefulness);
        }

        return rating;
    }

    private void attachDeckInfo(ResultSet rs, Rating rating) {
        try {
            rating.setDeckName(rs.getString("deck_name"));
        } catch (SQLException ignored) {
        }
        try {
            rating.setDeckSubject(rs.getString("deck_subject"));
        } catch (SQLException ignored) {
        }
    }

    private void attachUserInfo(ResultSet rs, Rating rating) {
        try {
            rating.setUserName(rs.getString("user_name"));
        } catch (SQLException ignored) {
        }
        try {
            rating.setUserEmail(rs.getString("user_email"));
        } catch (SQLException ignored) {
        }
        try {
            rating.setUsername(rs.getString("username"));
        } catch (SQLException ignored) {
        }
    }

    private void fillRatingStatement(PreparedStatement ps, Rating rating) throws SQLException {
        ps.setInt(1, rating.getUserId());
        ps.setInt(2, rating.getDeckId());
        ps.setInt(3, rating.getStars());
        setNullableString(ps, 4, rating.getComment());
        setNullableString(ps, 5, rating.getTags());
        setNullableInt(ps, 6, rating.getClarity());
        setNullableInt(ps, 7, rating.getCompleteness());
        setNullableInt(ps, 8, rating.getDifficulty());
        setNullableInt(ps, 9, rating.getUsefulness());
    }

    private void setNullableString(PreparedStatement ps, int idx, String value) throws SQLException {
        if (value != null && !value.isBlank()) {
            ps.setString(idx, value);
        } else {
            ps.setNull(idx, Types.LONGVARCHAR);
        }
    }

    private void setNullableInt(PreparedStatement ps, int idx, Integer value) throws SQLException {
        if (value != null) {
            ps.setInt(idx, value);
        } else {
            ps.setNull(idx, Types.TINYINT);
        }
    }
}
