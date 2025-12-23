package com.sopt.push.dto;

import com.sopt.push.enums.Category;

public record SendAllPushDto(
    String title,
    String content,
    Category category,
    String deepLink,
    String webLink) {}

