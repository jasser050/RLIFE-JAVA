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
        ensureConnectionAvailable();
        if (cnx == null) {
            System.out.println("ServiceUser.add: Database connection unavailable");
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
            pstm.setString(6, user.getPassword() == null ? "" : user.getPassword());
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
        ensureConnectionAvailable();
        if (cnx == null) {
            System.out.println("ServiceUser.update: Database connection unavailable");
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
        ensureConnectionAvailable();
        if (cnx == null) {
            System.out.println("ServiceUser.delete: Database connection unavailable");
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
        ensureConnectionAvailable();
        if (cnx == null) {
            return null;
        }
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String req = "SELECT * FROM `user` WHERE LOWER(`email`) = ?";
        try {
            PreparedStatement pstm = cnx.prepareStatement(req);
            pstm.setString(1, normalizedEmail);
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
        ensureConnectionAvailable();
        if (cnx == null) {
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

    private void ensureConnectionAvailable() {
        cnx = MyDataBase.getInstance().getConnection();
    }
}
