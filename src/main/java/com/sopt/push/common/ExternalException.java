package com.sopt.push.common;

/** 비즈니스 규칙 위반이 아닌, 외부 시스템/인프라(SNS 등) 연동 과정에서 발생하는 예외를 나타냅니다. 주로 5xx 계열 응답으로 매핑되는 시스템 오류에 사용합니다. */
public class ExternalException extends RuntimeException {

  public ExternalException(String message) {
    super(message);
  }

  public ExternalException(String message, Throwable cause) {
    super(message, cause);
  }
}
