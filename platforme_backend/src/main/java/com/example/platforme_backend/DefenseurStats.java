package com.example.platforme_backend;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "defenseurs")
public class DefenseurStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "joueur_id")
    private Long joueurId;

    @Column(name = "tacles_reussis")
    private Integer taclesReussis;

    @Column(name = "interceptions")
    private Integer interceptions;

    @Column(name = "duels_aeriens")
    private Integer duelsAeriens;

    @Column(name = "duels_gagnes")
    private Integer duelsGagnes;

    @Column(name = "clean_sheets")
    private Integer cleanSheets;

    @Column(name = "cartons_jaunes")
    private Integer cartonsJaunes;

    @Column(name = "cartons_rouge")
    private Integer cartonsRouge;

    @Column(name = "minutes_jouees")
    private Integer minutesJouees;

    @Column(name = "tackles")
    private Integer tackles;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getJoueurId() { return joueurId; }
    public void setJoueurId(Long joueurId) { this.joueurId = joueurId; }

    public Integer getTaclesReussis() { return taclesReussis; }
    public void setTaclesReussis(Integer taclesReussis) { this.taclesReussis = taclesReussis; }

    public Integer getInterceptions() { return interceptions; }
    public void setInterceptions(Integer interceptions) { this.interceptions = interceptions; }

    public Integer getDuelsAeriens() { return duelsAeriens; }
    public void setDuelsAeriens(Integer duelsAeriens) { this.duelsAeriens = duelsAeriens; }

    public Integer getDuelsGagnes() { return duelsGagnes; }
    public void setDuelsGagnes(Integer duelsGagnes) { this.duelsGagnes = duelsGagnes; }

    public Integer getCleanSheets() { return cleanSheets; }
    public void setCleanSheets(Integer cleanSheets) { this.cleanSheets = cleanSheets; }

    public Integer getCartonsJaunes() { return cartonsJaunes; }
    public void setCartonsJaunes(Integer cartonsJaunes) { this.cartonsJaunes = cartonsJaunes; }

    public Integer getCartonsRouge() { return cartonsRouge; }
    public void setCartonsRouge(Integer cartonsRouge) { this.cartonsRouge = cartonsRouge; }

    public Integer getMinutesJouees() { return minutesJouees; }
    public void setMinutesJouees(Integer minutesJouees) { this.minutesJouees = minutesJouees; }

    public Integer getTackles() { return tackles; }
    public void setTackles(Integer tackles) { this.tackles = tackles; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public DefenseurStats() {}
}
