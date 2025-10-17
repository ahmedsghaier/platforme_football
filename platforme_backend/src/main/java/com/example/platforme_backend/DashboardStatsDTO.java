package com.example.platforme_backend;

import lombok.Setter;

public class DashboardStatsDTO {

    @Setter
    private int searchesThisMonth;
    @Setter
    private double searchGrowthPercentage;
    @Setter
    private int favoritePlayers;
    @Setter
    private int favoritePlayersAddedThisWeek;
    @Setter
    private int comparisons;
    @Setter
    private double comparisonGrowthPercentage;
    @Setter
    private int reportsExported;
    @Setter
    private int reportsExportedThisWeek;

    public DashboardStatsDTO(int searchesThisMonth, double searchGrowthPercentage,
                             int favoritePlayers, int favoritePlayersAddedThisWeek,
                             int comparisons, double comparisonGrowthPercentage,
                             int reportsExported, int reportsExportedThisWeek) {
        this.searchesThisMonth = searchesThisMonth;
        this.searchGrowthPercentage = searchGrowthPercentage;
        this.favoritePlayers = favoritePlayers;
        this.favoritePlayersAddedThisWeek = favoritePlayersAddedThisWeek;
        this.comparisons = comparisons;
        this.comparisonGrowthPercentage = comparisonGrowthPercentage;
        this.reportsExported = reportsExported;
        this.reportsExportedThisWeek = reportsExportedThisWeek;
    }
    public DashboardStatsDTO() {}

    // Getters
    public int getSearchesThisMonth() { return searchesThisMonth; }
    public double getSearchGrowthPercentage() { return searchGrowthPercentage; }
    public int getFavoritePlayers() { return favoritePlayers; }
    public int getFavoritePlayersAddedThisWeek() { return favoritePlayersAddedThisWeek; }
    public int getComparisons() { return comparisons; }
    public double getComparisonGrowthPercentage() { return comparisonGrowthPercentage; }
    public int getReportsExported() { return reportsExported; }
    public int getReportsExportedThisWeek() { return reportsExportedThisWeek; }

}

