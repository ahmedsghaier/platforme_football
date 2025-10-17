package com.example.platforme_backend;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserSearchLogDTO {

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("playerId")
    private Long playerId; // Peut être null

    @JsonProperty("searchQuery")
    private String searchQuery;

    @JsonProperty("type")
    private String type; // "SEARCH" or "COMPARISON"

    @JsonProperty("searchFilters")
    private Object searchFilters; // Pour les filtres supplémentaires

    // Constructeur par défaut
    public UserSearchLogDTO() {}

    // Constructeur avec paramètres
    public UserSearchLogDTO(Long userId, Long playerId, String searchQuery, String type) {
        this.userId = userId;
        this.playerId = playerId;
        this.searchQuery = searchQuery;
        this.type = type;
    }

    // Getters et setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getSearchFilters() {
        return searchFilters;
    }

    public void setSearchFilters(Object searchFilters) {
        this.searchFilters = searchFilters;
    }

    @Override
    public String toString() {
        return "UserSearchLogDTO{" +
                "userId=" + userId +
                ", playerId=" + playerId +
                ", searchQuery='" + searchQuery + '\'' +
                ", type='" + type + '\'' +
                ", searchFilters=" + searchFilters +
                '}';
    }
}