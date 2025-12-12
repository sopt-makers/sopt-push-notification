package com.sopt.push.config;

import lombok.Getter;

@Getter
public class EnvConfig {

    private static final String DYNAMODB_TABLE_ENV_VAR = "DYNAMODB_TABLE";
    private static final String ALL_TOPIC_ARN_ENV_VAR = "ALL_TOPIC_ARN";

    private final String dynamoDbTableName;
    private final String allTopicArn;

    public EnvConfig() {
        this.dynamoDbTableName = System.getenv(DYNAMODB_TABLE_ENV_VAR);
        if (this.dynamoDbTableName == null || this.dynamoDbTableName.isBlank()) {
            throw new IllegalStateException("Required environment variable '" + DYNAMODB_TABLE_ENV_VAR + "' is not set.");
        }

        this.allTopicArn = System.getenv(ALL_TOPIC_ARN_ENV_VAR);
        if (this.allTopicArn == null || this.allTopicArn.isBlank()) {
            throw new IllegalStateException("Required environment variable '" + ALL_TOPIC_ARN_ENV_VAR + "' is not set.");
        }
    }}
