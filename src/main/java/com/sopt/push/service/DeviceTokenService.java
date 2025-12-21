package com.sopt.push.service;

import static com.sopt.push.common.Constants.DEVICE_TOKEN_ENTITY;
import static com.sopt.push.common.Constants.TOKEN_PREFIX;
import static com.sopt.push.common.Constants.USER_PREFIX;

import com.sopt.push.domain.DeviceTokenEntity;
import com.sopt.push.repository.DeviceTokenRepository;
import java.time.Instant;

public class DeviceTokenService {

  private final DeviceTokenRepository deviceTokenRepository;

  public DeviceTokenService(DeviceTokenRepository deviceTokenRepository) {
    this.deviceTokenRepository = deviceTokenRepository;
  }

  public void createToken(
      String userId,
      String deviceToken,
      String platform,
      String endpointArn,
      String subscriptionArn) {
    String userPk = USER_PREFIX + userId;
    String tokenPk = TOKEN_PREFIX + deviceToken;
    Instant now = Instant.now();

    DeviceTokenEntity deviceTokenEntity = new DeviceTokenEntity();
    deviceTokenEntity.setPk(tokenPk);
    deviceTokenEntity.setSk(userPk);
    deviceTokenEntity.setEntity(DEVICE_TOKEN_ENTITY);
    deviceTokenEntity.setPlatform(platform);
    deviceTokenEntity.setEndpointArn(endpointArn);
    deviceTokenEntity.setSubscriptionArn(subscriptionArn);
    deviceTokenEntity.setCreatedAt(now.toString());
    deviceTokenRepository.save(deviceTokenEntity);
  }

  public void deleteToken(String userId, String deviceToken) {
    String tokenPk = TOKEN_PREFIX + deviceToken;
    String userSk = USER_PREFIX + userId;

    deviceTokenRepository.delete(tokenPk, userSk);
  }
}
