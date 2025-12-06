package com.sopt.push.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sopt.push.common.BusinessException;
import com.sopt.push.common.ErrorMessage;

public enum NotificationType {
    EMAIL("email"),
    PUSH("push"),
    SMS("sms");

    private final String value;

    NotificationType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    @JsonCreator
    public static NotificationType fromValue(String value) {
       if (value == null) {
           throw new BusinessException(
                   ErrorMessage.INVALID_REQUEST, "NotificationType value cannot be null");
       }

        return switch (value) {
            case "email" -> EMAIL;
            case "push" -> PUSH;
            case "sms" -> SMS;
            default -> throw new BusinessException(
                    ErrorMessage.INVALID_REQUEST, "Unknown NotificationType: " + value);
        };

    }
}
