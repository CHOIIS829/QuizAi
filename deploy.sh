#!/usr/bin/env bash
set -Eeuo pipefail

STATE_DIR="${DEPLOY_STATE_DIR:-./.deploy-state}"
BACKEND_CONF="$STATE_DIR/nginx/backend-url.inc"
FRONTEND_CONF="$STATE_DIR/nginx/frontend-url.inc"
LOCK_DIR="$STATE_DIR/deploy.lock"
GATEWAY_CONTAINER="quizAi-gateway"
LEGACY_FRONTEND_CONTAINER="quizAi-frontend"
LEGACY_FRONTEND_CONF="./deploy/nginx/service-url.inc"
YTDLP_SMOKE_TEST_URL="${YTDLP_SMOKE_TEST_URL:-https://youtu.be/CoyQM_Zi0OM}"
YTDLP_SMOKE_TEST_RETRIES="${YTDLP_SMOKE_TEST_RETRIES:-3}"
BACKEND_STOP_TIMEOUT_SECONDS="${BACKEND_STOP_TIMEOUT_SECONDS:-120}"
DEPLOY_PULL_IMAGES="${DEPLOY_PULL_IMAGES:-false}"
LEGACY_FRONTEND_STOPPED=false

compose() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
  else
    docker-compose "$@"
  fi
}

log() {
  printf '[deploy] %s\n' "$*"
}

read_service() {
  local file="$1"
  local prefix="$2"
  local fallback="$3"
  local service

  service=$(grep -oE "${prefix}-(blue|green)" "$file" 2>/dev/null | head -n 1 || true)
  if [ -z "$service" ]; then
    service="$fallback"
  fi
  printf '%s' "$service"
}

opposite_service() {
  local service="$1"
  case "$service" in
    *-blue) printf '%s-green' "${service%-blue}" ;;
    *-green) printf '%s-blue' "${service%-green}" ;;
    *) log "알 수 없는 Blue/Green 서비스: $service" >&2; return 1 ;;
  esac
}

service_url() {
  printf 'http://%s:8080' "$1"
}

write_upstream() {
  local file="$1"
  local variable="$2"
  local url="$3"
  mkdir -p "$(dirname "$file")"
  printf 'set $%s %s;\n' "$variable" "$url" > "$file"
}

write_legacy_backend_upstream() {
  local url="$1"
  printf 'set $service_url %s;\n' "$url" > "$LEGACY_FRONTEND_CONF"
}

container_exists() {
  [ -n "$(compose ps -aq "$1" 2>/dev/null || true)" ]
}

container_running() {
  [ "$(docker inspect -f '{{.State.Running}}' "$(compose ps -q "$1" 2>/dev/null)" 2>/dev/null || true)" = "true" ]
}

wait_for_healthy_container() {
  local service="$1"
  local attempts="${2:-30}"
  local container_id
  local status

  container_id=$(compose ps -q "$service")
  for ((attempt = 1; attempt <= attempts; attempt++)); do
    status=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id" 2>/dev/null || true)
    if [ "$status" = "healthy" ] || [ "$status" = "running" ]; then
      log "$service 상태 확인 완료: $status"
      return 0
    fi
    if [ "$status" = "unhealthy" ] || [ "$status" = "exited" ] || [ "$status" = "dead" ]; then
      log "$service 상태 확인 실패: $status" >&2
      compose logs --tail=100 "$service" || true
      return 1
    fi
    log "$service 대기 중 ($attempt/$attempts): ${status:-unknown}"
    sleep 5
  done

  compose logs --tail=100 "$service" || true
  return 1
}

wait_for_backend() {
  local port="$1"
  local service="$2"

  for attempt in {1..24}; do
    if curl -fsS "http://127.0.0.1:${port}/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
      log "$service 애플리케이션 헬스체크 통과"
      return 0
    fi
    log "$service 애플리케이션 대기 중 ($attempt/24)"
    sleep 5
  done

  compose logs --tail=150 "$service" || true
  return 1
}

