package com.sopt.push.service;

import static com.sopt.push.common.Constants.TOKEN_PREFIX;
import static com.sopt.push.common.Constants.USER_PREFIX;

import com.sopt.push.domain.DeviceTokenEntity;
import com.sopt.push.domain.UserEntity;
import com.sopt.push.dto.UserTokenInfoDto;
import com.sopt.push.enums.Platform;
import com.sopt.push.repository.DeviceTokenRepository;
import com.sopt.push.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final DeviceTokenRepository deviceTokenRepository;

  public Set<UserTokenInfoDto> findTokenByUserIds(Set<String> userIds) {
    Set<UserTokenInfoDto> allUserTokens = new HashSet<>();

    for (String userId : userIds) {
      String pk = USER_PREFIX + userId;
      List<UserEntity> userEntities = userRepository.queryByPk(pk);

      userEntities.stream().map(this::mapUserEntityToInfoDto).forEach(allUserTokens::add);
    }
    return allUserTokens;
  }

  private UserTokenInfoDto mapUserEntityToInfoDto(UserEntity userEntity) {
    String userId =
        userEntity.getPk().startsWith(USER_PREFIX)
            ? userEntity.getPk().substring(USER_PREFIX.length())
            : userEntity.getPk();
    String deviceToken =
        userEntity.getSk().startsWith(TOKEN_PREFIX)
            ? userEntity.getSk().substring(TOKEN_PREFIX.length())
            : userEntity.getSk();

    return new UserTokenInfoDto(
        userId,
        deviceToken,
        userEntity.getEndpointArn(),
        Platform.fromValue(userEntity.getPlatform()),
        userEntity.getSubscriptionArn());
  }

  public void deleteUser(String userId, String deviceToken) {
    String userPk = USER_PREFIX + userId;
    String tokenSk = TOKEN_PREFIX + deviceToken;
    userRepository.delete(userPk, tokenSk);
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
