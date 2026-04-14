package com.studyflow.services;

import com.studyflow.interfaces.IService;
import com.studyflow.models.PlanningEntry;
import com.studyflow.models.User;
import com.studyflow.utils.MyDataBase;
import com.studyflow.utils.UserSession;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServicePlanning implements IService<PlanningEntry> {

    private final Connection cnx;

    public ServicePlanning() {
        this.cnx = MyDataBase.getInstance().getConnection();
        ensureTableExists();
    }

    @Override
    public void add(PlanningEntry planningEntry) {
        String req = "INSERT INTO planning (user_id, seance_id, date_debut, date_fin, color, feedback, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";

        try (PreparedStatement pstm = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            bindWriteFields(pstm, planningEntry);
            pstm.executeUpdate();

            try (ResultSet rs = pstm.getGeneratedKeys()) {
                if (rs.next()) {
                    planningEntry.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.out.println("ServicePlanning.add: " + e.getMessage());
        }
    }

    @Override
    public void update(PlanningEntry planningEntry) {
        String req = "UPDATE planning SET seance_id = ?, date_debut = ?, date_fin = ?, color = ?, feedback = ?, updated_at = NOW() " +
                "WHERE id = ? AND user_id = ?";

        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1, planningEntry.getSeanceId());
            pstm.setTimestamp(2, toStartTimestamp(planningEntry));
            pstm.setTimestamp(3, toEndTimestamp(planningEntry));
            pstm.setString(4, planningEntry.getColorHex());
            pstm.setString(5, planningEntry.getFeedback());
            pstm.setInt(6, planningEntry.getId());
            pstm.setInt(7, planningEntry.getUserId());
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ServicePlanning.update: " + e.getMessage());
        }
    }

    @Override
    public void delete(PlanningEntry planningEntry) {
        String req = "DELETE FROM planning WHERE id = ? AND user_id = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1, planningEntry.getId());
            pstm.setInt(2, planningEntry.getUserId());
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ServicePlanning.delete: " + e.getMessage());
        }
    }

    @Override
    public List<PlanningEntry> getAll() {
        List<PlanningEntry> planningEntries = new ArrayList<>();
        Integer userId = resolveCurrentUserId();
        if (userId == null) {
            return planningEntries;
        }

        String req = "SELECT p.id, p.user_id, p.seance_id, p.date_debut, p.date_fin, p.color, p.feedback, p.created_at, p.updated_at, s.titre " +
                "FROM planning p " +
                "JOIN seance s ON s.id = p.seance_id " +
                "WHERE p.user_id = ? " +
                "ORDER BY p.date_debut ASC, p.id ASC";

        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1, userId);
            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    planningEntries.add(mapPlanningEntry(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("ServicePlanning.getAll: " + e.getMessage());
        }

        return planningEntries;
    }

    public boolean isColorUsedOnDate(int userId, java.time.LocalDate date, String colorHex, Integer excludedPlanningId) {
        String req = "SELECT COUNT(*) FROM planning WHERE user_id = ? AND DATE(date_debut) = ? AND color = ? " +
                (excludedPlanningId != null ? "AND id <> ?" : "");
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1, userId);
            pstm.setDate(2, Date.valueOf(date));
            pstm.setString(3, colorHex);
            if (excludedPlanningId != null) {
                pstm.setInt(4, excludedPlanningId);
            }
            try (ResultSet rs = pstm.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("ServicePlanning.isColorUsedOnDate: " + e.getMessage());
            return false;
        }
    }

    public boolean hasTimeOverlap(int userId, LocalDate date, LocalTime startTime, LocalTime endTime, Integer excludedPlanningId) {
        String req = "SELECT COUNT(*) FROM planning " +
                "WHERE user_id = ? AND DATE(date_debut) = ? " +
                "AND ? < TIME(date_fin) " +
                "AND ? > TIME(date_debut) " +
                (excludedPlanningId != null ? "AND id <> ?" : "");

        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1, userId);
            pstm.setDate(2, Date.valueOf(date));
            pstm.setTime(3, Time.valueOf(startTime));
            pstm.setTime(4, Time.valueOf(endTime));
            if (excludedPlanningId != null) {
                pstm.setInt(5, excludedPlanningId);
            }

            try (ResultSet rs = pstm.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("ServicePlanning.hasTimeOverlap: " + e.getMessage());
            return false;
        }
    }

    private void bindWriteFields(PreparedStatement pstm, PlanningEntry planningEntry) throws SQLException {
        pstm.setInt(1, planningEntry.getUserId());
        pstm.setInt(2, planningEntry.getSeanceId());
        pstm.setTimestamp(3, toStartTimestamp(planningEntry));
        pstm.setTimestamp(4, toEndTimestamp(planningEntry));
        pstm.setString(5, planningEntry.getColorHex());
        pstm.setString(6, planningEntry.getFeedback());
    }

    private PlanningEntry mapPlanningEntry(ResultSet rs) throws SQLException {
        PlanningEntry planningEntry = new PlanningEntry();
        planningEntry.setId(rs.getInt("id"));
        planningEntry.setUserId(rs.getInt("user_id"));
        planningEntry.setSeanceId(rs.getInt("seance_id"));
        planningEntry.setSeanceTitle(rs.getString("titre"));
        Timestamp startTimestamp = rs.getTimestamp("date_debut");
        Timestamp endTimestamp = rs.getTimestamp("date_fin");
        if (startTimestamp != null) {
            planningEntry.setPlanningDate(startTimestamp.toLocalDateTime().toLocalDate());
            planningEntry.setStartTime(startTimestamp.toLocalDateTime().toLocalTime());
        }
        if (endTimestamp != null) {
            planningEntry.setEndTime(endTimestamp.toLocalDateTime().toLocalTime());
        }
        planningEntry.setColorHex(rs.getString("color"));
        planningEntry.setFeedback(rs.getString("feedback"));
        planningEntry.setCreatedAt(rs.getTimestamp("created_at"));
        planningEntry.setUpdatedAt(rs.getTimestamp("updated_at"));
        return planningEntry;
    }

    private Timestamp toStartTimestamp(PlanningEntry planningEntry) {
        LocalDateTime dateTime = LocalDateTime.of(planningEntry.getPlanningDate(), planningEntry.getStartTime());
        return Timestamp.valueOf(dateTime);
    }

    private Timestamp toEndTimestamp(PlanningEntry planningEntry) {
        LocalDateTime dateTime = LocalDateTime.of(planningEntry.getPlanningDate(), planningEntry.getEndTime());
        return Timestamp.valueOf(dateTime);
    }

    private Integer resolveCurrentUserId() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getId() : null;
    }

    private void ensureTableExists() {
        String sql = "CREATE TABLE IF NOT EXISTS planning (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "user_id INT NOT NULL, " +
                "seance_id INT NOT NULL, " +
                "date_debut DATETIME NOT NULL, " +
                "date_fin DATETIME NOT NULL, " +
                "color VARCHAR(20) NOT NULL, " +
                "feedback TEXT NULL, " +
                "created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "INDEX idx_planning_user_date (user_id, date_debut), " +
                "CONSTRAINT fk_planning_seance FOREIGN KEY (seance_id) REFERENCES seance(id) ON DELETE CASCADE" +
                ")";

        try (Statement stmt = cnx.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println("ServicePlanning.ensureTableExists: " + e.getMessage());
        }
    }
}
