# QuizAi API 개요

이 문서는 프론트엔드와 백엔드가 공유하는 API 계약의 요약입니다. 정확한 엔드포인트, 요청/응답 필드는 실제 Controller와 DTO를 기준으로 유지합니다.

## 기본 원칙

- API 서버는 Spring Boot에서 제공합니다.
- 프론트엔드는 Nginx 또는 환경별 설정을 통해 백엔드 API를 호출합니다.
- 응답은 가능한 한 공통 성공/오류 포맷을 사용합니다.
- 인증이 필요한 API는 JWT 쿠키 또는 인증 컨텍스트를 기준으로 보호합니다.

## 공통 응답 예시

성공 응답 예시:

```json
{
  "success": true,
  "data": {}
}
```

오류 응답 예시:

```json
{
  "success": false,
  "message": "요청을 처리할 수 없습니다.",
  "code": "BAD_REQUEST"
}
```

실제 필드명은 `global.response` 패키지의 구현을 우선합니다.

## 인증 API

관련 위치:

- `backend/src/main/java/com/quizAi/backend/domain/member/controller/AuthController.java`
- `backend/src/main/java/com/quizAi/backend/global/security`

예상 기능:

- Google OAuth 로그인 시작
- OAuth 콜백 처리
- 현재 로그인 상태 확인
- 로그아웃
- 토큰 재발급

주의사항:

- OAuth 리다이렉트 URL은 로컬/운영 환경별로 다를 수 있습니다.
- Kakao OAuth 등록은 현재 비활성화되어 있으며 프론트 버튼은 추후 지원 예정 안내만 표시합니다.
- 쿠키 옵션은 보안, 도메인, SameSite 정책에 영향을 받습니다.

## 회원 API

관련 위치:

- `backend/src/main/java/com/quizAi/backend/domain/member/controller/MemberController.java`

예상 기능:

- 현재 사용자 정보 조회
- 닉네임 설정 또는 변경
- 온보딩 상태 확인

예시 요청:

```http
GET /api/members/me
```

예시 응답:

```json
{
  "id": 1,
  "nickname": "quiz-user",
  "status": "ACTIVE"
}
```

## 퀴즈 생성 API

관련 위치:

- `backend/src/main/java/com/quizAi/backend/domain/quiz/controller/QuizController.java`
- `backend/src/main/java/com/quizAi/backend/domain/quiz/service/QuizService.java`

예상 기능:

- URL 기반 퀴즈 생성 요청
- 생성 Job 상태 조회
- 생성 완료 결과 조회

예시 요청:

```http
POST /api/quizzes/generate
Content-Type: application/json

{
  "url": "https://example.com/article",
  "questionCount": 5
}
```

예시 응답:

```json
{
  "jobId": "quiz-job-id",
  "status": "PROCESSING"
}
```

Job 상태 예시:

- `PROCESSING`
- `COMPLETED`
- `FAILED`

## 저장된 퀴즈 API

관련 위치:

- `backend/src/main/java/com/quizAi/backend/domain/quiz/controller/PersistedQuizController.java`
- `backend/src/main/java/com/quizAi/backend/domain/quiz/service/PersistedQuizService.java`

예상 기능:

- 생성된 퀴즈 저장
- 저장된 퀴즈 목록 조회
- 저장된 퀴즈 상세 조회
- 공개/비공개 퀴즈 조회
- 게스트 퀴즈를 회원 계정으로 가져오기

예시 요청:

```http
GET /api/quizzes
```

예시 응답:

```json
{
  "items": [
    {
      "id": 1,
      "title": "Spring Security 핵심 개념",
      "visibility": "PUBLIC",
      "questionCount": 5
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

## 태그 API

관련 위치:

- `backend/src/main/java/com/quizAi/backend/domain/quiz/entity/TopicTag.java`
- `backend/src/main/java/com/quizAi/backend/domain/quiz/service/TopicTagInitializer.java`

예상 기능:

- 퀴즈 주제 태그 제공
- 퀴즈 목록 필터링에 사용

## API 변경 체크리스트

API 요청/응답을 바꾸는 경우 아래 항목을 확인합니다.

- Controller, DTO, Service가 같은 계약을 사용하고 있는가?
- 프론트엔드 호출부가 새 필드를 반영했는가?
- 인증 필요 여부가 Security 설정과 일치하는가?
- 오류 응답이 사용자에게 노출 가능한 메시지인가?
- `docs/business-rules.md`에 영향을 주는 정책 변경이 있는가?