run_ytdlp_smoke_test() {
  local service="$1"

  if [ "${SKIP_YTDLP_SMOKE_TEST:-false}" = "true" ]; then
    log "SKIP_YTDLP_SMOKE_TEST=true: yt-dlp smoke test를 건너뜁니다."
    return 0
  fi

  log "설치된 yt-dlp 버전: $(compose exec -T "$service" yt-dlp --version)"
  for ((attempt = 1; attempt <= YTDLP_SMOKE_TEST_RETRIES; attempt++)); do
    if compose exec -T "$service" yt-dlp \
      --simulate \
      --no-playlist \
      --force-ipv4 \
      --extractor-args "youtube:player_client=android" \
      -f "18/ba[ext=m4a]/ba" \
      "$YTDLP_SMOKE_TEST_URL"; then
      log "yt-dlp smoke test 통과"
      return 0
    fi
    log "yt-dlp smoke test 재시도 ($attempt/$YTDLP_SMOKE_TEST_RETRIES)" >&2
    sleep 5
  done

  log "yt-dlp smoke test 실패. 긴급 우회가 필요하면 SKIP_YTDLP_SMOKE_TEST=true를 명시하세요." >&2
  return 1
}

reload_gateway() {
  docker exec "$GATEWAY_CONTAINER" nginx -t
  docker exec "$GATEWAY_CONTAINER" nginx -s reload
}

verify_gateway_routes() {
  local backend_url="$1"
  local frontend_url="$2"

  docker exec "$GATEWAY_CONTAINER" /bin/sh -c \
    "wget -qO- --timeout=5 '$backend_url/actuator/health' | grep -q '\"status\":\"UP\"'"
  docker exec "$GATEWAY_CONTAINER" /bin/sh -c \
    "wget -qO- --timeout=5 '$frontend_url/healthz' | grep -q '^ok$'"
  curl -fsS "http://127.0.0.1:${GATEWAY_PORT:-90}/healthz" | grep -q '^ok$'
}

acquire_lock() {
  mkdir -p "$STATE_DIR"
  if ! mkdir "$LOCK_DIR" 2>/dev/null; then
    log "다른 배포가 진행 중입니다: $LOCK_DIR" >&2
    exit 1
  fi
}

initialize_runtime_state() {
  local detected_backend=""
  local detected_frontend=""
  local active_state="$STATE_DIR/active.env"

  mkdir -p "$STATE_DIR/nginx"
  if [ -f "$active_state" ]; then
    # 이 파일은 deploy.sh가 고정된 서비스 이름만 기록합니다.
    # shellcheck disable=SC1090
    source "$active_state"
    detected_backend="${ACTIVE_BACKEND:-}"
    detected_frontend="${ACTIVE_FRONTEND:-}"
  fi

  if ! validate_state_service "$detected_backend" backend; then
    detected_backend=$(read_service "$BACKEND_CONF" backend "")
  fi
  if ! validate_state_service "$detected_backend" backend; then
    if docker ps --format '{{.Names}}' | grep -qx 'quizAi-backend-green'; then
      detected_backend="backend-green"
    else
      detected_backend="backend-blue"
    fi
  fi

  if ! validate_state_service "$detected_frontend" frontend; then
    detected_frontend=$(read_service "$FRONTEND_CONF" frontend "")
  fi
  if ! validate_state_service "$detected_frontend" frontend; then
    if docker ps --format '{{.Names}}' | grep -qx 'quizAi-frontend-green'; then
      detected_frontend="frontend-green"
    else
      detected_frontend="frontend-blue"
    fi
  fi

  write_upstream "$BACKEND_CONF" backend_url "$(service_url "$detected_backend")"
  write_upstream "$FRONTEND_CONF" frontend_url "$(service_url "$detected_frontend")"
  printf 'ACTIVE_BACKEND=%s\nACTIVE_FRONTEND=%s\n' \
    "$detected_backend" "$detected_frontend" > "$active_state"
}

