package com.evcharge.service;

import com.evcharge.client.HaApiClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class HaHistoryService {

    private static final Logger LOG = Logger.getLogger(HaHistoryService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final HaApiClient haApiClient;
    private final boolean enabled;
    private final String token;
    private final String odometerEntity;
    private final String socEntity;

    @Inject
    public HaHistoryService(
            @RestClient HaApiClient haApiClient,
            @ConfigProperty(name = "ha.enabled") boolean enabled,
            @ConfigProperty(name = "ha.token") String token,
            @ConfigProperty(name = "ha.odometer.entity") String odometerEntity,
            @ConfigProperty(name = "ha.soc.entity") String socEntity) {
        this.haApiClient = haApiClient;
        this.enabled = enabled;
        this.token = token;
        this.odometerEntity = odometerEntity;
        this.socEntity = socEntity;
    }

    public Optional<Integer> getSocAt(OffsetDateTime at) {
        if (!enabled || token.isBlank()) {
            LOG.debugf("HA SoC history skipped (disabled or no token) for %s", at);
            return Optional.empty();
        }
        try {
            String start = at.minusHours(1).format(FMT);
            String end = at.plusMinutes(1).format(FMT);
            List<List<HaApiClient.HaHistoryState>> result =
                haApiClient.getHistory(start, socEntity, end, true, "Bearer " + token);
            if (result == null || result.isEmpty() || result.get(0).isEmpty()) {
                LOG.debugf("HA SoC history: no states found for entity=%s at %s", socEntity, at);
                return Optional.empty();
            }
            List<HaApiClient.HaHistoryState> states = result.get(0);
            String value = states.get(states.size() - 1).state;
            if (value == null || value.equals("unavailable") || value.equals("unknown")) {
                LOG.debugf("HA SoC history: state unavailable for entity=%s at %s", socEntity, at);
                return Optional.empty();
            }
            int soc = (int) Math.round(Double.parseDouble(value));
            LOG.debugf("HA SoC history: resolved %d%% for entity=%s at %s", soc, socEntity, at);
            return Optional.of(soc);
        } catch (Exception e) {
            LOG.warnf("Failed to fetch SoC history at %s: %s", at, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Long> getOdometerAt(OffsetDateTime at) {
        if (!enabled || token.isBlank()) {
            LOG.debugf("HA odometer history skipped (disabled or no token) for %s", at);
            return Optional.empty();
        }
        try {
            String start = at.minusHours(2).format(FMT);
            String end = at.plusMinutes(1).format(FMT);
            List<List<HaApiClient.HaHistoryState>> result =
                haApiClient.getHistory(start, odometerEntity, end, true, "Bearer " + token);
            if (result == null || result.isEmpty() || result.get(0).isEmpty()) {
                LOG.debugf("HA odometer history: no states found for entity=%s at %s", odometerEntity, at);
                return Optional.empty();
            }
            List<HaApiClient.HaHistoryState> states = result.get(0);
            String value = states.get(states.size() - 1).state;
            if (value == null || value.equals("unavailable") || value.equals("unknown")) {
                LOG.debugf("HA odometer history: state unavailable for entity=%s at %s", odometerEntity, at);
                return Optional.empty();
            }
            long odometer = Math.round(Double.parseDouble(value));
            LOG.debugf("HA odometer history: resolved %d km for entity=%s at %s", odometer, odometerEntity, at);
            return Optional.of(odometer);
        } catch (Exception e) {
            LOG.warnf("Failed to fetch odometer history at %s: %s", at, e.getMessage());
            return Optional.empty();
        }
    }
}
