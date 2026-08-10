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

  public void notifyPushFailure(String userId, String deviceToken, String messageId) {
    send(
        "Push delivery failure",
        "SNS reported a failed push delivery.",
        Map.of(
            "User ID", replaceNullValue(userId),
            "Device Token", maskToken(deviceToken),
            "Message ID", replaceNullValue(messageId),
            "Detected At", Instant.now().toString()));
  }

  public void notifyProcessingFailure(String sqsMessageId, String reason) {
    send(
        "Push failure processing error",
        "The failure handler could not process an SQS message.",
        Map.of(
            "SQS Message ID", replaceNullValue(sqsMessageId),
            "Reason", replaceNullValue(reason),
            "Detected At", Instant.now().toString()));
  }

  private void send(String title, String summary, Map<String, String> fields) {
    if (webhookUrl == null) {
      log.warn("Slack failure webhook URL is not configured. title={}", title);
      return;
    }

    try {
      String body = objectMapper.writeValueAsString(createPayload(title, summary, fields));
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
      String title, String summary, Map<String, String> fields) {
    return Map.of(
        "text",
        title,
        "blocks",
        List.of(
            Map.of(
                "type",
                "header",
                "text",
                Map.of("type", "plain_text", "text", title, "emoji", true)),
            Map.of("type", "section", "text", Map.of("type", "mrkdwn", "text", summary)),
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
                    .toList())));
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
