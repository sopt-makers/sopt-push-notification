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

const getPrimaryKey = (userId = 'unknown', deviceToken: string) => {
  const userInfo = `u#${userId}`;
  const tokenInfo = `d#${deviceToken}`;

  return {
    userInfo,
    tokenInfo,
  };
};

const createToken = async (
  deviceToken: string,
  platform: Platform,
  endpointArn: string,
  subscriptionArn: string,
  userId?: string,
): Promise<void> => {
  const { userInfo, tokenInfo } = getPrimaryKey(userId, deviceToken);
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

const getToken = async (deviceToken: string, userId?: string): Promise<GetItemCommandOutput> => {
  const { userInfo, tokenInfo } = getPrimaryKey(userId, deviceToken);

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

const updateToken = async (deviceToken: string): Promise<UpdateItemCommandOutput> => {
  const command = new UpdateItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Key: {
      pk: { S: `d#${deviceToken}` },
    },
    UpdateExpression: 'SET deviceToken = :tokenValue',
    ExpressionAttributeValues: {
      ':tokenValue': { S: `d#${deviceToken}` },
    },
  });

  const tokenData = await ddbClient.send(command);

  return tokenData;
};

const deleteToken = async (deviceToken: string, userId?: string): Promise<DeleteItemCommandOutput> => {
  const { userInfo, tokenInfo } = getPrimaryKey(userId, deviceToken);

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

const updateUserId = async (deviceToken: string, userId: string): Promise<void> => {
  const command = new UpdateItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Key: {
      pk: { S: `d#${deviceToken}` },
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
    KeyConditionExpression: `pk = :pk AND begins_with(sk, :sk)`,
    ExpressionAttributeValues: {
      ':pk': { S: `u#${userId}` },
      ':sk': { S: 'd#' },
    },
  });
  return await ddbClient.send(command);
};

const queryTokenByDeviceToken = async (deviceToken: string): Promise<QueryCommandOutput> => {
  const command = new QueryCommand({
    TableName: process.env.DYNAMODB_TABLE,
    KeyConditionExpression: `pk = :pk AND begins_with(sk, :sk)`,
    ExpressionAttributeValues: {
      ':pk': { S: `d#${deviceToken}` },
      ':sk': { S: 'u#' },
    },
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
  queryTokenByDeviceToken,
};

export default tokenFactory;
