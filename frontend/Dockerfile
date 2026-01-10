# Step 1: React 빌드 (Node.js 환경)
FROM node:20-alpine as builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm install
COPY . .
RUN npm run build

# Step 2: Nginx로 정적 파일 서빙 (프로덕션 환경)
FROM nginx:alpine
# 빌드된 결과물(dist 폴더)을 Nginx의 웹 루트로 복사
COPY --from=builder /app/out /usr/share/nginx/html

# Nginx 설정 파일 복사
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY nginx/service-url.inc /etc/nginx/conf.d/service-url.inc

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]