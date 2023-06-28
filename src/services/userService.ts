import { AttributeValue } from '@aws-sdk/client-dynamodb';
import { isNil } from 'lodash';
import { UserTokenEntity } from '../types/tokens';
import tokenFactory from '../modules/tokenFactory';
import { Platform } from '../types';

const getTokenByUserId = async (userId: string): Promise<UserTokenEntity | null> => {
  const queryCommandOutput = await tokenFactory.queryTokenByUserId(userId);

  if (isNil(queryCommandOutput.Items)) {
    throw new Error('queryCommandOutput.Items is undefined');
  }

  if (queryCommandOutput.Items.length === 0) {
    return null;
  }
  const queryCommandOutputItems: Record<string, AttributeValue> = queryCommandOutput.Items[0];

  if (
    isNil(queryCommandOutputItems.pk.S) ||
    isNil(queryCommandOutputItems.sk.S) ||
    isNil(queryCommandOutputItems.platform.S) ||
    isNil(queryCommandOutputItems.endpointArn.S) ||
    isNil(queryCommandOutputItems.createdAt.S) ||
    isNil(queryCommandOutputItems.subscriptionArn.S)
  ) {
    throw new Error('queryCommandOutputItems is undefined');
  }

  const tokenEntity: UserTokenEntity = {
    pk: queryCommandOutput.Items[0].pk.S,
    sk: queryCommandOutput.Items[0].sk.S,
    entity: 'user',
    platform: queryCommandOutput.Items[0].platform.S as Platform,
    endpointArn: queryCommandOutput.Items[0].endpointArn.S,
    createdAt: queryCommandOutput.Items[0].createdAt.S,
    subscriptionArn: queryCommandOutput.Items[0].subscriptionArn.S,
  } as UserTokenEntity;

  return tokenEntity;
};

const findTokenByUserIds = async (userIds: string[]): Promise<UserTokenEntity[]> => {
  const result = await Promise.all(userIds.map(async (userId) => getTokenByUserId(userId)));
  return result.filter((user: UserTokenEntity | null): user is UserTokenEntity => user !== null);
};

export { getTokenByUserId, findTokenByUserIds };
