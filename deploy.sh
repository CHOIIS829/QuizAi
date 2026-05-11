#!/bin/bash
set -euo pipefail

# 환경 변수 로드 (필요시)
# source .env

RUNTIME_CONF="./deploy/nginx/service-url.inc"

compose() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
  else
    docker-compose "$@"
  fi
}

write_upstream() {
  mkdir -p "$(dirname "$RUNTIME_CONF")"
  printf 'set $service_url %s;\n' "$1" > "$RUNTIME_CONF"
}

IS_GREEN=$(docker ps --filter "name=quizAi-backend-green" --filter "status=running" --format '{{.Names}}' | grep -x "quizAi-backend-green" || true)

if [ -z "$IS_GREEN" ]; then
  echo "### Blue => Green ###"
  CURRENT_PORT=8090
  TARGET_PORT=8091
  TARGET_SERVICE="backend-green"
  TARGET_UPSTREAM="http://backend-green:8080"
  STOP_SERVICE="backend-blue"
else
  echo "### Green => Blue ###"
  CURRENT_PORT=8091
  TARGET_PORT=8090
  TARGET_SERVICE="backend-blue"
  TARGET_UPSTREAM="http://backend-blue:8080"
  STOP_SERVICE="backend-green"
fi

echo "1. Start new connection ($TARGET_SERVICE)..."
compose up -d --build "$TARGET_SERVICE"

# 마운트된 볼륨의 권한을 appuser로 변경 (root 권한 필요)
echo "1-1. Fix volume permissions for $TARGET_SERVICE..."
compose exec -T -u root "$TARGET_SERVICE" chown -R appuser:appuser /app/temp/video

echo "2. Health Check..."
for i in {1..15}
do
  sleep 5
  RESPONSE=$(curl -s "http://localhost:$TARGET_PORT/actuator/health" || true)
  if [[ "$RESPONSE" == *"UP"* ]]; then
      echo ">> Success!"
      break
  fi
  echo ">> Wait... (Attempt $i/15) - Response: $RESPONSE"

  if [ $i -eq 15 ]; then
    echo ">> Fail... Stopping new service."
    compose logs --tail=100 "$TARGET_SERVICE"
    compose stop "$TARGET_SERVICE"
    exit 1
  fi
done

echo "3. Update runtime upstream config..."
write_upstream "$TARGET_UPSTREAM"

echo "4. Check & Start Frontend..."
# service-url.inc는 호스트 파일로 마운트되어 frontend 컨테이너 재생성 후에도 유지된다.
compose up -d --build --no-deps frontend

echo "5. Reload Nginx Upstream..."
docker exec quizAi-frontend nginx -t && docker exec quizAi-frontend nginx -s reload

echo "6. Verify frontend can reach $TARGET_SERVICE..."
docker exec quizAi-frontend /bin/sh -c "wget -qO- --timeout=5 $TARGET_UPSTREAM/actuator/health | grep -q 'UP'"

echo "7. Stop old service ($STOP_SERVICE)..."
compose stop "$STOP_SERVICE"
compose rm -f "$STOP_SERVICE"

echo "Deploy Finished!"
