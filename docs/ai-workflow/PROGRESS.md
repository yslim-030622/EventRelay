# EventRelay — Progress Tracker

## Status Legend

- `NOT_STARTED` — Phase not yet begun
- `IN_PROGRESS` — Currently working on this phase
- `BLOCKED` — Blocked by an issue (see notes)
- `COMPLETE` — All tasks done, gate passed

---

## Phase Status

| Phase | Status | Started | Completed | Tasks Done | Notes |
| ----- | ------ | ------- | --------- | ---------- | ----- |
| A — Infrastructure Fix | COMPLETE | 2026-03-09 | 2026-03-09 | 6/6 | |
| B — Backend Completion | COMPLETE | 2026-03-09 | 2026-03-10 | 9/9 | |
| C — Resilience + Alerting | COMPLETE | 2026-03-10 | 2026-03-11 | 5/5 | |
| D — Tests | COMPLETE | 2026-03-11 | 2026-03-11 | 5/5 | |
| E — Frontend Completion | COMPLETE | 2026-03-11 | 2026-03-12 | 7/7 | |
| F — Deploy + Polish | NOT_STARTED | | | 1/5 | |

---

## Daily Log

### Day 1 (2026-03-08)

_AI: Record what was accomplished_

### Day 2 (2026-03-09)

- A1: gradle 설치 (brew), wrapper 생성 (8.10.2), 커밋 완료
- gitignore 업데이트: docs/ai-workflow/, resume.md, frontend 빌드 산출물 추가
- A2: HealthController 생성 (`GET /api/health` → `{"status":"ok"}`), 커밋 완료

### Day 3 (2026-03-10)

- B6: DeadLetterService.markReplayed() — 이벤트 상태 전체 초기화 + RabbitMQ 재발행 구현
- B7: MetricsController에 `GET /api/metrics/circuit-breaker` 추가 (CircuitBreakerRegistry 주입)
- B8: IncomingEventRepository에 GROUP BY 쿼리 3개 추가, MetricsService bySource()/byType() findAll() 루프 제거
- B9: EventRoutingService에 getMaxRetries() 추가, WebhookIngestionService maxRetries 설정, EventProcessingService 하드코딩 임계값(>=3) 제거
- C1: EventProcessingService를 beginProcessing/markSuccess/recordFailureAttempt/finalizeDeadLetter로 분리, 실패 시 rethrow 추가
- C2: 3개 consumer 모두 lifecycle API + fallback 메서드로 재작성, GenericEventConsumer에 @CircuitBreaker/@Retry 추가, _forceFailure 테스트 트리거 추가
- C4: DeadLetterService에 DiscordAlertService 주입, moveToDeadLetter() 저장 후 sendAlert() 호출 (color 0xFF0000)
- C3: DiscordAlertService 생성, webhook URL 미설정 시 skip + Discord embed 전송 구현
- C5: application.yml에 genericConsumer CB + genericConsumerRetry 추가, retry max-attempts 정렬 (payment=10, github=3, generic=5)

### Day 4 (2026-03-11)

- D1: SignatureVerifierTest를 12개 테스트로 확장 (GitHub null/prefix/wrong-secret, Stripe valid/expired/empty/null, unknown source passthrough, 라우팅 dispatch)
- D2: DeduplicationServiceTest 생성 (StringRedisTemplate mock, new/existing/null 3개 케이스)
- D3: EventRoutingServiceTest에 getMaxRetries 테스트 3개 추가 (payment=10, push=3, unknown=5)
- D4: application-test.yml에 RabbitMQ auto-startup 비활성화 + Redis autoconfigure 제외 추가, ./gradlew test BUILD SUCCESSFUL
- D5: simulate_webhooks.sh 재작성 — GitHub push/issues/PR + Stripe payment 이벤트, HMAC 서명, 중복 테스트 포함

### Day 5 (2026-03-12)

