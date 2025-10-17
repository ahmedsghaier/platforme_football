package com.example.platforme_backend;

import jakarta.persistence.*;

@Entity
@Table(name = "milieux")
public class MilieuStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "joueur_id")
    private Long joueurId;

    @Column(name = "passes_reussies")
    private Integer passesReussies;

    @Column(name = "recuperations")
    private Integer recuperations;

    @Column(name = "distance_parcourue")
    private Double distanceParcourue;

    @Column(name = "passes_cle")
    private Double passesCle;

    @Column(name = "passes_decisives")
    private Integer passesDecisives;

    @Column(name = "buts_marques")
    private Integer butsMarques;

    @Column(name = "minutes_jouees")
    private Integer minutesJouees;

    // Getters and Setters

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

    public Integer getPassesReussies() {
        return passesReussies;
    }

    public void setPassesReussies(Integer passesReussies) {
        this.passesReussies = passesReussies;
    }

    public Integer getRecuperations() {
        return recuperations;
    }

    public void setRecuperations(Integer recuperations) {
        this.recuperations = recuperations;
    }

    public Double getDistanceParcourue() {
        return distanceParcourue;
    }

    public void setDistanceParcourue(Double distanceParcourue) {
        this.distanceParcourue = distanceParcourue;
    }

    public Double getPassesCle() {
        return passesCle;
    }

    public void setPassesCle(Double passesCle) {
        this.passesCle = passesCle;
    }

    public Integer getPassesDecisives() {
        return passesDecisives;
    }

    public void setPassesDecisives(Integer passesDecisives) {
        this.passesDecisives = passesDecisives;
    }

    public Integer getButsMarques() {
        return butsMarques;
    }

    public void setButsMarques(Integer butsMarques) {
        this.butsMarques = butsMarques;
    }

    public Integer getMinutesJouees() {
        return minutesJouees;
    }

    public void setMinutesJouees(Integer minutesJouees) {
        this.minutesJouees = minutesJouees;
    }
}
