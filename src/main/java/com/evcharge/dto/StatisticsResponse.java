package com.evcharge.dto;

import java.math.BigDecimal;
import java.util.List;

public class StatisticsResponse {
    public BigDecimal totalKwh;
    public BigDecimal averagePricePerKwh;
    public BigDecimal totalPrice;
    public long sessionCount;
    public BigDecimal pricePer100km;
    public BigDecimal kwhPer100km;
    public List<MonthlyStats> monthlyStats;

    public StatisticsResponse() {
    }

    public StatisticsResponse(BigDecimal totalKwh, BigDecimal averagePricePerKwh, BigDecimal totalPrice,
                               long sessionCount, BigDecimal pricePer100km, BigDecimal kwhPer100km,
                               List<MonthlyStats> monthlyStats) {
        this.totalKwh = totalKwh;
        this.averagePricePerKwh = averagePricePerKwh;
        this.totalPrice = totalPrice;
        this.sessionCount = sessionCount;
        this.pricePer100km = pricePer100km;
        this.kwhPer100km = kwhPer100km;
        this.monthlyStats = monthlyStats;
    }
}
