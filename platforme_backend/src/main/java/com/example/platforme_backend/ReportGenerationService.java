package com.example.platforme_backend;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class ReportGenerationService {

    /**
     * Génère un rapport PDF
     */
    public byte[] generatePdfReport(CompleteDashboardDTO dashboardData) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Simulation de génération PDF
            // Dans un vrai projet, utiliser une bibliothèque comme iText ou Apache PDFBox
            StringBuilder pdfContent = new StringBuilder();
            pdfContent.append("=== RAPPORT DASHBOARD FOOTBALLAI ===\n\n");

            // Statistiques
            DashboardStatsDTO stats = dashboardData.getStats();
            pdfContent.append("STATISTIQUES:\n");
            pdfContent.append("- Recherches ce mois: ").append(stats.getSearchesThisMonth()).append("\n");
            pdfContent.append("- Croissance recherches: ").append(String.format("%.1f%%", stats.getSearchGrowthPercentage())).append("\n");
            pdfContent.append("- Joueurs favoris: ").append(stats.getFavoritePlayers()).append("\n");
            pdfContent.append("- Comparaisons: ").append(stats.getComparisons()).append("\n");
            pdfContent.append("- Rapports exportés: ").append(stats.getReportsExported()).append("\n\n");

            // Recherches récentes
            pdfContent.append("RECHERCHES RÉCENTES:\n");
            for (RecentSearchDTO search : dashboardData.getRecentSearches()) {
                pdfContent.append("- ").append(search.getPlayerName())
                        .append(" (").append(search.getClub()).append(") - ")
                        .append(search.getDate()).append("\n");
            }
            pdfContent.append("\n");

            // Joueurs favoris
            pdfContent.append("JOUEURS FAVORIS:\n");
            for (FavoritePlayerDTO player : dashboardData.getFavoritePlayers()) {
                pdfContent.append("- ").append(player.getName())
                        .append(" (").append(player.getClub()).append(") - ")
                        .append(player.getValue()).append(" - ")
                        .append(player.getTrend()).append("\n");
            }
            pdfContent.append("\n");

            // Tendances du marché
            MarketTrendsDTO trends = dashboardData.getMarketTrends();
            pdfContent.append("TENDANCES DU MARCHÉ:\n");
            pdfContent.append("- Attaquants: +").append(String.format("%.1f%%", trends.getAttackersGrowth())).append("\n");
            pdfContent.append("- Milieux créatifs: +").append(String.format("%.1f%%", trends.getMidfieldersGrowth())).append("\n");
            pdfContent.append("- Jeunes talents: +").append(String.format("%.1f%%", trends.getYoungTalentsGrowth())).append("\n");
            pdfContent.append("- Défenseurs: +").append(String.format("%.1f%%", trends.getDefendersGrowth())).append("\n\n");

            // Alertes
            pdfContent.append("ALERTES RÉCENTES:\n");
            for (AlertDTO alert : dashboardData.getAlerts()) {
                pdfContent.append("- [").append(alert.getType().toUpperCase()).append("] ")
                        .append(alert.getPlayer()).append(": ")
                        .append(alert.getMessage()).append(" (il y a ")
                        .append(alert.getTime()).append(")\n");
            }

            outputStream.write(pdfContent.toString().getBytes());
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }

    /**
     * Génère un rapport CSV
     */
    public byte[] generateCsvReport(CompleteDashboardDTO dashboardData) {
        try {
            StringBuilder csvContent = new StringBuilder();

            // En-tête du rapport
            csvContent.append("Type,Métrique,Valeur,Détail\n");

            // Statistiques principales
            DashboardStatsDTO stats = dashboardData.getStats();
            csvContent.append("Statistique,Recherches ce mois,").append(stats.getSearchesThisMonth()).append(",\n");
            csvContent.append("Statistique,Croissance recherches,").append(String.format("%.1f%%", stats.getSearchGrowthPercentage())).append(",\n");
            csvContent.append("Statistique,Joueurs favoris,").append(stats.getFavoritePlayers()).append(",\n");
            csvContent.append("Statistique,Favoris ajoutés cette semaine,").append(stats.getFavoritePlayersAddedThisWeek()).append(",\n");
            csvContent.append("Statistique,Comparaisons,").append(stats.getComparisons()).append(",\n");
            csvContent.append("Statistique,Croissance comparaisons,").append(String.format("%.1f%%", stats.getComparisonGrowthPercentage())).append(",\n");
            csvContent.append("Statistique,Rapports exportés,").append(stats.getReportsExported()).append(",\n");
            csvContent.append("Statistique,Rapports cette semaine,").append(stats.getReportsExportedThisWeek()).append(",\n");

            // Recherches récentes
            for (RecentSearchDTO search : dashboardData.getRecentSearches()) {
                csvContent.append("Recherche récente,").append(escapeCSV(search.getPlayerName()))
                        .append(",").append(escapeCSV(search.getClub()))
                        .append(",").append(search.getDate()).append("\n");
            }

            // Joueurs favoris
            for (FavoritePlayerDTO player : dashboardData.getFavoritePlayers()) {
                csvContent.append("Joueur favori,").append(escapeCSV(player.getName()))
                        .append(",").append(escapeCSV(player.getClub()))
                        .append(",").append(player.getValue()).append(" (").append(player.getTrend()).append(")\n");
            }

            // Tendances du marché
            MarketTrendsDTO trends = dashboardData.getMarketTrends();
            csvContent.append("Tendance marché,Attaquants,").append(String.format("%.1f%%", trends.getAttackersGrowth())).append(",\n");
            csvContent.append("Tendance marché,Milieux créatifs,").append(String.format("%.1f%%", trends.getMidfieldersGrowth())).append(",\n");
            csvContent.append("Tendance marché,Jeunes talents,").append(String.format("%.1f%%", trends.getYoungTalentsGrowth())).append(",\n");
            csvContent.append("Tendance marché,Défenseurs centraux,").append(String.format("%.1f%%", trends.getDefendersGrowth())).append(",\n");

            // Alertes
            for (AlertDTO alert : dashboardData.getAlerts()) {
                csvContent.append("Alerte,").append(alert.getType())
                        .append(",").append(escapeCSV(alert.getPlayer()))
                        .append(",").append(escapeCSV(alert.getMessage())).append(" (il y a ").append(alert.getTime()).append(")\n");
            }

            return csvContent.toString().getBytes("UTF-8");

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du CSV", e);
        }
    }

    /**
     * Échappe les caractères spéciaux pour CSV
     */
    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}