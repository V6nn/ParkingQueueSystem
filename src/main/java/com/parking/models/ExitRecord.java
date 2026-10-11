package com.parking.models;

public class ExitRecord {

    private int id;
    private int exitId;
    private String recordedAt;
    private int vehiclesExited;

    public ExitRecord() {
    }

    public ExitRecord(int id, int exitId, String recordedAt,
                      int vehiclesExited) {
        this.id = id;
        this.exitId = exitId;
        this.recordedAt = recordedAt;
        this.vehiclesExited = vehiclesExited;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getExitId() {
        return exitId;
    }

    public void setExitId(int exitId) {
        this.exitId = exitId;
    }

    public String getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(String recordedAt) {
        this.recordedAt = recordedAt;
    }

    public int getVehiclesExited() {
        return vehiclesExited;
    }

    public void setVehiclesExited(int vehiclesExited) {
        this.vehiclesExited = vehiclesExited;
    }
}