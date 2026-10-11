package com.parking.models;

public class EntranceRecord {

    private int id;
    private int entranceId;
    private String recordedAt;
    private int queueLength;
    private int vehiclesEntered;
    private Double averageServiceTime;
    private Double observationDurationMinutes;

    public EntranceRecord() {
    }

    public EntranceRecord(int id, int entranceId, String recordedAt,
                          int queueLength, int vehiclesEntered,
                          double averageServiceTime) {
        this(id, entranceId, recordedAt, queueLength, vehiclesEntered,
                Double.valueOf(averageServiceTime), null);
    }

    public EntranceRecord(int id, int entranceId, String recordedAt,
                          int queueLength, int vehiclesEntered,
                          Double averageServiceTime,
                          Double observationDurationMinutes) {
        this.id = id;
        this.entranceId = entranceId;
        this.recordedAt = recordedAt;
        this.queueLength = queueLength;
        this.vehiclesEntered = vehiclesEntered;
        this.averageServiceTime = averageServiceTime;
        this.observationDurationMinutes = observationDurationMinutes;
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

    public Double getAverageServiceTime() {
        return averageServiceTime;
    }

    public void setAverageServiceTime(Double averageServiceTime) {
        this.averageServiceTime = averageServiceTime;
    }

    public Double getObservationDurationMinutes() {
        return observationDurationMinutes;
    }

    public void setObservationDurationMinutes(
            Double observationDurationMinutes) {
        this.observationDurationMinutes = observationDurationMinutes;
    }
}