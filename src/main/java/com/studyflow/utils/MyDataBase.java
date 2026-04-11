package com.studyflow.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    private static final String URL      = "jdbc:mysql://localhost:3306/rlife";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private Connection connection;
    private static MyDataBase instance;

    private MyDataBase() {
        connect();
    }

    private void connect() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Database connected.");
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
        }
    }

    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    /**
     * Returns a valid connection, reconnecting automatically if the connection
     * was closed or timed out.
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                System.out.println("⚠️  Connection lost — reconnecting…");
                connect();
            }
        } catch (SQLException e) {
            System.err.println("❌ Error checking connection: " + e.getMessage());
            connect();
        }
        return connection;
    }
}