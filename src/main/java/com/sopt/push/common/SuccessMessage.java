package com.sopt.push.common;

public enum SuccessMessage {
  /** 200 OK */
  OK(StatusCode.OK, "성공적으로 처리되었습니다."),
  TOKEN_REGISTER_SUCCESS(StatusCode.OK, "토큰 등록 성공"),
  TOKEN_CANCEL_SUCCESS(StatusCode.OK, "토큰 해지 성공"),
  SEND_SUCCESS(StatusCode.OK, "메시지 전송 성공"),

  /** 201 Created */
  CREATED(StatusCode.CREATED, "생성 완료"),

  /** 204 No Content */
  NO_CONTENT_SUCCESS(StatusCode.NO_CONTENT, "성공적으로 처리되었습니다.");

  private final int httpStatus;
  private final String message;

  SuccessMessage(int httpStatus, String message) {
    this.httpStatus = httpStatus;
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public int getHttpStatus() {
    return httpStatus;
  }
}
