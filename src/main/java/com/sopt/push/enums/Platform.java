package com.sopt.push.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sopt.push.common.BusinessException;
import com.sopt.push.common.ErrorMessage;

public enum Platform {
  IOS("iOS", PushTopic.APNS),
  ANDROID("Android", PushTopic.FCM);

  private final String value;
  private final PushTopic topic;

  Platform(String value, PushTopic topic) {
    this.value = value;
    this.topic = topic;
  }

  @JsonValue
  public String getValue() {
    return this.value;
  }

  public PushTopic getTopic() {
    return topic;
  }

  @JsonCreator
  public static Platform fromValue(String value) {
    if (value == null) {
      throw new BusinessException(ErrorMessage.INVALID_REQUEST, "Platform value cannot be null");
    }

    return switch (value) {
      case "iOS" -> IOS;
      case "Android" -> ANDROID;
      default ->
          throw new BusinessException(ErrorMessage.INVALID_REQUEST, "Unknown Platform: " + value);
    };
  }
}
