package com.hotel.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // Windows Authentication — no username/password needed
    private static final String URL =
            "jdbc:sqlserver://DESKTOP-3M6HSIO:1433;" +
            "databaseName=HotelDB;" +
            "encrypt=true;" +
            "trustServerCertificate=true;" +
            "user=hotel_user;" +
            "password=StrongPass123!;";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MSSQL JDBC Driver not found. " +
                "Place mssql-jdbc-*.jar in the lib/ folder.", e);
        }
        return DriverManager.getConnection(URL);
    }
}
