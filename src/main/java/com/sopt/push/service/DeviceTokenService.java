package com.sopt.push.service;

import static com.sopt.push.common.Constants.DEVICE_TOKEN_ENTITY;
import static com.sopt.push.common.Constants.TOKEN_PREFIX;
import static com.sopt.push.common.Constants.USER_PREFIX;

import com.sopt.push.domain.DeviceTokenEntity;
import com.sopt.push.dto.UserTokenInfoDto;
import com.sopt.push.enums.Platform;
import com.sopt.push.repository.DeviceTokenRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeviceTokenService {

  private final DeviceTokenRepository deviceTokenRepository;

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

  public DeviceTokenEntity findTokenByDeviceTokenAndUserId(String deviceToken, String userId) {
    String tokenPk = TOKEN_PREFIX + deviceToken;
    String userSk = USER_PREFIX + userId;
    return deviceTokenRepository.findByPkAndSk(tokenPk, userSk).orElse(null);
  }

  public List<DeviceTokenEntity> findUserByTokenIds(List<String> deviceTokens) {
    List<DeviceTokenEntity> result = new ArrayList<>();
    for (String deviceToken : deviceTokens) {
      deviceTokenRepository.findByDeviceToken(deviceToken).ifPresent(result::add);
    }
    return result;
  }

  public Optional<DeviceTokenEntity> findByDeviceToken(String deviceToken) {
    return deviceTokenRepository.findByDeviceToken(deviceToken);
  }

  public UserTokenInfoDto mapDeviceTokenEntityToInfoDto(DeviceTokenEntity deviceTokenEntity) {
    String deviceToken =
        deviceTokenEntity.getPk().startsWith(TOKEN_PREFIX)
            ? deviceTokenEntity.getPk().substring(TOKEN_PREFIX.length())
            : deviceTokenEntity.getPk();
    String userId =
        deviceTokenEntity.getSk().startsWith(USER_PREFIX)
            ? deviceTokenEntity.getSk().substring(USER_PREFIX.length())
            : deviceTokenEntity.getSk();

    return new UserTokenInfoDto(
        userId,
        deviceToken,
        deviceTokenEntity.getEndpointArn(),
        Platform.fromValue(deviceTokenEntity.getPlatform()),
        deviceTokenEntity.getSubscriptionArn());
  }
}
