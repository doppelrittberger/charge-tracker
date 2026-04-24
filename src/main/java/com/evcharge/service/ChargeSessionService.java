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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
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

    private static final Logger LOG = Logger.getLogger(ChargeSessionService.class);

    @Inject
    JsonStorageService storage;

    @Inject
    OdometerService odometerService;

    @ConfigProperty(name = "vehicle.battery.capacity-kwh", defaultValue = "77.0")
    BigDecimal batteryCapacityKwh;

    public ChargeSessionResponse createSession(ChargeSessionRequest request) {
        ChargeSession session = new ChargeSession(
            null,
            request.timestamp != null ? request.timestamp : LocalDateTime.now(),
            request.kwhAmount,
            request.pricePerKwh
        );
        session.odometerKm = request.odometerKm;
        session.socStart = request.socStart;
        ChargeSession storedSession = storage.save(session);
        LOG.infof("Session created: id=%d timestamp=%s kwh=%s price=%s odometer=%s socStart=%s",
            storedSession.sessionId, storedSession.timestamp, storedSession.kwhAmount,
            storedSession.pricePerKwh, storedSession.odometerKm, storedSession.socStart);
        return toResponses(storage.findAll()).stream()
            .filter(r -> r.sessionId.equals(storedSession.sessionId))
            .findFirst().orElse(new ChargeSessionResponse(storedSession, null, batteryCapacityKwh));
    }

    public ChargeSessionResponse updateSession(Long id, ChargeSessionRequest request) {
        ChargeSession session = storage.findById(id);
        if (session == null) {
            LOG.warnf("Update failed: session id=%d not found", id);
            return null;
        }
        LOG.infof("Session update id=%d: timestamp=%s->%s kwh=%s->%s price=%s->%s odometer=%s->%s socStart=%s->%s",
            id, session.timestamp, request.timestamp, session.kwhAmount, request.kwhAmount,
            session.pricePerKwh, request.pricePerKwh, session.odometerKm, request.odometerKm,
            session.socStart, request.socStart);
        session.timestamp = request.timestamp != null ? request.timestamp : session.timestamp;
        session.kwhAmount = request.kwhAmount;
        session.pricePerKwh = request.pricePerKwh;
        session.odometerKm = request.odometerKm;
        session.socStart = request.socStart;
        storage.save(session);
        return toResponses(storage.findAll()).stream()
            .filter(r -> r.sessionId.equals(id))
            .findFirst().orElse(null);
    }

    public List<ChargeSessionResponse> getAllSessions() {
        return toResponses(storage.findAll());
    }

    public ChargeSessionResponse getSessionById(Long id) {
        if (storage.findById(id) == null) return null;
        return toResponses(storage.findAll()).stream()
            .filter(r -> r.sessionId.equals(id))
            .findFirst().orElse(null);
    }

    private List<ChargeSessionResponse> toResponses(List<ChargeSession> sessions) {
        List<ChargeSession> sorted = sessions.stream()
            .sorted(Comparator.comparing((ChargeSession s) -> s.timestamp).reversed())
            .toList();
        List<ChargeSessionResponse> result = new ArrayList<>();
        for (ChargeSession s : sorted) {
            ChargeSession prevSession = null;
            Long prevOdo = null;
            if (s.odometerKm != null) {
                // find the immediately preceding session by timestamp that has an odometer
                ChargeSession immediatePrev = sorted.stream()
                    .filter(o -> o.odometerKm != null && o.timestamp.isBefore(s.timestamp))
                    .max(Comparator.comparing(o -> o.timestamp))
                    .orElse(null);
                if (immediatePrev != null && immediatePrev.odometerKm.equals(s.odometerKm)) {
                    // same odometer as preceding session — no driving happened
                    prevOdo = s.odometerKm;
                } else {
                    prevSession = sorted.stream()
                        .filter(o -> o.odometerKm != null
                            && o.odometerKm < s.odometerKm
                            && o.timestamp.isBefore(s.timestamp))
                        .max(Comparator.comparing(o -> o.timestamp))
                        .orElse(null);
                    prevOdo = prevSession != null ? prevSession.odometerKm : null;
                }
            }
            ChargeSessionResponse r = new ChargeSessionResponse(s, prevOdo, batteryCapacityKwh);
            if (r.kmDriven != null && r.kmDriven > 0
                    && prevSession != null && prevSession.socStart != null
                    && s.socStart != null
                    && batteryCapacityKwh.compareTo(BigDecimal.ZERO) > 0) {
                int prevSocEnd = Math.min(prevSession.socStart + prevSession.kwhAmount
                    .multiply(BigDecimal.valueOf(100))
                    .divide(batteryCapacityKwh, 0, RoundingMode.HALF_UP)
                    .intValue(), 100);
                // net kWh stored in battery during prev session (capped to capacity)
                BigDecimal netStored = batteryCapacityKwh
                    .multiply(BigDecimal.valueOf(Math.max(0, prevSocEnd - prevSession.socStart)))
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                // kWh that went to driving = total charged minus what stayed in the battery
                BigDecimal kwhConsumed = prevSession.kwhAmount.subtract(netStored)
                    // plus whatever was drained from the battery before this charge session started
                    .add(batteryCapacityKwh
                        .multiply(BigDecimal.valueOf(Math.max(0, prevSocEnd - s.socStart)))
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                if (kwhConsumed.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal drivingCost = kwhConsumed.multiply(prevSession.pricePerKwh);
                    r.pricePer100km = drivingCost
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(r.kmDriven), 2, RoundingMode.HALF_UP);
                    r.kwhPer100km = kwhConsumed
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(r.kmDriven), 2, RoundingMode.HALF_UP);
                }
            }
            result.add(r);
        }
        return result;
    }

    public boolean deleteSession(Long id) {
        ChargeSession session = storage.findById(id);
        boolean deleted = storage.deleteById(id);
        if (deleted) {
            LOG.infof("Session deleted: id=%d timestamp=%s kwh=%s",
                id, session != null ? session.timestamp : "?", session != null ? session.kwhAmount : "?");
        } else {
            LOG.warnf("Delete failed: session id=%d not found", id);
        }
        return deleted;
    }

    public StatisticsResponse getStatistics() {
        List<ChargeSession> sessions = storage.findAll();
        List<OdometerSnapshot> snapshots = storage.findAllSnapshots(); // used for monthly stats

        if (sessions.isEmpty()) {
            return new StatisticsResponse(BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, 0L, null, null, List.of());
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

        BigDecimal kwhPer100km = odometerService.getOdometer()
            .filter(km -> km > 0)
            .map(km -> totalKwh.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(km), 2, RoundingMode.HALF_UP))
            .orElse(null);

        List<MonthlyStats> monthlyStats = computeMonthlyStats(sessions, snapshots);

        return new StatisticsResponse(totalKwh, averagePricePerKwh, totalPrice,
                sessions.size(), pricePer100km, kwhPer100km, monthlyStats);
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
        List<ChargeSession> filtered = storage.findAll().stream()
            .filter(s -> !s.timestamp.isBefore(start) && !s.timestamp.isAfter(end))
            .collect(Collectors.toList());
        return toResponses(filtered);
    }
}
