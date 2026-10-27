#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "==> Building UI"
(cd console-ui && npm ci --prefer-offline && npm run build)

echo "==> Compose up (DinD VMs + platform)"
docker compose up -d --build postgres vault keycloak registry demo-vm-a demo-vm-b jenkins nexus
echo "==> Waiting for DinD…"
for i in $(seq 1 40); do
  if docker compose exec -T demo-vm-a docker info >/dev/null 2>&1 \
    && docker compose exec -T demo-vm-b docker info >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

docker compose up -d --build watermelon-ci forgejo

echo "==> Waiting for API :8088"
for i in $(seq 1 60); do
  if curl -fsS http://localhost:8088/actuator/health >/dev/null 2>&1; then
    break
  fi
  sleep 3
done

echo "==> Seed demo"
curl -fsS -X POST http://localhost:8088/api/v1/demo/seed | tee /tmp/wm-demo-seed.json
echo
echo "Console:  http://localhost:8088/"
echo "Jenkins:  http://localhost:8080/  (admin/admin)"
echo "Registry: http://localhost:5001/v2/_catalog"
echo "Done."
