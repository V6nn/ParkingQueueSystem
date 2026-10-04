package com.parking.models;

public class ParkingLot {

    private int id;
    private String name;
    private int totalSpaces;
    private String operatingStart;
    private String operatingEnd;

    public ParkingLot() {
    }

    public ParkingLot(int id, String name, int totalSpaces,
                      String operatingStart, String operatingEnd) {
        this.id = id;
        this.name = name;
        this.totalSpaces = totalSpaces;
        this.operatingStart = operatingStart;
        this.operatingEnd = operatingEnd;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTotalSpaces() {
        return totalSpaces;
    }

    public void setTotalSpaces(int totalSpaces) {
        this.totalSpaces = totalSpaces;
    }

    public String getOperatingStart() {
        return operatingStart;
    }

    public void setOperatingStart(String operatingStart) {
        this.operatingStart = operatingStart;
    }

    public String getOperatingEnd() {
        return operatingEnd;
    }

    public void setOperatingEnd(String operatingEnd) {
        this.operatingEnd = operatingEnd;
    }
}