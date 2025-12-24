package com.sopt.push.config;

import com.sopt.push.common.DeviceTokenException;
import com.sopt.push.common.ErrorMessage;
import com.sopt.push.enums.Platform;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;

@Slf4j
public class SnsFactory {

  private final SnsClient snsClient;
  private final String allTopicArn;
  private static final String PLATFORM_APPLICATION_IOS_ENV = "PLATFORM_APPLICATION_iOS";
  private static final String PLATFORM_APPLICATION_ANDROID_ENV = "PLATFORM_APPLICATION_ANDROID";

  public SnsFactory(SnsClient snsClient, EnvConfig envConfig) {
    this.snsClient = snsClient;
    this.allTopicArn = envConfig.getAllTopicArn();
  }

  public SubscribeResponse subscribe(String endpointArn) {
    SubscribeRequest request =
        SubscribeRequest.builder()
            .protocol("application")
            .endpoint(endpointArn)
            .topicArn(allTopicArn)
            .build();

    return snsClient.subscribe(request);
  }

  public CreatePlatformEndpointResponse registerEndPoint(
      String deviceToken, Platform platform, String userId) {
    String platformApplicationArn = getPlatformApplicationArn(platform);

    CreatePlatformEndpointRequest.Builder requestBuilder =
        CreatePlatformEndpointRequest.builder()
            .platformApplicationArn(platformApplicationArn)
            .token(deviceToken);

    if (userId != null && !userId.isBlank()) {
      requestBuilder.customUserData(userId);
    }

    CreatePlatformEndpointRequest request = requestBuilder.build();
    return snsClient.createPlatformEndpoint(request);
  }

  private String getPlatformApplicationArn(Platform platform) {
    String envVar =
        platform == Platform.IOS ? PLATFORM_APPLICATION_IOS_ENV : PLATFORM_APPLICATION_ANDROID_ENV;
    String value = System.getenv(envVar);
    if (value == null || value.isBlank()) {
      throw new DeviceTokenException(
          ErrorMessage.PLATFORM_APP_ARN_NOT_SET, "Platform application ARN not set: " + envVar);
    }
    return value;
  }
}
