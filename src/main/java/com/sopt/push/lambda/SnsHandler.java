package com.sopt.push.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.push.client.DynamoDbClientProvider;
import com.sopt.push.common.Constants;
import com.sopt.push.config.EnvConfig;
import com.sopt.push.config.ObjectMapperConfig;
import com.sopt.push.domain.DeviceTokenEntity;
import com.sopt.push.domain.HistoryEntity;
import com.sopt.push.enums.NotificationStatus;
import com.sopt.push.repository.DeviceTokenRepository;
import com.sopt.push.repository.HistoryRepository;
import com.sopt.push.repository.UserRepository;
import com.sopt.push.service.SnsService;
import com.sopt.push.service.UserService;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

@Slf4j
public class SnsHandler implements RequestHandler<SNSEvent, Void> {

  private static final int SPLIT_LIMIT_FOR_USER_ID_EXTRACTION = 2;
  private static final int USER_ID_INDEX_IN_SPLIT_PARTS = 1;

  private final EnvConfig envConfig;
  private final DeviceTokenRepository deviceTokenRepository;
  private final HistoryRepository historyRepository;
  private final UserService userService;
  private final ObjectMapper objectMapper;

  public SnsHandler() {
    this.envConfig = new EnvConfig();
    DynamoDbEnhancedClient enhancedClient = DynamoDbClientProvider.getClient();
    this.deviceTokenRepository =
        new DeviceTokenRepository(enhancedClient, envConfig.getDynamoDbTableName());
    UserRepository userRepository =
        new UserRepository(enhancedClient, envConfig.getDynamoDbTableName());
    this.historyRepository =
        new HistoryRepository(enhancedClient, envConfig.getDynamoDbTableName());
    SnsService snsService = new SnsService(envConfig);
    this.userService = new UserService(deviceTokenRepository, userRepository, snsService);
    this.objectMapper = ObjectMapperConfig.getObjectMapper();
  }

  @Override
  public Void handleRequest(SNSEvent event, Context context) {
    boolean invalidEvent =
        event == null || event.getRecords() == null || event.getRecords().isEmpty();
    if (invalidEvent) {
      log.warn("SNS event is null or has no records");
      return null;
    }

    Map<String, DeviceTokenEntity> tokenToEntity = collectTokenEntities(event.getRecords());
    processFailureRecords(event.getRecords(), tokenToEntity);
    return null;
  }

  private Map<String, DeviceTokenEntity> collectTokenEntities(
      java.util.List<SNSEvent.SNSRecord> records) {
    Map<String, DeviceTokenEntity> tokenToEntity = new HashMap<>();
    for (SNSEvent.SNSRecord record : records) {
      String token = extractTokenFromRecord(record);
      boolean invalidToken = token == null || token.isBlank();
      if (invalidToken) {
        log.warn("SNS record does not contain a valid token: {}", record);
        continue;
      }

      String pk = Constants.TOKEN_PREFIX + token;
      Optional<DeviceTokenEntity> entityOptional = deviceTokenRepository.findByPk(pk);
      entityOptional.ifPresent(entity -> tokenToEntity.put(token, entity));
    }
    return tokenToEntity;
  }

  private void processFailureRecords(
      List<SNSEvent.SNSRecord> records, Map<String, DeviceTokenEntity> tokenToEntity) {
    for (SNSEvent.SNSRecord record : records) {
      String token = extractTokenFromRecord(record);
      boolean invalidToken = token == null || token.isBlank();
      if (invalidToken) {
        continue;
      }

      DeviceTokenEntity deviceTokenEntity = tokenToEntity.get(token);
      boolean entityNotFound = deviceTokenEntity == null;
      if (entityNotFound) {
        log.warn("No DeviceTokenEntity found for token: {}", token);
        continue;
      }

      String messageId = record.getSNS().getMessageId();
      boolean invalidMessageId = messageId == null || messageId.isBlank();
      if (invalidMessageId) {
        log.warn("SNS record does not contain a messageId: {}", record);
      }

      String userId = extractUserIdFromDeviceTokenEntity(deviceTokenEntity);
      boolean invalidUserId = userId == null || userId.isBlank();
      if (invalidUserId) {
        log.warn("Unable to extract userId from DeviceTokenEntity: {}", deviceTokenEntity);
      }

      createFailureHistory(userId, messageId);
      userService.unregisterToken(deviceTokenEntity);
    }
  }

  private String extractTokenFromRecord(SNSEvent.SNSRecord record) {
    try {
      String message = record.getSNS().getMessage();
      boolean invalidMessage = message == null || message.isBlank();
      if (invalidMessage) {
        return null;
      }
      JsonNode root = objectMapper.readTree(message);
      JsonNode tokenNode = root.path("Token");
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

  private String extractUserIdFromDeviceTokenEntity(DeviceTokenEntity entity) {
    String sk = entity.getSk();
    boolean invalidSk = sk == null || !sk.contains(Constants.DELIMITER);
    if (invalidSk) {
      return null;
    }

    String[] parts = sk.split(Constants.DELIMITER, SPLIT_LIMIT_FOR_USER_ID_EXTRACTION);
    boolean validParts = parts.length == SPLIT_LIMIT_FOR_USER_ID_EXTRACTION;
    return validParts ? parts[USER_ID_INDEX_IN_SPLIT_PARTS] : null;
  }

  private void createFailureHistory(String userId, String messageId) {
    ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
    String yearMonth =
        String.format(Constants.YEAR_MONTH_FORMAT, now.getYear(), now.getMonthValue());
    String timestamp = DateTimeFormatter.ISO_INSTANT.format(now.toInstant());
    String transactionId = UUID.randomUUID().toString();
    HistoryEntity history = new HistoryEntity();
    history.setPk(Constants.HISTORY_PREFIX + yearMonth);
    history.setSk(Constants.HISTORY_PREFIX + timestamp + Constants.DELIMITER + transactionId);
    history.setEntity(Constants.HISTORY_ENTITY);
    history.setStatus(NotificationStatus.FAIL.getValue());
    history.setNotificationType("sendPushNotification");

    boolean validUserId = userId != null && !userId.isBlank();
    if (validUserId) {
      Set<String> userIds = new HashSet<>();
      userIds.add(userId);
      history.setUserIds(userIds);
    }

    boolean validMessageId = messageId != null && !messageId.isBlank();
    if (validMessageId) {
      Set<String> messageIds = new HashSet<>();
      messageIds.add(messageId);
      history.setMessageIds(messageIds);
    }
    historyRepository.save(history);
  }
}
