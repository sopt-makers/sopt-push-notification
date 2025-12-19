package com.sopt.push.dto;

import com.sopt.push.enums.Platform;

public record UserTokenInfoDto(
    String userId,
    String deviceToken,
    String endpointArn,
    Platform platform,
    String subscriptionArn) {}
