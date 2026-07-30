# Step 1: Build Stage
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Gradle 래퍼와 핵심 설정 파일들을 먼저 복사하여 종속성 캐싱
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Gradle 자체와 의존성들을 미리 다운로드 (소스 코드가 바뀌어도 이 단계는 캐싱됨)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 코드 복사 후 빌드 (clean 제거하여 속도 향상)
COPY src src
RUN ./gradlew build -x test --no-daemon

# Step 2: Runtime Stage
FROM eclipse-temurin:17-jre-noble

ARG VCS_REF=local
ARG BUILD_DATE=unknown
LABEL org.opencontainers.image.source="QuizAi" \
      org.opencontainers.image.revision="$VCS_REF" \
      org.opencontainers.image.created="$BUILD_DATE"

# 2. 필수 패키지 설치 (Python3, venv, FFMPEG, CURL)
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    python3-venv \
    ffmpeg \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 3. yt-dlp 설치 (시스템 Python과 분리된 가상환경 사용)
# PyPI의 최신 버전 정보를 확인하여 yt-dlp 설치 레이어의 캐시를 무효화합니다.
ADD https://pypi.org/pypi/yt-dlp/json /tmp/yt-dlp-version.json
RUN python3 -m venv /opt/yt-dlp \
    && /opt/yt-dlp/bin/python -m pip install --no-cache-dir -U yt-dlp \
    && /opt/yt-dlp/bin/yt-dlp --version | tee /opt/yt-dlp-version.txt \
    && rm /tmp/yt-dlp-version.json

ENV PATH="/opt/yt-dlp/bin:${PATH}"

# 3-1. Non-root 사용자 생성
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# 4. 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 5. 임시 파일 저장소 생성 및 권한 부여
RUN mkdir -p temp/video && chown -R appuser:appuser /app

# 6. 사용자 전환
USER appuser

EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=12 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
