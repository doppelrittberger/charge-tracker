# Charge Tracker Add-on

Track and visualize your EV charging sessions from Home Assistant.

## Installation

1. Go to **Settings → Add-ons → Add-on Store**
2. Click **⋮ → Repositories**
3. Add: `https://github.com/doppelrittberger/charge-tracker`
4. Find **Charge Tracker** in the store and install it

## Usage

After starting the add-on, open the Web UI on port `8080`.

Session data is stored at `/share/charge-sessions.json` and persists across updates.

## Configuration

No configuration required. The data file path can be overridden via the `CHARGE_TRACKER_DATA_FILE` environment variable.
