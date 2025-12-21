# SNS 이벤트 파일 수정 가이드

## 필드별 수정 가이드

### 🔴 필수 수정 항목 (실제 테스트를 위해)

#### 1. `Message` 필드 내부의 `Token` (가장 중요!)
```json
"Message": "{\"Token\":\"실제-디바이스-토큰-값\",...}"
```

**수정 방법:**
- DynamoDB의 `DeviceTokenEntity` 테이블에서 실제 디바이스 토큰 조회
- `pk`가 `d#`로 시작하는 레코드에서 `d#`를 제거한 값이 디바이스 토큰
- 예: `pk = "d#abc123def456"` → `Token = "abc123def456"`

**확인 방법:**
```bash
# AWS CLI로 확인
aws dynamodb query \
  --table-name notification-dev \
  --key-condition-expression "pk = :pk" \
  --expression-attribute-values '{":pk":{"S":"d#your-device-token"}}'
```

#### 2. `Message` 필드 내부의 `EndpointArn` (선택사항)
```json
"Message": "{...,\"EndpointArn\":\"실제-엔드포인트-ARN\",...}"
```

**수정 방법:**
- DynamoDB의 `DeviceTokenEntity`에서 `endpointArn` 필드 값 사용
- 또는 `UserEntity`에서 `endpointArn` 필드 값 사용

**예시:**
```json
"EndpointArn": "arn:aws:sns:ap-northeast-2:379013966998:endpoint/APNS/Makers-test-iOS/12345678-1234-1234-1234-123456789012"
```

### 🟡 선택적 수정 항목 (로컬 테스트에서는 예시 값으로도 가능)

#### 3. `MessageId`
- UUID 형식의 메시지 ID
- 로컬 테스트에서는 예시 값으로도 가능
- 실제 값으로 변경하려면: `uuidgen` 명령어 사용

#### 4. `TopicArn`
- SNS Topic ARN (푸시 실패 알림을 받는 Topic)
- 로컬 테스트에서는 예시 값으로도 가능
- 실제 값: AWS 콘솔에서 SNS Topic ARN 확인

#### 5. `EventSubscriptionArn`
- Lambda가 SNS Topic을 구독할 때 생성되는 구독 ARN
- 로컬 테스트에서는 예시 값으로도 가능
- 형식: `arn:aws:sns:{region}:{account-id}:{topic-name}:{subscription-id}`

#### 6. `Timestamp`
- 이벤트 발생 시간 (ISO 8601 형식)
- 로컬 테스트에서는 예시 값으로도 가능
- 현재 시간으로 변경: `date -u +"%Y-%m-%dT%H:%M:%S.000Z"`

### 🟢 수정 불필요 항목

- `EventSource`: 항상 `"aws:sns"`
- `EventVersion`: 항상 `"1.0"`
- `Type`: 항상 `"Notification"`
- `Subject`: 항상 `"Amazon SNS Notification"`
- `SignatureVersion`, `Signature`, `SigningCertUrl`, `UnsubscribeUrl`: 로컬 테스트에서는 `"EXAMPLE"`로 유지 가능
- `MessageAttributes`: 빈 객체 `{}`로 유지

## 실제 수정 예시

### 시나리오 1: 최소 수정 (핵심 테스트)
```json
{
  "Records": [
    {
      "EventSource": "aws:sns",
      "EventVersion": "1.0",
      "EventSubscriptionArn": "arn:aws:sns:ap-northeast-2:123456789012:push-failures:12345678-1234-1234-1234-123456789012",
      "Sns": {
        "Type": "Notification",
        "MessageId": "95df01b4-ee98-5cb9-9903-4c221d41eb5e",
        "TopicArn": "arn:aws:sns:ap-northeast-2:123456789012:push-failures",
        "Subject": "Amazon SNS Notification",
        "Message": "{\"Token\":\"실제-DynamoDB에-존재하는-디바이스-토큰\",\"EndpointArn\":\"arn:aws:sns:ap-northeast-2:379013966998:endpoint/APNS/Makers-test-iOS/실제-엔드포인트-ID\",\"MessageId\":\"95df01b4-ee98-5cb9-9903-4c221d41eb5e\"}",
        "Timestamp": "2024-01-15T12:00:00.000Z",
        "SignatureVersion": "1",
        "Signature": "EXAMPLE",
        "SigningCertUrl": "EXAMPLE",
        "UnsubscribeUrl": "EXAMPLE",
        "MessageAttributes": {}
      }
    }
  ]
}
```

### 시나리오 2: 완전한 실제 값 사용
```json
{
  "Records": [
    {
      "EventSource": "aws:sns",
      "EventVersion": "1.0",
      "EventSubscriptionArn": "arn:aws:sns:ap-northeast-2:379013966998:SOPT-PUSH-FAILURES-DEV:실제-구독-ID",
      "Sns": {
        "Type": "Notification",
        "MessageId": "실제-UUID-생성",
        "TopicArn": "arn:aws:sns:ap-northeast-2:379013966998:SOPT-PUSH-FAILURES-DEV",
        "Subject": "Amazon SNS Notification",
        "Message": "{\"Token\":\"실제-디바이스-토큰\",\"EndpointArn\":\"실제-엔드포인트-ARN\",\"MessageId\":\"실제-UUID\"}",
        "Timestamp": "2024-12-19T10:30:00.000Z",
        "SignatureVersion": "1",
        "Signature": "EXAMPLE",
        "SigningCertUrl": "EXAMPLE",
        "UnsubscribeUrl": "EXAMPLE",
        "MessageAttributes": {}
      }
    }
  ]
}
```

## 빠른 수정 스크립트

### 디바이스 토큰 조회 (AWS CLI)
```bash
# DynamoDB에서 디바이스 토큰 조회
aws dynamodb scan \
  --table-name notification-dev \
  --filter-expression "begins_with(pk, :prefix)" \
  --expression-attribute-values '{":prefix":{"S":"d#"}}' \
  --limit 1 \
  --query 'Items[0].pk.S' \
  --output text | sed 's/^d#//'
```

### UUID 생성
```bash
# macOS/Linux
uuidgen

# 또는 Python 사용
python3 -c "import uuid; print(uuid.uuid4())"
```

## 테스트 시나리오별 권장 값

### 1. 정상 처리 테스트
- `Token`: DynamoDB에 존재하는 실제 디바이스 토큰
- `EndpointArn`: 해당 토큰과 연결된 실제 엔드포인트 ARN

### 2. 존재하지 않는 토큰 테스트
- `Token`: DynamoDB에 존재하지 않는 임의의 값
- 핸들러가 안전하게 처리하는지 확인

### 3. 잘못된 메시지 형식 테스트
- `Message`에서 `Token` 필드 제거
- 핸들러가 null을 안전하게 처리하는지 확인

## 주의사항

⚠️ **로컬 테스트 시:**
- `Token`만 실제 값으로 변경해도 핵심 로직 테스트 가능
- 나머지 필드는 예시 값으로도 동작 (로컬에서는 실제 SNS와 연결되지 않음)

⚠️ **실제 AWS 환경 테스트 시:**
- 모든 ARN 값들을 실제 값으로 변경 필요
- SNS Topic에 Lambda 함수가 구독되어 있어야 함

