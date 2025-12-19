package com.sopt.push.dto;

import java.util.Map;

public record CustomEventDetailDto(EventHeaderDto header, Map<String, Object> body) {}
