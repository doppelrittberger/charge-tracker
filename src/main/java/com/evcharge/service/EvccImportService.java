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
import java.time.YearMonth;
import java.util.ArrayList;
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

    @Scheduled(every = "5m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void importSessions() {
        if (!importEnabled) {
            return;
        }
        try {
            List<ChargeSession> existing = storage.findAll();

            boolean hasEvccData = existing.stream().anyMatch(s -> s.evccId != null);
            List<EvccSession> response = new ArrayList<>();

            if (!hasEvccData) {
                LOG.info("evcc import: no existing evcc data, fetching all sessions");
                List<EvccSession> all = evccApiClient.getAllSessions();
                if (all != null) response.addAll(all);
            } else {
                YearMonth currentMonth = YearMonth.now();
                YearMonth fromMonth = existing.stream()
                    .filter(s -> s.evccId != null && s.timestamp != null)
                    .map(s -> YearMonth.from(s.timestamp))
                    .min(YearMonth::compareTo)
                    .orElse(currentMonth);

                for (YearMonth ym = fromMonth; !ym.isAfter(currentMonth); ym = ym.plusMonths(1)) {
                    List<EvccSession> batch = evccApiClient.getSessions(ym.getMonthValue(), ym.getYear());
                    if (batch != null) {
                        LOG.debugf("evcc import: fetched %d sessions for %s", batch.size(), ym);
                        response.addAll(batch);
                    }
                }
            }

            if (response.isEmpty()) {
                LOG.debug("evcc returned no sessions");
                return;
            }

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
                    LOG.infof("evcc import: new session evccId=%d created=%s kwh=%s price=%s odometer=%s socStart=%d",
                        evccSession.id, created, kwh, pricePerKwh, session.odometerKm, session.socStart);
                    imported++;
                } else {
                    boolean changed = false;
                    if (match.evccId == null) {
                        match.evccId = evccSession.id;
                        changed = true;
                    }
                    if (match.odometerKm == null) {
                        Long resolved = resolveOdometer(evccSession);
                        if (resolved != null) {
                            match.odometerKm = resolved;
                            LOG.infof("evcc enrich: evccId=%d odometer=%d km", evccSession.id, resolved);
                            changed = true;
                        }
                    }
                    if (match.socStart == null) {
                        match.socStart = haHistoryService.getSocAt(evccSession.created).orElse(0);
                        LOG.infof("evcc enrich: evccId=%d socStart=%d%%", evccSession.id, match.socStart);
                        changed = true;
                    }
                    if (changed) {
                        storage.save(match);
                        enriched++;
                    } else {
                        LOG.debugf("evcc import: evccId=%d already up-to-date", evccSession.id);
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
