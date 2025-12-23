package com.sopt.push.dto;

import java.util.Set;

public record RegisterUserDto(
    String deviceToken,
    Set<String> userIds) {}

