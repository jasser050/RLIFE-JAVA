package com.studyflow.utils;

import java.sql.Connection;

/**
 * Compatibility wrapper for code that still expects a DataSource singleton.
 * The project's real JDBC access is centralized in MyDataBase.
 */
public class DataSource {
    private static DataSource instance;

    private DataSource() {
    }

    public static DataSource getInstance() {
        if (instance == null) {
            instance = new DataSource();
        }
        return instance;
    }

    public Connection getConnection() {
        return MyDataBase.getInstance().getConnection();
    }
}
