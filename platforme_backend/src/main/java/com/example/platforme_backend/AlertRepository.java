package com.example.platforme_backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /**
     * Récupère les alertes d'un utilisateur triées par date de création
     */
    List<Alert> findByUserIdOrderByCreatedDateDesc(Long userId);

    /**
     * Récupère les alertes non lues d'un utilisateur
     */
    List<Alert> findByUserIdAndIsReadFalseOrderByCreatedDateDesc(Long userId);

    /**
     * Compte les alertes non lues d'un utilisateur
     */
    Long countByUserIdAndIsReadFalse(Long userId);

    /**
     * Récupère les alertes pour un joueur spécifique
     */
    List<Alert> findByUserIdAndPlayerIdOrderByCreatedDateDesc(Long userId, Long playerId);

    /**
     * Récupère les alertes par type
     */
    List<Alert> findByUserIdAndTypeOrderByCreatedDateDesc(Long userId, String type);

    /**
     * Marque toutes les alertes d'un utilisateur comme lues
     */
    @Query("UPDATE Alert a SET a.isRead = true WHERE a.userId = :userId")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    /**
     * Supprime les anciennes alertes (plus de 30 jours)
     */
    void deleteByCreatedDateBefore(LocalDateTime cutoffDate);
}

