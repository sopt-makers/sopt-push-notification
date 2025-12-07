package com.sopt.push.dto;

import com.sopt.push.enums.Platform;
import com.sopt.push.enums.Services;
import java.util.List;

public record RequestRegisterUserDto(
    String transactionId,
    Services service,
    Platform platform,
    String deviceToken,
    List<String> userIds) {}
