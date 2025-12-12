package com.sopt.push.message;

import static com.sopt.push.common.Constants.DEFAULT_MESSAGE;

import com.sopt.push.dto.MessageFactoryDto;
import java.util.HashMap;
import java.util.Map;

public class AllMessageBuilder {

  private AllMessageBuilder() {}

  public static Map<String, Object> build(MessageFactoryDto dto) {
    Map<String, Object> message = new HashMap<>();
    message.put("default", DEFAULT_MESSAGE);
    message.put("APNS", ApnsMessageBuilder.build(dto));
    message.put("GCM", FcmMessageBuilder.build(dto));
    return message;
  }
}
