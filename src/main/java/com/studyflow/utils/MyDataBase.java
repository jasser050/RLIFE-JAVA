package com.studyflow.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    final String URL = "jdbc:mysql://localhost:3306/rlife";
    final String USER = "root";
    final String PASSWORD = "";

    
    private Connection connection;
    private String lastError;
    private static MyDataBase instance;

    private MyDataBase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connected to database successfully");
        } catch (ClassNotFoundException e) {
            lastError = "MySQL driver not found: " + e.getMessage();
            System.err.println("MySQL driver not found: " + e.getMessage());
        } catch (SQLException e) {
            lastError = e.getMessage();
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
                lastError = null;
                System.out.println("Reconnected to database");
            }
        } catch (SQLException e) {
            lastError = e.getMessage();
            System.err.println("DB reconnect failed: " + e.getMessage());
        }
        return connection;
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            lastError = e.getMessage();
            return false;
        }
    }

    public String getLastError() {
        return lastError;
    }
}
