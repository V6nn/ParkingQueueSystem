package com.parking;

import com.parking.database.DatabaseInitializer;
import com.parking.ui.ParkingDashboard;

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
            ParkingDashboard dashboard = new ParkingDashboard();
            dashboard.setVisible(true);
        });
    }
}
