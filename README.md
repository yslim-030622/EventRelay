# EventRelay

EventRelay is a webhook ingestion and event delivery platform built around Spring Boot, PostgreSQL, Redis, RabbitMQ, and a React operations dashboard.

## Goals

- Accept signed webhooks from external sources.
- Deduplicate deliveries before they hit downstream consumers.
- Route accepted events through RabbitMQ for asynchronous processing.
- Expose operational visibility through metrics, dead-letter views, and event traces.

## Repository Layout

```text
.
├── .github/
│   └── workflows/
│       └── ci.yml
├── backend/
│   ├── build.gradle
│   ├── Dockerfile
│   ├── settings.gradle
│   └── src/
│       ├── main/
│       │   ├── java/com/eventrelay/
│       │   │   ├── config/
│       │   │   ├── consumer/
│       │   │   ├── controller/
│       │   │   ├── dto/
│       │   │   ├── exception/
│       │   │   ├── model/
│       │   │   ├── repository/
│       │   │   ├── service/
│       │   │   └── EventRelayApplication.java
│       │   └── resources/
│       └── test/
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   ├── src/
│   │   ├── api/
│   │   ├── components/
│   │   └── pages/
│   └── vite.config.ts
├── scripts/
│   └── simulate_webhooks.sh
├── .env.example
├── docker-compose.yml
└── README.md
```

## Backend Modules

- `config`: RabbitMQ and web configuration.
- `model`: JPA entities for webhook sources, events, deliveries, and dead letters.
- `repository`: persistence interfaces.
- `service`: ingestion, routing, deduplication, metrics, and replay workflow hooks.
- `controller`: REST APIs for sources, webhooks, events, metrics, and dead letters.
- `consumer`: message listeners for queue-based processing.

## Frontend Modules

- `api`: typed API client and contracts.
- `components`: shell and reusable cards.
- `pages`: operations views for events, metrics, sources, and dead letters.

## Local Development

### 1. Start infrastructure and applications

```bash
docker compose up --build
```

### 2. Register a source

```bash
curl -X POST http://localhost:8080/api/sources \
  -H "Content-Type: application/json" \
  -d '{
    "name": "github",
    "displayName": "GitHub",
    "signingSecret": "test-secret-123"
  }'
```

### 3. Send a sample webhook

```bash
chmod +x scripts/simulate_webhooks.sh
./scripts/simulate_webhooks.sh
```

### 4. Open the dashboard

- App: `http://localhost:3000`
- API: `http://localhost:8080/api`
- RabbitMQ: `http://localhost:15672`

## API Surface

- `POST /api/sources`
- `GET /api/sources`
- `POST /api/webhooks/{source}`
- `GET /api/events`
- `GET /api/events/{id}`
- `GET /api/metrics/summary`
- `GET /api/metrics/by-source`
- `GET /api/metrics/by-type`
- `GET /api/dead-letters`
- `POST /api/dead-letters/{id}/replay`

## Next Implementation Milestones

- Add real outbound delivery integrations in `EventProcessingService`.
- Expand retry and replay policies per source and event type.
- Add source-specific signature verification strategies.
- Add database migrations and seed data.
- Add richer charts and filters to the dashboard.
