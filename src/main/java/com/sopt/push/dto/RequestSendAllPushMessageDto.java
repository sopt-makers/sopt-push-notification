package com.sopt.push.dto;

import com.sopt.push.enums.Category;
import com.sopt.push.enums.Services;

public record RequestSendAllPushMessageDto(
        String transactionId,
        Services service,
        String title,
        String content,
        Category category,
        String deepLink,
        String webLink
) {
}
