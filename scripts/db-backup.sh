#!/usr/bin/env bash
set -Eeuo pipefail

DB_CONTAINER="${DB_CONTAINER:-quizAi-db}"
BACKUP_DIR="${BACKUP_DIR:-./backups/mysql}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"

case "$BACKUP_DIR" in
  ""|"/"|"."|"..")
    printf '[backup] 안전하지 않은 BACKUP_DIR입니다: %s\n' "$BACKUP_DIR" >&2
    exit 1
    ;;
esac

if ! [[ "$BACKUP_RETENTION_DAYS" =~ ^[0-9]+$ ]]; then
  printf '[backup] BACKUP_RETENTION_DAYS는 0 이상의 정수여야 합니다.\n' >&2
  exit 1
fi

if ! docker inspect "$DB_CONTAINER" >/dev/null 2>&1; then
  printf '[backup] MySQL 컨테이너를 찾을 수 없습니다: %s\n' "$DB_CONTAINER" >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR"
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
backup_file="$BACKUP_DIR/quizai-${timestamp}.sql.gz"

printf '[backup] MySQL 백업을 생성합니다: %s\n' "$backup_file"
docker exec "$DB_CONTAINER" /bin/sh -c '
  exec mysqldump \
    -uroot \
    -p"$MYSQL_ROOT_PASSWORD" \
    --single-transaction \
    --routines \
    --triggers \
    --events \
    --hex-blob \
    --set-gtid-purged=OFF \
    --no-tablespaces \
    --databases "$MYSQL_DATABASE"
' | gzip -9 > "$backup_file"

gzip -t "$backup_file"

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  printf 'backup_file=%s\n' "$backup_file" >> "$GITHUB_OUTPUT"
fi

if [ "$BACKUP_RETENTION_DAYS" -gt 0 ]; then
  find "$BACKUP_DIR" -type f -name 'quizai-*.sql.gz' -mtime "+$BACKUP_RETENTION_DAYS" -delete
fi

printf '[backup] 백업 완료: %s\n' "$backup_file"
