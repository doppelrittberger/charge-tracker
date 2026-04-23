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

    @Inject
    @RestClient
    HaApiClient haApiClient;

    @ConfigProperty(name = "ha.odometer.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "ha.odometer.token", defaultValue = "")
    String token;

    @ConfigProperty(name = "ha.odometer.entity", defaultValue = "sensor.skoda_odometer")
    String odometerEntity;

    @ConfigProperty(name = "ha.soc.entity", defaultValue = "sensor.skoda_state_of_charge")
    String socEntity;

    public Optional<Integer> getSocAt(OffsetDateTime at) {
        if (!enabled || token.isBlank()) return Optional.empty();
        try {
            String start = at.minusHours(1).format(FMT);
            String end = at.plusMinutes(1).format(FMT);
            List<List<HaApiClient.HaHistoryState>> result =
                haApiClient.getHistory(start, socEntity, end, true, "Bearer " + token);
            if (result == null || result.isEmpty() || result.get(0).isEmpty()) {
                return Optional.empty();
            }
            List<HaApiClient.HaHistoryState> states = result.get(0);
            String value = states.get(states.size() - 1).state;
            if (value == null || value.equals("unavailable") || value.equals("unknown")) {
                return Optional.empty();
            }
            return Optional.of((int) Math.round(Double.parseDouble(value)));
        } catch (Exception e) {
            LOG.warnf("Failed to fetch SoC history at %s: %s", at, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Long> getOdometerAt(OffsetDateTime at) {
        if (!enabled || token.isBlank()) return Optional.empty();
        try {
            String start = at.minusHours(2).format(FMT);
            String end = at.plusMinutes(1).format(FMT);
            List<List<HaApiClient.HaHistoryState>> result =
                haApiClient.getHistory(start, odometerEntity, end, true, "Bearer " + token);
            if (result == null || result.isEmpty() || result.get(0).isEmpty()) {
                return Optional.empty();
            }
            List<HaApiClient.HaHistoryState> states = result.get(0);
            String value = states.get(states.size() - 1).state;
            if (value == null || value.equals("unavailable") || value.equals("unknown")) {
                return Optional.empty();
            }
            return Optional.of(Math.round(Double.parseDouble(value)));
        } catch (Exception e) {
            LOG.warnf("Failed to fetch odometer history at %s: %s", at, e.getMessage());
            return Optional.empty();
        }
    }
}
