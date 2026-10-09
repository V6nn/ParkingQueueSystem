// Saving parking lot details.
// Updating existing details.
// Finding the parking lot by ID.
// Retrieving all parking lot records.


package com.parking.services;

import com.parking.database.DatabaseConnection;
import com.parking.models.ParkingLot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParkingLotService {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm")
                    .withResolverStyle(ResolverStyle.STRICT);

    public int save(ParkingLot parkingLot) throws SQLException {
        validate(parkingLot);

        String sql = """
                INSERT INTO parking_lot
                (name, total_spaces, operating_start, operating_end)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, parkingLot.getName());
            statement.setInt(2, parkingLot.getTotalSpaces());
            statement.setString(3, parkingLot.getOperatingStart());
            statement.setString(4, parkingLot.getOperatingEnd());

            statement.executeUpdate();

            try (PreparedStatement idStatement =
                         connection.prepareStatement(
                                 "SELECT last_insert_rowid()");
                 ResultSet resultSet = idStatement.executeQuery()) {

                if (resultSet.next()) {
                    int id = resultSet.getInt(1);
                    parkingLot.setId(id);
                    return id;
                }
            }
        }

        throw new SQLException("Could not retrieve the parking lot ID.");
    }

    public boolean update(ParkingLot parkingLot) throws SQLException {
        validate(parkingLot);

        if (parkingLot.getId() <= 0) {
            throw new IllegalArgumentException(
                    "A valid parking lot ID is required.");
        }

        String sql = """
                UPDATE parking_lot
                SET name = ?,
                    total_spaces = ?,
                    operating_start = ?,
                    operating_end = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, parkingLot.getName());
            statement.setInt(2, parkingLot.getTotalSpaces());
            statement.setString(3, parkingLot.getOperatingStart());
            statement.setString(4, parkingLot.getOperatingEnd());
            statement.setInt(5, parkingLot.getId());

            return statement.executeUpdate() > 0;
        }
    }

    public Optional<ParkingLot> findById(int id) throws SQLException {
        String sql = "SELECT * FROM parking_lot WHERE id = ?";

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    public List<ParkingLot> findAll() throws SQLException {
        String sql = "SELECT * FROM parking_lot ORDER BY id";
        List<ParkingLot> parkingLots = new ArrayList<>();

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                parkingLots.add(mapRow(resultSet));
            }
        }

        return parkingLots;
    }

    private void validate(ParkingLot parkingLot) {
        if (parkingLot == null) {
            throw new IllegalArgumentException(
                    "Parking lot cannot be null.");
        }

        if (parkingLot.getName() == null
                || parkingLot.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Parking lot name is required.");
        }

        if (parkingLot.getTotalSpaces() <= 0) {
            throw new IllegalArgumentException(
                    "Total parking spaces must be greater than zero.");
        }

        validateTime(parkingLot.getOperatingStart(), "Operating start");
        validateTime(parkingLot.getOperatingEnd(), "Operating end");
    }

    private void validateTime(String time, String fieldName) {
        if (time == null || time.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " time is required.");
        }

        try {
            LocalTime.parse(time, TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    fieldName + " time must use HH:mm format (00:00–23:59).");
        }
    }

    private ParkingLot mapRow(ResultSet resultSet) throws SQLException {
        return new ParkingLot(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getInt("total_spaces"),
                resultSet.getString("operating_start"),
                resultSet.getString("operating_end")
        );
    }
}