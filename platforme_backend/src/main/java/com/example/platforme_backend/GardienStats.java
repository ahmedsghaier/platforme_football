package com.example.platforme_backend;

import jakarta.persistence.*;

@Entity
@Table(name = "gardiens")
public class GardienStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "joueur_id")
    private Long joueurId;

    @Column(name = "pourcentage_arrets")
    private Double pourcentageArrets;

    @Column(name = "clean_sheets")
    private Integer cleanSheets;

    @Column(name = "penalties_arretes")
    private Integer penaltiesArretes;

    @Column(name = "matchs_joues")
    private Integer matchsJoues;

    @Column(name = "saves")
    private Integer saves;

    // Getters et setters

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

    public Double getPourcentageArrets() {
        return pourcentageArrets;
    }

    public void setPourcentageArrets(Double pourcentageArrets) {
        this.pourcentageArrets = pourcentageArrets;
    }

    public Integer getCleanSheets() {
        return cleanSheets;
    }

    public void setCleanSheets(Integer cleanSheets) {
        this.cleanSheets = cleanSheets;
    }

    public Integer getPenaltiesArretes() {
        return penaltiesArretes;
    }

    public void setPenaltiesArretes(Integer penaltiesArretes) {
        this.penaltiesArretes = penaltiesArretes;
    }

    public Integer getMatchsJoues() {
        return matchsJoues;
    }

    public void setMatchsJoues(Integer matchsJoues) {
        this.matchsJoues = matchsJoues;
    }

    public Integer getSaves() {
        return saves;
    }

    public void setSaves(Integer saves) {
        this.saves = saves;
    }
}
