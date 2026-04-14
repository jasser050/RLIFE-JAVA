package com.studyflow.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    final String URL = "jdbc:mysql://localhost:3306/rlife";
    final String USER = "root";
    final String PASSWORD = "";

    
    private Connection connection;
    private static MyDataBase instance;

    private MyDataBase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connected to database successfully");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    public static MyDataBase getInstance() {
        if (instance == null)
            instance = new MyDataBase();
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Reconnected to database");
            }
        } catch (SQLException e) {
            System.err.println("DB reconnect failed: " + e.getMessage());
        }
        return connection;
    }
}
