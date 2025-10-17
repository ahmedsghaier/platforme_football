package com.example.platforme_backend;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@Table(name = "comparisons")
public class Comparison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comparison_name", nullable = false, length = 255)
    private String comparisonName;

    @Column(name = "player_ids", length = 500)
    private String playerIds; // IDs séparés par des virgules (ex: "12,45,78")

    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = true; // Pour la suppression logique

    @Column(name = "shared_url", length = 500)
    private String sharedUrl; // URL de partage unique

    @Column(name = "user_id")
    private Long userId;

    @Version
    private Long version; // Pour la gestion de la concurrence optimiste

    public Comparison() {
        // Initialisation automatique des timestamps
        Timestamp now = new Timestamp(System.currentTimeMillis());
        this.createdAt = now;
        this.updatedAt = now;
        this.isActive = true;
    }

    public Comparison(String comparisonName, String description, Long userId) {
        this();
        this.comparisonName = comparisonName;
        this.description = description;
        this.userId = userId;
    }

    // Méthodes de cycle de vie JPA
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getComparisonName() {
        return comparisonName;
    }

    public void setComparisonName(String comparisonName) {
        this.comparisonName = comparisonName;
        // Mettre à jour le timestamp lors de la modification
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    public String getPlayerIds() {
        return playerIds;
    }

    public void setPlayerIds(String playerIds) {
        this.playerIds = playerIds;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    public String getSharedUrl() {
        return sharedUrl;
    }

    public void setSharedUrl(String sharedUrl) {
        this.sharedUrl = sharedUrl;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    // Méthodes transient pour la gestion des listes d'IDs
    @Transient
    public List<Long> getPlayerIdList() {
        if (playerIds == null || playerIds.isEmpty()) return List.of();
        return List.of(playerIds.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
    }

    @Transient
    public void setPlayerIdList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            this.playerIds = "";
        } else {
            this.playerIds = ids.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        }
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    // Méthodes utilitaires pour la gestion

    /**
     * Ajoute un joueur à la comparaison
     */
    @Transient
    public void addPlayer(Long playerId) {
        if (playerId == null) return;

        List<Long> currentIds = getPlayerIdList();
        if (!currentIds.contains(playerId)) {
            currentIds.add(playerId);
            setPlayerIdList(currentIds);
        }
    }

    /**
     * Supprime un joueur de la comparaison
     */
    @Transient
    public void removePlayer(Long playerId) {
        if (playerId == null) return;

        List<Long> currentIds = getPlayerIdList().stream()
                .filter(id -> !id.equals(playerId))
                .collect(Collectors.toList());
        setPlayerIdList(currentIds);
    }

    /**
     * Vérifie si un joueur est dans la comparaison
     */
    @Transient
    public boolean containsPlayer(Long playerId) {
        return playerId != null && getPlayerIdList().contains(playerId);
    }

    /**
     * Obtient le nombre de joueurs dans la comparaison
     */
    @Transient
    public int getPlayerCount() {
        return getPlayerIdList().size();
    }

    /**
     * Suppression logique de la comparaison
     */
    public void softDelete() {
        this.isActive = false;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Restauration de la comparaison
     */
    public void restore() {
        this.isActive = true;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Met à jour les informations de base
     */
    public void updateBasicInfo(String name, String description) {
        if (name != null && !name.trim().isEmpty()) {
            this.comparisonName = name.trim();
        }
        if (description != null) {
            this.description = description.trim();
        }
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Génère une URL de partage unique
     */
    public void generateSharedUrl() {
        this.sharedUrl = "comparison-" + this.id + "-" + System.currentTimeMillis();
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    // equals et hashCode basés sur l'ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comparison that = (Comparison) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // toString pour le debugging
    @Override
    public String toString() {
        return "Comparison{" +
                "id=" + id +
                ", comparisonName='" + comparisonName + '\'' +
                ", playerIds='" + playerIds + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", isActive=" + isActive +
                ", version=" + version +
                '}';
    }

    // Méthodes de validation

    /**
     * Valide que la comparaison a des données cohérentes
     */
    @Transient
    public boolean isValid() {
        return comparisonName != null && !comparisonName.trim().isEmpty()
                && getPlayerCount() >= 2
                && getPlayerCount() <= 5; // Limite raisonnable
    }

    /**
     * Vérifie si la comparaison peut être modifiée
     */
    @Transient
    public boolean canBeModified() {
        return isActive != null && isActive;
    }

    /**
     * Vérifie si la comparaison est récente (moins de 30 jours)
     */
    @Transient
    public boolean isRecent() {
        if (createdAt == null) return false;
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        return createdAt.getTime() > thirtyDaysAgo;
    }
}