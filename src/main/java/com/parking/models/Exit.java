package com.parking.models;

public class Exit {

    private int id;
    private String name;
    private int laneCount;
    private int activeLanes;
    private String status;

    public Exit() {
    }

    public Exit(int id, String name, int laneCount,
                int activeLanes, String status) {
        this.id = id;
        this.name = name;
        this.laneCount = laneCount;
        this.activeLanes = activeLanes;
        this.status = status;
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

    public int getLaneCount() {
        return laneCount;
    }

    public void setLaneCount(int laneCount) {
        this.laneCount = laneCount;
    }

    public int getActiveLanes() {
        return activeLanes;
    }

    public void setActiveLanes(int activeLanes) {
        this.activeLanes = activeLanes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return name + " (ID: " + id + ")";
    }
}