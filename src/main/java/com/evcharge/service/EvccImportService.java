package com.evcharge.service;

import com.evcharge.client.EvccApiClient;
import com.evcharge.dto.EvccSession;
import com.evcharge.model.ChargeSession;
import com.evcharge.storage.JsonStorageService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
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

    @ConfigProperty(name = "evcc.import.enabled", defaultValue = "false")
    boolean importEnabled;

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

            for (EvccSession evccSession : response) {
                if (evccSession.finished == null || evccSession.chargedEnergy <= 0) {
                    continue;
                }
                if (evccSession.pricePerKWh == null || evccSession.pricePerKWh <= 0) {
                    LOG.debugf("Skipping evcc session %d: no pricePerKWh", evccSession.id);
                    continue;
                }

                BigDecimal kwh = BigDecimal.valueOf(evccSession.chargedEnergy).setScale(2, RoundingMode.HALF_UP);
                BigDecimal pricePerKwh = BigDecimal.valueOf(evccSession.pricePerKWh).setScale(4, RoundingMode.HALF_UP);
                var finished = evccSession.finished.toLocalDateTime();

                boolean duplicate = existing.stream().anyMatch(s ->
                    s.timestamp.equals(finished) ||
                    (s.timestamp.toLocalDate().equals(finished.toLocalDate()) &&
                     s.kwhAmount.compareTo(kwh) == 0)
                );

                if (!duplicate) {
                    ChargeSession session = new ChargeSession(null, finished, kwh, pricePerKwh);
                    storage.save(session);
                    existing.add(session);
                    imported++;
                }
            }

            if (imported > 0) {
                LOG.infof("Imported %d new sessions from evcc", imported);
            } else {
                LOG.debug("evcc import: no new sessions found");
            }
        } catch (Exception e) {
            LOG.errorf("Failed to import sessions from evcc: %s", e.getMessage());
        }
    }
}
