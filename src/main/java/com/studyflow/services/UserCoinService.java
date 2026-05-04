package com.studyflow.services;

import com.studyflow.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserCoinService {
    private final Connection cnx;

    public UserCoinService() {
        this.cnx = MyDataBase.getInstance().getConnection();
    }

    public boolean isDatabaseAvailable() {
        return cnx != null && MyDataBase.getInstance().isConnected();
    }

    public int addUsageCoins(int userId, int coins) {
        if (coins <= 0) {
            return 0;
        }
        requireDatabase();
        String sql = "UPDATE `user` SET coins = COALESCE(coins, 0) + ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, coins);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0 ? coins : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to award usage coins.", e);
        }
    }

    public int getCoins(int userId) {
        requireDatabase();
        String sql = "SELECT COALESCE(coins, 0) FROM `user` WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load user coins.", e);
        }
    }

    private void requireDatabase() {
        if (!isDatabaseAvailable()) {
            throw new RuntimeException("Database unavailable.");
        }
    }
}
