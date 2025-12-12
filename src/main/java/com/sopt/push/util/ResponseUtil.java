package com.sopt.push.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.push.common.SuccessMessage;
import com.sopt.push.config.ObjectMapperConfig;
import com.sopt.push.dto.ResponseDto;
import java.util.Map;

import static com.sopt.push.common.StatusCode.INTERNAL_SERVER_ERROR;

public class ResponseUtil {

  private static final ObjectMapper MAPPER = ObjectMapperConfig.getObjectMapper();

  private static final String KEY_STATUS_CODE = "statusCode";
  private static final String KEY_HEADERS = "headers";
  private static final String KEY_BODY = "body";

  private static final String HEADER_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
  private static final String HEADER_ALLOW_HEADERS = "Access-Control-Allow-Headers";
  private static final String HEADER_ALLOW_METHODS = "Access-Control-Allow-Methods";
  private static final String HEADER_VALUE_ALL = "*";

  private static final String ERROR_MESSAGE_FATAL = "fatal";


  private ResponseUtil() {}

  public static Map<String, Object> successResponse(SuccessMessage success)
      throws JsonProcessingException {
    ResponseDto<?> body = ResponseDto.success(success);

    return Map.of(
        KEY_STATUS_CODE, success.getHttpStatus(),
        KEY_HEADERS, corsHeaders(),
        KEY_BODY, MAPPER.writeValueAsString(body));
  }

  public static Map<String, Object> errorResponse(int status, String message) {
    try {
      ResponseDto<?> body = ResponseDto.fail(status, message);
      return Map.of(
          KEY_STATUS_CODE, status,
          KEY_HEADERS, corsHeaders(),
          KEY_BODY, MAPPER.writeValueAsString(body));
    } catch (Exception e) {
      return Map.of(KEY_STATUS_CODE, INTERNAL_SERVER_ERROR, KEY_BODY, ERROR_MESSAGE_FATAL);
    }
  }

  public static Map<String, String> corsHeaders() {
    return Map.of(
        HEADER_ALLOW_ORIGIN, HEADER_VALUE_ALL,
        HEADER_ALLOW_HEADERS, HEADER_VALUE_ALL,
        HEADER_ALLOW_METHODS, HEADER_VALUE_ALL);
  }
}
