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
}
