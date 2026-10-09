
package com.parking;

import com.parking.database.DatabaseInitializer;
import com.parking.ui.ManualDataEntryFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.sql.SQLException;

public class App {

    public static void main(String[] args) {
        try {
            DatabaseInitializer.initialize();
        } catch (IllegalStateException exception) {
            JOptionPane.showMessageDialog(
                    null,
                    "Could not initialize the parking database:\n"
                            + exception.getMessage(),
                    "Startup Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        SwingUtilities.invokeLater(() -> {
            ManualDataEntryFrame frame = new ManualDataEntryFrame();
            frame.setVisible(true);
        });
    }
}