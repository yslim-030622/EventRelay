# Phase F: Deploy + Polish

## Goal

CI green, nginx proxies API, Docker full-stack works end-to-end, README is portfolio-ready.

## Pre-condition Check (Phase E Gate)

```bash
# Frontend builds
cd frontend && npm run build 2>&1 | tail -1
# Must succeed

# Backend tests pass
cd backend && ./gradlew test 2>&1 | tail -1
# Must show BUILD SUCCESSFUL

# All API endpoints respond
for endpoint in health events metrics/summary metrics/circuit-breaker dead-letters sources; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/$endpoint)
  echo "$endpoint: $STATUS"
done
# All should be 200
```

## Current State

- nginx.conf: Only serves static files, NO `/api/` proxy
- docker-compose.yml: Has frontend service (port 3000) but nginx can't reach backend
- .github/workflows/ci.yml: Exists but may reference `./gradlew` which didn't exist before Phase A
- README.md: Skeleton, not portfolio-ready
- .gitignore: Properly configured (ai-workflow/ excluded)

## Task List (5 tasks)

### F1: `fix(frontend): add API proxy to nginx.conf`

- **File**: `frontend/nginx.conf`
- **Current**:

```nginx
server {
    listen 80;
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
}
```

- **Add**:

```nginx
    location /api/ {
        proxy_pass http://app:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
```

- **Verify**: `curl localhost:3000/api/health` returns `{"status":"ok"}`
- [x] Done

### F2: `ci: fix GitHub Actions workflow`

- **File**: `.github/workflows/ci.yml`
- **Verify/fix**:
  - Uses `./gradlew build` (gradlew now exists after Phase A)
  - Services: postgres:16 + redis:7-alpine with healthchecks
  - RabbitMQ not needed in CI if tests mock it (check application-test.yml)
  - SPRING_PROFILES_ACTIVE=test
  - Java 17 setup
- **Spec ref**: IMPLEMENTATION.md lines 2985-3043
- [x] Done

### F3: `docs: write portfolio-ready README`

- **File**: `README.md`
- **Sections**:
  1. Project title + one-line description
  2. Architecture diagram (ASCII: webhook → verify → dedup → store → RabbitMQ → consumers)
  3. Key features (6-8 bullets: HMAC verification, dedup, type-based routing, circuit breaker, retry + DLQ, monitoring dashboard, Discord alerts)
  4. Tech stack table (Spring Boot, PostgreSQL, Redis, RabbitMQ, Resilience4j, React, Docker)
  5. API endpoints table (all routes with method, path, description)
  6. Design decisions (3-4 with reasoning: why EventStatus enum, why service layer extraction, why topic exchange, why Redis dedup)
  7. Local development: `docker compose up --build`, open localhost:3000
  8. Running tests: `cd backend && ./gradlew test`
- [ ] Done

### F4: `chore(docker): verify full-stack Docker Compose`

- **Action**: Clean rebuild and end-to-end test
- **Commands**:

```bash
docker compose down -v
docker compose up --build -d
sleep 30
# Create source
curl -X POST localhost:8080/api/sources \
  -H "Content-Type: application/json" \
  -d '{"name":"github","displayName":"GitHub","signingSecret":"test-secret-123"}'
# Send webhook with valid signature
SECRET="test-secret-123"
PAYLOAD='{"ref":"main","commits":[{"message":"final"}]}'
SIG="sha256=$(printf '%s' "$PAYLOAD" | openssl dgst -sha256 -hmac "$SECRET" | awk '{print $2}')"
curl -X POST localhost:8080/api/webhooks/github \
  -H "Content-Type: application/json" \
  -H "X-Hub-Signature-256: $SIG" \
  -H "X-GitHub-Event: push" \
  -H "X-GitHub-Delivery: final-$(date +%s)" \
  -d "$PAYLOAD"
sleep 5
# Verify via frontend proxy
curl localhost:3000/api/events
curl localhost:3000/api/metrics/summary
```

- [ ] Done

### F5: `chore: final cleanup`

- **Action**:
  - Remove resolved TODO comments in code
  - Verify no secrets in committed files
  - Verify .gitignore covers all necessary paths
  - Final `./gradlew test` + `npm run build`
- [ ] Done

## Gate Check

```bash
# Full stack
docker compose down -v && docker compose up --build -d && sleep 30

# Backend direct
curl -s http://localhost:8080/api/health
# 200

# Frontend proxy
curl -s http://localhost:3000/api/health
# 200 (through nginx)

# Frontend HTML
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000
# 200

# End-to-end flow
curl -X POST http://localhost:8080/api/sources \
  -H "Content-Type: application/json" \
  -d '{"name":"github","displayName":"GitHub","signingSecret":"s"}'

PAYLOAD='{"ref":"main"}'
SIG="$(printf '%s' "$PAYLOAD" | openssl dgst -sha256 -hmac "s" | awk '{print $2}')"

curl -X POST http://localhost:8080/api/webhooks/github \
  -H "Content-Type: application/json" \
  -H "X-Hub-Signature-256: sha256=${SIG}" \
  -H "X-GitHub-Event: push" \
  -H "X-GitHub-Delivery: e2e-$(date +%s)" \
  -d "$PAYLOAD"

sleep 3
curl -s http://localhost:8080/api/metrics/summary | python3 -m json.tool

# Tests
cd backend && ./gradlew test

# README
head -20 README.md
```

## AI Notes Space

_Record any decisions or issues:_
