package com.sopt.push.service;

import static com.sopt.push.common.Constants.DEVICE_TOKEN_ENTITY;
import static com.sopt.push.common.Constants.TOKEN_PREFIX;
import static com.sopt.push.common.Constants.USER_ENTITY;
import static com.sopt.push.common.Constants.USER_PREFIX;

import com.sopt.push.common.DeviceTokenException;
import com.sopt.push.common.ErrorMessage;
import com.sopt.push.config.SnsFactory;
import com.sopt.push.domain.DeviceTokenEntity;
import com.sopt.push.domain.UserEntity;
import com.sopt.push.dto.UserTokenInfoDto;
import com.sopt.push.enums.Platform;
import com.sopt.push.repository.DeviceTokenRepository;
import com.sopt.push.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DeviceTokenService {

  private final DeviceTokenRepository deviceTokenRepository;
  private final UserRepository userRepository;
  private final SnsFactory snsFactory;

  public DeviceTokenService(
      DeviceTokenRepository deviceTokenRepository,
      UserRepository userRepository,
      SnsFactory snsFactory) {
    this.deviceTokenRepository = deviceTokenRepository;
    this.userRepository = userRepository;
    this.snsFactory = snsFactory;
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
    deleteUser(userId, deviceToken);
  }

  public DeviceTokenEntity findTokenByDeviceTokenAndUserId(String deviceToken, String userId) {
    String tokenPk = TOKEN_PREFIX + deviceToken;
    String userSk = USER_PREFIX + userId;
    return deviceTokenRepository.findByPkAndSk(tokenPk, userSk).orElse(null);
  }

  public void registerToken(String deviceToken, Platform platform, String userId) {
    String actualUserId = userId != null ? userId : "unknown";
    String tokenPk = TOKEN_PREFIX + deviceToken;
    List<DeviceTokenEntity> existingTokens = deviceTokenRepository.queryByPk(tokenPk);

    if (!existingTokens.isEmpty()) {
      DeviceTokenEntity existing = existingTokens.get(0);
      String existingUserId = extractUserIdFromSk(existing.getSk());
      if (!changedUserPayload(existingUserId, actualUserId)) {
        return;
      }
    }

    var endpoint = snsFactory.registerEndPoint(deviceToken, platform, userId);

    String endpointArn = endpoint.endpointArn();
    if (endpointArn == null || endpointArn.isBlank()) {
      throw new DeviceTokenException(ErrorMessage.ENDPOINT_ARN_UNDEFINED);
    }

    var sub = snsFactory.subscribe(endpointArn);

    String subscriptionArn = sub.subscriptionArn();
    if (subscriptionArn == null || subscriptionArn.isBlank()) {
      throw new DeviceTokenException(ErrorMessage.SUBSCRIPTION_ARN_UNDEFINED);
    }

    createToken(actualUserId, deviceToken, platform.getValue(), endpointArn, subscriptionArn);
    saveUserEntity(actualUserId, deviceToken, platform.getValue(), endpointArn, subscriptionArn);
  }

  private void saveUserEntity(
      String userId,
      String deviceToken,
      String platform,
      String endpointArn,
      String subscriptionArn) {
    String userPk = USER_PREFIX + userId;
    String tokenSk = TOKEN_PREFIX + deviceToken;

    UserEntity userEntity = new UserEntity();
    userEntity.setPk(userPk);
    userEntity.setSk(tokenSk);
    userEntity.setEntity(USER_ENTITY);
    userEntity.setPlatform(platform);
    userEntity.setEndpointArn(endpointArn);
    userEntity.setSubscriptionArn(subscriptionArn);
    userEntity.setCreatedAt(Instant.now().toString());

    userRepository.save(userEntity);
  }

  public void deleteUser(String userId, String deviceToken) {
    String userPk = USER_PREFIX + userId;
    String tokenSk = TOKEN_PREFIX + deviceToken;
    userRepository.delete(userPk, tokenSk);
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

  private String extractUserIdFromSk(String sk) {
    if (sk.startsWith(USER_PREFIX)) {
      return sk.substring(USER_PREFIX.length());
    }
    return sk;
  }

  private String extractDeviceTokenFromPk(String pk) {
    if (pk.startsWith(TOKEN_PREFIX)) {
      return pk.substring(TOKEN_PREFIX.length());
    }
    return pk;
  }

  public List<DeviceTokenEntity> findUserByTokenIds(List<String> deviceTokens) {
    List<DeviceTokenEntity> result = new ArrayList<>();
    for (String deviceToken : deviceTokens) {
      deviceTokenRepository.findByDeviceToken(deviceToken).ifPresent(result::add);
    }
    return result;
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
