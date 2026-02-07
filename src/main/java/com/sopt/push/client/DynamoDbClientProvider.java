package com.sopt.push.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DynamoDbClientProvider {

  private static final DynamoDbEnhancedClient ENHANCED_CLIENT;

  static {
    DynamoDbClient standardClient = DynamoDbClient.builder().build();
    ENHANCED_CLIENT = DynamoDbEnhancedClient.builder().dynamoDbClient(standardClient).build();
  }

  public static DynamoDbEnhancedClient getClient() {
    return ENHANCED_CLIENT;
  }
}
