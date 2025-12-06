package com.sopt.push.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sopt.push.common.BusinessException;
import com.sopt.push.common.ErrorMessage;

public enum User {
    ALL("u#all");

    private final String value;

    User(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static User fromValue(String value) {
        if (value == null) {
            throw new BusinessException(
                    ErrorMessage.INVALID_REQUEST, "User value cannot be null");
        }

        if (value.equalsIgnoreCase("u#all")) {
            return ALL;
        } else {
            throw new BusinessException(
                    ErrorMessage.INVALID_REQUEST, "Unknown User: " + value);
        }
    }
}
