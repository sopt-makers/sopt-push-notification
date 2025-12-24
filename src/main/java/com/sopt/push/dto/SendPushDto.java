package com.sopt.push.dto;

import com.sopt.push.enums.Category;
import java.util.Set;

public record SendPushDto(
    Set<String> userIds,
    String title,
    String content,
    Category category,
    String deepLink,
    String webLink) {}
