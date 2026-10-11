package com.parking.services;

import com.parking.database.DatabaseConnection;
import com.parking.models.VehicleTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VehicleTransactionService {

    // Demo fees only. These are not real parking rates.
    public static final double CAR_FEE = 50.00;
    public static final double MOTORCYCLE_FEE = 30.00;

    public VehicleTransaction createReservation(
            String plateNumber,
            String vehicleType,
            String scheduledArrival) throws SQLException {

        String plate = normalizePlate(plateNumber);
        String type = normalizeVehicleType(vehicleType);

        if (scheduledArrival == null
                || scheduledArrival.isBlank()) {
            throw new IllegalArgumentException(
                    "Select a scheduled arrival date and time.");
        }

        LocalDateTime arrival;

        try {
            arrival = LocalDateTime.parse(scheduledArrival);
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "The scheduled arrival date and time is invalid.");
        }

        if (arrival.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Scheduled arrival cannot be in the past.");
        }

        double amount = getFee(type);
        String now = LocalDateTime.now().toString();

        try (Connection connection = DatabaseConnection.connect()) {
            connection.setAutoCommit(false);

            try {
                int total = getTotalSpaces(connection);
                int occupied = getOccupiedSpaces(connection);
                int reserved = getReservedSpaces(connection);
                validateOccupancy(occupied, total);

                if (occupied + reserved >= total) {
                    throw new IllegalArgumentException(
                            "No parking spaces are available for reservation.");
                }

                if (hasActivePlate(connection, plate)) {
                    throw new IllegalArgumentException(
                            "This plate already has an active reservation "
                                    + "or parking record.");
                }

                int id = insertTransaction(
                        connection, plate, type, amount,
                        "PROCESSING", "RESERVED", now,
                        arrival.toString(), null, null);

                connection.commit();

                return new VehicleTransaction(
                        id, plate, type, amount,
                        "PROCESSING", "RESERVED", now,
                        arrival.toString(), null, null);

            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    /*
     * Compatibility method for the old DriverWindow.
     * Despite its old name, this no longer charges the driver.
     * Replace the old DriverWindow flow in the next step.
     */
    public VehicleTransaction createPaidReservation(
            String plateNumber,
            String vehicleType) throws SQLException {

        return createReservation(
                plateNumber,
                vehicleType,
                LocalDateTime.now().plusMinutes(5).toString());
    }

    /*
     * Registers a walk-in who has arrived and is admitted immediately.
     * No payment is collected here.
     */
    public VehicleTransaction registerWalkIn(
            String plateNumber,
            String vehicleType) throws SQLException {

        return recordVehicleEntry(plateNumber, vehicleType);
    }

    /*
     * Records vehicle entry. An existing reservation is converted
     * to OCCUPIED; a vehicle without a reservation is treated as
     * a walk-in.
     */
    public VehicleTransaction recordVehicleEntry(
            String plateNumber,
            String vehicleType) throws SQLException {

        String plate = normalizePlate(plateNumber);
        String type = normalizeVehicleType(vehicleType);
        String now = LocalDateTime.now().toString();

        try (Connection connection = DatabaseConnection.connect()) {
            connection.setAutoCommit(false);

            try {
                int total = getTotalSpaces(connection);
                int occupied = getOccupiedSpaces(connection);
                validateOccupancy(occupied, total);

                VehicleTransaction active =
                        findActiveByPlate(connection, plate);

                if (active != null
                        && "OCCUPIED".equals(active.getParkingStatus())) {
                    throw new IllegalArgumentException(
                            "This vehicle is already marked as occupied.");
                }

                if (occupied >= total) {
                    throw new IllegalArgumentException(
                            "The parking lot is full.");
                }

                int id;
                double amount = getFee(type);
                String scheduledArrival = null;

                if (active != null) {
                    if (!active.getVehicleType().equals(type)) {
                        throw new IllegalArgumentException(
                                "Vehicle type does not match the reservation.");
                    }

                    id = active.getId();
                    scheduledArrival = active.getScheduledArrival();

                    try (PreparedStatement statement =
                                 connection.prepareStatement("""
                                         UPDATE vehicle_transactions
                                         SET parking_status = 'OCCUPIED',
                                             entered_at = ?
                                         WHERE id = ?
                                           AND parking_status = 'RESERVED'
                                         """)) {

                        statement.setString(1, now);
                        statement.setInt(2, id);

                        if (statement.executeUpdate() != 1) {
                            throw new IllegalArgumentException(
                                    "The reservation could not be checked in.");
                        }
                    }

                    amount = active.getAmount();

                } else {
                    if (hasActivePlate(connection, plate)) {
                        throw new IllegalArgumentException(
                                "This plate already has an active record.");
                    }

                    id = insertTransaction(
                            connection, plate, type, amount,
                            "PROCESSING", "OCCUPIED", now,
                            null, now, null);
                }

                saveOccupancySnapshot(
                        connection, total, occupied + 1, now);

                connection.commit();

                return new VehicleTransaction(
                        id, plate, type, amount,
                        active == null
                                ? "PROCESSING"
                                : active.getPaymentStatus(),
                        "OCCUPIED",
                        active == null ? now : active.getCreatedAt(),
                        scheduledArrival, now, null);

            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    /*
     * Simulates successful payment at exit.
     * The vehicle becomes AVAILABLE only after payment succeeds.
     */
    public VehicleTransaction payAtExit(
            String plateNumber) throws SQLException {

        String plate = normalizePlate(plateNumber);
        String now = LocalDateTime.now().toString();

        try (Connection connection = DatabaseConnection.connect()) {
            connection.setAutoCommit(false);

            try {
                int total = getTotalSpaces(connection);
                int occupied = getOccupiedSpaces(connection);
                validateOccupancy(occupied, total);

                VehicleTransaction active =
                        findActiveByPlate(connection, plate);

                if (active == null
                        || !"OCCUPIED".equals(
                                active.getParkingStatus())) {
                    throw new IllegalArgumentException(
                            "No occupied vehicle was found with this plate.");
                }

                if (occupied <= 0) {
                    throw new IllegalArgumentException(
                            "Parking occupancy is inconsistent. "
                                    + "Check the occupancy records first.");
                }

                try (PreparedStatement statement =
                             connection.prepareStatement("""
                                     UPDATE vehicle_transactions
                                     SET payment_status = 'PAID',
                                         parking_status = 'AVAILABLE',
                                         exited_at = ?
                                     WHERE id = ?
                                       AND parking_status = 'OCCUPIED'
                                     """)) {

                    statement.setString(1, now);
                    statement.setInt(2, active.getId());

                    if (statement.executeUpdate() != 1) {
                        throw new IllegalArgumentException(
                                "The vehicle exit could not be completed.");
                    }
                }

                saveOccupancySnapshot(
                        connection, total, occupied - 1, now);

                connection.commit();

                return new VehicleTransaction(
                        active.getId(),
                        active.getPlateNumber(),
                        active.getVehicleType(),
                        active.getAmount(),
                        "PAID",
                        "AVAILABLE",
                        active.getCreatedAt(),
                        active.getScheduledArrival(),
                        active.getEnteredAt(),
                        now);

            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public VehicleTransaction updatePaymentStatus(
            int transactionId,
            String newPaymentStatus) throws SQLException {

        if (newPaymentStatus == null) {
            throw new IllegalArgumentException(
                    "Select a payment status.");
        }

        String status = newPaymentStatus.trim().toUpperCase(Locale.ROOT);

        if (!status.equals("PROCESSING")
                && !status.equals("PAID")
                && !status.equals("FAILED")) {
            throw new IllegalArgumentException(
                    "Invalid payment status.");
        }

        try (Connection connection = DatabaseConnection.connect()) {
            connection.setAutoCommit(false);

            try {
                VehicleTransaction transaction = null;

                String selectSql = """
                        SELECT id, plate_number, vehicle_type, amount,
                            payment_status, parking_status, created_at,
                            scheduled_arrival, entered_at, exited_at
                        FROM vehicle_transactions
                        WHERE id = ?
                        """;

                try (PreparedStatement statement =
                            connection.prepareStatement(selectSql)) {
                    statement.setInt(1, transactionId);

                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            transaction = mapTransaction(result);
                        }
                    }
                }

                if (transaction == null) {
                    throw new IllegalArgumentException(
                            "Transaction not found.");
                }

                if ("AVAILABLE".equals(transaction.getParkingStatus())
                        && !"PAID".equals(status)) {
                    throw new IllegalArgumentException(
                            "A completed vehicle exit must remain PAID.");
                }

                if ("OCCUPIED".equals(transaction.getParkingStatus())
                        && "PAID".equals(status)) {
                    throw new IllegalArgumentException(
                            "Use the vehicle exit process to record payment "
                                    + "and release the parking space.");
                }

                if ("RESERVED".equals(transaction.getParkingStatus())
                        && "PAID".equals(status)) {
                    throw new IllegalArgumentException(
                            "Reservations are paid at exit, not in advance.");
                }

                try (PreparedStatement statement =
                            connection.prepareStatement("""
                                    UPDATE vehicle_transactions
                                    SET payment_status = ?
                                    WHERE id = ?
                                    """)) {
                    statement.setString(1, status);
                    statement.setInt(2, transactionId);
                    statement.executeUpdate();
                }

                connection.commit();

                return new VehicleTransaction(
                        transaction.getId(),
                        transaction.getPlateNumber(),
                        transaction.getVehicleType(),
                        transaction.getAmount(),
                        status,
                        transaction.getParkingStatus(),
                        transaction.getCreatedAt(),
                        transaction.getScheduledArrival(),
                        transaction.getEnteredAt(),
                        transaction.getExitedAt());

            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public List<VehicleTransaction> findAll()
            throws SQLException {

        String sql = """
                SELECT id, plate_number, vehicle_type, amount,
                       payment_status, parking_status, created_at,
                       scheduled_arrival, entered_at, exited_at
                FROM vehicle_transactions
                ORDER BY id DESC
                """;

        List<VehicleTransaction> transactions = new ArrayList<>();

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                transactions.add(mapTransaction(result));
            }
        }

        return transactions;
    }

    private VehicleTransaction mapTransaction(ResultSet result)
            throws SQLException {

        return new VehicleTransaction(
                result.getInt("id"),
                result.getString("plate_number"),
                result.getString("vehicle_type"),
                result.getDouble("amount"),
                result.getString("payment_status"),
                result.getString("parking_status"),
                result.getString("created_at"),
                result.getString("scheduled_arrival"),
                result.getString("entered_at"),
                result.getString("exited_at"));
    }

    private int insertTransaction(
            Connection connection,
            String plate,
            String type,
            double amount,
            String paymentStatus,
            String parkingStatus,
            String createdAt,
            String scheduledArrival,
            String enteredAt,
            String exitedAt) throws SQLException {

        String sql = """
                INSERT INTO vehicle_transactions (
                    plate_number, vehicle_type, amount,
                    payment_status, parking_status, created_at,
                    scheduled_arrival, entered_at, exited_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, plate);
            statement.setString(2, type);
            statement.setDouble(3, amount);
            statement.setString(4, paymentStatus);
            statement.setString(5, parkingStatus);
            statement.setString(6, createdAt);
            statement.setString(7, scheduledArrival);
            statement.setString(8, enteredAt);
            statement.setString(9, exitedAt);

            statement.executeUpdate();
        }

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             "SELECT last_insert_rowid()");
             ResultSet result = statement.executeQuery()) {

            if (result.next()) {
                return result.getInt(1);
            }
        }

        throw new SQLException("Could not retrieve transaction ID.");
    }

    private VehicleTransaction findActiveByPlate(
            Connection connection, String plate) throws SQLException {

        String sql = """
                SELECT id, plate_number, vehicle_type, amount,
                       payment_status, parking_status, created_at,
                       scheduled_arrival, entered_at, exited_at
                FROM vehicle_transactions
                WHERE plate_number = ?
                  AND parking_status IN ('RESERVED', 'OCCUPIED')
                LIMIT 1
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, plate);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapTransaction(result) : null;
            }
        }
    }

    private boolean hasActivePlate(
            Connection connection, String plate) throws SQLException {

        return findActiveByPlate(connection, plate) != null;
    }

    private int getTotalSpaces(Connection connection)
            throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement("""
                             SELECT total_spaces
                             FROM parking_lot
                             ORDER BY id
                             LIMIT 1
                             """);
             ResultSet result = statement.executeQuery()) {

            if (!result.next()) {
                throw new IllegalArgumentException(
                        "Configure the parking lot before accepting vehicles.");
            }

            return result.getInt("total_spaces");
        }
    }

    private int getOccupiedSpaces(Connection connection)
            throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement("""
                             SELECT occupied_spaces
                             FROM parking_records
                             ORDER BY datetime(recorded_at) DESC, id DESC
                             LIMIT 1
                             """);
             ResultSet result = statement.executeQuery()) {

            if (result.next()) {
                return result.getInt("occupied_spaces");
            }
        }

        try (PreparedStatement statement =
                     connection.prepareStatement("""
                             SELECT COUNT(*)
                             FROM vehicle_transactions
                             WHERE parking_status = 'OCCUPIED'
                             """);
             ResultSet result = statement.executeQuery()) {

            result.next();
            return result.getInt(1);
        }
    }

    private int getReservedSpaces(Connection connection)
            throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement("""
                             SELECT COUNT(*)
                             FROM vehicle_transactions
                             WHERE parking_status = 'RESERVED'
                             """);
             ResultSet result = statement.executeQuery()) {

            result.next();
            return result.getInt(1);
        }
    }

    private void validateOccupancy(int occupiedSpaces, int totalSpaces) {
        if (occupiedSpaces < 0 || occupiedSpaces > totalSpaces) {
            throw new IllegalStateException(
                    "Current occupancy is outside the configured capacity. "
                            + "Correct it before processing vehicle transactions.");
        }
    }

    private void saveOccupancySnapshot(
            Connection connection,
            int totalSpaces,
            int occupiedSpaces,
            String timestamp) throws SQLException {

        if (occupiedSpaces < 0 || occupiedSpaces > totalSpaces) {
            throw new IllegalArgumentException(
                    "Updated occupancy is outside the parking capacity.");
        }

        try (PreparedStatement statement =
                     connection.prepareStatement("""
                             INSERT INTO parking_records (
                                 recorded_at, occupied_spaces, available_spaces
                             )
                             VALUES (?, ?, ?)
                             """)) {

            statement.setString(1, timestamp);
            statement.setInt(2, occupiedSpaces);
            statement.setInt(3, totalSpaces - occupiedSpaces);
            statement.executeUpdate();
        }
    }

    private double getFee(String type) {
        return "CAR".equals(type) ? CAR_FEE : MOTORCYCLE_FEE;
    }

    private String normalizePlate(String plateNumber) {
        if (plateNumber == null) {
            throw new IllegalArgumentException(
                    "Enter a plate number.");
        }

        String plate = plateNumber.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", "");

        if (!plate.matches("[A-Z0-9-]{3,12}")
                || !plate.matches(".*[A-Z].*")
                || !plate.matches(".*[0-9].*")) {
            throw new IllegalArgumentException(
                    "Enter a valid plate using 3–12 letters, "
                            + "numbers, or hyphens, including at least "
                            + "one letter and one number.");
        }

        return plate;
    }

    private String normalizeVehicleType(String vehicleType) {
        if (vehicleType == null) {
            throw new IllegalArgumentException(
                    "Select a vehicle type.");
        }

        String type = vehicleType.trim().toUpperCase(Locale.ROOT);

        if (!type.equals("CAR") && !type.equals("MOTORCYCLE")) {
            throw new IllegalArgumentException(
                    "Vehicle type must be Car or Motorcycle.");
        }

        return type;
    }
}