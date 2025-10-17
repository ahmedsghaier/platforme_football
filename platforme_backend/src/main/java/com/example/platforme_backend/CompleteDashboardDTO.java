package com.example.platforme_backend;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Setter
@Getter
public class CompleteDashboardDTO {

    private DashboardStatsDTO stats;
    private List<RecentSearchDTO> recentSearches;
    private List<RecentSearchDTO> recentComparisons;
    private List<FavoritePlayerDTO> favoritePlayers;
    private List<AlertDTO> alerts;
    private MarketTrendsDTO marketTrends;
    public CompleteDashboardDTO() {};
    public CompleteDashboardDTO(
            DashboardStatsDTO stats,
            List<RecentSearchDTO> recentSearches,
            List<RecentSearchDTO> recentComparisons,
            List<FavoritePlayerDTO> favoritePlayers,
            List<AlertDTO> alerts,
            MarketTrendsDTO marketTrends) {
        this.stats = stats;
        this.recentSearches = recentSearches;
        this.recentComparisons = recentComparisons;
        this.favoritePlayers = favoritePlayers;
        this.alerts = alerts;
        this.marketTrends = marketTrends;
    }
    public DashboardStatsDTO getStats() { return stats; }
    public List<RecentSearchDTO> getRecentSearches() { return recentSearches; }
    public List<FavoritePlayerDTO> getFavoritePlayers() { return favoritePlayers; }
    public List<AlertDTO> getAlerts() { return alerts; }
    public MarketTrendsDTO getMarketTrends() { return marketTrends; }
}