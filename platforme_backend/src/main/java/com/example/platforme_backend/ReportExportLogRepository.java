package com.example.platforme_backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportExportLogRepository extends JpaRepository<ReportExportLog, Long> {

    /**
     * Compte les rapports exportés par un utilisateur entre deux dates
     */
    Long countByUserIdAndExportDateBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Récupère l'historique d'export d'un utilisateur
     */
    List<ReportExportLog> findByUserIdOrderByExportDateDesc(Long userId);
}