package com.evcharge.dto;

import java.math.BigDecimal;

public class MonthlyStats {
    public String yearMonth;
    public BigDecimal totalCost;
    public BigDecimal totalKwh;
    public long sessionCount;
    public Long kmDriven;
    public BigDecimal pricePer100km;

    public MonthlyStats(String yearMonth, BigDecimal totalCost, BigDecimal totalKwh,
                        long sessionCount, Long kmDriven, BigDecimal pricePer100km) {
        this.yearMonth = yearMonth;
        this.totalCost = totalCost;
        this.totalKwh = totalKwh;
        this.sessionCount = sessionCount;
        this.kmDriven = kmDriven;
        this.pricePer100km = pricePer100km;
    }
}
