package com.sopt.push.service;

import static com.sopt.push.common.Constants.FORMATTER;
import static com.sopt.push.common.Constants.HEADER_CONTENT_TYPE;
import static com.sopt.push.common.Constants.HTTP_METHOD_PATCH;
import static com.sopt.push.common.Constants.HTTP_METHOD_POST;
import static com.sopt.push.common.Constants.HTTP_REQUEST_TIMEOUT_SECONDS;
import static com.sopt.push.common.Constants.MEDIA_TYPE_APPLICATION_JSON;
import static com.sopt.push.common.Constants.SYSTEM_NAME_APP_SERVER;
import static com.sopt.push.common.Constants.SYSTEM_NAME_OPERATION_SERVER;
import static com.sopt.push.common.Constants.TIME_ZONE_KST;
import static com.sopt.push.common.Constants.URL_PATH_FORMAT_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.push.common.ExternalException;
import com.sopt.push.common.StatusCode;
import com.sopt.push.config.EnvConfig;
import com.sopt.push.config.ObjectMapperConfig;
import com.sopt.push.dto.PushSuccessMessageDto;
import com.sopt.push.dto.ScheduleSuccessWebHookDto;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebHookService {

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final EnvConfig envConfig;

  public WebHookService(HttpClient httpClient, EnvConfig envConfig) {
    this.httpClient = httpClient;
    this.objectMapper = ObjectMapperConfig.getObjectMapper();
    this.envConfig = envConfig;
  }

  public void pushSuccessWebHook(PushSuccessMessageDto dto) {
    String url = envConfig.getMakersAppServerUrl();
    String body = toJson(dto);
    sendWebhook(url, HTTP_METHOD_POST, body, SYSTEM_NAME_APP_SERVER);
  }

  public void scheduleSuccessWebHook(String alarmId) {
    validateAlarmId(alarmId);

    String url =
        String.format(URL_PATH_FORMAT_ID, envConfig.getMakersOperationServerUrl(), alarmId);

    ScheduleSuccessWebHookDto dto =
        new ScheduleSuccessWebHookDto(
            ZonedDateTime.now(ZoneId.of(TIME_ZONE_KST)).format(FORMATTER));

    String body = toJson(dto);
    sendWebhook(url, HTTP_METHOD_PATCH, body, SYSTEM_NAME_OPERATION_SERVER);
  }

  private void sendWebhook(String url, String method, String body, String systemName) {
    try {
      HttpRequest request = createRequest(url, method, body);

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      checkResponse(response, systemName);

    } catch (IOException | InterruptedException e) {
      log.error("{} webhook failed", systemName, e);
      throw new ExternalException(systemName + " webhook failed", e);
    }
  }

  private String toJson(Object dto) {
    try {
      return objectMapper.writeValueAsString(dto);
    } catch (Exception e) {
      throw new ExternalException("Failed to serialize webhook body", e);
    }
  }

  private void checkResponse(HttpResponse<String> response, String systemName) {
    int status = response.statusCode();

    if (!isSuccess(status)) {
      String error =
          String.format(
              "%s webhook failed with status %d and body: %s", systemName, status, response.body());
      log.error(error);
      throw new ExternalException(error, null);
    }
  }

  private HttpRequest createRequest(String url, String method, String body) {
    return HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header(HEADER_CONTENT_TYPE, MEDIA_TYPE_APPLICATION_JSON)
        .method(method, HttpRequest.BodyPublishers.ofString(body))
        .timeout(Duration.ofSeconds(HTTP_REQUEST_TIMEOUT_SECONDS))
        .build();
  }

  private boolean isSuccess(int status) {
    return status >= StatusCode.OK && status < StatusCode.MULTIPLE_CHOICES;
  }

  private void validateAlarmId(String alarmId) {
    if (alarmId == null || alarmId.isBlank()) {
      throw new IllegalArgumentException("schedule alarm id not defined");
    }
  }
}
