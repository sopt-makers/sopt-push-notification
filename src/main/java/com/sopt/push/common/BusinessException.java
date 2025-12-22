package com.sopt.push.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

  private final ErrorMessage errorMessage;

  public BusinessException(ErrorMessage errorMessage) {
    super(errorMessage.getMessage());
    this.errorMessage = errorMessage;
  }

  public BusinessException(ErrorMessage errorMessage, String detail) {
    super(errorMessage.getMessage() + ": " + detail);
    this.errorMessage = errorMessage;
  }

  public BusinessException(ErrorMessage errorMessage, Throwable cause) {
    super(errorMessage.getMessage(), cause);
    this.errorMessage = errorMessage;
  }

  public BusinessException(ErrorMessage errorMessage, String detail, Throwable cause) {
    super(errorMessage.getMessage() + ": " + detail, cause);
    this.errorMessage = errorMessage;
  }
}
