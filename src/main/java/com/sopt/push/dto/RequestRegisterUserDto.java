package com.sopt.push.dto;

import com.sopt.push.enums.Platform;
import com.sopt.push.enums.Services;
import java.util.Set;

public record RequestRegisterUserDto(
    String transactionId,
    Services service,
    Platform platform,
    String deviceToken,
    Set<String> userIds) {}
