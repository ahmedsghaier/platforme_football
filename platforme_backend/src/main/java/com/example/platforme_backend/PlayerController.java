package com.example.platforme_backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/players")
public class PlayerController {

    @Autowired
    private PlayerRepository playerRepository;

    @GetMapping("/top")
    public List<Player> getTopPlayers() {
        return playerRepository.findTop4ByOrderByMarketValueNumericDesc();
    }
    @Autowired
    private PlayerService playerService;

    @GetMapping("/{id}")
    public ResponseEntity<PlayerProfileDTO> getPlayerProfile(@PathVariable Long id) {
        try {
            PlayerProfileDTO profile = playerService.getPlayerProfile(id);
            return ResponseEntity.ok(profile);
        } catch (PlayerNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<PlayerProfileDTO>> getAllPlayers() {
        try {
            List<PlayerProfileDTO> players = playerService.getAllPlayers();
            return ResponseEntity.ok(players);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Recherche simple par nom, club ou position
     */
    @GetMapping("/search")
    public ResponseEntity<List<Player>> searchPlayers(@RequestParam String query) {
        try {
            List<Player> players = playerService.searchPlayers(query);
            return ResponseEntity.ok(players);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search/advanced")
    public ResponseEntity<List<PlayerProfileDTO>> advancedSearch(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String club,
            @RequestParam(required = false) String league,
            @RequestParam(required = false) Integer ageMin,
            @RequestParam(required = false) Integer ageMax,
            @RequestParam(required = false) Integer valueMin,
            @RequestParam(required = false) Integer valueMax,
            @RequestParam(required = false) String nationality) {
        try {
            PlayerSearchCriteria criteria = new PlayerSearchCriteria(
                    query, position, club, league, ageMin, ageMax,
                    valueMin, valueMax, nationality
            );
            List<PlayerProfileDTO> players = playerService.advancedSearch(criteria);
            return ResponseEntity.ok(players);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les joueurs par position
     */
    @GetMapping("/position/{position}")
    public ResponseEntity<List<PlayerProfileDTO>> getPlayersByPosition(@PathVariable String position) {
        try {
            List<PlayerProfileDTO> players = playerService.getPlayersByPosition(position);
            return ResponseEntity.ok(players);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les joueurs par club
     */
    @GetMapping("/club/{clubName}")
    public ResponseEntity<List<PlayerProfileDTO>> getPlayersByClub(@PathVariable String clubName) {
        try {
            List<PlayerProfileDTO> players = playerService.getPlayersByClub(clubName);
            return ResponseEntity.ok(players);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les joueurs par nationalité
     */
    @GetMapping("/nationality/{nationality}")
    public ResponseEntity<List<PlayerProfileDTO>> getPlayersByNationality(@PathVariable String nationality) {
        try {
            List<PlayerProfileDTO> players = playerService.getPlayersByNationality(nationality);
            return ResponseEntity.ok(players);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les joueurs dans une fourchette d'âge
     */
    @GetMapping("/age-range")
    public ResponseEntity<List<PlayerProfileDTO>> getPlayersByAgeRange(
            @RequestParam Integer minAge,
            @RequestParam Integer maxAge) {
        try {
            List<PlayerProfileDTO> players = playerService.getPlayersByAgeRange(minAge, maxAge);
            return ResponseEntity.ok(players);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les joueurs dans une fourchette de valeur marchande
     */
    @GetMapping("/value-range")
    public ResponseEntity<List<PlayerProfileDTO>> getPlayersByValueRange(
            @RequestParam Integer minValue,
            @RequestParam Integer maxValue) {
        try {
            List<PlayerProfileDTO> players = playerService.getPlayersByValueRange(minValue, maxValue);
            return ResponseEntity.ok(players);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère toutes les positions disponibles
     */
    @GetMapping("/filters/positions")
    public ResponseEntity<List<String>> getAvailablePositions() {
        try {
            List<String> positions = playerService.getAvailablePositions();
            return ResponseEntity.ok(positions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère tous les clubs disponibles
     */
    @GetMapping("/filters/clubs")
    public ResponseEntity<List<String>> getAvailableClubs() {
        try {
            List<String> clubs = playerService.getAvailableClubs();
            return ResponseEntity.ok(clubs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère toutes les nationalités disponibles
     */
    @GetMapping("/filters/nationalities")
    public ResponseEntity<List<String>> getAvailableNationalities() {
        try {
            List<String> nationalities = playerService.getAvailableNationalities();
            return ResponseEntity.ok(nationalities);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/filters/leagues")
    public ResponseEntity<List<String>> getLeagues() {
        List<String> leagues = playerService.getAvailableLeagues();
        if (leagues == null || leagues.isEmpty()) {
            return ResponseEntity.noContent().build(); // 204 No Content
        }
        return ResponseEntity.ok(leagues);
    }

    /**
     * Récupère les statistiques générales
     */
    @GetMapping("/stats/overview")
    public ResponseEntity<PlayerStatsOverview> getPlayersStatsOverview() {
        try {
            PlayerStatsOverview stats = playerService.getPlayersStatsOverview();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    /**
     * Crée une nouvelle comparaison
     */
    @PostMapping("/comparisons")
    public ResponseEntity<ComparisonResult> createComparison(@RequestBody ComparisonRequest request) {
        try {
            ComparisonResult result = playerService.createComparison(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Récupère une comparaison par son ID
     */
    @GetMapping("/comparisons/{id}")
    public ResponseEntity<ComparisonResult> getComparison(@PathVariable Long id) {
        try {
            ComparisonResult result = playerService.getComparison(id);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère toutes les comparaisons
     */
    @GetMapping("/comparisons")
    public ResponseEntity<List<ComparisonResult>> getAllComparisons() {
        try {
            List<ComparisonResult> comparisons = playerService.getAllComparisons();
            return ResponseEntity.ok(comparisons);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}