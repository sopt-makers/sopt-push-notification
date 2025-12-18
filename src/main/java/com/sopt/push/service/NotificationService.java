package com.sopt.push.service;

import com.sopt.push.common.BusinessException;
import com.sopt.push.common.ErrorMessage;
import com.sopt.push.config.EnvConfig;
import com.sopt.push.dto.MessageFactoryDto;
import com.sopt.push.dto.PushMessagePayloadDto;
import com.sopt.push.dto.ResponsePushNotificationDto;
import com.sopt.push.enums.Platform;
import com.sopt.push.enums.PushTopic;
import com.sopt.push.message.MessageCreator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NotificationService {

  private final SnsService snsService;
  private final EnvConfig envConfig;

  public Optional<ResponsePushNotificationDto> sendPlatformPushMessage(
      PushMessagePayloadDto messagePayload, String endpointArn, Platform platform) {

    validatePayload(messagePayload);

    boolean invalidEndpointArn = endpointArn == null || endpointArn.isBlank();

    if (invalidEndpointArn) {
      throw new BusinessException(ErrorMessage.INVALID_REQUEST, "endpointArn is null or blank");
    }

    MessageFactoryDto dto =
        new MessageFactoryDto(
            platform.getTopic(),
            messagePayload.id(),
            messagePayload.title(),
            messagePayload.content(),
            messagePayload.category(),
            messagePayload.deepLink(),
            messagePayload.webLink());

    String messageJson = MessageCreator.create(dto);
    return sendPushMessage(endpointArn, messageJson, false);
  }

  public Optional<ResponsePushNotificationDto> sendAllTopicPushMessage(
      PushMessagePayloadDto messagePayload) {

    validatePayload(messagePayload);

    MessageFactoryDto dto =
        new MessageFactoryDto(
            PushTopic.ALL,
            messagePayload.id(),
            messagePayload.title(),
            messagePayload.content(),
            messagePayload.category(),
            messagePayload.deepLink(),
            messagePayload.webLink());

    String messageJson = MessageCreator.create(dto);
    String topicArn = envConfig.getAllTopicArn();
    return sendPushMessage(topicArn, messageJson, true);
  }

  private void validatePayload(PushMessagePayloadDto messagePayload) {
    boolean invalidId = messagePayload.id() == null || messagePayload.id().isBlank();
    boolean invalidTitle = messagePayload.title() == null || messagePayload.title().isBlank();
    boolean invalidContent = messagePayload.content() == null || messagePayload.content().isBlank();
    boolean invalidCategory = messagePayload.category() == null;

    if (invalidId || invalidTitle || invalidContent || invalidCategory) {
      throw new BusinessException(ErrorMessage.INVALID_REQUEST, "Invalid push message payload");
    }
  }

  private Optional<ResponsePushNotificationDto> sendPushMessage(
      String arn, String messageJson, boolean sendAll) {
    var publishResponse =
        sendAll
            ? snsService.publishToTopicArn(arn, messageJson)
            : snsService.publishToEndpoint(arn, messageJson);
    String messageId = publishResponse.messageId();
    boolean invalidMessageId = messageId == null || messageId.isBlank();

    if (invalidMessageId) {
      return Optional.empty();
    }

    return Optional.of(new ResponsePushNotificationDto(messageId));
  }
}
