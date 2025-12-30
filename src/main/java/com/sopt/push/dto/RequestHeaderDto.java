package com.sopt.push.dto;

import com.sopt.push.enums.Actions;
import com.sopt.push.enums.Platform;
import com.sopt.push.enums.Services;

public record RequestHeaderDto(
    String transactionId, Services service, Platform platform, Actions action) {}
