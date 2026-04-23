#!/bin/sh
OPTIONS=/data/options.json

if [ -f "$OPTIONS" ]; then
    DATA_FILE=$(jq -r '.data_file // "/share/charge-sessions.json"' "$OPTIONS")
    EVCC_URL=$(jq -r '.evcc_url // "http://localhost:7070"' "$OPTIONS")
    EVCC_ENABLED=$(jq -r '.evcc_import_enabled // "false"' "$OPTIONS")
    HA_URL=$(jq -r '.ha_url // "http://localhost:8123"' "$OPTIONS")
    HA_TOKEN=$(jq -r '.ha_token // ""' "$OPTIONS")
    HA_ENTITY=$(jq -r '.ha_odometer_entity // "sensor.skoda_odometer"' "$OPTIONS")
    HA_ENABLED=$(jq -r '.ha_odometer_enabled // "false"' "$OPTIONS")
    SOC_ENTITY=$(jq -r '.ha_soc_entity // "sensor.skoda_state_of_charge"' "$OPTIONS")
    BATTERY_CAPACITY=$(jq -r '.vehicle_battery_capacity_kwh // "77.0"' "$OPTIONS")
fi

exec java -jar /app/quarkus-run.jar \
    -Dquarkus.http.host=0.0.0.0 \
    -Dcharge.tracker.data-file="$DATA_FILE" \
    -Dquarkus.rest-client.evcc-api.url="$EVCC_URL" \
    -Devcc.import.enabled="$EVCC_ENABLED" \
    -Dquarkus.rest-client.ha-api.url="$HA_URL" \
    -Dha.odometer.token="$HA_TOKEN" \
    -Dha.odometer.entity="$HA_ENTITY" \
    -Dha.odometer.enabled="$HA_ENABLED" \
    -Dha.soc.entity="$SOC_ENTITY" \
    -Dvehicle.battery.capacity-kwh="$BATTERY_CAPACITY"
