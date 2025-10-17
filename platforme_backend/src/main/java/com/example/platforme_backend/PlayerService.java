package com.example.platforme_backend;

import jakarta.transaction.Transactional;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PlayerService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerService.class);


    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private AttaquantStatsRepository attaquantStatsRepository;

    @Autowired
    private MilieuStatsRepository milieuStatsRepository;

    @Autowired
    private DefenseurStatsRepository defenseurStatsRepository;

    @Autowired
    private GardienStatsRepository gardienStatsRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ComparisonRepository comparisonRepository;

    private static final Map<String, String> POSITION_MAPPING = new HashMap<>();

    static {
        // Gardiens
        POSITION_MAPPING.put("GK", "Gardien");
        POSITION_MAPPING.put("GARDIEN", "Gardien");

        // Défenseurs
        POSITION_MAPPING.put("DF", "Défenseur");
        POSITION_MAPPING.put("DEFENSEUR", "Défenseur");
        POSITION_MAPPING.put("DÉFENSEUR", "Défenseur");
        POSITION_MAPPING.put("CB", "Défenseur");
        POSITION_MAPPING.put("LB", "Défenseur");
        POSITION_MAPPING.put("RB", "Défenseur");
        POSITION_MAPPING.put("LWB", "Défenseur");
        POSITION_MAPPING.put("RWB", "Défenseur");

        // Milieux
        POSITION_MAPPING.put("MF", "Milieu");
        POSITION_MAPPING.put("MILIEU", "Milieu");
        POSITION_MAPPING.put("CM", "Milieu");
        POSITION_MAPPING.put("CDM", "Milieu");
        POSITION_MAPPING.put("CAM", "Milieu");
        POSITION_MAPPING.put("RM", "Milieu");
        POSITION_MAPPING.put("LM", "Milieu");

        // Attaquants
        POSITION_MAPPING.put("FW", "Attaquant");
        POSITION_MAPPING.put("ATTAQUANT", "Attaquant");
        POSITION_MAPPING.put("ST", "Attaquant");
        POSITION_MAPPING.put("CF", "Attaquant");
        POSITION_MAPPING.put("RW", "Attaquant");
        POSITION_MAPPING.put("LW", "Attaquant");
    }

    public String simplifyPosition(String position) {
        if (position == null || position.isEmpty()) return "N/A";

        // Prendre uniquement le premier poste en cas de positions multiples
        String firstPosition = position.split(",")[0].trim();

        // Normaliser : retirer les accents et mettre en majuscule
        String normalized = Normalizer.normalize(firstPosition, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toUpperCase();

        return POSITION_MAPPING.getOrDefault(normalized, "N/A");
    }

    // NOUVELLE FONCTION : Trouver un joueur par nom (insensible à la casse)
    public Optional<Player> findByNameIgnoreCase(String name) {
        try {
            if (name == null || name.trim().isEmpty()) {
                return Optional.empty();
            }

            String searchName = name.trim();
            logger.debug("Searching for player with name: '{}'", searchName);

            // D'abord essayer une correspondance exacte (insensible à la casse)
            Optional<Player> exactMatch = playerRepository.findByNameIgnoreCase(searchName);
            if (exactMatch.isPresent()) {
                logger.debug("Found exact match for name: '{}'", searchName);
                return exactMatch;
            }

            // Si pas de correspondance exacte, essayer de trouver par nom contenant
            List<Player> partialMatches = playerRepository.findByNameContainingIgnoreCase(searchName);
            if (!partialMatches.isEmpty()) {
                logger.debug("Found {} partial matches for name: '{}'", partialMatches.size(), searchName);
                // Retourner le premier résultat pour une correspondance partielle
                return Optional.of(partialMatches.get(0));
            }

            logger.debug("No player found for name: '{}'", searchName);
            return Optional.empty();

        } catch (Exception e) {
            logger.error("Error finding player by name '{}': {}", name, e.getMessage(), e);
            return Optional.empty();
        }
    }

    // NOUVELLE FONCTION : Rechercher des joueurs (pour le contrôleur)
    public List<Player> searchPlayers(String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                logger.debug("Empty search query, returning all players");
                return playerRepository.findAll().stream().limit(50).collect(Collectors.toList());
            }

            String searchTerm = query.toLowerCase().trim();
            logger.info("Searching players with query: '{}'", searchTerm);

            // Recherche par nom (exacte puis partielle)
            List<Player> nameMatches = new ArrayList<>();

            // Correspondance exacte par nom
            Optional<Player> exactMatch = playerRepository.findByNameIgnoreCase(searchTerm);
            if (exactMatch.isPresent()) {
                nameMatches.add(exactMatch.get());
            }

            // Correspondances partielles par nom
            List<Player> partialNameMatches = playerRepository.findByNameContainingIgnoreCase(searchTerm)
                    .stream()
                    .filter(player -> !nameMatches.contains(player)) // Éviter les doublons
                    .limit(20)
                    .collect(Collectors.toList());
            nameMatches.addAll(partialNameMatches);

            // Recherche par club
            List<Player> clubMatches = playerRepository.findByClubNameContainingIgnoreCase(searchTerm)
                    .stream()
                    .filter(player -> !nameMatches.contains(player)) // Éviter les doublons
                    .limit(10)
                    .collect(Collectors.toList());

            // Recherche par position
            List<Player> positionMatches = playerRepository.findAll()
                    .stream()
                    .filter(player -> !nameMatches.contains(player) && !clubMatches.contains(player))
                    .filter(player -> positionMatchesSearch(player.getPosition(), searchTerm))
                    .limit(10)
                    .collect(Collectors.toList());

            // Combiner tous les résultats
            List<Player> allResults = new ArrayList<>();
            allResults.addAll(nameMatches);
            allResults.addAll(clubMatches);
            allResults.addAll(positionMatches);

            logger.info("Found {} players for query '{}': {} name matches, {} club matches, {} position matches",
                    allResults.size(), searchTerm, nameMatches.size(), clubMatches.size(), positionMatches.size());

            return allResults.stream().limit(50).collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error searching players with query '{}': {}", query, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Récupère le profil complet d'un joueur
     */
    public PlayerProfileDTO getPlayerProfile(Long playerId) {
        try {
            Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new PlayerNotFoundException("Player not found with id: " + playerId));

            PlayerProfileDTO profile = new PlayerProfileDTO();
            profile.setId(player.getId());
            profile.setName(player.getName());
            profile.setAge(player.getAge());
            profile.setPosition(simplifyPosition(player.getPosition()));
            profile.setNationality(player.getNationality());
            profile.setImage(player.getImage());
            profile.setMarketValue(player.getMarketValue());
            profile.setMarketValueNumeric(player.getMarketValueNumeric());

            // Get club information
            if (player.getClub() != null) {
                if (player.getClub().getId() != null) {
                    Club club = clubRepository.findById(player.getClub().getId()).orElse(null);
                    if (club != null) {
                        profile.setClubName(club.getName());
                    }
                } else {
                    // Si le club est déjà chargé
                    profile.setClubName(player.getClub().getName());
                }
            }

            // Calculate AI confidence based on data completeness and performance
            profile.setConfidence(calculateConfidence(player));

            // Get position-specific stats
            Map<String, Object> stats = getPlayerStats(player.getId(), player.getPosition());
            profile.setStats(stats);

            // Generate value history
            profile.setValueHistory(generateValueHistory(player.getMarketValueNumeric()));

            return profile;
        } catch (Exception e) {
            logger.error("Error getting player profile for playerId {}: {}", playerId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Récupère tous les joueurs avec informations complètes
     */
    public List<PlayerProfileDTO> getAllPlayers() {
        try {
            return playerRepository.findAll().stream()
                    .map(this::convertToBasicDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting all players: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Récupère les top joueurs par valeur marchande
     */
    public List<PlayerProfileDTO> getTopPlayers() {
        try {
            List<Player> topPlayers = playerRepository.findTop4ByOrderByMarketValueNumericDesc();
            return topPlayers.stream()
                    .map(this::convertToBasicDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting top players: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Recherche simple par texte avec amélioration - CORRIGÉE
     */
    public List<PlayerProfileDTO> searchPlayersDTO(String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return getAllPlayers();
            }

            List<Player> players = searchPlayers(query);
            return players.stream()
                    .map(this::convertToBasicDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error in searchPlayersDTO with query '{}': {}", query, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Recherche avancée avec critères multiples améliorée
     */
    public List<PlayerProfileDTO> advancedSearch(PlayerSearchCriteria criteria) {
        try {
            List<Player> allPlayers = playerRepository.findAll();

            return allPlayers.stream()
                    .filter(player -> matchesCriteria(player, criteria))
                    .map(this::convertToBasicDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error in advanced search: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Récupère les joueurs par position (avec position simplifiée)
     */
    public List<PlayerProfileDTO> getPlayersByPosition(String position) {
        try {
            List<Player> allPlayers = playerRepository.findAll();

            List<Player> filteredPlayers = allPlayers.stream()
                    .filter(player -> {
                        String playerSimplifiedPosition = simplifyPosition(player.getPosition());
                        return playerSimplifiedPosition != null && playerSimplifiedPosition.equalsIgnoreCase(position);
                    })
                    .collect(Collectors.toList());

            return filteredPlayers.stream()
                    .map(this::convertToBasicDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting players by position '{}': {}", position, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Récupère les joueurs par club
     */
    public List<PlayerProfileDTO> getPlayersByClub(String clubName) {
        try {
            List<Player> players = playerRepository.findByClubNameContainingIgnoreCase(clubName);
            return players.stream()
                    .map(this::convertToBasicDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting players by club '{}': {}", clubName, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Récupère les joueurs par nationalité
     */
    public List<PlayerProfileDTO> getPlayersByNationality(String nationality) {
        try {
            List<Player> players = playerRepository.findByNationalityContainingIgnoreCase(nationality);
            return players.stream()
                    .map(this::convertToBasicDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting players by nationality '{}': {}", nationality, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Récupère les joueurs dans une fourchette d'âge
     */
    public List<PlayerProfileDTO> getPlayersByAgeRange(Integer minAge, Integer maxAge) {
        try {
            List<Player> players = playerRepository.findByAgeBetween(minAge, maxAge);
            return players.stream()
                    .map(this::convertToBasicDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting players by age range {}-{}: {}", minAge, maxAge, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Récupère les joueurs dans une fourchette de valeur
     */
    public List<PlayerProfileDTO> getPlayersByValueRange(Integer minValue, Integer maxValue) {
        try {
            List<Player> players = playerRepository.findByMarketValueNumericBetween(minValue, maxValue);
            return players.stream()
                    .map(this::convertToBasicDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting players by value range {}-{}: {}", minValue, maxValue, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Récupère toutes les positions disponibles (simplifiées)
     */
    public List<String> getAvailablePositions() {
        try {
            List<Player> allPlayers = playerRepository.findAll();
            return allPlayers.stream()
                    .map(Player::getPosition)
                    .filter(Objects::nonNull)
                    .flatMap(pos -> Arrays.stream(pos.split(",")).map(String::trim))
                    .map(this::simplifyPosition)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting available positions: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Récupère tous les clubs disponibles
     */
    public List<String> getAvailableClubs() {
        try {
            return playerRepository.findAll().stream()
                    .map(player -> player.getClub() != null ? player.getClub().getName() : null)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting available clubs: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Récupère tous les championnats disponibles
     */
    public List<String> getAvailableLeagues() {
        try {
            return playerRepository.findAll().stream()
                    .map(player -> {
                        if (player.getClub() != null && player.getClub().getLeague() != null) {
                            return player.getClub().getLeague().trim();
                        }
                        return null;
                    })
                    .filter(league -> league != null && !league.isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting available leagues: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Récupère toutes les nationalités disponibles
     */
    public List<String> getAvailableNationalities() {
        try {
            return playerRepository.findDistinctNationalities()
                    .stream()
                    .filter(Objects::nonNull)
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting available nationalities: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Vérifie si un joueur correspond à la recherche textuelle
     */
    private boolean matchesTextualSearch(Player player, String searchTerm) {
        return (player.getName() != null && player.getName().toLowerCase().contains(searchTerm)) ||
                (player.getClub() != null && player.getClub().getName() != null &&
                        player.getClub().getName().toLowerCase().contains(searchTerm)) ||
                (player.getPosition() != null && positionMatchesSearch(player.getPosition(), searchTerm)) ||
                (player.getNationality() != null && player.getNationality().toLowerCase().contains(searchTerm)) ||
                (player.getClub() != null && player.getClub().getLeague() != null &&
                        player.getClub().getLeague().toLowerCase().contains(searchTerm));
    }

    /**
     * Vérifie si la position correspond à la recherche
     */
    private boolean positionMatchesSearch(String position, String searchTerm) {
        if (position == null) return false;

        String simplifiedPosition = simplifyPosition(position);
        return position.toLowerCase().contains(searchTerm) ||
                (simplifiedPosition != null && simplifiedPosition.toLowerCase().contains(searchTerm));
    }

    /**
     * Récupère les statistiques générales
     */
    public PlayerStatsOverview getPlayersStatsOverview() {
        try {
            List<Player> allPlayers = playerRepository.findAll();

            if (allPlayers.isEmpty()) {
                return new PlayerStatsOverview();
            }

            // Calculs des statistiques avec positions simplifiées
            long totalPlayers = allPlayers.size();

            Map<String, Long> playersByPosition = allPlayers.stream()
                    .filter(p -> p.getPosition() != null)
                    .map(p -> simplifyPosition(p.getPosition()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(pos -> pos, Collectors.counting()));

            Map<String, Long> playersByClub = allPlayers.stream()
                    .filter(p -> p.getClub() != null && p.getClub().getName() != null)
                    .collect(Collectors.groupingBy(p -> p.getClub().getName(), Collectors.counting()));

            Map<String, Long> playersByNationality = allPlayers.stream()
                    .filter(p -> p.getNationality() != null)
                    .collect(Collectors.groupingBy(Player::getNationality, Collectors.counting()));

            // Calculs statistiques numériques
            OptionalDouble avgAge = allPlayers.stream()
                    .filter(p -> p.getAge() != null)
                    .mapToInt(Player::getAge)
                    .average();

            OptionalDouble avgValue = allPlayers.stream()
                    .filter(p -> p.getMarketValueNumeric() != null)
                    .mapToInt(Player::getMarketValueNumeric)
                    .average();

            OptionalInt minAge = allPlayers.stream()
                    .filter(p -> p.getAge() != null)
                    .mapToInt(Player::getAge)
                    .min();

            OptionalInt maxAge = allPlayers.stream()
                    .filter(p -> p.getAge() != null)
                    .mapToInt(Player::getAge)
                    .max();

            OptionalInt minValue = allPlayers.stream()
                    .filter(p -> p.getMarketValueNumeric() != null)
                    .mapToInt(Player::getMarketValueNumeric)
                    .min();

            OptionalInt maxValue = allPlayers.stream()
                    .filter(p -> p.getMarketValueNumeric() != null)
                    .mapToInt(Player::getMarketValueNumeric)
                    .max();

            // Joueurs remarquables
            String mostValuablePlayer = allPlayers.stream()
                    .filter(p -> p.getMarketValueNumeric() != null)
                    .max(Comparator.comparing(Player::getMarketValueNumeric))
                    .map(Player::getName)
                    .orElse("N/A");

            String youngestPlayer = allPlayers.stream()
                    .filter(p -> p.getAge() != null)
                    .min(Comparator.comparing(Player::getAge))
                    .map(Player::getName)
                    .orElse("N/A");

            String oldestPlayer = allPlayers.stream()
                    .filter(p -> p.getAge() != null)
                    .max(Comparator.comparing(Player::getAge))
                    .map(Player::getName)
                    .orElse("N/A");

            return new PlayerStatsOverview(
                    totalPlayers,
                    playersByPosition,
                    playersByClub,
                    playersByNationality,
                    avgAge.orElse(0.0),
                    avgValue.orElse(0.0),
                    minAge.orElse(0),
                    maxAge.orElse(0),
                    maxValue.orElse(0),
                    minValue.orElse(0),
                    mostValuablePlayer,
                    youngestPlayer,
                    oldestPlayer
            );
        } catch (Exception e) {
            logger.error("Error getting players stats overview: {}", e.getMessage(), e);
            return new PlayerStatsOverview();
        }
    }

    /**
     * Obtient les statistiques spécifiques par position avec les vraies données de la base
     */
    private Map<String, Object> getPlayerStats(Long playerId, String position) {
        Map<String, Object> stats = new HashMap<>();

        try {
            if (position == null || position.isBlank()) {
                stats.put("matchsPlayed", 25);
                stats.put("minutesPlayed", 2250);
                return stats;
            }

            // Simplifier la position pour la comparaison
            String simplifiedPosition = simplifyPosition(position);

            switch (simplifiedPosition) {
                case "Attaquant":
                    AttaquantStats attaquantStats = attaquantStatsRepository.findByJoueurId(playerId).orElse(null);
                    if (attaquantStats != null) {
                        stats.put("goals", getSafeInt(attaquantStats.getButsMarques()));
                        stats.put("assists", getSafeInt(attaquantStats.getPassesDecisives()));
                        stats.put("shots", getSafeInt(attaquantStats.getShotsTotal()));
                        stats.put("conversionRate", attaquantStats.getTauxConversion() != null ?
                                String.format("%.1f%%", attaquantStats.getTauxConversion() * 100) : "0%");
                        stats.put("minutesPlayed", getSafeInt(attaquantStats.getMinutesJouees()));
                        stats.put("keyPasses", calculateKeyPasses(attaquantStats));
                    } else {
                        // Fallback
                        stats.put("goals", generateRandomStat(0, 25));
                        stats.put("assists", generateRandomStat(0, 15));
                        stats.put("shots", generateRandomStat(20, 60));
                        stats.put("minutesPlayed", generateRandomStat(1500, 3000));
                    }
                    break;

                case "Milieu":
                    MilieuStats milieuStats = milieuStatsRepository.findByJoueurId(playerId).orElse(null);
                    if (milieuStats != null) {
                        stats.put("goals", getSafeInt(milieuStats.getButsMarques()));
                        stats.put("assists", getSafeInt(milieuStats.getPassesDecisives()));
                        stats.put("passesReussies", getSafeInt(milieuStats.getPassesReussies()));
                        stats.put("recuperations", getSafeInt(milieuStats.getRecuperations()));
                        stats.put("distanceParcourue", milieuStats.getDistanceParcourue() != null ? milieuStats.getDistanceParcourue() : 0.0);
                        stats.put("keyPasses", milieuStats.getPassesCle() != null ? milieuStats.getPassesCle().intValue() : 0);
                        stats.put("minutesPlayed", getSafeInt(milieuStats.getMinutesJouees()));
                    } else {
                        // Fallback
                        stats.put("passes", generateRandomStat(800, 2000));
                        stats.put("passAccuracy", Math.round((75 + Math.random() * 20) * 10) / 10.0);
                        stats.put("keyPasses", generateRandomStat(10, 50));
                        stats.put("minutesPlayed", generateRandomStat(1500, 3000));
                    }
                    break;

                case "Défenseur":
                    DefenseurStats defenseurStats = defenseurStatsRepository.findByJoueurId(playerId).orElse(null);
                    if (defenseurStats != null) {
                        stats.put("tackles", getSafeInt(defenseurStats.getTackles()));
                        stats.put("taclesReussis", getSafeInt(defenseurStats.getTaclesReussis()));
                        stats.put("interceptions", getSafeInt(defenseurStats.getInterceptions()));
                        stats.put("duelsAeriens", getSafeInt(defenseurStats.getDuelsAeriens()));
                        stats.put("duelsGagnes", getSafeInt(defenseurStats.getDuelsGagnes()));
                        stats.put("cleanSheets", getSafeInt(defenseurStats.getCleanSheets()));
                        stats.put("cartonsJaunes", getSafeInt(defenseurStats.getCartonsJaunes()));
                        stats.put("cartonsRouge", getSafeInt(defenseurStats.getCartonsRouge()));
                        stats.put("minutesPlayed", getSafeInt(defenseurStats.getMinutesJouees()));

                        // Taux de réussite des tacles
                        if (defenseurStats.getTackles() != null && defenseurStats.getTaclesReussis() != null && defenseurStats.getTackles() > 0) {
                            double successRate = (double) defenseurStats.getTaclesReussis() / defenseurStats.getTackles() * 100;
                            stats.put("tackleSuccessRate", String.format("%.1f%%", successRate));
                        } else {
                            stats.put("tackleSuccessRate", "0%");
                        }

                        // Taux de réussite des duels aériens
                        if (defenseurStats.getDuelsAeriens() != null && defenseurStats.getDuelsGagnes() != null && defenseurStats.getDuelsAeriens() > 0) {
                            double aerialRate = (double) defenseurStats.getDuelsGagnes() / defenseurStats.getDuelsAeriens() * 100;
                            stats.put("aerialSuccessRate", String.format("%.1f%%", aerialRate));
                        } else {
                            stats.put("aerialSuccessRate", "0%");
                        }
                    } else {
                        // Fallback
                        stats.put("tackles", generateRandomStat(30, 80));
                        stats.put("interceptions", generateRandomStat(20, 60));
                        stats.put("cleanSheets", generateRandomStat(5, 20));
                        stats.put("minutesPlayed", generateRandomStat(1500, 3000));
                    }
                    break;

                case "Gardien":
                    GardienStats gardienStats = gardienStatsRepository.findByJoueurId(playerId).orElse(null);
                    if (gardienStats != null) {
                        stats.put("saves", getSafeInt(gardienStats.getSaves()));
                        stats.put("cleanSheets", getSafeInt(gardienStats.getCleanSheets()));
                        stats.put("savePercentage", gardienStats.getPourcentageArrets() != null ?
                                String.format("%.1f%%", gardienStats.getPourcentageArrets()) : "0%");
                        stats.put("penaltiesSaved", getSafeInt(gardienStats.getPenaltiesArretes()));
                        stats.put("matchsPlayed", getSafeInt(gardienStats.getMatchsJoues()));
                    } else {
                        // Fallback
                        stats.put("saves", generateRandomStat(50, 150));
                        stats.put("cleanSheets", generateRandomStat(5, 20));
                        stats.put("savePercentage", Math.round((70 + Math.random() * 25) * 10) / 10.0 + "%");
                        stats.put("matchsPlayed", generateRandomStat(15, 40));
                    }
                    break;

                default:
                    stats.put("matchsPlayed", 25);
                    stats.put("minutesPlayed", 2250);
                    break;
            }

        } catch (Exception e) {
            logger.error("Error getting player stats for playerId {}: {}", playerId, e.getMessage(), e);
            stats.put("matchsPlayed", 25);
            stats.put("minutesPlayed", 2250);
        }

        return stats;
    }

    private int getSafeInt(Integer value) {
        return value != null ? value : 0;
    }

    /**
     * Vérifie si un joueur correspond aux critères de recherche améliorée
     */
    private boolean matchesCriteria(Player player, PlayerSearchCriteria criteria) {
        try {
            // Recherche textuelle
            if (criteria.hasTextualSearch()) {
                if (!matchesTextualSearch(player, criteria.getQuery().toLowerCase())) {
                    return false;
                }
            }

            // Filtre par position (avec simplification)
            if (criteria.hasPositionFilter()) {
                String playerSimplifiedPosition = simplifyPosition(player.getPosition());
                if (playerSimplifiedPosition == null || !playerSimplifiedPosition.equalsIgnoreCase(criteria.getPosition())) {
                    return false;
                }
            }

            // Filtre par club
            if (criteria.hasClubFilter()) {
                if (player.getClub() == null || player.getClub().getName() == null ||
                        !player.getClub().getName().toLowerCase().contains(criteria.getClub().toLowerCase())) {
                    return false;
                }
            }

            // Filtre par championnat
            if (criteria.hasLeagueFilter()) {
                if (player.getClub() == null || player.getClub().getLeague() == null ||
                        !player.getClub().getLeague().toLowerCase().contains(criteria.getLeague().toLowerCase())) {
                    return false;
                }
            }

            // Filtre par âge
            if (criteria.hasAgeFilter()) {
                if (player.getAge() == null) {
                    return false;
                }
                if (criteria.getAgeMin() != null && player.getAge() < criteria.getAgeMin()) {
                    return false;
                }
                if (criteria.getAgeMax() != null && player.getAge() > criteria.getAgeMax()) {
                    return false;
                }
            }

            // Filtre par valeur marchande
            if (criteria.hasValueFilter()) {
                if (player.getMarketValueNumeric() == null) {
                    return false;
                }
                if (criteria.getValueMin() != null && player.getMarketValueNumeric() < criteria.getValueMin()) {
                    return false;
                }
                if (criteria.getValueMax() != null && player.getMarketValueNumeric() > criteria.getValueMax()) {
                    return false;
                }
            }

            // Filtre par nationalité
            if (criteria.hasNationalityFilter()) {
                if (player.getNationality() == null ||
                        !player.getNationality().toLowerCase().contains(criteria.getNationality().toLowerCase())) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            logger.error("Error matching criteria for player {}: {}", player.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Convertit un Player en PlayerProfileDTO (version basique pour les listes)
     */
    private PlayerProfileDTO convertToBasicDTO(Player player) {
        try {
            PlayerProfileDTO dto = new PlayerProfileDTO();
            dto.setId(player.getId());
            dto.setName(player.getName());
            dto.setPosition(simplifyPosition(player.getPosition())); // Position simplifiée
            dto.setMarketValue(player.getMarketValue());
            dto.setMarketValueNumeric(player.getMarketValueNumeric());
            dto.setImage(player.getImage());
            dto.setAge(player.getAge());
            dto.setNationality(player.getNationality());

            // Get club information avec sécurité
            if (player.getClub() != null) {
                if (player.getClub().getId() != null) {
                    Club club = clubRepository.findById(player.getClub().getId()).orElse(null);
                    if (club != null) {
                        dto.setClubName(club.getName());
                    }
                } else {
                    // Si le club est déjà chargé
                    dto.setClubName(player.getClub().getName());
                }
            }

            return dto;
        } catch (Exception e) {
            logger.error("Error converting player {} to DTO: {}", player.getId(), e.getMessage(), e);
            // Retourner un DTO minimal en cas d'erreur
            PlayerProfileDTO dto = new PlayerProfileDTO();
            dto.setId(player.getId());
            dto.setName(player.getName() != null ? player.getName() : "Unknown");
            return dto;
        }
    }

    /**
     * Calcule un pourcentage de confiance pour le joueur
     */
    private String calculateConfidence(Player player) {
        try {
            int confidence = 85; // Base confidence

            // Add confidence based on data completeness
            if (player.getMarketValueNumeric() != null && player.getMarketValueNumeric() > 50000000) {
                confidence += 10;
            }
            if (player.getImage() != null && !player.getImage().isEmpty()) {
                confidence += 5;
            }
            if (player.getNationality() != null && !player.getNationality().isEmpty()) {
                confidence += 3;
            }

            // Additional logic based on age
            if (player.getAge() != null) {
                if (player.getAge() <= 23) {
                    confidence += 5;
                } else if (player.getAge() <= 27) {
                    confidence += 3;
                } else if (player.getAge() > 32) {
                    confidence -= 2;
                }
            }

            // Club information completeness
            if (player.getClub() != null && player.getClub().getLeague() != null) {
                confidence += 2;
            }

            return Math.min(confidence, 98) + "%";
        } catch (Exception e) {
            logger.error("Error calculating confidence for player {}: {}", player.getId(), e.getMessage(), e);
            return "85%";
        }
    }

    /**
     * Calcule les passes clés pour un attaquant
     */
    private Integer calculateKeyPasses(AttaquantStats stats) {
        try {
            int baseKeyPasses = stats.getPassesDecisives() != null ? stats.getPassesDecisives() : 0;
            return (int) (baseKeyPasses * 2.5 + Math.random() * 20);
        } catch (Exception e) {
            logger.error("Error calculating key passes: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Génère un historique de valeur marchande
     */
    private List<ValueHistoryDTO> generateValueHistory(Integer currentValue) {
        try {
            List<ValueHistoryDTO> history = new ArrayList<>();
            String[] months = {"Jan", "Fév", "Mar", "Avr", "Mai", "Juin"};

            long baseValue = currentValue != null ? currentValue : 50000000L;

            for (int i = 0; i < months.length; i++) {
                long value = (long) (baseValue * (0.85 + i * 0.03 + Math.random() * 0.1));
                history.add(new ValueHistoryDTO(months[i], value / 1000000)); // Convert to millions
            }

            return history;
        } catch (Exception e) {
            logger.error("Error generating value history: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Génère une statistique aléatoire dans un intervalle
     */
    private int generateRandomStat(int min, int max) {
        return min + (int) (Math.random() * (max - min + 1));
    }

    /**
     * Crée une nouvelle comparaison
     */
    public ComparisonResult createComparison(ComparisonRequest request) {
        try {
            Comparison comparison = new Comparison();
            comparison.setComparisonName(request.getName());
            comparison.setDescription(request.getDescription());
            comparison.setPlayerIdList(request.getPlayerIds()); // Convertit en string automatiquement

            comparison = comparisonRepository.save(comparison);

            List<PlayerProfileDTO> players = request.getPlayerIds().stream()
                    .map(this::getPlayerProfile)
                    .collect(Collectors.toList());

            ComparisonResult result = new ComparisonResult(
                    comparison.getId(),
                    comparison.getComparisonName(),
                    request.getPlayerIds(),
                    comparison.getCreatedAt()
            );
            result.setPlayers(players);
            return result;
        } catch (Exception e) {
            logger.error("Error creating comparison: {}", e.getMessage(), e);
            throw new RuntimeException("Error creating comparison: " + e.getMessage());
        }
    }

    /**
     * Récupère une comparaison par ID
     */
    public ComparisonResult getComparison(Long id) {
        try {
            Comparison comparison = comparisonRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Comparison not found with id: " + id));

            List<Long> playerIds = comparison.getPlayerIdList();
            List<PlayerProfileDTO> players = playerIds.stream()
                    .map(this::getPlayerProfile)
                    .collect(Collectors.toList());

            ComparisonResult result = new ComparisonResult(
                    comparison.getId(),
                    comparison.getComparisonName(),
                    playerIds,
                    comparison.getCreatedAt()
            );
            result.setPlayers(players);
            return result;
        } catch (Exception e) {
            logger.error("Error getting comparison {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error getting comparison: " + e.getMessage());
        }
    }

    /**
     * Récupère toutes les comparaisons
     */
    public List<ComparisonResult> getAllComparisons() {
        try {
            return comparisonRepository.findAll().stream().map(comparison -> {
                try {
                    List<Long> playerIds = comparison.getPlayerIdList();
                    List<PlayerProfileDTO> players = playerIds.stream()
                            .map(this::getPlayerProfile)
                            .collect(Collectors.toList());

                    ComparisonResult result = new ComparisonResult(
                            comparison.getId(),
                            comparison.getComparisonName(),
                            playerIds,
                            comparison.getCreatedAt()
                    );
                    result.setPlayers(players);
                    return result;
                } catch (Exception e) {
                    logger.error("Error processing comparison {}: {}", comparison.getId(), e.getMessage(), e);
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting all comparisons: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // NOUVELLE FONCTION : Vérifie si un joueur existe par ID
    public boolean playerExists(Long playerId) {
        try {
            return playerId != null && playerRepository.existsById(playerId);
        } catch (Exception e) {
            logger.error("Error checking if player exists with id {}: {}", playerId, e.getMessage(), e);
            return false;
        }
    }

    // NOUVELLE FONCTION : Récupère un joueur par ID (version simple)
    public Optional<Player> findById(Long playerId) {
        try {
            if (playerId == null) {
                return Optional.empty();
            }
            return playerRepository.findById(playerId);
        } catch (Exception e) {
            logger.error("Error finding player by id {}: {}", playerId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    // NOUVELLE FONCTION : Compte le nombre total de joueurs
    public long countAllPlayers() {
        try {
            return playerRepository.count();
        } catch (Exception e) {
            logger.error("Error counting all players: {}", e.getMessage(), e);
            return 0L;
        }
    }

    // NOUVELLE FONCTION : Récupère les joueurs paginés
    public List<PlayerProfileDTO> getPlayersPaginated(int page, int size) {
        try {
            // Note: Vous devez implémenter la pagination dans le repository
            // Pour l'instant, on utilise une approche simple
            List<Player> allPlayers = playerRepository.findAll();
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, allPlayers.size());

            if (startIndex >= allPlayers.size()) {
                return new ArrayList<>();
            }

            return allPlayers.subList(startIndex, endIndex)
                    .stream()
                    .map(this::convertToBasicDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting players paginated (page: {}, size: {}): {}", page, size, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // NOUVELLE FONCTION : Récupère les statistiques d'un joueur par ID
    public Map<String, Object> getPlayerStatsById(Long playerId) {
        try {
            Optional<Player> playerOpt = playerRepository.findById(playerId);
            if (playerOpt.isPresent()) {
                Player player = playerOpt.get();
                return getPlayerStats(playerId, player.getPosition());
            }
            return new HashMap<>();
        } catch (Exception e) {
            logger.error("Error getting player stats by id {}: {}", playerId, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    // NOUVELLE FONCTION : Supprime un joueur (si nécessaire)
    public boolean deletePlayer(Long playerId) {
        try {
            if (playerId == null || !playerRepository.existsById(playerId)) {
                return false;
            }
            playerRepository.deleteById(playerId);
            logger.info("Successfully deleted player with id: {}", playerId);
            return true;
        } catch (Exception e) {
            logger.error("Error deleting player with id {}: {}", playerId, e.getMessage(), e);
            return false;
        }
    }

    // NOUVELLE FONCTION : Met à jour les informations d'un joueur
    public PlayerProfileDTO updatePlayer(Long playerId, Player updatedPlayer) {
        try {
            Optional<Player> existingPlayerOpt = playerRepository.findById(playerId);
            if (existingPlayerOpt.isEmpty()) {
                throw new PlayerNotFoundException("Player not found with id: " + playerId);
            }

            Player existingPlayer = existingPlayerOpt.get();

            // Mettre à jour les champs non null
            if (updatedPlayer.getName() != null) {
                existingPlayer.setName(updatedPlayer.getName());
            }
            if (updatedPlayer.getAge() != null) {
                existingPlayer.setAge(updatedPlayer.getAge());
            }
            if (updatedPlayer.getPosition() != null) {
                existingPlayer.setPosition(updatedPlayer.getPosition());
            }
            if (updatedPlayer.getNationality() != null) {
                existingPlayer.setNationality(updatedPlayer.getNationality());
            }
            if (updatedPlayer.getMarketValue() != null) {
                existingPlayer.setMarketValue(updatedPlayer.getMarketValue());
            }
            if (updatedPlayer.getMarketValueNumeric() != null) {
                existingPlayer.setMarketValueNumeric(updatedPlayer.getMarketValueNumeric());
            }
            if (updatedPlayer.getImage() != null) {
                existingPlayer.setImage(updatedPlayer.getImage());
            }

            Player savedPlayer = playerRepository.save(existingPlayer);
            logger.info("Successfully updated player with id: {}", playerId);

            return convertToBasicDTO(savedPlayer);
        } catch (Exception e) {
            logger.error("Error updating player with id {}: {}", playerId, e.getMessage(), e);
            throw new RuntimeException("Error updating player: " + e.getMessage());
        }
    }

    // NOUVELLE FONCTION : Recherche rapide (limitée à un petit nombre de résultats)
    public List<PlayerProfileDTO> quickSearch(String query, int limit) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return getTopPlayers();
            }

            List<Player> players = searchPlayers(query);
            return players.stream()
                    .limit(Math.max(1, limit))
                    .map(this::convertToBasicDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error in quick search with query '{}': {}", query, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // NOUVELLE FONCTION : Récupère les joueurs similaires (par position et valeur)
    public List<PlayerProfileDTO> getSimilarPlayers(Long playerId, int limit) {
        try {
            Optional<Player> playerOpt = playerRepository.findById(playerId);
            if (playerOpt.isEmpty()) {
                return new ArrayList<>();
            }

            Player targetPlayer = playerOpt.get();
            String position = simplifyPosition(targetPlayer.getPosition());
            Integer marketValue = targetPlayer.getMarketValueNumeric();

            List<Player> allPlayers = playerRepository.findAll();
            return allPlayers.stream()
                    .filter(p -> !p.getId().equals(playerId)) // Exclure le joueur lui-même
                    .filter(p -> {
                        String playerPosition = simplifyPosition(p.getPosition());
                        return position != null && position.equals(playerPosition);
                    })
                    .filter(p -> {
                        if (marketValue == null || p.getMarketValueNumeric() == null) {
                            return true;
                        }
                        // Joueurs avec une valeur similaire (+/- 50%)
                        double minValue = marketValue * 0.5;
                        double maxValue = marketValue * 1.5;
                        return p.getMarketValueNumeric() >= minValue && p.getMarketValueNumeric() <= maxValue;
                    })
                    .sorted((p1, p2) -> {
                        // Trier par proximité de valeur marchande
                        if (marketValue == null) return 0;
                        Integer v1 = p1.getMarketValueNumeric();
                        Integer v2 = p2.getMarketValueNumeric();
                        if (v1 == null && v2 == null) return 0;
                        if (v1 == null) return 1;
                        if (v2 == null) return -1;
                        return Integer.compare(
                                Math.abs(v1 - marketValue),
                                Math.abs(v2 - marketValue)
                        );
                    })
                    .limit(Math.max(1, limit))
                    .map(this::convertToBasicDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting similar players for id {}: {}", playerId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}