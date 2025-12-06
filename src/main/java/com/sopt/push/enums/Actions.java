package com.sopt.push.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sopt.push.common.BusinessException;
import com.sopt.push.common.ErrorMessage;

public enum Actions {
    REGISTER("register"),
    CANCEL("cancel"),
    SEND("send"),
    SEND_ALL("sendAll");

    private final String value;

    Actions(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    @JsonCreator
    public static Actions fromValue(String value) {
        if (value == null) {
            throw new BusinessException(
                    ErrorMessage.INVALID_REQUEST, "Actions value cannot be null"
            );
        }

        return switch (value) {
            case "register" -> REGISTER;
            case "cancel" -> CANCEL;
            case "send" -> SEND;
            case "sendAll" -> SEND_ALL;
            default -> throw new BusinessException(
                    ErrorMessage.INVALID_REQUEST, "Unknown Actions: " + value
            );
        };
    }

}
