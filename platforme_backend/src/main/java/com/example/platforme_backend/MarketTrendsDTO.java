package com.example.platforme_backend;

import lombok.Setter;

@Setter
public class MarketTrendsDTO {
    private double attackersGrowth;
    private double midfieldersGrowth;
    private double youngTalentsGrowth;
    private double defendersGrowth;

    public MarketTrendsDTO(double attackersGrowth, double midfieldersGrowth,
                           double youngTalentsGrowth, double defendersGrowth) {
        this.attackersGrowth = attackersGrowth;
        this.midfieldersGrowth = midfieldersGrowth;
        this.youngTalentsGrowth = youngTalentsGrowth;
        this.defendersGrowth = defendersGrowth;
    }

    public MarketTrendsDTO() {

    }

    public double getAttackersGrowth() { return attackersGrowth; }
    public double getMidfieldersGrowth() { return midfieldersGrowth; }
    public double getYoungTalentsGrowth() { return youngTalentsGrowth; }
    public double getDefendersGrowth() { return defendersGrowth; }
}
