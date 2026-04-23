package com.evcharge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EvccSession {
    public long id;
    public OffsetDateTime created;
    public OffsetDateTime finished;
    public double chargedEnergy;
    public Double pricePerKWh;
    public Double price;
    public String loadpoint;
    public String vehicle;
}
