import tokenFactory from './modules/tokenFactory';
import snsFactory from './modules/snsFactory';
import response from './constants/response';
import status from './constants/status';
import statusCode from './constants/statusCode';
import {
  Actions,
  NotificationStatus,
  NotificationType,
  Platform,
  RequestBodyDTO,
  RequestSendPushMessageDTO,
  Services,
} from './types';
import responseMessage from './constants/responseMessage';
import * as userService from './services/userService';
import { APIGatewayProxyEvent, SNSEvent } from 'aws-lambda';
import logFactory from './modules/logFactory';
import notificationService from './services/notificationService';
import { DeviceTokenEntity, UserTokenEntity } from './types/tokens';
import { isNull, isUndefined } from 'lodash';
import { ResponsePushNotification } from './types/notifications';
import User from './constants/user';

const registerUser = async (
  transactionId: string,
  fcmToken: string,
  platform: Platform,
  service: Services,
  userIds?: string[],
): Promise<void> => {
  try {
    await logFactory.createLog({
      transactionId,
      userIds,
      fcmToken,
      platform: platform as Platform,
      action: Actions.REGISTER,
      notificationType: NotificationType.PUSH,
      orderServiceName: service as Services,
      status: NotificationStatus.START,
    });

    const userId = userIds?.[0];
    const tokenData = await tokenFactory.getToken(fcmToken);

    if (!tokenData.Item) {
      const endPointData = await snsFactory.registerEndPoint(fcmToken, platform);
      const arn = endPointData.EndpointArn;

      if (arn === undefined) {
        throw new Error('arn is undefined');
      }

      const topicData = await snsFactory.subscribe(arn);
      const topicArn = topicData.SubscriptionArn;

      if (topicArn === undefined) {
        throw new Error('topicArn is undefined');
      }

      await tokenFactory.createToken(fcmToken, platform, arn, topicArn, userId);
    } else if (tokenData.Item.sk.S === `u#unknown`) {
      if (tokenData.Item.endpointArn.S === undefined || tokenData.Item.subscriptionArn.S === undefined) {
        throw new Error('endpointArn or topicArn is undefined');
      }

      await tokenFactory.deleteToken(fcmToken);
      await tokenFactory.createToken(
        fcmToken,
        platform,
        tokenData.Item.endpointArn.S,
        tokenData.Item.subscriptionArn.S,
        userId,
      );
    }

    await logFactory.createLog({
      transactionId,
      userIds,
      fcmToken,
      platform: platform as Platform,
      action: Actions.REGISTER,
      notificationType: NotificationType.PUSH,
      orderServiceName: service as Services,
      status: NotificationStatus.SUCCESS,
    });
  } catch (e) {
    await logFactory.createLog({
      transactionId,
      userIds,
      fcmToken,
      platform: platform as Platform,
      action: Actions.REGISTER,
      notificationType: NotificationType.PUSH,
      orderServiceName: service as Services,
      status: NotificationStatus.FAIL,
    });

    throw new Error(`registerUser error: ${e}`);
  }
};

const deleteToken = async (
  fcmToken: string,
  service: Services,
  platform: Platform,
  transactionId: string,
): Promise<void> => {
  const logUserIds = ['NULL'];
  await logFactory.createLog({
    transactionId,
    userIds: logUserIds,
    fcmToken,
    platform: platform as Platform,
    action: Actions.CANCEL,
    notificationType: NotificationType.PUSH,
    orderServiceName: service as Services,
    status: NotificationStatus.START,
  });

  try {
    const deletedData = await tokenFactory.deleteToken(fcmToken);

    const arn = deletedData.Attributes?.endpointArn.S;
    const topicArn = deletedData.Attributes?.subscriptionArn.S;

    if (arn === undefined || topicArn === undefined) {
      throw new Error('arn or topicArn is undefined');
    }

    await Promise.all([snsFactory.cancelEndPoint(arn), snsFactory.unSubscribe(topicArn)]);
    await logFactory.createLog({
      transactionId,
      userIds: logUserIds,
      fcmToken,
      platform: platform as Platform,
      action: Actions.CANCEL,
      notificationType: NotificationType.PUSH,
      orderServiceName: service as Services,
      status: NotificationStatus.SUCCESS,
    });
  } catch (e) {
    await logFactory.createLog({
      transactionId,
      userIds: logUserIds,
      fcmToken,
      platform: platform as Platform,
      action: Actions.CANCEL,
      notificationType: NotificationType.PUSH,
      orderServiceName: service as Services,
      status: NotificationStatus.FAIL,
    });

    throw new Error(`deleteToken error: ${e}`);
  }
};

