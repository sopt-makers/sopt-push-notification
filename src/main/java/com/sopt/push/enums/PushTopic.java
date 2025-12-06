package com.sopt.push.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sopt.push.common.BusinessException;
import com.sopt.push.common.ErrorMessage;

public enum PushTopic {
  APNS("apns"),
  FCM("fcm"),
  ALL("all");

  private final String value;

  PushTopic(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return this.value;
  }

  @JsonCreator
  public static PushTopic fromValue(String value) {
    if (value == null) {
      throw new BusinessException(ErrorMessage.INVALID_REQUEST, "PushTopic value cannot be null");
    }

    return switch (value) {
      case "apns" -> APNS;
      case "fcm" -> FCM;
      case "all" -> ALL;
      default ->
          throw new BusinessException(ErrorMessage.INVALID_REQUEST, "Unknown PushTopic: " + value);
    };
  }
}
