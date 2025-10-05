package com;

import com.connectDB.ConnectDB;

import javax.swing.*;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        // Connect to the database first
        try {
            ConnectDB.getInstance().connect();
        } catch (SQLException ex) {
            // Show a dialog and print stack trace; continue to show GUI even if DB fails
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Failed to connect to database: " + ex.getMessage(),
                    "DB Connection Error",
                    JOptionPane.ERROR_MESSAGE));
            ex.printStackTrace();
        }

        // Ensure DB disconnects when the app exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                ConnectDB.getInstance().disconnect();
            } catch (Exception ignored) {
            }
        }));

        // Launch GUI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }




        });
    }
}