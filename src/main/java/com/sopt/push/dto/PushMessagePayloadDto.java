package com.sopt.push.dto;

import com.sopt.push.enums.Category;

/** 푸시 메시지 내용(타이틀, 본문, 링크 등)을 표현하는 내부용 DTO 입니다. NotificationService와 상위 레이어 간의 데이터 전달에 사용합니다. */
public record PushMessagePayloadDto(
    String id, String title, String content, Category category, String deepLink, String webLink) {}
