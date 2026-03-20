# Step 1: Build Stage
FROM eclipse-temurin:17-jdk-jammy as builder
WORKDIR /app

# Gradle 래퍼와 핵심 설정 파일들을 먼저 복사하여 종속성 캐싱
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Gradle 자체와 의존성들을 미리 다운로드 (소스 코드가 바뀌어도 이 단계는 캐싱됨)
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사 후 빌드 (clean 제거하여 속도 향상)
COPY src src
RUN ./gradlew build -x test --no-daemon

# Step 2: Runtime Stage
FROM eclipse-temurin:17-jdk-jammy

# 2. 필수 패키지 설치 (Python3, PIP, FFMPEG)
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# 3. yt-dlp 설치 (PyPI에서 최신 버전 정보를 확인하여 캐시 무효화)
ADD https://pypi.org/pypi/yt-dlp/json /tmp/yt-dlp-version.json
RUN python3 -m pip install -U yt-dlp && rm /tmp/yt-dlp-version.json

# 3-1. Non-root 사용자 생성
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# 4. 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 5. 임시 파일 저장소 생성 및 권한 부여
RUN mkdir -p temp/video && chown -R appuser:appuser /app

# 6. 사용자 전환
USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]