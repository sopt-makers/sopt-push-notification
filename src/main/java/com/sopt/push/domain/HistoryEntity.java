package com.sopt.push.domain;

import com.sopt.push.enums.Category;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@Getter
@Setter
@NoArgsConstructor
public class HistoryEntity {

  private String pk;
  private String sk;
  private String entity;
  private String title;
  private String content;
  private String deviceToken;
  private String webLink;
  private String applink;
  private String notificationType;
  private String orderServiceName;
  private String status;
  private String action;
  private String platform;
  private Category category;
  private Set<String> userIds;
  private Set<String> messageIds;
  private String errorCode;
  private String errorMessage;
  private String id;

  @DynamoDbPartitionKey
  public String getPk() {
    return pk;
  }

  @DynamoDbSortKey
  public String getSk() {
    return sk;
  }
}
