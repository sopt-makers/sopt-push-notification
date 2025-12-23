package com.sopt.push.dto;

import java.util.Map;

public record ApiGatewayRequestDto(
    RegisterHeaderDto header,
    Map<String, Object> body) {}

