package com.example.platforme_backend;

public class RecentSearchDTO {
    private Long id;
    private String playerName;
    private String club;
    private String date;
    private Long playerId;

    // Constructeurs
    public RecentSearchDTO() {}

    public RecentSearchDTO(String playerName, String club, String date) {
        this.playerName = playerName;
        this.club = club;
        this.date = date;
    }

    public RecentSearchDTO(Long id, String playerName, String club, String date, Long playerId) {
        this.id = id;
        this.playerName = playerName;
        this.club = club;
        this.date = date;
        this.playerId = playerId;
    }

    // Getters et setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getClub() { return club; }
    public void setClub(String club) { this.club = club; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }
}