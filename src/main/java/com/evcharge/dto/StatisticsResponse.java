package com.evcharge.dto;

import java.math.BigDecimal;

public class StatisticsResponse {
    public BigDecimal totalKwh;
    public BigDecimal averagePricePerKwh;
    public BigDecimal totalPrice;
    public long sessionCount;

    public StatisticsResponse() {
    }

    public StatisticsResponse(BigDecimal totalKwh, BigDecimal averagePricePerKwh, BigDecimal totalPrice, long sessionCount) {
        this.totalKwh = totalKwh;
        this.averagePricePerKwh = averagePricePerKwh;
        this.totalPrice = totalPrice;
        this.sessionCount = sessionCount;
    }
}
