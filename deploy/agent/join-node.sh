#!/usr/bin/env bash
# Watermelon CI worker node bootstrap
set -euo pipefail

: "${WM_API_URL:?set WM_API_URL}"
: "${WM_CLUSTER:?set WM_CLUSTER}"
: "${WM_NODE_ID:?set WM_NODE_ID}"
: "${WM_JOIN_TOKEN:?set WM_JOIN_TOKEN}"
: "${WM_ENGINE_URL:=http://127.0.0.1:2375}"

HOST="$(hostname)"
ARCH="$(uname -m)"
CPU="$(getconf _NPROCESSORS_ONLN 2>/dev/null || sysctl -n hw.ncpu)"
MEM="$(sysctl -n hw.memsize 2>/dev/null || awk '/MemTotal/ {print $2*1024}' /proc/meminfo)"

curl -fsS -X POST "${WM_API_URL}/api/v1/fleet/clusters/${WM_CLUSTER}/nodes/${WM_NODE_ID}/join" \
  -H 'Content-Type: application/json' \
  -d "{\"joinToken\":\"${WM_JOIN_TOKEN}\",\"engineUrl\":\"${WM_ENGINE_URL}\",\"hostname\":\"${HOST}\",\"architecture\":\"${ARCH}\",\"cpuCores\":${CPU},\"memoryBytes\":${MEM},\"agentVersion\":\"0.1.0\"}"

echo
echo "Node joined. Heartbeat loop..."
while true; do
  curl -fsS -X POST "${WM_API_URL}/api/v1/fleet/clusters/${WM_CLUSTER}/nodes/${WM_NODE_ID}/heartbeat" \
    -H 'Content-Type: application/json' \
    -d "{\"hostname\":\"${HOST}\",\"cpuCores\":${CPU},\"memoryBytes\":${MEM}}" \
    >/dev/null || true
  sleep 20
done
