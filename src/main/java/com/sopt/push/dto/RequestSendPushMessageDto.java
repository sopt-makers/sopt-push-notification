package com.sopt.push.dto;

import com.sopt.push.enums.Category;
import com.sopt.push.enums.Services;
import java.util.Set;

public record RequestSendPushMessageDto(
    String transactionId,
    Services service,
    Set<String> userIds,
    String title,
    String content,
    Category category,
    String deepLink,
    String webLink) {}
