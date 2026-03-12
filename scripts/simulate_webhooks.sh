#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
GITHUB_SECRET="${2:-test-secret-123}"
STRIPE_SECRET="${3:-stripe-secret-456}"

echo "=== EventRelay Webhook Simulator ==="
echo "Base URL : $BASE_URL"
echo ""

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

github_sig() {
  local payload="$1"
  local secret="$2"
  printf '%s' "$payload" | openssl dgst -sha256 -hmac "$secret" | awk '{print $2}'
}

stripe_sig() {
  local payload="$1"
  local secret="$2"
  local ts="$3"
  printf '%s' "${ts}.${payload}" | openssl dgst -sha256 -hmac "$secret" | awk '{print $2}'
}

send_github() {
  local event="$1"
  local payload="$2"
  local delivery_id="github-${event}-$(date +%s)-$$"
  local sig
  sig="sha256=$(github_sig "$payload" "$GITHUB_SECRET")"

  echo "-> GitHub $event"
  curl -s -o /dev/null -w "   HTTP %{http_code}\n" \
    -X POST "${BASE_URL}/api/webhooks/github" \
    -H "Content-Type: application/json" \
    -H "X-GitHub-Event: ${event}" \
    -H "X-GitHub-Delivery: ${delivery_id}" \
    -H "X-Hub-Signature-256: ${sig}" \
    -d "$payload"
}

send_stripe() {
  local payload="$1"
  local ts
  ts="$(date +%s)"
  local sig
  sig="$(stripe_sig "$payload" "$STRIPE_SECRET" "$ts")"

  echo "-> Stripe payment_intent.succeeded"
  curl -s -o /dev/null -w "   HTTP %{http_code}\n" \
    -X POST "${BASE_URL}/api/webhooks/stripe" \
    -H "Content-Type: application/json" \
    -H "Stripe-Signature: t=${ts},v1=${sig}" \
    -d "$payload"
}

# ---------------------------------------------------------------------------
# Ensure sources exist
# ---------------------------------------------------------------------------

echo "--- Ensuring sources exist ---"

GITHUB_RESULT=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST "${BASE_URL}/api/sources" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"github\",\"displayName\":\"GitHub\",\"signingSecret\":\"${GITHUB_SECRET}\"}")
echo "   GitHub source: HTTP $GITHUB_RESULT"

STRIPE_RESULT=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST "${BASE_URL}/api/sources" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"stripe\",\"displayName\":\"Stripe\",\"signingSecret\":\"${STRIPE_SECRET}\"}")
echo "   Stripe source: HTTP $STRIPE_RESULT"
echo ""

# ---------------------------------------------------------------------------
# GitHub events
# ---------------------------------------------------------------------------

echo "--- GitHub events ---"

PUSH_PAYLOAD='{"ref":"refs/heads/main","commits":[{"id":"abc123","message":"feat: new feature"}],"repository":{"full_name":"yslim0622/eventrelay"}}'
send_github "push" "$PUSH_PAYLOAD"
sleep 1

ISSUES_PAYLOAD='{"action":"opened","issue":{"number":42,"title":"Bug report"},"repository":{"full_name":"yslim0622/eventrelay"}}'
send_github "issues" "$ISSUES_PAYLOAD"
sleep 1

PR_PAYLOAD='{"action":"opened","pull_request":{"number":1,"title":"Initial PR"},"repository":{"full_name":"yslim0622/eventrelay"}}'
send_github "pull_request" "$PR_PAYLOAD"
echo ""

# ---------------------------------------------------------------------------
# Stripe events
# ---------------------------------------------------------------------------

echo "--- Stripe events ---"

STRIPE_PAYLOAD="{\"id\":\"evt_$(date +%s)\",\"type\":\"payment_intent.succeeded\",\"data\":{\"object\":{\"amount\":2000,\"currency\":\"usd\"}}}"
send_stripe "$STRIPE_PAYLOAD"
echo ""

# ---------------------------------------------------------------------------
# Duplicate test
# ---------------------------------------------------------------------------

echo "--- Duplicate test (same delivery ID) ---"
DUP_DELIVERY="dup-test-delivery-001"
DUP_PAYLOAD='{"action":"closed","pull_request":{"number":2},"repository":{"full_name":"yslim0622/eventrelay"}}'
DUP_SIG="sha256=$(github_sig "$DUP_PAYLOAD" "$GITHUB_SECRET")"

echo "-> First send (expect 202)"
curl -s -o /dev/null -w "   HTTP %{http_code}\n" \
  -X POST "${BASE_URL}/api/webhooks/github" \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: pull_request" \
  -H "X-GitHub-Delivery: ${DUP_DELIVERY}" \
  -H "X-Hub-Signature-256: ${DUP_SIG}" \
  -d "$DUP_PAYLOAD"

echo "-> Second send (expect 200 duplicate)"
curl -s -o /dev/null -w "   HTTP %{http_code}\n" \
  -X POST "${BASE_URL}/api/webhooks/github" \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: pull_request" \
  -H "X-GitHub-Delivery: ${DUP_DELIVERY}" \
  -H "X-Hub-Signature-256: ${DUP_SIG}" \
  -d "$DUP_PAYLOAD"

echo ""
echo "=== Done ==="
