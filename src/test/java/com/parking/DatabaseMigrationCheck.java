
package com.parking;

import com.parking.database.DatabaseInitializer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseMigrationCheck {

    public static void main(String[] args) throws Exception {
        System.setProperty(
                "parking.db.url",
                "jdbc:sqlite:parking-migration-test.db"
        );

        DatabaseInitializer.initialize();

        try (Connection connection = DriverManager.getConnection(
                     "jdbc:sqlite:parking-migration-test.db");
             Statement statement = connection.createStatement()) {

            System.out.println("=== MIGRATED COLUMNS ===");

            try (ResultSet result = statement.executeQuery(
                    "PRAGMA table_info(vehicle_transactions)")) {
                while (result.next()) {
                    System.out.println(result.getString("name"));
                }
            }

            System.out.println("\n=== PRESERVED RECORD ===");

            try (ResultSet result = statement.executeQuery("""
                    SELECT id, plate_number, payment_status, parking_status
                    FROM vehicle_transactions
                    WHERE plate_number = '539UOY'
                    """)) {
                while (result.next()) {
                    System.out.printf(
                            "ID=%d, Plate=%s, Payment=%s, Parking=%s%n",
                            result.getInt("id"),
                            result.getString("plate_number"),
                            result.getString("payment_status"),
                            result.getString("parking_status")
                    );
                }
            }

            System.out.println("\n=== MIGRATED STATUS CONSTRAINT ===");
            try (ResultSet result = statement.executeQuery("""
                    SELECT sql
                    FROM sqlite_master
                    WHERE type = 'table'
                      AND name = 'vehicle_transactions'
                    """)) {
                if (result.next()) {
                    System.out.println(result.getString("sql"));
                }
            }
        }
    }
}