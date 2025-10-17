package com.example.platforme_backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerHistoryRepository extends JpaRepository<PlayerHistory, Long> {

    /**
     * Trouve la valeur la plus récente pour un joueur
     */
    Optional<PlayerHistory> findTopByPlayerIdOrderByRecordDateDesc(Long playerId);

    /**
     * Trouve la valeur la plus récente avant une date donnée
     */
    Optional<PlayerHistory> findTopByPlayerIdAndRecordDateBeforeOrderByRecordDateDesc(
            Long playerId, LocalDateTime beforeDate);

    /**
     * Trouve toutes les valeurs pour un joueur dans une période
     */
    List<PlayerHistory> findByPlayerIdAndRecordDateBetweenOrderByRecordDateDesc(
            Long playerId, LocalDateTime start, LocalDateTime end);

    /**
     * Calcule la croissance moyenne pour tous les joueurs sur une période
     */
    @Query("SELECT AVG((h2.marketValue - h1.marketValue) * 100.0 / h1.marketValue) " +
            "FROM PlayerHistory h1, PlayerHistory h2 " +
            "WHERE h1.playerId = h2.playerId " +
            "AND h1.recordDate = :startDate " +
            "AND h2.recordDate = :endDate " +
            "AND h1.marketValue > 0")
    Optional<Double> calculateAverageGrowthBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}