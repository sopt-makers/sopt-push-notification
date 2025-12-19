package com.sopt.push.dto;

import com.sopt.push.enums.Actions;
import com.sopt.push.enums.Services;

public record EventHeaderDto(
    Actions action, String transactionId, Services service, String alarmId) {}
