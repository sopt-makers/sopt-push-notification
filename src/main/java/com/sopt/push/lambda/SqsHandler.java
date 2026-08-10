package com.sopt.push.lambda;

import static com.sopt.push.common.Constants.TOKEN;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
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
import com.sopt.push.service.EndpointFacade;
import com.sopt.push.service.HistoryService;
import com.sopt.push.service.SlackAlertService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqsHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

  private static final String MESSAGE = "Message";
  private static final String MESSAGE_ID = "MessageId";

  private final DeviceTokenService deviceTokenService;
  private final HistoryService historyService;
  private final EndpointFacade endpointFacade;
  private final SlackAlertService slackAlertService;
  private final ObjectMapper objectMapper;

  public SqsHandler() {
    AppFactory factory = AppFactory.getInstance();
    this.deviceTokenService = factory.deviceTokenService();
    this.historyService = factory.historyService();
    this.endpointFacade = factory.endpointFacade();
    this.slackAlertService = factory.slackAlertService();
    this.objectMapper = ObjectMapperConfig.getObjectMapper();
  }

  @Override
  public SQSBatchResponse handleRequest(SQSEvent event, Context context) {
    List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();

    if (event == null || event.getRecords() == null || event.getRecords().isEmpty()) {
      log.warn("SQS event is null or has no records");
      return new SQSBatchResponse(failures);
    }

    log.info("Received SQS records count={}", event.getRecords().size());

    for (SQSEvent.SQSMessage record : event.getRecords()) {
      try {
        processRecord(record);
      } catch (Exception ex) {
        log.error("Failed to process SQS record. messageId={}", record.getMessageId(), ex);
        slackAlertService.notifyProcessingFailure(record.getMessageId(), ex.getMessage());
        failures.add(new SQSBatchResponse.BatchItemFailure(record.getMessageId()));
      }
    }

    return new SQSBatchResponse(failures);
  }

  private void processRecord(SQSEvent.SQSMessage record) throws Exception {
    FailureMessage failureMessage = extractFailureMessage(record);
    String token = failureMessage.token();

    if (token == null || token.isBlank()) {
      slackAlertService.notifyProcessingFailure(record.getMessageId(), "Missing device token");
      log.warn("Push failure message has no token. sqsMessageId={}", record.getMessageId());
      return;
    }

    DeviceTokenEntity tokenEntity = deviceTokenService.findByDeviceToken(token).orElse(null);
    if (tokenEntity == null) {
      log.info("No token entity found for failed token. sqsMessageId={}", record.getMessageId());
      slackAlertService.notifyPushFailure(null, token, failureMessage.messageId());
      return;
    }

    UserTokenInfoDto userTokenInfoDto =
        deviceTokenService.mapDeviceTokenEntityToInfoDto(tokenEntity);
    log.info(
        "Processing invalid push endpoint for userId={}, messageId={}",
        userTokenInfoDto.userId(),
        failureMessage.messageId());

    createFailLog(userTokenInfoDto.userId(), failureMessage.messageId());
    endpointFacade.clean(userTokenInfoDto);
    slackAlertService.notifyPushFailure(
        userTokenInfoDto.userId(), userTokenInfoDto.deviceToken(), failureMessage.messageId());
  }

  private FailureMessage extractFailureMessage(SQSEvent.SQSMessage record) throws Exception {
    JsonNode body = objectMapper.readTree(record.getBody());
    String messageId = textOrNull(body.path(MESSAGE_ID));
    JsonNode payload = body;

    if (body.hasNonNull(MESSAGE)) {
      payload = objectMapper.readTree(body.path(MESSAGE).asText());
    }

    String token = textOrNull(payload.path(TOKEN));
    String payloadMessageId = textOrNull(payload.path(MESSAGE_ID));
    String fallbackMessageId = messageId != null ? messageId : record.getMessageId();
    return new FailureMessage(
        token, payloadMessageId != null ? payloadMessageId : fallbackMessageId);
  }

  private void createFailLog(String userId, String messageId) {
    Set<String> userIds = userId != null && !userId.isBlank() ? Set.of(userId) : null;
    Set<String> messageIds = messageId != null && !messageId.isBlank() ? Set.of(messageId) : null;

    CreateHistoryDto createHistoryDto =
        new CreateHistoryDto(
            UUID.randomUUID().toString(),
            null,
            null,
            null,
            null,
            NotificationType.PUSH.getValue(),
            null,
            NotificationStatus.FAIL.getValue(),
            null,
            null,
            null,
            null,
            userIds,
            null,
            messageIds,
            null,
            null);
    historyService.createLog(createHistoryDto);
  }

  private String textOrNull(JsonNode node) {
    return node == null || node.isMissingNode() || node.asText().isBlank() ? null : node.asText();
  }

  private record FailureMessage(String token, String messageId) {

  }
}