stop_legacy_frontend_for_gateway_migration() {
  if docker ps --format '{{.Names}}' | grep -qx "$LEGACY_FRONTEND_CONTAINER"; then
    log "기존 단일 Frontend를 중지하고 고정 Gateway로 1회 전환합니다."
    write_legacy_backend_upstream "$(service_url "$ACTIVE_BACKEND")"
    docker stop "$LEGACY_FRONTEND_CONTAINER" >/dev/null
    LEGACY_FRONTEND_STOPPED=true
  fi
}

release_lock() {
  rmdir "$LOCK_DIR" 2>/dev/null || true
}

rollback_runtime_config() {
  log "Gateway upstream을 기존 서비스로 복원합니다."
  write_upstream "$BACKEND_CONF" backend_url "$(service_url "$ACTIVE_BACKEND")"
  write_upstream "$FRONTEND_CONF" frontend_url "$(service_url "$ACTIVE_FRONTEND")"
  if docker ps --format '{{.Names}}' | grep -qx "$GATEWAY_CONTAINER"; then
    reload_gateway || true
  fi
}

on_error() {
  local exit_code=$?
  local line_number="${1:-unknown}"
  trap - ERR
  log "배포 실패 (line: $line_number, exit: $exit_code)" >&2
  rollback_runtime_config
  if [ "$LEGACY_FRONTEND_STOPPED" = "true" ]; then
    compose stop gateway >/dev/null 2>&1 || true
    docker start "$LEGACY_FRONTEND_CONTAINER" >/dev/null 2>&1 || true
  fi
  if [ -n "${TARGET_BACKEND:-}" ]; then
    compose logs --tail=100 "$TARGET_BACKEND" || true
  fi
  release_lock
  exit "$exit_code"
}

validate_state_service() {
  local value="$1"
  local prefix="$2"
  [[ "$value" =~ ^${prefix}-(blue|green)$ ]]
}

rollback_previous_release() {
  local state_file="$STATE_DIR/previous.env"
  local previous_backend previous_frontend current_backend current_frontend

  if [ ! -f "$state_file" ]; then
    log "롤백 상태 파일이 없습니다: $state_file" >&2
    exit 1
  fi

  # 이 파일은 deploy.sh가 고정된 서비스 이름만 기록합니다.
  # shellcheck disable=SC1090
  source "$state_file"
  previous_backend="${PREVIOUS_BACKEND:-}"
  previous_frontend="${PREVIOUS_FRONTEND:-}"
  current_backend="${CURRENT_BACKEND:-}"
  current_frontend="${CURRENT_FRONTEND:-}"

  validate_state_service "$previous_backend" backend
  validate_state_service "$previous_frontend" frontend
  validate_state_service "$current_backend" backend
  validate_state_service "$current_frontend" frontend

  for service in "$previous_backend" "$previous_frontend"; do
    if ! container_exists "$service"; then
      log "롤백할 기존 컨테이너가 없습니다: $service" >&2
      exit 1
    fi
    docker start "$(compose ps -aq "$service")" >/dev/null
    wait_for_healthy_container "$service"
  done

  write_upstream "$BACKEND_CONF" backend_url "$(service_url "$previous_backend")"
  write_upstream "$FRONTEND_CONF" frontend_url "$(service_url "$previous_frontend")"
  reload_gateway
  verify_gateway_routes "$(service_url "$previous_backend")" "$(service_url "$previous_frontend")"

  compose stop -t "$BACKEND_STOP_TIMEOUT_SECONDS" "$current_backend" || true
  compose stop "$current_frontend" || true
  printf 'ACTIVE_BACKEND=%s\nACTIVE_FRONTEND=%s\n' \
    "$previous_backend" "$previous_frontend" > "$STATE_DIR/active.env"
  printf 'PREVIOUS_BACKEND=%s\nPREVIOUS_FRONTEND=%s\nCURRENT_BACKEND=%s\nCURRENT_FRONTEND=%s\n' \
    "$current_backend" "$current_frontend" "$previous_backend" "$previous_frontend" > "$STATE_DIR/previous.env"
  log "롤백 완료: $previous_frontend / $previous_backend"
}

