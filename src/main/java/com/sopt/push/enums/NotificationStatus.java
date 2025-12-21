package com.sopt.push.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sopt.push.common.BusinessException;
import com.sopt.push.common.ErrorMessage;

public enum NotificationStatus {
  FAIL("fail"),
  SUCCESS("success"),
  PARTIAL_SUCCESS("partial_success");

  private final String value;

  NotificationStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return this.value;
  }

  @JsonCreator
  public static NotificationStatus fromValue(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(
          ErrorMessage.INVALID_REQUEST, "NotificationStatus cannot be null or blank");
    }

    return switch (value.trim()) {
      case "fail" -> FAIL;
      case "success" -> SUCCESS;
      case "partial_success" -> PARTIAL_SUCCESS;
      default ->
          throw new BusinessException(
              ErrorMessage.INVALID_REQUEST, "Unknown NotificationStatus: " + value);
    };
  }
}
