package com.sopt.push.dto;

import com.sopt.push.enums.Actions;
import com.sopt.push.enums.Category;
import com.sopt.push.enums.NotificationStatus;
import com.sopt.push.enums.NotificationType;
import com.sopt.push.enums.Platform;
import com.sopt.push.enums.Services;

import java.util.Set;

public record LogCreateRequestDto(
        String transactionId,
        String entity,
        String title,
        String content,
        String deviceToken,
        String webLink,
        String applink,
        NotificationType notificationType,
        Services orderServiceName,
        NotificationStatus status,
        Actions action,
        Platform platform,
        Category category,
        String errorCode,
        String errorMessage,
        Set<String> userIds,
        Set<String> messageIds,
        String id
) {
    private static final String NULL = "NULL";

    private static String nvl(String v) { return (v == null || v.isBlank()) ? NULL : v; }
    private static Set<String> normalizeSet(Set<String> s) { return (s == null || s.isEmpty()) ? Set.of(NULL) : s; }
}

