# QuizAi 아키텍처 개요

이 문서는 QuizAi의 전체 구조를 빠르게 파악하기 위한 초안입니다. 실제 구현이 바뀌면 이 문서도 함께 갱신합니다.

## 전체 구성

QuizAi는 프론트엔드, 백엔드, 데이터 저장소, 외부 AI API로 구성됩니다.

```text
사용자 브라우저
  -> Cloudflare Tunnel
  -> Gateway Nginx
     -> Frontend Blue 또는 Green (Next.js 정적 export)
     -> Backend Blue 또는 Green (Spring Boot API)
  -> MySQL, Redis
  -> Google Gemini API
```

## Backend

위치: `backend/`

주요 기술:

- Java 17
- Spring Boot 3.5
- Spring Security, OAuth2 Client, JWT
- Spring Data JPA
- Redis
- WebFlux WebClient
- Jsoup

패키지 구조 예시:

```text
com.quizAi.backend
├─ domain
│  ├─ gemini
│  ├─ member
│  └─ quiz
└─ global
   ├─ config
   ├─ controller
   ├─ exception
   ├─ response
   └─ security
```

### 주요 책임

- `domain.quiz`: 퀴즈 생성, 조회, 저장, 공개 범위, 태그, Job 상태 관리
- `domain.gemini`: Gemini API 요청/응답 처리
- `domain.member`: 회원, OAuth 계정, 닉네임, 현재 사용자 조회
- `global.security`: OAuth2 로그인, JWT 발급/검증, 인증 필터
- `global.response`: 공통 성공/오류 응답 포맷

## Frontend

위치: `frontend/`

주요 기술:

- Next.js 16
- React 19
- Tailwind CSS v4
- lucide-react

운영 환경은 Frontend Blue/Green 컨테이너가 정적 export 결과물을 서빙합니다. 항상 실행되는 Gateway Nginx가 `.deploy-state/nginx/frontend-url.inc`를 읽어 활성 Frontend를 선택하며, API와 OAuth 경로는 별도의 Backend upstream으로 전달합니다. runtime upstream과 활성 release 상태는 Git checkout에 덮어쓰이지 않도록 `.deploy-state`에 보존합니다.

## Data Stores

### MySQL

영속 데이터 저장소입니다.

예상 저장 대상:

- 사용자
- OAuth 계정
- 저장된 퀴즈
- 퀴즈 문제
- 주제 태그

운영 스키마는 Flyway migration으로 변경하고 Hibernate `validate`로 Entity와의 정합성을 확인합니다. 기존 운영 스키마는 Flyway version 1로 baseline 처리하며, 이후 변경은 `backend/src/main/resources/db/migration` 아래의 버전 SQL로 관리합니다.
AI가 생성하는 문제, 해설, 코드 스니펫과 길이가 유동적인 원본 URL은 MySQL `TEXT`로 저장하여 255바이트 제한으로 인한 저장 실패를 방지합니다.

### Redis

짧은 수명의 상태와 캐시성 데이터를 다룹니다.

예상 저장 대상:

- 퀴즈 생성 Job 상태
- 생성 중/완료 결과
- Rate Limit 관련 카운터
- Refresh Token 또는 인증 보조 데이터

운영 Redis는 AOF(`appendfsync everysec`)를 사용합니다. 20분 이상 `PROCESSING`인 Job은 재시작 또는 처리 시간 초과로 간주하여 `FAILED`로 전환하고, 모든 Job 데이터는 기존 30분 TTL 정책을 유지합니다.

## 퀴즈 생성 흐름

```text
1. 사용자가 URL과 문제 수를 입력한다.
2. 프론트엔드가 백엔드에 퀴즈 생성을 요청한다.
3. 백엔드는 Job을 생성하고 Redis에 PROCESSING 상태를 저장한다.
4. 백엔드는 URL 유형을 판별한다.
   - 블로그: Jsoup으로 본문 텍스트를 추출한다.
   - 유튜브: 영상/음성 처리 후 Gemini에 전달할 자료를 준비한다.
5. 백엔드는 Gemini API에 퀴즈 생성을 요청한다.
6. Gemini 응답을 검증하고 내부 DTO/Entity로 변환한다.
7. Redis Job 상태를 COMPLETED 또는 FAILED로 갱신한다.
8. 프론트엔드는 Job 상태를 폴링해 결과를 화면에 표시한다.
```

## 운영 환경

로컬 개발은 `docker-compose.dev.yml`과 `dev.sh`를 기준으로 합니다.

운영 배포는 `docker-compose.yml`, `deploy.sh`, Gateway Nginx 설정, Cloudflare Tunnel 설정을 함께 확인합니다.

### Blue/Green 배포

1. `deploy.sh`가 runtime upstream 파일에서 현재 Frontend/Backend 색상을 확인합니다.
2. MySQL, Redis, Gateway를 중지하지 않고 상태를 확인합니다.
3. 비활성 Frontend/Backend를 이미지로 기동합니다.
4. Backend health, Frontend health, yt-dlp smoke test를 통과시킵니다.
5. 두 upstream을 변경하고 `nginx -t` 후 Gateway를 reload합니다.
6. Gateway 내부 및 호스트 포트에서 새 release를 검증합니다.
7. 기존 Backend는 비동기 퀴즈 작업을 기다리는 graceful shutdown 후 중지합니다.
8. 실패하면 기존 upstream으로 자동 복원합니다. 직전 release는 `./deploy.sh rollback`으로 다시 활성화할 수 있습니다.

MySQL과 Redis는 상태 저장소이므로 Blue/Green 전환 대상이 아닙니다. migration은 이전 애플리케이션도 사용할 수 있는 확장 변경을 먼저 적용하고, 삭제·이름 변경 같은 축소 변경은 이후 release에서 수행합니다.

백업, 복구, migration과 수동 롤백 절차는 `docs/operations.md`를 따릅니다.

## 변경 시 문서 갱신 기준

- 새로운 도메인 패키지가 생기면 이 문서에 책임을 추가합니다.
- 데이터 저장소 종류나 역할이 바뀌면 Data Stores 섹션을 수정합니다.
- 퀴즈 생성 흐름, 인증 흐름, 배포 흐름이 바뀌면 해당 플로우를 갱신합니다.
