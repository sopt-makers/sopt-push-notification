# 로컬 테스트 가이드

이 문서는 AWS SAM CLI를 사용하여 Lambda 함수를 로컬에서 테스트하는 방법을 설명합니다.

## 사전 요구사항

1. **AWS SAM CLI 설치**
   ```bash
   # macOS
   brew install aws-sam-cli
   
   # 또는 공식 문서 참조
   # https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html
   ```

2. **Docker 설치 및 실행**
   - SAM CLI는 로컬 테스트를 위해 Docker를 사용합니다
   - Docker Desktop이 실행 중이어야 합니다

3. **AWS 자격 증명 설정** (선택사항)
   - 실제 AWS 서비스에 접근할 필요가 없는 경우 생략 가능
   - 필요시 `aws configure` 실행

## 빌드

먼저 프로젝트를 빌드합니다:

```bash
./gradlew build
```

## 환경 변수 설정

로컬 테스트를 위한 환경 변수를 설정합니다. `sam local` 명령어에서 `--env-vars` 옵션을 사용하거나, `env.json` 파일을 생성할 수 있습니다.

### 방법 1: env.json 파일 생성 (권장)

프로젝트 루트에 `env.json` 파일을 생성합니다:

```json
{
  "ApiHandlerFunction": {
    "STAGE": "local",
    "DYNAMODB_TABLE": "sopt-push-notification-local",
    "PLATFORM_APPLICATION_iOS": "arn:aws:sns:ap-northeast-2:123456789012:app/APNS/iOS-App",
    "PLATFORM_APPLICATION_ANDROID": "arn:aws:sns:ap-northeast-2:123456789012:app/GCM/Android-App",
    "ALL_TOPIC_ARN": "arn:aws:sns:ap-northeast-2:123456789012:sopt-push-all",
    "MAKERS_APP_SERVER_URL": "https://api.makers.sopt.org",
    "MAKERS_OPERATION_SERVER_URL": "https://operation.makers.sopt.org"
  }
}
```

### 방법 2: 명령줄에서 직접 전달

```bash
sam local invoke ApiHandlerFunction \
  --env-vars env.json \
  --event events/api-gateway-register.json
```

## API Gateway 이벤트 테스트

### 1. REGISTER 액션 테스트

`events/api-gateway-register.json` 파일을 사용:

```bash
sam local invoke ApiHandlerFunction \
  --env-vars env.json \
  --event events/api-gateway-register.json
```

### 2. CANCEL 액션 테스트

`events/api-gateway-cancel.json` 파일을 사용:

```bash
sam local invoke ApiHandlerFunction \
  --env-vars env.json \
  --event events/api-gateway-cancel.json
```

### 3. SEND 액션 테스트

`events/api-gateway-send.json` 파일을 사용:

```bash
sam local invoke ApiHandlerFunction \
  --env-vars env.json \
  --event events/api-gateway-send.json
```

### 4. SEND_ALL 액션 테스트

`events/api-gateway-send-all.json` 파일을 사용:

```bash
sam local invoke ApiHandlerFunction \
  --env-vars env.json \
  --event events/api-gateway-send-all.json
```

## 로컬 API 서버 실행 (API Gateway 시뮬레이션)

로컬에서 API Gateway를 시뮬레이션하여 HTTP 요청을 테스트할 수 있습니다:

```bash
sam local start-api --env-vars env.json
```

이 명령어를 실행하면 기본적으로 `http://localhost:3000`에서 API가 실행됩니다.

### cURL로 테스트

```bash
# REGISTER
curl -X POST http://localhost:3000/ \
  -H "Content-Type: application/json" \
  -H "action: REGISTER" \
  -H "platform: iOS" \
  -H "transactionId: test-123" \
  -H "service: app" \
  -d '{
    "deviceToken": "test-device-token-123",
    "userIds": ["user-123"]
  }'

# CANCEL
curl -X POST http://localhost:3000/ \
  -H "Content-Type: application/json" \
  -H "action: CANCEL" \
  -H "platform: iOS" \
  -H "transactionId: test-123" \
  -H "service: app" \
  -d '{
    "deviceToken": "test-device-token-123",
    "userIds": ["user-123"]
  }'

# SEND
curl -X POST http://localhost:3000/ \
  -H "Content-Type: application/json" \
  -H "action: SEND" \
  -H "transactionId: test-123" \
  -H "service: app" \
  -d '{
    "userIds": ["user-123"],
    "title": "테스트 제목",
    "content": "테스트 내용",
    "category": "NOTICE",
    "deepLink": "",
    "webLink": "https://makers.sopt.org"
  }'

# SEND_ALL
curl -X POST http://localhost:3000/ \
  -H "Content-Type: application/json" \
  -H "action: SEND_ALL" \
  -H "transactionId: test-123" \
  -H "service: app" \
  -d '{
    "title": "전체 공지",
    "content": "전체 공지 내용",
    "category": "NOTICE",
    "deepLink": "",
    "webLink": "https://makers.sopt.org"
  }'
```

## EventBridge 이벤트 테스트

EventBridge 핸들러를 테스트하려면:

```bash
sam local invoke EventBridgeHandlerFunction \
  --env-vars env.json \
  --event event.json
```

## 디버깅

### 로그 확인

SAM CLI는 실행 중인 Lambda 함수의 로그를 콘솔에 출력합니다. 더 자세한 로그를 보려면:

```bash
sam local invoke ApiHandlerFunction \
  --env-vars env.json \
  --event events/api-gateway-register.json \
  --debug
```

### 포트 변경

로컬 API 서버의 포트를 변경하려면:

```bash
sam local start-api --env-vars env.json --port 8080
```

## 주의사항

1. **DynamoDB**: 로컬 테스트 시 실제 AWS DynamoDB에 접근하려면 AWS 자격 증명이 필요합니다. 또는 DynamoDB Local을 사용할 수 있습니다.

2. **SNS**: 로컬 테스트 시 실제 AWS SNS에 접근하려면 AWS 자격 증명이 필요합니다. 테스트용 ARN을 사용하거나 모킹을 고려하세요.

3. **환경 변수**: `env.json`의 ARN 값들은 실제 AWS 리소스의 ARN으로 변경해야 합니다.

4. **비용**: 실제 AWS 서비스를 사용하는 경우 비용이 발생할 수 있습니다.

## 문제 해결

### Docker 관련 오류
- Docker Desktop이 실행 중인지 확인
- `docker ps` 명령어로 Docker가 정상 작동하는지 확인

### 빌드 오류
- `./gradlew clean build` 실행
- `build/libs/app.jar` 파일이 생성되었는지 확인

### 환경 변수 오류
- `env.json` 파일이 올바른 형식인지 확인
- 필수 환경 변수가 모두 설정되었는지 확인

