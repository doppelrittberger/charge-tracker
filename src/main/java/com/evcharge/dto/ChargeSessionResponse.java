package com.evcharge.dto;

import com.evcharge.model.ChargeSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class ChargeSessionResponse {
    public Long sessionId;
    public LocalDateTime timestamp;
    public BigDecimal kwhAmount;
    public BigDecimal pricePerKwh;
    public Long odometerKm;
    public Long kmDriven;
    public Integer socStart;
    public Integer socEnd;
    public BigDecimal pricePer100km;
    public BigDecimal kwhPer100km;

    public ChargeSessionResponse() {
    }

    public ChargeSessionResponse(ChargeSession session, Long prevOdometerKm, BigDecimal batteryCapacityKwh) {
        this.sessionId = session.sessionId;
        this.timestamp = session.timestamp;
        this.kwhAmount = session.kwhAmount.setScale(2, RoundingMode.HALF_UP);
        this.pricePerKwh = session.pricePerKwh.setScale(2, RoundingMode.HALF_UP);
        this.odometerKm = session.odometerKm;
        this.kmDriven = (session.odometerKm != null && prevOdometerKm != null)
            ? session.odometerKm - prevOdometerKm : null;
        this.socStart = session.socStart;
        if (session.socStart != null && batteryCapacityKwh != null
                && batteryCapacityKwh.compareTo(BigDecimal.ZERO) > 0) {
            int computed = session.socStart + session.kwhAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(batteryCapacityKwh, 0, RoundingMode.HALF_UP)
                .intValue();
            this.socEnd = Math.min(computed, 100);
        }
    }

    public BigDecimal getTotalPrice() {
        return kwhAmount.multiply(pricePerKwh).setScale(2, RoundingMode.HALF_UP);
    }
}
