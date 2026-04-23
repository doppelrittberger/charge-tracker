package com.evcharge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChargeSession {

    public Long sessionId;
    public Long evccId;
    public LocalDateTime timestamp;
    public BigDecimal kwhAmount;
    public BigDecimal pricePerKwh;
    public Long odometerKm;
    public Integer socStart;

    public ChargeSession() {
    }

    public ChargeSession(Long sessionId, LocalDateTime timestamp, BigDecimal kwhAmount, BigDecimal pricePerKwh) {
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.kwhAmount = kwhAmount;
        this.pricePerKwh = pricePerKwh;
    }

    @JsonIgnore
    public BigDecimal getTotalPrice() {
        return kwhAmount.multiply(pricePerKwh).setScale(2, RoundingMode.HALF_UP);
    }
}
