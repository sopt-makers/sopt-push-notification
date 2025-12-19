package com.sopt.push.service;

import com.sopt.push.common.Constants;
import com.sopt.push.common.ExternalException;
import com.sopt.push.domain.DeviceTokenEntity;
import com.sopt.push.domain.UserEntity;
import com.sopt.push.enums.Platform;
import com.sopt.push.repository.DeviceTokenRepository;
import com.sopt.push.repository.UserRepository;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UserService {

  private final DeviceTokenRepository deviceTokenRepository;
  private final UserRepository userRepository;
  private final SnsService snsService;

  public void registerToken(String deviceToken, Platform platform, String userId) {
    Optional<DeviceTokenEntity> existingTokenOptional = findDeviceTokenByToken(deviceToken);

    if (existingTokenOptional.isPresent()) {
      DeviceTokenEntity existingToken = existingTokenOptional.get();
      String existingUserId = extractUserIdFromSk(existingToken.getSk());
      boolean userChanged = hasUserChanged(existingUserId, userId);

      if (!userChanged) {
        return;
      }
      unregisterToken(existingToken);
    }

    var endpointResponse = snsService.registerEndpoint(deviceToken, platform, userId);
    String endpointArn = endpointResponse.endpointArn();
    boolean invalidEndpointArn = endpointArn == null || endpointArn.isBlank();
    if (invalidEndpointArn) {
      log.error("endpointArn is undefined");
      throw new ExternalException("endpointArn is undefined");
    }

    var subscribeResponse = snsService.subscribe(endpointArn);
    String subscriptionArn = subscribeResponse.subscriptionArn();
    boolean invalidSubscriptionArn = subscriptionArn == null || subscriptionArn.isBlank();
    if (invalidSubscriptionArn) {
      log.error("subscriptionArn is undefined");
      throw new ExternalException("subscriptionArn is undefined");
    }

    String actualUserId = userId != null && !userId.isBlank() ? userId : Constants.UNKNOWN_USER;
    String createdAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

    saveTokenEntities(deviceToken, platform, endpointArn, subscriptionArn, actualUserId, createdAt);
  }

  public void unregisterToken(DeviceTokenEntity deviceTokenEntity) {
    String subscriptionArn = deviceTokenEntity.getSubscriptionArn();
    String endpointArn = deviceTokenEntity.getEndpointArn();
    String pk = deviceTokenEntity.getPk();
    String sk = deviceTokenEntity.getSk();

    snsService.unsubscribe(subscriptionArn);
    snsService.cancelEndpoint(endpointArn);
    deviceTokenRepository.delete(pk, sk);

    String userId = extractUserIdFromSk(sk);
    String userPk = Constants.USER_PREFIX + userId;
    String userSk = Constants.TOKEN_PREFIX + extractTokenFromPk(pk);
    userRepository.delete(userPk, userSk);
  }

  private Optional<DeviceTokenEntity> findDeviceTokenByToken(String deviceToken) {
    String pk = Constants.TOKEN_PREFIX + deviceToken;
    return deviceTokenRepository.findByPk(pk);
  }

  private boolean hasUserChanged(String existingUserId, String newUserId) {
    boolean sameUserId = existingUserId != null && existingUserId.equals(newUserId);
    if (sameUserId) {
      return false;
    }

    boolean newUserIdIsUnknown = newUserId == null || newUserId.isBlank();
    boolean existingUserIdIsUnknown = Constants.UNKNOWN_USER.equals(existingUserId);
    boolean bothUnknown = newUserIdIsUnknown && existingUserIdIsUnknown;
    return !bothUnknown;
  }

  private String extractUserIdFromSk(String sk) {
    boolean invalidSk = sk == null || !sk.contains(Constants.DELIMITER);
    if (invalidSk) {
      return null;
    }
    String[] parts = sk.split(Constants.DELIMITER, 2);
    boolean validParts = parts.length == 2;
    return validParts ? parts[1] : null;
  }

  private String extractTokenFromPk(String pk) {
    boolean invalidPk = pk == null || !pk.startsWith(Constants.TOKEN_PREFIX);
    if (invalidPk) {
      return null;
    }
    return pk.substring(Constants.TOKEN_PREFIX.length());
  }

  private void saveTokenEntities(
      String deviceToken,
      Platform platform,
      String endpointArn,
      String subscriptionArn,
      String userId,
      String createdAt) {

    String userPk = Constants.USER_PREFIX + userId;
    String userSk = Constants.TOKEN_PREFIX + deviceToken;
    String tokenPk = Constants.TOKEN_PREFIX + deviceToken;
    String tokenSk = Constants.USER_PREFIX + userId;

    UserEntity userEntity = new UserEntity();
    userEntity.setPk(userPk);
    userEntity.setSk(userSk);
    userEntity.setEntity(Constants.USER_ENTITY);
    userEntity.setPlatform(platform.getValue());
    userEntity.setEndpointArn(endpointArn);
    userEntity.setSubscriptionArn(subscriptionArn);
    userEntity.setCreatedAt(createdAt);

    DeviceTokenEntity deviceTokenEntity = new DeviceTokenEntity();
    deviceTokenEntity.setPk(tokenPk);
    deviceTokenEntity.setSk(tokenSk);
    deviceTokenEntity.setEntity(Constants.DEVICE_TOKEN_ENTITY);
    deviceTokenEntity.setPlatform(platform.getValue());
    deviceTokenEntity.setEndpointArn(endpointArn);
    deviceTokenEntity.setSubscriptionArn(subscriptionArn);
    deviceTokenEntity.setCreatedAt(createdAt);

    userRepository.save(userEntity);
    deviceTokenRepository.save(deviceTokenEntity);
  }
}
