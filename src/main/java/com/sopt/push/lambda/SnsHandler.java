package com.sopt.push.lambda;

import static com.sopt.push.common.Constants.TOKEN;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.push.config.AppFactory;
import com.sopt.push.config.ObjectMapperConfig;
import com.sopt.push.domain.DeviceTokenEntity;
import com.sopt.push.dto.CreateHistoryDto;
import com.sopt.push.dto.UserTokenInfoDto;
import com.sopt.push.enums.NotificationStatus;
import com.sopt.push.enums.NotificationType;
import com.sopt.push.service.DeviceTokenService;
import com.sopt.push.service.HistoryService;
import com.sopt.push.service.InvalidEndpointCleaner;
import com.sopt.push.service.UserService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SnsHandler implements RequestHandler<SNSEvent, String> {

  private final UserService userService;
  private final DeviceTokenService deviceTokenService;
  private final HistoryService historyService;
  private final InvalidEndpointCleaner invalidEndpointCleaner;
  private final ObjectMapper objectMapper;

  public SnsHandler() {
    AppFactory factory = AppFactory.getInstance();
    this.userService = factory.userService();
    this.deviceTokenService = factory.deviceTokenService();
    this.historyService = factory.historyService();
    this.invalidEndpointCleaner = factory.invalidEndpointCleaner();
    this.objectMapper = ObjectMapperConfig.getObjectMapper();
  }

  @Override
  public String handleRequest(SNSEvent event, Context context) {
    boolean invalidEvent =
        event == null || event.getRecords() == null || event.getRecords().isEmpty();
    if (invalidEvent) {
      log.warn("SNS event is null or has no records");
      return "No records found";
    }

    log.info("Received SNS records count={}", event.getRecords().size());

    try {
      Map<SNSEvent.SNSRecord, String> recordTokenMap = extractRecordTokens(event.getRecords());
      List<String> deviceTokens =
          recordTokenMap.values().stream()
              .filter(token -> token != null && !token.isBlank())
              .toList();

      List<DeviceTokenEntity> deviceTokenEntities =
          deviceTokenService.findUserByTokenIds(deviceTokens);

      Map<String, UserTokenInfoDto> tokenMap =
          deviceTokenEntities.stream()
              .map(deviceTokenService::mapDeviceTokenEntityToInfoDto)
              .collect(
                  Collectors.toMap(
                      UserTokenInfoDto::deviceToken, Function.identity(), (a, b) -> a));

      for (Map.Entry<SNSEvent.SNSRecord, String> entry : recordTokenMap.entrySet()) {
        try {
          processRecord(entry.getKey(), entry.getValue(), tokenMap);
        } catch (Exception ex) {
          log.error("Failed to process SNS record: {}", entry.getKey(), ex);
        }
      }

      return "SNS handler processed successfully";

    } catch (RuntimeException ex) {
      log.error("SNS handler failed with runtime exception", ex);
      throw ex;
    } catch (Exception ex) {
      log.error("SNS handler failed with unexpected exception", ex);
      throw new RuntimeException("SNS handler processing failed", ex);
    }
  }

  private Map<SNSEvent.SNSRecord, String> extractRecordTokens(List<SNSEvent.SNSRecord> records) {
    Map<SNSEvent.SNSRecord, String> recordTokenMap = new HashMap<>();
    for (SNSEvent.SNSRecord record : records) {
      String token = extractTokenFromRecord(record);
      recordTokenMap.put(record, token);
    }
    return recordTokenMap;
  }

  private void processRecord(
      SNSEvent.SNSRecord record, String token, Map<String, UserTokenInfoDto> tokenMap) {
    if (token == null || token.isBlank()) {
      return;
    }

    UserTokenInfoDto userTokenInfoDto = tokenMap.get(token);
    if (userTokenInfoDto == null) {
      log.info("No UserTokenInfoDto found for token: {}", token);
      return;
    }

    SNSEvent.SNS sns = record.getSNS();
    String messageId = sns != null ? sns.getMessageId() : null;

    log.info(
        "Processing invalid push endpoint for userId={}, messageId={}",
        userTokenInfoDto.userId(),
        messageId);
    handleInvalidPushEndpoint(userTokenInfoDto, messageId);
  }

  private void handleInvalidPushEndpoint(UserTokenInfoDto userTokenInfoDto, String messageId) {
    createFailLog(userTokenInfoDto.userId(), messageId);

    try {
      invalidEndpointCleaner.clean(userTokenInfoDto);
    } catch (Exception e) {
      log.error("Failed to clean invalid endpoint for userId={}", userTokenInfoDto.userId(), e);
    }
  }

  private String extractTokenFromRecord(SNSEvent.SNSRecord record) {
    try {
      SNSEvent.SNS sns = record.getSNS();
      if (sns == null) {
        return null;
      }

      String message = sns.getMessage();
      boolean invalidMessage = message == null || message.isBlank();
      if (invalidMessage) {
        return null;
      }

      JsonNode root = objectMapper.readTree(message);
      JsonNode tokenNode = root.path(TOKEN);
      boolean missingOrBlankToken = tokenNode.isMissingNode() || tokenNode.asText().isBlank();
      if (missingOrBlankToken) {
        return null;
      }

      return tokenNode.asText();

    } catch (Exception e) {
      log.error("Failed to extract token from SNS record message", e);
      return null;
    }
  }

  private void createFailLog(String userId, String messageId) {
    boolean hasValidUserId = userId != null && !userId.isBlank();
    Set<String> userIds = hasValidUserId ? Set.of(userId) : null;

    boolean hasValidMessageId = messageId != null && !messageId.isBlank();
    Set<String> messageIds = hasValidMessageId ? Set.of(messageId) : null;

    CreateHistoryDto createHistoryDto = createFailureHistoryDto(userIds, messageIds);
    historyService.createLog(createHistoryDto);
  }

  private CreateHistoryDto createFailureHistoryDto(Set<String> userIds, Set<String> messageIds) {
    return new CreateHistoryDto(
        UUID.randomUUID().toString(), // transactionId
        null, // title
        null, // content
        null, // webLink
        null, // applink
        NotificationType.PUSH.getValue(), // notificationType
        null, // orderServiceName
        NotificationStatus.FAIL.getValue(), // status
        null, // action
        null, // platform
        null, // deviceToken
        null, // category
        userIds, // userIds
        null, // id
        messageIds, // messageIds
        null, // errorCode
        null // errorMessage
        );
  }
}
