package com.sopt.push.message;

import static com.sopt.push.common.Constants.ALERT;
import static com.sopt.push.common.Constants.APS;
import static com.sopt.push.common.Constants.BODY;
import static com.sopt.push.common.Constants.CATEGORY;
import static com.sopt.push.common.Constants.ID;
import static com.sopt.push.common.Constants.SEND_AT;
import static com.sopt.push.common.Constants.TITLE;
import static java.time.LocalDateTime.now;

import com.sopt.push.dto.MessageFactoryDto;
import com.sopt.push.util.JsonUtil;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApnsMessageBuilder {

  public static String build(MessageFactoryDto dto) {
    Map<String, Object> alert =
        Map.of(
            TITLE, dto.title(),
            BODY, dto.content());

    Map<String, Object> aps = Map.of(ALERT, alert);

    Map<String, Object> payload = new HashMap<>();
    payload.put(APS, aps);
    payload.put(CATEGORY, dto.category().name());
    payload.put(ID, dto.id());
    payload.put(SEND_AT, now());

    return JsonUtil.toJson(payload);
  }
}
