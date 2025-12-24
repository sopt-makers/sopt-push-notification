package com.sopt.push.dto;

import java.util.Set;

public record DeleteTokenDto(String deviceToken, Set<String> userIds) {}
