package com.example.platforme_backend;

import java.sql.Timestamp;
import java.util.List;

public class ComparisonResult {
    private Long id;
    private String comparisonName; // anciennement "name"
    private List<Long> playerIds;
    private Timestamp createdAt;
    private List<PlayerProfileDTO> players;

    public ComparisonResult() {}

    public ComparisonResult(Long id, String comparisonName, List<Long> playerIds, Timestamp createdAt) {
        this.id = id;
        this.comparisonName = comparisonName;
        this.playerIds = playerIds;
        this.createdAt = createdAt;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getComparisonName() { return comparisonName; }
    public void setComparisonName(String comparisonName) { this.comparisonName = comparisonName; }

    public List<Long> getPlayerIds() { return playerIds; }
    public void setPlayerIds(List<Long> playerIds) { this.playerIds = playerIds; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public List<PlayerProfileDTO> getPlayers() { return players; }
    public void setPlayers(List<PlayerProfileDTO> players) { this.players = players; }
}
