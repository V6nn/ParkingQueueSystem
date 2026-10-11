package com.parking.ui;

import com.parking.models.Entrance;
import com.parking.models.EntranceRecord;
import com.parking.models.ParkingLot;
import com.parking.models.ParkingRecord;
import com.parking.models.VehicleTransaction;
import com.parking.optimization.OptimizationService;
import com.parking.optimization.OptimizationService.OptimizationRecommendation;
import com.parking.services.EntranceRecordService;
import com.parking.services.EntranceService;
import com.parking.services.ParkingLotService;
import com.parking.services.ParkingRecordService;
import com.parking.services.VehicleTransactionService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ParkingDashboard extends JFrame {

    private final EntranceService entranceService =
            new EntranceService();

    private final EntranceRecordService entranceRecordService =
            new EntranceRecordService();

    private final ParkingLotService parkingLotService =
            new ParkingLotService();

    private final ParkingRecordService parkingRecordService =
            new ParkingRecordService();

    private final OptimizationService optimizationService =
            new OptimizationService();

    private final VehicleTransactionService vehicleTransactionService =
            new VehicleTransactionService();

    private final JLabel occupancyLabel =
            new JLabel("Loading...");

    private final JLabel availableLabel =
            new JLabel("Loading...");

    private final JLabel capacityLabel =
            new JLabel("Loading...");

    private final JLabel parkingTimestampLabel =
            new JLabel("Last updated: --");

    private final JLabel statusLabel =
            new JLabel("Ready");

    private final DefaultTableModel tableModel =
            new DefaultTableModel(
                    new String[]{
                            "Entrance",
                            "Status",
                            "Active / Total Lanes",
                            "Queue (vehicles)",
                            "Wait (no new arrivals)",
                            "Queue in 5 min (constant rates)",
                            "Recommended Open Lanes",
                            "Recommended Entry Capacity (vehicles)",
                            "Last Queue Update"
                    }, 0) {

                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final DefaultTableModel vehicleTableModel =
            new DefaultTableModel(
                    new String[]{
                            "Transaction ID",
                            "Plate Number",
                            "Vehicle Type",
                            "Amount",
                            "Payment Status",
                            "Parking Status",
                            "Transaction Time",
                            "Exit Time"
                    }, 0) {

                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final javax.swing.Timer refreshTimer;

    ParkingDashboard() {
        ParkflowTheme.install();
        setTitle("Parkflow - Dashboard");
        setSize(1400, 650);
        setMinimumSize(new Dimension(1000, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBackground(ParkflowTheme.BACKGROUND);
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ParkflowTheme.NAVY);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(
                        0, 5, 0, 0, ParkflowTheme.GREEN),
                BorderFactory.createEmptyBorder(15, 18, 15, 18)));

        JPanel branding = new JPanel(new GridLayout(2, 1, 0, 3));
        branding.setOpaque(false);
        JLabel title = new JLabel("Parkflow");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Parking operations at a glance");
        subtitle.setForeground(new Color(204, 216, 229));
        branding.add(title);
        branding.add(subtitle);
        header.add(branding, BorderLayout.WEST);

        JLabel sectionLabel = new JLabel("ADMIN DASHBOARD");
        sectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sectionLabel.setForeground(new Color(176, 197, 216));
        header.add(sectionLabel, BorderLayout.EAST);
        mainPanel.add(header, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 15));
        centerPanel.setBackground(ParkflowTheme.BACKGROUND);

        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 12, 0));
        summaryPanel.setBackground(ParkflowTheme.BACKGROUND);

        summaryPanel.add(createSummaryCard(
                "Occupied Parking Spaces",
                occupancyLabel,
                parkingTimestampLabel,
                ParkflowTheme.NAVY));

        summaryPanel.add(createSummaryCard(
                "Available Parking Spaces",
                availableLabel,
                new JLabel("Spaces currently open"),
                ParkflowTheme.GREEN));

        summaryPanel.add(createSummaryCard(
                "Total Parking Capacity",
                capacityLabel,
                new JLabel("Configured spaces"),
                new Color(77, 139, 166)));

        // Entrance queues table
        JTable entranceTable = new JTable(tableModel);
        ParkflowTheme.styleTable(entranceTable);
        entranceTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        int[] columnWidths = {
                140, 90, 140, 110, 170, 140, 170, 240, 180
        };

        for (int i = 0; i < columnWidths.length; i++) {
            entranceTable.getColumnModel()
                    .getColumn(i)
                    .setPreferredWidth(columnWidths[i]);
        }

        JScrollPane tableScrollPane = new JScrollPane(entranceTable);

        tableScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ParkflowTheme.BORDER),
                        BorderFactory.createEmptyBorder(4, 4, 4, 4)),
                "  Entrance queues and recommendations  "));

        // Vehicle transactions table
        JTable vehicleTable = new JTable(vehicleTableModel);
        ParkflowTheme.styleTable(vehicleTable);
        vehicleTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        vehicleTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        int[] vehicleColumnWidths = {
                130, 130, 130, 110, 140, 140, 200, 200
        };

        for (int i = 0; i < vehicleColumnWidths.length; i++) {
            vehicleTable.getColumnModel()
                    .getColumn(i)
                    .setPreferredWidth(vehicleColumnWidths[i]);
        }

        // Payment status dropdown
        JComboBox<String> paymentStatusCombo = new JComboBox<>(
                new String[]{"PROCESSING", "PAID", "FAILED"});

        JButton updatePaymentStatusButton =
                new JButton("Update Payment Status");
        ParkflowTheme.styleButton(updatePaymentStatusButton, true);

        updatePaymentStatusButton.addActionListener(event -> {
            int selectedRow = vehicleTable.getSelectedRow();

            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please select a vehicle transaction first.",
                        "No Transaction Selected",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            int modelRow =
                    vehicleTable.convertRowIndexToModel(selectedRow);

            String transactionIdText =
                    vehicleTableModel.getValueAt(modelRow, 0).toString();

            // Table IDs are displayed as TXN-0001, TXN-0002, etc.
            int transactionId;

            try {
                transactionId = Integer.parseInt(
                        transactionIdText.replace("TXN-", ""));
            } catch (NumberFormatException exception) {
                JOptionPane.showMessageDialog(
                        this,
                        "The selected transaction ID is invalid.",
                        "Invalid Transaction",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            String newStatus =
                    (String) paymentStatusCombo.getSelectedItem();

            if (newStatus == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please select a payment status.");
                return;
            }

            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Change payment status for "
                            + transactionIdText
                            + " to " + newStatus + "?",
                    "Confirm Payment Status Update",
                    JOptionPane.YES_NO_OPTION);

            if (choice != JOptionPane.YES_OPTION) {
                return;
            }

            try {
                vehicleTransactionService.updatePaymentStatus(
                        transactionId,
                        newStatus);

                JOptionPane.showMessageDialog(
                        this,
                        "Payment status updated successfully.",
                        "Update Successful",
                        JOptionPane.INFORMATION_MESSAGE);

                refreshDashboard(true);

            } catch (SQLException | IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(
                        this,
                        exception.getMessage(),
                        "Update Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // Put the vehicle table and payment controls in the same panel.
        JPanel vehicleTransactionsPanel =
                new JPanel(new BorderLayout(8, 8));

        JScrollPane vehicleScrollPane =
                new JScrollPane(vehicleTable);

        vehicleScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ParkflowTheme.BORDER),
                        BorderFactory.createEmptyBorder(4, 4, 4, 4)),
                "  Vehicle transactions  "));

        JPanel paymentControlsPanel =
                new JPanel(new FlowLayout(FlowLayout.LEFT));

        paymentControlsPanel.add(new JLabel("Payment Status:"));
        paymentControlsPanel.add(paymentStatusCombo);
        paymentControlsPanel.add(updatePaymentStatusButton);

        vehicleTransactionsPanel.add(
                vehicleScrollPane,
                BorderLayout.CENTER);

        vehicleTransactionsPanel.add(
                paymentControlsPanel,
                BorderLayout.SOUTH);

        // Dashboard tabs
        JTabbedPane dashboardTabs = new JTabbedPane();

        dashboardTabs.addTab(
                "Entrance Queues & Optimization",
                tableScrollPane);

        dashboardTabs.addTab(
                "Vehicle Transactions",
                vehicleTransactionsPanel);

        JPanel tablePanel = new JPanel(new BorderLayout(0, 8));
        tablePanel.add(summaryPanel, BorderLayout.NORTH);
        tablePanel.add(dashboardTabs, BorderLayout.CENTER);

        centerPanel.add(tablePanel, BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Refresh button
        JButton refreshButton = new JButton("Refresh Data");
        ParkflowTheme.styleButton(refreshButton, true);

        refreshButton.addActionListener(
                event -> refreshDashboard(true));

        // Manual data entry button
        JButton manualEntryButton = new JButton("Manual Data Entry");
        ParkflowTheme.styleButton(manualEntryButton, false);

        manualEntryButton.addActionListener(event -> {
            refreshDashboard(false);
            ManualDataEntryFrame manualEntry =
                    new ManualDataEntryFrame(
                            () -> refreshDashboard(true));
            manualEntry.setVisible(true);
        });

        // Driver window button
        JButton driverWindowButton =
                new JButton("Open Driver Window");
        ParkflowTheme.styleButton(driverWindowButton, false);

        driverWindowButton.addActionListener(event -> {
            refreshDashboard(false);
            DriverWindow driverWindow =
                    new DriverWindow(() -> refreshDashboard(true));
            driverWindow.setVisible(true);
        });

        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setBackground(ParkflowTheme.BACKGROUND);

        buttonPanel.add(manualEntryButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(driverWindowButton);

        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setBackground(ParkflowTheme.BACKGROUND);

        footer.add(statusLabel, BorderLayout.CENTER);
        footer.add(buttonPanel, BorderLayout.EAST);

        mainPanel.add(footer, BorderLayout.SOUTH);

        setContentPane(mainPanel);

        // Refresh automatically every 3 seconds.
        refreshTimer = new javax.swing.Timer(
                3000,
                event -> refreshDashboard(false));

        refreshDashboard(false);
        refreshTimer.start();
    }

    private JPanel createSummaryCard(
            String heading,
            JLabel valueLabel,
            JLabel detailLabel,
            Color accent) {

        JPanel card = new JPanel(new GridLayout(3, 1, 4, 4));
        card.setBackground(ParkflowTheme.SURFACE);

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createMatteBorder(
                                        0, 4, 0, 0, accent),
                                BorderFactory.createLineBorder(
                                        ParkflowTheme.BORDER)),
                        BorderFactory.createEmptyBorder(11, 14, 11, 14)));

        JLabel headingLabel = new JLabel(heading);
        headingLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        headingLabel.setForeground(ParkflowTheme.MUTED);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        valueLabel.setForeground(ParkflowTheme.TEXT);
        detailLabel.setForeground(ParkflowTheme.MUTED);

        card.add(headingLabel);
        card.add(valueLabel);
        card.add(detailLabel);

        return card;
    }

    private void refreshDashboard(boolean showError) {
        statusLabel.setText("Loading parking data...");

        try {
            // Read parking lot configuration and occupancy.
            List<ParkingLot> parkingLots =
                    parkingLotService.findAll();

            ParkingRecord parkingRecord =
                    parkingRecordService.findLatest();

            boolean capacityWarning = false;

            if (parkingLots.isEmpty()) {
                occupancyLabel.setText("Not configured");
                availableLabel.setText("Not configured");
                capacityLabel.setText("Not configured");
                parkingTimestampLabel.setText(
                        "Configure the parking lot first.");

            } else if (parkingRecord == null) {
                capacityLabel.setText(
                        String.valueOf(
                                parkingLots.get(0).getTotalSpaces()));
                occupancyLabel.setText("No data");
                availableLabel.setText("No data");
                parkingTimestampLabel.setText(
                        "Save an initial occupancy record.");

            } else {
                int totalSpaces =
                        parkingLots.get(0).getTotalSpaces();
                capacityLabel.setText(String.valueOf(totalSpaces));

                int occupied =
                        parkingRecord.getOccupiedSpaces();

                occupancyLabel.setText(String.valueOf(occupied));

                parkingTimestampLabel.setText(
                        "Last occupancy record: "
                                + parkingRecord.getRecordedAt());

                if (occupied < 0 || occupied > totalSpaces) {
                    availableLabel.setText("Capacity exceeded");
                    capacityWarning = true;
                } else {
                    int available = totalSpaces - occupied;
                    availableLabel.setText(String.valueOf(available));
                }
            }

            // Refresh entrance queue information.
            tableModel.setRowCount(0);

            List<Entrance> entrances = entranceService.findAll();

            for (Entrance entrance : entrances) {
                Optional<EntranceRecord> latestRecord =
                        entranceRecordService.findLatestByEntranceId(
                                entrance.getId());

                String queue = "No data";
                String wait = "Unavailable";
                String predictedQueue = "Unavailable";
                String recommendedLanes = "Unavailable";
                String recommendedCapacity = "Unavailable";
                String queueTimestamp = "No records";

                if (latestRecord.isPresent()) {
                    EntranceRecord record = latestRecord.get();

                    queue = String.valueOf(record.getQueueLength());
                    queueTimestamp = record.getRecordedAt();
                }

                Optional<OptimizationRecommendation> result =
                        optimizationService.generateRecommendation(
                                entrance.getId(), 10, 5);

                if (result.isPresent()) {
                    OptimizationRecommendation recommendation =
                            result.get();

                    if (recommendation
                            .getPredictedQueueLength().isPresent()) {
                        predictedQueue = String.valueOf(
                                recommendation
                                        .getPredictedQueueLength()
                                        .getAsInt());
                    }

                    if (recommendation
                            .getEstimatedWaitMinutes().isPresent()) {
                        wait = String.format(
                                "%.1f minutes",
                                recommendation
                                        .getEstimatedWaitMinutes()
                                        .getAsDouble());
                    }

                    if (recommendation
                            .getRecommendedLanes().isPresent()) {
                        recommendedLanes = String.valueOf(
                                recommendation
                                        .getRecommendedLanes()
                                        .getAsInt());
                    }

                    if (recommendation
                            .getRecommendedCapacity().isPresent()) {
                        recommendedCapacity = String.valueOf(
                                recommendation
                                        .getRecommendedCapacity()
                                        .getAsInt());
                    }
                }

                tableModel.addRow(new Object[]{
                        entrance.getName(),
                        entrance.getStatus(),
                        entrance.getActiveLanes()
                                + " / " + entrance.getLaneCount(),
                        queue,
                        wait,
                        predictedQueue,
                        recommendedLanes,
                        recommendedCapacity,
                        queueTimestamp
                });
            }

            // Refresh vehicle transactions.
            vehicleTableModel.setRowCount(0);

            List<VehicleTransaction> transactions =
                    vehicleTransactionService.findAll();

            for (VehicleTransaction transaction : transactions) {
                vehicleTableModel.addRow(new Object[]{
                        "TXN-" + String.format(
                                "%04d", transaction.getId()),
                        transaction.getPlateNumber(),
                        transaction.getVehicleType(),
                        String.format(
                                "₱%.2f", transaction.getAmount()),
                        transaction.getPaymentStatus(),
                        transaction.getParkingStatus(),
                        transaction.getCreatedAt(),
                        transaction.getExitedAt() == null
                                ? "Not exited"
                                : transaction.getExitedAt()
                });
            }

            if (entrances.isEmpty()) {
                statusLabel.setText("No entrances configured.");
            } else if (capacityWarning) {
                statusLabel.setText(
                        "WARNING: Occupancy exceeds configured capacity. "
                                + "Review the configuration and recorded occupancy.");
            } else {
                statusLabel.setText(
                        "Parkflow is up to date  •  Auto-refreshes every 3 seconds");
            }

        } catch (SQLException | IllegalArgumentException
                 | IllegalStateException exception) {

            statusLabel.setText("Could not load dashboard data.");

            if (showError) {
                JOptionPane.showMessageDialog(
                        this,
                        "Unable to load parking data:\n"
                                + exception.getMessage(),
                        "Dashboard Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }

        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                LoginWindow loginWindow = new LoginWindow();
                loginWindow.setVisible(true);
            } catch (SQLException exception) {
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