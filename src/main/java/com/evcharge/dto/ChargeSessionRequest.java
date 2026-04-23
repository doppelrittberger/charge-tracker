package com.evcharge.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ChargeSessionRequest {
    public BigDecimal kwhAmount;
    public BigDecimal pricePerKwh;
    public LocalDateTime timestamp;

    public ChargeSessionRequest() {
    }
}
