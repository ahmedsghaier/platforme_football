package com.example.platforme_backend;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_search_log")
public class UserSearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "player_id", nullable = true) // Explicitement nullable
    private Long playerId;

    @Column(name = "search_query", nullable = false, length = 1000)
    private String searchQuery;

    @Column(name = "type", nullable = false, length = 50)
    private String type; // "SEARCH" or "COMPARISON"

    @Column(name = "search_date", nullable = false)
    private LocalDateTime searchDate;

    // Constructeur par défaut
    public UserSearchLog() {
        this.searchDate = LocalDateTime.now();
    }

    // Constructeur avec paramètres
    public UserSearchLog(Long userId, Long playerId, String searchQuery, String type) {
        this.userId = userId;
        this.playerId = playerId;
        this.searchQuery = searchQuery;
        this.type = type;
        this.searchDate = LocalDateTime.now();
    }

    // Getters et setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getSearchDate() {
        return searchDate;
    }

    public void setSearchDate(LocalDateTime searchDate) {
        this.searchDate = searchDate;
    }

    @Override
    public String toString() {
        return "UserSearchLog{" +
                "id=" + id +
                ", userId=" + userId +
                ", playerId=" + playerId +
                ", searchQuery='" + searchQuery + '\'' +
                ", type='" + type + '\'' +
                ", searchDate=" + searchDate +
                '}';
    }
}