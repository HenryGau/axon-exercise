# Rate Limiter Design

## Stack
- **Framework:** Quarkus
- **Storage:** In-memory (ConcurrentLinkedDeque per client/window)
- **Config:** JSON file (`rate-limits.json`) with hot reload
- **Tests:** Unit tests + in-memory Quarkus integration tests (`@QuarkusTest`)

## Architecture

```
HTTP Request
    │
    ▼
RateLimitFilter (globally applied, ContainerRequestFilter)
    │
    ├── Client in adminClientIds? ──→ ALLOW (skip rate limiting)
    │
    ▼
RateLimiterService (thread-safe core logic)
    │
    ▼
SlidingWindowLog (in-memory Deque<Instant> per clientId + window)
```

## Sliding Window Algorithm

For each `(clientId, windowDuration)` pair:
1. Prune timestamps older than `now - window` from deque front
2. Count remaining — if `>= maxRequests`, reject (429)
3. Otherwise append `now` to deque tail and allow

## API Behavior

| Aspect | Decision |
|---|---|
| Filter scope | Global (all paths) |
| Client ID resolution | `X-Client-ID` header → `clientId` query param → requestor IP |
| 429 response | `{ "message": "exceed <max> request for last <window>" }` |

## Admin Bypass

Client IDs listed in `adminClientIds` skip rate limiting entirely. The filter checks this list before consulting `SlidingWindowLog` — if the client is an admin, the request passes through immediately.

## Config (`rate-limits.json`)

```json
{
  "adminClientIds": ["admin-1", "admin-2"],
  "default": [
    { "window": "1m", "maxRequests": 100 },
    { "window": "10m", "maxRequests": 500 },
    { "window": "1h", "maxRequests": 1000 },
    { "window": "1d", "maxRequests": 5000 }
  ],
  "clients": {
    "client-A": [
      { "window": "1m", "maxRequests": 200 }
    ]
  }
}
```

Hot reload via Quarkus file watch — config is re-read on file change without restart.

## Thread Safety

- `ConcurrentHashMap` for top-level maps
- Synchronized blocks on individual deque objects for prune+check+append operations

## Project Structure

```
src/main/java/com/example/ratelimit/
├── config/
│   ├── RateLimitConfig.java
│   └── RateLimitRule.java
├── service/
│   ├── RateLimiterService.java
│   └── SlidingWindowLog.java
├── filter/
│   └── RateLimitFilter.java
└── dto/
    └── RateLimitError.java

src/test/java/com/example/ratelimit/
├── service/
│   └── RateLimiterServiceTest.java
└── filter/
    └── RateLimitFilterTest.java
```
