package com.studyflow.services;

import com.studyflow.interfaces.IService;
import com.studyflow.models.User;
import com.studyflow.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceUser implements IService<User> {

    private final Connection cnx;

    public ServiceUser() {
        this.cnx = MyDataBase.getInstance().getConnection();
    }

    public boolean isDatabaseAvailable() {
        return MyDataBase.getInstance().isConnected() && cnx != null;
    }

    private boolean ensureConnection(String operation) {
        if (isDatabaseAvailable()) {
            return true;
        }
        System.out.println(operation + ": database connection unavailable.");
        return false;
    }

    @Override
    public void add(User user) {
        if (!ensureConnection("ServiceUser.add")) {
            return;
        }
        String req = "INSERT INTO `user`(`email`, `first_name`, `last_name`, `username`, `roles`, `password`, `gender`, `created_at`, `updated_at`) VALUES (?,?,?,?,?,?,?,NOW(),NOW())";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            pstm.setString(1, user.getEmail());
            pstm.setString(2, user.getFirstName());
            pstm.setString(3, user.getLastName());
            pstm.setString(4, user.getUsername());
            pstm.setString(5, "[\"ROLE_USER\"]");
            pstm.setString(6, ""); // placeholder password
            pstm.setString(7, "male");
            pstm.executeUpdate();
            ResultSet rs = pstm.getGeneratedKeys();
            if (rs.next()) {
                user.setId(rs.getInt(1));
            }
            System.out.println("User added: " + user);
        } catch (SQLException e) {
            System.out.println("ServiceUser.add: " + e.getMessage());
        }
    }

    @Override
    public void update(User user) {
        if (!ensureConnection("ServiceUser.update")) {
            return;
        }
        String req = "UPDATE `user` SET `first_name`=?, `last_name`=?, `username`=?, `phone_number`=?, `bio`=?, `gender`=?, `university`=?, `student_id`=?, `password`=?, `updated_at`=NOW() WHERE `id`=?";
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
            pstm.setInt(10, user.getId());
            pstm.executeUpdate();
            System.out.println("User updated: " + user);
        } catch (SQLException e) {
            System.out.println("ServiceUser.update: " + e.getMessage());
        }
    }

    @Override
    public void delete(User user) {
        if (!ensureConnection("ServiceUser.delete")) {
            return;
        }
        String req = "DELETE FROM `user` WHERE `id`=?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setInt(1, user.getId());
            pstm.executeUpdate();
            System.out.println("User deleted: " + user);
        } catch (SQLException e) {
            System.out.println("ServiceUser.delete: " + e.getMessage());
        }
    }

    public User findByEmail(String email) {
        if (!ensureConnection("ServiceUser.findByEmail")) {
            return null;
        }
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

    public User findByIdentifier(String identifier) {
        if (!ensureConnection("ServiceUser.findByIdentifier")) {
            return null;
        }
        String normalized = identifier == null ? "" : identifier.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        List<User> exactMatches = findMatchingUsers(normalized, false);
        if (exactMatches.size() == 1) {
            return exactMatches.get(0);
        }
        if (exactMatches.size() > 1) {
            return null;
        }

        List<User> partialMatches = findMatchingUsers(normalized, true);
        return partialMatches.size() == 1 ? partialMatches.get(0) : null;
    }

    public User authenticate(String email, String password) {
        User user = findByEmail(email);
        if (user != null && password.equals(user.getPassword())) {
            return user;
        }
        return null;
    }

    public List<User> searchUsers(String query, int limit, Integer excludeUserId) {
        List<User> users = new ArrayList<>();
        if (!ensureConnection("ServiceUser.searchUsers")) {
            return users;
        }

        String normalized = query == null ? "" : query.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return users;
        }

        String wildcard = "%" + normalized + "%";
        int safeLimit = Math.max(1, limit);
        String req = "SELECT `id`, `email`, `first_name`, `last_name`, `username`, `phone_number`, `student_id` "
                + "FROM `user` "
                + "WHERE (? IS NULL OR `id` <> ?) AND ("
                + "LOWER(COALESCE(`email`, '')) LIKE ? OR "
                + "LOWER(COALESCE(`username`, '')) LIKE ? OR "
                + "LOWER(COALESCE(`first_name`, '')) LIKE ? OR "
                + "LOWER(COALESCE(`last_name`, '')) LIKE ? OR "
                + "LOWER(TRIM(CONCAT(COALESCE(`first_name`, ''), ' ', COALESCE(`last_name`, '')))) LIKE ? OR "
                + "LOWER(COALESCE(`phone_number`, '')) LIKE ? OR "
                + "LOWER(COALESCE(`student_id`, '')) LIKE ?) "
                + "ORDER BY "
                + "CASE "
                + "WHEN LOWER(COALESCE(`username`, '')) = ? THEN 0 "
                + "WHEN LOWER(COALESCE(`email`, '')) = ? THEN 1 "
                + "WHEN LOWER(TRIM(CONCAT(COALESCE(`first_name`, ''), ' ', COALESCE(`last_name`, '')))) = ? THEN 2 "
                + "ELSE 3 END, `username` ASC "
                + "LIMIT ?";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setObject(1, excludeUserId, Types.INTEGER);
            pstm.setObject(2, excludeUserId, Types.INTEGER);
            for (int index = 3; index <= 9; index++) {
                pstm.setString(index, wildcard);
            }
            pstm.setString(10, normalized);
            pstm.setString(11, normalized);
            pstm.setString(12, normalized);
            pstm.setInt(13, safeLimit);
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.out.println("ServiceUser.searchUsers: " + e.getMessage());
        }
        return users;
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
        return u;
    }

    private List<User> findMatchingUsers(String identifier, boolean partial) {
        List<User> users = new ArrayList<>();
        String value = identifier.toLowerCase();
        String comparator = partial ? "LIKE" : "=";
        String parameter = partial ? "%" + value + "%" : value;
        String req = "SELECT * FROM `user` WHERE "
                + "LOWER(COALESCE(`email`, '')) " + comparator + " ? OR "
                + "LOWER(COALESCE(`username`, '')) " + comparator + " ? OR "
                + "LOWER(COALESCE(`first_name`, '')) " + comparator + " ? OR "
                + "LOWER(COALESCE(`last_name`, '')) " + comparator + " ? OR "
                + "LOWER(TRIM(CONCAT(COALESCE(`first_name`, ''), ' ', COALESCE(`last_name`, '')))) " + comparator + " ? OR "
                + "LOWER(COALESCE(`phone_number`, '')) " + comparator + " ? OR "
                + "LOWER(COALESCE(`student_id`, '')) " + comparator + " ? "
                + "LIMIT 2";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            for (int index = 1; index <= 7; index++) {
                pstm.setString(index, parameter);
            }
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.out.println("ServiceUser.findMatchingUsers: " + e.getMessage());
        }
        return users;
    }

    private String safeString(ResultSet rs, String col) {
        try { return rs.getString(col); } catch (SQLException e) { return null; }
    }

    private int safeInt(ResultSet rs, String col) {
        try { return rs.getInt(col); } catch (SQLException e) { return 0; }
    }

    @Override
    public List<User> getAll() {
        if (!ensureConnection("ServiceUser.getAll")) {
            return new ArrayList<>();
        }
        List<User> users = new ArrayList<>();
        String req = "SELECT `id`, `email`, `first_name`, `last_name`, `username` FROM `user`";
        try {
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setEmail(rs.getString("email"));
                u.setFirstName(rs.getString("first_name"));
                u.setLastName(rs.getString("last_name"));
                u.setUsername(rs.getString("username"));
                users.add(u);
            }
        } catch (SQLException e) {
            System.out.println("ServiceUser.getAll: " + e.getMessage());
        }
        return users;
    }
}
