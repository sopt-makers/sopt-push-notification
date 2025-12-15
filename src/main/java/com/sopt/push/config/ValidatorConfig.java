package com.sopt.push.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidatorConfig {

  private static final Validator VALIDATOR = createValidator();

  private static Validator createValidator() {
    ValidatorFactory factory =
        Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory();

    return factory.getValidator();
  }

  public static Validator getValidator() {
    return VALIDATOR;
  }
}
