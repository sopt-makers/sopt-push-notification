package com.sopt.push.common;

import lombok.Getter;

@Getter
public class DeviceTokenException extends RuntimeException {

  private final ErrorMessage errorMessage;

  public DeviceTokenException(ErrorMessage errorMessage) {
    super(errorMessage.getMessage());
    this.errorMessage = errorMessage;
  }

  public DeviceTokenException(ErrorMessage errorMessage, String detail) {
    super(errorMessage.getMessage() + ": " + detail);
    this.errorMessage = errorMessage;
  }

  public DeviceTokenException(ErrorMessage errorMessage, String detail, Throwable cause) {
    super(errorMessage.getMessage() + ": " + detail, cause);
    this.errorMessage = errorMessage;
  }
}
