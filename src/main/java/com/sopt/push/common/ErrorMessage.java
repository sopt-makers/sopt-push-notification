package com.sopt.push.common;

import lombok.Getter;

@Getter
public enum ErrorMessage {
  /** 400 Bad Request */
  INVALID_REQUEST(StatusCode.BAD_REQUEST, "잘못된 요청입니다."),
  NULL_VALUE(StatusCode.BAD_REQUEST, "필요한 값이 없습니다."),
  TOKEN_NOT_EXIST(StatusCode.BAD_REQUEST, "존재하지 않는 토큰입니다."),
  USER_ID_REQUIRED(StatusCode.BAD_REQUEST, "userId가 필요합니다."),
  TOKEN_NOT_FOUND(StatusCode.BAD_REQUEST, "토큰을 찾을 수 없습니다."),
  ARN_UNDEFINED(StatusCode.BAD_REQUEST, "arn 또는 topicArn이 정의되지 않았습니다."),
  INVALID_ENDPOINT(StatusCode.BAD_REQUEST, "유효하지 않은 SNS 엔드포인트입니다."),
  ENDPOINT_ARN_UNDEFINED(StatusCode.INTERNAL_SERVER_ERROR, "endpointArn이 정의되지 않았습니다."),
  SUBSCRIPTION_ARN_UNDEFINED(StatusCode.INTERNAL_SERVER_ERROR, "subscriptionArn이 정의되지 않았습니다."),
  PLATFORM_APP_ARN_NOT_SET(StatusCode.INTERNAL_SERVER_ERROR, "플랫폼 애플리케이션 ARN이 설정되지 않았습니다."),

  /** 500 Internal Server Error */
  SEND_FAIL(StatusCode.INTERNAL_SERVER_ERROR, "메시지 전송 실패."),
  INTERNAL_SERVER_ERROR(StatusCode.INTERNAL_SERVER_ERROR, "서버 내부 오류"),
  REGISTER_USER_ERROR(StatusCode.INTERNAL_SERVER_ERROR, "토큰 등록 중 오류가 발생했습니다."),
  DELETE_TOKEN_ERROR(StatusCode.INTERNAL_SERVER_ERROR, "토큰 삭제 중 오류가 발생했습니다."),
  SNS_PUBLISH_FAILED(StatusCode.INTERNAL_SERVER_ERROR, "SNS 발행 실패."),
  UNKNOWN_PUSH_ERROR(StatusCode.INTERNAL_SERVER_ERROR, "알 수 없는 푸시 전송 오류가 발생했습니다.");

  private final int httpStatus;
  private final String message;

  ErrorMessage(int httpStatus, String message) {
    this.httpStatus = httpStatus;
    this.message = message;
  }
}
