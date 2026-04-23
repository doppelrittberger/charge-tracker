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

    public ChargeSessionResponse() {
    }

    public ChargeSessionResponse(ChargeSession session) {
        this.sessionId = session.sessionId;
        this.timestamp = session.timestamp;
        this.kwhAmount = session.kwhAmount.setScale(2, RoundingMode.HALF_UP);
        this.pricePerKwh = session.pricePerKwh.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalPrice() {
        return kwhAmount.multiply(pricePerKwh).setScale(2, RoundingMode.HALF_UP);
    }
}
