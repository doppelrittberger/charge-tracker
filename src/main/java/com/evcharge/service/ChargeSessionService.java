package com.evcharge.service;

import com.evcharge.dto.ChargeSessionRequest;
import com.evcharge.dto.ChargeSessionResponse;
import com.evcharge.dto.StatisticsResponse;
import com.evcharge.model.ChargeSession;
import com.evcharge.storage.JsonStorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ChargeSessionService {

    @Inject
    JsonStorageService storage;

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
        
        if (sessions.isEmpty()) {
            return new StatisticsResponse(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0L
            );
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

        return new StatisticsResponse(
            totalKwh,
            averagePricePerKwh,
            totalPrice,
            sessions.size()
        );
    }

    public List<ChargeSessionResponse> getSessionsByDateRange(LocalDateTime start, LocalDateTime end) {
        return storage.findAll()
            .stream()
            .filter(s -> !s.timestamp.isBefore(start) && !s.timestamp.isAfter(end))
            .map(ChargeSessionResponse::new)
            .collect(Collectors.toList());
    }
}
