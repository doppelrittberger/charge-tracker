# JSON Storage Architecture

## Overview

The application uses a **file-based JSON storage system** with in-memory caching for optimal performance and simplicity.

## How It Works

### Storage Service (`JsonStorageService`)

Located at: `src/main/java/com/evcharge/storage/JsonStorageService.java`

**Key Features:**
- **In-Memory Cache**: All data is kept in a `ConcurrentHashMap` for fast access
- **Automatic Persistence**: Every create/delete operation immediately saves to disk
- **Thread-Safe**: Uses concurrent data structures for safe multi-threaded access
- **Auto-Increment IDs**: Maintains an `AtomicLong` for generating unique session IDs

### Data File

**Location**: `charge-sessions.json` (project root directory)

**Format**:
```json
{
  "sessions": [
    {
      "id": 1,
      "timestamp": "2024-04-16T10:30:00",
      "kwhAmount": 45.5,
      "pricePerKwh": 0.35
    },
    ...
  ]
}
```

### Lifecycle

1. **Startup** (`@Observes StartupEvent`):
   - Loads existing data from `charge-sessions.json`
   - Populates in-memory cache
   - If file doesn't exist or is empty, creates sample data

2. **Runtime**:
   - All read operations use the in-memory cache (fast)
   - All write operations update cache AND persist to file (durable)

3. **Shutdown**:
   - Data is already persisted, no special cleanup needed

## Benefits

✅ **No Database Required**: Simple deployment, no DB setup  
✅ **Fast Performance**: In-memory cache for reads  
✅ **Durable**: Immediate persistence on writes  
✅ **Portable**: Just copy the JSON file to backup/migrate  
✅ **Human-Readable**: Easy to inspect and edit manually  
✅ **Version Control Friendly**: Can track data changes in git (if desired)  
✅ **Thread-Safe**: Concurrent access handled properly  

## Limitations

⚠️ **Single Instance**: Not suitable for multi-instance deployments  
⚠️ **File I/O**: Write operations involve disk I/O (though async could be added)  
⚠️ **Memory**: All data must fit in memory  
⚠️ **No Transactions**: No ACID guarantees across multiple operations  

## When to Use

**Good For:**
- Personal applications
- Small to medium datasets (thousands of records)
- Single-user or low-concurrency scenarios
- Development and prototyping
- Applications requiring easy data portability

**Not Good For:**
- High-concurrency applications
- Large datasets (millions of records)
- Multi-instance deployments
- Applications requiring complex queries or transactions

## Migration Path

If you need to scale to a database later:
1. Keep the service layer interface the same
2. Create a new implementation using JPA/Hibernate
3. Write a migration script to import from JSON to DB
4. Swap implementations via dependency injection

The current architecture makes this transition straightforward!
