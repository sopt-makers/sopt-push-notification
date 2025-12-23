package com.sopt.push.dto;

import com.sopt.push.enums.Actions;
import com.sopt.push.enums.Platform;
import com.sopt.push.enums.Services;

public record RegisterHeaderDto(
    String transactionId,
    Services service,
    Platform platform,
    Actions action) {
  
  public RegisterHeaderDto {
    if ((action == Actions.REGISTER || action == Actions.CANCEL) && platform == null) {
      throw new IllegalArgumentException("Platform is required for REGISTER and CANCEL actions");
    }
  }
}

