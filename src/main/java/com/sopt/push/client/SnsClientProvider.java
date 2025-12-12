package com.sopt.push.client;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

public class SnsClientProvider {

  private static final SnsClient snsClient;

  static {
    snsClient =
        SnsClient.builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
  }

  private SnsClientProvider() {}

  public static SnsClient getClient() {
    return snsClient;
  }
}
