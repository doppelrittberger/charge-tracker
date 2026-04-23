package com.evcharge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.time.YearMonth;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OdometerSnapshot {
    public String yearMonth;
    public long odometerKm;
    public LocalDate recordedAt;

    public OdometerSnapshot() {
    }

    public OdometerSnapshot(YearMonth yearMonth, long odometerKm, LocalDate recordedAt) {
        this.yearMonth = yearMonth.toString();
        this.odometerKm = odometerKm;
        this.recordedAt = recordedAt;
    }

    public YearMonth getYearMonth() {
        return YearMonth.parse(yearMonth);
    }
}
