package com.sopt.push.common;

public class InvalidEndpointException extends RuntimeException {

  private final String endpointArn;

  public InvalidEndpointException(String endpointArn, Throwable cause) {
    super("Invalid SNS endpoint: " + endpointArn, cause);
    this.endpointArn = endpointArn;
  }
}