package com.sopt.push.message;

import com.sopt.push.dto.MessageFactoryDto;
import com.sopt.push.util.JsonUtil;

public class MessageCreator {

  private MessageCreator() {}

  public static String create(MessageFactoryDto dto) {
    return switch (dto.topic()) {
      case APNS -> JsonUtil.toJson(ApnsMessageBuilder.build(dto));
      case FCM -> JsonUtil.toJson(FcmMessageBuilder.build(dto));
      case ALL -> JsonUtil.toJson(AllMessageBuilder.build(dto));
    };
  }
}
