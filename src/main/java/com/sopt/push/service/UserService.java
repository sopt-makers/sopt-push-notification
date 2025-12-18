package com.sopt.push.service;

import com.sopt.push.domain.DeviceTokenEntity;
import com.sopt.push.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserService {

  private final DeviceTokenRepository deviceTokenRepository;
  private final SnsService snsService;

  public void unregisterToken(DeviceTokenEntity deviceTokenEntity) {
    String subscriptionArn = deviceTokenEntity.getSubscriptionArn();
    String endpointArn = deviceTokenEntity.getEndpointArn();
    String pk = deviceTokenEntity.getPk();
    String sk = deviceTokenEntity.getSk();

    snsService.unsubscribe(subscriptionArn);
    snsService.cancelEndpoint(endpointArn);
    deviceTokenRepository.delete(pk, sk);
  }
}
