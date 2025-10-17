package com.example.platforme_backend;

import java.util.List;

public class ComparisonRequest {
    private String name;
    private List<Long> playerIds;
    private String description;

    public ComparisonRequest() {}

    public ComparisonRequest(String name, List<Long> playerIds, String description) {
        this.name = name;
        this.playerIds = playerIds;
        this.description = description;
    }

    // Getters et Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Long> getPlayerIds() { return playerIds; }
    public void setPlayerIds(List<Long> playerIds) { this.playerIds = playerIds; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
