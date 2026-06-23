# Session Log

## 1 — Propose high-level design

**User:** look at the requriement.md and propose a high level design

**AI:** Read requirement.md (rate limiter: Java, rolling window, 1m/10m/1h/1d, per-client, 429 response). Asked 4 clarifying questions: framework (Spring Boot/Micronaut/Quarkus/plain Java), storage backend (In-memory/Redis/both), configuration approach (static config/dynamic API), and whether to include tests.

---

## 2 — Answered clarifying questions

**User:** [Quarkus, In-memory, JSON file for config, Unit + integration tests]

**AI:** Proposed full high-level design with architecture diagram (RateLimitFilter → RateLimiterService → SlidingWindowLog), data model (RateLimitRule, RateLimitConfig), rolling window algorithm, 429 response format, client ID resolution priority (header → query param → IP), thread safety approach, and project structure. Asked 3 open points: filter scope, hot reload approach, and integration test methodology.

---

## 3 — Filter scope global

**User:** 1 filter globally

**AI:** Asked remaining 2 questions (hot reload vs startup-only, QuarkusTest with TestHTTPEndpoint for integration).

---

## 4 — Write design decisions

**User:** 1 filter globally, 2 hot reload, 3, in memory. Now write these design decsion to design.md file

**AI:** Wrote design.md with full architecture, algorithm, config format, thread safety, and project structure.

---

## 5 — Admin bypass

**User:** I want support configurable bypass for admin. In config.json file, we have adminClientIds: [clientIds]. For those admins, we don't do rate limiting. Update the design

**AI:** Updated design.md — added `adminClientIds` field to config JSON, updated architecture diagram to show admin short-circuit in filter, and added Admin Bypass section.

---

## 6 — Build it

**User:** build it

**AI:** Checked environment (Java 21, Maven 3.9.9, no Quarkus CLI), scaffolded Quarkus project with pom.xml, wrote all 8 source files (RateLimitRule, RateLimitConfig, ConfigLoader, SlidingWindowLog, RateLimiterService, RateLimitFilter, RateLimitError, PingResource), config files (application.properties, rate-limits.json), and 2 test files. Compiled and ran tests: 5/5 integration passed, 4/5 unit passed. Fixed missing `s` suffix support in parseDuration(). All 10 tests passed.

---

## 7 — Create README

**User:** create a README.md file for this project

**AI:** Created README.md covering features, quick start, usage examples, configuration reference, project structure, and test commands.

---

## 8 — Duplicate data in deques

**User:** I noticed "Map<Duration, Deque<Instant>> clientWindows" will contains duplicate data, for example 1min 10min 1h 1d will all contains timestamps of 1min. Is there a better / more efficient algorithm?

**AI:** Analyzed the problem — N deques per client with duplicated timestamps, N prunes + N traversals per request, plus a partial consumption bug. Proposed 3 alternative approaches: (1) single deque + one-pass scan across all rules, (2) two-phase check-then-record (fixes partial consumption but has TOCTOU race), (3) windowed bucketed counters (overengineered). Recommended approach 1.

---

## 9 — Pros/cons analysis

**User:** what's pros and cons of the new approach vs old? is there any other alernative?

**AI:** Detailed table comparing 4 approaches:

| Approach | Pros | Cons |
|---|---|---|
| Current (per-window deques, append on check) | Simple, pure data structure, small per-window deques | N duplicates, N traversals, partial consumption bug |
| Single deque + one-pass scan | No duplicates, 1 prune + 1 scan, atomic | SlidingWindowLog knows about rules |
| Two-phase check-then-record | Clean separation, fixes partial consumption | Still N duplicates + N traversals, TOCTOU race |
| Bucketed counters | O(1) per request | Overengineered for this use case |

---

## 10 — Commit and push

**User:** create commit and push to git@github.com:HenryGau/axon-exercise.git

**AI:** Configured git remote, created .gitignore, staged all project files, committed with descriptive message, pushed to GitHub.

---

## 11 — Test readability and coverage

**User:** can we make the test a bit easier to read? also what cases that we covered and what cases that we want to add?

**AI:** Read both test files. Refactored with helper methods (assertAllowed, assertDenied, assertDeniedOnNth, buildService, rule, config), added @DisplayName, removed @Nested (instance management issues). Fixed bug: `assertDeniedOnNth` was being called after `assertAllowed`, causing double counting. Listed covered (9 scenarios) and uncovered (4 scenarios: concurrent access, missing X-Forwarded-For, stale config after hot-reload, malformed JSON).

---

## 12 — X-Forwarded-For fallback explained

**User:** what is " X-Forwarded-For fallback" ?

**AI:** Explained it's the IP fallback in RateLimitFilter.resolveClientId() — when no X-Client-ID header or clientId query param is present, the filter falls back to the X-Forwarded-For header set by proxies/load balancers, rate-limiting anonymous requests by source IP.

---

## 13 — Add missing tests

**User:** Concurrent access — multiple threads hitting the same client simultaneously, Missing X-Forwarded-For fallback — filter resolves by IP but no integration test exercises it, Config hot-reload — file change detection not tested => let's add these tests

**AI:** Added 3 new test areas:
1. Concurrent access (RateLimiterServiceTest): 20 threads × 10 requests via CountDownLatch, verifies exactly `limit` allowed
2. X-Forwarded-For (RateLimitFilterTest): exhausts limit via header only, verifies 4th request gets 429
3. ConfigLoader hot-reload (ConfigLoaderTest): temp JSON file, verify initial load, reload on modification, cached fallback on deletion
Refactored ConfigLoader to accept path as constructor parameter for test isolation. All 23 tests passed, committed and pushed.

---

## 14 — Save prompts to file

**User:** I want all the prompts I typed here for the entire session to save to prompts.md file

**AI:** Created prompts.md with user prompts only.

---

## 15 — Add AI responses

**User:** can you add the response from AI after every input prompt to that file too?

**AI:** Updated prompts.md to include the full session log with both user prompts and AI responses summarized for each turn.
