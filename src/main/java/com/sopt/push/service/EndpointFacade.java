package com.sopt.push.service;

import static com.sopt.push.common.Constants.USER_PREFIX;

import com.sopt.push.common.DeviceTokenException;
import com.sopt.push.common.ErrorMessage;
import com.sopt.push.domain.DeviceTokenEntity;
import com.sopt.push.dto.UserTokenInfoDto;
import com.sopt.push.enums.Platform;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;

@Slf4j
@RequiredArgsConstructor
public class EndpointFacade {

  private final DeviceTokenService deviceTokenService;
  private final UserService userService;
  private final NotificationService notificationService;

  public void register(String deviceToken, Platform platform, String inputUserId) {
    Optional<DeviceTokenEntity> existingToken = deviceTokenService.findByDeviceToken(deviceToken);
    checkAndCleanExistingToken(existingToken, inputUserId, deviceToken);

    String endpointArn = createSnsEndpoint(deviceToken, platform, inputUserId);
    String subscriptionArn = subscribeToSnsTopic(endpointArn);

    deviceTokenService.registerToken(
        inputUserId, deviceToken, platform.getValue(), endpointArn, subscriptionArn);
    userService.registerUser(
        inputUserId, deviceToken, platform.getValue(), endpointArn, subscriptionArn);
  }

  public void clean(UserTokenInfoDto token) {
    userService.deleteUser(token.userId(), token.deviceToken());
    deviceTokenService.deleteToken(token.userId(), token.deviceToken());

    try {
      notificationService.deleteEndpoint(token.endpointArn());
      notificationService.unsubscribe(token.subscriptionArn());
    } catch (Exception e) {
      log.error("Failed to delete SNS endpoint: {} - {}", token.endpointArn(), e.getMessage());
    }
  }

  private void checkAndCleanExistingToken(
      Optional<DeviceTokenEntity> existingTokenOpt, String actualUserId, String deviceToken) {
    if (existingTokenOpt.isEmpty()) return;

    DeviceTokenEntity deviceTokenEntity = existingTokenOpt.get();
    String existingUserId = extractUserId(deviceTokenEntity.getSk());
    boolean isSameUserId = actualUserId.equals(existingUserId);
    if (isSameUserId) return;

    UserTokenInfoDto existingToken =
        new UserTokenInfoDto(
            existingUserId,
            deviceToken,
            deviceTokenEntity.getEndpointArn(),
            Platform.fromValue(deviceTokenEntity.getPlatform()),
            deviceTokenEntity.getSubscriptionArn());

    clean(existingToken);
  }

  private String createSnsEndpoint(String deviceToken, Platform platform, String userId) {
    CreatePlatformEndpointResponse endpoint =
        notificationService.registerEndpoint(deviceToken, platform, userId);

    String endpointArn = endpoint.endpointArn();
    boolean isInvalidEndpointArn = endpointArn == null || endpointArn.isBlank();
    if (isInvalidEndpointArn) {
      throw new DeviceTokenException(ErrorMessage.ENDPOINT_ARN_UNDEFINED);
    }
    return endpointArn;
  }

  private String subscribeToSnsTopic(String endpointArn) {
    SubscribeResponse subscription = notificationService.subscribe(endpointArn);

    String subscriptionArn = subscription.subscriptionArn();
    boolean isInvalidSubscriptionArn = subscriptionArn == null || subscriptionArn.isBlank();
    if (isInvalidSubscriptionArn) {
      throw new DeviceTokenException(ErrorMessage.SUBSCRIPTION_ARN_UNDEFINED);
    }
    return subscriptionArn;
  }

  private String extractUserId(String sk) {
    if (sk.startsWith(USER_PREFIX)) {
      return sk.substring(USER_PREFIX.length());
    }
    return sk;
  }
}
