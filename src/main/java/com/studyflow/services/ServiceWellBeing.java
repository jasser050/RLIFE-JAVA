package com.studyflow.services;

import com.studyflow.interfaces.IService;
import com.studyflow.models.WellBeing;
import com.studyflow.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceWellBeing implements IService<WellBeing> {

    private final Connection cnx;

    public ServiceWellBeing() {
        this.cnx = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void add(WellBeing wellBeing) {
        if (wellBeing.getUserId() == null) {
            throw new RuntimeException("User is required for wellbeing check-in.");
        }
        String sql = """
                INSERT INTO well_being
                (entry_date_well, mood_well, stress_level_well, energy_level_well, sleep_hours_well, note_well, created_at_well, user_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement pstm = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setTimestamp(1, Timestamp.valueOf(wellBeing.getEntryDate()));
            pstm.setString(2, wellBeing.getMood());
            pstm.setInt(3, wellBeing.getStressLevel());
            pstm.setInt(4, wellBeing.getEnergyLevel());
            pstm.setDouble(5, wellBeing.getSleepHours());
            pstm.setString(6, wellBeing.getNote());
            pstm.setTimestamp(7, Timestamp.valueOf(wellBeing.getCreatedAt()));
            if (wellBeing.getUserId() != null) {
                pstm.setInt(8, wellBeing.getUserId());
            } else {
                pstm.setNull(8, java.sql.Types.INTEGER);
            }
            pstm.executeUpdate();

            ResultSet rs = pstm.getGeneratedKeys();
            if (rs.next()) {
                wellBeing.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add wellbeing check-in", e);
        }
    }

    @Override
    public void update(WellBeing wellBeing) {
        if (wellBeing.getUserId() == null) {
            throw new RuntimeException("User is required for wellbeing update.");
        }
        String sql = """
                UPDATE well_being
                SET entry_date_well = ?, mood_well = ?, stress_level_well = ?, energy_level_well = ?, sleep_hours_well = ?,
                    note_well = ?, updated_at_well = ?, user_id = ?
                WHERE id = ? AND user_id = ?
                """;
        try (PreparedStatement pstm = cnx.prepareStatement(sql)) {
            pstm.setTimestamp(1, Timestamp.valueOf(wellBeing.getEntryDate()));
            pstm.setString(2, wellBeing.getMood());
            pstm.setInt(3, wellBeing.getStressLevel());
            pstm.setInt(4, wellBeing.getEnergyLevel());
            pstm.setDouble(5, wellBeing.getSleepHours());
            pstm.setString(6, wellBeing.getNote());
            pstm.setTimestamp(7, Timestamp.valueOf(wellBeing.getUpdatedAt()));
            if (wellBeing.getUserId() != null) {
                pstm.setInt(8, wellBeing.getUserId());
            } else {
                pstm.setNull(8, java.sql.Types.INTEGER);
            }
            pstm.setInt(9, wellBeing.getId());
            pstm.setInt(10, wellBeing.getUserId());
            int affectedRows = pstm.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("Check-in not found for current user.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update wellbeing check-in", e);
        }
    }

    @Override
    public void delete(WellBeing wellBeing) {
        if (wellBeing.getUserId() == null) {
            throw new RuntimeException("User is required for wellbeing delete.");
        }
        try (PreparedStatement pstm = cnx.prepareStatement("DELETE FROM well_being WHERE id = ? AND user_id = ?")) {
            pstm.setInt(1, wellBeing.getId());
            pstm.setInt(2, wellBeing.getUserId());
            int affectedRows = pstm.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("Check-in not found for current user.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete wellbeing check-in", e);
        }
    }

    @Override
    public List<WellBeing> getAll() {
        return new ArrayList<>();
    }

    public List<WellBeing> findAllForUser(Integer userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        String sql = """
                SELECT id, entry_date_well, mood_well, stress_level_well, energy_level_well, sleep_hours_well,
                       note_well, created_at_well, updated_at_well, user_id
                FROM well_being
                WHERE user_id = ?
                ORDER BY entry_date_well DESC
                """;

        try (PreparedStatement pstm = cnx.prepareStatement(sql)) {
            pstm.setInt(1, userId);
            ResultSet rs = pstm.executeQuery();
            List<WellBeing> items = new ArrayList<>();
            while (rs.next()) {
                items.add(map(rs));
            }
            return items;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch wellbeing check-ins", e);
        }
    }

    private WellBeing map(ResultSet rs) throws SQLException {
        WellBeing item = new WellBeing();
        item.setId(rs.getInt("id"));
        Timestamp entryTs = rs.getTimestamp("entry_date_well");
        Timestamp createdTs = rs.getTimestamp("created_at_well");
        Timestamp updatedTs = rs.getTimestamp("updated_at_well");
        item.setEntryDate(entryTs != null ? entryTs.toLocalDateTime() : LocalDateTime.now());
        item.setMood(rs.getString("mood_well"));
        item.setStressLevel(rs.getInt("stress_level_well"));
        item.setEnergyLevel(rs.getInt("energy_level_well"));
        item.setSleepHours(rs.getDouble("sleep_hours_well"));
        item.setNote(rs.getString("note_well"));
        item.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);
        item.setUpdatedAt(updatedTs != null ? updatedTs.toLocalDateTime() : null);
        int userId = rs.getInt("user_id");
        item.setUserId(rs.wasNull() ? null : userId);
        return item;
    }
}
