package com.sopt.push.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class ObjectMapperConfig {

  private static final ObjectMapper MAPPER = createObjectMapper();

  private static ObjectMapper createObjectMapper() {
    return new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .registerModule(new JavaTimeModule());
  }

  public static ObjectMapper getObjectMapper() {
    return MAPPER;
  }

  private ObjectMapperConfig() {}
}
