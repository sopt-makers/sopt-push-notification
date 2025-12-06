package com.sopt.push.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sopt.push.common.BusinessException;
import com.sopt.push.common.ErrorMessage;

public enum WebHookType {
    SEND("SEND"),
    SEND_ALL("SEND_ALL"),
    FAIL("FAIL");

    private final String value;

    WebHookType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static WebHookType fromValue(String value) {
        if (value == null) {
            throw new BusinessException(
                    ErrorMessage.INVALID_REQUEST, "WebHookType value cannot be null");
        }

        return switch (value) {
            case "SEND" -> SEND;
            case "SEND_ALL" -> SEND_ALL;
            case "FAIL" -> FAIL;
            default -> throw new BusinessException(
                    ErrorMessage.INVALID_REQUEST, "Unknown WebHookType: " + value);
        };
    }
}
