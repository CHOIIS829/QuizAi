# 🧠 QuizAi - 나만의 AI 튜터

<p align="center">
  <img width="500" alt="Image" src="https://github.com/user-attachments/assets/45b9efcd-1e40-4673-ae9a-54daf4fac81f" />
</p>

> **URL 하나로 끝내는 맞춤형 학습.**  
> 기술 블로그, 유튜브 영상을 분석하여 핵심 퀴즈를 자동으로 생성해주는 AI 기반 학습 플랫폼입니다.

<br>

## 🛠 기술 스택

| Category | Tech Stack |
| :--- | :--- |
| **Language** | [![Java](https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=java&logoColor=white)](https://www.java.com/) |
| **Backend** | [![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.8-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot) [![Google Gemini](https://img.shields.io/badge/Google_Gemini-AI-8E75B2?style=for-the-badge&logo=google-bard&logoColor=white)](https://deepmind.google/technologies/gemini/) |
| **Frontend** | [![Next.js](https://img.shields.io/badge/Next.js-16.1-black?style=for-the-badge&logo=next.js&logoColor=white)](https://nextjs.org/) [![React](https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/) [![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-v4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)](https://tailwindcss.com/) |
| **DB** | [![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/) [![Redis](https://img.shields.io/badge/Redis-Cache-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/) |
| **Infra** | [![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/) [![Nginx](https://img.shields.io/badge/Nginx-Proxy-009639?style=for-the-badge&logo=nginx&logoColor=white)](https://nginx.org/) [![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-CI/CD-2088FF?style=for-the-badge&logo=github-actions&logoColor=white)](https://github.com/features/actions) [![Cloudflare](https://img.shields.io/badge/Cloudflare-Tunnel-F38020?style=for-the-badge&logo=cloudflare&logoColor=white)](https://www.cloudflare.com/) |


<br>

## 🔗 Live Demo
👉 **[서비스 바로가기 (https://quizai.co.kr)](https://quizai.co.kr)**


<br>

## � 프로젝트 소개

**QuizAi**는 학습하고 싶은 **기술 블로그**나 **유튜브 영상**의 링크만 입력하면, **Google Gemini AI**가 내용을 정밀 분석하여 퀴즈를 생성해주는 서비스입니다.

단순한 요약을 넘어, 학습자가 내용을 제대로 이해했는지 검증할 수 있는 **문제 풀이 경험**을 제공하는 서비스입니다.

### 🎯 핵심 가치
- **시간 절약**: 긴 영상이나 글을 다 보지 않아도 핵심 내용을 퀴즈로 빠르게 파악.
- **능동적 학습**: 눈으로만 보는 것이 아니라 직접 문제를 풀며 학습.
- **접근성**: 별도의 회원가입 없이 URL 입력만으로 즉시 시작.

<br>

## 📸 서비스 프로세스

### 1. 퀴즈 요청
- URL과 문제 개수를 입력하여 퀴즈를 요청합니다.
<p align="center">
  <img width="600" alt="Image" src="https://github.com/user-attachments/assets/0fd6a527-1753-4d16-8e89-dbb4d57ecaba" />
</p>

### 2. AI 분석 및 퀴즈 생성
Gemini AI가 콘텐츠를 분석하여 즉시 퀴즈를 생성합니다.
<p align="center">
  <img width="600" alt="Image" src="https://github.com/user-attachments/assets/0652be19-4c1d-4266-b91a-a8d7e47a34d1" />
</p>

### 3. 퀴즈 풀이
- 생성된 문제는 4지선다 형식으로 제공되며, 앞 문제를 풀어야 다음 문제로 넘어갈 수 있습니다.
<p align="center">
  <img width="600" alt="Image" src="https://github.com/user-attachments/assets/ad431f99-5f50-49c6-b773-b820ba56cf16" />
</p>

### 4. 결과 확인 및 해설
- 퀴즈를 모두 풀면 정답과 함께 해당 문제에 대한 해설을 확인할 수 있습니다.
<p align="center">
  <img width="45%" alt="Result 1" src="https://github.com/user-attachments/assets/b23bb08e-dd8a-4e09-bbd7-584b36c0ffae" />
  <img width="45%" alt="Result 2" src="https://github.com/user-attachments/assets/197ef5a3-5f3e-4472-ae7b-74734acc4367" />
</p>

<br>

## � 기술 스택

### **Frontend**
| Name | Description |
| :--- | :--- |
| **Next.js 16** | App Router 기반의 서버 사이드 렌더링 및 최신 React 기능 활용 |
| **React 19** | 컴포넌트 기반 UI 개발 |
| **Tailwind CSS v4** | 유틸리티 퍼스트 CSS 프레임워크로 빠르고 일관된 스타일링 |

### **Backend**
| Name | Description |
| :--- | :--- |
| **Java 17 & Spring Boot 3.5** | 안정적이고 확장 가능한 백엔드 서버 구축 |
| **Spring Data JPA & Redis** | MySQL 영속성 관리 및 Redis 캐싱을 통한 성능 최적화 |
| **WebFlux (WebClient)** | Gemini API와의 비동기 논블로킹 통신 처리 |
| **Jsoup & yt-dlp** | 웹 크롤링 및 유튜브 비디오 오디오 추출 및 텍스트 변환 전처리 |

### **Infra & DevOps**
| Name | Description |
| :--- | :--- |
| **Docker & Compose** | 컨테이너 기반의 격리된 실행 환경 구성 |
| **Nginx** | 고정 Gateway 리버스 프록시 및 정적 파일 라우팅 |
| **Shell Scripting** | `deploy.sh`를 활용한 Frontend/Backend Blue/Green 무중단 배포 |

<br>

## 🧩 시스템 아키텍처

<p align="center">
  <img width="800" height="758" alt="Image" src="https://github.com/user-attachments/assets/e16f4b49-bdd4-4bed-9278-442089fc3067" />
</p>

<br>

## � 주요 기능

- **📝 멀티 모달 입력 지원**: 텍스트 기반의 **블로그 포스트**뿐만 아니라 **유튜브 영상** 링크까지 지원하여 다양한 형태의 학습 자료를 처리합니다.
- **⚡ 비동기 이벤트 처리**: `WebFlux`와 `Redis`를 활용한 비동기 작업 처리로 퀴즈 생성 중에도 사용자에게 실시간 진행 상황(대기 상태 등)을 안정적으로 피드백합니다.
- **🔄 무중단 배포**: 고정 Gateway 뒤에서 Frontend와 Backend를 독립적으로 전환하고 실패 시 upstream을 자동 복원하는 **Blue/Green 배포 전략**이 구현되어 있습니다.

<br>

## 📊 퀴즈 생성 플로우차트

```mermaid
sequenceDiagram
    participant Client
    participant Server as Quiz Server
    participant Redis
    participant Gemini as Gemini API
    
    %% 1. 요청 및 초기 응답
    Client->>Server: 1. 퀴즈 생성 요청 (URL)
    Server->>Redis: 2. Job 생성 (상태: PROCESSING)
    Server-->>Client: 3. Job ID 반환 (Non-Blocking)
    
    %% 4. 백그라운드 비동기 작업 시작
    par Async Processing & Polling
        rect rgb(240, 248, 255)
            Note over Server, Gemini: 백그라운드 작업 시작
            alt is Youtube
                Server->>Server: yt-dlp 영상 다운로드 (worst quality)
                Server->>Gemini: 영상 파일 업로드
            else is Blog
                Server->>Server: Jsoup 텍스트 크롤링
            end
            
            loop Processing Wait (Non-Blocking Polling)
               Server->>Gemini: 파일 처리 상태 확인
               Gemini-->>Server: State: PROCESSING / ACTIVE
            end
            
            Server->>Gemini: 5. 퀴즈 생성 요청 (Prompt + Content)
            Gemini-->>Server: 6. JSON 응답 생성
            Server->>Redis: 7. 결과 저장 및 상태 업데이트 (COMPLETED)
        end
        
        rect rgb(255, 250, 240)
            Note over Client, Redis: 클라이언트 폴링
            loop Every 2 Seconds
                Client->>Server: 작업 상태 확인 (Job ID)
                Server->>Redis: 상태 조회
                Redis-->>Client: PROCESSING / COMPLETED
            end
        end
    end
    
    %% 8. 최종 결과 반환
    Client->>Client: 8. 완료 확인 및 결과 렌더링
```


## 🚀 시작하기

로컬 환경에서 프로젝트를 실행해보시려면 다음 단계를 따라주세요.

```bash
# 1. 저장소 클론
git clone https://github.com/your-repo/quizai.git

# 2. 프로젝트 이동
cd quizai

# 3. 환경 변수 설정
# 프로젝트 루트에 .env 파일을 생성하고 아래 형식을 참고하여 작성하세요.
# (GEMINI_API_KEY는 필수입니다)

DATABASE_NAME=quizAi
DATABASE_USERNAME=quizAi
DATABASE_PASSWORD=your_password
DATABASE_ROOT_PASSWORD=your_root_password

GEMINI_API_KEY=your_api_key_here

MYSQL_PORT=3306
REDIS_HOST=localhost
REDIS_PORT=6379

# 4. 개발 모드 실행
./dev.sh up
```

<br>

## 📝 회고
프로젝트 개발 과정에서의 기술적 고민과 해결 과정은 [RETROSPECTIVE.md](./RETROSPECTIVE.md)에서 확인하실 수 있습니다.

<br>

## 📨 Contact
- **Developer**: INSU
- **Email**: cth7097@naver.com
- **GitHub**: [github.com/CHOIIS829](https://github.com/CHOIIS829)


