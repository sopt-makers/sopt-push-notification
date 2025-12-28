package com.sopt.push.service;

import static com.sopt.push.common.Constants.JSON;
import static com.sopt.push.common.Constants.SNS_PROTOCOL_APPLICATION;
import static com.sopt.push.util.ValidationUtil.validateDto;

import com.sopt.push.common.ExternalException;
import com.sopt.push.common.InvalidEndpointException;
import com.sopt.push.config.EnvConfig;
import com.sopt.push.config.ValidatorConfig;
import com.sopt.push.dto.MessageFactoryDto;
import com.sopt.push.enums.Category;
import com.sopt.push.enums.Platform;
import com.sopt.push.enums.PushTopic;
import com.sopt.push.message.MessageCreator;
import jakarta.validation.Validator;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse;
import software.amazon.awssdk.services.sns.model.DeleteEndpointRequest;
import software.amazon.awssdk.services.sns.model.EndpointDisabledException;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;

public class NotificationService {

  private final SnsClient snsClient;
  private final Validator validator;
  private final String allTopicArn;
  private final String iosArn;
  private final String androidArn;

  public NotificationService(SnsClient snsClient, EnvConfig envConfig) {
    this.snsClient = snsClient;
    this.validator = ValidatorConfig.getValidator();
    this.allTopicArn = envConfig.getAllTopicArn();
    this.iosArn = envConfig.getPlatformApplicationIosArn();
    this.androidArn = envConfig.getPlatformApplicationAndroidArn();
  }

  public String platformPush(
      String endpointArn,
      String title,
      String content,
      String webLink,
      String deepLink,
      Category category,
      String messageId,
      Platform platform) {
    try {
      MessageFactoryDto dto =
          new MessageFactoryDto(
              platform.getTopic(), messageId, title, content, category, deepLink, webLink);

      validateDto(dto);

      String messageJson = MessageCreator.create(dto);

      PublishRequest publishRequest =
          PublishRequest.builder()
              .messageStructure(JSON)
              .targetArn(endpointArn)
              .message(messageJson)
              .build();

      PublishResponse result = snsClient.publish(publishRequest);
      return result.messageId();

    } catch (EndpointDisabledException | InvalidParameterException ex) {
      throw new InvalidEndpointException(endpointArn, ex);

    } catch (SnsException ex) {
      throw new ExternalException("SNS publish failed: " + ex.getMessage(), ex);
    } catch (Exception ex) {
      throw new ExternalException("Unknown error while sending push: " + ex.getMessage(), ex);
    }
  }

  public String allTopicPush(
      String title,
      String content,
      Category category,
      String webLink,
      String deepLink,
      String messageId) {
    try {
      MessageFactoryDto messageFactoryDto =
          new MessageFactoryDto(
              PushTopic.ALL, messageId, title, content, category, deepLink, webLink);

      validateDto(messageFactoryDto);

      String messageJson = MessageCreator.create(messageFactoryDto);

      PublishRequest publishRequest =
          PublishRequest.builder()
              .messageStructure(JSON)
              .topicArn(allTopicArn)
              .message(messageJson)
              .build();

      PublishResponse result = snsClient.publish(publishRequest);
      return result.messageId();

    } catch (EndpointDisabledException | InvalidParameterException ex) {
      throw new InvalidEndpointException(allTopicArn, ex);
    } catch (SnsException ex) {
      throw new ExternalException("SNS publish failed: " + ex.getMessage(), ex);
    } catch (Exception ex) {
      throw new ExternalException("Unknown error while sending push: " + ex.getMessage(), ex);
    }
  }

  public void deleteEndpoint(String endpointArn) {
    snsClient.deleteEndpoint(DeleteEndpointRequest.builder().endpointArn(endpointArn).build());
  }

  public void unsubscribe(String subscriptionArn) {
    snsClient.unsubscribe(UnsubscribeRequest.builder().subscriptionArn(subscriptionArn).build());
  }

  public CreatePlatformEndpointResponse registerEndpoint(
      String deviceToken, Platform platform, String userId) {
    String platformApplicationArn = getPlatformApplicationArn(platform);
    CreatePlatformEndpointRequest.Builder requestBuilder =
        CreatePlatformEndpointRequest.builder()
            .platformApplicationArn(platformApplicationArn)
            .token(deviceToken);
    boolean isValidUserId = userId != null && !userId.isBlank();

    if (isValidUserId) {
      requestBuilder.customUserData(userId);
    }

    CreatePlatformEndpointRequest request = requestBuilder.build();
    return snsClient.createPlatformEndpoint(request);
  }

  public SubscribeResponse subscribe(String endpointArn) {
    SubscribeRequest request =
        SubscribeRequest.builder()
            .protocol(SNS_PROTOCOL_APPLICATION)
            .endpoint(endpointArn)
            .topicArn(allTopicArn)
            .build();

    return snsClient.subscribe(request);
  }

  private String getPlatformApplicationArn(Platform platform) {
    return platform == Platform.IOS ? iosArn : androidArn;
  }
}
