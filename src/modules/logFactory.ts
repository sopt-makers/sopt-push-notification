import { DynamoDBClient, PutItemCommand } from '@aws-sdk/client-dynamodb';
import dayjs from 'dayjs';
import { Actions, Entity, NotificationStatus, NotificationType, Platform, Services } from '../types';

const ddbClient = new DynamoDBClient({ region: process.env.AWS_REGION });

const createLog = async ({
  transactionId,
  title = 'NULL',
  content = 'NULL',
  fcmToken,
  webLink = 'NULL',
  applink = 'NULL',
  notificationType,
  orderServiceName,
  status,
  action,
  platform,
  errorCode = 'NULL',
  errorMessage = 'NULL',
  userIds = ['NULL'],
  messageIds = ['NULL'],
}: {
  transactionId: string;
  title?: string;
  content?: string;
  fcmToken: string;
  webLink?: string;
  applink?: string;
  notificationType: NotificationType;
  orderServiceName: Services;
  status: NotificationStatus;
  action: Actions;
  platform: Platform;
  errorCode?: string;
  errorMessage?: string;
  userIds?: string[];
  messageIds?: string[];
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
      fcmToken: { S: fcmToken },
      webLink: { S: webLink },
      applink: { S: applink },
      notificationType: { S: notificationType },
      orderServiceName: { S: orderServiceName },
      status: { S: status },
      action: { S: action },
      platform: { S: platform },
      userIds: { SS: userIds },
      messageIds: { SS: messageIds },
      errorCode: { S: errorCode },
      errorMessage: { S: errorMessage },
    },
  });

  await ddbClient.send(command);
};

const logFactory = {
  createLog,
};

export default logFactory;
