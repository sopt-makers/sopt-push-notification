package com.sopt.push.dto;

import com.sopt.push.enums.Category;
import com.sopt.push.enums.Services;
import com.sopt.push.enums.WebHookType;
import java.util.Set;

public record PushSuccessMessageDto(
    String id,
    String title,
    String content,
    Category category,
    Services service,
    WebHookType type,
    String deepLink,
    String webLink,
    Set<String> userIds) {}
