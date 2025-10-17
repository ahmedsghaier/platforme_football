package com.example.platforme_backend;

import jakarta.persistence.*;

@Entity
@Table(name = "attaquants")
public class AttaquantStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "joueur_id")
    private Long joueurId;

    @Column(name = "buts_marques")
    private Integer butsMarques;

    @Column(name = "taux_conversion")
    private Double tauxConversion;

    @Column(name = "passes_decisives")
    private Integer passesDecisives;

    @Column(name = "minutes_jouees")
    private Integer minutesJouees;

    @Column(name = "shots_total")
    private Integer shotsTotal;

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJoueurId() {
        return joueurId;
    }

    public void setJoueurId(Long joueurId) {
        this.joueurId = joueurId;
    }

    public Integer getButsMarques() {
        return butsMarques;
    }

    public void setButsMarques(Integer butsMarques) {
        this.butsMarques = butsMarques;
    }

    public Double getTauxConversion() {
        return tauxConversion;
    }

    public void setTauxConversion(Double tauxConversion) {
        this.tauxConversion = tauxConversion;
    }

    public Integer getPassesDecisives() {
        return passesDecisives;
    }

    public void setPassesDecisives(Integer passesDecisives) {
        this.passesDecisives = passesDecisives;
    }

    public Integer getMinutesJouees() {
        return minutesJouees;
    }

    public void setMinutesJouees(Integer minutesJouees) {
        this.minutesJouees = minutesJouees;
    }

    public Integer getShotsTotal() {
        return shotsTotal;
    }

    public void setShotsTotal(Integer shotsTotal) {
        this.shotsTotal = shotsTotal;
    }
}

