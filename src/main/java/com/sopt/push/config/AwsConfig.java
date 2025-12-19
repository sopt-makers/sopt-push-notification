package com.sopt.push.config;

import com.sopt.push.client.DynamoDbClientProvider;
import com.sopt.push.client.SnsClientProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.sns.SnsClient;

public class AwsConfig {

  private final EnvConfig envConfig;

  public AwsConfig(EnvConfig envConfig) {
    this.envConfig = envConfig;
  }

  public DynamoDbEnhancedClient dynamoClient() {
    return DynamoDbClientProvider.getClient();
  }

  public SnsClient snsClient() {
    return SnsClientProvider.getClient();
  }

  public String tableName() {
    return envConfig.getDynamoDbTableName();
  }
}
