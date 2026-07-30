#!/usr/bin/env bash
set -Eeuo pipefail

DB_CONTAINER="${DB_CONTAINER:-quizAi-db}"
backup_file="${1:-}"

if [ -z "$backup_file" ] || [ ! -f "$backup_file" ]; then
  printf '사용법: CONFIRM_RESTORE=quizai %s <backup.sql.gz>\n' "$0" >&2
  exit 1
fi

if [ "${CONFIRM_RESTORE:-}" != "quizai" ]; then
  printf '[restore] 복원은 기존 DB를 변경합니다. CONFIRM_RESTORE=quizai를 명시하세요.\n' >&2
  exit 1
fi

gzip -t "$backup_file"
if ! docker inspect "$DB_CONTAINER" >/dev/null 2>&1; then
  printf '[restore] MySQL 컨테이너를 찾을 수 없습니다: %s\n' "$DB_CONTAINER" >&2
  exit 1
fi

printf '[restore] %s 파일을 %s 컨테이너에 복원합니다.\n' "$backup_file" "$DB_CONTAINER"
gzip -dc "$backup_file" | docker exec -i "$DB_CONTAINER" /bin/sh -c \
  'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD"'
printf '[restore] 복원이 완료되었습니다. 애플리케이션 기동 및 데이터 정합성을 확인하세요.\n'
