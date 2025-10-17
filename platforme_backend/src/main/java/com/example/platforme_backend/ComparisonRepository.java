package com.example.platforme_backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
interface ComparisonRepository extends JpaRepository<Comparison, Long> {

    List<Comparison> findAllByOrderByCreatedAtDesc();
    List<Comparison> findByComparisonNameContainingIgnoreCase(String name);

    /**
     * Compte les comparaisons d'un utilisateur entre deux dates
     * Note: Utilise Timestamp au lieu de LocalDateTime pour correspondre à l'entité
     */
    Long countByUserIdAndCreatedAtBetween(Long userId, Timestamp startDate, Timestamp endDate);

    /**
     * Récupère les comparaisons récentes d'un utilisateur
     */
    List<Comparison> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Récupère toutes les comparaisons d'un utilisateur
     */
    List<Comparison> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Version avec LocalDateTime en utilisant une requête personnalisée
     */
    @Query("SELECT COUNT(c) FROM Comparison c WHERE c.userId = :userId AND c.createdAt BETWEEN :startDate AND :endDate")
    Long countByUserIdAndCreatedAtBetweenDates(@Param("userId") Long userId,
                                               @Param("startDate") Timestamp startDate,
                                               @Param("endDate") Timestamp endDate);
}