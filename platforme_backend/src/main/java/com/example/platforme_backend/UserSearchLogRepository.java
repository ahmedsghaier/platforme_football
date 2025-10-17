package com.example.platforme_backend;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserSearchLogRepository extends JpaRepository<UserSearchLog, Long> {

    /**
     * Compte les recherches d'un utilisateur entre deux dates
     */
    Long countByUserIdAndSearchDateBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Récupère les 10 dernières recherches d'un utilisateur
     */
    default List<UserSearchLog> findTop10ByUserIdOrderBySearchDateDesc(Long userId) {
        return findTop10ByUserIdOrderBySearchDateDesc(userId, PageRequest.of(0, 10));
    }

    /**
     * Récupère toutes les recherches d'un utilisateur pour un joueur spécifique
     */
    List<UserSearchLog> findByUserIdAndPlayerIdOrderBySearchDateDesc(Long userId, Long playerId);

    /**
     * Récupère les recherches les plus fréqu  entes d'un utilisateur
     */
    @Query(value = "SELECT player_id, COUNT(*) as search_count " +
            "FROM user_search_logs " +
            "WHERE user_id = :userId " +
            "GROUP BY player_id " +
            "ORDER BY search_count DESC " +
            "LIMIT 10", nativeQuery = true)
    List<Object[]> findMostSearchedPlayersByUser(@Param("userId") Long userId);

    /**
     * Supprime les anciennes recherches (plus de 6 mois)
     */
    void deleteBySearchDateBefore(LocalDateTime cutoffDate);
    Long countByPlayerIdAndSearchDateAfter(Long playerId, LocalDateTime afterDate);
    Long countByUserIdAndSearchDateBetweenAndSearchQueryContaining(
            Long userId, LocalDateTime start, LocalDateTime end, String searchTerm);

    /**
     * Recherches les plus populaires
     */
    @Query("SELECT s.searchQuery, COUNT(s) as searchCount " +
            "FROM UserSearchLog s " +
            "WHERE s.searchDate BETWEEN :start AND :end " +
            "GROUP BY s.searchQuery " +
            "ORDER BY searchCount DESC")
    List<Object[]> findMostPopularSearches(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Joueurs les plus recherchés
     */
    @Query("SELECT s.playerId, COUNT(s) as searchCount " +
            "FROM UserSearchLog s " +
            "WHERE s.playerId IS NOT NULL " +
            "AND s.searchDate BETWEEN :start AND :end " +
            "GROUP BY s.playerId " +
            "ORDER BY searchCount DESC")
    List<Object[]> findMostSearchedPlayers(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
    @Query("SELECT COUNT(s) FROM UserSearchLog s WHERE s.userId = :userId AND s.searchDate BETWEEN :start AND :end AND s.type = :type")
    Long countByUserIdAndSearchDateBetweenAndType(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("type") String type);

    @Query("SELECT s FROM UserSearchLog s JOIN FETCH s.playerId WHERE s.userId = :userId AND s.type = :type ORDER BY s.searchDate DESC")
    List<UserSearchLog> findTop10ByUserIdAndTypeOrderBySearchDateDesc(@Param("userId") Long userId, @Param("type") String type);
    @Query("SELECT u FROM UserSearchLog u WHERE u.userId = :userId ORDER BY u.searchDate DESC")
    List<UserSearchLog> findTop10ByUserIdOrderBySearchDateDesc(@Param("userId") Long userId, Pageable pageable);


    List<UserSearchLog> findTop10ByUserIdAndTypeAndSearchQueryNotOrderBySearchDateDesc(
            Long userId, String type, String excludeQuery
    );
    int countByUserIdAndTypeAndSearchDateBetween(Long userId, String type, LocalDateTime start, LocalDateTime end);

    List<UserSearchLog> findByUserIdAndTypeAndSearchDateBetweenOrderBySearchDateDesc(
            Long userId, String type, LocalDateTime start, LocalDateTime end, Pageable pageable);
    @Query(value = "SELECT COUNT(*) FROM user_search_log u " +
            "WHERE u.user_id = :userId " +
            "AND u.type = :type " +
            "AND YEAR(u.search_date) = YEAR(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH)) " +
            "AND MONTH(u.search_date) = MONTH(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH))",
            nativeQuery = true)
    long countByUserIdAndTypeAndPreviousMonth(@Param("userId") Long userId,
                                              @Param("type") String type);
    // Compter les recherches de ce mois pour un utilisateur
    @Query("SELECT COUNT(u) FROM UserSearchLog u WHERE u.userId = :userId " +
            "AND u.type = 'SEARCH' " +
            "AND YEAR(u.searchDate) = YEAR(CURRENT_DATE) " +
            "AND MONTH(u.searchDate) = MONTH(CURRENT_DATE)")
    long countSearchesThisMonth(@Param("userId") Long userId);

    // Compter les comparaisons de ce mois pour un utilisateur
    @Query("SELECT COUNT(u) FROM UserSearchLog u WHERE u.userId = :userId " +
            "AND u.type = 'COMPARISON' " +
            "AND YEAR(u.searchDate) = YEAR(CURRENT_DATE) " +
            "AND MONTH(u.searchDate) = MONTH(CURRENT_DATE)")
    long countComparisonsByUserIdAndMonth(@Param("userId") Long userId);




}