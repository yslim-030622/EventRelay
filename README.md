# EventRelay

A production-grade webhook ingestion platform that verifies HMAC signatures, deduplicates deliveries, routes events through RabbitMQ, and exposes a React monitoring dashboard.

---

## Architecture

```
External Service
      │
      ▼
POST /api/webhooks/{source}
      │
      ▼
┌─────────────────┐
│ HMAC Signature  │  ← SignatureVerifier (GitHub sha256, Stripe v1)
│   Verification  │
└────────┬────────┘
         │ valid
         ▼
┌─────────────────┐
│  Deduplication  │  ← Redis (delivery UUID as key, 24h TTL)
│     Check       │
└────────┬────────┘
         │ not duplicate
         ▼
┌─────────────────┐
│  Persist Event  │  ← PostgreSQL (IncomingEvent, PROCESSING)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│    RabbitMQ     │  ← Topic Exchange, routing key = event type
│  Topic Exchange │
└────────┬────────┘
         │
    ┌────┴─────┐
    ▼          ▼          ▼
Payment    GitHub    Generic
Consumer  Consumer  Consumer
    │          │          │
    └──────────┴──────────┘
              │
    Resilience4j: @Retry + @CircuitBreaker
              │
      PROCESSED or DEAD_LETTER
```

---

## Key Features

- **HMAC Signature Verification** — validates GitHub `X-Hub-Signature-256` and Stripe `Stripe-Signature` headers; rejects invalid requests with 401
- **Idempotent Deduplication** — stores delivery UUIDs in Redis (24 h TTL) to silently drop replayed webhooks before they reach the database
- **Type-Based Routing** — RabbitMQ TopicExchange dispatches events to `PaymentEventConsumer`, `GitHubEventConsumer`, or `GenericEventConsumer` based on routing key
- **Circuit Breaker + Retry** — Resilience4j `@CircuitBreaker` and `@Retry` on every consumer; configurable thresholds per consumer type
- **Dead-Letter Queue + Replay** — events that exhaust retries are persisted as dead letters; one-click replay re-publishes to RabbitMQ and resets state
- **Monitoring Dashboard** — React SPA with live event feed, metrics summary, per-source/per-type breakdowns, circuit breaker state cards, and dead-letter management
- **Discord Alerts** — `DiscordAlertService` posts an embed to a configured webhook URL whenever an event transitions to dead-letter state
- **Multi-Source Support** — webhook sources are registered at runtime via REST; each carries its own signing secret and display name

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend framework | Spring Boot 3.x + Spring Data JPA |
| Database | PostgreSQL 16 |
| Cache / dedup state | Redis 7 |
| Message broker | RabbitMQ 3 (Spring AMQP) |
| Resilience | Resilience4j (circuit breaker, retry) |
| Frontend | React 18 + TypeScript + Tailwind CSS |
| Build | Gradle 8 (backend), Vite (frontend) |
| Containers | Docker Compose (local), Azure Container Apps (prod) |
| CI | GitHub Actions |

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/health` | Liveness check — returns `{"status":"ok"}` |
| `GET` | `/api/sources` | List all registered webhook sources |
| `POST` | `/api/sources` | Register a new webhook source |
| `POST` | `/api/webhooks/{source}` | Receive a signed webhook for the named source |
| `GET` | `/api/events` | Paginated event list; filterable by `status` and `source` |
| `GET` | `/api/events/{id}` | Event detail with full payload and delivery history |
| `POST` | `/api/events/{id}/replay` | Re-publish a processed event to RabbitMQ |
| `GET` | `/api/metrics/summary` | Aggregate counts: total, processed, failed, dead-letter |
| `GET` | `/api/metrics/by-source` | Event counts grouped by source name |
| `GET` | `/api/metrics/by-type` | Event counts grouped by event type |
| `GET` | `/api/metrics/circuit-breaker` | Live state for all Resilience4j circuit breakers |
| `GET` | `/api/dead-letters` | List all dead-letter events |
| `POST` | `/api/dead-letters/{id}/replay` | Mark replayed and re-publish dead-letter event |

---

## Design Decisions

**Why an `EventStatus` enum instead of a boolean `processed` flag?**
Webhooks move through multiple states (`PROCESSING` → `PROCESSED` / `FAILED` / `DEAD_LETTER`). A boolean would collapse `FAILED` and `DEAD_LETTER` into the same bucket, making retry logic ambiguous and preventing the dashboard from distinguishing transient failures from exhausted retries.

**Why extract a dedicated service layer instead of putting logic in controllers?**
Controllers are thin HTTP adapters. Business rules — signature verification, deduplication, routing, retry thresholds — change independently of HTTP concerns. A service layer keeps each class testable in isolation with plain Mockito mocks and avoids coupling HTTP status codes to routing logic.

**Why a RabbitMQ TopicExchange instead of a DirectExchange?**
A `TopicExchange` with pattern-matched routing keys (e.g. `payment.#`, `github.push`) lets a single published message fan out to multiple consumers without code changes. Adding a new event type is a configuration change, not a code change. `DirectExchange` would require an exact key match per consumer, making wildcard subscriptions impossible.

**Why Redis for deduplication instead of a database unique constraint?**
A database unique constraint requires a round-trip write on every request to detect duplicates. Redis `SET NX` is a single in-memory atomic operation that returns in under 1 ms and auto-expires keys after 24 hours, keeping the dedup store self-cleaning without a background job.

---

## Local Development

### Prerequisites

- Docker and Docker Compose

### Start everything

```bash
docker compose up --build
```

This starts PostgreSQL, Redis, RabbitMQ, the Spring Boot backend, and the React frontend behind nginx.

| Service | URL |
|---|---|
| Dashboard | http://localhost:3000 |
| Backend API | http://localhost:8080/api |
| RabbitMQ UI | http://localhost:15672 (guest / guest) |

### Register a source and send a test webhook

```bash
# Register a source
curl -X POST http://localhost:8080/api/sources \
  -H "Content-Type: application/json" \
  -d '{"name":"github","displayName":"GitHub","signingSecret":"test-secret-123"}'

# Send a signed webhook
chmod +x scripts/simulate_webhooks.sh
./scripts/simulate_webhooks.sh
```

---

## Running Tests

```bash
cd backend && ./gradlew test
```

Tests use Spring profile `test` with RabbitMQ auto-startup disabled and Redis autoconfiguration excluded, so no external services are required.
