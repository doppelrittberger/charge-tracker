package com.evcharge.service;

import com.evcharge.dto.ChargeSessionRequest;
import com.evcharge.dto.ChargeSessionResponse;
import com.evcharge.dto.MonthlyStats;
import com.evcharge.dto.StatisticsResponse;
import com.evcharge.model.ChargeSession;
import com.evcharge.model.OdometerSnapshot;
import com.evcharge.storage.JsonStorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class ChargeSessionService {

    @Inject
    JsonStorageService storage;

    @Inject
    OdometerService odometerService;

    public ChargeSessionResponse createSession(ChargeSessionRequest request) {
        ChargeSession session = new ChargeSession(
            null,
            request.timestamp != null ? request.timestamp : LocalDateTime.now(),
            request.kwhAmount,
            request.pricePerKwh
        );
        session = storage.save(session);
        return new ChargeSessionResponse(session);
    }

    public List<ChargeSessionResponse> getAllSessions() {
        return storage.findAll()
            .stream()
            .map(ChargeSessionResponse::new)
            .collect(Collectors.toList());
    }

    public ChargeSessionResponse getSessionById(Long id) {
        ChargeSession session = storage.findById(id);
        if (session == null) {
            return null;
        }
        return new ChargeSessionResponse(session);
    }

    public boolean deleteSession(Long id) {
        return storage.deleteById(id);
    }

    public StatisticsResponse getStatistics() {
        List<ChargeSession> sessions = storage.findAll();
        List<OdometerSnapshot> snapshots = storage.findAllSnapshots(); // used for monthly stats

        if (sessions.isEmpty()) {
            return new StatisticsResponse(BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, 0L, null, List.of());
        }

        BigDecimal totalKwh = sessions.stream()
            .map(s -> s.kwhAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalPrice = sessions.stream()
            .map(ChargeSession::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal averagePricePerKwh = sessions.stream()
            .map(s -> s.pricePerKwh)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(sessions.size()), 2, RoundingMode.HALF_UP);

        BigDecimal pricePer100km = odometerService.getOdometer()
            .filter(km -> km > 0)
            .map(km -> totalPrice.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(km), 2, RoundingMode.HALF_UP))
            .orElse(null);

        List<MonthlyStats> monthlyStats = computeMonthlyStats(sessions, snapshots);

        return new StatisticsResponse(totalKwh, averagePricePerKwh, totalPrice,
                sessions.size(), pricePer100km, monthlyStats);
    }

    private List<MonthlyStats> computeMonthlyStats(List<ChargeSession> sessions, List<OdometerSnapshot> snapshots) {
        Map<YearMonth, List<ChargeSession>> byMonth = sessions.stream()
            .collect(Collectors.groupingBy(s -> YearMonth.from(s.timestamp)));

        List<OdometerSnapshot> sortedSnaps = snapshots.stream()
            .sorted(Comparator.comparing(s -> s.getYearMonth()))
            .collect(Collectors.toList());

        List<MonthlyStats> result = new ArrayList<>();

        for (int i = 0; i < sortedSnaps.size(); i++) {
            OdometerSnapshot current = sortedSnaps.get(i);
            YearMonth month = current.getYearMonth();
            Optional<OdometerSnapshot> prev = i > 0 ? Optional.of(sortedSnaps.get(i - 1)) : Optional.empty();

            List<ChargeSession> monthSessions = byMonth.getOrDefault(month, List.of());
            BigDecimal cost = monthSessions.stream()
                .map(ChargeSession::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
            BigDecimal kwh = monthSessions.stream()
                .map(s -> s.kwhAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

            Long kmDriven = prev.map(p -> current.odometerKm - p.odometerKm).orElse(null);
            BigDecimal pricePer100km = null;
            if (kmDriven != null && kmDriven > 0 && cost.compareTo(BigDecimal.ZERO) > 0) {
                pricePer100km = cost
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(kmDriven), 2, RoundingMode.HALF_UP);
            }

            result.add(new MonthlyStats(month.toString(), cost, kwh,
                    monthSessions.size(), kmDriven, pricePer100km));
        }

        result.sort(Comparator.comparing((MonthlyStats m) -> m.yearMonth).reversed());
        return result;
    }

    public List<ChargeSessionResponse> getSessionsByDateRange(LocalDateTime start, LocalDateTime end) {
        return storage.findAll()
            .stream()
            .filter(s -> !s.timestamp.isBefore(start) && !s.timestamp.isAfter(end))
            .map(ChargeSessionResponse::new)
            .collect(Collectors.toList());
    }
}
