# EventRelay

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3-FF6600?logo=rabbitmq&logoColor=white)
![Resilience4j](https://img.shields.io/badge/Resilience4j-circuit_breaker-5C6BC0)
![React](https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=black)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-CI/CD-2088FF?logo=githubactions&logoColor=white)

I built EventRelay to understand what production webhook infrastructure actually looks like from the inside. Most tutorials show you how to receive a webhook — they skip signature verification, deduplication, retry policy, dead-letter handling, and operational visibility. This project builds the full pipeline: from raw HTTP to a Resilience4j-protected consumer, persisted in PostgreSQL and tracked in real time on a React dashboard.

## Architecture Overview

```
                              EventRelay Ingestion Pipeline
  ─────────────────────────────────────────────────────────────────────────
  External        Ingestion             Broker              Consumers
  Service         Layer                 Layer               Layer
  ─────────────────────────────────────────────────────────────────────────

  GitHub  ──┐
  Stripe  ──┼──▶  POST /api/webhooks/{source}
  Other   ──┘              │
                           │
                    ┌──────▼───────┐
                    │ Verify HMAC  │  GitHub: X-Hub-Signature-256
                    │  Signature   │  Stripe: Stripe-Signature v1
                    └──────┬───────┘
                     401 ◀─┤ valid
                           │
                    ┌──────▼───────┐
                    │  Dedup Check │  Redis SET NX — 24 h TTL
                    │   (Redis)    │  keyed on delivery UUID
                    └──────┬───────┘
                    skip ◀─┤ new
                           │
                    ┌──────▼───────┐
                    │   Persist    │  PostgreSQL — status: PROCESSING
                    │  (Postgres)  │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │   RabbitMQ   │  TopicExchange
                    │Topic Exchange│  routing key = event type
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
         Payment        GitHub       Generic
         Consumer       Consumer     Consumer
              │            │            │
              └────────────┼────────────┘
                           │
                  Resilience4j: @Retry + @CircuitBreaker
                           │
                  PROCESSED │ DEAD_LETTER ──▶ Discord Alert
```

## Request Flow

```
  Client                 Backend                 Infrastructure
  ──────                 ───────                 ──────────────

  POST /webhooks/{src} ──▶ WebhookController
                              │
                         WebhookIngestionService
                              │
                    ┌─────────┼──────────┐
                    ▼         ▼          ▼
              SignatureVerifier  DeduplicationService  EventRoutingService
                    │         │          │
                    └─────────┼──────────┘
                              │
                         Save event ──────────────────▶ PostgreSQL
                              │
                         Publish msg ─────────────────▶ RabbitMQ
                              │
                         202 Accepted
                              │
                    (async) Consumer picks up
                              │
                    ┌─────────┴─────────┐
                  success            failure (retry)
                    │                   │
              PROCESSED          retryCount++ ──▶ maxRetries?
                                        │               │
                                   rethrow          DEAD_LETTER
                                  (Retry/CB)             │
                                                  Discord embed
```

## Tech Stack

| Layer | Technology | Why |
|-------|-----------|-----|
| Backend | Spring Boot 3.x + Spring Data JPA | Convention-over-configuration with production-ready auto-configuration |
| Database | PostgreSQL 16 | Strong transactional guarantees; JSONB for raw payload storage |
| Cache / Dedup | Redis 7 | Atomic `SET NX` dedup in under 1 ms; keys self-expire with 24 h TTL |
| Message Broker | RabbitMQ 3 (Spring AMQP) | TopicExchange enables wildcard routing — new event types need zero code changes |
| Resilience | Resilience4j | Per-consumer circuit breaker + retry thresholds; state visible in dashboard |
| Frontend | React 18 + TypeScript + Tailwind CSS | Type-safe SPA with real-time auto-refresh for event and metrics views |
| Build | Gradle 8 (backend), Vite (frontend) | Incremental builds on backend; near-instant HMR on frontend |
| Containers | Docker Compose (local), Azure Container Apps (prod) | Single command local stack; managed scaling without Kubernetes overhead |
| CI/CD | GitHub Actions | Automated test + build gate on every push |

## Data Model

**5 types** across **2 domains**

- **Sources & Events**: `WebhookSource`, `IncomingEvent` (with `EventStatus` enum: `PROCESSING` → `PROCESSED` / `FAILED` / `DEAD_LETTER`), `EventDelivery`
- **Dead Letters**: `DeadLetterEvent` — persists exhausted events with full error context; replayable via API

```
WebhookSource          IncomingEvent              EventDelivery
─────────────          ─────────────              ─────────────
id (PK)       ◀──┐    id (PK)                    id (PK)
name               └── sourceId (FK)              eventId (FK) ──▶ IncomingEvent
displayName         deliveryId (unique)            attemptNumber
signingSecret       eventType                      status
active              status (enum)                  errorMessage
                    payload (JSONB)                processedAt
                    retryCount
                    maxRetries          DeadLetterEvent
                    createdAt           ───────────────
                                        id (PK)
                                        eventId (FK)
                                        errorMessage
                                        replayed
                                        replayedAt
```

## API Highlights

