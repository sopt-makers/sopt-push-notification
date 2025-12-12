package com.sopt.push.client;

import software.amazon.awssdk.services.sns.SnsClient;

public class SnsClientProvider {

  private static final SnsClient snsClient;

  static {
    snsClient = SnsClient.builder().build();
  }

  private SnsClientProvider() {}

  public static SnsClient getClient() {
    return snsClient;
  }
}
