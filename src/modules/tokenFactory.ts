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

const createToken = async (
  fcmToken: string,
  userId = 'unknown',
  platform: Platform,
  endpointArn: string,
  subscriptionArn: string,
): Promise<void> => {
  const command = new PutItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Item: {
      pk: { S: `u#${userId}` },
      sk: { S: `d#${fcmToken}` },
      entity: { S: Entity.DEVICE_TOKEN },
      platform: { S: platform },
      fcmToken: { S: fcmToken },
      endpointArn: { S: endpointArn },
      subscriptionArn: { S: subscriptionArn },
      createdAt: { S: dayjs().toISOString() },
    },
  });

  await ddbClient.send(command);
};

const getToken = async (fcmToken: string): Promise<GetItemCommandOutput> => {
  const command = new GetItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Key: {
      fcmToken: { S: fcmToken },
    },
  });

  const tokenData = await ddbClient.send(command);

  return tokenData;
};

const updateToken = async (fcmToken: string): Promise<UpdateItemCommandOutput> => {
  const command = new UpdateItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Key: {
      fcmToken: { S: fcmToken },
    },
    UpdateExpression: 'SET fcmToken = :tokenValue',
    ExpressionAttributeValues: {
      ':tokenValue': { S: fcmToken },
    },
  });

  const tokenData = await ddbClient.send(command);

  return tokenData;
};

const deleteToken = async (fcmToken: string): Promise<DeleteItemCommandOutput> => {
  const command = new DeleteItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Key: {
      fcmToken: { S: fcmToken },
    },
    ReturnValues: 'ALL_OLD',
  });

  const deletedData = await ddbClient.send(command);

  return deletedData;
};

const updateUserId = async (fcmToken: string, userId: string): Promise<void> => {
  const command = new UpdateItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Key: {
      fcmToken: { S: fcmToken },
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
