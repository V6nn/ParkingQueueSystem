
package com.parking.services;

import com.parking.database.DatabaseConnection;
import com.parking.models.Entrance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EntranceService {

    public void save(Entrance entrance) throws SQLException {
        validate(entrance);

        String sql = """
                INSERT INTO entrances
                (name, lane_count, active_lanes, status)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, entrance.getName());
            statement.setInt(2, entrance.getLaneCount());
            statement.setInt(3, entrance.getActiveLanes());
            statement.setString(4, entrance.getStatus().toUpperCase());

            statement.executeUpdate();

            try (PreparedStatement idStatement =
                         connection.prepareStatement(
                                 "SELECT last_insert_rowid()");
                 ResultSet resultSet = idStatement.executeQuery()) {

                if (resultSet.next()) {
                    entrance.setId(resultSet.getInt(1));
                }
            }
        }
    }

    public boolean update(Entrance entrance) throws SQLException {
        validate(entrance);

        if (entrance.getId() <= 0) {
            throw new IllegalArgumentException(
                    "Entrance ID must be greater than zero.");
        }

        String sql = """
                UPDATE entrances
                SET name = ?,
                    lane_count = ?,
                    active_lanes = ?,
                    status = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, entrance.getName());
            statement.setInt(2, entrance.getLaneCount());
            statement.setInt(3, entrance.getActiveLanes());
            statement.setString(4, entrance.getStatus().toUpperCase());
            statement.setInt(5, entrance.getId());

            return statement.executeUpdate() > 0;
        }
    }

    public Optional<Entrance> findById(int id) throws SQLException {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Entrance ID must be greater than zero.");
        }

        String sql = "SELECT * FROM entrances WHERE id = ?";

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

    public List<Entrance> findAll() throws SQLException {
        String sql = "SELECT * FROM entrances ORDER BY id";
        List<Entrance> entrances = new ArrayList<>();

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                entrances.add(mapRow(resultSet));
            }
        }

        return entrances;
    }

    public boolean delete(int id) throws SQLException {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Entrance ID must be greater than zero.");
        }

        String sql = "DELETE FROM entrances WHERE id = ?";

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private void validate(Entrance entrance) {
        if (entrance == null) {
            throw new IllegalArgumentException("Entrance cannot be null.");
        }

        if (entrance.getName() == null
                || entrance.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Entrance name cannot be empty.");
        }

        if (entrance.getLaneCount() <= 0) {
            throw new IllegalArgumentException(
                    "Lane count must be greater than zero.");
        }

        if (entrance.getActiveLanes() < 0
                || entrance.getActiveLanes() > entrance.getLaneCount()) {
            throw new IllegalArgumentException(
                    "Active lanes must be between zero and lane count.");
        }

        if (entrance.getStatus() == null
                || !(entrance.getStatus().equalsIgnoreCase("OPEN")
                || entrance.getStatus().equalsIgnoreCase("CLOSED"))) {
            throw new IllegalArgumentException(
                    "Status must be OPEN or CLOSED.");
        }

        if (entrance.getStatus().equalsIgnoreCase("CLOSED")
                && entrance.getActiveLanes() != 0) {
            throw new IllegalArgumentException(
                    "A closed entrance must have zero active lanes.");
        }
    }

    private Entrance mapRow(ResultSet resultSet) throws SQLException {
        return new Entrance(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getInt("lane_count"),
                resultSet.getInt("active_lanes"),
                resultSet.getString("status")
        );
    }
}