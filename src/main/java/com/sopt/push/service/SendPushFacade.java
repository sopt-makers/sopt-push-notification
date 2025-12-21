package com.sopt.push.service;

import com.sopt.push.common.InvalidEndpointException;
import com.sopt.push.common.PushFailException;
import com.sopt.push.dto.CreateHistoryDto;
import com.sopt.push.dto.PushContext;
import com.sopt.push.dto.PushSuccessMessageDto;
import com.sopt.push.dto.RequestSendAllPushMessageDto;
import com.sopt.push.dto.RequestSendPushMessageDto;
import com.sopt.push.dto.UserTokenInfoDto;
import com.sopt.push.enums.Actions;
import com.sopt.push.enums.NotificationStatus;
import com.sopt.push.enums.NotificationType;
import com.sopt.push.enums.User;
import com.sopt.push.enums.WebHookType;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SendPushFacade {

  private final NotificationService notificationService;
  private final WebHookService webHookService;
  private final HistoryService historyService;
  private final UserService userService;
  private final DeviceTokenService deviceTokenService;
  private final InvalidEndpointCleaner cleaner;

  public SendPushFacade(
      NotificationService notificationService,
      WebHookService webHookService,
      HistoryService historyService,
      UserService userService,
      DeviceTokenService deviceTokenService,
      InvalidEndpointCleaner invalidEndpointCleaner) {
    this.notificationService = notificationService;
    this.webHookService = webHookService;
    this.historyService = historyService;
    this.userService = userService;
    this.deviceTokenService = deviceTokenService;
    this.cleaner = invalidEndpointCleaner;
  }

  public void sendPush(RequestSendPushMessageDto dto) {

    PushContext pushContext = PushContext.from(dto);
    final Set<String> userIds = dto.userIds();

    Set<UserTokenInfoDto> users = userService.findTokenByUserIds(userIds);
    if (users.isEmpty()) {
      log.warn("No users found for push.");
      return;
    }

    final String messageId = UUID.randomUUID().toString();
    Set<String> snsMessageIds = new HashSet<>();

    for (UserTokenInfoDto userTokenInfoDto : users) {
      String snsId = sendToUser(userTokenInfoDto, pushContext, messageId);
      if (snsId != null) {
        snsMessageIds.add(snsId);
      }
    }

    NotificationStatus notificationStatus;

    if (snsMessageIds.isEmpty()) {
      notificationStatus = NotificationStatus.FAIL;
    } else if (snsMessageIds.size() < users.size()) {
      notificationStatus = NotificationStatus.PARTIAL_SUCCESS;
    } else {
      notificationStatus = NotificationStatus.SUCCESS;
    }

    if (notificationStatus != NotificationStatus.FAIL) {
      PushSuccessMessageDto pushSuccessMessageDto =
          new PushSuccessMessageDto(
              messageId,
              pushContext.title(),
              pushContext.content(),
              pushContext.category(),
              WebHookType.SEND,
              pushContext.deepLink(),
              pushContext.webLink(),
              userIds);
      webHookService.pushSuccessWebHook(pushSuccessMessageDto);
    }

    CreateHistoryDto createHistoryDto =
        new CreateHistoryDto(
            pushContext.transactionId(),
            pushContext.title(),
            pushContext.content(),
            pushContext.webLink(),
            pushContext.deepLink(),
            NotificationType.PUSH.getValue(),
            pushContext.service().getValue(),
            notificationStatus.getValue(),
            Actions.SEND.getValue(),
            null,
            null,
            pushContext.category(),
            userIds,
            messageId,
            snsMessageIds,
            null,
            null);

    historyService.createLog(createHistoryDto);
  }

  private String sendToUser(
      UserTokenInfoDto userTokenInfoDto, PushContext pushContext, String messageId) {
    try {
      return notificationService.platformPush(
          userTokenInfoDto.endpointArn(),
          pushContext.title(),
          pushContext.content(),
          pushContext.webLink(),
          pushContext.deepLink(),
          pushContext.category(),
          messageId,
          userTokenInfoDto.platform());

    } catch (InvalidEndpointException ex) {
      cleaner.clean(userTokenInfoDto);

    } catch (PushFailException ex) {
      log.error("Push failed for user={} err={}", userTokenInfoDto.userId(), ex.getMessage());
    }
    return null;
  }

  public void sendPushAll(RequestSendAllPushMessageDto dto) {
    PushContext pushContext = PushContext.from(dto);
    final String messageId = UUID.randomUUID().toString();
    String actualSnsMessageId;
    try {
      actualSnsMessageId =
          notificationService.allTopicPush(
              pushContext.title(),
              pushContext.content(),
              pushContext.category(),
              pushContext.webLink(),
              pushContext.deepLink(),
              messageId);
    } catch (Exception e) {
      String errorMessage = String.format("Send Push All error: %s", e.getMessage());
      log.error(errorMessage, e);
      throw new PushFailException(errorMessage, e);
    }

    try {
      PushSuccessMessageDto webHookDto =
          new PushSuccessMessageDto(
              messageId,
              pushContext.title(),
              pushContext.content(),
              pushContext.category(),
              WebHookType.SEND_ALL,
              pushContext.deepLink(),
              pushContext.webLink(),
              Set.of(User.ALL.getValue()));
      webHookService.pushSuccessWebHook(webHookDto);
    } catch (Exception e) {
      log.warn("Failed to send webhook for successful push. messageId: {}", messageId, e);
    }

    try {
      CreateHistoryDto createHistoryDto =
          new CreateHistoryDto(
              pushContext.transactionId(),
              pushContext.title(),
              pushContext.content(),
              pushContext.webLink(),
              pushContext.deepLink(),
              NotificationType.PUSH.getValue(),
              pushContext.service().getValue(),
              NotificationStatus.SUCCESS.getValue(),
              Actions.SEND_ALL.getValue(),
              null,
              null,
              pushContext.category(),
              Set.of(User.ALL.getValue()),
              messageId,
              Set.of(actualSnsMessageId),
              null,
              null);
      historyService.createLog(createHistoryDto);
    } catch (Exception e) {
      log.warn("Failed to create history log for successful push. messageId: {}", messageId, e);
    }
  }
}
