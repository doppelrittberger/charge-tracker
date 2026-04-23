# Quick Start Guide

## Start the Application

```bash
./mvnw quarkus:dev
```

The application will start on `http://localhost:8080`

## What You'll See

### Dashboard (http://localhost:8080)
- **Statistics Cards**: Total sessions, kWh charged, average price, total cost
- **Add Session Form**: Enter kWh amount and price per kWh
- **Charts**: Visual representation of kWh and costs per session
- **Sessions Table**: All charge sessions with filtering capabilities

### Sample Data
The application comes pre-loaded with 5 sample charge sessions to demonstrate functionality. Data is stored in `charge-sessions.json` in the project root directory.

## REST API Examples

### Add a new charge session
```bash
curl -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -d '{"kwhAmount": 50.5, "pricePerKwh": 0.40}'
```

### Get all sessions
```bash
curl http://localhost:8080/api/sessions
```

### Get statistics
```bash
curl http://localhost:8080/api/sessions/statistics
```

### Delete a session
```bash
curl -X DELETE http://localhost:8080/api/sessions/1
```

## Features to Try

1. **Add Sessions**: Use the form to add new charge sessions
2. **View Charts**: See how your charging patterns look visually
3. **Filter Sessions**: Use the date range filter to view specific periods
4. **Delete Sessions**: Remove unwanted entries
5. **Statistics**: Monitor your total consumption and costs

## Development Mode Features

- **Live Reload**: Changes to code are automatically reflected
- **Dev UI**: Access at http://localhost:8080/q/dev for debugging tools
- **JSON Storage**: Data persisted to `charge-sessions.json` file
- **Easy Backup**: Simply copy the JSON file to backup your data

## Stopping the Application

Press `Ctrl+C` in the terminal where the application is running.
