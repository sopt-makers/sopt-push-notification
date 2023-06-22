import { PutItemCommand, GetItemCommand, UpdateItemCommand, DeleteItemCommand } from '@aws-sdk/client-dynamodb';

const createToken = (fcmToken: string, userId: string, arn: string, topicArn: string) => {
  const command = new PutItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Item: {
      fcmToken: { S: fcmToken },
      userId: { S: userId },
      arn: { S: arn },
      topicArn: { S: topicArn },
    },
  });

  return command;
};

const getToken = (fcmToken: string) => {
  const command = new GetItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Key: {
      fcmToken: { S: fcmToken },
    },
  });

  return command;
};

const updateToken = (fcmToken: string) => {
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

  return command;
};

const updateUserId = (fcmToken: string, userId: string) => {
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

  return command;
};

const deleteToken = (fcmToken: string) => {
  const command = new DeleteItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Key: {
      fcmToken: { S: fcmToken },
    },
    ReturnValues: 'ALL_OLD',
  });

  return command;
};

const tokenCommands = {
  createToken,
  getToken,
  updateToken,
  updateUserId,
  deleteToken,
};

export default tokenCommands;
