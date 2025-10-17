package com.example.platforme_backend;

import java.util.Map;

/**
 * Classe pour les statistiques générales des joueurs
 */
public class PlayerStatsOverview {

    private long totalPlayers;
    private Map<String, Long> playersByPosition;
    private Map<String, Long> playersByClub;
    private Map<String, Long> playersByNationality;
    private double averageAge;
    private double averageMarketValue;
    private int youngestPlayerAge;
    private int oldestPlayerAge;
    private double highestMarketValue;
    private double lowestMarketValue;
    private String mostValuablePlayer;
    private String youngestPlayer;
    private String oldestPlayer;

    // Constructeurs
    public PlayerStatsOverview() {}

    public PlayerStatsOverview(long totalPlayers, Map<String, Long> playersByPosition,
                               Map<String, Long> playersByClub, Map<String, Long> playersByNationality,
                               double averageAge, double averageMarketValue, int youngestPlayerAge,
                               int oldestPlayerAge, double highestMarketValue, double lowestMarketValue,
                               String mostValuablePlayer, String youngestPlayer, String oldestPlayer) {
        this.totalPlayers = totalPlayers;
        this.playersByPosition = playersByPosition;
        this.playersByClub = playersByClub;
        this.playersByNationality = playersByNationality;
        this.averageAge = averageAge;
        this.averageMarketValue = averageMarketValue;
        this.youngestPlayerAge = youngestPlayerAge;
        this.oldestPlayerAge = oldestPlayerAge;
        this.highestMarketValue = highestMarketValue;
        this.lowestMarketValue = lowestMarketValue;
        this.mostValuablePlayer = mostValuablePlayer;
        this.youngestPlayer = youngestPlayer;
        this.oldestPlayer = oldestPlayer;
    }

    // Getters et Setters
    public long getTotalPlayers() {
        return totalPlayers;
    }

    public void setTotalPlayers(long totalPlayers) {
        this.totalPlayers = totalPlayers;
    }

    public Map<String, Long> getPlayersByPosition() {
        return playersByPosition;
    }

    public void setPlayersByPosition(Map<String, Long> playersByPosition) {
        this.playersByPosition = playersByPosition;
    }

    public Map<String, Long> getPlayersByClub() {
        return playersByClub;
    }

    public void setPlayersByClub(Map<String, Long> playersByClub) {
        this.playersByClub = playersByClub;
    }

    public Map<String, Long> getPlayersByNationality() {
        return playersByNationality;
    }

    public void setPlayersByNationality(Map<String, Long> playersByNationality) {
        this.playersByNationality = playersByNationality;
    }

    public double getAverageAge() {
        return averageAge;
    }

    public void setAverageAge(double averageAge) {
        this.averageAge = averageAge;
    }

    public double getAverageMarketValue() {
        return averageMarketValue;
    }

    public void setAverageMarketValue(double averageMarketValue) {
        this.averageMarketValue = averageMarketValue;
    }

    public int getYoungestPlayerAge() {
        return youngestPlayerAge;
    }

    public void setYoungestPlayerAge(int youngestPlayerAge) {
        this.youngestPlayerAge = youngestPlayerAge;
    }

    public int getOldestPlayerAge() {
        return oldestPlayerAge;
    }

    public void setOldestPlayerAge(int oldestPlayerAge) {
        this.oldestPlayerAge = oldestPlayerAge;
    }

    public double getHighestMarketValue() {
        return highestMarketValue;
    }

    public void setHighestMarketValue(double highestMarketValue) {
        this.highestMarketValue = highestMarketValue;
    }

    public double getLowestMarketValue() {
        return lowestMarketValue;
    }

    public void setLowestMarketValue(double lowestMarketValue) {
        this.lowestMarketValue = lowestMarketValue;
    }

    public String getMostValuablePlayer() {
        return mostValuablePlayer;
    }

    public void setMostValuablePlayer(String mostValuablePlayer) {
        this.mostValuablePlayer = mostValuablePlayer;
    }

    public String getYoungestPlayer() {
        return youngestPlayer;
    }

    public void setYoungestPlayer(String youngestPlayer) {
        this.youngestPlayer = youngestPlayer;
    }

    public String getOldestPlayer() {
        return oldestPlayer;
    }

    public void setOldestPlayer(String oldestPlayer) {
        this.oldestPlayer = oldestPlayer;
    }
}