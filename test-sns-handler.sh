#!/bin/bash

# SnsHandler 로컬 테스트 스크립트

set -e

echo "🚀 SnsHandler 로컬 테스트 시작"

# 1. 빌드 확인
if [ ! -f "build/libs/app.jar" ]; then
    echo "📦 JAR 파일이 없습니다. 빌드를 시작합니다..."
    ./gradlew shadowJar
else
    echo "✅ JAR 파일이 존재합니다."
fi

# 2. params-dev.json 확인
if [ ! -f "params-dev.json" ]; then
    echo "⚠️  params-dev.json 파일이 없습니다."
    echo "params-dev.json 파일을 생성하고 실제 값으로 채워넣으세요."
    exit 1
fi

# 3. 이벤트 파일 확인
EVENT_FILE=${1:-"events/sns-event-single.json"}
if [ ! -f "$EVENT_FILE" ]; then
    echo "❌ 이벤트 파일을 찾을 수 없습니다: $EVENT_FILE"
    exit 1
fi

echo "📄 이벤트 파일: $EVENT_FILE"

# 4. SAM CLI 확인
if ! command -v sam &> /dev/null; then
    echo "❌ SAM CLI가 설치되어 있지 않습니다."
    echo "설치 방법: brew install aws-sam-cli 또는 pip install aws-sam-cli"
    exit 1
fi

# 5. 테스트 실행
echo "🧪 SnsHandler 테스트 실행 중..."
sam local invoke SnsHandlerFunction \
  --event "$EVENT_FILE" \
  --env-vars params-dev.json \
  --profile sopt-platform \
  --debug

echo "✅ 테스트 완료!"

