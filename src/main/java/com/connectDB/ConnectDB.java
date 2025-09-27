package com.connectDB;


import java.sql.*;

public class ConnectDB {
    private static Connection con = null;
    public static ConnectDB instance = new ConnectDB();

    public static Connection getCon() {
        return con;
    }

    public static ConnectDB getInstance() {
        return instance;
    }

    public void connect() throws SQLException {

        String url = "jdbc:sqlserver://localhost:1433;"
                +"databaseName=MediWOW;"
                +"encrypt=true;"
                +"trustServerCertificate=true;";
        String user = "sa";
        String password = "sapassword";
        con = DriverManager.getConnection(url, user, password);
        System.out.println("Connected to the database.");
    }

    public void disconnect() {
        if (con != null) {
            try {
                con.close();
                System.out.println("Disconnected from the database.");
            } catch (SQLException e) {
                // Log and ignore during shutdown
                System.err.println("Error closing DB connection: " + e.getMessage());
            }
        }
    }
}