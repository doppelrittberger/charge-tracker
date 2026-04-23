package com.evcharge.service;

import com.evcharge.client.HaApiClient;
import com.evcharge.model.OdometerSnapshot;
import com.evcharge.storage.JsonStorageService;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

@ApplicationScoped
public class OdometerService {

    private static final Logger LOG = Logger.getLogger(OdometerService.class);

    @Inject
    @RestClient
    HaApiClient haApiClient;

    @Inject
    JsonStorageService storage;

    @ConfigProperty(name = "ha.odometer.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "ha.odometer.token", defaultValue = "")
    String token;

    @ConfigProperty(name = "ha.odometer.entity", defaultValue = "sensor.skoda_odometer")
    String entityId;

    private Long latestOdometer = null;
    private YearMonth lastSnapshotMonth = null;

    void onStart(@Observes StartupEvent ev) {
        fetchOdometer();
    }

    @Scheduled(every = "5m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void fetchOdometer() {
        if (!enabled || token.isBlank()) {
            return;
        }
        try {
            HaApiClient.HaState state = haApiClient.getState(entityId, "Bearer " + token);
            if (state == null || state.state == null
                    || state.state.equals("unavailable") || state.state.equals("unknown")) {
                LOG.warnf("HA odometer entity '%s' is unavailable", entityId);
                return;
            }
            latestOdometer = Math.round(Double.parseDouble(state.state));
            LOG.debugf("Odometer updated: %d km", latestOdometer);
            maybeRecordBaselineSnapshot();
            maybeRecordMonthlySnapshot();
        } catch (Exception e) {
            LOG.errorf("Failed to fetch odometer from HA: %s", e.getMessage());
        }
    }

    private void maybeRecordBaselineSnapshot() {
        if (storage.findAllSnapshots().isEmpty()) {
            YearMonth currentMonth = YearMonth.from(LocalDate.now());
            OdometerSnapshot baseline = new OdometerSnapshot(currentMonth, latestOdometer, LocalDate.now());
            storage.saveSnapshot(baseline);
            LOG.infof("Saved baseline odometer snapshot for %s: %d km", currentMonth, latestOdometer);
        }
    }

    private void maybeRecordMonthlySnapshot() {
        if (latestOdometer == null) return;
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth prevMonth = currentMonth.minusMonths(1);

        if (lastSnapshotMonth != null && lastSnapshotMonth.equals(prevMonth)) {
            return;
        }

        boolean isFirstDayOfMonth = today.getDayOfMonth() <= 2;
        if (!isFirstDayOfMonth) return;

        if (storage.findSnapshot(prevMonth).isEmpty()) {
            OdometerSnapshot snapshot = new OdometerSnapshot(prevMonth, latestOdometer, today);
            storage.saveSnapshot(snapshot);
            lastSnapshotMonth = prevMonth;
            LOG.infof("Saved end-of-month odometer snapshot for %s: %d km", prevMonth, latestOdometer);
        }
    }

    public Optional<Long> getOdometer() {
        if (latestOdometer != null) return Optional.of(latestOdometer);
        return storage.findAllSnapshots().stream()
            .max(java.util.Comparator.comparing(s -> s.getYearMonth()))
            .map(s -> s.odometerKm);
    }
}
