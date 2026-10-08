#!/usr/bin/env bash
DISH_ID="${1:-1}"
REQUESTS="${2:-50}"
BASE_URL="${3:-http://localhost:8080}"

echo "HTTP status distribution for ${REQUESTS} simultaneous requests:"
seq 1 "${REQUESTS}" | xargs -P "${REQUESTS}" -I{} curl -s -o /dev/null -w "%{http_code}\n" \
  -X POST "${BASE_URL}/orders" \
  -H "Content-Type: application/json" \
  -d "{\"dishId\": ${DISH_ID}, \"quantity\": 1}" | sort | uniq -c

echo "Final dish state:"
curl -s "${BASE_URL}/dishes/${DISH_ID}"
echo
