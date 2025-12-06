package com.sopt.push.message;

import static java.time.LocalDateTime.now;

import com.sopt.push.dto.MessageFactoryDto;
import com.sopt.push.util.JsonUtil;
import java.util.HashMap;
import java.util.Map;

public class ApnsMessageBuilder {

  private ApnsMessageBuilder() {}

  public static String build(MessageFactoryDto dto) {
    Map<String, Object> aps =
        Map.of(
            "alert",
            Map.of(
                "title", dto.title(),
                "body", dto.content()));

    Map<String, Object> payload = new HashMap<>();
    payload.put("aps", aps);
    payload.put("category", dto.category().name());
    payload.put("id", dto.id());
    payload.put("webLink", dto.webLink());
    payload.put("deepLink", dto.deepLink());
    payload.put("sendAt", now());

    return JsonUtil.toJson(payload);
  }
}
