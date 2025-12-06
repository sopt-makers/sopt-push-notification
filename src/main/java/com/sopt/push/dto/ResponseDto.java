package com.sopt.push.dto;

import com.sopt.push.common.SuccessMessage;

public record ResponseDto<T>(
        int status,
        String message,
        T data
) {
    public static <T> ResponseDto<T> success(SuccessMessage msg, T data) {
        return new ResponseDto<>(msg.getHttpStatus(), msg.getMessage(), data);
    }

    public static <T> ResponseDto<T> success(SuccessMessage msg) {
        return new ResponseDto<>(msg.getHttpStatus(), msg.getMessage(), null);
    }

    public static <T> ResponseDto<T> fail(int status, String message) {
        return new ResponseDto<>(status, message, null);
    }
}
