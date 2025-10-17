package com.example.platforme_backend;

import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    private static final String UNKNOWN_PLAYER = "Unknown Player";
    private static final String UNKNOWN_CLUB = "Unknown Club";

    @Autowired
    private UserSearchLogRepository userSearchLogRepository;

    @Autowired
    private UserFavoriteRepository userFavoriteRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private ReportGenerationService reportGenerationService;
    @Autowired
    private UserFavoriteRepository favoritePlayerRepository;
    @Autowired
    private ExportLogRepository exportLogRepository;


    /**
     * Récupère les statistiques du dashboard pour un utilisateur avec gestion d'erreur améliorée
     */
    public DashboardStatsDTO getDashboardStats(Long userId) {
        try {
            DashboardStatsDTO stats = new DashboardStatsDTO();

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime weekStart = now.minusWeeks(1);
            LocalDateTime lastMonthStart = monthStart.minusMonths(1);
            LocalDateTime lastMonthEnd = monthStart.minusDays(1);

            // Recherches ce mois
            int searchesThisMonth = userSearchLogRepository.countByUserIdAndTypeAndSearchDateBetween(
                    userId, "SEARCH", monthStart, now);

            int searchesLastMonth = userSearchLogRepository.countByUserIdAndTypeAndSearchDateBetween(
                    userId, "SEARCH", lastMonthStart, lastMonthEnd);

            // Comparaisons ce mois - CORRECTION : Compter les comparaisons par type
            int comparisonsThisMonth = userSearchLogRepository.countByUserIdAndTypeAndSearchDateBetween(
                    userId, "COMPARISON", monthStart, now);

            int comparisonsLastMonth = userSearchLogRepository.countByUserIdAndTypeAndSearchDateBetween(
                    userId, "COMPARISON", lastMonthStart, lastMonthEnd);

            // Joueurs favoris
            int favoritePlayers = favoritePlayerRepository.countByUserId(userId);
            int favoritePlayersLastWeek = favoritePlayerRepository.countByUserIdAndCreatedDateBetween(
                    userId, weekStart, now);

            // Rapports exportés - CORRECTION : Utiliser ExportLogRepository
            int reportsExportedThisMonth = exportLogRepository.countByUserIdAndExportDateBetween(
                    userId, monthStart, now);

            int reportsExportedThisWeek = exportLogRepository.countByUserIdAndExportDateBetween(
                    userId, weekStart, now);

            // Calculer les pourcentages de croissance
            double searchGrowthPercentage = calculateGrowthPercentage(searchesThisMonth, searchesLastMonth);
            double comparisonGrowthPercentage = calculateGrowthPercentage(comparisonsThisMonth, comparisonsLastMonth);

            // Remplir le DTO
            stats.setSearchesThisMonth(searchesThisMonth);
            stats.setSearchGrowthPercentage(searchGrowthPercentage);
            stats.setFavoritePlayers(favoritePlayers);
            stats.setFavoritePlayersAddedThisWeek(favoritePlayersLastWeek);
            stats.setComparisons(comparisonsThisMonth);
            stats.setComparisonGrowthPercentage(comparisonGrowthPercentage);
            stats.setReportsExported(reportsExportedThisMonth);
            stats.setReportsExportedThisWeek(reportsExportedThisWeek);

            logger.info("Dashboard stats for userId {}: searches={}, comparisons={}, exports={}, favorites={}",
                    userId, searchesThisMonth, comparisonsThisMonth, reportsExportedThisMonth, favoritePlayers);

            return stats;

        } catch (Exception e) {
            logger.error("Error calculating dashboard stats for userId {}: {}", userId, e.getMessage(), e);
            return createEmptyStats();
        }
    }
    private double calculateGrowthPercentage(int current, int previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((double) (current - previous) / previous) * 100.0;
    }

    private DashboardStatsDTO createEmptyStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        stats.setSearchesThisMonth(0);
        stats.setSearchGrowthPercentage(0);
        stats.setFavoritePlayers(0);
        stats.setFavoritePlayersAddedThisWeek(0);
        stats.setComparisons(0);
        stats.setComparisonGrowthPercentage(0);
        stats.setReportsExported(0);
        stats.setReportsExportedThisWeek(0);
        return stats;
    }

    /**
     * Wrapper sécurisé pour les opérations de comptage
     */
    private Long safeCount(Supplier<Long> countOperation) {
        try {
            Long result = countOperation.get();
            return result != null ? result : 0L;
        } catch (Exception e) {
            logger.warn("Error in count operation: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * Récupère les recherches récentes d'un utilisateur avec gestion d'erreur
     */
    public List<RecentSearchDTO> getRecentSearches(Long userId) {
        try {
            if (userId == null || userId <= 0) {
                logger.error("Invalid user ID: {}", userId);
                return Collections.emptyList();
            }

            // CORRECTION: Récupérer uniquement les recherches SEARCH, exclure les navigations
            List<UserSearchLog> searches = userSearchLogRepository
                    .findTop10ByUserIdAndTypeAndSearchQueryNotOrderBySearchDateDesc(
                            userId, "SEARCH", "dashboard_navigation"
                    );

            if (searches.isEmpty()) {
                logger.info("No recent searches found for userId: {}", userId);
                return Collections.emptyList();
            }

            logger.info("Processing {} searches for userId: {}", searches.size(), userId);

            return searches.stream()
                    .map(search -> {
                        try {
                            String playerName;
                            String clubName = UNKNOWN_CLUB;
                            Long searchPlayerId = search.getPlayerId();

                            // CORRECTION: Priorité au playerId existant
                            if (searchPlayerId != null && searchPlayerId > 0) {
                                Optional<Player> playerOpt = playerRepository.findById(searchPlayerId);
                                if (playerOpt.isPresent()) {
                                    Player player = playerOpt.get();
                                    playerName = player.getName();
                                    clubName = Optional.ofNullable(player.getClub())
                                            .map(Club::getName)
                                            .orElse(UNKNOWN_CLUB);
                                } else {
                                    // Player supprimé mais log existe encore
                                    playerName = search.getSearchQuery();
                                    logger.warn("Player {} not found for search log", searchPlayerId);
                                }
                            } else {
                                // CORRECTION: Pas de playerId, essayer de trouver et mettre à jour
                                playerName = search.getSearchQuery();
                                try {
                                    Optional<Player> playerOpt = playerRepository
                                            .findByNameIgnoreCase(search.getSearchQuery().trim());
                                    if (playerOpt.isPresent()) {
                                        Player player = playerOpt.get();
                                        playerName = player.getName();
                                        clubName = Optional.ofNullable(player.getClub())
                                                .map(Club::getName)
                                                .orElse(UNKNOWN_CLUB);

                                        // CORRECTION: Mettre à jour le log avec le playerId trouvé
                                        search.setPlayerId(player.getId());
                                        try {
                                            userSearchLogRepository.save(search);
                                            searchPlayerId = player.getId();
                                            logger.debug("Updated search log with playerId: {}", player.getId());
                                        } catch (Exception updateEx) {
                                            logger.debug("Could not update search log: {}", updateEx.getMessage());
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.debug("Could not find player by name '{}': {}",
                                            search.getSearchQuery(), e.getMessage());
                                }
                            }

                            return new RecentSearchDTO(
                                    search.getId(),
                                    playerName,
                                    clubName,
                                    search.getSearchDate().toLocalDate().toString(),
                                    searchPlayerId
                            );

                        } catch (Exception e) {
                            logger.error("Error processing search log {}: {}", search.getId(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting recent searches for userId={}: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    /**
     * Récupère les joueurs favoris avec gestion d'erreur améliorée
     */
    public List<FavoritePlayerDTO> getFavoritePlayers(Long userId) {
        try {
            if (userId == null || userId <= 0) {
                logger.error("Invalid user ID for favorite players: {}", userId);
                return Collections.emptyList();
            }

            List<UserFavorite> favorites = userFavoriteRepository.findByUserIdOrderByCreatedDateDesc(userId);
            if (favorites == null) {
                return Collections.emptyList();
            }

            return favorites.stream().map(favorite -> {
                try {
                    if (favorite.getPlayerId() == null) {
                        logger.warn("Favorite has null playerId for userId={}", userId);
                        return null;
                    }

                    Player player = playerRepository.findById(favorite.getPlayerId()).orElse(null);
                    if (player == null) {
                        logger.warn("Player not found for favorite playerId={}", favorite.getPlayerId());
                        return null;
                    }

                    String clubName = Optional.ofNullable(player.getClub())
                            .map(Club::getName)
                            .orElse(UNKNOWN_CLUB);

                    // Calculer la tendance de manière sécurisée
                    String trend = safeCalculatePlayerTrend(player.getId());

                    return new FavoritePlayerDTO(
                            player.getName(),
                            clubName,
                            formatValue(player.getMarketValueNumeric()),
                            trend
                    );
                } catch (Exception e) {
                    logger.error("Error processing favorite player for userId={}, playerId={}: {}",
                            userId, favorite.getPlayerId(), e.getMessage());
                    return null;
                }
            }).filter(dto -> dto != null).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting favorite players for userId={}: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Ajoute ou retire un joueur des favoris avec validation
     */
    public boolean toggleFavoritePlayer(Long userId, Long playerId) {
        try {
            if (userId == null || userId <= 0) {
                throw new IllegalArgumentException("Invalid user ID");
            }
            if (playerId == null || playerId <= 0) {
                throw new IllegalArgumentException("Invalid player ID");
            }

            // Vérifier que le joueur existe
            if (!playerRepository.existsById(playerId)) {
                throw new IllegalArgumentException("Player not found with ID: " + playerId);
            }

            Optional<UserFavorite> existingOpt = userFavoriteRepository.findByUserIdAndPlayerId(userId, playerId);

            if (existingOpt.isPresent()) {
                userFavoriteRepository.delete(existingOpt.get());
                logger.info("Removed favorite player {} for user {}", playerId, userId);
                return false; // Retiré
            } else {
                UserFavorite newFavorite = new UserFavorite();
                newFavorite.setUserId(userId);
                newFavorite.setPlayerId(playerId);
                newFavorite.setCreatedDate(LocalDateTime.now());
                userFavoriteRepository.save(newFavorite);
                logger.info("Added favorite player {} for user {}", playerId, userId);
                return true; // Ajouté
            }
        } catch (Exception e) {
            logger.error("Error toggling favorite player for userId={}, playerId={}: {}",
                    userId, playerId, e.getMessage(), e);
            throw new RuntimeException("Failed to toggle favorite player", e);
        }
    }

    /**
     * Récupère les alertes pour un utilisateur avec gestion d'erreur
     */
    public List<AlertDTO> getAlerts(Long userId) {
        try {
            if (userId == null || userId <= 0) {
                logger.error("Invalid user ID for alerts: {}", userId);
                return Collections.emptyList();
            }

            List<Alert> alerts = alertRepository.findByUserIdOrderByCreatedDateDesc(userId);
            if (alerts == null) {
                return Collections.emptyList();
            }

            return alerts.stream().map(alert -> {
                try {
                    String playerName = UNKNOWN_PLAYER;
                    if (alert.getPlayerId() != null) {
                        Player player = playerRepository.findById(alert.getPlayerId()).orElse(null);
                        if (player != null) {
                            playerName = player.getName();
                        }
                    }

                    String timeAgo = calculateTimeAgo(alert.getCreatedDate());

                    return new AlertDTO(
                            alert.getType(),
                            playerName,
                            alert.getMessage(),
                            timeAgo
                    );
                } catch (Exception e) {
                    logger.error("Error processing alert for userId={}: {}", userId, e.getMessage());
                    return null;
                }
            }).filter(dto -> dto != null).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting alerts for userId={}: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Récupère les tendances du marché avec gestion d'erreur
     */
    public MarketTrendsDTO getMarketTrends() {
        try {
            // Calculer les tendances par position avec des valeurs par défaut
            double attackersGrowth = safeCalculatePositionTrend("Attaquant");
            double midfieldersGrowth = safeCalculatePositionTrend("Milieu");
            double youngTalentsGrowth = safeCalculateYoungTalentsTrend();
            double defendersGrowth = safeCalculatePositionTrend("Défenseur");

            return new MarketTrendsDTO(
                    attackersGrowth,
                    midfieldersGrowth,
                    youngTalentsGrowth,
                    defendersGrowth
            );
        } catch (Exception e) {
            logger.error("Error getting market trends: {}", e.getMessage(), e);
            // Retourner des tendances neutres
            return new MarketTrendsDTO(0.0, 0.0, 0.0, 0.0);
        }
    }

    /**
     * Enregistre une recherche utilisateur avec validation
     */
    public void logUserSearch(UserSearchLogDTO searchLog) {
        try {
            if (searchLog == null) {
                throw new IllegalArgumentException("Search log cannot be null");
            }
            if (searchLog.getUserId() == null || searchLog.getUserId() <= 0) {
                throw new IllegalArgumentException("Invalid user ID");
            }
            if (searchLog.getSearchQuery() == null || searchLog.getSearchQuery().isBlank()) {
                throw new IllegalArgumentException("Search query cannot be empty");
            }

            UserSearchLog log = new UserSearchLog();
            log.setUserId(searchLog.getUserId());
            log.setPlayerId(searchLog.getPlayerId());
            log.setSearchQuery(searchLog.getSearchQuery());
            log.setType(searchLog.getType() != null ? searchLog.getType() : "SEARCH");
            log.setSearchDate(LocalDateTime.now());
            userSearchLogRepository.save(log);

            logger.info("Logged {} for user {}: playerId={}, query={}",
                    log.getType(), log.getUserId(), log.getPlayerId(), log.getSearchQuery());
        } catch (Exception e) {
            logger.error("Error logging user search: {}", e.getMessage(), e);
            // Ne pas lancer d'exception pour ne pas interrompre le flux principal
        }
    }

    /**
     * Génère un rapport selon le format demandé avec gestion d'erreur
     */
    public byte[] generateReport(Long userId, String format) {
        try {
            if (userId == null || userId <= 0) {
                throw new IllegalArgumentException("Invalid user ID");
            }
            if (format == null || format.isBlank()) {
                throw new IllegalArgumentException("Format cannot be empty");
            }

            CompleteDashboardDTO dashboardData = getCompleteDashboard(userId);

            if ("pdf".equals(format)) {
                return reportGenerationService.generatePdfReport(dashboardData);
            } else if ("excel".equals(format) || "csv".equals(format)) {
                return reportGenerationService.generateCsvReport(dashboardData);
            } else {
                throw new IllegalArgumentException("Format non supporté: " + format);
            }
        } catch (Exception e) {
            logger.error("Error generating report for userId={}, format={}: {}",
                    userId, format, e.getMessage(), e);
            throw new RuntimeException("Failed to generate report", e);
        }
    }

    /**
     * Récupère toutes les données du dashboard avec gestion d'erreur robuste
     */
    public CompleteDashboardDTO getCompleteDashboard(Long userId) {
        try {
            if (userId == null || userId <= 0) {
                logger.error("Invalid userId: {}", userId);
                throw new IllegalArgumentException("Invalid user ID");
            }

            logger.info("Fetching complete dashboard for userId={}", userId);

            // Récupérer chaque section avec gestion d'erreur individuelle
            DashboardStatsDTO stats = getDashboardStats(userId);
            List<RecentSearchDTO> searches = getRecentSearches(userId);
            List<RecentSearchDTO> comparisons = getRecentComparisons(userId);
            List<FavoritePlayerDTO> favorites = getFavoritePlayers(userId);
            List<AlertDTO> alerts = getAlerts(userId);
            MarketTrendsDTO trends = getMarketTrends();

            CompleteDashboardDTO result = new CompleteDashboardDTO(stats, searches, comparisons, favorites, alerts, trends);
            logger.info("Successfully fetched complete dashboard for userId={}", userId);
            return result;
        } catch (Exception e) {
            logger.error("Error fetching complete dashboard for userId={}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch complete dashboard", e);
        }
    }

    /**
     * Récupère les comparaisons récentes avec gestion d'erreur
     */
    public List<RecentSearchDTO> getRecentComparisons(Long userId) {
        try {
            if (userId == null || userId <= 0) {
                logger.error("Invalid user ID for recent searches: {}", userId);
                return Collections.emptyList();
            }

            List<UserSearchLog> searches = userSearchLogRepository.findTop10ByUserIdAndTypeOrderBySearchDateDesc(userId, "SEARCH");
            if (searches == null || searches.isEmpty()) {
                logger.info("No recent searches found for userId={}", userId);
                return Collections.emptyList(); // Retourner liste vide au lieu de données de test
            }

            List<RecentSearchDTO> result = searches.stream()
                    .map(search -> {
                        try {
                            if (search.getPlayerId() == null) {
                                logger.warn("Search log has null playerId for userId={}", userId);
                                return null;
                            }

                            Optional<Player> playerOpt = playerRepository.findById(search.getPlayerId());
                            if (playerOpt.isEmpty()) {
                                logger.warn("Player not found for playerId={}", search.getPlayerId());
                                return new RecentSearchDTO(
                                        UNKNOWN_PLAYER,
                                        UNKNOWN_CLUB,
                                        search.getSearchDate().toLocalDate().toString()
                                );
                            }

                            Player player = playerOpt.get();
                            String clubName = Optional.ofNullable(player.getClub())
                                    .map(Club::getName)
                                    .orElse(UNKNOWN_CLUB);

                            return new RecentSearchDTO(
                                    player.getName(),
                                    clubName,
                                    search.getSearchDate().toLocalDate().toString()
                            );
                        } catch (Exception e) {
                            logger.error("Error processing search for playerId={}: {}", search.getPlayerId(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());

            logger.info("Found {} valid recent searches for userId={}", result.size(), userId);
            return result;

        } catch (Exception e) {
            logger.error("Error getting recent searches for userId={}: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // Méthodes utilitaires privées avec gestion d'erreur

    private double calculateGrowthPercentage(Long current, Long previous) {
        try {
            if (current == null) current = 0L;
            if (previous == null) previous = 0L;
            if (previous == 0) return current > 0 ? 100.0 : 0.0;
            return ((double)(current - previous) / previous) * 100.0;
        } catch (Exception e) {
            logger.warn("Error calculating growth percentage: {}", e.getMessage());
            return 0.0;
        }
    }

    private String safeCalculatePlayerTrend(Long playerId) {
        try {
            return calculatePlayerTrend(playerId);
        } catch (Exception e) {
            logger.warn("Error calculating player trend for playerId={}: {}", playerId, e.getMessage());
            return "0%";
        }
    }

    private String calculatePlayerTrend(Long playerId) {
        // Logique pour calculer la tendance d'un joueur
        // À implémenter selon vos besoins (historique des valeurs, etc.)
        return "+5%"; // Exemple
    }

    private String formatValue(Integer value) {
        try {
            if (value == null) return "N/A";
            if (value >= 1000000) {
                return (value / 1000000) + "M€";
            } else if (value >= 1000) {
                return (value / 1000) + "K€";
            }
            return value + "€";
        } catch (Exception e) {
            logger.warn("Error formatting value: {}", e.getMessage());
            return "N/A";
        }
    }

    private String calculateTimeAgo(LocalDateTime dateTime) {
        try {
            if (dateTime == null) return "N/A";
            LocalDateTime now = LocalDateTime.now();
            long hours = ChronoUnit.HOURS.between(dateTime, now);

            if (hours < 1) {
                long minutes = ChronoUnit.MINUTES.between(dateTime, now);
                return minutes + "min";
            } else if (hours < 24) {
                return hours + "h";
            } else {
                long days = ChronoUnit.DAYS.between(dateTime, now);
                return days + "j";
            }
        } catch (Exception e) {
            logger.warn("Error calculating time ago: {}", e.getMessage());
            return "N/A";
        }
    }

    private Long getReportsExportedCount(Long userId, LocalDateTime start, LocalDateTime end) {
        try {
            // À implémenter selon votre logique d'export de rapports
            // Pour l'instant, retourner 0 pour éviter les erreurs
            return 0L;
        } catch (Exception e) {
            logger.warn("Error getting reports exported count: {}", e.getMessage());
            return 0L;
        }
    }

    private double safeCalculatePositionTrend(String position) {
        try {
            return calculatePositionTrend(position);
        } catch (Exception e) {
            logger.warn("Error calculating position trend for {}: {}", position, e.getMessage());
            return 0.0;
        }
    }

    private double calculatePositionTrend(String position) {
        try {
            // Logique pour calculer la tendance par position
            return Math.random() * 20; // Exemple
        } catch (Exception e) {
            logger.warn("Error in position trend calculation: {}", e.getMessage());
            return 0.0;
        }
    }

    private double safeCalculateYoungTalentsTrend() {
        try {
            return calculateYoungTalentsTrend();
        } catch (Exception e) {
            logger.warn("Error calculating young talents trend: {}", e.getMessage());
            return 0.0;
        }
    }

    private double calculateYoungTalentsTrend() {
        try {
            // Logique pour calculer la tendance des jeunes talents
            return Math.random() * 15; // Exemple
        } catch (Exception e) {
            logger.warn("Error in young talents trend calculation: {}", e.getMessage());
            return 0.0;
        }
    }


}