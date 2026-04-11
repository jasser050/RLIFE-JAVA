package com.studyflow.services;

import com.studyflow.models.TypeSeance;
import com.studyflow.utils.MyDataBase;
import com.studyflow.utils.UserSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServiceTypeSeance {

    private final Connection cnx;

    public ServiceTypeSeance() {
        this.cnx = MyDataBase.getInstance().getConnection();
    }

    public List<TypeSeance> getAvailableTypes() {
        List<TypeSeance> types = new ArrayList<>();
        Integer currentUserId = UserSession.getInstance().getCurrentUser() != null
                ? UserSession.getInstance().getCurrentUser().getId()
                : null;

        String req = "SELECT id, name, user_id FROM type_seance " +
                "WHERE user_id IS NULL OR user_id = ? ORDER BY name";

        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            if (currentUserId == null) {
                pstm.setNull(1, java.sql.Types.INTEGER);
            } else {
                pstm.setInt(1, currentUserId);
            }

            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    TypeSeance typeSeance = new TypeSeance();
                    typeSeance.setId(rs.getInt("id"));
                    typeSeance.setName(rs.getString("name"));
                    int userId = rs.getInt("user_id");
                    typeSeance.setUserId(rs.wasNull() ? null : userId);
                    types.add(typeSeance);
                }
            }
        } catch (SQLException e) {
            System.out.println("ServiceTypeSeance.getAvailableTypes: " + e.getMessage());
        }

        return types;
    }
}
