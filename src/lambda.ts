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
  RequestSendAllPushMessageDTO,
  RequestSendPushMessageDTO,
  Services,
} from './types';
import responseMessage from './constants/responseMessage';
import * as userService from './services/userService';
import { APIGatewayProxyEvent, SNSEvent } from 'aws-lambda';
import logFactory from './modules/logFactory';
import notificationService from './services/notificationService';
import { DeviceTokenEntity, UserTokenEntity } from './types/tokens';
import { ResponsePushNotification } from './types/notifications';
import User from './constants/user';
import dtoValidator from './modules/dtoValidator';

const registerUser = async (
  transactionId: string,
  deviceToken: string,
  platform: Platform,
  service: Services,
  userIds?: string[],
): Promise<void> => {
  try {
    await logFactory.createLog({
      transactionId,
      userIds,
      deviceToken,
      platform: platform as Platform,
      action: Actions.REGISTER,
      notificationType: NotificationType.PUSH,
      orderServiceName: service as Services,
      status: NotificationStatus.START,
    });

    await userService.registerToken(deviceToken, platform, userIds?.[0]);

    await logFactory.createLog({
      transactionId,
      userIds,
      deviceToken,
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
      deviceToken,
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
  deviceToken: string,
  service: Services,
  platform: Platform,
  transactionId: string,
  userIds?: string[],
): Promise<void> => {
  const logUserIds = ['NULL'];
  await logFactory.createLog({
    transactionId,
    userIds: logUserIds,
    deviceToken,
    platform: platform as Platform,
    action: Actions.CANCEL,
    notificationType: NotificationType.PUSH,
    orderServiceName: service as Services,
    status: NotificationStatus.START,
  });

  try {
    const deletedData = await tokenFactory.deleteToken(deviceToken, userIds?.[0]);

    const arn = deletedData.Attributes?.endpointArn.S;
    const topicArn = deletedData.Attributes?.subscriptionArn.S;

    if (arn === undefined || topicArn === undefined) {
      throw new Error('arn or topicArn is undefined');
    }

    await Promise.all([snsFactory.cancelEndPoint(arn), snsFactory.unSubscribe(topicArn)]);
    await logFactory.createLog({
      transactionId,
      userIds: logUserIds,
      deviceToken,
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
      deviceToken,
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
        .filter((result: ResponsePushNotification | null): result is ResponsePushNotification => result !== null)
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
      deviceToken: '',
      userIds: userIds.map((userId) => `u#${userId}`),
    });
    //todo send webHooks
  } catch (e) {
    throw new Error(`send Push error: ${e}`);
  }
};
const sendPushAll = async (dto: RequestSendAllPushMessageDTO) => {
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
      deviceToken: '',
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
  const { deviceToken, userIds } = eventBody;

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
        await registerUser(transactionId, deviceToken, platform as Platform, service as Services, userIds);

        return response(200, status.success(statusCode.OK, responseMessage.TOKEN_REGISTER_SUCCESS));
      case Actions.CANCEL:
        await deleteToken(deviceToken, service as Services, platform as Platform, transactionId, userIds);

        return response(200, status.success(statusCode.OK, responseMessage.TOKEN_CANCEL_SUCCESS));
      case Actions.SEND: {
        const dto: RequestSendPushMessageDTO = {
          ...JSON.parse(event.body),
          transactionId,
          service,
        };
        if (!dtoValidator.toRequestSendPushMessageDto(dto)) {
          return response(400, status.success(statusCode.BAD_REQUEST, responseMessage.INVALID_REQUEST));
        }
        await sendPush(dto);
        return response(200, status.success(statusCode.OK, responseMessage.SEND_SUCCESS));
      }
      case Actions.SEND_ALL: {
        const dto: RequestSendAllPushMessageDTO = {
          ...JSON.parse(event.body),
          transactionId,
          service,
        };

        if (!dtoValidator.toRequestSendAllPushMessageDTO(dto)) {
          return response(400, status.success(statusCode.BAD_REQUEST, responseMessage.INVALID_REQUEST));
        }

        await sendPushAll(dto);
        return response(200, status.success(statusCode.OK, responseMessage.SEND_SUCCESS));
      }
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
  const deviceTokens: string[] = Records.map((record) => record.Sns.Token).filter(
    (token): token is string => token !== undefined,
  );
  const deviceTokenEntities: DeviceTokenEntity[] = await userService.findUserByTokenIds(deviceTokens);

  for (const record of Records) {
    const deviceToken = deviceTokenEntities.find(
      (deviceTokenEntity) => deviceTokenEntity.deviceToken === record.Sns.Token,
    );
    if (deviceToken === undefined) {
      continue;
    }
    await logFactory.createFailLog({
      messageIds: [record.Sns.MessageId],
      userIds: [deviceToken.userId],
    });
    await userService.unRegisterToken(deviceToken);
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
