import { PutItemCommand, GetItemCommand, UpdateItemCommand, DeleteItemCommand } from '@aws-sdk/client-dynamodb';

const createToken = (fcmToken: string, userId: string) => {
  const command = new PutItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Item: {
      fcmToken: { S: fcmToken },
      userId: { S: userId },
    },
  });

  return command;
};

const getToken = (fcmToken: string, userId: string) => {
  const command = new GetItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Key: {
      fcmToken: { S: fcmToken },
      userId: { S: userId },
    },
  });

  return command;
};

const updateToken = (fcmToken: string, userId: string) => {
  const command = new UpdateItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Key: {
      userId: { S: userId },
      fcmToken: { S: fcmToken },
    },
    UpdateExpression: 'SET fcmToken = :tokenValue',
    ExpressionAttributeValues: {
      ':tokenValue': { S: fcmToken },
    },
  });

  return command;
};

const deleteToken = (fcmToken: string, userId: string) => {
  const command = new DeleteItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Key: {
      userId: { S: userId },
      fcmToken: { S: fcmToken },
    },
  });

  return command;
};

const tokenCommands = {
  createToken,
  getToken,
  updateToken,
  deleteToken,
};

export default tokenCommands;
