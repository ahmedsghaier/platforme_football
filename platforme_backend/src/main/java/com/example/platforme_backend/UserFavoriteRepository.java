package com.example.platforme_backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {

    /**
     * Trouve tous les favoris d'un utilisateur
     */
    List<UserFavorite> findByUserId(Long userId);

    /**
     * Trouve tous les favoris pour un joueur spécifique
     */
    List<UserFavorite> findByPlayerId(Long playerId);

    /**
     * Compte le nombre de joueurs favoris d'un utilisateur
     */
    int countByUserId(Long userId);

    /**
     * Compte les favoris ajoutés entre deux dates pour un utilisateur
     */
    @Query("SELECT COUNT(uf) FROM UserFavorite uf WHERE uf.userId = :userId AND uf.createdDate BETWEEN :startDate AND :endDate")
    int countByUserIdAndCreatedDateBetween(@Param("userId") Long userId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Récupère tous les favoris d'un utilisateur triés par date de création décroissante
     */
    List<UserFavorite> findByUserIdOrderByCreatedDateDesc(Long userId);

    /**
     * Trouve un favori spécifique utilisateur/joueur
     */
    Optional<UserFavorite> findByUserIdAndPlayerId(Long userId, Long playerId);

    /**
     * Vérifie si un joueur est dans les favoris d'un utilisateur
     */
    boolean existsByUserIdAndPlayerId(Long userId, Long playerId);

    /**
     * Supprime tous les favoris d'un utilisateur pour un joueur spécifique
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserFavorite uf WHERE uf.userId = :userId AND uf.playerId = :playerId")
    void deleteByUserIdAndPlayerId(@Param("userId") Long userId, @Param("playerId") Long playerId);

    /**
     * Trouve les favoris les plus récents d'un utilisateur (limité)
     */
    @Query("SELECT uf FROM UserFavorite uf WHERE uf.userId = :userId ORDER BY uf.createdDate DESC")
    List<UserFavorite> findRecentFavoritesByUserId(@Param("userId") Long userId,
                                                   org.springframework.data.domain.Pageable pageable);

    /**
     * Trouve les utilisateurs qui ont un joueur en favoris
     */
    @Query("SELECT DISTINCT uf.userId FROM UserFavorite uf WHERE uf.playerId = :playerId")
    List<Long> findUserIdsByPlayerId(@Param("playerId") Long playerId);

    /**
     * Compte le nombre total de fois qu'un joueur a été mis en favoris
     */
    Long countByPlayerId(Long playerId);

    /**
     * Trouve les favoris créés après une certaine date
     */
    @Query("SELECT uf FROM UserFavorite uf WHERE uf.createdDate > :date ORDER BY uf.createdDate DESC")
    List<UserFavorite> findFavoritesCreatedAfter(@Param("date") LocalDateTime date);

    /**
     * Supprime tous les favoris d'un utilisateur
     */
    @Modifying
    @Transactional
    void deleteByUserId(Long userId);
    Long countByPlayerIdAndCreatedDateAfter(Long playerId, LocalDateTime afterDate);

    /**
     * Joueurs les plus mis en favoris
     */
    @Query("SELECT f.playerId, COUNT(f) as favoriteCount " +
            "FROM UserFavorite f " +
            "WHERE f.createdDate BETWEEN :start AND :end " +
            "GROUP BY f.playerId " +
            "ORDER BY favoriteCount DESC")
    List<Object[]> findMostFavoritedPlayers(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);


}