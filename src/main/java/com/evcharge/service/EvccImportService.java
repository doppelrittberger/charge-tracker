package com.evcharge.service;

import com.evcharge.client.EvccApiClient;
import com.evcharge.dto.EvccSession;
import com.evcharge.model.ChargeSession;
import com.evcharge.storage.JsonStorageService;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@ApplicationScoped
public class EvccImportService {

    private static final Logger LOG = Logger.getLogger(EvccImportService.class);

    @Inject
    @RestClient
    EvccApiClient evccApiClient;

    @Inject
    JsonStorageService storage;

    @Inject
    HaHistoryService haHistoryService;

    @ConfigProperty(name = "evcc.import.enabled", defaultValue = "false")
    boolean importEnabled;

    void onStart(@Observes @Priority(200) StartupEvent ev) {
        importSessions();
    }

    @Scheduled(every = "5m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void importSessions() {
        if (!importEnabled) {
            return;
        }
        try {
            List<EvccSession> response = evccApiClient.getSessions();
            if (response == null) {
                LOG.warn("evcc returned empty sessions response");
                return;
            }

            List<ChargeSession> existing = storage.findAll();
            int imported = 0;
            int enriched = 0;

            for (EvccSession evccSession : response) {
                if (evccSession.created == null || evccSession.finished == null || evccSession.chargedEnergy <= 0) {
                    continue;
                }
                if (evccSession.pricePerKWh == null || evccSession.pricePerKWh <= 0) {
                    LOG.debugf("Skipping evcc session %d: no pricePerKWh", evccSession.id);
                    continue;
                }

                BigDecimal kwh = BigDecimal.valueOf(evccSession.chargedEnergy).setScale(2, RoundingMode.HALF_UP);
                BigDecimal pricePerKwh = BigDecimal.valueOf(evccSession.pricePerKWh).setScale(4, RoundingMode.HALF_UP);
                var created = evccSession.created.toLocalDateTime();

                ChargeSession match = existing.stream()
                    .filter(s -> s.evccId != null && s.evccId.equals(evccSession.id))
                    .findFirst().orElse(null);

                if (match == null) {
                    ChargeSession session = new ChargeSession(null, created, kwh, pricePerKwh);
                    session.evccId = evccSession.id;
                    session.odometerKm = resolveOdometer(evccSession);
                    session.socStart = haHistoryService.getSocAt(evccSession.created).orElse(0);
                    storage.save(session);
                    existing.add(session);
                    imported++;
                } else {
                    boolean changed = false;
                    if (match.evccId == null) {
                        match.evccId = evccSession.id;
                        changed = true;
                    }
                    if (match.odometerKm == null) {
                        match.odometerKm = resolveOdometer(evccSession);
                        if (match.odometerKm != null) changed = true;
                    }
                    if (match.socStart == null) {
                        match.socStart = haHistoryService.getSocAt(evccSession.created).orElse(0);
                        changed = true;
                    }
                    if (changed) {
                        storage.save(match);
                        enriched++;
                    }
                }
            }

            if (imported > 0 || enriched > 0) {
                LOG.infof("evcc import: %d new, %d enriched sessions", imported, enriched);
            } else {
                LOG.debug("evcc import: no new or missing data found");
            }
        } catch (Exception e) {
            LOG.errorf("Failed to import sessions from evcc: %s", e.getMessage());
        }
    }

    private Long resolveOdometer(EvccSession evccSession) {
        if (evccSession.odometer != null && evccSession.odometer > 0) {
            return Math.round(evccSession.odometer);
        }
        return haHistoryService.getOdometerAt(evccSession.finished).orElse(null);
    }
}
