package com.example.platforme_backend;

public class PlayerSearchCriteria {

    private String query;          // Recherche textuelle générale
    private String position;       // Position du joueur
    private String club;          // Club du joueur
    private String league;        // Championnat
    private Integer ageMin;       // Âge minimum
    private Integer ageMax;       // Âge maximum
    private Integer valueMin;     // Valeur marchande minimum (en millions)
    private Integer valueMax;     // Valeur marchande maximum (en millions)
    private String nationality;   // Nationalité

    // Constructeurs
    public PlayerSearchCriteria() {}

    public PlayerSearchCriteria(String query, String position, String club, String league,
                                Integer ageMin, Integer ageMax, Integer valueMin, Integer valueMax,
                                String nationality) {
        this.query = query;
        this.position = position;
        this.club = club;
        this.league = league;
        this.ageMin = ageMin;
        this.ageMax = ageMax;
        this.valueMin = valueMin;
        this.valueMax = valueMax;
        this.nationality = nationality;
    }

    // Méthodes utilitaires
    public boolean hasTextualSearch() {
        return query != null && !query.trim().isEmpty();
    }

    public boolean hasPositionFilter() {
        return position != null && !position.trim().isEmpty();
    }

    public boolean hasClubFilter() {
        return club != null && !club.trim().isEmpty();
    }

    public boolean hasLeagueFilter() {
        return league != null && !league.trim().isEmpty();
    }

    public boolean hasAgeFilter() {
        return ageMin != null || ageMax != null;
    }

    public boolean hasValueFilter() {
        return valueMin != null || valueMax != null;
    }

    public boolean hasNationalityFilter() {
        return nationality != null && !nationality.trim().isEmpty();
    }

    public boolean isEmpty() {
        return !hasTextualSearch() && !hasPositionFilter() && !hasClubFilter() &&
                !hasLeagueFilter() && !hasAgeFilter() && !hasValueFilter() &&
                !hasNationalityFilter();
    }

    // Getters et Setters
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getClub() {
        return club;
    }

    public void setClub(String club) {
        this.club = club;
    }

    public String getLeague() {
        return league;
    }

    public void setLeague(String league) {
        this.league = league;
    }

    public Integer getAgeMin() {
        return ageMin;
    }

    public void setAgeMin(Integer ageMin) {
        this.ageMin = ageMin;
    }

    public Integer getAgeMax() {
        return ageMax;
    }

    public void setAgeMax(Integer ageMax) {
        this.ageMax = ageMax;
    }

    public Integer getValueMin() {
        return valueMin;
    }

    public void setValueMin(Integer valueMin) {
        this.valueMin = valueMin;
    }

    public Integer getValueMax() {
        return valueMax;
    }

    public void setValueMax(Integer valueMax) {
        this.valueMax = valueMax;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    @Override
    public String toString() {
        return "PlayerSearchCriteria{" +
                "query='" + query + '\'' +
                ", position='" + position + '\'' +
                ", club='" + club + '\'' +
                ", league='" + league + '\'' +
                ", ageMin=" + ageMin +
                ", ageMax=" + ageMax +
                ", valueMin=" + valueMin +
                ", valueMax=" + valueMax +
                ", nationality='" + nationality + '\'' +
                '}';
    }
}