if [ "${1:-deploy}" = "rollback" ]; then
  acquire_lock
  trap release_lock EXIT
  rollback_previous_release
  exit 0
fi

acquire_lock
trap release_lock EXIT

initialize_runtime_state

ACTIVE_BACKEND=$(read_service "$BACKEND_CONF" backend backend-blue)
ACTIVE_FRONTEND=$(read_service "$FRONTEND_CONF" frontend frontend-blue)
TARGET_BACKEND=$(opposite_service "$ACTIVE_BACKEND")
TARGET_FRONTEND=$(opposite_service "$ACTIVE_FRONTEND")

case "$TARGET_BACKEND" in
  backend-blue) TARGET_BACKEND_PORT=8090 ;;
  backend-green) TARGET_BACKEND_PORT=8091 ;;
esac

trap 'on_error $LINENO' ERR

log "현재 release: $ACTIVE_FRONTEND / $ACTIVE_BACKEND"
log "대상 release: $TARGET_FRONTEND / $TARGET_BACKEND"

log "1. MySQL과 Redis를 재생성하지 않고 유지·기동합니다."
compose up -d --no-recreate db redis
wait_for_healthy_container db
wait_for_healthy_container redis

log "2. 비활성 Frontend/Backend release를 준비합니다."
if [ "$DEPLOY_PULL_IMAGES" = "true" ]; then
  compose pull "$TARGET_FRONTEND" "$TARGET_BACKEND"
  compose up -d --no-deps --force-recreate "$TARGET_FRONTEND" "$TARGET_BACKEND"
else
  compose up -d --build --no-deps "$TARGET_FRONTEND" "$TARGET_BACKEND"
fi

compose exec -T -u root "$TARGET_BACKEND" chown -R appuser:appuser /app/temp/video
wait_for_healthy_container "$TARGET_FRONTEND"
wait_for_backend "$TARGET_BACKEND_PORT" "$TARGET_BACKEND"

log "3. yt-dlp 기능을 검증합니다."
run_ytdlp_smoke_test "$TARGET_BACKEND"

log "4. Gateway upstream을 새 release로 전환합니다."
if ! docker ps --format '{{.Names}}' | grep -qx "$GATEWAY_CONTAINER"; then
  stop_legacy_frontend_for_gateway_migration
fi
write_upstream "$BACKEND_CONF" backend_url "$(service_url "$TARGET_BACKEND")"
write_upstream "$FRONTEND_CONF" frontend_url "$(service_url "$TARGET_FRONTEND")"
compose up -d gateway
wait_for_healthy_container gateway
reload_gateway

log "5. Gateway에서 새 release를 최종 검증합니다."
verify_gateway_routes "$(service_url "$TARGET_BACKEND")" "$(service_url "$TARGET_FRONTEND")"

log "6. 롤백 정보를 보존하고 기존 release를 graceful shutdown 합니다."
printf 'PREVIOUS_BACKEND=%s\nPREVIOUS_FRONTEND=%s\nCURRENT_BACKEND=%s\nCURRENT_FRONTEND=%s\n' \
  "$ACTIVE_BACKEND" "$ACTIVE_FRONTEND" "$TARGET_BACKEND" "$TARGET_FRONTEND" > "$STATE_DIR/previous.env"
printf 'ACTIVE_BACKEND=%s\nACTIVE_FRONTEND=%s\n' \
  "$TARGET_BACKEND" "$TARGET_FRONTEND" > "$STATE_DIR/active.env"

if container_running "$ACTIVE_BACKEND"; then
  compose stop -t "$BACKEND_STOP_TIMEOUT_SECONDS" "$ACTIVE_BACKEND" \
    || log "기존 Backend 중지에 실패했습니다. 새 release는 활성 상태로 유지합니다."
fi
if container_running "$ACTIVE_FRONTEND"; then
  compose stop "$ACTIVE_FRONTEND" \
    || log "기존 Frontend 중지에 실패했습니다. 새 release는 활성 상태로 유지합니다."
fi

trap - ERR
log "배포 완료: $TARGET_FRONTEND / $TARGET_BACKEND"