**13 endpoints** across **6 routers**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/health` | Liveness check |
| `GET` | `/api/sources` | List registered webhook sources |
| `POST` | `/api/sources` | Register a new source with signing secret |
| `POST` | `/api/webhooks/{source}` | Receive a signed webhook — verify, dedup, store, publish |
| `GET` | `/api/events` | Paginated event list; filterable by `status` and `source` |
| `GET` | `/api/events/{id}` | Event detail with full payload |
| `POST` | `/api/events/{id}/replay` | Re-publish event to RabbitMQ |
| `GET` | `/api/metrics/summary` | Totals: received, processed, failed, dead-letter |
| `GET` | `/api/metrics/by-source` | Event counts grouped by source |
| `GET` | `/api/metrics/by-type` | Event counts grouped by event type |
| `GET` | `/api/metrics/circuit-breaker` | Live Resilience4j state for all consumers |
| `GET` | `/api/dead-letters` | List all dead-letter events |
| `POST` | `/api/dead-letters/{id}/replay` | Mark replayed and re-publish to RabbitMQ |

## Design Decisions

**Why `EventStatus` enum instead of a boolean `processed` flag?**
Webhooks move through multiple states (`PROCESSING` → `PROCESSED` / `FAILED` / `DEAD_LETTER`). A boolean collapses `FAILED` and `DEAD_LETTER` into the same bucket, making retry logic ambiguous and preventing the dashboard from distinguishing transient failures from exhausted events.

**Why a RabbitMQ TopicExchange instead of DirectExchange?**
A `TopicExchange` with pattern-matched routing keys (`payment.#`, `github.push`) lets consumers subscribe to wildcard patterns without code changes. Adding a new event type is a configuration change. `DirectExchange` requires an exact key per consumer — no wildcard subscriptions.

**Why Redis for deduplication instead of a database unique constraint?**
A database unique constraint requires a write on every request to detect duplicates. Redis `SET NX` is a single in-memory atomic operation returning in under 1 ms, and keys auto-expire after 24 hours — no background cleanup job needed.

**Why extract a dedicated service layer instead of putting logic in controllers?**
Controllers are thin HTTP adapters. Signature verification, deduplication, routing, and retry thresholds change independently of HTTP concerns. A service layer keeps each class testable in isolation with plain Mockito mocks and avoids coupling HTTP semantics to routing logic.

## Testing

**20 tests** across **4 files**. CI enforces `BUILD SUCCESSFUL` on every push.

| File | Tests | What It Covers |
|------|------:|----------------|
| `SignatureVerifierTest` | 12 | GitHub null/prefix/wrong-secret, Stripe valid/expired/empty/null, unknown source passthrough |
| `EventRoutingServiceTest` | 4 | Routing key dispatch, `maxRetries` per source (payment=10, github=3, generic=5) |
| `DeduplicationServiceTest` | 3 | New UUID accepted, duplicate UUID rejected, null key handled |
| `DeadLetterServiceTest` | 1 | `markReplayed()` resets state and re-publishes to RabbitMQ |

Testing practices:

- Services tested in isolation with Mockito — no Spring context required
- RabbitMQ auto-startup disabled in `test` profile; Redis autoconfiguration excluded
- All tests run without external services in CI

## Project Structure

```
eventrelay/
├── backend/
│   ├── src/main/java/com/eventrelay/
│   │   ├── config/          # RabbitMQ, Redis, CORS, Resilience4j config
│   │   ├── controller/      # Thin route handlers (6 controllers)
│   │   ├── service/         # Business logic: ingestion, routing, dedup, metrics, alerts
│   │   ├── consumer/        # RabbitMQ listeners with @CircuitBreaker + @Retry
│   │   ├── model/           # JPA entities (4 tables + EventStatus enum)
│   │   ├── dto/             # Request/response records
│   │   └── repository/      # Spring Data JPA interfaces
│   └── src/test/            # 20 unit tests across 4 service classes
├── frontend/
│   ├── src/api/             # Typed API client
│   ├── src/components/      # Shared UI components
│   └── src/pages/           # Events, Metrics, Sources, Dead Letters views
├── scripts/
│   └── simulate_webhooks.sh # Signed GitHub + Stripe test payloads
├── docker-compose.yml
└── .github/workflows/ci.yml
```

The backend follows a **thin controllers, thick services** pattern. Route handlers delegate immediately to the service layer; all business logic — signature policy, retry thresholds, routing rules — lives in dedicated service classes.

## Getting Started

### Docker (quickest)

```bash
git clone <repo-url> && cd EventRelay
docker compose up --build
```

| Service | URL |
|---------|-----|
| React Dashboard | http://localhost:3000 |
| Backend API | http://localhost:8080/api |
| OpenAPI (Swagger) | http://localhost:8080/swagger-ui.html |
| RabbitMQ UI | http://localhost:15672 — guest / guest |

### Send a test webhook

```bash
# Register a source
curl -X POST http://localhost:8080/api/sources \
  -H "Content-Type: application/json" \
  -d '{"name":"github","displayName":"GitHub","signingSecret":"test-secret-123"}'

# Fire signed test payloads (GitHub push, issues, PR + Stripe payment)
chmod +x scripts/simulate_webhooks.sh
./scripts/simulate_webhooks.sh
```

### Run tests

```bash
cd backend && ./gradlew test
```
