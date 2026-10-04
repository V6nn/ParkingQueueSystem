package com.parking.models;

public class ParkingRecord {

    private int id;
    private String recordedAt;
    private int occupiedSpaces;
    private int availableSpaces;

    public ParkingRecord() {
    }

    public ParkingRecord(int id, String recordedAt,
                         int occupiedSpaces, int availableSpaces) {
        this.id = id;
        this.recordedAt = recordedAt;
        this.occupiedSpaces = occupiedSpaces;
        this.availableSpaces = availableSpaces;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(String recordedAt) {
        this.recordedAt = recordedAt;
    }

    public int getOccupiedSpaces() {
        return occupiedSpaces;
    }

    public void setOccupiedSpaces(int occupiedSpaces) {
        this.occupiedSpaces = occupiedSpaces;
    }

    public int getAvailableSpaces() {
        return availableSpaces;
    }

    public void setAvailableSpaces(int availableSpaces) {
        this.availableSpaces = availableSpaces;
    }
}