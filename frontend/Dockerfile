# Step 1: React 빌드 (Node.js 환경)
FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Step 2: Nginx로 정적 파일 서빙 (프로덕션 환경 - 보안 강화 버전)
FROM nginxinc/nginx-unprivileged:alpine

ARG VCS_REF=local
ARG BUILD_DATE=unknown
LABEL org.opencontainers.image.source="QuizAi" \
      org.opencontainers.image.revision="$VCS_REF" \
      org.opencontainers.image.created="$BUILD_DATE"

# 빌드된 결과물(dist 폴더)을 Nginx의 웹 루트로 복사
COPY --from=builder /app/out /usr/share/nginx/html

# 운영 Frontend는 정적 파일만 제공하고 API 프록시는 Gateway가 담당합니다.
COPY nginx/static.conf /etc/nginx/conf.d/default.conf

# 비권한 이미지는 기본적으로 8080 포트를 사용합니다.
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=6 \
    CMD wget -qO- http://127.0.0.1:8080/healthz | grep -q '^ok$' || exit 1
CMD ["nginx", "-g", "daemon off;"]
