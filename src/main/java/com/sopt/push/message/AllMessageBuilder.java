package com.sopt.push.message;

import static com.sopt.push.common.Constants.DEFAULT_MESSAGE;

import com.sopt.push.dto.MessageFactoryDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AllMessageBuilder {

  public static Map<String, Object> build(MessageFactoryDto dto) {
    Map<String, Object> message = new HashMap<>();
    message.put("default", DEFAULT_MESSAGE);
    message.put("APNS", ApnsMessageBuilder.build(dto));
    message.put("GCM", FcmMessageBuilder.build(dto));
    return message;
  }
}
