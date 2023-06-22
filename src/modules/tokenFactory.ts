import {
  DynamoDBClient,
  PutItemCommand,
  GetItemCommand,
  UpdateItemCommand,
  DeleteItemCommand,
} from '@aws-sdk/client-dynamodb';

const ddbClient = new DynamoDBClient({ region: process.env.AWS_REGION });

const createToken = async (fcmToken: string, userId: string, arn: string, topicArn: string) => {
  const command = new PutItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Item: {
      fcmToken: { S: fcmToken },
      userId: { S: userId },
      arn: { S: arn },
      topicArn: { S: topicArn },
    },
  });

  await ddbClient.send(command);
};

const getToken = async (fcmToken: string) => {
  const command = new GetItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Key: {
      fcmToken: { S: fcmToken },
    },
  });

  const tokenData = await ddbClient.send(command);

  return tokenData;
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

  ddbClient.send(command);
};

const deleteToken = async (fcmToken: string) => {
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

const tokenFactory = {
  createToken,
  getToken,
  updateToken,
  updateUserId,
  deleteToken,
};

export default tokenFactory;
