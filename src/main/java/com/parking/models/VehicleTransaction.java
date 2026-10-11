
package com.parking.models;

public class VehicleTransaction {

    private final int id;
    private final String plateNumber;
    private final String vehicleType;
    private final double amount;
    private final String paymentStatus;
    private final String parkingStatus;
    private final String createdAt;
    private final String scheduledArrival;
    private final String enteredAt;
    private final String exitedAt;

    // Original constructor retained for compatibility.
    public VehicleTransaction(
            int id,
            String plateNumber,
            String vehicleType,
            double amount,
            String paymentStatus,
            String parkingStatus,
            String createdAt) {

        this(id, plateNumber, vehicleType, amount,
                paymentStatus, parkingStatus, createdAt,
                null, null, null);
    }

    // Constructor with reservation and entry/exit timestamps.
    public VehicleTransaction(
            int id,
            String plateNumber,
            String vehicleType,
            double amount,
            String paymentStatus,
            String parkingStatus,
            String createdAt,
            String scheduledArrival,
            String enteredAt,
            String exitedAt) {

        this.id = id;
        this.plateNumber = plateNumber;
        this.vehicleType = vehicleType;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
        this.parkingStatus = parkingStatus;
        this.createdAt = createdAt;
        this.scheduledArrival = scheduledArrival;
        this.enteredAt = enteredAt;
        this.exitedAt = exitedAt;
    }

    public int getId() {
        return id;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public double getAmount() {
        return amount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getParkingStatus() {
        return parkingStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getScheduledArrival() {
        return scheduledArrival;
    }

    public String getEnteredAt() {
        return enteredAt;
    }

    public String getExitedAt() {
        return exitedAt;
    }
}