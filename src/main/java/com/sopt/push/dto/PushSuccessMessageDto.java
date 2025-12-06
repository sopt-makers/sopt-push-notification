package com.sopt.push.dto;

import com.sopt.push.enums.Category;
import com.sopt.push.enums.Services;
import com.sopt.push.enums.WebHookType;
import java.util.List;

public record PushSuccessMessageDto(
    String id,
    String title,
    String content,
    Category category,
    Services service,
    WebHookType type,
    String deepLink,
    String webLink,
    List<String> userIds) {}
