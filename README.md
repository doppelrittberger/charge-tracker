# EV Charge Tracker

A modern web application built with Quarkus to track electric vehicle charging sessions with comprehensive statistics and analytics.

## Features

- **REST API** for managing charge sessions
  - Create, read, and delete charge sessions
  - Get statistics (total kWh, average price, total cost)
  - Filter sessions by date range
  
- **Interactive Dashboard**
  - Real-time statistics cards
  - Visual charts (kWh per session, cost analysis)
  - Session filtering by date
  - Responsive Bootstrap UI

- **Data Tracking**
  - Timestamp (automatically set to current time)
  - kWh amount
  - Price per kWh
  - Calculated total price

## Technology Stack

- **Quarkus 3.8.1** - Supersonic Subatomic Java Framework
- **JSON File Storage** - Local file-based persistence with in-memory caching
- **Jackson** - JSON serialization/deserialization
- **Quarkus Qute** - Server-side templating
- **Bootstrap 5.3** - Modern responsive UI
- **Chart.js 4.4** - Interactive charts
- **RESTEasy Reactive** - High-performance REST endpoints

## Data Storage

The application uses a **local JSON file** (`charge-sessions.json`) for data persistence:
- Data is loaded into memory on startup for fast access
- All changes (create/delete) are immediately persisted to the JSON file
- No database required - simple, portable, and easy to backup
- Sample data is automatically created on first run

## Getting Started

### Prerequisites

- Java 17 or later
- Maven 3.8+

### Running the Application

1. Navigate to the project directory:
```bash
cd charge-tracker
```

2. Run in development mode:
```bash
./mvnw quarkus:dev
```

3. Open your browser and navigate to:
```
http://localhost:8080
```

## REST API Endpoints

### Create a Charge Session
```bash
POST /api/sessions
Content-Type: application/json

{
  "kwhAmount": 45.5,
  "pricePerKwh": 0.35
}
```

### Get All Sessions
```bash
GET /api/sessions
```

### Get Sessions by Date Range
```bash
GET /api/sessions?start=2024-01-01T00:00:00&end=2024-12-31T23:59:59
```

### Get Session by ID
```bash
GET /api/sessions/{id}
```

### Delete a Session
```bash
DELETE /api/sessions/{id}
```

### Get Statistics
```bash
GET /api/sessions/statistics
```

Response:
```json
{
  "totalKwh": 150.5,
  "averagePricePerKwh": 0.35,
  "totalPrice": 52.68,
  "sessionCount": 5
}
```

## Building for Production

### Package the application
```bash
./mvnw package
```

### Run the packaged application
```bash
java -jar target/quarkus-app/quarkus-run.jar
```

### Create a native executable (requires GraalVM)
```bash
./mvnw package -Pnative
```

## Project Structure

```
charge-tracker/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/evcharge/
│   │   │       ├── dto/              # Data Transfer Objects
│   │   │       ├── model/            # Domain Models
│   │   │       ├── resource/         # REST API Resources
│   │   │       ├── service/          # Business Logic
│   │   │       ├── storage/          # JSON Storage Service
│   │   │       └── web/              # Web Controllers
│   │   └── resources/
│   │       ├── templates/            # Qute Templates
│   │       └── application.properties
│   └── test/
├── charge-sessions.json              # Data file (created on first run)
└── pom.xml
```

## Development

The application uses Quarkus Dev Mode which provides:
- Live reload
- Dev UI at http://localhost:8080/q/dev
- Continuous testing

### Data File

The `charge-sessions.json` file is created in the project root directory and contains all charge session data in JSON format. You can:
- Back it up by copying the file
- Reset data by deleting the file (sample data will be recreated on next startup)
- Manually edit it if needed (changes will be loaded on next startup)

## License

This project is open source and available under the MIT License.
