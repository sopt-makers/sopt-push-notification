package com.sopt.push.service;

import com.sopt.push.client.SnsClientProvider;
import com.sopt.push.common.ExternalException;
import com.sopt.push.config.EnvConfig;
import com.sopt.push.enums.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse;
import software.amazon.awssdk.services.sns.model.DeleteEndpointRequest;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;

@Slf4j
@RequiredArgsConstructor
public class SnsService {

  private static final String MESSAGE_STRUCTURE_JSON = "json";
  private static final String APPLICATION_PROTOCOL = "application";

  private final EnvConfig envConfig;
  private final SnsClient snsClient = SnsClientProvider.getClient();

  public SubscribeResponse subscribe(String endpointArn) {
    SubscribeRequest request =
        SubscribeRequest.builder()
            .topicArn(envConfig.getAllTopicArn())
            .protocol(APPLICATION_PROTOCOL)
            .endpoint(endpointArn)
            .build();
    SubscribeResponse response = snsClient.subscribe(request);
    boolean hasError =
        response.sdkHttpResponse() == null || !response.sdkHttpResponse().isSuccessful();

    if (hasError) {
      log.error("SNS subscribe error: {}", response);
      throw new ExternalException("SNS subscribe failed");
    }
    return response;
  }

  public void unsubscribe(String subscriptionArn) {
    UnsubscribeRequest request =
        UnsubscribeRequest.builder().subscriptionArn(subscriptionArn).build();
    var response = snsClient.unsubscribe(request);
    boolean hasError =
        response.sdkHttpResponse() == null || !response.sdkHttpResponse().isSuccessful();

    if (hasError) {
      log.error("SNS unsubscribe error: {}", response);
      throw new ExternalException("SNS unsubscribe failed");
    }
  }

  public CreatePlatformEndpointResponse registerEndpoint(
      String deviceToken, Platform platform, String userId) {
    String platformApplicationArn =
        switch (platform) {
          case IOS -> envConfig.getPlatformApplicationIosArn();
          case ANDROID -> envConfig.getPlatformApplicationAndroidArn();
        };
    CreatePlatformEndpointRequest.Builder builder =
        CreatePlatformEndpointRequest.builder()
            .platformApplicationArn(platformApplicationArn)
            .token(deviceToken);

    if (userId != null && !userId.isBlank()) {
      builder.customUserData(userId);
    }

    CreatePlatformEndpointResponse response = snsClient.createPlatformEndpoint(builder.build());
    boolean hasError =
        response.sdkHttpResponse() == null || !response.sdkHttpResponse().isSuccessful();

    if (hasError) {
      log.error("SNS register endpoint error: {}", response);
      throw new ExternalException("SNS register endpoint failed");
    }
    return response;
  }

  public void cancelEndpoint(String endpointArn) {
    DeleteEndpointRequest request =
        DeleteEndpointRequest.builder().endpointArn(endpointArn).build();
    var response = snsClient.deleteEndpoint(request);
    boolean hasError =
        response.sdkHttpResponse() == null || !response.sdkHttpResponse().isSuccessful();

    if (hasError) {
      log.error("SNS delete endpoint error: {}", response);
      throw new ExternalException("SNS delete endpoint failed");
    }
  }

  public PublishResponse publishToTopicArn(String topicArn, String messageJson) {
    try {
      PublishRequest request =
          PublishRequest.builder()
              .topicArn(topicArn)
              .message(messageJson)
              .messageStructure(MESSAGE_STRUCTURE_JSON)
              .build();
      PublishResponse response = snsClient.publish(request);
      boolean hasError =
          response.sdkHttpResponse() == null || !response.sdkHttpResponse().isSuccessful();

      if (hasError) {
        log.error("SNS publish TopicArn error: {}", response);
        throw new ExternalException("SNS publish to topic failed");
      }
      return response;
    } catch (AwsServiceException e) {
      log.error("SNS publish TopicArn AWS service error", e);
      throw new ExternalException(
          "AWS service error during publish to topic: " + e.awsErrorDetails(), e);
    } catch (SdkClientException e) {
      log.error("SNS publish TopicArn SDK client error", e);
      throw new ExternalException("SDK client error during publish to topic: " + e.getMessage(), e);
    } catch (RuntimeException e) {
      log.error("SNS publish TopicArn unexpected error", e);
      throw new ExternalException(
          "Unexpected error during publish to topic: " + e.getMessage(), e);
    }
  }

  public PublishResponse publishToEndpoint(String endpointArn, String messageJson) {
    try {
      PublishRequest request =
          PublishRequest.builder()
              .targetArn(endpointArn)
              .message(messageJson)
              .messageStructure(MESSAGE_STRUCTURE_JSON)
              .build();
      PublishResponse response = snsClient.publish(request);
      boolean hasError =
          response.sdkHttpResponse() == null || !response.sdkHttpResponse().isSuccessful();

      if (hasError) {
        log.error("SNS endpoint publish error: {}", response);
        throw new ExternalException("SNS publish to endpoint failed");
      }
      return response;
    } catch (AwsServiceException e) {
      log.error("SNS endpoint publish AWS service error", e);
      throw new ExternalException(
          "AWS service error during publish to endpoint: " + e.awsErrorDetails(), e);
    } catch (SdkClientException e) {
      log.error("SNS endpoint publish SDK client error", e);
      throw new ExternalException(
          "SDK client error during publish to endpoint: " + e.getMessage(), e);
    } catch (RuntimeException e) {
      log.error("SNS endpoint publish unexpected error", e);
      throw new ExternalException(
          "Unexpected error during publish to endpoint: " + e.getMessage(), e);
    }
  }
}
