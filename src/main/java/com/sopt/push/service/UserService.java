package com.sopt.push.service;

import static com.sopt.push.common.Constants.TOKEN_PREFIX;
import static com.sopt.push.common.Constants.USER_PREFIX;

import com.sopt.push.domain.UserEntity;
import com.sopt.push.dto.UserTokenInfoDto;
import com.sopt.push.enums.Platform;
import com.sopt.push.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserService {

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public Set<UserTokenInfoDto> findTokenByUserIds(Set<String> userIds) {
    Set<UserTokenInfoDto> allUserTokens = new HashSet<>();

    for (String userId : userIds) {
      String pk = USER_PREFIX + userId;
      List<UserEntity> userEntities = userRepository.queryByPk(pk);

      userEntities.stream().map(this::mapUserEntityToInfoDto).forEach(allUserTokens::add);
    }
    return allUserTokens;
  }

  public void deleteUser(String userId, String deviceToken) {
    String userPk = USER_PREFIX + userId;
    String tokenSk = TOKEN_PREFIX + deviceToken;
    userRepository.delete(userPk, tokenSk);
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
}
