package de.blau.android;

import androidx.annotation.NonNull;

public class Season {
    private String name;
    private String startDate;
    private String endDate;

    public Season(String startDate, String name, String endDate) {
        this.startDate = startDate;
        this.name = name;
        this.endDate = endDate;
    }

    public Season(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NonNull
    @Override
    public String toString() {
        return getName();
    }
}
