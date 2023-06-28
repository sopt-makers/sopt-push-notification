import {
  DynamoDBClient,
  PutItemCommand,
  GetItemCommand,
  UpdateItemCommand,
  DeleteItemCommand,
  GetItemCommandOutput,
  UpdateItemCommandOutput,
  DeleteItemCommandOutput,
  QueryCommand,
  QueryCommandOutput,
} from '@aws-sdk/client-dynamodb';
import { Entity, Platform } from '../types';
import dayjs from 'dayjs';

const ddbClient = new DynamoDBClient({ region: process.env.AWS_REGION });

const getPrimaryKey = (userId = 'unknown', fcmToken: string) => {
  const userInfo = `u#${userId}`;
  const tokenInfo = `d#${fcmToken}`;

  return {
    userInfo,
    tokenInfo,
  };
};

const createToken = async (
  fcmToken: string,
  platform: Platform,
  endpointArn: string,
  subscriptionArn: string,
  userId?: string,
): Promise<void> => {
  const { userInfo, tokenInfo } = getPrimaryKey(userId, fcmToken);
  const createdAt = dayjs().toISOString();

  const userEntityPutCommand = new PutItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Item: {
      pk: { S: userInfo },
      sk: { S: tokenInfo },
      entity: { S: Entity.USER },
      platform: { S: platform },
      endpointArn: { S: endpointArn },
      subscriptionArn: { S: subscriptionArn },
      createdAt: { S: createdAt },
    },
  });
  const putUserEntityRequest = ddbClient.send(userEntityPutCommand);

  const tokenEntityPutCommand = new PutItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Item: {
      pk: { S: tokenInfo },
      sk: { S: userInfo },
      entity: { S: Entity.DEVICE_TOKEN },
      platform: { S: platform },
      endpointArn: { S: endpointArn },
      subscriptionArn: { S: subscriptionArn },
      createdAt: { S: createdAt },
    },
  });
  const putTokenEntityRequest = ddbClient.send(tokenEntityPutCommand);

  await Promise.all([putUserEntityRequest, putTokenEntityRequest]);
};

const getToken = async (fcmToken: string, userId?: string): Promise<GetItemCommandOutput> => {
  const { userInfo, tokenInfo } = getPrimaryKey(userId, fcmToken);

  const command = new GetItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Key: {
      pk: { S: tokenInfo },
      sk: { S: userInfo },
    },
  });

  const tokenData = await ddbClient.send(command);

  return tokenData;
};

const updateToken = async (fcmToken: string): Promise<UpdateItemCommandOutput> => {
  const command = new UpdateItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Key: {
      pk: { S: `d#${fcmToken}` },
    },
    UpdateExpression: 'SET fcmToken = :tokenValue',
    ExpressionAttributeValues: {
      ':tokenValue': { S: `d#${fcmToken}` },
    },
  });

  const tokenData = await ddbClient.send(command);

  return tokenData;
};

const deleteToken = async (fcmToken: string, userId?: string): Promise<DeleteItemCommandOutput> => {
  const { userInfo, tokenInfo } = getPrimaryKey(userId, fcmToken);

  const userEntityDeleteCommand = new DeleteItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Key: {
      pk: { S: userInfo },
      sk: { S: tokenInfo },
    },
    ReturnValues: 'ALL_OLD',
  });
  const userEntityDeleteRequest = ddbClient.send(userEntityDeleteCommand);

  const tokenEntityDeleteCommand = new DeleteItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Key: {
      pk: { S: tokenInfo },
      sk: { S: userInfo },
    },
    ReturnValues: 'ALL_OLD',
  });
  const tokenEntityDeleteRequest = ddbClient.send(tokenEntityDeleteCommand);

  const [deletedUserData] = await Promise.all([userEntityDeleteRequest, tokenEntityDeleteRequest]);

  return deletedUserData;
};

const updateUserId = async (fcmToken: string, userId: string): Promise<void> => {
  const command = new UpdateItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Key: {
      pk: { S: `d#${fcmToken}` },
    },
    UpdateExpression: 'SET userId = :uidValue',
    ExpressionAttributeValues: {
      ':uidValue': { S: userId },
    },
  });

  await ddbClient.send(command);
};

const queryTokenByUserId = async (userId: string): Promise<QueryCommandOutput> => {
  const command = new QueryCommand({
    TableName: process.env.DYNAMODB_TABLE,
    KeyConditionExpression: `pk = :u#${userId} AND begins_with(sk, :d#)`,
    Limit: 1,
  });
  return await ddbClient.send(command);
};

const queryTokenByDeviceToken = async (deviceToken: string): Promise<QueryCommandOutput> => {
  const command = new QueryCommand({
    TableName: process.env.DYNAMODB_TABLE,
    KeyConditionExpression: `pk = :d#${deviceToken} AND begins_with(sk, :u#)`,
    Limit: 1,
  });
  return await ddbClient.send(command);
};

const tokenFactory = {
  createToken,
  getToken,
  updateToken,
  updateUserId,
  deleteToken,
  queryTokenByUserId,
};

export default tokenFactory;
