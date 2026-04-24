package com.evcharge.storage;

import com.evcharge.model.ChargeSession;
import com.evcharge.model.OdometerSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class JsonStorageService {

    private static final Logger LOG = Logger.getLogger(JsonStorageService.class);

    @ConfigProperty(name = "charge.tracker.data-file", defaultValue = "charge-sessions.json")
    String dataFile;
    
    private final ConcurrentHashMap<Long, ChargeSession> sessionCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, OdometerSnapshot> snapshotCache = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final ObjectMapper objectMapper;

    public JsonStorageService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        loadFromFile();
    }

    private void loadFromFile() {
        File file = new File(dataFile);
        if (file.exists()) {
            try {
                StorageWrapper wrapper = objectMapper.readValue(file, StorageWrapper.class);
                sessionCache.clear();
                wrapper.sessions.forEach(session -> sessionCache.put(session.sessionId, session));
                if (wrapper.odometerSnapshots != null) {
                    wrapper.odometerSnapshots.forEach(s -> snapshotCache.put(s.yearMonth, s));
                }
                
                long maxId = wrapper.sessions.stream()
                    .mapToLong(s -> s.sessionId)
                    .max()
                    .orElse(0L);
                idGenerator.set(maxId + 1);
                
                LOG.info("Loaded " + sessionCache.size() + " sessions from " + dataFile);
            } catch (IOException e) {
                LOG.error("Error loading data from file", e);
            }
        }
    }

    private void saveToFile() {
        try {
            File file = new File(dataFile);
            if (file.getParentFile() != null) file.getParentFile().mkdirs();
            StorageWrapper wrapper = new StorageWrapper();
            wrapper.sessions = new ArrayList<>(sessionCache.values());
            wrapper.odometerSnapshots = new ArrayList<>(snapshotCache.values());
            objectMapper.writeValue(file, wrapper);
            LOG.debug("Saved " + sessionCache.size() + " sessions to " + dataFile);
        } catch (IOException e) {
            LOG.error("Error saving data to file", e);
        }
    }

    public ChargeSession save(ChargeSession session) {
        if (session.sessionId == null) {
            session.sessionId = idGenerator.getAndIncrement();
        }
        sessionCache.put(session.sessionId, session);
        saveToFile();
        return session;
    }

    public List<ChargeSession> findAll() {
        return new ArrayList<>(sessionCache.values());
    }

    public ChargeSession findById(Long id) {
        return sessionCache.get(id);
    }

    public boolean deleteById(Long id) {
        boolean removed = sessionCache.remove(id) != null;
        if (removed) {
            saveToFile();
        }
        return removed;
    }

    public void saveSnapshot(OdometerSnapshot snapshot) {
        snapshotCache.put(snapshot.yearMonth, snapshot);
        saveToFile();
    }

    public Optional<OdometerSnapshot> findSnapshot(YearMonth yearMonth) {
        return Optional.ofNullable(snapshotCache.get(yearMonth.toString()));
    }

    public List<OdometerSnapshot> findAllSnapshots() {
        return new ArrayList<>(snapshotCache.values());
    }

    public static class StorageWrapper {
        public List<ChargeSession> sessions = new ArrayList<>();
        public List<OdometerSnapshot> odometerSnapshots = new ArrayList<>();
    }
}
