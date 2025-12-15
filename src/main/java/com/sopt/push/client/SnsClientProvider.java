package com.sopt.push.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.sns.SnsClient;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SnsClientProvider {

  private static final SnsClient SNS_CLIENT;

  static {
    SNS_CLIENT = SnsClient.builder().build();
  }

  public static SnsClient getClient() {
    return SNS_CLIENT;
  }
}
