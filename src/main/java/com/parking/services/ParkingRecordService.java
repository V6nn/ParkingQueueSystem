
// save() — stores an occupancy snapshot and checks that the occupied and available spaces add up to total capacity.
//findAll() — retrieves the history, newest first.
// findLatest() — retrieves the most recent saved snapshot.

package com.parking.services;

import com.parking.database.DatabaseConnection;
import com.parking.models.ParkingRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ParkingRecordService {

    public void save(ParkingRecord record, int totalSpaces)
            throws SQLException {

        validate(record, totalSpaces);

        String sql = """
                INSERT INTO parking_records
                (recorded_at, occupied_spaces, available_spaces)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, record.getRecordedAt());
            statement.setInt(2, record.getOccupiedSpaces());
            statement.setInt(3, record.getAvailableSpaces());

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

    public List<ParkingRecord> findAll() throws SQLException {
        String sql = """
                SELECT *
                FROM parking_records
                ORDER BY recorded_at DESC, id DESC
                """;

        List<ParkingRecord> records = new ArrayList<>();

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

    public ParkingRecord findLatest() throws SQLException {
        String sql = """
                SELECT *
                FROM parking_records
                ORDER BY recorded_at DESC, id DESC
                LIMIT 1
                """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return mapRow(resultSet);
            }
        }

        return null;
    }

    private void validate(ParkingRecord record, int totalSpaces) {
        if (record == null) {
            throw new IllegalArgumentException(
                    "Parking record cannot be null.");
        }

        if (totalSpaces <= 0) {
            throw new IllegalArgumentException(
                    "Total spaces must be greater than zero.");
        }

        TimestampValidator.validate(record.getRecordedAt());

        if (record.getOccupiedSpaces() < 0
                || record.getOccupiedSpaces() > totalSpaces) {
            throw new IllegalArgumentException(
                    "Occupied spaces must be between zero and total capacity.");
        }

        int expectedAvailable =
                totalSpaces - record.getOccupiedSpaces();

        if (record.getAvailableSpaces() != expectedAvailable) {
            throw new IllegalArgumentException(
                    "Available spaces must equal total spaces minus occupied spaces.");
        }
    }

    private ParkingRecord mapRow(ResultSet resultSet)
            throws SQLException {

        return new ParkingRecord(
                resultSet.getInt("id"),
                resultSet.getString("recorded_at"),
                resultSet.getInt("occupied_spaces"),
                resultSet.getInt("available_spaces")
        );
    }
}