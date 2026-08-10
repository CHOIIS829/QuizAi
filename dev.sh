#!/bin/bash
# 개발 환경 실행 스크립트
# 사용법: ./dev.sh [up|down|build|logs]

COMMAND=${1:-up}

# GitHub Actions와 동일한 순서로 프론트엔드 의존성과 빌드 상태를 검증한다.
verify_frontend() {
  (
    cd frontend || exit 1

    npm ci || exit 1
    npm run audit:prod || exit 1
    npm run lint || exit 1

    # 이전 Next.js 실행에서 남은 추적 파일과 잠금 파일의 권한 문제를 방지한다.
    pkill -f "next dev" >/dev/null 2>&1 || true
    pkill -f "next build" >/dev/null 2>&1 || true
    rm -rf .next out

    npm run build || exit 1
  )
}

if [ "$COMMAND" = "up" ] || [ "$COMMAND" = "build" ]; then
  echo "🔍 Verifying & Building Frontend..."
  if ! verify_frontend; then
    echo "❌ Frontend verification or build failed! Aborting."
    exit 1
  fi
  echo "✅ Frontend verification & build complete."
fi

echo "🚀 Starting Backend & Frontend in DEV mode..."
docker-compose -f docker-compose.dev.yml -p quizai-dev --env-file .env.dev $COMMAND -d --build --force-recreate
