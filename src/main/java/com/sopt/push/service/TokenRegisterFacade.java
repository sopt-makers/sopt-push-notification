package com.sopt.push.service;

import static com.sopt.push.common.Constants.TOKEN_PREFIX;
import static com.sopt.push.common.Constants.USER_PREFIX;

import com.sopt.push.common.DeviceTokenException;
import com.sopt.push.common.ErrorMessage;
import com.sopt.push.domain.DeviceTokenEntity;
import com.sopt.push.enums.Platform;
import com.sopt.push.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;

@Slf4j
@RequiredArgsConstructor
public class TokenRegisterFacade {

  private final DeviceTokenService deviceTokenService;
  private final UserService userService;
  private final NotificationService notificationService;
  private final DeviceTokenRepository deviceTokenRepository;

  public void register(String deviceToken, Platform platform, String userId) {
    String actualUserId = userId != null ? userId : "unknown";
    String tokenPk = TOKEN_PREFIX + deviceToken;
    java.util.List<DeviceTokenEntity> existingTokens =
        deviceTokenRepository.findAllByDeviceToken(tokenPk);

    // 0. 기존 토큰 체크
    if (!existingTokens.isEmpty()) {
      DeviceTokenEntity deviceTokenEntity = existingTokens.get(0);
      String existingUserId = extractUserId(deviceTokenEntity.getSk());
      if (shouldSkipRegistration(existingUserId, actualUserId)) {
        return;
      }
      deviceTokenService.deleteToken(existingUserId, deviceToken);
    }

    // 1. SNS 엔드포인트 생성
    CreatePlatformEndpointResponse endpoint =
        notificationService.registerEndpoint(deviceToken, platform, actualUserId);

    String endpointArn = endpoint.endpointArn();
    if (endpointArn == null || endpointArn.isBlank()) {
      throw new DeviceTokenException(ErrorMessage.ENDPOINT_ARN_UNDEFINED);
    }

    // 2. SNS 구독
    SubscribeResponse subscription = notificationService.subscribe(endpointArn);

    String subscriptionArn = subscription.subscriptionArn();
    if (subscriptionArn == null || subscriptionArn.isBlank()) {
      throw new DeviceTokenException(ErrorMessage.SUBSCRIPTION_ARN_UNDEFINED);
    }

    // 3. DeviceTokenEntity 저장
    deviceTokenService.registerToken(
        actualUserId, deviceToken, platform.getValue(), endpointArn, subscriptionArn);

    // 4. UserEntity 저장
    userService.registerUser(
        actualUserId, deviceToken, platform.getValue(), endpointArn, subscriptionArn);
  }

  private boolean shouldSkipRegistration(String existingUserId, String newUserId) {
    return !changedUserPayload(existingUserId, newUserId);
  }

  private boolean changedUserPayload(String tokenUserId, String inputUserId) {
    if (inputUserId == null && tokenUserId.equals("unknown")) {
      return false;
    }
    if (inputUserId != null && inputUserId.equals(tokenUserId)) {
      return false;
    }
    return true;
  }

  private String extractUserId(String sk) {
    if (sk.startsWith(USER_PREFIX)) {
      return sk.substring(USER_PREFIX.length());
    }
    return sk;
  }
}
