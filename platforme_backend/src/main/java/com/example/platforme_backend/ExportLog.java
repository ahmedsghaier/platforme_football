package com.example.platforme_backend;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "export_logs")
@Getter
@Setter
public class ExportLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "export_format", nullable = false)
    private String exportFormat; // PDF, EXCEL

    @Column(name = "data_type")
    private String dataType; // DASHBOARD, COMPARISON, PLAYER

    @Column(name = "player_ids", columnDefinition = "TEXT")
    private String playerIds; // JSON array ou comma-separated

    @Column(name = "export_date", nullable = false)
    private LocalDateTime exportDate;

    public ExportLog() {}

}
