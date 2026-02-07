package com.sopt.push.domain;

import com.sopt.push.enums.Category;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
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

  public HistoryEntity(HistoryEntity historyEntity) {
    this.pk = historyEntity.pk;
    this.sk = historyEntity.sk;
    this.entity = historyEntity.entity;
    this.title = historyEntity.title;
    this.content = historyEntity.content;
    this.deviceToken = historyEntity.deviceToken;
    this.webLink = historyEntity.webLink;
    this.applink = historyEntity.applink;
    this.notificationType = historyEntity.notificationType;
    this.orderServiceName = historyEntity.orderServiceName;
    this.status = historyEntity.status;
    this.action = historyEntity.action;
    this.platform = historyEntity.platform;
    this.category = historyEntity.category;
    this.userIds = historyEntity.userIds;
    this.messageIds = historyEntity.messageIds;
    this.errorCode = historyEntity.errorCode;
    this.errorMessage = historyEntity.errorMessage;
    this.id = historyEntity.id;
  }

  @DynamoDbPartitionKey
  @DynamoDbAttribute("pk")
  public String getPk() {
    return pk;
  }

  @DynamoDbSortKey
  @DynamoDbAttribute("sk")
  public String getSk() {
    return sk;
  }
}
