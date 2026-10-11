
package com.parking.services;

import com.parking.database.DatabaseConnection;
import com.parking.models.ExitRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExitRecordService {

    public void save(ExitRecord record) throws SQLException {
        validate(record);

        String sql = """
                INSERT INTO exit_records
                (exit_id, recorded_at, vehicles_exited)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, record.getExitId());
            statement.setString(2, record.getRecordedAt());
            statement.setInt(3, record.getVehiclesExited());

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

    public void saveAndUpdateOccupancy(
            ExitRecord record, int totalSpaces) throws SQLException {

        validate(record);

        if (totalSpaces <= 0) {
            throw new IllegalArgumentException(
                    "Total parking spaces must be greater than zero.");
        }

        String latestOccupancySql = """
                SELECT occupied_spaces
                FROM parking_records
                ORDER BY recorded_at DESC, id DESC
                LIMIT 1
                """;

        String insertExitSql = """
                INSERT INTO exit_records
                (exit_id, recorded_at, vehicles_exited)
                VALUES (?, ?, ?)
                """;

        String insertParkingSql = """
                INSERT INTO parking_records
                (recorded_at, occupied_spaces, available_spaces)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.connect()) {
            connection.setAutoCommit(false);

            try {
                int occupied;

                try (PreparedStatement statement =
                            connection.prepareStatement(latestOccupancySql);
                    ResultSet resultSet = statement.executeQuery()) {

                    if (!resultSet.next()) {
                        throw new IllegalStateException(
                                "Save an initial parking occupancy record "
                                        + "before recording vehicle exits.");
                    }

                    occupied = resultSet.getInt("occupied_spaces");
                }

                if (occupied > totalSpaces) {
                    throw new IllegalStateException(
                            "The saved occupancy exceeds the parking lot capacity.");
                }

                if (record.getVehiclesExited() > occupied) {
                    throw new IllegalArgumentException(
                            "Vehicles exited cannot exceed currently occupied spaces.");
                }

                try (PreparedStatement statement =
                            connection.prepareStatement(insertExitSql)) {

                    statement.setInt(1, record.getExitId());
                    statement.setString(2, record.getRecordedAt());
                    statement.setInt(3, record.getVehiclesExited());
                    statement.executeUpdate();
                }

                int updatedOccupied =
                        occupied - record.getVehiclesExited();

                int updatedAvailable =
                        totalSpaces - updatedOccupied;

                try (PreparedStatement statement =
                            connection.prepareStatement(insertParkingSql)) {

                    statement.setString(1, record.getRecordedAt());
                    statement.setInt(2, updatedOccupied);
                    statement.setInt(3, updatedAvailable);
                    statement.executeUpdate();
                }

                try (PreparedStatement statement =
                            connection.prepareStatement(
                                    "SELECT last_insert_rowid()");
                    ResultSet resultSet = statement.executeQuery()) {

                    if (resultSet.next()) {
                        record.setId(resultSet.getInt(1));
                    }
                }

                connection.commit();

            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }
    public List<ExitRecord> findAll() throws SQLException {
        String sql = """
                SELECT *
                FROM exit_records
                ORDER BY recorded_at DESC, id DESC
                """;

        List<ExitRecord> records = new ArrayList<>();

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

    public List<ExitRecord> findByExitId(int exitId)
            throws SQLException {

        if (exitId <= 0) {
            throw new IllegalArgumentException(
                    "Exit ID must be greater than zero.");
        }

        String sql = """
                SELECT *
                FROM exit_records
                WHERE exit_id = ?
                ORDER BY recorded_at DESC, id DESC
                """;

        List<ExitRecord> records = new ArrayList<>();

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, exitId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    records.add(mapRow(resultSet));
                }
            }
        }

        return records;
    }

    public Optional<ExitRecord> findLatestByExitId(int exitId)
            throws SQLException {

        if (exitId <= 0) {
            throw new IllegalArgumentException(
                    "Exit ID must be greater than zero.");
        }

        String sql = """
                SELECT *
                FROM exit_records
                WHERE exit_id = ?
                ORDER BY recorded_at DESC, id DESC
                LIMIT 1
                """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, exitId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    private void validate(ExitRecord record) {
        if (record == null) {
            throw new IllegalArgumentException(
                    "Exit record cannot be null.");
        }

        if (record.getExitId() <= 0) {
            throw new IllegalArgumentException(
                    "A valid exit ID is required.");
        }

        TimestampValidator.validate(record.getRecordedAt());

        if (record.getVehiclesExited() < 0) {
            throw new IllegalArgumentException(
                    "Vehicles exited cannot be negative.");
        }
    }

    private ExitRecord mapRow(ResultSet resultSet)
            throws SQLException {

        return new ExitRecord(
                resultSet.getInt("id"),
                resultSet.getInt("exit_id"),
                resultSet.getString("recorded_at"),
                resultSet.getInt("vehicles_exited")
        );
    }
}