package com.studyflow.services;

import com.studyflow.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class EvalMatService {

    private final Connection conn;

    public EvalMatService() {
        this.conn = MyDataBase.getInstance().getConnection();
    }

    public void addEvalMat(int evaluationId, int matiereId) {
        String sql = "INSERT INTO evalmat (evaluation_id, matiere_id) VALUES (?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, evaluationId);
            ps.setInt(2, matiereId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error while inserting EvalMat: " + e.getMessage());
        }
    }
}