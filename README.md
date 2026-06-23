# Axon Rate Limiter

A Quarkus-based rate limiting middleware for HTTP API services. Uses a **sliding window log** algorithm (per-client, per-window) with configurable thresholds and admin bypass.

## Features

- **Sliding window** — exact rolling window, not fixed; prunes expired timestamps on each request
- **Variable windows** — `30s`, `1m`, `10m`, `1h`, `1d` (configurable per rule)
- **Per-client rules** — default rules applied unless overridden per client ID
- **Admin bypass** — client IDs in `adminClientIds` skip rate limiting entirely
- **Hot-reload config** — `rate-limits.json` is re-read on file change (no restart)
- **Thread-safe** — `ConcurrentHashMap` + synchronized per-deque operations
- **Tool-agnostic** — client ID resolved from `X-Client-ID` header, `clientId` query param, or requestor IP

## Quick Start

```bash
mvn quarkus:dev
```

The service starts on `http://localhost:8080`.

## Usage

```bash
# Normal request
curl -H "X-Client-ID: user-1" http://localhost:8080/ping

# Admin bypass (no rate limiting)
curl -H "X-Client-ID: admin-1" http://localhost:8080/ping

# Rate limited (after exceeding threshold)
curl -H "X-Client-ID: heavy-user" http://localhost:8080/ping
# → 429 Too Many Requests
# {"message":"exceed 100 requests for last 1m"}
```

## Configuration

Edit `src/main/resources/rate-limits.json` (or a copy at the project root for hot-reload):

```json
{
  "adminClientIds": ["admin-1", "admin-2"],
  "default": [
    { "window": "1m", "maxRequests": 100 },
    { "window": "1h", "maxRequests": 1000 }
  ],
  "clients": {
    "client-A": [
      { "window": "1m", "maxRequests": 200 }
    ]
  }
}
```

| Field | Description |
|---|---|
| `adminClientIds` | Client IDs that skip rate limiting |
| `default` | Fallback rules for unmatched clients |
| `clients` | Per-client override rules (merged by client ID) |

Window suffix: `s` (seconds), `m` (minutes), `h` (hours), `d` (days).

## Project Structure

```
src/main/java/com/example/ratelimit/
├── config/
│   ├── ConfigLoader.java        # JSON loader with hot-reload
│   ├── RateLimitConfig.java     # Config model
│   └── RateLimitRule.java       # Rule model + window parser
├── dto/
│   └── RateLimitError.java      # 429 response body
├── filter/
│   └── RateLimitFilter.java     # Global JAX-RS filter
├── service/
│   ├── RateLimiterService.java  # Orchestrates rate check
│   └── SlidingWindowLog.java    # In-memory sliding window store
└── PingResource.java            # Test endpoint
```

## Tests

```bash
mvn test
```

- **Unit tests** — core sliding window logic, rate limit enforcement, admin bypass
- **Integration tests** — `@QuarkusTest` with REST-assured, full HTTP round-trips

## Stack

- Java 21, Quarkus 3.14, Maven
- In-memory storage (single-node)
- RESTEasy Reactive + Jackson
