# QuizAi 운영 절차

## 배포 전 필수 조건

- 운영 `.env`에 DB, Redis, Gemini, Google OAuth, JWT 비밀값이 모두 있어야 합니다.
- Kakao OAuth는 현재 비활성화되어 있으며 추후 지원 시 Compose와 Spring Security 설정을 함께 활성화합니다.
- `JWT_SECRET_KEY`는 운영에서 기본값을 사용하지 않으며 누락 시 Compose 렌더링이 실패합니다.
- Cloudflare Tunnel이 같은 호스트에서 동작하는 기본 구성은 Gateway를 `127.0.0.1:90`에 바인딩합니다. 다른 호스트 또는 컨테이너에서 접근한다면 방화벽을 먼저 제한하고 `GATEWAY_BIND_ADDRESS`를 조정합니다.
- 동시에 하나의 배포만 실행할 수 있으며 `.deploy-state/deploy.lock`이 배포 잠금 역할을 합니다.

## DB migration

운영 Hibernate는 `ddl-auto: validate`이며 스키마를 자동 수정하지 않습니다. 모든 운영 변경은 Flyway SQL로 관리합니다.

1. 새 SQL을 `backend/src/main/resources/db/migration/V{번호}__{설명}.sql`로 추가합니다.
2. 기존 Blue와 호환되는 테이블 또는 nullable 컬럼 추가를 우선합니다.
3. 새 Backend가 시작될 때 Flyway가 migration을 한 번 실행합니다.
4. migration 후 Hibernate `validate`가 실패하면 새 Backend는 health check를 통과하지 못하고 Gateway 전환이 취소됩니다.
5. 컬럼 삭제, rename, 강제 `NOT NULL`은 데이터 backfill과 애플리케이션 전환 이후 별도 migration으로 처리합니다.

기존 운영 DB에 Flyway 이력 테이블이 없고 테이블이 이미 존재하면 version 1로 baseline됩니다. 비어 있는 새 DB에서는 `V1__baseline_schema.sql`이 전체 초기 스키마를 생성합니다.

## MySQL 백업

GitHub Actions의 홈 서버 배포 workflow는 `deploy.sh` 실행 전에 압축 백업을 생성·검증하고 14일 보관 GitHub artifact로 서버 밖에 저장합니다. 백업 생성 또는 업로드가 실패하면 배포를 시작하지 않습니다.

별도의 `Backup Production Database` workflow도 매일 한국 시간 오전 3시에 self-hosted runner에서 동일한 백업을 생성합니다. 따라서 정상적인 Actions 배포에서는 사용자가 직접 백업 명령을 실행할 필요가 없습니다.

수동 백업:

```bash
./scripts/db-backup.sh
```

보관 위치와 기간은 `BACKUP_DIR`, `BACKUP_RETENTION_DAYS`로 변경할 수 있습니다. 생성된 파일에는 DB 데이터와 비밀정보가 포함될 수 있으므로 공개 저장소에 업로드하지 않습니다.

## MySQL 복구

복구는 기존 DB를 변경하므로 백업 파일을 먼저 검증하고 명시적인 확인 변수가 필요합니다.

```bash
CONFIRM_RESTORE=quizai ./scripts/db-restore.sh backups/mysql/quizai-YYYYMMDDTHHMMSSZ.sql.gz
```

복구 후 Backend health, Flyway schema history, 주요 사용자·퀴즈 데이터를 확인합니다. 최소 분기 1회 별도 환경에서 실제 복구 테스트를 수행합니다.

## Redis 복구 정책

- AOF와 `appendfsync everysec`로 Redis 재시작 시 손실 범위를 줄입니다.
- 퀴즈 Job은 30분 TTL을 유지합니다.
- 20분 이상 처리 중인 Job은 스케줄러가 `FAILED`로 전환합니다.
- refresh token이 유실되면 사용자는 다시 로그인해야 하며 MySQL 데이터에는 영향을 주지 않습니다.

일반 애플리케이션 배포는 `--no-recreate`로 MySQL과 Redis를 유지합니다. 기존 Redis 컨테이너에 AOF 설정을 처음 반영할 때만 사전 백업 후 별도 점검 시간에 다음 유지보수를 한 번 수행합니다.

```bash
docker compose up -d --force-recreate redis
```

MySQL 이미지 또는 리소스 설정 변경도 일반 배포와 분리된 유지보수 절차로 수행합니다.

## 배포와 롤백

일반 배포:

```bash
./deploy.sh
```

yt-dlp smoke test는 `https://youtu.be/CoyQM_Zi0OM`을 사용합니다. YouTube 자체 장애가 확인된 긴급 상황에서만 다음처럼 명시적으로 우회합니다.

```bash
SKIP_YTDLP_SMOKE_TEST=true ./deploy.sh
```

직전 release 롤백:

```bash
./deploy.sh rollback
```

롤백은 보존된 직전 Frontend/Backend 컨테이너를 다시 시작해 upstream을 복원합니다. DB migration은 자동으로 되돌리지 않으므로 모든 migration은 직전 애플리케이션과 호환되어야 합니다.
