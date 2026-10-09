
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

        if (record.getRecordedAt() == null
                || record.getRecordedAt().isBlank()) {
            throw new IllegalArgumentException(
                    "Record timestamp is required.");
        }

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