package com.sopt.push.common;

import lombok.Getter;

@Getter
public enum ErrorMessage {
  /** 400 Bad Request */
  INVALID_REQUEST(StatusCode.BAD_REQUEST, "잘못된 요청입니다."),
  NULL_VALUE(StatusCode.BAD_REQUEST, "필요한 값이 없습니다."),
  TOKEN_NOT_EXIST(StatusCode.BAD_REQUEST, "존재하지 않는 토큰입니다."),

  /** 500 Internal Server Error */
  SEND_FAIL(StatusCode.INTERNAL_SERVER_ERROR, "메시지 전송 실패."),
  INTERNAL_SERVER_ERROR(StatusCode.INTERNAL_SERVER_ERROR, "서버 내부 오류");

  private final int httpStatus;
  private final String message;

  ErrorMessage(int httpStatus, String message) {
    this.httpStatus = httpStatus;
    this.message = message;
  }
}
