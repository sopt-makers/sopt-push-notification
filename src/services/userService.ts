import { AttributeValue, QueryCommandOutput } from '@aws-sdk/client-dynamodb';
import { DeviceTokenEntity, UserTokenEntity } from '../types/tokens';
import tokenFactory from '../modules/tokenFactory';
import { Platform } from '../types';
import user from '../constants/user';
import snsFactory from '../modules/snsFactory';
import { CreatePlatformEndpointCommandOutput } from '@aws-sdk/client-sns';

function isTokenUserEntity(queryCommandOutputItems: Record<string, AttributeValue>): queryCommandOutputItems is Record<
  string,
  AttributeValue
> & {
  pk: AttributeValue.SMember;
  sk: AttributeValue.SMember;
  platform: AttributeValue.SMember;
  endpointArn: AttributeValue.SMember;
  createdAt: AttributeValue.SMember;
  subscriptionArn: AttributeValue.SMember;
} {
  if (
    queryCommandOutputItems.pk.S === undefined ||
    queryCommandOutputItems.sk.S === undefined ||
    queryCommandOutputItems.platform.S === undefined ||
    queryCommandOutputItems.endpointArn.S === undefined ||
    queryCommandOutputItems.createdAt.S === undefined ||
    queryCommandOutputItems.subscriptionArn.S === undefined
  ) {
    return false;
  }

  if (queryCommandOutputItems.pk.S.split('#').length !== 2) {
    return false;
  }
  if (queryCommandOutputItems.sk.S.split('#').length !== 2) {
    return false;
  }
  return true;
}

const getTokenByUserId = async (userId: string): Promise<UserTokenEntity | null> => {
  const queryCommandOutput = await tokenFactory.queryTokenByUserId(userId);

  if (queryCommandOutput.Items === undefined) {
    throw new Error('queryCommandOutput.Items is undefined');
  }

  if (queryCommandOutput.Items.length === 0) {
    return null;
  }
  const queryCommandOutputItems: Record<string, AttributeValue> = queryCommandOutput.Items[0];

  if (!isTokenUserEntity(queryCommandOutputItems)) {
    throw new Error('queryCommandOutputItems is not UserTokenEntity');
  }

  const tokenEntity: UserTokenEntity = {
    userId: queryCommandOutputItems.pk.S.split('#')[1],
    deviceToken: queryCommandOutputItems.sk.S.split('#')[1],
    entity: 'user',
    platform: queryCommandOutputItems.platform.S as Platform,
    endpointArn: queryCommandOutputItems.endpointArn.S,
    createdAt: queryCommandOutputItems.createdAt.S,
    subscriptionArn: queryCommandOutputItems.subscriptionArn.S,
  } as UserTokenEntity;

  return tokenEntity;
};

const getUserByTokenId = async (deviceToken: string): Promise<DeviceTokenEntity | null> => {
  const queryCommandOutput: QueryCommandOutput = await tokenFactory.queryTokenByDeviceToken(deviceToken);
  if (queryCommandOutput.Items === undefined) {
    return null;
  }

  if (queryCommandOutput.Items.length === 0) {
    return null;
  }
  const queryCommandOutputItems: Record<string, AttributeValue> = queryCommandOutput.Items[0];

  if (!isTokenUserEntity(queryCommandOutputItems)) {
    throw new Error('queryCommandOutputItems is not DeviceTokenEntity');
  }

  const tokenEntity: DeviceTokenEntity = {
    deviceToken: queryCommandOutputItems.pk.S.split('#')[1],
    userId: queryCommandOutputItems.sk.S.split('#')[1],
    entity: 'deviceToken',
    platform: queryCommandOutputItems.platform.S as Platform,
    endpointArn: queryCommandOutputItems.endpointArn.S,
    createdAt: queryCommandOutputItems.createdAt.S,
    subscriptionArn: queryCommandOutputItems.subscriptionArn.S,
  } as DeviceTokenEntity;

  return tokenEntity;
};

const findTokenByUserIds = async (userIds: string[]): Promise<UserTokenEntity[]> => {
  const result = await Promise.all(userIds.map(async (userId) => getTokenByUserId(userId)));
  return result.filter((user: UserTokenEntity | null): user is UserTokenEntity => user !== null);
};

const findUserByTokenIds = async (deviceTokens: string[]): Promise<DeviceTokenEntity[]> => {
  const result = await Promise.all(deviceTokens.map(async (deviceToken) => getUserByTokenId(deviceToken)));
  return result.filter((user: DeviceTokenEntity | null): user is DeviceTokenEntity => user !== null);
};

const changedUserPayload = (tokenUserId: string, inputUserId?: string): boolean => {
  if (inputUserId === tokenUserId) {
    return false;
  }

  if (inputUserId === undefined && tokenUserId === user.UNKNOWN) {
    return false;
  }

  return true;
};

const registerToken = async (deviceToken: string, platform: Platform, userId?: string): Promise<void> => {
  const result: DeviceTokenEntity | null = await getUserByTokenId(deviceToken);

  if (result) {
    if (!changedUserPayload(result.userId, userId)) {
      return;
    }
    await unRegisterToken(result);
  }

  const endpoint: CreatePlatformEndpointCommandOutput = await snsFactory.registerEndPoint(
    deviceToken,
    platform,
    userId,
  );
  if (endpoint.EndpointArn === undefined) {
    console.error('endpointArn is undefined', endpoint.$metadata.httpStatusCode);
    throw new Error('endpointArn is undefined');
  }

  const subscriptionArn = await snsFactory.subscribe(endpoint.EndpointArn);
  if (subscriptionArn.SubscriptionArn === undefined) {
    console.error('subscriptionArn is undefined', endpoint.$metadata.httpStatusCode);
    throw new Error('subscriptionArn is undefined');
  }
  await tokenFactory.createToken(deviceToken, platform, endpoint.EndpointArn, subscriptionArn.SubscriptionArn, userId);
};

const unRegisterToken = async (deviceTokenEntity: DeviceTokenEntity): Promise<void> => {
  const { deviceToken, userId, subscriptionArn, endpointArn } = deviceTokenEntity;
  await tokenFactory.deleteToken(deviceToken, userId);
  await snsFactory.unSubscribe(subscriptionArn);
  await snsFactory.cancelEndPoint(endpointArn);
};

export { getTokenByUserId, findTokenByUserIds, findUserByTokenIds, unRegisterToken, registerToken };
