package com.example.platforme_backend;

public class FavoritePlayerDTO {
    private String name;
    private String club;
    private String value;
    private String trend;

    public FavoritePlayerDTO(String name, String club, String value, String trend) {
        this.name = name;
        this.club = club;
        this.value = value;
        this.trend = trend;
    }

    public String getName() { return name; }
    public String getClub() { return club; }
    public String getValue() { return value; }
    public String getTrend() { return trend; }
}