const sendPush = async (dto: RequestSendPushMessageDTO) => {
  try {
    const { transactionId, title, content, webLink, deepLink, userIds, service } = dto;
    const users = await userService.findTokenByUserIds(userIds);
    if (users.length === 0) {
      return;
    }
    const executors = users.map(
      async (user: UserTokenEntity) =>
        await notificationService.platformPush({
          messagePayload: {
            title,
            content,
            webLink,
            deepLink,
          },
          endpointPayload: { endpointArn: user.endpointArn, platform: user.platform },
        }),
    );
    const messageIds = await Promise.all(executors).then((results: (ResponsePushNotification | null)[]) =>
      results
        .filter((result: ResponsePushNotification | null): result is ResponsePushNotification => !isNull(result))
        .map((result: ResponsePushNotification) => result.messageId),
    );
    await logFactory.createLog({
      transactionId,
      title: title,
      content: content,
      webLink: webLink,
      applink: deepLink,
      notificationType: NotificationType.PUSH,
      orderServiceName: service as Services,
      status: NotificationStatus.SUCCESS,
      action: Actions.SEND,
      messageIds: messageIds,
      platform: Platform.None,
      fcmToken: '',
      userIds: userIds.map((userId) => `u#${userId}`),
    });
    //todo send webHooks
  } catch (e) {
    throw new Error(`send Push error: ${e}`);
  }
};
const sendPushAll = async (dto: RequestSendPushMessageDTO) => {
  try {
    const { transactionId, title, content, webLink, deepLink, service } = dto;

    const result = await notificationService.allTopicPush({
      messagePayload: {
        title,
        content,
        webLink,
        deepLink,
      },
    });

    if (result === null) {
      throw new Error('sendPushAll error');
    }

    await logFactory.createLog({
      transactionId,
      title: title,
      content: content,
      webLink: webLink,
      applink: deepLink,
      notificationType: NotificationType.PUSH,
      orderServiceName: service as Services,
      status: NotificationStatus.SUCCESS,
      action: Actions.SEND,
      messageIds: [result.messageId],
      platform: Platform.None,
      fcmToken: '',
      userIds: [User.ALL],
    });
    //todo send webHooks
  } catch (e) {
    throw new Error(`send Push error: ${e}`);
  }
};

const isEnum = <T extends Record<string, any>>(value: any, enumType: T): value is T => {
  return Object.values(enumType).includes(value);
};

const apiGateWayHandler = async (event: APIGatewayProxyEvent) => {
  if (event.body === null || event.headers.platform === undefined || event.headers.action === undefined) {
    return response(400, status.success(statusCode.BAD_REQUEST, responseMessage.INVALID_REQUEST));
  }
  const eventBody: RequestBodyDTO = JSON.parse(event.body);
  const { platform, action, transactionId, service } = event.headers;
  const { fcmToken, userIds } = eventBody;

  if (
    isEnum(platform, Platform) === false ||
    isEnum(action, Actions) === false ||
    isEnum(service, Services) === false ||
    transactionId === undefined
  ) {
    return response(400, status.success(statusCode.BAD_REQUEST, responseMessage.INVALID_REQUEST));
  }

  try {
    switch (action) {
      case Actions.REGISTER:
        await registerUser(transactionId, fcmToken, platform as Platform, service as Services, userIds);

        return response(200, status.success(statusCode.OK, responseMessage.TOKEN_REGISTER_SUCCESS));
      case Actions.CANCEL:
        await deleteToken(fcmToken, service as Services, platform as Platform, transactionId);

        return response(200, status.success(statusCode.OK, responseMessage.TOKEN_CANCEL_SUCCESS));
      case Actions.SEND:
        await sendPush({
          ...JSON.parse(event.body),
          transactionId,
          service,
        } as RequestSendPushMessageDTO);
        return response(200, status.success(statusCode.OK, responseMessage.SEND_SUCCESS));
      case Actions.SEND_ALL:
        await sendPushAll({
          ...JSON.parse(event.body),
          transactionId,
          service,
        } as RequestSendPushMessageDTO);
        return response(200, status.success(statusCode.OK, responseMessage.SEND_SUCCESS));
      default:
        return response(400, status.success(statusCode.BAD_REQUEST, responseMessage.INVALID_REQUEST));
    }
  } catch (e) {
    console.error(e);

    return response(500, status.success(statusCode.INTERNAL_ERROR, responseMessage.INTERNAL_SERVER_ERROR));
  }
};

const snsHandler = async (event: SNSEvent) => {
  console.log('SNS event', JSON.stringify(event));
  const { Records } = event;
  const deviceTokens = Records.map((record) => record.Sns.Token).filter((token): token is string => isUndefined(token));
  const deviceTokenEntities: DeviceTokenEntity[] = await userService.findUserByTokenIds(deviceTokens);

  for (const record of Records) {
    const deviceToken = deviceTokenEntities.find(
      (deviceTokenEntity) => deviceTokenEntity.pk.split('#')[1] === record.Sns.Token,
    );
    if (isUndefined(deviceToken)) {
      continue;
    }
    await logFactory.createFailLog({
      messageIds: [record.Sns.MessageId],
      userIds: [deviceToken.sk],
    });

    await snsFactory.unSubscribe(deviceToken.subscriptionArn);
    await snsFactory.cancelEndPoint(deviceToken.endpointArn);
    await userService.deleteUser(deviceToken.pk.split('#')[1], deviceToken.sk.split('#')[1]);
  }
  return response(204, status.success(statusCode.NO_CONTENT, responseMessage.NO_CONTENT));
};

export const service = async (event: APIGatewayProxyEvent | SNSEvent): Promise<any> => {
  if ('Records' in event) {
    return await snsHandler(event);
  } else {
    return await apiGateWayHandler(event);
  }
};
