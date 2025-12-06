package com.sopt.push.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.push.common.BusinessException;
import com.sopt.push.common.ErrorMessage;

public class JsonUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonUtil() {}

    public static String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    ErrorMessage.INTERNAL_SERVER_ERROR,
                    "JSON serialization error: " + e.getMessage()
            );
        }
    }
}