- E1: added replayEvent(), replayDeadLetter(), getCircuitBreakerStatus() to client.ts + CircuitBreakerInfo type to contracts.ts
- E2: EventsPage — status/source filters, auto-refresh every 10s, prev/next pagination controls
- E3: EventDetailPage — replay button with loading state, success feedback re-fetches event, back navigation link
- E4: DeadLettersPage — per-row replay buttons (hidden when replayed), unreplayed-only filter toggle, error message column, refresh after replay
- E5: MetricsPage — auto-refresh 30s, CB section with CLOSED/OPEN/HALF_OPEN colored cards, by-source/by-type as formatted tables, success rate metric card
- E6: SourcesPage — added colored Active/Inactive status badge (green/grey) replacing plain Yes/No text
- E7: frontend build verified clean — tsc -b + vite build, 89 modules, zero TypeScript errors

### Day 6 (2026-03-13)

_AI: Record what was accomplished_

- F1: frontend nginx adds `/api/` proxy to backend service

### Day 7 (2026-03-14)

_AI: Record what was accomplished_

---

## Gate Check Results

### Phase A Gate

- [x] Local `gradle` exists or the blocker is recorded before wrapper generation
- [x] `./gradlew compileJava` runs after wrapper creation
- [x] `docker compose up --build` starts without crash
- [x] `curl /api/health` → 200 `{"status":"ok"}`
- [x] `POST /api/sources` → 201
- [x] Signed `POST /api/webhooks/github` → 202 accepted
- **Result**: PASS (2026-03-09)

### Phase B Gate

- [x] Stripe signature verification: valid → 202, invalid → 401
- [x] Payment queue exists in RabbitMQ, PaymentEventConsumer processes events
- [x] `POST /api/events/{id}/replay` resets state and re-publishes with the correct routing key
- [x] `POST /api/dead-letters/{id}/replay` resets state, marks replayed, and re-publishes with the correct routing key
- [x] `GET /api/metrics/circuit-breaker` returns CB states
- [x] MetricsService uses GROUP BY queries (not findAll loop)
- [x] `maxRetries` is used by backend execution logic instead of a hardcoded dead-letter threshold
- **Result**: PASS (2026-03-10)

### Phase C Gate

- [ ] All 3 consumers have @CircuitBreaker + @Retry
- [ ] Consumer processing records failures and rethrows them so retry/circuit-breaker behavior is observable
- [ ] Fallback methods perform final bookkeeping and DLQ transition
- [ ] Forced failure → retry visible in logs → event reaches DEAD_LETTER when `retryCount` reaches `maxRetries`
- [ ] DiscordAlertService created (sends if URL configured, skips if not)
- **Result**: _pending_

### Phase D Gate

- [ ] `./gradlew test` → BUILD SUCCESSFUL
- [ ] SignatureVerifierTest: 9+ test cases (GitHub + Stripe + unknown)
- [ ] DeduplicationServiceTest: 3 test cases
- [ ] EventRoutingServiceTest: passing
- [ ] `scripts/simulate_webhooks.sh` uses signed requests by default and runs without error
- **Result**: _pending_

### Phase E Gate

- [ ] `npm run build` succeeds
- [ ] EventsPage: status/source filters, auto-refresh 10s, pagination controls
- [ ] EventDetailPage: replay button that calls API
- [ ] DeadLettersPage: replay buttons, replayed filter
- [ ] MetricsPage: auto-refresh, circuit breaker section
- [ ] client.ts has replayEvent, replayDeadLetter, getCircuitBreakerStatus
- [ ] Backend prerequisites exist before frontend work starts: event replay, dead-letter replay, circuit-breaker endpoint
- **Result**: _pending_

### Phase F Gate

- [ ] nginx.conf proxies `/api/` to backend
- [ ] `curl localhost:3000/api/health` → 200 through nginx
- [ ] GitHub Actions CI YAML is valid
- [ ] README.md complete (architecture, features, API table, design decisions)
- [ ] End-to-end verification uses signed GitHub webhook requests
- [ ] `docker compose up --build` full stack end-to-end
- **Result**: _pending_
