package com.parking.ui;

import com.parking.models.Entrance;
import com.parking.models.EntranceRecord;
import com.parking.models.ParkingRecord;
import com.parking.optimization.OptimizationService;
import com.parking.optimization.OptimizationService.OptimizationRecommendation;
import com.parking.services.EntranceRecordService;
import com.parking.services.EntranceService;
import com.parking.services.ParkingRecordService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ParkingDashboard extends JFrame {

    private final EntranceService entranceService = new EntranceService();
    private final EntranceRecordService entranceRecordService =
            new EntranceRecordService();
    private final ParkingRecordService parkingRecordService =
            new ParkingRecordService();
    private final OptimizationService optimizationService =
            new OptimizationService();

    private final JLabel occupancyLabel = new JLabel("Loading...");
    private final JLabel availableLabel = new JLabel("Loading...");
    private final JLabel parkingTimestampLabel =
            new JLabel("Last updated: --");
    private final JLabel statusLabel = new JLabel("Ready");

    private final DefaultTableModel tableModel =
            new DefaultTableModel(
                    new String[]{
                            "Entrance", "Status", "Active / Total Lanes",
                            "Queue", "Estimated Wait", "Recommended Lanes",
                            "Recommended Capacity", "Last Queue Update"
                    }, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    public ParkingDashboard() {
        setTitle("Parking Queue System - Dashboard");
        setSize(1200, 650);
        setMinimumSize(new Dimension(900, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Parking Queue System Dashboard");
        title.setFont(new Font("Arial", Font.BOLD, 25));
        mainPanel.add(title, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 15));

        JPanel summaryPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        summaryPanel.add(createSummaryCard(
                "Occupied Parking Spaces",
                occupancyLabel,
                parkingTimestampLabel));
        summaryPanel.add(createSummaryCard(
                "Available Parking Spaces",
                availableLabel,
                new JLabel("Based on the latest parking record")));

        JTable entranceTable = new JTable(tableModel);
        entranceTable.setRowHeight(28);
        entranceTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JScrollPane tableScrollPane = new JScrollPane(entranceTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder(
                "Entrance Queues and Optimization Recommendations"));

        JPanel tablePanel = new JPanel(new BorderLayout(0, 8));
        tablePanel.add(summaryPanel, BorderLayout.NORTH);
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);

        centerPanel.add(tablePanel, BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh Data");
        refreshButton.addActionListener(event -> refreshDashboard());

        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.add(statusLabel, BorderLayout.CENTER);
        footer.add(refreshButton, BorderLayout.EAST);

        mainPanel.add(footer, BorderLayout.SOUTH);
        setContentPane(mainPanel);

        refreshDashboard();
    }

    private JPanel createSummaryCard(
            String heading, JLabel valueLabel, JLabel detailLabel) {

        JPanel card = new JPanel(new GridLayout(3, 1, 4, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));

        JLabel headingLabel = new JLabel(heading);
        headingLabel.setFont(new Font("Arial", Font.BOLD, 16));

        valueLabel.setFont(new Font("Arial", Font.BOLD, 25));

        card.add(headingLabel);
        card.add(valueLabel);
        card.add(detailLabel);

        return card;
    }

    private void refreshDashboard() {
        statusLabel.setText("Loading parking data...");
        tableModel.setRowCount(0);

        try {
            ParkingRecord parkingRecord = parkingRecordService.findLatest();

            if (parkingRecord == null) {
                occupancyLabel.setText("No data available");
                availableLabel.setText("No data available");
                parkingTimestampLabel.setText("No parking records found");
            } else {
                occupancyLabel.setText(
                        String.valueOf(parkingRecord.getOccupiedSpaces()));
                availableLabel.setText(
                        String.valueOf(parkingRecord.getAvailableSpaces()));
                parkingTimestampLabel.setText(
                        "Last updated: " + parkingRecord.getRecordedAt());
            }

            List<Entrance> entrances = entranceService.findAll();

            if (entrances.isEmpty()) {
                statusLabel.setText(
                        "No entrances found. Check the database records.");
                return;
            }

            for (Entrance entrance : entrances) {
                Optional<EntranceRecord> latestRecord =
                        entranceRecordService.findLatestByEntranceId(
                                entrance.getId());

                String queue = "No data";
                String wait = "Unavailable";
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

                    if (recommendation.getEstimatedWaitMinutes().isPresent()) {
                        wait = String.format("%.1f minutes",
                                recommendation.getEstimatedWaitMinutes()
                                        .getAsDouble());
                    }

                    if (recommendation.getRecommendedLanes().isPresent()) {
                        recommendedLanes = String.valueOf(
                                recommendation.getRecommendedLanes()
                                        .getAsInt());
                    }

                    if (recommendation.getRecommendedCapacity().isPresent()) {
                        recommendedCapacity = String.valueOf(
                                recommendation.getRecommendedCapacity()
                                        .getAsInt());
                    }
                }

                tableModel.addRow(new Object[]{
                        entrance.getName(),
                        entrance.getStatus(),
                        entrance.getActiveLanes() + " / "
                                + entrance.getLaneCount(),
                        queue,
                        wait,
                        recommendedLanes,
                        recommendedCapacity,
                        queueTimestamp
                });
            }

            statusLabel.setText(
                    "Dashboard updated. Displaying saved database records.");

        } catch (SQLException | IllegalArgumentException exception) {
            statusLabel.setText("Could not load dashboard data.");
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load parking data:\n"
                            + exception.getMessage(),
                    "Dashboard Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ParkingDashboard dashboard = new ParkingDashboard();
            dashboard.setVisible(true);
        });
    }
}