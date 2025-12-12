package com.sopt.push.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

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

  private ValidatorConfig() {}
}
