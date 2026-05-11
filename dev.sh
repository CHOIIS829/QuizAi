#!/bin/bash
# 개발 환경 실행 스크립트
# 사용법: ./dev.sh [up|down|build|logs]

COMMAND=${1:-up}

if [ "$COMMAND" = "up" ] || [ "$COMMAND" = "build" ]; then
  echo "🎨 Building Frontend..."
  cd frontend
  # Prevent stale trace/lock permission issues from previous Next.js runs
  pkill -f "next dev" >/dev/null 2>&1 || true
  pkill -f "next build" >/dev/null 2>&1 || true
  rm -rf .next out
  npm run build
  if [ $? -ne 0 ]; then
      echo "❌ Frontend build failed! Aborting."
      exit 1
  fi
  cd ..
  echo "✅ Frontend build complete."
fi

echo "🚀 Starting Backend & Frontend in DEV mode..."
docker-compose -f docker-compose.dev.yml -p quizai-dev --env-file .env.dev $COMMAND -d --build --force-recreate
