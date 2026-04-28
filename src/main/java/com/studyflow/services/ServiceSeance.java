package com.studyflow.services;

import com.studyflow.interfaces.IService;
import com.studyflow.models.Seance;
import com.studyflow.models.User;
import com.studyflow.utils.MyDataBase;
import com.studyflow.utils.UserSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ServiceSeance implements IService<Seance> {

    private final Connection cnx;
    private final boolean hasMatiereIdColumn;

    public ServiceSeance() {
        this.cnx = MyDataBase.getInstance().getConnection();
        this.hasMatiereIdColumn = hasColumn("seance", "matiere_id");
    }

    @Override
    public void add(Seance seance) {
        String req = hasMatiereIdColumn
                ? "INSERT INTO seance (user_id, titre, type_seance, description, partage_avec, statut, created_at, updated_at, type_seance_id, matiere_id) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW(), ?, ?)"
                : "INSERT INTO seance (user_id, titre, type_seance, description, partage_avec, statut, created_at, updated_at, type_seance_id) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW(), ?)";

        try (PreparedStatement pstm = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            bindWriteFields(pstm, seance);
            pstm.executeUpdate();

            try (ResultSet rs = pstm.getGeneratedKeys()) {
                if (rs.next()) {
                    seance.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.out.println("ServiceSeance.add: " + e.getMessage());
        }
    }

    @Override
    public void update(Seance seance) {
        String req = hasMatiereIdColumn
                ? "UPDATE seance SET titre = ?, type_seance = ?, description = ?, partage_avec = ?, statut = ?, updated_at = NOW(), type_seance_id = ?, matiere_id = ? WHERE id = ? AND user_id = ?"
                : "UPDATE seance SET titre = ?, type_seance = ?, description = ?, partage_avec = ?, statut = ?, updated_at = NOW(), type_seance_id = ? WHERE id = ? AND user_id = ?";

        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setString(1, seance.getTitre());
            pstm.setString(2, seance.getTypeSeance());
            pstm.setString(3, emptyToNull(seance.getDescription()));
            pstm.setNull(4, java.sql.Types.LONGVARCHAR);
            pstm.setNull(5, java.sql.Types.VARCHAR);
            if (seance.getTypeSeanceId() == null) {
                pstm.setNull(6, java.sql.Types.INTEGER);
            } else {
                pstm.setInt(6, seance.getTypeSeanceId());
            }
            if (hasMatiereIdColumn) {
                pstm.setNull(7, java.sql.Types.INTEGER);
                pstm.setInt(8, seance.getId());
                pstm.setInt(9, seance.getUserId());
            } else {
                pstm.setInt(7, seance.getId());
                pstm.setInt(8, seance.getUserId());
            }
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ServiceSeance.update: " + e.getMessage());
        }
    }

    @Override
    public void delete(Seance seance) {
        String req = "DELETE FROM seance WHERE id = ? AND user_id = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1, seance.getId());
            pstm.setInt(2, seance.getUserId());
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ServiceSeance.delete: " + e.getMessage());
        }
    }

    @Override
    public List<Seance> getAll() {
        List<Seance> seances = new ArrayList<>();
        Integer userId = resolveCurrentUserId();

        String req = hasMatiereIdColumn
                ? "SELECT s.id, s.user_id, s.titre, s.type_seance, s.description, s.partage_avec, s.statut, s.created_at, s.updated_at, s.type_seance_id, s.matiere_id, ts.name AS type_name FROM seance s LEFT JOIN type_seance ts ON ts.id = s.type_seance_id "
                : "SELECT s.id, s.user_id, s.titre, s.type_seance, s.description, s.partage_avec, s.statut, s.created_at, s.updated_at, s.type_seance_id, ts.name AS type_name FROM seance s LEFT JOIN type_seance ts ON ts.id = s.type_seance_id ";
        req += (userId != null ? "WHERE s.user_id = ? " : "") + "ORDER BY s.created_at DESC, s.id DESC";

        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            if (userId != null) {
                pstm.setInt(1, userId);
            }

            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    seances.add(mapSeance(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("ServiceSeance.getAll: " + e.getMessage());
        }

        return seances;
    }

    private void bindWriteFields(PreparedStatement pstm, Seance seance) throws SQLException {
        pstm.setInt(1, seance.getUserId());
        pstm.setString(2, seance.getTitre());
        pstm.setString(3, emptyToNull(seance.getTypeSeance()));
        pstm.setString(4, emptyToNull(seance.getDescription()));
        pstm.setNull(5, java.sql.Types.LONGVARCHAR);
        pstm.setNull(6, java.sql.Types.VARCHAR);
        if (seance.getTypeSeanceId() == null) {
            pstm.setNull(7, java.sql.Types.INTEGER);
        } else {
            pstm.setInt(7, seance.getTypeSeanceId());
        }
        if (hasMatiereIdColumn) {
            pstm.setNull(8, java.sql.Types.INTEGER);
        }
    }

    private Seance mapSeance(ResultSet rs) throws SQLException {
        Seance seance = new Seance();
        seance.setId(rs.getInt("id"));
        seance.setUserId(rs.getInt("user_id"));
        seance.setTitre(rs.getString("titre"));
        seance.setTypeSeance(rs.getString("type_seance"));
        seance.setDescription(rs.getString("description"));
        seance.setPartageAvec(rs.getString("partage_avec"));
        seance.setStatut(rs.getString("statut"));
        seance.setCreatedAt(rs.getTimestamp("created_at"));
        seance.setUpdatedAt(rs.getTimestamp("updated_at"));
        int typeSeanceId = rs.getInt("type_seance_id");
        seance.setTypeSeanceId(rs.wasNull() ? null : typeSeanceId);
        if (hasMatiereIdColumn) {
            int matiereId = rs.getInt("matiere_id");
            seance.setMatiereId(rs.wasNull() ? null : matiereId);
        } else {
            seance.setMatiereId(null);
        }
        seance.setTypeSeanceName(rs.getString("type_name"));
        return seance;
    }

    private Integer resolveCurrentUserId() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getId() : null;
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasColumn(String tableName, String columnName) {
        try {
            var metadata = cnx.getMetaData();
            try (ResultSet rs = metadata.getColumns(cnx.getCatalog(), null, tableName, columnName)) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }
}
