package com.studyflow.services;

import com.studyflow.interfaces.IService;
import com.studyflow.models.User;
import com.studyflow.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceUser implements IService<User> {

    private Connection cnx;

    public ServiceUser() {
        this.cnx = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void add(User user) {
        String req = "INSERT INTO `user`(`email`, `first_name`, `last_name`, `username`, `roles`, `password`, `gender`, `phone_number`, `university`, `student_id`, `profile_pic`, `created_at`, `updated_at`) VALUES (?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            pstm.setString(1, user.getEmail());
            pstm.setString(2, user.getFirstName());
            pstm.setString(3, user.getLastName());
            pstm.setString(4, user.getUsername());
            pstm.setString(5, "[\"ROLE_USER\"]");
            pstm.setString(6, user.getPassword() != null ? user.getPassword() : "");
            pstm.setString(7, user.getGender() != null ? user.getGender() : "male");
            pstm.setString(8, user.getPhoneNumber());
            pstm.setString(9, user.getUniversity());
            pstm.setString(10, user.getStudentId());
            pstm.setString(11, user.getProfilePic());
            pstm.executeUpdate();
            ResultSet rs = pstm.getGeneratedKeys();
            if (rs.next()) {
                user.setId(rs.getInt(1));
            }
            System.out.println("User added with id=" + user.getId());
        } catch (SQLException e) {
            System.err.println("ServiceUser.add FAILED: " + e.getMessage());
            throw new RuntimeException("Failed to create account: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(User user) {
        String req = "UPDATE `user` SET `first_name`=?, `last_name`=?, `username`=?, `phone_number`=?, `bio`=?, `gender`=?, `university`=?, `student_id`=?, `password`=?, `profile_pic`=?, `updated_at`=NOW() WHERE `id`=?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setString(1, user.getFirstName());
            pstm.setString(2, user.getLastName());
            pstm.setString(3, user.getUsername());
            pstm.setString(4, user.getPhoneNumber());
            pstm.setString(5, user.getBio());
            pstm.setString(6, user.getGender());
            pstm.setString(7, user.getUniversity());
            pstm.setString(8, user.getStudentId());
            pstm.setString(9, user.getPassword());
            pstm.setString(10, user.getProfilePic());
            pstm.setInt(11, user.getId());
            pstm.executeUpdate();
            System.out.println("User updated: " + user);
        } catch (SQLException e) {
            System.out.println("ServiceUser.update: " + e.getMessage());
        }
    }

    @Override
    public void delete(User user) {
        try {
            // Remove related records first (foreign key constraints)
            PreparedStatement cleanSettings = cnx.prepareStatement("DELETE FROM `user_settings` WHERE `user_id`=?");
            cleanSettings.setInt(1, user.getId());
            cleanSettings.executeUpdate();

            PreparedStatement pstm = cnx.prepareStatement("DELETE FROM `user` WHERE `id`=?");
            pstm.setInt(1, user.getId());
            pstm.executeUpdate();
            System.out.println("User deleted: " + user);
        } catch (SQLException e) {
            System.err.println("ServiceUser.delete FAILED: " + e.getMessage());
            throw new RuntimeException("Failed to delete account: " + e.getMessage(), e);
        }
    }

    public User findByEmail(String email) {
        String req = "SELECT * FROM `user` WHERE `email` = ?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setString(1, email);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                return mapUser(rs);
            }
        } catch (SQLException e) {
            System.out.println("ServiceUser.findByEmail: " + e.getMessage());
        }
        return null;
    }

    public User authenticate(String email, String password) {
        User user = findByEmail(email);
        if (user != null && password.equals(user.getPassword())) {
            return user;
        }
        return null;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setEmail(rs.getString("email"));
        u.setFirstName(safeString(rs, "first_name"));
        u.setLastName(safeString(rs, "last_name"));
        u.setUsername(safeString(rs, "username"));
        u.setPassword(safeString(rs, "password"));
        u.setPhoneNumber(safeString(rs, "phone_number"));
        u.setBio(safeString(rs, "bio"));
        u.setGender(safeString(rs, "gender"));
        u.setUniversity(safeString(rs, "university"));
        u.setStudentId(safeString(rs, "student_id"));
        u.setProfilePic(safeString(rs, "profile_pic"));
        u.setCoins(safeInt(rs, "coins"));
        u.setBanned(safeInt(rs, "is_banned") == 1);
        u.setBanReason(safeString(rs, "ban_reason"));
        try { Timestamp ts = rs.getTimestamp("created_at"); if (ts != null) u.setCreatedAt(ts.toString()); } catch (SQLException ignored) {}
        return u;
    }

    private String safeString(ResultSet rs, String col) {
        try { return rs.getString(col); } catch (SQLException e) { return null; }
    }

    private int safeInt(ResultSet rs, String col) {
        try { return rs.getInt(col); } catch (SQLException e) { return 0; }
    }

    @Override
    public List<User> getAll() {
        List<User> users = new ArrayList<>();
        String req = "SELECT * FROM `user`";
        try {
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.out.println("ServiceUser.getAll: " + e.getMessage());
        }
        return users;
    }

    public void updatePassword(String email, String newPassword) {
        String req = "UPDATE `user` SET `password`=?, `updated_at`=NOW() WHERE `email`=?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setString(1, newPassword);
            pstm.setString(2, email);
            pstm.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update password: " + e.getMessage(), e);
        }
    }

    // ── Admin methods ────────────────────────────────────────────────

    public int countUsers() {
        try {
            ResultSet rs = cnx.createStatement().executeQuery("SELECT COUNT(*) FROM `user`");
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.err.println("countUsers: " + e.getMessage()); }
        return 0;
    }

    public int countBannedUsers() {
        try {
            ResultSet rs = cnx.createStatement().executeQuery("SELECT COUNT(*) FROM `user` WHERE `is_banned` = 1");
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.err.println("countBanned: " + e.getMessage()); }
        return 0;
    }

    public void banUser(int userId, String reason) {
        String req = "UPDATE `user` SET `is_banned`=1, `ban_reason`=?, `banned_at`=NOW() WHERE `id`=?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setString(1, reason);
            pstm.setInt(2, userId);
            pstm.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ban user: " + e.getMessage(), e);
        }
    }

    public void unbanUser(int userId) {
        String req = "UPDATE `user` SET `is_banned`=0, `ban_reason`=NULL, `banned_at`=NULL WHERE `id`=?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setInt(1, userId);
            pstm.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to unban user: " + e.getMessage(), e);
        }
    }

    public List<String[]> getAuditLog() {
        List<String[]> log = new ArrayList<>();
        String req = "SELECT id, email, CONCAT(first_name,' ',last_name) as name, " +
                     "created_at, is_banned, banned_at, ban_reason FROM `user` ORDER BY created_at DESC";
        try {
            ResultSet rs = cnx.createStatement().executeQuery(req);
            while (rs.next()) {
                String action;
                String date;
                if (rs.getInt("is_banned") == 1 && rs.getTimestamp("banned_at") != null) {
                    action = "BANNED: " + (rs.getString("ban_reason") != null ? rs.getString("ban_reason") : "No reason");
                    date = rs.getTimestamp("banned_at").toString();
                } else {
                    action = "REGISTERED";
                    date = rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString() : "N/A";
                }
                log.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("email"),
                    rs.getString("name"),
                    action,
                    date
                });
            }
        } catch (SQLException e) { System.err.println("getAuditLog: " + e.getMessage()); }
        return log;
    }

    public java.util.Map<String, Integer> getUserRegistrationsByMonth() {
        java.util.Map<String, Integer> stats = new java.util.LinkedHashMap<>();
        String req = "SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COUNT(*) as cnt " +
                     "FROM `user` WHERE created_at IS NOT NULL " +
                     "GROUP BY month ORDER BY month DESC LIMIT 6";
        try {
            ResultSet rs = cnx.createStatement().executeQuery(req);
            while (rs.next()) {
                stats.put(rs.getString("month"), rs.getInt("cnt"));
            }
        } catch (SQLException e) { System.err.println("getRegByMonth: " + e.getMessage()); }
        return stats;
    }
}
