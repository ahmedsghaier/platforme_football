package com.example.platforme_backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = {"http://localhost:4200"})
public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    @Autowired
    private UserSearchLogRepository userSearchLogRepository;
    @Autowired
    private DashboardService dashboardService;
    private final PlayerService playerService;
    private ExportLogRepository exportLogRepository;

    public DashboardController(DashboardService dashboardService, PlayerService playerService) {
        this.dashboardService = dashboardService;
        this.playerService = playerService;
    }
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    /**
     * Récupère le dashboard complet avec gestion d'erreur robuste
     */
    @GetMapping("/complete")
    public ResponseEntity<?> getCompleteDashboard(@RequestParam Long userId) {
        logger.info("Request received for complete dashboard, userId={}", userId);

        try {
            // Validation des paramètres
            if (userId == null || userId <= 0) {
                logger.warn("Invalid userId parameter: {}", userId);
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid user ID parameter"));
            }

            CompleteDashboardDTO dashboard = dashboardService.getCompleteDashboard(userId);

            if (dashboard == null) {
                logger.warn("Dashboard service returned null for userId={}", userId);
                // Retourner des données vides plutôt qu'une erreur
                dashboard = createEmptyDashboard();
            }

            // Logging détaillé des données retournées
            logger.info("Dashboard data for userId={}: stats={}, searches={}, favorites={}, alerts={}",
                    userId,
                    dashboard.getStats() != null,
                    dashboard.getRecentSearches() != null ? dashboard.getRecentSearches().size() : 0,
                    dashboard.getFavoritePlayers() != null ? dashboard.getFavoritePlayers().size() : 0,
                    dashboard.getAlerts() != null ? dashboard.getAlerts().size() : 0);

            logger.info("Successfully retrieved complete dashboard for userId={}", userId);
            return ResponseEntity.ok(dashboard);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid argument for dashboard request userId={}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid request: " + e.getMessage()));

        } catch (Exception e) {
            logger.error("Unexpected error retrieving dashboard for userId={}: {}", userId, e.getMessage(), e);

            // En cas d'erreur, retourner des données par défaut plutôt qu'une erreur 500
            try {
                CompleteDashboardDTO fallbackDashboard = createEmptyDashboard();
                logger.info("Returning fallback dashboard data for userId={}", userId);
                return ResponseEntity.ok(fallbackDashboard);
            } catch (Exception fallbackError) {
                logger.error("Error creating fallback dashboard: {}", fallbackError.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse("Internal server error while retrieving dashboard"));
            }
        }
    }

    /**
     * Nouvelle méthode pour créer un dashboard vide
     */
    private CompleteDashboardDTO createEmptyDashboard() {
        CompleteDashboardDTO dashboard = new CompleteDashboardDTO();

        // Stats par défaut
        DashboardStatsDTO stats = new DashboardStatsDTO();
        stats.setSearchesThisMonth(0);
        stats.setSearchGrowthPercentage(0);
        stats.setFavoritePlayers(0);
        stats.setFavoritePlayersAddedThisWeek(0);
        stats.setComparisons(0);
        stats.setComparisonGrowthPercentage(0);
        stats.setReportsExported(0);
        stats.setReportsExportedThisWeek(0);
        dashboard.setStats(stats);

        // Listes vides
        dashboard.setRecentSearches(new ArrayList<>());
        dashboard.setRecentComparisons(new ArrayList<>());
        dashboard.setFavoritePlayers(new ArrayList<>());
        dashboard.setAlerts(new ArrayList<>());

        // Tendances par défaut
        MarketTrendsDTO trends = new MarketTrendsDTO();
        trends.setAttackersGrowth(0);
        trends.setMidfieldersGrowth(0);
        trends.setYoungTalentsGrowth(0);
        trends.setDefendersGrowth(0);
        dashboard.setMarketTrends(trends);

        return dashboard;
    }

    /**
     * Récupère uniquement les statistiques du dashboard
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats(@RequestParam Long userId) {
        logger.info("Request received for dashboard stats, userId={}", userId);

        try {
            if (userId == null || userId <= 0) {
                logger.warn("Invalid userId parameter: {}", userId);
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid user ID parameter"));
            }

            DashboardStatsDTO stats = dashboardService.getDashboardStats(userId);
            logger.info("Successfully retrieved dashboard stats for userId={}", userId);
            return ResponseEntity.ok(stats);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid argument for stats request userId={}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid request: " + e.getMessage()));

        } catch (Exception e) {
            logger.error("Error retrieving dashboard stats for userId={}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving dashboard statistics"));
        }
    }

    /**
     * Récupère les recherches récentes
     */
    @GetMapping("/recent-searches")
    public ResponseEntity<?> getRecentSearches(@RequestParam Long userId) {
        logger.info("Request received for recent searches, userId={}", userId);

        try {
            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid user ID parameter"));
            }

            List<RecentSearchDTO> searches = dashboardService.getRecentSearches(userId);

            logger.info("Returning {} recent searches for userId={}", searches.size(), userId);
            return ResponseEntity.ok(searches);

        } catch (Exception e) {
            logger.error("Error retrieving recent searches for userId={}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving recent searches"));
        }
    }


    /**
     * Récupère les comparaisons récentes
     */
    @GetMapping("/recent-comparisons")
    public ResponseEntity<?> getRecentComparisons(@RequestParam Long userId) {
        logger.info("Request received for recent comparisons, userId={}", userId);

        try {
            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid user ID parameter"));
            }

            List<RecentSearchDTO> comparisons = dashboardService.getRecentComparisons(userId);
            return ResponseEntity.ok(comparisons);

        } catch (Exception e) {
            logger.error("Error retrieving recent comparisons for userId={}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving recent comparisons"));
        }
    }

    /**
     * Récupère les joueurs favoris
     */
    @GetMapping("/favorite-players")
    public ResponseEntity<?> getFavoritePlayers(@RequestParam Long userId) {
        logger.info("Request received for favorite players, userId={}", userId);

        try {
            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid user ID parameter"));
            }

            List<FavoritePlayerDTO> favorites = dashboardService.getFavoritePlayers(userId);
            return ResponseEntity.ok(favorites);

        } catch (Exception e) {
            logger.error("Error retrieving favorite players for userId={}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving favorite players"));
        }
    }

    /**
     * Toggle un joueur dans les favoris
     */
    @PostMapping("/favorite-players/{playerId}")
    public ResponseEntity<?> toggleFavoritePlayer(
            @RequestParam Long userId,
            @PathVariable Long playerId) {
        logger.info("Request to toggle favorite player, userId={}, playerId={}", userId, playerId);

        try {
            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid user ID parameter"));
            }

            if (playerId == null || playerId <= 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid player ID parameter"));
            }

            boolean added = dashboardService.toggleFavoritePlayer(userId, playerId);
            String message = added ? "Player added to favorites" : "Player removed from favorites";

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", message);
            response.put("added", added);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid argument for toggle favorite userId={}, playerId={}: {}",
                    userId, playerId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid request: " + e.getMessage()));

        } catch (Exception e) {
            logger.error("Error toggling favorite player userId={}, playerId={}: {}",
                    userId, playerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error updating favorite player"));
        }
    }

    /**
     * Récupère les alertes
     */
    @GetMapping("/alerts")
    public ResponseEntity<?> getAlerts(@RequestParam Long userId) {
        logger.info("Request received for alerts, userId={}", userId);

        try {
            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid user ID parameter"));
            }

            List<AlertDTO> alerts = dashboardService.getAlerts(userId);
            return ResponseEntity.ok(alerts);

        } catch (Exception e) {
            logger.error("Error retrieving alerts for userId={}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving alerts"));
        }
    }

    /**
     * Marque une alerte comme lue
     */
    @PutMapping("/alerts/{alertId}/read")
    public ResponseEntity<?> markAlertAsRead(
            @RequestParam Long userId,
            @PathVariable Long alertId) {
        logger.info("Request to mark alert as read, userId={}, alertId={}", userId, alertId);

        try {
            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid user ID parameter"));
            }

            if (alertId == null || alertId <= 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid alert ID parameter"));
            }

            // Ici vous devriez implémenter la logique pour marquer l'alerte comme lue
            // dashboardService.markAlertAsRead(userId, alertId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Alert marked as read");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error marking alert as read userId={}, alertId={}: {}",
                    userId, alertId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error marking alert as read"));
        }
    }

    /**
     * Récupère les tendances du marché
     */
    @GetMapping("/market-trends")
    public ResponseEntity<?> getMarketTrends() {
        logger.info("Request received for market trends");

        try {
            MarketTrendsDTO trends = dashboardService.getMarketTrends();
            return ResponseEntity.ok(trends);

        } catch (Exception e) {
            logger.error("Error retrieving market trends: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving market trends"));
        }
    }

    /**
     * Enregistre une recherche utilisateur
     */
    @PostMapping("/log-search")
    public ResponseEntity<?> logUserSearch(@RequestBody UserSearchLog request) {
        logger.info("Request received to log search for userId={}, query='{}'",
                request.getUserId(), request.getSearchQuery());

        try {
            if (request.getUserId() == null || request.getUserId() <= 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid user ID"));
            }

            if (request.getSearchQuery() == null || request.getSearchQuery().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Search query cannot be empty"));
            }

            // Créer le DTO pour l'enregistrement
            UserSearchLogDTO searchLog = new UserSearchLogDTO();
            searchLog.setUserId(request.getUserId());
            searchLog.setSearchQuery(request.getSearchQuery().trim());
            searchLog.setPlayerId(request.getPlayerId());
            searchLog.setType(request.getType() != null ? request.getType() : "SEARCH");

            // CORRECTION : Si playerId n'est pas fourni, essayer de le trouver
            if (searchLog.getPlayerId() == null) {
                try {
                    Optional<Player> playerOpt = playerService.findByNameIgnoreCase(request.getSearchQuery().trim());
                    if (playerOpt.isPresent()) {
                        searchLog.setPlayerId(playerOpt.get().getId());
                        logger.info("Found matching player for search '{}': playerId={}",
                                request.getSearchQuery(), playerOpt.get().getId());
                    }
                } catch (Exception e) {
                    logger.debug("Could not find player for search query '{}': {}",
                            request.getSearchQuery(), e.getMessage());
                }
            }

            // Enregistrer la recherche
            dashboardService.logUserSearch(searchLog);

            logger.info("Successfully logged search for userId={}, playerId={}, query='{}'",
                    request.getUserId(), searchLog.getPlayerId(), request.getSearchQuery());

            return ResponseEntity.ok(Map.of("success", true, "message", "Search logged successfully"));

        } catch (Exception e) {
            logger.error("Error logging search for userId={}: {}", request.getUserId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error logging search"));
        }
    }

    /**
     * Enregistre une comparaison utilisateur
     */
    @PostMapping("/log-comparison")
    public ResponseEntity<Map<String, String>> logComparison(@RequestBody UserSearchLogDTO dto) {
        try {
            // Validation des paramètres d'entrée
            if (dto == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Request body cannot be null"));
            }

            if (dto.getUserId() == null || dto.getUserId() <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid user ID"));
            }

            if (dto.getSearchQuery() == null || dto.getSearchQuery().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Search query cannot be empty"));
            }

            // Définir le type comme COMPARISON
            dto.setType("COMPARISON");

            // Appeler la méthode de logging
            logUserSearch(dto);

            return ResponseEntity.ok(Map.of("message", "Comparison logged successfully"));

        } catch (Exception e) {
            logger.error("Error in logComparison endpoint: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

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

            // CORRECTION : Vérifier si le playerId est fourni, sinon le chercher par nom
            Long playerId = searchLog.getPlayerId();
            if (playerId == null && searchLog.getSearchQuery() != null) {
                // Essayer de trouver le joueur par nom
                PlayerRepository playerRepository = null;
                Optional<Player> playerOpt = playerRepository.findByNameIgnoreCase(searchLog.getSearchQuery().trim());
                if (playerOpt.isPresent()) {
                    playerId = playerOpt.get().getId();
                    logger.info("Found playerId {} for search query '{}'", playerId, searchLog.getSearchQuery());
                } else {
                    logger.warn("No player found for search query: '{}'", searchLog.getSearchQuery());
                }
            }

            UserSearchLog log = new UserSearchLog();
            log.setUserId(searchLog.getUserId());
            log.setPlayerId(playerId); // CORRECTION : Utiliser le playerId trouvé ou fourni
            log.setSearchQuery(searchLog.getSearchQuery());
            log.setType(searchLog.getType() != null ? searchLog.getType() : "SEARCH");
            log.setSearchDate(LocalDateTime.now());

            UserSearchLog savedLog = userSearchLogRepository.save(log);

            logger.info("Logged {} for user {}: playerId={}, query='{}', logId={}",
                    savedLog.getType(), savedLog.getUserId(), savedLog.getPlayerId(),
                    savedLog.getSearchQuery(), savedLog.getId());

        } catch (Exception e) {
            logger.error("Error logging user search: {}", e.getMessage(), e);
            // Ne pas lancer d'exception pour ne pas interrompre le flux principal
        }
    }


    /**
     * Exporte un rapport
     */
    @PostMapping("/export-report")
    public ResponseEntity<?> exportReport(
            @RequestParam Long userId,
            @RequestParam String format) {
        logger.info("Request to export report, userId={}, format={}", userId, format);

        try {
            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid user ID parameter"));
            }

            if (format == null || format.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Format parameter is required"));
            }

            byte[] reportData = dashboardService.generateReport(userId, format.toLowerCase());

            if (reportData == null || reportData.length == 0) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(createErrorResponse("No data available for report generation"));
            }

            String contentType = "pdf".equals(format.toLowerCase()) ?
                    "application/pdf" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Content-Disposition", "attachment; filename=dashboard_report." + format)
                    .body(reportData);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid export parameters userId={}, format={}: {}",
                    userId, format, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid request: " + e.getMessage()));

        } catch (Exception e) {
            logger.error("Error exporting report userId={}, format={}: {}",
                    userId, format, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error generating report"));
        }
    }

    /**
     * Endpoint de test pour vérifier la connectivité
     */
    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint(@RequestParam(required = false) Long userId) {
        logger.info("Test endpoint called with userId={}", userId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "Dashboard API is working");

        if (userId != null) {
            response.put("userId", userId);
            response.put("userIdValid", userId > 0);
        }

        return ResponseEntity.ok(response);
    }
    // NOUVEAU : Endpoint pour effectuer une recherche de joueur
    @GetMapping("/search-players")
    public ResponseEntity<?> searchPlayers(@RequestParam Long userId,
                                           @RequestParam String query) {
        logger.info("Player search request: userId={}, query='{}'", userId, query);

        try {
            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid user ID"));
            }

            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Search query cannot be empty"));
            }

            // Effectuer la recherche
            List<Player> players = playerService.searchPlayers(query.trim());

            // CORRECTION : Enregistrer automatiquement la recherche
            UserSearchLogDTO searchLog = new UserSearchLogDTO();
            searchLog.setUserId(userId);
            searchLog.setSearchQuery(query.trim());
            searchLog.setType("SEARCH");

            // Si un joueur exact est trouvé, enregistrer son ID
            Optional<Player> exactMatch = players.stream()
                    .filter(p -> p.getName().equalsIgnoreCase(query.trim()))
                    .findFirst();
            if (exactMatch.isPresent()) {
                searchLog.setPlayerId(exactMatch.get().getId());
            }

            // Enregistrer la recherche de manière asynchrone
            try {
                dashboardService.logUserSearch(searchLog);
            } catch (Exception e) {
                logger.warn("Failed to log search, but continuing with results: {}", e.getMessage());
            }

            // Convertir les résultats en DTO si nécessaire
            Map<String, Object> response = Map.of(
                    "players", players,
                    "totalResults", players.size(),
                    "exactMatch", exactMatch.isPresent()
            );

            logger.info("Found {} players for query '{}', userId={}", players.size(), query, userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error searching players for userId={}, query='{}': {}",
                    userId, query, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error searching players"));
        }
    }
    /**
     * Gestion globale des erreurs pour ce contrôleur
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception e) {
        logger.error("Unhandled exception in DashboardController: {}", e.getMessage(), e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An unexpected error occurred"));
    }

    /**
     * Gestion des erreurs d'arguments invalides
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("Invalid argument in DashboardController: {}", e.getMessage());

        return ResponseEntity.badRequest()
                .body(createErrorResponse("Invalid request: " + e.getMessage()));
    }

    /**
     * Crée une réponse d'erreur standardisée
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", true);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    @PostMapping("/log-export")
    public ResponseEntity<Map<String, String>> logExport(@RequestBody ExportLogDTO dto) {
        logger.info("Request to log export: userId={}, format={}, dataType={}",
                dto.getUserId(), dto.getExportFormat(), dto.getDataType());

        try {
            if (dto.getUserId() == null || dto.getUserId() <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid user ID"));
            }

            if (dto.getExportFormat() == null || dto.getExportFormat().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Export format cannot be empty"));
            }

            // Créer l'entité pour l'export log
            ExportLog exportLog = new ExportLog();
            exportLog.setUserId(dto.getUserId());
            exportLog.setExportFormat(dto.getExportFormat().toUpperCase());
            exportLog.setDataType(dto.getDataType());
            exportLog.setPlayerIds(dto.getPlayerIds().toString());
            exportLog.setExportDate(LocalDateTime.now());

            // Sauvegarder dans la base de données
            exportLogRepository.save(exportLog);

            logger.info("Export logged successfully: userId={}, format={}",
                    dto.getUserId(), dto.getExportFormat());

            return ResponseEntity.ok(Map.of("message", "Export logged successfully"));

        } catch (Exception e) {
            logger.error("Error logging export: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }
}