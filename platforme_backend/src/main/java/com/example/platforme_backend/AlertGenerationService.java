package com.example.platforme_backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class AlertGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(AlertGenerationService.class);
    private static final double SIGNIFICANT_VALUE_CHANGE_THRESHOLD = 5.0; // 5%
    private static final double MARKET_TREND_THRESHOLD = 10.0; // 10%
    private static final int OPPORTUNITY_MAX_VALUE = 50000000; // 50M
    private static final int OPPORTUNITY_MIN_VALUE = 5000000;  // 5M

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private UserFavoriteRepository userFavoriteRepository;

    /**
     * Génère des alertes basées sur les changements de valeur des joueurs
     */
    public void generateValueChangeAlerts() {
        try {
            logger.info("Génération des alertes de changement de valeur...");

            List<Player> playersWithValueChanges = findPlayersWithSignificantValueChanges();
            logger.info("Nombre de joueurs avec changements significatifs: {}", playersWithValueChanges.size());

            for (Player player : playersWithValueChanges) {
                List<UserFavorite> interestedUsers = userFavoriteRepository.findByPlayerId(player.getId());

                for (UserFavorite favorite : interestedUsers) {
                    createValueChangeAlert(favorite.getUserId(), player);
                }
            }

            logger.info("Génération des alertes de changement de valeur terminée");
        } catch (Exception e) {
            logger.error("Erreur lors de la génération des alertes de changement de valeur", e);
            throw new RuntimeException("Erreur lors de la génération des alertes", e);
        }
    }

    /**
     * Génère des alertes d'opportunités d'investissement
     */
    public void generateOpportunityAlerts() {
        try {
            logger.info("Génération des alertes d'opportunité...");

            List<Player> opportunityPlayers = findOpportunityPlayers();
            logger.info("Nombre d'opportunités détectées: {}", opportunityPlayers.size());

            for (Player player : opportunityPlayers) {
                List<Long> interestedUserIds = findInterestedUsers(player);

                for (Long userId : interestedUserIds) {
                    createOpportunityAlert(userId, player);
                }
            }

            logger.info("Génération des alertes d'opportunité terminée");
        } catch (Exception e) {
            logger.error("Erreur lors de la génération des alertes d'opportunité", e);
            throw new RuntimeException("Erreur lors de la génération des alertes d'opportunité", e);
        }
    }

    /**
     * Génère des alertes de tendances du marché
     */
    public void generateMarketTrendAlerts() {
        try {
            logger.info("Génération des alertes de tendance marché...");

            Map<String, Double> positionTrends = calculatePositionTrends();
            logger.info("Tendances calculées pour {} positions", positionTrends.size());

            for (Map.Entry<String, Double> trend : positionTrends.entrySet()) {
                if (trend.getValue() > MARKET_TREND_THRESHOLD) {
                    List<Long> allUsers = getAllActiveUsers();

                    for (Long userId : allUsers) {
                        createMarketTrendAlert(userId, trend.getKey(), trend.getValue());
                    }
                }
            }

            logger.info("Génération des alertes de tendance marché terminée");
        } catch (Exception e) {
            logger.error("Erreur lors de la génération des alertes de tendance marché", e);
            throw new RuntimeException("Erreur lors de la génération des alertes de tendance marché", e);
        }
    }

    private void createValueChangeAlert(Long userId, Player player) {
        try {
            Double growth = calculateGrowthPercentage(player.getId());
            String message = String.format(
                    "Valeur en hausse de %.2f%% cette semaine pour %s",
                    growth != null ? growth : 0.0,
                    player.getName() != null ? player.getName() : "joueur inconnu"
            );

            saveAlert(userId, player.getId(), "increase", message);
            logger.debug("Alerte de changement de valeur créée pour l'utilisateur {} et le joueur {}",
                    userId, player.getId());
        } catch (Exception e) {
            logger.error("Erreur lors de la création de l'alerte de changement de valeur pour l'utilisateur {} et le joueur {}",
                    userId, player.getId(), e);
        }
    }

    private void createOpportunityAlert(Long userId, Player player) {
        try {
            String message = String.format(
                    "Opportunité d'investissement détectée pour %s",
                    player.getName() != null ? player.getName() : "joueur inconnu"
            );

            saveAlert(userId, player.getId(), "opportunity", message);
            logger.debug("Alerte d'opportunité créée pour l'utilisateur {} et le joueur {}",
                    userId, player.getId());
        } catch (Exception e) {
            logger.error("Erreur lors de la création de l'alerte d'opportunité pour l'utilisateur {} et le joueur {}",
                    userId, player.getId(), e);
        }
    }

    private void createMarketTrendAlert(Long userId, String position, Double growthPercentage) {
        try {
            String message = String.format(
                    "Tendance positive sur le marché des %s (+%.1f%%)",
                    position != null ? position : "joueurs",
                    growthPercentage != null ? growthPercentage : 0.0
            );

            saveAlert(userId, null, "market", message);
            logger.debug("Alerte de tendance marché créée pour l'utilisateur {} - position: {}",
                    userId, position);
        } catch (Exception e) {
            logger.error("Erreur lors de la création de l'alerte de tendance marché pour l'utilisateur {}",
                    userId, e);
        }
    }

    private void saveAlert(Long userId, Long playerId, String type, String message) {
        try {
            // Vérifier si une alerte similaire n'existe pas déjà récemment
            if (!isDuplicateAlert(userId, playerId, type, message)) {
                Alert alert = new Alert(userId, playerId, type, message);
                alert.setCreatedDate(LocalDateTime.now());
                alertRepository.save(alert);
                logger.debug("Alerte sauvegardée: type={}, userId={}, playerId={}", type, userId, playerId);
            } else {
                logger.debug("Alerte en double évitée: type={}, userId={}, playerId={}", type, userId, playerId);
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la sauvegarde de l'alerte: type={}, userId={}, playerId={}",
                    type, userId, playerId, e);
        }
    }

    private boolean isDuplicateAlert(Long userId, Long playerId, String type, String message) {
        // Vérifier s'il existe une alerte similaire dans les dernières 24h
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        // Cette méthode devrait être ajoutée au AlertRepository
        // return alertRepository.existsByUserIdAndPlayerIdAndTypeAndCreatedDateAfter(userId, playerId, type, yesterday);

        // Pour l'instant, on retourne false (pas de vérification de doublon)
        return false;
    }

    // Méthodes utilitaires avec implémentation améliorée
    private List<Player> findPlayersWithSignificantValueChanges() {
        try {
            // Implémentation réelle à faire selon votre logique métier
            // Exemple: retourner les joueurs avec changement > SIGNIFICANT_VALUE_CHANGE_THRESHOLD
            return playerRepository.findPlayersWithRecentValueChanges(SIGNIFICANT_VALUE_CHANGE_THRESHOLD);
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des joueurs avec changements significatifs", e);
            return List.of(); // Retourner une liste vide en cas d'erreur
        }
    }

    private List<Player> findOpportunityPlayers() {
        try {
            // Utiliser la méthode du repository avec paramètres
            return playerRepository.findOpportunityPlayers(OPPORTUNITY_MIN_VALUE, OPPORTUNITY_MAX_VALUE);
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des opportunités", e);
            // Fallback vers la méthode sans paramètres
            return playerRepository.findOpportunityPlayers();
        }
    }
    private List<Long> findInterestedUsers(Player player) {
        try {
            // Trouver les utilisateurs qui ont ce joueur en favoris
            List<UserFavorite> favorites = userFavoriteRepository.findByPlayerId(player.getId());
            return favorites.stream()
                    .map(UserFavorite::getUserId)
                    .distinct()
                    .toList();
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des utilisateurs intéressés pour le joueur {}",
                    player.getId(), e);
            return List.of();
        }
    }

    private List<Long> getAllActiveUsers() {
        try {
            // Implémentation réelle à faire - récupérer depuis UserRepository
            // return userRepository.findActiveUserIds();
            return List.of(1L, 2L, 3L); // Placeholder temporaire
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des utilisateurs actifs", e);
            return List.of();
        }
    }

    private Map<String, Double> calculatePositionTrends() {
        try {
            // Implémentation réelle à faire selon votre logique métier
            // return playerRepository.calculatePositionTrends();
            return Map.of(
                    "Attaquant", 15.0,
                    "Milieu", 8.0,
                    "Défenseur", 5.0
            ); // Placeholder temporaire
        } catch (Exception e) {
            logger.error("Erreur lors du calcul des tendances par position", e);
            return Map.of();
        }
    }

    private Double calculateGrowthPercentage(Long playerId) {
        try {
            // Implémentation réelle à faire selon votre logique métier
            // return playerRepository.calculateGrowthPercentage(playerId);
            return 8.5; // Placeholder temporaire
        } catch (Exception e) {
            logger.error("Erreur lors du calcul du pourcentage de croissance pour le joueur {}",
                    playerId, e);
            return 0.0;
        }
    }
}