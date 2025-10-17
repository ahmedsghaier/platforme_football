package com.example.platforme_backend;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "type", nullable = false)
    private String type; // 'increase', 'opportunity', 'market'

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    // Constructeur par défaut
    public Alert() {}

    // Constructeur personnalisé
    public Alert(Long userId, Long playerId, String type, String message) {
        this.userId = userId;
        this.playerId = playerId;
        this.type = type;
        this.message = message;
        this.createdDate = LocalDateTime.now(); // Date auto
        this.isRead = false; // Par défaut non lu
    }

    // Getters et setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
}
