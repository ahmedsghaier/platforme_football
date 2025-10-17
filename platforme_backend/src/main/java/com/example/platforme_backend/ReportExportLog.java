package com.example.platforme_backend;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
@Entity
@Table(name = "report_export_logs")
public class ReportExportLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "report_type", nullable = false)
    private String reportType; // "dashboard", "player", "comparison"

    @Column(name = "export_format", nullable = false)
    private String exportFormat; // "pdf", "csv", "excel"

    @Column(name = "export_date", nullable = false)
    private LocalDateTime exportDate;

    @Column(name = "file_size")
    private Long fileSize;

    // Constructeurs
    public ReportExportLog() {}

    public ReportExportLog(Long userId, String reportType, String exportFormat, Long fileSize) {
        this.userId = userId;
        this.reportType = reportType;
        this.exportFormat = exportFormat;
        this.fileSize = fileSize;
        this.exportDate = LocalDateTime.now();
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getExportFormat() { return exportFormat; }
    public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }

    public LocalDateTime getExportDate() { return exportDate; }
    public void setExportDate(LocalDateTime exportDate) { this.exportDate = exportDate; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
}
