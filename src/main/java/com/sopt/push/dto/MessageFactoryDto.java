package com.sopt.push.dto;

import com.sopt.push.enums.Category;
import com.sopt.push.enums.PushTopic;

public record MessageFactoryDto(
    PushTopic topic,
    String id,
    String title,
    String content,
    Category category,
    String deepLink,
    String webLink) {}
