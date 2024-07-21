# SOPT 알림 서버


## Script

운영상의 편의를 위해 작성된 스크립트들을 모아놓은 곳입니다.
### Platform(iOS, Android) 별로 DeviceToken을 초기화 시킨다
Apns 또는 firebase 자격증명이 변경되면 SNS [PlatformApplication을 자격증명을 다시 업데이트 하고](https://www.notion.so/sopt-makers/iOS-dev-d1bb5c7d827e4aadb9de87022d840fda?pvs=4) DeviceToken을 다시 갱신시켜야 합니다
DeviceToken을 갱신시키는 방법은 사실상 불가능한 상태라서 DeviceToken을 초기화 시킨 후 DeviceToken을 재 등록 하는 방향으로 풀어나가야 합니다.
이 스크립트는 Platform별로 DeviceToken을 초기화 시키는 스크립트입니다.

이 스크립트를 사용하기 위해 아래 권한을 가진 AWS 계정과 Credential이 필요합니다.
role의 권한은 아래와 같습니다. 담당자에게 키를 발급 받은 후 env.sample 형식에 맞게 credential을 작성해주세요.
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "sns:Unsubscribe",
        "sns:ListEndpointsByPlatformApplication",
        "sns:ListSubscriptionsByTopicCommand",
        "sns:DeleteEndpoint",
        "dynamodb:DeleteItem"
      ],
      "Resource": "*"
    }
  ]
}
```

```bash
// 운영 환경 토큰 삭제
 npx cross-env NODE_ENV=prod ts-node scripts/removeToken.ts
 
 // 개발 환경 토큰 삭제
 npx cross-env NODE_ENV=dev ts-node scripts/removeToken.ts
```
기본 설정은 iOS로 되어있으며, Android로 변경하고 싶다면 아래와 같이 코드를 수정 후 실행하면 됩니다.
```ts
void (async () => {
    const snsClient = new SNSClient({
        region: 'ap-northeast-2',
        credentials: credentials,
    });
    const ddbClient = new DynamoDBClient({
        region: 'ap-northeast-2',
        credentials: credentials,
    });

    //플랫폼별 디바이스 토큰을 모두 삭제한다
    await removeToken(snsClient, ddbClient, 'android');
})();
```

