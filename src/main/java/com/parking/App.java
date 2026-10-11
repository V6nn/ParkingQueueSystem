package com.parking;

import com.parking.database.DatabaseInitializer;
import com.parking.ui.LoginWindow;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class App {

    public static void main(String[] args) {
        try {
            DatabaseInitializer.initialize();
        } catch (IllegalStateException exception) {
            JOptionPane.showMessageDialog(
                    null,
                    "Database initialization failed:\n"
                            + exception.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                LoginWindow loginWindow = new LoginWindow();
                loginWindow.setVisible(true);
            } catch (java.sql.SQLException exception) {
                JOptionPane.showMessageDialog(
                        null,
                        "Could not load the admin account:\n"
                                + exception.getMessage(),
                        "Login Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
