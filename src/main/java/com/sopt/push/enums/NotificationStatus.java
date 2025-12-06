package com.sopt.push.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sopt.push.common.BusinessException;
import com.sopt.push.common.ErrorMessage;

public enum NotificationStatus {
    START("start"),
    FAIL("fail"),
    SUCCESS("success");

    private final String value;

    NotificationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    @JsonCreator
    public static NotificationStatus fromValue(String value) {
        if (value == null) {
            throw new BusinessException(
                    ErrorMessage.INVALID_REQUEST, "NotificationStatus cannot be null"
            );
        }

        return switch (value.toLowerCase()) {
            case "start" -> START;
            case "fail" -> FAIL;
            case "success" -> SUCCESS;
            default -> throw new BusinessException(
                    ErrorMessage.INVALID_REQUEST, "Unknown NotificationStatus: " + value
            );
        };
    }

}
