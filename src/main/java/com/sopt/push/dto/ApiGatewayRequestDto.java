package com.sopt.push.dto;

import java.util.Map;

public record ApiGatewayRequestDto(RequestHeaderDto header, Map<String, Object> body) {}
