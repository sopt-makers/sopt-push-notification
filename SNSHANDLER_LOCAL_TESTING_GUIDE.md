# SnsHandler 로컬 테스트 가이드

이 가이드는 SAM CLI를 사용하여 로컬에서 SnsHandler Lambda 함수를 테스트하는 방법을 설명합니다.

## 사전 요구사항

1. **SAM CLI 설치**
   ```bash
   # macOS
   brew install aws-sam-cli
   
   # 또는 pip 사용
   pip install aws-sam-cli
   ```

2. **AWS 자격 증명 설정**
   ```bash
   aws configure
   ```
   또는 환경 변수 설정:
   ```bash
   export AWS_ACCESS_KEY_ID=your-access-key
   export AWS_SECRET_ACCESS_KEY=your-secret-key
   export AWS_DEFAULT_REGION=ap-northeast-2
   ```

3. **Java 21 설치 확인**
   ```bash
   java -version
   ```

## 1. 프로젝트 빌드

```bash
cd sopt-push-notification
./gradlew shadowJar
```

빌드가 완료되면 `build/libs/app.jar` 파일이 생성됩니다.

## 2. template.yaml에 SnsHandler 함수 추가

`template.yaml` 파일에 다음 함수를 추가합니다:

```yaml
SnsHandlerFunction:
  Type: AWS::Serverless::Function
  Properties:
    FunctionName: !Sub "sopt-push-notification-lambda-sns-${Stage}"
    Handler: com.sopt.push.lambda.SnsHandler::handleRequest
    CodeUri: build/libs/app.jar
    Role: !GetAtt PushLambdaRole.Arn
    Events:
      SnsEvent:
        Type: SNS
        Properties:
          Topic: !Ref PushFailuresTopic
```

또는 SNS 이벤트 없이 직접 invoke할 수 있도록 이벤트 없이 정의할 수도 있습니다:

```yaml
SnsHandlerFunction:
  Type: AWS::Serverless::Function
  Properties:
    FunctionName: !Sub "sopt-push-notification-lambda-sns-${Stage}"
    Handler: com.sopt.push.lambda.SnsHandler::handleRequest
    CodeUri: build/libs/app.jar
    Role: !GetAtt PushLambdaRole.Arn
```

## 3. 로컬 테스트 방법

### 방법 1: SAM CLI로 직접 invoke (권장)

```bash
# 단일 레코드 테스트
sam local invoke SnsHandlerFunction \
  --event events/sns-event-single.json \
  --env-vars params-dev.json

# 여러 레코드 테스트
sam local invoke SnsHandlerFunction \
  --event events/sns-event-sample.json \
  --env-vars params-dev.json
```

### 방법 2: 환경 변수 파일 확인

`params-dev.json` 파일에 `SnsHandlerFunction` 섹션이 있는지 확인합니다. 다른 handler 개발자들과 통일된 형식을 사용합니다.

기존 `params-dev.json`에 `SnsHandlerFunction` 섹션을 추가하세요:

```json
{
  "EventBridgeHandlerFunction": {
    
  },
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

`EventBridgeHandlerFunction`과 동일한 환경 변수를 사용하거나, 필요에 따라 다르게 설정할 수 있습니다.

### 방법 3: DynamoDB Local 사용 (선택사항)

로컬 DynamoDB를 사용하려면:

```bash
# DynamoDB Local 실행
docker run -p 8000:8000 amazon/dynamodb-local

# SAM local invoke 시 DynamoDB 엔드포인트 지정
sam local invoke SnsHandlerFunction \
  --event events/sns-event-single.json \
  --env-vars params-dev.json \
  --docker-network host \
  --parameter-overrides DynamoDbEndpoint=http://localhost:8000
```

## 4. 테스트 이벤트 파일 커스터마이징

`events/sns-event-single.json` 파일을 수정하여 실제 테스트 시나리오에 맞게 변경할 수 있습니다:

- `Message` 필드의 `Token` 값을 실제 DynamoDB에 존재하는 디바이스 토큰으로 변경
- 여러 레코드를 추가하여 배치 처리 테스트
- 잘못된 형식의 메시지로 에러 처리 테스트

## 5. 실제 AWS 환경에서 테스트

### SNS Topic에 Lambda 함수 구독

1. AWS 콘솔에서 SNS Topic 생성
2. Lambda 함수를 구독자로 추가
3. 푸시 알림 실패 시 자동으로 Lambda 함수가 트리거됨

### 수동으로 SNS 이벤트 발행

```bash
aws sns publish \
  --topic-arn arn:aws:sns:ap-northeast-2:123456789012:push-failures \
  --message '{"Token":"test-device-token-12345","EndpointArn":"arn:aws:sns:ap-northeast-2:123456789012:endpoint/APNS/test-app/test-endpoint-123"}'
```

## 6. 디버깅 팁

### 로그 확인

```bash
# SAM local invoke 실행 시 로그가 콘솔에 출력됩니다
sam local invoke SnsHandlerFunction \
  --event events/sns-event-single.json \
  --env-vars params-dev.json \
  --debug
```

### CloudWatch Logs 확인 (배포 후)

AWS 콘솔에서 Lambda 함수의 CloudWatch Logs를 확인하여 실행 로그를 볼 수 있습니다.

## 7. 테스트 시나리오

### 시나리오 1: 정상 처리
- DynamoDB에 존재하는 디바이스 토큰으로 테스트
- 실패 로그가 생성되고 토큰이 정리되는지 확인

### 시나리오 2: 존재하지 않는 토큰
- DynamoDB에 존재하지 않는 디바이스 토큰으로 테스트
- 핸들러가 정상적으로 처리하고 에러 없이 종료되는지 확인

### 시나리오 3: 잘못된 메시지 형식
- `Message` 필드에 `Token`이 없는 경우
- 핸들러가 안전하게 처리하는지 확인

### 시나리오 4: 배치 처리
- 여러 레코드를 포함한 이벤트로 테스트
- 각 레코드가 독립적으로 처리되는지 확인

## 8. 문제 해결

### 문제: "Handler not found"
- `Handler` 경로가 정확한지 확인: `com.sopt.push.lambda.SnsHandler::handleRequest`
- JAR 파일이 올바르게 빌드되었는지 확인: `./gradlew shadowJar`

### 문제: "ClassNotFoundException"
- `shadowJar` 태스크가 모든 의존성을 포함하는지 확인
- `build/libs/app.jar` 파일 크기가 충분한지 확인

### 문제: "DynamoDB 연결 실패"
- AWS 자격 증명이 올바르게 설정되었는지 확인
- DynamoDB 테이블이 존재하고 접근 권한이 있는지 확인

### 문제: "SNS 권한 오류"
- Lambda 실행 역할에 필요한 SNS 권한이 있는지 확인
- `template.yaml`의 IAM 역할 설정 확인

