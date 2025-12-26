package com.sopt.push.service;

import com.sopt.push.dto.UserTokenInfoDto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InvalidEndpointCleaner {

  private final UserService userService;
  private final DeviceTokenService tokenService;
  private final NotificationService notificationService;

  public InvalidEndpointCleaner(
      UserService userService,
      DeviceTokenService tokenService,
      NotificationService notificationService) {
    this.userService = userService;
    this.tokenService = tokenService;
    this.notificationService = notificationService;
  }

  public void clean(UserTokenInfoDto token) {

    userService.deleteUser(token.userId(), token.deviceToken());
    tokenService.deleteToken(token.userId(), token.deviceToken());

    try {
      notificationService.deleteEndpoint(token.endpointArn());
      notificationService.unsubscribe(token.subscriptionArn());

    } catch (Exception e) {
      log.error("Failed to delete SNS endpoint: {} - {}", token.endpointArn(), e.getMessage());
    }
  }
}
