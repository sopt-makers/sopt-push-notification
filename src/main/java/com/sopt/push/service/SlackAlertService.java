package com.sopt.push.service;

import static com.sopt.push.common.Constants.HEADER_CONTENT_TYPE;
import static com.sopt.push.common.Constants.HTTP_METHOD_POST;
import static com.sopt.push.common.Constants.HTTP_REQUEST_TIMEOUT_SECONDS;
import static com.sopt.push.common.Constants.MEDIA_TYPE_APPLICATION_JSON;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.push.config.EnvConfig;
import com.sopt.push.config.ObjectMapperConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SlackAlertService {

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String webhookUrl;

  public SlackAlertService(HttpClient httpClient, EnvConfig envConfig) {
    this.httpClient = httpClient;
    this.objectMapper = ObjectMapperConfig.getObjectMapper();
    this.webhookUrl = envConfig.getSlackFailureWebhookUrl();
  }

  public void notifyPushFailures(List<PushFailureAlert> alerts) {
    if (alerts == null || alerts.isEmpty()) {
      return;
    }

    send(
        "Push delivery failure",
        String.format("SNS reported %d failed push delivery event(s).", alerts.size()),
        alerts.stream().map(this::createPushFailureFields).toList());
  }

  public void notifyProcessingFailures(List<ProcessingFailureAlert> alerts) {
    if (alerts == null || alerts.isEmpty()) {
      return;
    }

    send(
        "Push failure processing error",
        String.format("The failure handler could not process %d SQS message(s).", alerts.size()),
        alerts.stream().map(this::createProcessingFailureFields).toList());
  }

  private void send(String title, String summary, List<Map<String, String>> fieldGroups) {
    if (webhookUrl == null) {
      log.warn("Slack failure webhook URL is not configured. title={}", title);
      return;
    }

    try {
      String body = objectMapper.writeValueAsString(createPayload(title, summary, fieldGroups));
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(webhookUrl))
              .header(HEADER_CONTENT_TYPE, MEDIA_TYPE_APPLICATION_JSON)
              .method(HTTP_METHOD_POST, HttpRequest.BodyPublishers.ofString(body))
              .timeout(Duration.ofSeconds(HTTP_REQUEST_TIMEOUT_SECONDS))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn("Slack failure webhook returned status={}", response.statusCode());
      }
    } catch (Exception e) {
      log.warn("Failed to send Slack failure alert", e);
    }
  }

  private Map<String, Object> createPayload(
      String title, String summary, List<Map<String, String>> fieldGroups) {
    List<Map<String, Object>> blocks = new ArrayList<>();
    blocks.add(
        Map.of(
            "type", "header", "text", Map.of("type", "plain_text", "text", title, "emoji", true)));
    blocks.add(Map.of("type", "section", "text", Map.of("type", "mrkdwn", "text", summary)));

    for (Map<String, String> fields : fieldGroups) {
      blocks.add(
          Map.of(
              "type",
              "section",
              "fields",
              fields.entrySet().stream()
                  .map(
                      entry ->
                          Map.of(
                              "type",
                              "mrkdwn",
                              "text",
                              String.format("*%s*\n%s", entry.getKey(), entry.getValue())))
                  .toList()));
    }

    return Map.of("text", title, "blocks", blocks);
  }

  private Map<String, String> createPushFailureFields(PushFailureAlert alert) {
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("User ID", replaceNullValue(alert.userId));
    fields.put("Device Token", maskToken(alert.deviceToken));
    fields.put("Message ID", replaceNullValue(alert.messageId));
    fields.put("Detected At", alert.detectedAt.toString());
    return fields;
  }

  private Map<String, String> createProcessingFailureFields(ProcessingFailureAlert alert) {
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("SQS Message ID", replaceNullValue(alert.sqsMessageId));
    fields.put("Reason", replaceNullValue(alert.reason));
    fields.put("Detected At", alert.detectedAt.toString());
    return fields;
  }

  public static PushFailureAlert pushFailureAlert(
      String userId, String deviceToken, String messageId) {
    return new PushFailureAlert(userId, deviceToken, messageId, Instant.now());
  }

  public static ProcessingFailureAlert processingFailureAlert(String sqsMessageId, String reason) {
    return new ProcessingFailureAlert(sqsMessageId, reason, Instant.now());
  }

  public static final class PushFailureAlert {

    private final String userId;
    private final String deviceToken;
    private final String messageId;
    private final Instant detectedAt;

    private PushFailureAlert(
        String userId, String deviceToken, String messageId, Instant detectedAt) {
      this.userId = userId;
      this.deviceToken = deviceToken;
      this.messageId = messageId;
      this.detectedAt = detectedAt;
    }
  }

  public static final class ProcessingFailureAlert {

    private final String sqsMessageId;
    private final String reason;
    private final Instant detectedAt;

    private ProcessingFailureAlert(String sqsMessageId, String reason, Instant detectedAt) {
      this.sqsMessageId = sqsMessageId;
      this.reason = reason;
      this.detectedAt = detectedAt;
    }
  }

  private String replaceNullValue(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }

  private String maskToken(String token) {
    if (token == null || token.isBlank()) {
      return "-";
    }

    int visibleLength = Math.min(8, token.length());
    return token.substring(0, visibleLength) + "...";
  }
}
