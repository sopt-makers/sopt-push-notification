package com.sopt.push.repository;

import static com.sopt.push.common.Constants.TOKEN_PREFIX;

import com.sopt.push.domain.DeviceTokenEntity;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

public class DeviceTokenRepository {

  private final DynamoDbTable<DeviceTokenEntity> deviceTokenTable;

  public DeviceTokenRepository(DynamoDbEnhancedClient enhancedClient, String tableName) {
    this.deviceTokenTable =
        enhancedClient.table(tableName, TableSchema.fromBean(DeviceTokenEntity.class));
  }

  public void save(DeviceTokenEntity deviceTokenEntity) {
    deviceTokenTable.putItem(deviceTokenEntity);
  }

  public void delete(String pk, String sk) {
    deviceTokenTable.deleteItem(Key.builder().partitionValue(pk).sortValue(sk).build());
  }

  public Optional<DeviceTokenEntity> findByPkAndSk(String pk, String sk) {
    Key key = Key.builder().partitionValue(pk).sortValue(sk).build();
    return Optional.ofNullable(deviceTokenTable.getItem(key));
  }

  public List<DeviceTokenEntity> queryByPk(String pk) {
    QueryConditional queryConditional =
        QueryConditional.keyEqualTo(Key.builder().partitionValue(pk).build());
    return deviceTokenTable.query(queryConditional).items().stream().toList();
  }

  public Optional<DeviceTokenEntity> findByDeviceToken(String deviceToken) {
    String pk = TOKEN_PREFIX + deviceToken;
    QueryConditional queryConditional =
        QueryConditional.keyEqualTo(Key.builder().partitionValue(pk).build());
    return deviceTokenTable.query(queryConditional).items().stream().findFirst();
  }
}
