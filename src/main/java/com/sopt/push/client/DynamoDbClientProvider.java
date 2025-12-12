package com.sopt.push.client;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class DynamoDbClientProvider {

  private static final DynamoDbEnhancedClient ENHANCED_CLIENT;

  static {
    DynamoDbClient standardClient =
        DynamoDbClient.builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .region(Region.AP_NORTHEAST_2)
            .build();

    ENHANCED_CLIENT = DynamoDbEnhancedClient.builder().dynamoDbClient(standardClient).build();
  }

  private DynamoDbClientProvider() {}

  public static DynamoDbEnhancedClient getClient() {
    return ENHANCED_CLIENT;
  }
}
