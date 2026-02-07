package com.sopt.push.repository;

import com.sopt.push.domain.HistoryEntity;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

public class HistoryRepository {

  private final DynamoDbTable<HistoryEntity> historyTable;

  public HistoryRepository(DynamoDbEnhancedClient enhancedClient, String tableName) {
    this.historyTable = enhancedClient.table(tableName, TableSchema.fromBean(HistoryEntity.class));
  }

  public void save(HistoryEntity historyEntity) {
    if (historyEntity == null) {
      throw new IllegalArgumentException("historyEntity must not be null");
    }

    HistoryEntity copy = new HistoryEntity(historyEntity);

    if (historyEntity.getUserIds() == null || historyEntity.getUserIds().isEmpty()) {
      copy.setUserIds(null);
    }
    if (historyEntity.getMessageIds() == null || historyEntity.getMessageIds().isEmpty()) {
      copy.setMessageIds(null);
    }

    historyTable.putItem(copy);
  }
}
