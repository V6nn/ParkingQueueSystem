
package com.parking.services;

import com.parking.database.DatabaseConnection;
import com.parking.models.EntranceRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EntranceRecordService {

    public void save(EntranceRecord record) throws SQLException {
        validate(record);

        String sql = """
                INSERT INTO entrance_records
                (entrance_id, recorded_at, queue_length,
                 vehicles_entered, average_service_time)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, record.getEntranceId());
            statement.setString(2, record.getRecordedAt());
            statement.setInt(3, record.getQueueLength());
            statement.setInt(4, record.getVehiclesEntered());
            statement.setDouble(5, record.getAverageServiceTime());

            statement.executeUpdate();

            try (PreparedStatement idStatement =
                         connection.prepareStatement(
                                 "SELECT last_insert_rowid()");
                 ResultSet resultSet = idStatement.executeQuery()) {

                if (resultSet.next()) {
                    record.setId(resultSet.getInt(1));
                }
            }
        }
    }

    public void saveAndUpdateOccupancy(EntranceRecord record)
            throws SQLException {

        validate(record);

        String capacitySql = """
                SELECT total_spaces
                FROM parking_lot
                ORDER BY id
                LIMIT 1
                """;

        String occupancySql = """
                SELECT occupied_spaces
                FROM parking_records
                ORDER BY recorded_at DESC, id DESC
                LIMIT 1
                """;

        String entranceSql = """
                INSERT INTO entrance_records
                (entrance_id, recorded_at, queue_length,
                vehicles_entered, average_service_time)
                VALUES (?, ?, ?, ?, ?)
                """;

        String parkingSql = """
                INSERT INTO parking_records
                (recorded_at, occupied_spaces, available_spaces)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.connect()) {
            connection.setAutoCommit(false);

            try {
                int totalSpaces;

                try (PreparedStatement statement =
                            connection.prepareStatement(capacitySql);
                    ResultSet result = statement.executeQuery()) {

                    if (!result.next()) {
                        throw new IllegalStateException(
                                "Configure the parking lot first.");
                    }

                    totalSpaces = result.getInt("total_spaces");
                }

                int occupiedSpaces;

                try (PreparedStatement statement =
                            connection.prepareStatement(occupancySql);
                    ResultSet result = statement.executeQuery()) {

                    if (!result.next()) {
                        throw new IllegalStateException(
                                "Save an initial parking occupancy record "
                                        + "before recording vehicle entries.");
                    }

                    occupiedSpaces = result.getInt("occupied_spaces");
                }

                if (occupiedSpaces < 0 || occupiedSpaces > totalSpaces) {
                    throw new IllegalStateException(
                            "Current occupancy is outside the configured "
                                    + "parking capacity. Correct it first.");
                }

                int vehiclesEntered = record.getVehiclesEntered();

                if (vehiclesEntered > totalSpaces - occupiedSpaces) {
                    throw new IllegalArgumentException(
                            "Not enough available parking spaces for "
                                    + vehiclesEntered + " vehicles.");
                }

                // Save the entrance observation first.
                try (PreparedStatement statement =
                            connection.prepareStatement(entranceSql)) {

                    statement.setInt(1, record.getEntranceId());
                    statement.setString(2, record.getRecordedAt());
                    statement.setInt(3, record.getQueueLength());
                    statement.setInt(4, vehiclesEntered);
                    statement.setDouble(
                            5, record.getAverageServiceTime());

                    statement.executeUpdate();
                }

                // Capture the entrance record ID before inserting
                // the parking snapshot.
                try (PreparedStatement statement =
                            connection.prepareStatement(
                                    "SELECT last_insert_rowid()");
                    ResultSet result = statement.executeQuery()) {

                    if (result.next()) {
                        record.setId(result.getInt(1));
                    }
                }

                int updatedOccupied =
                        occupiedSpaces + vehiclesEntered;

                int updatedAvailable =
                        totalSpaces - updatedOccupied;

                try (PreparedStatement statement =
                            connection.prepareStatement(parkingSql)) {

                    statement.setString(1, record.getRecordedAt());
                    statement.setInt(2, updatedOccupied);
                    statement.setInt(3, updatedAvailable);
                    statement.executeUpdate();
                }

                connection.commit();

            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public List<EntranceRecord> findAll() throws SQLException {
        String sql = """
                SELECT *
                FROM entrance_records
                ORDER BY recorded_at DESC, id DESC
                """;

        List<EntranceRecord> records = new ArrayList<>();

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                records.add(mapRow(resultSet));
            }
        }

        return records;
    }

    public List<EntranceRecord> findByEntranceId(int entranceId)
            throws SQLException {

        if (entranceId <= 0) {
            throw new IllegalArgumentException(
                    "Entrance ID must be greater than zero.");
        }

        String sql = """
                SELECT *
                FROM entrance_records
                WHERE entrance_id = ?
                ORDER BY recorded_at DESC, id DESC
                """;

        List<EntranceRecord> records = new ArrayList<>();

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, entranceId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    records.add(mapRow(resultSet));
                }
            }
        }

        return records;
    }

    public Optional<EntranceRecord> findLatestByEntranceId(
            int entranceId) throws SQLException {

        if (entranceId <= 0) {
            throw new IllegalArgumentException(
                    "Entrance ID must be greater than zero.");
        }

        String sql = """
                SELECT *
                FROM entrance_records
                WHERE entrance_id = ?
                ORDER BY recorded_at DESC, id DESC
                LIMIT 1
                """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, entranceId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    private void validate(EntranceRecord record) {
        if (record == null) {
            throw new IllegalArgumentException(
                    "Entrance record cannot be null.");
        }

        if (record.getEntranceId() <= 0) {
            throw new IllegalArgumentException(
                    "A valid entrance ID is required.");
        }

        TimestampValidator.validate(record.getRecordedAt());

        if (record.getQueueLength() < 0) {
            throw new IllegalArgumentException(
                    "Queue length cannot be negative.");
        }

        if (record.getVehiclesEntered() < 0) {
            throw new IllegalArgumentException(
                    "Vehicles entered cannot be negative.");
        }

        if (!Double.isFinite(record.getAverageServiceTime())
                || record.getAverageServiceTime() < 0) {
            throw new IllegalArgumentException(
                    "Average service time must be finite and non-negative.");
        }
    }

    private EntranceRecord mapRow(ResultSet resultSet)
            throws SQLException {

        return new EntranceRecord(
                resultSet.getInt("id"),
                resultSet.getInt("entrance_id"),
                resultSet.getString("recorded_at"),
                resultSet.getInt("queue_length"),
                resultSet.getInt("vehicles_entered"),
                resultSet.getDouble("average_service_time")
        );
    }
}