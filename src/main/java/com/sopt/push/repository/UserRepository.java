package com.sopt.push.repository;

import com.sopt.push.domain.UserEntity;
import java.util.List;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

public class UserRepository {

  private final DynamoDbTable<UserEntity> userTable;

  public UserRepository(DynamoDbEnhancedClient enhancedClient, String tableName) {
    this.userTable = enhancedClient.table(tableName, TableSchema.fromBean(UserEntity.class));
  }

  public void save(UserEntity userEntity) {
    userTable.putItem(userEntity);
  }

  public void delete(String pk, String sk) {
    userTable.deleteItem(Key.builder().partitionValue(pk).sortValue(sk).build());
  }

  public List<UserEntity> queryByPk(String pk) {
    QueryConditional queryConditional =
        QueryConditional.keyEqualTo(Key.builder().partitionValue(pk).build());
    return userTable.query(queryConditional).items().stream().toList();
  }
}
