package com.parking.ui;

import com.parking.models.EntranceRecord;
import com.parking.models.ExitRecord;
import com.parking.models.ParkingRecord;
import com.parking.services.EntranceRecordService;
import com.parking.services.ExitRecordService;
import com.parking.services.ParkingRecordService;
import com.parking.models.VehicleTransaction;
import com.parking.services.VehicleTransactionService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.parking.models.Entrance;
import com.parking.models.ParkingLot;
import com.parking.services.EntranceService;
import com.parking.services.ParkingLotService;
import com.parking.models.Exit;
import com.parking.services.ExitService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class ManualDataEntryFrame extends JFrame {

    private final Runnable onDataChanged;

    private final VehicleTransactionService vehicleTransactionService =
            new VehicleTransactionService();

    private JTextField vehicleEntryPlateField;
    private JComboBox<String> vehicleEntryTypeBox;

    private JTextField vehicleExitPlateField;
    private JComboBox<String> vehicleExitTypeBox;

    private final ParkingRecordService parkingRecordService =
        new ParkingRecordService();

    private final EntranceRecordService entranceRecordService =
            new EntranceRecordService();

    private final ExitRecordService exitRecordService =
            new ExitRecordService();

    private final ParkingLotService parkingLotService =
            new ParkingLotService();
    private final EntranceService entranceService =
            new EntranceService();

    private final ExitService exitService = new ExitService();

    private JTextField exitNameField;
    private JTextField exitLaneCountField;
    private JTextField exitActiveLanesField;
    private JComboBox<String> exitStatusBox;
    private JTable exitTable;
    private DefaultTableModel exitTableModel;
    private JLabel exitStatusLabel;
    private int selectedExitId = 0;

    private JTextField nameField;
    private JTextField capacityField;
    private JTextField startTimeField;
    private JTextField endTimeField;
    private JLabel statusLabel;
    private int parkingLotId = 0;

    private JTextField entranceNameField;
    private JTextField laneCountField;
    private JTextField activeLanesField;
    private JComboBox<String> entranceStatusBox;
    private JTable entranceTable;
    private DefaultTableModel entranceTableModel;
    private JLabel entranceStatusLabel;
    private int selectedEntranceId = 0;

    private JTextField occupiedSpacesField;
    private JTextField observationTimeField;

    private JComboBox<Entrance> observationEntranceBox;
    private JTextField queueLengthField;
    private JTextField vehiclesEnteredField;
    private JTextField averageServiceTimeField;
    private JTextField observationDurationMinutesField;

    private JComboBox<Exit> observationExitBox;
    private JTextField vehiclesExitedField;

    private JLabel observationStatusLabel;

    private void loadObservationOptions() {
        try {
            observationEntranceBox.removeAllItems();
            for (Entrance entrance : entranceService.findAll()) {
                observationEntranceBox.addItem(entrance);
            }

            observationExitBox.removeAllItems();
            for (Exit exit : exitService.findAll()) {
                observationExitBox.addItem(exit);
            }
        } catch (SQLException exception) {
            showError("Could not load observation options: "
                    + exception.getMessage());
        }
    }

    public ManualDataEntryFrame() {
        this(() -> { });
    }

    public ManualDataEntryFrame(Runnable onDataChanged) {
        if (onDataChanged == null) {
            throw new IllegalArgumentException(
                    "Data change callback cannot be null.");
        }
        this.onDataChanged = onDataChanged;

        ParkflowTheme.install();
        setTitle("Parkflow - Manual Data Entry");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 650);
        setMinimumSize(new Dimension(800, 550));
        setLocationRelativeTo(null);

        createInterface();
        loadParkingLot();
        loadEntrances();
        loadExits();
        loadObservationOptions();
    }

    private void createInterface() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(ParkflowTheme.BACKGROUND);
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(18, 20, 20, 20));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ParkflowTheme.NAVY);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(
                        0, 5, 0, 0, ParkflowTheme.GREEN),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        JPanel brandingPanel = new JPanel(new GridLayout(2, 1, 0, 3));
        brandingPanel.setOpaque(false);
        JLabel titleLabel = new JLabel("Parkflow");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 23));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel(
                "Manage your lot, entrances, exits, and observations.");
        subtitleLabel.setForeground(new Color(205, 220, 225));
        brandingPanel.add(titleLabel);
        brandingPanel.add(subtitleLabel);
        headerPanel.add(brandingPanel, BorderLayout.WEST);

        JLabel sectionLabel = new JLabel("MANUAL DATA ENTRY");
        sectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        sectionLabel.setForeground(new Color(176, 205, 210));
        headerPanel.add(sectionLabel, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Parking Lot", createParkingLotPanel());
        tabs.addTab("Entrances", createEntrancesPanel());
        tabs.addTab("Exits", createExitsPanel());
        tabs.addTab("Observations", createObservationsPanel());

        tabs.addChangeListener(event -> {
            int selectedIndex = tabs.getSelectedIndex();

            if (selectedIndex >= 0
                    && "Observations".equals(tabs.getTitleAt(selectedIndex))) {
                loadObservationOptions();
            }
        });

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(tabs, BorderLayout.CENTER);
        setContentPane(mainPanel);
    }

    // PARKING LOT TAB

    private JPanel createParkingLotPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(
                BorderFactory.createEmptyBorder(20, 15, 15, 15));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        nameField = new JTextField(25);
        capacityField = new JTextField(25);
        startTimeField = new JTextField("08:00", 25);
        endTimeField = new JTextField("22:00", 25);

        addFormRow(formPanel, gbc, 0,
                "Parking lot name:", nameField);
        addFormRow(formPanel, gbc, 1,
                "Total parking spaces:", capacityField);
        addFormRow(formPanel, gbc, 2,
                "Operating start (HH:mm):", startTimeField);
        addFormRow(formPanel, gbc, 3,
                "Operating end (HH:mm):", endTimeField);

        JButton saveButton = new JButton("Save Configuration");
        ParkflowTheme.styleButton(saveButton, true);
        saveButton.addActionListener(event -> saveParkingLot());

        JButton reloadButton = new JButton("Reload Saved Data");
        ParkflowTheme.styleButton(reloadButton, false);
        reloadButton.addActionListener(event -> loadParkingLot());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(saveButton);
        buttons.add(reloadButton);

        statusLabel = new JLabel("Enter the parking lot configuration.");

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 10));
        bottomPanel.add(buttons, BorderLayout.NORTH);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        panel.add(formPanel, BorderLayout.NORTH);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void saveParkingLot() {
        try {
            ParkingLot parkingLot = new ParkingLot(
                    parkingLotId,
                    nameField.getText().trim(),
                    Integer.parseInt(capacityField.getText().trim()),
                    startTimeField.getText().trim(),
                    endTimeField.getText().trim()
            );

            if (parkingLotId == 0) {
                parkingLotId = parkingLotService.save(parkingLot);
                statusLabel.setText("Parking lot configuration saved.");
            } else if (parkingLotService.update(parkingLot)) {
                statusLabel.setText("Parking lot configuration updated.");
            } else {
                showError("Parking lot not found. Reload saved data.");
                loadParkingLot();
                return;
            }

            notifyDataChanged();
            JOptionPane.showMessageDialog(
                    this, "Parking lot configuration saved successfully.");

        } catch (NumberFormatException exception) {
            showError("Total parking spaces must be a whole number.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception.getMessage());
        } catch (SQLException exception) {
            showError("Database error: " + exception.getMessage());
        }
    }

    private void loadParkingLot() {
        try {
            List<ParkingLot> parkingLots = parkingLotService.findAll();

            if (parkingLots.isEmpty()) {
                parkingLotId = 0;
                statusLabel.setText(
                        "No parking lot saved yet. Enter the configuration.");
                return;
            }

            ParkingLot lot = parkingLots.get(0);
            parkingLotId = lot.getId();
            nameField.setText(lot.getName());
            capacityField.setText(String.valueOf(lot.getTotalSpaces()));
            startTimeField.setText(lot.getOperatingStart());
            endTimeField.setText(lot.getOperatingEnd());
            statusLabel.setText(
                    "Loaded saved configuration. Saving will update it.");

        } catch (SQLException exception) {
            showError("Could not load parking lot: " + exception.getMessage());
        }
    }

    // ENTRANCES TAB

    private JPanel createEntrancesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        entranceNameField = new JTextField(18);
        laneCountField = new JTextField("2", 18);
        activeLanesField = new JTextField("2", 18);
        entranceStatusBox = new JComboBox<>(
                new String[]{"OPEN", "CLOSED"});

        entranceStatusBox.addActionListener(event -> {
            if ("CLOSED".equals(entranceStatusBox.getSelectedItem())) {
                activeLanesField.setText("0");
            } else if ("0".equals(activeLanesField.getText().trim())) {
                activeLanesField.setText(
                        laneCountField.getText().trim());
            }
        });

        addFormRow(form, gbc, 0, "Entrance name:", entranceNameField);
        addFormRow(form, gbc, 1, "Total lanes:", laneCountField);
        addFormRow(form, gbc, 2, "Active lanes:", activeLanesField);
        addFormRow(form, gbc, 3, "Status:", entranceStatusBox);

        JButton saveButton = new JButton("Add Entrance");
        ParkflowTheme.styleButton(saveButton, true);
        saveButton.addActionListener(event -> saveEntrance());

        JButton updateButton = new JButton("Update Selected");
        ParkflowTheme.styleButton(updateButton, false);
        updateButton.addActionListener(event -> updateEntrance());

        JButton clearButton = new JButton("Clear Form");
        ParkflowTheme.styleButton(clearButton, false);
        clearButton.addActionListener(event -> clearEntranceForm());

        JButton refreshButton = new JButton("Refresh List");
        ParkflowTheme.styleButton(refreshButton, false);
        refreshButton.addActionListener(event -> loadEntrances());

        JButton deleteButton = new JButton("Delete Selected");
        ParkflowTheme.styleButton(deleteButton, false);
        deleteButton.addActionListener(event -> deleteEntrance());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(saveButton);
        buttons.add(updateButton);
        buttons.add(clearButton);
        buttons.add(refreshButton);
        buttons.add(deleteButton);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        entranceTableModel = new DefaultTableModel(
                new Object[]{"ID", "Name", "Total Lanes",
                        "Active Lanes", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        entranceTable = new JTable(entranceTableModel);
        ParkflowTheme.styleTable(entranceTable);
        entranceTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        entranceTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()
                    && entranceTable.getSelectedRow() >= 0) {
                int row = entranceTable.getSelectedRow();

                selectedEntranceId =
                        (int) entranceTableModel.getValueAt(row, 0);
                entranceNameField.setText(
                        entranceTableModel.getValueAt(row, 1).toString());
                laneCountField.setText(
                        entranceTableModel.getValueAt(row, 2).toString());
                activeLanesField.setText(
                        entranceTableModel.getValueAt(row, 3).toString());
                entranceStatusBox.setSelectedItem(
                        entranceTableModel.getValueAt(row, 4).toString());
            }
        });

        entranceStatusLabel = new JLabel(
                "Select a row to edit an existing entrance.");

        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        bottom.add(new JScrollPane(entranceTable), BorderLayout.CENTER);
        bottom.add(entranceStatusLabel, BorderLayout.SOUTH);

        panel.add(top, BorderLayout.NORTH);
        panel.add(bottom, BorderLayout.CENTER);
        return panel;
    }

    private void saveEntrance() {
        try {
            Entrance entrance = readEntranceForm();
            entranceService.save(entrance);
            clearEntranceForm();
            loadEntrances();
            entranceStatusLabel.setText("Entrance added successfully.");
            notifyDataChanged();

        } catch (NumberFormatException exception) {
            showError("Lane counts must be whole numbers.");
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        } catch (SQLException exception) {
            showError("Could not save entrance: " + exception.getMessage());
        }
    }

    private void updateEntrance() {
        if (selectedEntranceId <= 0) {
            showError("Select an entrance from the table first.");
            return;
        }

        try {
            Entrance entrance = readEntranceForm();
            entrance.setId(selectedEntranceId);

            if (entranceService.update(entrance)) {
                loadEntrances();
                clearEntranceForm();
                entranceStatusLabel.setText(
                        "Entrance updated successfully.");
                notifyDataChanged();
            } else {
                showError("Entrance not found. Refresh the list.");
            }

        } catch (NumberFormatException exception) {
            showError("Lane counts must be whole numbers.");
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        } catch (SQLException exception) {
            showError("Could not update entrance: "
                    + exception.getMessage());
        }
    }

    private void deleteEntrance() {
        if (selectedEntranceId <= 0) {
            showError("Select an entrance from the table first.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Delete the selected entrance? This cannot be undone.",
                "Confirm Entrance Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            if (entranceService.delete(selectedEntranceId)) {
                clearEntranceForm();
                loadEntrances();
                loadObservationOptions();
                entranceStatusLabel.setText("Entrance deleted.");
                notifyDataChanged();
            } else {
                showError("Entrance not found. Refresh the list.");
            }
        } catch (SQLException exception) {
            showError("Could not delete entrance. Historical queue "
                    + "observations or recommendations may refer to it. "
                    + exception.getMessage());
        }
    }

    private Entrance readEntranceForm() {
        return new Entrance(
                selectedEntranceId,
                entranceNameField.getText().trim(),
                Integer.parseInt(laneCountField.getText().trim()),
                Integer.parseInt(activeLanesField.getText().trim()),
                entranceStatusBox.getSelectedItem().toString()
        );
    }

    private void loadEntrances() {
        try {
            entranceTableModel.setRowCount(0);

            for (Entrance entrance : entranceService.findAll()) {
                entranceTableModel.addRow(new Object[]{
                        entrance.getId(),
                        entrance.getName(),
                        entrance.getLaneCount(),
                        entrance.getActiveLanes(),
                        entrance.getStatus()
                });
            }

            entranceStatusLabel.setText(
                    "Saved entrances: " + entranceTableModel.getRowCount());

        } catch (SQLException exception) {
            showError("Could not load entrances: "
                    + exception.getMessage());
        }
    }

    private void clearEntranceForm() {
        selectedEntranceId = 0;
        entranceTable.clearSelection();
        entranceNameField.setText("");
        laneCountField.setText("2");
        activeLanesField.setText("2");
        entranceStatusBox.setSelectedItem("OPEN");
    }

    // SHARED UI HELPERS

    private void addFormRow(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String labelText,
            JComponent field) {

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private JPanel createExitsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        exitNameField = new JTextField(18);
        exitLaneCountField = new JTextField("2", 18);
        exitActiveLanesField = new JTextField("2", 18);
        exitStatusBox = new JComboBox<>(new String[]{"OPEN", "CLOSED"});

        exitStatusBox.addActionListener(event -> {
            if ("CLOSED".equals(exitStatusBox.getSelectedItem())) {
                exitActiveLanesField.setText("0");
            } else if ("0".equals(exitActiveLanesField.getText().trim())) {
                exitActiveLanesField.setText(exitLaneCountField.getText().trim());
            }
        });

        addFormRow(form, gbc, 0, "Exit name:", exitNameField);
        addFormRow(form, gbc, 1, "Total lanes:", exitLaneCountField);
        addFormRow(form, gbc, 2, "Active lanes:", exitActiveLanesField);
        addFormRow(form, gbc, 3, "Status:", exitStatusBox);

        JButton addButton = new JButton("Add Exit");
        ParkflowTheme.styleButton(addButton, true);
        addButton.addActionListener(event -> saveExit());

        JButton updateButton = new JButton("Update Selected");
        ParkflowTheme.styleButton(updateButton, false);
        updateButton.addActionListener(event -> updateExit());

        JButton clearButton = new JButton("Clear Form");
        ParkflowTheme.styleButton(clearButton, false);
        clearButton.addActionListener(event -> clearExitForm());

        JButton refreshButton = new JButton("Refresh List");
        ParkflowTheme.styleButton(refreshButton, false);
        refreshButton.addActionListener(event -> loadExits());

        JButton deleteButton = new JButton("Delete Selected");
        ParkflowTheme.styleButton(deleteButton, false);
        deleteButton.addActionListener(event -> deleteExit());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(clearButton);
        buttons.add(refreshButton);
        buttons.add(deleteButton);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        exitTableModel = new DefaultTableModel(
                new Object[]{"ID", "Name", "Total Lanes", "Active Lanes", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        exitTable = new JTable(exitTableModel);
        ParkflowTheme.styleTable(exitTable);
        exitTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        exitTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && exitTable.getSelectedRow() >= 0) {
                int row = exitTable.getSelectedRow();

                selectedExitId = (int) exitTableModel.getValueAt(row, 0);
                exitNameField.setText(exitTableModel.getValueAt(row, 1).toString());
                exitLaneCountField.setText(exitTableModel.getValueAt(row, 2).toString());
                exitActiveLanesField.setText(exitTableModel.getValueAt(row, 3).toString());
                exitStatusBox.setSelectedItem(exitTableModel.getValueAt(row, 4).toString());
            }
        });

        exitStatusLabel = new JLabel("Select a row to edit an existing exit.");

        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        bottom.add(new JScrollPane(exitTable), BorderLayout.CENTER);
        bottom.add(exitStatusLabel, BorderLayout.SOUTH);

        panel.add(top, BorderLayout.NORTH);
        panel.add(bottom, BorderLayout.CENTER);
        return panel;
    }

    private Exit readExitForm() {
        return new Exit(
                selectedExitId,
                exitNameField.getText().trim(),
                Integer.parseInt(exitLaneCountField.getText().trim()),
                Integer.parseInt(exitActiveLanesField.getText().trim()),
                exitStatusBox.getSelectedItem().toString()
        );
    }

    private void saveExit() {
        try {
            Exit exit = readExitForm();
            exitService.save(exit);
            clearExitForm();
            loadExits();
            exitStatusLabel.setText("Exit added successfully.");
            notifyDataChanged();

        } catch (NumberFormatException exception) {
            showError("Lane counts must be whole numbers.");
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        } catch (SQLException exception) {
            showError("Could not save exit: " + exception.getMessage());
        }
    }

    private void updateExit() {
        if (selectedExitId <= 0) {
            showError("Select an exit from the table first.");
            return;
        }

        try {
            Exit exit = readExitForm();
            exit.setId(selectedExitId);

            if (exitService.update(exit)) {
                loadExits();
                clearExitForm();
                exitStatusLabel.setText("Exit updated successfully.");
                notifyDataChanged();
            } else {
                showError("Exit not found. Refresh the list.");
            }

        } catch (NumberFormatException exception) {
            showError("Lane counts must be whole numbers.");
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        } catch (SQLException exception) {
            showError("Could not update exit: " + exception.getMessage());
        }
    }

    private void deleteExit() {
        if (selectedExitId <= 0) {
            showError("Select an exit from the table first.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Delete the selected exit? This cannot be undone.",
                "Confirm Exit Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            if (exitService.delete(selectedExitId)) {
                clearExitForm();
                loadExits();
                loadObservationOptions();
                exitStatusLabel.setText("Exit deleted.");
                notifyDataChanged();
            } else {
                showError("Exit not found. Refresh the list.");
            }
        } catch (SQLException exception) {
            showError("Could not delete exit. Historical exit records may "
                    + "refer to it. " + exception.getMessage());
        }
    }

    private void loadExits() {
        try {
            exitTableModel.setRowCount(0);

            for (Exit exit : exitService.findAll()) {
                exitTableModel.addRow(new Object[]{
                        exit.getId(),
                        exit.getName(),
                        exit.getLaneCount(),
                        exit.getActiveLanes(),
                        exit.getStatus()
                });
            }

            exitStatusLabel.setText("Saved exits: " + exitTableModel.getRowCount());

        } catch (SQLException exception) {
            showError("Could not load exits: " + exception.getMessage());
        }
    }

    private void clearExitForm() {
        selectedExitId = 0;
        exitTable.clearSelection();
        exitNameField.setText("");
        exitLaneCountField.setText("2");
        exitActiveLanesField.setText("2");
        exitStatusBox.setSelectedItem("OPEN");
    }
    
    private JPanel createObservationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTabbedPane observationTabs = new JTabbedPane();

        observationTabs.addTab(
                "Parking Occupancy", createOccupancyPanel());
        observationTabs.addTab(
                "Entrance Record", createEntranceObservationPanel());
        observationTabs.addTab(
                "Exit Record", createExitObservationPanel());
        observationTabs.addTab(
                "Vehicle Entry", createVehicleEntryPanel());
        observationTabs.addTab(
                "Vehicle Exit", createVehicleExitPanel());

        observationStatusLabel =
                new JLabel("Ready to record observations.");

        panel.add(observationTabs, BorderLayout.CENTER);
        panel.add(observationStatusLabel, BorderLayout.SOUTH);

        return panel;
    }

        private JPanel createVehicleEntryPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            vehicleEntryPlateField = new JTextField(20);
            vehicleEntryTypeBox = new JComboBox<>(
                    new String[]{"Car", "Motorcycle"});

            addFormRow(panel, gbc, 0,
                    "Plate number:", vehicleEntryPlateField);
            addFormRow(panel, gbc, 1,
                    "Vehicle type:", vehicleEntryTypeBox);

            JButton entryButton = new JButton("Record Vehicle Entry");
            ParkflowTheme.styleButton(entryButton, true);
            entryButton.addActionListener(event -> saveVehicleEntry());

            gbc.gridx = 1;
            gbc.gridy = 2;
            panel.add(entryButton, gbc);

            return panel;
        }

        private void saveVehicleEntry() {
            String plate = vehicleEntryPlateField.getText().trim();

            if (plate.isEmpty()) {
                showError("Enter the vehicle's plate number.");
                return;
            }

            String selectedType =
                    (String) vehicleEntryTypeBox.getSelectedItem();

            String vehicleType = "Car".equals(selectedType)
                    ? "CAR" : "MOTORCYCLE";

            try {
                VehicleTransaction transaction =
                        vehicleTransactionService.recordVehicleEntry(
                                plate, vehicleType);
                notifyDataChanged();

                observationStatusLabel.setText(
                        "Vehicle entry recorded: " + transaction.getPlateNumber());

                JOptionPane.showMessageDialog(
                        this,
                        "Vehicle entry recorded successfully."
                        + "\nPlate: " + transaction.getPlateNumber()
                        + "\nVehicle type: " + transaction.getVehicleType()
                        + "\nParking status: " + transaction.getParkingStatus(),
                        "Vehicle Entry",
                        JOptionPane.INFORMATION_MESSAGE);

                vehicleEntryPlateField.setText("");

            } catch (SQLException | IllegalArgumentException
                    | IllegalStateException exception) {
                showError("Could not record vehicle entry: "
                        + exception.getMessage());
            }
        }

    private JPanel createVehicleExitPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        vehicleExitPlateField = new JTextField(20);
        vehicleExitTypeBox = new JComboBox<>(
                new String[]{"Car", "Motorcycle"});

        addFormRow(panel, gbc, 0,
                "Plate number:", vehicleExitPlateField);
        addFormRow(panel, gbc, 1,
                "Vehicle type:", vehicleExitTypeBox);

        JButton exitButton = new JButton("Process Exit Payment");
        ParkflowTheme.styleButton(exitButton, true);
        exitButton.addActionListener(event -> saveVehicleExit());

        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(exitButton, gbc);

        JLabel notice = new JLabel(
                "Demo only: payment is simulated when the vehicle exits.");

        gbc.gridy = 3;
        panel.add(notice, gbc);

        return panel;
    }

    private void saveVehicleExit() {
        String plate = vehicleExitPlateField.getText().trim();

        if (plate.isEmpty()) {
            showError("Enter the vehicle's plate number.");
            return;
        }

        String selectedType =
                (String) vehicleExitTypeBox.getSelectedItem();

        String vehicleType = "Car".equals(selectedType)
                ? "CAR" : "MOTORCYCLE";

        try {
            VehicleTransaction activeTransaction = null;

            for (VehicleTransaction transaction
                    : vehicleTransactionService.findAll()) {

                if (transaction.getPlateNumber().equalsIgnoreCase(plate)
                        && "OCCUPIED".equals(
                                transaction.getParkingStatus())) {
                    activeTransaction = transaction;
                    break;
                }
            }

            if (activeTransaction == null) {
                showError("No occupied vehicle was found with plate "
                        + plate.toUpperCase() + ".");
                return;
            }

            if (!activeTransaction.getVehicleType().equals(vehicleType)) {
                showError("The selected vehicle type does not match "
                        + "the registered vehicle.");
                return;
            }

            int choice = JOptionPane.showConfirmDialog(
                    this,
                    String.format(
                            "Process simulated exit payment?%n%n"
                            + "Plate: %s%n"
                            + "Vehicle: %s%n"
                            + "Amount: PHP %.2f",
                            activeTransaction.getPlateNumber(),
                            activeTransaction.getVehicleType(),
                            activeTransaction.getAmount()),
                    "Confirm Vehicle Exit",
                    JOptionPane.YES_NO_OPTION);

            if (choice != JOptionPane.YES_OPTION) {
                return;
            }

            VehicleTransaction completed =
                    vehicleTransactionService.payAtExit(plate);
            notifyDataChanged();

            observationStatusLabel.setText(
                    "Vehicle exited: " + completed.getPlateNumber());

            JOptionPane.showMessageDialog(
                    this,
                    String.format(
                            "Vehicle exit completed!%n%n"
                            + "Plate: %s%n"
                            + "Payment: %s%n"
                            + "Parking status: %s",
                            completed.getPlateNumber(),
                            completed.getPaymentStatus(),
                            completed.getParkingStatus()),
                    "Vehicle Exit",
                    JOptionPane.INFORMATION_MESSAGE);

            vehicleExitPlateField.setText("");

        } catch (SQLException | IllegalArgumentException
                | IllegalStateException exception) {
            showError("Could not process vehicle exit: "
                    + exception.getMessage());
        }
    }
    private JPanel createOccupancyPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        occupiedSpacesField = new JTextField(20);
        observationTimeField = new JTextField(currentTimestamp(), 20);

        addFormRow(panel, gbc, 0, "Occupied spaces:", occupiedSpacesField);
        addFormRow(panel, gbc, 1, "Recorded at:", observationTimeField);

        JButton saveButton = new JButton("Save Occupancy");
        ParkflowTheme.styleButton(saveButton, true);
        saveButton.addActionListener(event -> saveOccupancy());

        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(saveButton, gbc);

        return panel;
    }

    private JPanel createEntranceObservationPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        observationEntranceBox = new JComboBox<>();
        queueLengthField = new JTextField("0", 20);
        vehiclesEnteredField = new JTextField("0", 20);
        averageServiceTimeField = new JTextField("", 20);
        observationDurationMinutesField = new JTextField("", 20);

        addFormRow(panel, gbc, 0, "Entrance:", observationEntranceBox);
        addFormRow(panel, gbc, 1, "Queue length (vehicles):", queueLengthField);
        addFormRow(panel, gbc, 2,
                "Vehicles entered during interval:", vehiclesEnteredField);
        addFormRow(panel, gbc, 3,
                "Average service time (seconds/vehicle, optional):",
                averageServiceTimeField);
        addFormRow(panel, gbc, 4,
                "Observation duration (minutes, optional for prediction):",
                observationDurationMinutesField);

        JButton saveButton = new JButton("Save Entrance Record");
        ParkflowTheme.styleButton(saveButton, true);
        saveButton.addActionListener(event -> saveEntranceObservation());

        gbc.gridx = 1;
        gbc.gridy = 5;
        panel.add(saveButton, gbc);

        return panel;
    }

    private JPanel createExitObservationPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        observationExitBox = new JComboBox<>();
        vehiclesExitedField = new JTextField("0", 20);

        addFormRow(panel, gbc, 0, "Exit:", observationExitBox);
        addFormRow(panel, gbc, 1, "Vehicles exited:", vehiclesExitedField);

        JButton saveButton = new JButton("Save Exit Record");
        ParkflowTheme.styleButton(saveButton, true);
        saveButton.addActionListener(event -> saveExitObservation());

        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(saveButton, gbc);

        return panel;
    }

    private String currentTimestamp() {
        return LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private void saveOccupancy() {
        try {
            List<ParkingLot> parkingLots = parkingLotService.findAll();

            if (parkingLots.isEmpty()) {
                showError("Configure the parking lot before recording occupancy.");
                return;
            }

            int totalSpaces = parkingLots.get(0).getTotalSpaces();
            int occupied = Integer.parseInt(
                    occupiedSpacesField.getText().trim());

            int available = totalSpaces - occupied;

            ParkingRecord record = new ParkingRecord(
                    0,
                    observationTimeField.getText().trim(),
                    occupied,
                    available
            );

            parkingRecordService.save(record, totalSpaces);

            notifyDataChanged();
            observationStatusLabel.setText(
                    "Occupancy saved. Available spaces: " + available);
            JOptionPane.showMessageDialog(
                    this, "Occupancy record saved successfully.");

        } catch (NumberFormatException exception) {
            showError("Occupied spaces must be a whole number.");
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        } catch (SQLException exception) {
            showError("Could not save occupancy: " + exception.getMessage());
        }
    }

    private void saveEntranceObservation() {
        try {
            Entrance entrance =
                    (Entrance) observationEntranceBox.getSelectedItem();

            if (entrance == null) {
                showError("Add an entrance before recording observations.");
                return;
            }

            EntranceRecord record = new EntranceRecord(
                    0,
                    entrance.getId(),
                    currentTimestamp(),
                    Integer.parseInt(queueLengthField.getText().trim()),
                    Integer.parseInt(vehiclesEnteredField.getText().trim()),
                    averageServiceTimeField.getText().isBlank()
                            ? null
                            : Double.parseDouble(
                                    averageServiceTimeField.getText().trim()),
                    observationDurationMinutesField.getText().isBlank()
                            ? null
                            : Double.parseDouble(
                                    observationDurationMinutesField
                                            .getText().trim())
            );

            entranceRecordService.saveAndUpdateOccupancy(record);

            notifyDataChanged();
            observationStatusLabel.setText("Entrance observation saved.");
            JOptionPane.showMessageDialog(
                    this, "Entrance record saved successfully.");

        } catch (NumberFormatException exception) {
            showError("Queue length and vehicles entered must be whole numbers. "
                    + "Service time must be a number.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception.getMessage());
        } catch (SQLException exception) {
            showError("Could not save entrance record: "
                    + exception.getMessage());
        }
    }

    private void saveExitObservation() {
        try {
            Exit exit = (Exit) observationExitBox.getSelectedItem();

            if (exit == null) {
                showError("Add an exit before recording observations.");
                return;
            }

            List<ParkingLot> parkingLots = parkingLotService.findAll();

            if (parkingLots.isEmpty()) {
                showError("Configure the parking lot first.");
                return;
            }

            int totalSpaces = parkingLots.get(0).getTotalSpaces();

            int vehiclesExited = Integer.parseInt(
                    vehiclesExitedField.getText().trim());

            ExitRecord record = new ExitRecord(
                    0,
                    exit.getId(),
                    currentTimestamp(),
                    vehiclesExited
            );

            exitRecordService.saveAndUpdateOccupancy(
                    record, totalSpaces);

            notifyDataChanged();
            observationStatusLabel.setText(
                    "Exit saved. Vehicles exited: " + vehiclesExited);

            JOptionPane.showMessageDialog(
                    this,
                    "Exit record saved and parking occupancy updated.");

        } catch (NumberFormatException exception) {
            showError("Vehicles exited must be a whole number.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception.getMessage());
        } catch (SQLException exception) {
            showError("Could not save exit record: "
                    + exception.getMessage());
        }
    }

    private void notifyDataChanged() {
        onDataChanged.run();
    }
}