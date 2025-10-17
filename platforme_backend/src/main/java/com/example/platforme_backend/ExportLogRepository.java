package com.example.platforme_backend;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExportLogRepository extends JpaRepository<ExportLog, Long> {
    int countByUserIdAndExportDateBetween(Long userId, LocalDateTime start, LocalDateTime end);

    List<ExportLog> findByUserIdAndExportDateBetweenOrderByExportDateDesc(
            Long userId, LocalDateTime start, LocalDateTime end);
    @Query("SELECT COUNT(e) FROM ExportLog e WHERE e.userId = :userId " +
            "AND YEARWEEK(e.exportDate, 1) = YEARWEEK(CURRENT_DATE, 1)")
    long countExportsByUserIdThisWeek(@Param("userId") Long userId);

    // Compter les exports de ce mois pour un utilisateur
    @Query("SELECT COUNT(e) FROM ExportLog e WHERE e.userId = :userId " +
            "AND YEAR(e.exportDate) = YEAR(CURRENT_DATE) " +
            "AND MONTH(e.exportDate) = MONTH(CURRENT_DATE)")
    long countExportsByUserIdThisMonth(@Param("userId") Long userId);

    // Exports récents pour un utilisateur
    @Query("SELECT e FROM ExportLog e WHERE e.userId = :userId " +
            "ORDER BY e.exportDate DESC")
    Page<ExportLog> findRecentExportsByUserId(@Param("userId") Long userId, Pageable pageable);
}