package com.parking.ui;

import com.parking.models.VehicleTransaction;
import com.parking.services.VehicleTransactionService;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class DriverWindow extends JFrame {

    private final Runnable onDataChanged;

    private final VehicleTransactionService transactionService =
            new VehicleTransactionService();

    private final JTextField plateField = new JTextField(20);

    public DriverWindow() {
        this(() -> { });
    }

    public DriverWindow(Runnable onDataChanged) {
        if (onDataChanged == null) {
            throw new IllegalArgumentException(
                    "Data change callback cannot be null.");
        }
        this.onDataChanged = onDataChanged;

        ParkflowTheme.install();
        ParkflowTheme.styleTextField(plateField);
        plateField.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        plateField.setPreferredSize(new Dimension(0, 54));
        setTitle("Parkflow - Driver Portal");
        setSize(620, 420);
        setMinimumSize(new Dimension(540, 380));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(ParkflowTheme.BACKGROUND);

        JPanel form = new JPanel(new GridLayout(0, 1, 10, 12));
        form.setBackground(ParkflowTheme.SURFACE);
        form.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ParkflowTheme.BORDER),
                        BorderFactory.createEmptyBorder(28, 32, 28, 32)));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ParkflowTheme.NAVY);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(
                        0, 5, 0, 0, ParkflowTheme.GREEN),
                BorderFactory.createEmptyBorder(17, 22, 17, 22)));

        JLabel title = new JLabel("Parkflow");
        title.setFont(new Font("Segoe UI", Font.BOLD, 23));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        JLabel section = new JLabel("DRIVER PORTAL");
        section.setFont(new Font("Segoe UI", Font.BOLD, 11));
        section.setForeground(new Color(174, 202, 205));
        header.add(section, BorderLayout.EAST);

        JLabel instruction =
                new JLabel("Enter your plate number to pay and exit.");
        instruction.setFont(new Font("Segoe UI", Font.BOLD, 16));
        form.add(instruction);
        form.add(plateField);

        JButton exitButton = new JButton("Pay and Exit");
        ParkflowTheme.styleButton(exitButton, true);
        exitButton.addActionListener(event -> payAtExit());

        JLabel notice = new JLabel(
                "<html><center>"
                + "Payment is collected when you exit."
                + "<br>Demo only — no real payment is processed."
                + "</center></html>",
                SwingConstants.CENTER);
        notice.setForeground(ParkflowTheme.MUTED);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setBackground(ParkflowTheme.BACKGROUND);
        bottom.setBorder(
                BorderFactory.createEmptyBorder(12, 32, 22, 32));
        bottom.add(exitButton, BorderLayout.NORTH);
        bottom.add(notice, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(exitButton);
    }

    private void payAtExit() {
        String plate = plateField.getText().trim();

        if (plate.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Enter your registered plate number.",
                    "Plate Required",
                    JOptionPane.WARNING_MESSAGE);
            plateField.requestFocusInWindow();
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Confirm payment and exit for plate "
                        + plate.toUpperCase() + "?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            VehicleTransaction transaction =
                    transactionService.payAtExit(plate);
            onDataChanged.run();

            JOptionPane.showMessageDialog(
                    this,
                    String.format(
                            "Payment successful!%n%n"
                                    + "Transaction: TXN-%04d%n"
                                    + "Plate: %s%n"
                                    + "Vehicle: %s%n"
                                    + "Amount: PHP %.2f%n"
                                    + "Payment status: %s%n"
                                    + "Parking status: %s%n"
                                    + "Exited at: %s",
                            transaction.getId(),
                            transaction.getPlateNumber(),
                            transaction.getVehicleType(),
                            transaction.getAmount(),
                            transaction.getPaymentStatus(),
                            transaction.getParkingStatus(),
                            transaction.getExitedAt()),
                    "Exit Completed",
                    JOptionPane.INFORMATION_MESSAGE);

            plateField.setText("");
            plateField.requestFocusInWindow();

        } catch (SQLException | IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    exception.getMessage(),
                    "Exit Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                com.parking.database.DatabaseInitializer.initialize();
                new DriverWindow().setVisible(true);
            } catch (RuntimeException exception) {
                JOptionPane.showMessageDialog(
                        null,
                        "Unable to initialize the parking database:\n"
                                + exception.getMessage(),
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}