package com.example.platforme_backend;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    // Méthodes existantes de base
    List<Player> findTop4ByOrderByMarketValueNumericDesc();
    Optional<Player> findById(Long id);
    List<Player> findAll();

    // Recherche simple avec pagination
    Page<Player> findByNameContainingIgnoreCaseOrClub_NameContainingIgnoreCaseOrPositionContainingIgnoreCase(
            String name, String clubName, String position, Pageable pageable);

    // Recherche par position avec pagination
    Page<Player> findByPositionContainingIgnoreCase(String position, Pageable pageable);

    // Recherche par club avec pagination
    Page<Player> findByClub_NameContainingIgnoreCase(String clubName, Pageable pageable);

    // Recherche par nationalité avec pagination
    Page<Player> findByNationalityContainingIgnoreCase(String nationality, Pageable pageable);

    // Recherche par fourchette d'âge avec pagination
    Page<Player> findByAgeBetween(Integer minAge, Integer maxAge, Pageable pageable);

    // Recherche par fourchette de valeur avec pagination
    Page<Player> findByMarketValueNumericBetween(Integer minValue, Integer maxValue, Pageable pageable);

    // Recherche avancée combinée avec pagination
    @Query("SELECT p FROM Player p WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            " LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            " LOWER(p.club.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            " LOWER(p.position) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:position IS NULL OR :position = '' OR LOWER(p.position) LIKE LOWER(CONCAT('%', :position, '%'))) AND " +
            "(:club IS NULL OR :club = '' OR LOWER(p.club.name) LIKE LOWER(CONCAT('%', :club, '%'))) AND " +
            "(:league IS NULL OR :league = '' OR LOWER(p.club.league) LIKE LOWER(CONCAT('%', :league, '%'))) AND " +
            "(:ageMin IS NULL OR p.age >= :ageMin) AND " +
            "(:ageMax IS NULL OR p.age <= :ageMax) AND " +
            "(:valueMin IS NULL OR p.marketValueNumeric >= :valueMin) AND " +
            "(:valueMax IS NULL OR p.marketValueNumeric <= :valueMax) AND " +
            "(:nationality IS NULL OR :nationality = '' OR LOWER(p.nationality) LIKE LOWER(CONCAT('%', :nationality, '%')))")
    Page<Player> findByAdvancedCriteria(
            @Param("query") String query,
            @Param("position") String position,
            @Param("club") String club,
            @Param("league") String league,
            @Param("ageMin") Integer ageMin,
            @Param("ageMax") Integer ageMax,
            @Param("valueMin") Integer valueMin,
            @Param("valueMax") Integer valueMax,
            @Param("nationality") String nationality,
            Pageable pageable);

    // Recherche de joueurs similaires avec pagination
    @Query("SELECT p FROM Player p WHERE " +
            "p.position = :position AND " +
            "p.age BETWEEN :minAge AND :maxAge AND " +
            "p.marketValueNumeric BETWEEN :minValue AND :maxValue AND " +
            "p.id != :excludeId " +
            "ORDER BY ABS(p.marketValueNumeric - :targetValue)")
    Page<Player> findSimilarPlayers(
            @Param("position") String position,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge,
            @Param("minValue") Integer minValue,
            @Param("maxValue") Integer maxValue,
            @Param("excludeId") Long excludeId,
            Pageable pageable);

    // Version simplifiée sans tri spécifique
    @Query("SELECT p FROM Player p WHERE " +
            "p.position = :position AND " +
            "p.age BETWEEN :minAge AND :maxAge AND " +
            "p.marketValueNumeric BETWEEN :minValue AND :maxValue AND " +
            "p.id != :excludeId")
    Page<Player> findSimilarPlayersBasic(
            @Param("position") String position,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge,
            @Param("minValue") Integer minValue,
            @Param("maxValue") Integer maxValue,
            @Param("excludeId") Long excludeId,
            Pageable pageable);

    // Obtenir des valeurs distinctes pour les filtres
    @Query("SELECT DISTINCT p.position FROM Player p WHERE p.position IS NOT NULL ORDER BY p.position")
    List<String> findDistinctPositions();

    @Query("SELECT DISTINCT c.name FROM Player p JOIN p.club c WHERE c.name IS NOT NULL ORDER BY c.name")
    List<String> findDistinctClubs();

    @Query("SELECT DISTINCT p.nationality FROM Player p WHERE p.nationality IS NOT NULL ORDER BY p.nationality")
    List<String> findDistinctNationalities();

    @Query("SELECT DISTINCT c.league FROM Player p JOIN p.club c WHERE c.league IS NOT NULL ORDER BY c.league")
    List<String> findDistinctLeagues();

    // Statistiques globales
    @Query("SELECT COUNT(p) FROM Player p")
    Long countAllPlayers();

    @Query("SELECT AVG(p.age) FROM Player p WHERE p.age IS NOT NULL")
    Double getAverageAge();

    @Query("SELECT AVG(p.marketValueNumeric) FROM Player p WHERE p.marketValueNumeric IS NOT NULL")
    Double getAverageMarketValue();

    @Query("SELECT MIN(p.age) FROM Player p WHERE p.age IS NOT NULL")
    Integer getMinAge();

    @Query("SELECT MAX(p.age) FROM Player p WHERE p.age IS NOT NULL")
    Integer getMaxAge();

    @Query("SELECT MIN(p.marketValueNumeric) FROM Player p WHERE p.marketValueNumeric IS NOT NULL")
    Integer getMinMarketValue();

    @Query("SELECT MAX(p.marketValueNumeric) FROM Player p WHERE p.marketValueNumeric IS NOT NULL")
    Integer getMaxMarketValue();

    // Top joueurs par position avec pagination
    @Query("SELECT p FROM Player p WHERE LOWER(p.position) LIKE LOWER(CONCAT('%', :position, '%')) " +
            "ORDER BY p.marketValueNumeric DESC")
    Page<Player> findTopPlayersByPosition(@Param("position") String position, Pageable pageable);

    // Joueurs récemment ajoutés avec pagination
    List<Player> findTop10ByOrderByCreatedAtDesc(Pageable pageable);

    // Recherche pour l'autocomplétion
    @Query("SELECT p.name FROM Player p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY p.name LIMIT 10")
    List<String> findPlayerNamesForAutocomplete(@Param("query") String query);

    // Méthodes supplémentaires corrigées
    List<Player> findByNameContainingIgnoreCase(String name);

    @Query("SELECT p FROM Player p WHERE LOWER(p.club.name) LIKE LOWER(CONCAT('%', :clubName, '%'))")
    List<Player> findByClubNameContainingIgnoreCase(@Param("clubName") String clubName);

    List<Player> findByNationalityContainingIgnoreCase(String nationality);
    List<Player> findByAgeBetween(Integer minAge, Integer maxAge);
    List<Player> findByMarketValueNumericBetween(Integer minValue, Integer maxValue);
    List<Player> findByPositionAndAgeBetween(String position, Integer minAge, Integer maxAge);

    @Query("SELECT p FROM Player p WHERE p.position = :position AND LOWER(p.club.name) LIKE LOWER(CONCAT('%', :clubName, '%'))")
    List<Player> findByPositionAndClubNameContainingIgnoreCase(
            @Param("position") String position,
            @Param("clubName") String clubName);

    List<Player> findByMarketValueNumericGreaterThanEqual(Integer minValue);
    List<Player> findByMarketValueNumericLessThanEqual(Integer maxValue);
    List<Player> findAllByOrderByMarketValueNumericDesc();
    List<Player> findAllByOrderByAgeAsc();
    List<Player> findAllByOrderByNameAsc();

    // Recherche des joueurs les plus jeunes/âgés
    @Query("SELECT p FROM Player p WHERE p.age = (SELECT MIN(p2.age) FROM Player p2 WHERE p2.age IS NOT NULL)")
    List<Player> findYoungestPlayers();

    @Query("SELECT p FROM Player p WHERE p.age = (SELECT MAX(p2.age) FROM Player p2 WHERE p2.age IS NOT NULL)")
    List<Player> findOldestPlayers();

    // Recherche des joueurs les plus/moins chers
    @Query("SELECT p FROM Player p WHERE p.marketValueNumeric = (SELECT MAX(p2.marketValueNumeric) FROM Player p2 WHERE p2.marketValueNumeric IS NOT NULL)")
    List<Player> findMostValuablePlayers();

    @Query("SELECT p FROM Player p WHERE p.marketValueNumeric = (SELECT MIN(p2.marketValueNumeric) FROM Player p2 WHERE p2.marketValueNumeric IS NOT NULL)")
    List<Player> findLeastValuablePlayers();

    // Recherche par tranche d'âge spécifique avec pagination
    @Query("SELECT p FROM Player p WHERE p.age >= :minAge AND p.age <= :maxAge ORDER BY p.marketValueNumeric DESC")
    Page<Player> findPlayersByAgeRange(@Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge, Pageable pageable);

    // Recherche des espoirs (jeunes talents) avec pagination
    @Query("SELECT p FROM Player p WHERE p.age <= 23 AND p.marketValueNumeric >= :minValue ORDER BY p.marketValueNumeric DESC")
    Page<Player> findYoungTalents(@Param("minValue") Integer minValue, Pageable pageable);

    // Recherche des vétérans avec pagination
    @Query("SELECT p FROM Player p WHERE p.age >= 32 ORDER BY p.marketValueNumeric DESC")
    Page<Player> findVeteranPlayers(Pageable pageable);

    // Recherche par première lettre du nom
    List<Player> findByNameStartingWithIgnoreCase(String letter);

    // Recherche combinée complexe avec pagination
    @Query("SELECT p FROM Player p WHERE " +
            "(:position IS NULL OR p.position = :position) AND " +
            "(:clubName IS NULL OR LOWER(p.club.name) LIKE LOWER(CONCAT('%', :clubName, '%'))) AND " +
            "(:nationality IS NULL OR LOWER(p.nationality) LIKE LOWER(CONCAT('%', :nationality, '%'))) AND " +
            "(:minAge IS NULL OR p.age >= :minAge) AND " +
            "(:maxAge IS NULL OR p.age <= :maxAge) AND " +
            "(:minValue IS NULL OR p.marketValueNumeric >= :minValue) AND " +
            "(:maxValue IS NULL OR p.marketValueNumeric <= :maxValue)")
    Page<Player> findByComplexCriteria(
            @Param("position") String position,
            @Param("clubName") String clubName,
            @Param("nationality") String nationality,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge,
            @Param("minValue") Integer minValue,
            @Param("maxValue") Integer maxValue,
            Pageable pageable);

    // Compteurs par critères
    @Query("SELECT COUNT(p) FROM Player p WHERE p.position = :position")
    long countByPosition(@Param("position") String position);

    @Query("SELECT COUNT(p) FROM Player p WHERE LOWER(p.club.name) LIKE LOWER(CONCAT('%', :clubName, '%'))")
    long countByClubName(@Param("clubName") String clubName);

    @Query("SELECT COUNT(p) FROM Player p WHERE p.nationality = :nationality")
    long countByNationality(@Param("nationality") String nationality);

    // Opportunités d'investissement
    @Query("SELECT p FROM Player p WHERE " +
            "p.age BETWEEN 18 AND 28 AND " +
            "p.marketValueNumeric BETWEEN :minValue AND :maxValue " +
            "ORDER BY p.marketValueNumeric ASC")
    List<Player> findOpportunityPlayers(@Param("minValue") Integer minValue,
                                        @Param("maxValue") Integer maxValue);

    @Query("SELECT p FROM Player p WHERE p.age <= 25 AND p.marketValueNumeric <= :maxValue " +
            "ORDER BY p.marketValueNumeric DESC")
    List<Player> findOpportunityPlayers(@Param("maxValue") Integer maxValue);

    default List<Player> findOpportunityPlayers() {
        return findOpportunityPlayers(50000000); // 50M comme seuil par défaut
    }

    // Version corrigée pour les changements de valeur
    @Query("SELECT p FROM Player p WHERE p.marketValueNumeric >= :minValue " +
            "ORDER BY p.marketValueNumeric DESC")
    List<Player> findPlayersWithRecentValueChanges(@Param("minValue") Double minValue);

    // Autres méthodes utiles
    Optional<Player> findByNameIgnoreCase(String name);

    // CORRECTION PRINCIPALE : Remplacer la méthode problématique
    @Query("SELECT p FROM Player p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Player> findByNameContainingIgnoreCaseCustom(@Param("name") String name);

    List<Player> findByNameStartsWithIgnoreCase(String name);
}