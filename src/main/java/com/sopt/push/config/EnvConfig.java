package com.sopt.push.config;

import lombok.Getter;

@Getter
public final class EnvConfig {

  private static final String DYNAMODB_TABLE_ENV_VAR = "DYNAMODB_TABLE";
  private static final String ALL_TOPIC_ARN_ENV_VAR = "ALL_TOPIC_ARN";
  private static final String MAKERS_APP_SERVER_URL = "MAKERS_APP_SERVER_URL";
  private static final String MAKERS_OPERATION_SERVER_URL = "MAKERS_OPERATION_SERVER_URL";
  private static final String PLATFORM_APPLICATION_IOS_ENV = "PLATFORM_APPLICATION_iOS";
  private static final String PLATFORM_APPLICATION_ANDROID_ENV = "PLATFORM_APPLICATION_ANDROID";

  private final String dynamoDbTableName;
  private final String allTopicArn;
  private final String makersAppServerUrl;
  private final String makersOperationServerUrl;
  private final String platformApplicationIosArn;
  private final String platformApplicationAndroidArn;

  public EnvConfig() {
    this.dynamoDbTableName = getRequiredEnv(DYNAMODB_TABLE_ENV_VAR);
    this.allTopicArn = getRequiredEnv(ALL_TOPIC_ARN_ENV_VAR);
    this.makersAppServerUrl = getRequiredEnv(MAKERS_APP_SERVER_URL);
    this.makersOperationServerUrl = getRequiredEnv(MAKERS_OPERATION_SERVER_URL);
    this.platformApplicationIosArn = getRequiredEnv(PLATFORM_APPLICATION_IOS_ENV);
    this.platformApplicationAndroidArn = getRequiredEnv(PLATFORM_APPLICATION_ANDROID_ENV);
  }

  private static String getRequiredEnv(String key) {
    String value = System.getenv(key);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Required environment variable '" + key + "' is not set.");
    }
    return value;
  }
}
