package com.studyflow.services;

import com.studyflow.interfaces.IService;
import com.studyflow.models.TypeSeance;
import com.studyflow.models.User;
import com.studyflow.utils.MyDataBase;
import com.studyflow.utils.UserSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceTypeSeance implements IService<TypeSeance> {

    private final Connection cnx;
    private final boolean hasUserIdColumn;

    public ServiceTypeSeance() {
        this.cnx = MyDataBase.getInstance().getConnection();
        this.hasUserIdColumn = hasColumn("type_seance", "user_id");
    }

    @Override
    public void add(TypeSeance typeSeance) {
        Integer userId = resolveCurrentUserId();
        if (hasUserIdColumn && userId == null) {
            return;
        }

        String req = hasUserIdColumn
                ? "INSERT INTO type_seance (name, user_id) VALUES (?, ?)"
                : "INSERT INTO type_seance (name) VALUES (?)";
        try (PreparedStatement pstm = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setString(1, emptyToNull(typeSeance.getName()));
            if (hasUserIdColumn) {
                pstm.setInt(2, userId);
            }
            pstm.executeUpdate();

            try (ResultSet rs = pstm.getGeneratedKeys()) {
                if (rs.next()) {
                    typeSeance.setId(rs.getInt(1));
                }
            }
            typeSeance.setUserId(hasUserIdColumn ? userId : null);
        } catch (SQLException e) {
            System.out.println("ServiceTypeSeance.add: " + e.getMessage());
        }
    }

    @Override
    public void update(TypeSeance typeSeance) {
        Integer userId = resolveCurrentUserId();
        if (typeSeance == null || (hasUserIdColumn && userId == null)) {
            return;
        }

        String req = hasUserIdColumn
                ? "UPDATE type_seance SET name = ? WHERE id = ? AND user_id = ?"
                : "UPDATE type_seance SET name = ? WHERE id = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setString(1, emptyToNull(typeSeance.getName()));
            pstm.setInt(2, typeSeance.getId());
            if (hasUserIdColumn) {
                pstm.setInt(3, userId);
            }
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ServiceTypeSeance.update: " + e.getMessage());
        }
    }

    @Override
    public void delete(TypeSeance typeSeance) {
        Integer userId = resolveCurrentUserId();
        if (typeSeance == null || (hasUserIdColumn && userId == null)) {
            return;
        }

        String req = hasUserIdColumn
                ? "DELETE FROM type_seance WHERE id = ? AND user_id = ?"
                : "DELETE FROM type_seance WHERE id = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1, typeSeance.getId());
            if (hasUserIdColumn) {
                pstm.setInt(2, userId);
            }
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ServiceTypeSeance.delete: " + e.getMessage());
        }
    }

    @Override
    public List<TypeSeance> getAll() {
        return getAvailableTypes();
    }

    public List<TypeSeance> getAvailableTypes() {
        List<TypeSeance> types = new ArrayList<>();
        Integer currentUserId = resolveCurrentUserId();

        String req = hasUserIdColumn
                ? "SELECT id, name, user_id FROM type_seance WHERE user_id IS NULL OR user_id = ? ORDER BY name ASC"
                : "SELECT id, name FROM type_seance ORDER BY name ASC";

        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            if (hasUserIdColumn) {
                if (currentUserId == null) {
                    pstm.setNull(1, java.sql.Types.INTEGER);
                } else {
                    pstm.setInt(1, currentUserId);
                }
            }

            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    TypeSeance typeSeance = new TypeSeance();
                    typeSeance.setId(rs.getInt("id"));
                    typeSeance.setName(rs.getString("name"));
                    if (hasUserIdColumn) {
                        int userId = rs.getInt("user_id");
                        typeSeance.setUserId(rs.wasNull() ? null : userId);
                    } else {
                        typeSeance.setUserId(null);
                    }
                    types.add(typeSeance);
                }
            }
        } catch (SQLException e) {
            System.out.println("ServiceTypeSeance.getAvailableTypes: " + e.getMessage());
        }

        return types;
    }

    public boolean isNameUsedByCurrentUser(String name, Integer excludedTypeId) {
        Integer userId = resolveCurrentUserId();
        if (userId == null || name == null || name.isBlank()) {
            return false;
        }

        String req = "SELECT COUNT(*) FROM type_seance WHERE user_id = ? AND LOWER(name) = LOWER(?) AND (? IS NULL OR id <> ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1, userId);
            pstm.setString(2, name.trim());
            if (excludedTypeId == null) {
                pstm.setNull(3, java.sql.Types.INTEGER);
                pstm.setNull(4, java.sql.Types.INTEGER);
            } else {
                pstm.setInt(3, excludedTypeId);
                pstm.setInt(4, excludedTypeId);
            }

            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("ServiceTypeSeance.isNameUsedByCurrentUser: " + e.getMessage());
        }
        return false;
    }

    public boolean hasLinkedSessions(int typeSeanceId) {
        String req = "SELECT COUNT(*) FROM seance WHERE type_seance_id = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1, typeSeanceId);
            try (ResultSet rs = pstm.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("ServiceTypeSeance.hasLinkedSessions: " + e.getMessage());
        }
        return false;
    }

    public boolean isOwnedByCurrentUser(TypeSeance typeSeance) {
        Integer userId = resolveCurrentUserId();
        return typeSeance != null && userId != null && typeSeance.getUserId() != null && typeSeance.getUserId().equals(userId);
    }

    private Integer resolveCurrentUserId() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        return currentUser == null ? null : currentUser.getId();
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
