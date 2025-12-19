package com.sopt.push.message;

import static com.sopt.push.common.Constants.CATEGORY;
import static com.sopt.push.common.Constants.CONTENT;
import static com.sopt.push.common.Constants.DATA;
import static com.sopt.push.common.Constants.DEEP_LINK;
import static com.sopt.push.common.Constants.ID;
import static com.sopt.push.common.Constants.SEND_AT;
import static com.sopt.push.common.Constants.TITLE;
import static com.sopt.push.common.Constants.WEB_LINK;
import static java.time.LocalDateTime.now;

import com.sopt.push.dto.MessageFactoryDto;
import com.sopt.push.util.JsonUtil;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FcmMessageBuilder {

  public static String build(MessageFactoryDto dto) {
    Map<String, Object> data = new HashMap<>();
    data.put(ID, dto.id());
    data.put(TITLE, dto.title());
    data.put(CONTENT, dto.content());
    data.put(CATEGORY, dto.category().name());
    data.put(WEB_LINK, dto.webLink());
    data.put(DEEP_LINK, dto.deepLink());
    data.put(SEND_AT, now());

    Map<String, Object> payload = Map.of(DATA, data);

    return JsonUtil.toJson(payload);
  }
}
