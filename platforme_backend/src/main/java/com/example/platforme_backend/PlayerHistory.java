package com.example.platforme_backend;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "player_history")
public class PlayerHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "market_value")
    private Integer marketValue;

    @Column(name = "record_date", nullable = false)
    private LocalDateTime recordDate;

    @Column(name = "source")
    private String source; // Source de la donnée

    @Column(name = "notes")
    private String notes; // Notes sur le changement

    // Constructeurs
    public PlayerHistory() {}

    public PlayerHistory(Long playerId, Integer marketValue, LocalDateTime recordDate) {
        this.playerId = playerId;
        this.marketValue = marketValue;
        this.recordDate = recordDate;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public Integer getMarketValue() { return marketValue; }
    public void setMarketValue(Integer marketValue) { this.marketValue = marketValue; }

    public LocalDateTime getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDateTime recordDate) { this.recordDate = recordDate; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}