#!/bin/bash

# 환경 변수 로드 (필요시)
# source .env

IS_GREEN=$(docker ps | grep backend-green)
DEFAULT_CONF="/etc/nginx/conf.d/service-url.inc"

if [ -z "$IS_GREEN" ];then
  echo "### Blue => Green ###"
  CURRENT_PORT=8090
  TARGET_PORT=8091
  TARGET_SERVICE="backend-green"
  TARGET_UPSTREAM="http://quizAi-backend-green:8080"
  STOP_SERVICE="backend-blue"
else
  echo "### Green => Blue ###"
  CURRENT_PORT=8091
  TARGET_PORT=8090
  TARGET_SERVICE="backend-blue"
  TARGET_UPSTREAM="http://quizAi-backend-blue:8080"
  STOP_SERVICE="backend-green"
fi

echo "1. Start new connection ($TARGET_SERVICE)..."
docker-compose up -d --build $TARGET_SERVICE

# 마운트된 볼륨의 권한을 appuser로 변경 (root 권한 필요)
echo "1-1. Fix volume permissions for $TARGET_SERVICE..."
docker exec -u root $TARGET_SERVICE chown -R appuser:appuser /app/temp/video

echo "2. Health Check..."
for i in {1..15}
do
  sleep 5
  RESPONSE=$(curl -s http://localhost:$TARGET_PORT/actuator/health)
  if [[ "$RESPONSE" == *"UP"* ]]; then
      echo ">> Success!"
      break
  fi
  echo ">> Wait... (Attempt $i/15) - Response: $RESPONSE"

  if [ $i -eq 15 ]; then
    echo ">> Fail... Stopping new service."
    docker-compose logs --tail=100 $TARGET_SERVICE
    docker-compose stop $TARGET_SERVICE
    exit 1
  fi
done

echo "3. Check & Start Frontend..."
# 프론트엔드(Nginx) 컨테이너가 켜져 있는지 확인하고, 없거나 변경사항이 있으면 실행
docker-compose up -d --build frontend

echo "4. Change Nginx Upstream..."
# Nginx 컨테이너 내부의 service-url.inc 파일 내용 변경 (명시적으로 세미콜론 추가)
docker exec quizAi-frontend /bin/sh -c "echo 'set \$service_url $TARGET_UPSTREAM;' > $DEFAULT_CONF"
docker exec quizAi-frontend nginx -t && docker exec quizAi-frontend nginx -s reload

echo "5. Stop old service ($STOP_SERVICE)..."
docker-compose stop $STOP_SERVICE
docker-compose rm -f $STOP_SERVICE

echo "Deploy Finished!"
