package com.sopt.push.dto;

import com.sopt.push.enums.Category;
import com.sopt.push.enums.Services;

public record PushContext(
    String transactionId,
    String title,
    String content,
    String webLink,
    String deepLink,
    Services service,
    Category category) {

  public static PushContext from(RequestSendPushMessageDto dto) {
    return new PushContext(
        dto.transactionId(),
        dto.title(),
        dto.content(),
        dto.webLink(),
        dto.deepLink(),
        dto.service(),
        dto.category());
  }

  public static PushContext from(RequestSendAllPushMessageDto dto) {
    return new PushContext(
        dto.transactionId(),
        dto.title(),
        dto.content(),
        dto.webLink(),
        dto.deepLink(),
        dto.service(),
        dto.category());
  }
}
