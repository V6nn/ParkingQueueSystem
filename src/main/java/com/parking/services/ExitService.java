
package com.parking.services;

import com.parking.database.DatabaseConnection;
import com.parking.models.Exit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExitService {

    public void save(Exit exit) throws SQLException {
        validate(exit);

        String sql = """
                INSERT INTO exits
                (name, lane_count, active_lanes, status)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, exit.getName());
            statement.setInt(2, exit.getLaneCount());
            statement.setInt(3, exit.getActiveLanes());
            statement.setString(4, exit.getStatus().toUpperCase());

            statement.executeUpdate();

            try (PreparedStatement idStatement =
                         connection.prepareStatement(
                                 "SELECT last_insert_rowid()");
                 ResultSet resultSet = idStatement.executeQuery()) {

                if (resultSet.next()) {
                    exit.setId(resultSet.getInt(1));
                }
            }
        }
    }

    public boolean update(Exit exit) throws SQLException {
        validate(exit);

        if (exit.getId() <= 0) {
            throw new IllegalArgumentException(
                    "Exit ID must be greater than zero.");
        }

        String sql = """
                UPDATE exits
                SET name = ?,
                    lane_count = ?,
                    active_lanes = ?,
                    status = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, exit.getName());
            statement.setInt(2, exit.getLaneCount());
            statement.setInt(3, exit.getActiveLanes());
            statement.setString(4, exit.getStatus().toUpperCase());
            statement.setInt(5, exit.getId());

            return statement.executeUpdate() > 0;
        }
    }

    public Optional<Exit> findById(int id) throws SQLException {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Exit ID must be greater than zero.");
        }

        String sql = "SELECT * FROM exits WHERE id = ?";

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

    public List<Exit> findAll() throws SQLException {
        String sql = "SELECT * FROM exits ORDER BY id";
        List<Exit> exits = new ArrayList<>();

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                exits.add(mapRow(resultSet));
            }
        }

        return exits;
    }

    public boolean delete(int id) throws SQLException {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Exit ID must be greater than zero.");
        }

        String sql = "DELETE FROM exits WHERE id = ?";

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private void validate(Exit exit) {
        if (exit == null) {
            throw new IllegalArgumentException("Exit cannot be null.");
        }

        if (exit.getName() == null || exit.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Exit name cannot be empty.");
        }

        if (exit.getLaneCount() <= 0) {
            throw new IllegalArgumentException(
                    "Lane count must be greater than zero.");
        }

        if (exit.getActiveLanes() < 0
                || exit.getActiveLanes() > exit.getLaneCount()) {
            throw new IllegalArgumentException(
                    "Active lanes must be between zero and lane count.");
        }

        if (exit.getStatus() == null
                || !(exit.getStatus().equalsIgnoreCase("OPEN")
                || exit.getStatus().equalsIgnoreCase("CLOSED"))) {
            throw new IllegalArgumentException(
                    "Status must be OPEN or CLOSED.");
        }

        if (exit.getStatus().equalsIgnoreCase("CLOSED")
                && exit.getActiveLanes() != 0) {
            throw new IllegalArgumentException(
                    "A closed exit must have zero active lanes.");
        }
    }

    private Exit mapRow(ResultSet resultSet) throws SQLException {
        return new Exit(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getInt("lane_count"),
                resultSet.getInt("active_lanes"),
                resultSet.getString("status")
        );
    }
}