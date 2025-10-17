package com.example.platforme_backend;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "clubs")
public class Club {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String league;
    private String level;

    @Column(name = "created_at")
    private Timestamp createdAt;

    // Constructeurs, Getters, Setters
    public Club() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLeague() { return league; }
    public void setLeague(String league) { this.league = league; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
