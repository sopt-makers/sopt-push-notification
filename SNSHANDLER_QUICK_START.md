# SnsHandler 빠른 시작 가이드

## 1. 빌드

```bash
cd sopt-push-notification
./gradlew shadowJar
```

## 2. 환경 변수 설정

`params-dev.json` 파일에 `SnsHandlerFunction` 섹션이 있는지 확인하고, 없으면 추가하세요:

```json
{
  "SnsHandlerFunction": {
    "DYNAMODB_TABLE": "your-dynamodb-table-name",
    "PLATFORM_APPLICATION_iOS": "arn:aws:sns:...",
    "PLATFORM_APPLICATION_ANDROID": "arn:aws:sns:...",
    "ALL_TOPIC_ARN": "arn:aws:sns:...",
    "STAGE": "dev",
    "MAKERS_APP_SERVER_URL": "https://...",
    "MAKERS_OPERATION_SERVER_URL": "https://..."
  }
}
```

## 3. 테스트 이벤트 준비

`events/sns-event-single.json` 파일의 `Message` 필드 내 `Token` 값을 실제 DynamoDB에 존재하는 디바이스 토큰으로 변경하세요.

## 4. 로컬 테스트 실행

```bash
sam local invoke SnsHandlerFunction \
  --event events/sns-event-single.json \
  --env-vars params-dev.json
```

## 5. 결과 확인

- 콘솔에 출력되는 로그 확인
- DynamoDB에서 실패 로그(History) 생성 확인
- 디바이스 토큰이 정리되었는지 확인

## 문제 해결

- **"Handler not found"**: `./gradlew shadowJar` 실행 후 다시 시도
- **"DynamoDB 연결 실패"**: AWS 자격 증명 확인 (`aws configure`)
- **"params-dev.json not found"**: `params-dev.json` 파일이 존재하는지 확인하고, `SnsHandlerFunction` 섹션이 있는지 확인

자세한 내용은 `LOCAL_TESTING_GUIDE.md`를 참고하세요.

