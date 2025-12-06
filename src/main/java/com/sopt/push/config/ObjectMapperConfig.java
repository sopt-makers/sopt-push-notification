package com.sopt.push.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class ObjectMapperConfig {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private ObjectMapperConfig() {}

  static {
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  public static ObjectMapper getObjectMapper() {
    return objectMapper;
  }
}
