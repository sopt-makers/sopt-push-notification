package com.sopt.push.message;

import com.sopt.push.common.Constants;
import com.sopt.push.dto.MessageFactoryDto;
import com.sopt.push.util.JsonUtil;

import java.util.HashMap;
import java.util.Map;

import static java.time.LocalDateTime.now;

public class FcmMessageBuilder {

    private FcmMessageBuilder() {}

    public static Map<String, Object> build(MessageFactoryDto dto) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", dto.id());
        data.put("title", dto.title());
        data.put("content", dto.content());
        data.put("category", dto.category().name());
        data.put("webLink", dto.webLink());
        data.put("deepLink", dto.deepLink());
        data.put("sendAt", now());

        Map<String, Object> payload = Map.of("data", data);

        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("default", Constants.DEFAULT_MESSAGE);
        wrapper.put("GCM", JsonUtil.toJson(payload));

        return wrapper;
    }
}
