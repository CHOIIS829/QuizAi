#!/bin/bash
# 개발 환경 실행 스크립트
# 사용법: ./dev.sh [up|down|build|logs]

COMMAND=${1:-up}

echo "Starting Backend & Frontend in DEV mode..."
docker-compose -f docker-compose.dev.yml --env-file .env_dev $COMMAND -d --build
