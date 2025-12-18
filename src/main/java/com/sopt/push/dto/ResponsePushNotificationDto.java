package com.sopt.push.dto;

/** SNS 발송 후 반환되는 MessageId 를 담는 응답 DTO 입니다. */
public record ResponsePushNotificationDto(String messageId) {}
