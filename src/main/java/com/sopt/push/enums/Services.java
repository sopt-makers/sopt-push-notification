package com.sopt.push.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sopt.push.common.BusinessException;
import com.sopt.push.common.ErrorMessage;

public enum Services {
  CREW("crew"),
  OFFICIAL("official"),
  OPERATION("operation"),
  PLAYGROUND("playground"),
  APP("app");

  private final String value;

  Services(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static Services fromValue(String value) {
    if (value == null) {
      throw new BusinessException(ErrorMessage.INVALID_REQUEST, "Services value cannot be null");
    }

    return switch (value) {
      case "crew" -> CREW;
      case "official" -> OFFICIAL;
      case "operation" -> OPERATION;
      case "playground" -> PLAYGROUND;
      case "app" -> APP;
      default ->
          throw new BusinessException(ErrorMessage.INVALID_REQUEST, "Unknown Services: " + value);
    };
  }
}
