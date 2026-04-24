#!/bin/sh
OPTIONS=/data/options.json

echo "[run.sh] Starting charge-tracker"
echo "[run.sh] Looking for options file: $OPTIONS"

if [ -f "$OPTIONS" ]; then
    echo "[run.sh] Found $OPTIONS, contents:"
    cat "$OPTIONS"
    echo ""

    DATA_FILE=$(jq -r '.data_file // "/share/charge-sessions.json"' "$OPTIONS")
    EVCC_URL=$(jq -r '.evcc_url // "http://localhost:7070"' "$OPTIONS")
    EVCC_ENABLED=$(jq -r '.evcc_import_enabled // "false"' "$OPTIONS")
    HA_URL=$(jq -r '.ha_url // "http://localhost:8123"' "$OPTIONS")
    HA_TOKEN=$(jq -r '.ha_token // ""' "$OPTIONS")
    HA_ENABLED=$(jq -r '.ha_enabled // "false"' "$OPTIONS")
    HA_ENTITY=$(jq -r '.ha_odometer_entity // "sensor.skoda_odometer"' "$OPTIONS")
    SOC_ENTITY=$(jq -r '.ha_soc_entity // "sensor.skoda_state_of_charge"' "$OPTIONS")
    BATTERY_CAPACITY=$(jq -r '.vehicle_battery_capacity_kwh // "77.0"' "$OPTIONS")
    CHARGE_TRACKER_DATA_FILE=$(jq -r '.charge_tracker_data_file // "/share/charge-sessions.json"' "$OPTIONS")

    echo "[run.sh] Resolved config:"
    echo "  DATA_FILE=$DATA_FILE"
    echo "  EVCC_URL=$EVCC_URL"
    echo "  EVCC_ENABLED=$EVCC_ENABLED"
    echo "  HA_URL=$HA_URL"
    echo "  HA_TOKEN=${HA_TOKEN:+<set>}${HA_TOKEN:-<empty>}"
    echo "  HA_ENABLED=$HA_ENABLED"
    echo "  HA_ENTITY=$HA_ENTITY"
    echo "  SOC_ENTITY=$SOC_ENTITY"
    echo "  BATTERY_CAPACITY=$BATTERY_CAPACITY"
    echo "  CHARGE_TRACKER_DATA_FILE=$CHARGE_TRACKER_DATA_FILE"
else
    echo "[run.sh] WARNING: $OPTIONS not found — using built-in defaults"
fi

exec java -jar /app/quarkus-run.jar \
    -Dquarkus.http.host=0.0.0.0 \
    -Dcharge.tracker.data-file="$DATA_FILE" \
    -Dquarkus.rest-client.evcc-api.url="$EVCC_URL" \
    -Devcc.import.enabled="$EVCC_ENABLED" \
    -Dquarkus.rest-client.ha-api.url="$HA_URL" \
    -Dha.token="$HA_TOKEN" \
    -Dha.enabled="$HA_ENABLED" \
    -Dha.odometer.entity="$HA_ENTITY" \
    -Dha.soc.entity="$SOC_ENTITY" \
    -Dvehicle.battery.capacity-kwh="$BATTERY_CAPACITY" \
    -Dcharge.tracker.data-file="$CHARGE_TRACKER_DATA_FILE"
