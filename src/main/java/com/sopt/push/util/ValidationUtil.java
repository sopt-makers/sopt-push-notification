package com.sopt.push.util;

import com.sopt.push.common.BusinessException;
import com.sopt.push.common.ErrorMessage;
import com.sopt.push.config.ValidatorConfig;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationUtil {

  private static final Validator VALIDATOR = ValidatorConfig.getValidator();

  public static <T> void validate(T dto) {
    Set<ConstraintViolation<T>> violations = VALIDATOR.validate(dto);
    if (!violations.isEmpty()) {
      throw new BusinessException(ErrorMessage.INVALID_REQUEST);
    }
  }
}
