package com.parking.models;

public class EntranceRecord {

    private int id;
    private int entranceId;
    private String recordedAt;
    private int queueLength;
    private int vehiclesEntered;
    private double averageServiceTime;

    public EntranceRecord() {
    }

    public EntranceRecord(int id, int entranceId, String recordedAt,
                          int queueLength, int vehiclesEntered,
                          double averageServiceTime) {
        this.id = id;
        this.entranceId = entranceId;
        this.recordedAt = recordedAt;
        this.queueLength = queueLength;
        this.vehiclesEntered = vehiclesEntered;
        this.averageServiceTime = averageServiceTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEntranceId() {
        return entranceId;
    }

    public void setEntranceId(int entranceId) {
        this.entranceId = entranceId;
    }

    public String getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(String recordedAt) {
        this.recordedAt = recordedAt;
    }

    public int getQueueLength() {
        return queueLength;
    }

    public void setQueueLength(int queueLength) {
        this.queueLength = queueLength;
    }

    public int getVehiclesEntered() {
        return vehiclesEntered;
    }

    public void setVehiclesEntered(int vehiclesEntered) {
        this.vehiclesEntered = vehiclesEntered;
    }

    public double getAverageServiceTime() {
        return averageServiceTime;
    }

    public void setAverageServiceTime(double averageServiceTime) {
        this.averageServiceTime = averageServiceTime;
    }
}