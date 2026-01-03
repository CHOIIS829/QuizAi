# Step 1: Build Stage
FROM eclipse-temurin:17-jdk-jammy as builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew clean build -x test

# Step 2: Runtime Stage
FROM eclipse-temurin:17-jdk-jammy

# 2. 필수 패키지 설치 (Python3, PIP, FFMPEG)
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    ffmpeg \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 3. yt-dlp 설치
RUN curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp
RUN chmod a+rx /usr/local/bin/yt-dlp

WORKDIR /app

# 4. 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 5. 임시 파일 저장소 생성
RUN mkdir -p temp

ENTRYPOINT ["java", "-jar", "app.jar"]