#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
SECRET="${2:-test-secret-123}"
DELIVERY_ID="${3:-github-$(date +%s)}"
PAYLOAD='{"action":"opened","repository":{"full_name":"yslim0622/eventrelay"},"pull_request":{"number":1,"title":"Initial skeleton"}}'
SIGNATURE="sha256=$(printf '%s' "$PAYLOAD" | openssl dgst -sha256 -hmac "$SECRET" | awk '{print $2}')"

curl -i \
  -X POST "${BASE_URL}/api/webhooks/github" \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: pull_request" \
  -H "X-GitHub-Delivery: ${DELIVERY_ID}" \
  -H "X-Hub-Signature-256: ${SIGNATURE}" \
  -d "$PAYLOAD"
