import { DynamoDBClient, PutItemCommand, PutItemCommandOutput } from '@aws-sdk/client-dynamodb';
import dayjs from 'dayjs';
import { v4 as uuid } from 'uuid';
import { Actions, Category, Entity, NotificationStatus, NotificationType, Platform, Services } from '../types';

const ddbClient = new DynamoDBClient({ region: process.env.AWS_REGION });

const createLog = async ({
  transactionId,
  notificationType,
  orderServiceName,
  status,
  action,
  platform,
  deviceToken,
  title = 'NULL',
  content = 'NULL',
  webLink = 'NULL',
  applink = 'NULL',
  category = Category.NONE,
  errorCode = 'NULL',
  errorMessage = 'NULL',
  userIds = ['NULL'],
  messageIds = ['NULL'],
  id = 'NULL',
}: {
  transactionId: string;
  deviceToken: string;
  notificationType: NotificationType;
  orderServiceName: Services;
  status: NotificationStatus;
  action: Actions;
  platform: Platform;
  title?: string;
  content?: string;
  webLink?: string;
  applink?: string;
  category?: Category;
  errorCode?: string;
  errorMessage?: string;
  userIds?: string[];
  messageIds?: string[];
  id?: string;
}): Promise<void> => {
  const now = dayjs();
  const year = now.format('YYYY');
  const month = now.format('MM');

  const command = new PutItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Item: {
      pk: { S: `h#${year}-${month}` },
      sk: { S: `h#${now.toISOString()}#${transactionId}` },
      entity: { S: Entity.HISTORY },
      title: { S: title },
      content: { S: content },
      deviceToken: { S: deviceToken },
      webLink: { S: webLink },
      applink: { S: applink },
      notificationType: { S: notificationType },
      orderServiceName: { S: orderServiceName },
      status: { S: status },
      action: { S: action },
      platform: { S: platform },
      category: { S: category },
      userIds: { SS: userIds },
      messageIds: { SS: messageIds },
      errorCode: { S: errorCode },
      errorMessage: { S: errorMessage },
      id: { S: id },
    },
  });

  await ddbClient.send(command);
};

/**
 *
 * error, ErrorMessage 속성을 SNS Event에서 찾을 수 없어서 일단 제외!
 */
const createFailLog = async (dto: { userIds: string[]; messageIds: string[] }): Promise<PutItemCommandOutput> => {
  const { userIds, messageIds } = dto;
  const now = dayjs();
  const year = now.format('YYYY');
  const month = now.format('MM');
  const transactionId = uuid();
  const command = new PutItemCommand({
    TableName: process.env.DYNAMODB_TABLE,
    Item: {
      pk: { S: `h#${year}-${month}` },
      sk: { S: `h#${now.toISOString()}#${transactionId}` },
      entity: { S: Entity.HISTORY },
      status: { S: 'fail' },
      notificationType: { S: 'sendPushNotification' },
      userIds: { SS: userIds },
      messageIds: { SS: messageIds },
    },
  });

  const result = await ddbClient.send(command);
  if (result.$metadata.httpStatusCode !== 200) {
    throw new Error('createFailLog error');
  }
  return result;
};

const logFactory = {
  createLog,
  createFailLog,
};

export default logFactory;
