package com.sopt.push.dto;

import com.sopt.push.enums.Category;
import java.util.Set;

public record CreateHistoryDto(
    String transactionId,
    String title,
    String content,
    String webLink,
    String applink,
    String notificationType,
    String orderServiceName,
    String status,
    String action,
    String platform,
    String deviceToken,
    Category category,
    Set<String> userIds,
    String id,
    Set<String> messageIds,
    String errorCode,
    String errorMessage) {}
