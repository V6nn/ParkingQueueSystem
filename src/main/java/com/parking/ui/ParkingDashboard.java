
package com.parking.ui;

import com.parking.models.Entrance;
import com.parking.models.EntranceRecord;
import com.parking.models.ParkingLot;
import com.parking.models.ParkingRecord;
import com.parking.optimization.OptimizationService;
import com.parking.optimization.OptimizationService.OptimizationRecommendation;
import com.parking.services.EntranceRecordService;
import com.parking.services.EntranceService;
import com.parking.services.ParkingLotService;
import com.parking.services.ParkingRecordService;

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

    private final JLabel occupancyLabel =
            new JLabel("Loading...");

    private final JLabel availableLabel =
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
                            "Estimated Wait",
                            "Recommended Open Lanes",
                            "Recommended Entry Capacity (vehicles)",
                            "Last Queue Update"
                    }, 0) {

                @Override
                public boolean isCellEditable(
                        int row, int column) {
                    return false;
                }
            };

    private final javax.swing.Timer refreshTimer;

    public ParkingDashboard() {
        setTitle("Parking Queue System - Dashboard");
        setSize(1400, 650);
        setMinimumSize(new Dimension(1000, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel =
                new JPanel(new BorderLayout(12, 12));

        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(
                        20, 20, 20, 20));

        JLabel title =
                new JLabel("Parking Queue System Dashboard");

        title.setFont(new Font("Arial", Font.BOLD, 25));
        mainPanel.add(title, BorderLayout.NORTH);

        JPanel centerPanel =
                new JPanel(new BorderLayout(10, 15));

        JPanel summaryPanel =
                new JPanel(new GridLayout(1, 2, 12, 0));

        summaryPanel.add(createSummaryCard(
                "Occupied Parking Spaces",
                occupancyLabel,
                parkingTimestampLabel));

        summaryPanel.add(createSummaryCard(
                "Available Parking Spaces",
                availableLabel,
                new JLabel(
                        "Current capacity minus recorded occupancy")));

        JTable entranceTable = new JTable(tableModel);
        entranceTable.setRowHeight(28);
        entranceTable.setAutoResizeMode(
                JTable.AUTO_RESIZE_OFF);

        int[] columnWidths = {
                140, 90, 140, 110, 120, 170, 240, 180
        };

        for (int i = 0; i < columnWidths.length; i++) {
            entranceTable.getColumnModel()
                    .getColumn(i)
                    .setPreferredWidth(columnWidths[i]);
        }

        JScrollPane tableScrollPane =
                new JScrollPane(entranceTable);

        tableScrollPane.setBorder(
                BorderFactory.createTitledBorder(
                        "Entrance Queues and Optimization Recommendations"));

        JPanel tablePanel =
                new JPanel(new BorderLayout(0, 8));

        tablePanel.add(summaryPanel, BorderLayout.NORTH);
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);
        centerPanel.add(tablePanel, BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        JButton refreshButton =
                new JButton("Refresh Data");

        refreshButton.addActionListener(
                event -> refreshDashboard(true));

        JButton manualEntryButton =
                new JButton("Manual Data Entry");

        manualEntryButton.addActionListener(event -> {
            ManualDataEntryFrame manualEntry =
                    new ManualDataEntryFrame();
            manualEntry.setVisible(true);
        });

        JPanel buttonPanel =
                new JPanel(new FlowLayout(
                        FlowLayout.RIGHT, 8, 0));

        buttonPanel.add(manualEntryButton);
        buttonPanel.add(refreshButton);

        JPanel footer =
                new JPanel(new BorderLayout(10, 0));

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
            JLabel detailLabel) {

        JPanel card =
                new JPanel(new GridLayout(3, 1, 4, 4));

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                Color.LIGHT_GRAY),
                        BorderFactory.createEmptyBorder(
                                12, 15, 12, 15)));

        JLabel headingLabel = new JLabel(heading);
        headingLabel.setFont(
                new Font("Arial", Font.BOLD, 16));

        valueLabel.setFont(
                new Font("Arial", Font.BOLD, 25));

        card.add(headingLabel);
        card.add(valueLabel);
        card.add(detailLabel);

        return card;
    }

    private void refreshDashboard(boolean showError) {
        statusLabel.setText("Loading parking data...");

        try {
            // Read the current parking lot configuration.
            List<ParkingLot> parkingLots =
                    parkingLotService.findAll();

            ParkingRecord parkingRecord =
                    parkingRecordService.findLatest();

            boolean capacityWarning = false;

            if (parkingLots.isEmpty()) {
                occupancyLabel.setText("Not configured");
                availableLabel.setText("Not configured");
                parkingTimestampLabel.setText(
                        "Configure the parking lot first.");

            } else if (parkingRecord == null) {
                occupancyLabel.setText("No data");
                availableLabel.setText("No data");
                parkingTimestampLabel.setText(
                        "Save an initial occupancy record.");

            } else {
                int totalSpaces =
                        parkingLots.get(0).getTotalSpaces();

                int occupied =
                        parkingRecord.getOccupiedSpaces();

                occupancyLabel.setText(
                        String.valueOf(occupied));

                parkingTimestampLabel.setText(
                        "Last occupancy record: "
                                + parkingRecord.getRecordedAt());

                if (occupied > totalSpaces) {
                    availableLabel.setText(
                            "Capacity exceeded");

                    capacityWarning = true;
                } else {
                    int available = totalSpaces - occupied;

                    availableLabel.setText(
                            String.valueOf(available));
                }
            }

            tableModel.setRowCount(0);

            List<Entrance> entrances =
                    entranceService.findAll();

            for (Entrance entrance : entrances) {
                Optional<EntranceRecord> latestRecord =
                        entranceRecordService
                                .findLatestByEntranceId(
                                        entrance.getId());

                String queue = "No data";
                String wait = "Unavailable";
                String recommendedLanes = "Unavailable";
                String recommendedCapacity = "Unavailable";
                String queueTimestamp = "No records";

                if (latestRecord.isPresent()) {
                    EntranceRecord record =
                            latestRecord.get();

                    queue = String.valueOf(
                            record.getQueueLength());

                    queueTimestamp =
                            record.getRecordedAt();
                }

                Optional<OptimizationRecommendation> result =
                        optimizationService.generateRecommendation(
                                entrance.getId(), 10, 5);

                if (result.isPresent()) {
                    OptimizationRecommendation recommendation =
                            result.get();

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
                        recommendedLanes,
                        recommendedCapacity,
                        queueTimestamp
                });
            }

            if (entrances.isEmpty()) {
                statusLabel.setText(
                        "No entrances configured.");
            } else if (capacityWarning) {
                statusLabel.setText(
                        "WARNING: Occupancy exceeds configured "
                                + "capacity. Review the configuration "
                                + "and recorded occupancy.");
            } else {
                statusLabel.setText(
                        "Dashboard updated. Auto-refresh: 3 seconds.");
            }

        } catch (SQLException | IllegalArgumentException
                 | IllegalStateException exception) {

            statusLabel.setText(
                    "Could not load dashboard data.");

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
            ParkingDashboard dashboard =
                    new ParkingDashboard();

            dashboard.setVisible(true);
        });
    }
}
