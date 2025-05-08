import { APIGatewayProxyEvent, EventBridgeEvent, SNSEvent } from 'aws-lambda';
import { v4 as uuid } from 'uuid';

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
  WebHookType,
  RequestSendAllPushMessageDTO,
  RequestSendPushMessageDTO,
  RequestRegisterUserDTO,
  RequestDeleteTokenDTO,
  PushSuccessMessageDTO,
} from './types';
import responseMessage from './constants/responseMessage';
import * as userService from './services/userService';
import logFactory from './modules/logFactory';
import notificationService from './services/notificationService';
import { DeviceTokenEntity, UserTokenEntity } from './types/tokens';
import { ResponsePushNotification } from './types/notifications';
import User from './constants/user';
import dtoValidator from './modules/dtoValidator';
import webHookService from './services/webHookService';

const registerUser = async (dto: RequestRegisterUserDTO): Promise<void> => {
  const { transactionId, deviceToken, platform, service, userIds } = dto;

  try {
    await logFactory.createLog({
      transactionId,
      userIds,
      deviceToken,
      platform: platform,
      action: Actions.REGISTER,
      notificationType: NotificationType.PUSH,
      orderServiceName: service,
      status: NotificationStatus.START,
    });

    await userService.registerToken(deviceToken, platform, userIds?.[0]);

    await logFactory.createLog({
      transactionId,
      userIds,
      deviceToken,
      platform: platform,
      action: Actions.REGISTER,
      notificationType: NotificationType.PUSH,
      orderServiceName: service,
      status: NotificationStatus.SUCCESS,
    });
  } catch (e) {
    await logFactory.createLog({
      transactionId,
      userIds,
      deviceToken,
      platform: platform,
      action: Actions.REGISTER,
      notificationType: NotificationType.PUSH,
      orderServiceName: service,
      status: NotificationStatus.FAIL,
    });

    throw new Error(`registerUser error: ${e}`);
  }
};

const deleteToken = async (dto: RequestDeleteTokenDTO): Promise<void> => {
  const { deviceToken, service, platform, transactionId, userIds } = dto;

  const logUserIds = ['NULL'];

  await logFactory.createLog({
    transactionId,
    userIds: logUserIds,
    deviceToken,
    platform: platform,
    action: Actions.CANCEL,
    notificationType: NotificationType.PUSH,
    orderServiceName: service,
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
      orderServiceName: service,
      status: NotificationStatus.SUCCESS,
    });
  } catch (e) {
    await logFactory.createLog({
      transactionId,
      userIds: logUserIds,
      deviceToken,
      platform: platform,
      action: Actions.CANCEL,
      notificationType: NotificationType.PUSH,
      orderServiceName: service,
      status: NotificationStatus.FAIL,
    });

    throw new Error(`deleteToken error: ${e}`);
  }
};

const sendPush = async (dto: RequestSendPushMessageDTO) => {
  try {
    const { transactionId, title, content, webLink, deepLink, userIds, service, category } = dto;
    const users = await userService.findTokenByUserIds(userIds);
    if (users.length === 0) {
      return;
    }
    const messageId = uuid();
    const executors = users.map(
      async (user: UserTokenEntity) =>
        await notificationService.platformPush({
          messagePayload: {
            title,
            content,
            webLink,
            deepLink,
            category,
            id: messageId,
          },
          endpointPayload: { endpointArn: user.endpointArn, platform: user.platform },
        }),
    );
    const messageIds = await Promise.all(executors).then((results: (ResponsePushNotification | null)[]) =>
      results
        .filter((result: ResponsePushNotification | null): result is ResponsePushNotification => result !== null)
        .map((result: ResponsePushNotification) => result.messageId),
    );

    const webHookDto: PushSuccessMessageDTO = {
      id: messageId,
      userIds: userIds,
      title: title,
      content: content,
      category: category,
      deepLink: deepLink,
      webLink: webLink,
      service: service,
      type: WebHookType.SEND,
    };

    await webHookService.pushSuccessWebHook(webHookDto);

    await logFactory.createLog({
      transactionId,
      title: title,
      content: content,
      webLink: webLink,
      applink: deepLink,
      notificationType: NotificationType.PUSH,
      orderServiceName: service,
      status: NotificationStatus.SUCCESS,
      action: Actions.SEND,
      messageIds: messageIds,
      platform: Platform.None,
      deviceToken: '',
      category: category,
      id: messageId,
      userIds: userIds.map((userId) => `u#${userId}`),
    });
  } catch (e) {
    throw new Error(`send Push error: ${e}`);
  }
};

const sendPushAll = async (dto: RequestSendAllPushMessageDTO) => {
  try {
    const { transactionId, title, content, category, webLink, deepLink, service } = dto;
    const messageId = uuid();

    const result = await notificationService.allTopicPush({
      messagePayload: {
        title,
        content,
        category,
        webLink,
        deepLink,
        id: messageId,
      },
    });

    if (result === null) {
      throw new Error('sendPushAll error');
    }

    const webHookDto: PushSuccessMessageDTO = {
      id: messageId,
      title: title,
      content: content,
      category: category,
      deepLink: deepLink,
      webLink: webLink,
      service: service,
      type: WebHookType.SEND_ALL,
    };

    await webHookService.pushSuccessWebHook(webHookDto);

    await logFactory.createLog({
      transactionId,
      title: title,
      content: content,
      webLink: webLink,
      applink: deepLink,
      notificationType: NotificationType.PUSH,
      orderServiceName: service,
      status: NotificationStatus.SUCCESS,
      action: Actions.SEND,
      messageIds: [result.messageId],
      platform: Platform.None,
      deviceToken: '',
      category: category,
      userIds: [User.ALL],
      id: messageId,
    });
  } catch (e) {
    throw new Error(`send Push error: ${e}`);
  }
};

const eventBridgeHandler = async (event: EventBridgeEvent<string, any>) => {
  const { header, body } = event.detail;

  if (!header || !body) {
    return response(400, status.success(statusCode.BAD_REQUEST, responseMessage.INVALID_REQUEST));
  }

  const { action, transactionId, service, alarmId } = header;

  try {
    switch (action) {
      case Actions.SEND: {
        const dto: RequestSendPushMessageDTO = {
          ...body,
          transactionId,
          service,
        };

        if (!dtoValidator.toRequestSendPushMessageDto(dto)) {
          return response(400, status.success(statusCode.BAD_REQUEST, responseMessage.INVALID_REQUEST));
        }

        await sendPush(dto);
        await webHookService.scheduleSuccessWebHook(alarmId);
        return response(200, status.success(statusCode.OK, responseMessage.SEND_SUCCESS));
      }
      case Actions.SEND_ALL: {
        const dto: RequestSendAllPushMessageDTO = {
          ...body,
          transactionId,
          service,
        };

        if (!dtoValidator.toRequestSendAllPushMessageDTO(dto)) {
          return response(400, status.success(statusCode.BAD_REQUEST, responseMessage.INVALID_REQUEST));
        }

        await sendPushAll(dto);
        await webHookService.scheduleSuccessWebHook(alarmId);
        return response(200, status.success(statusCode.OK, responseMessage.SEND_SUCCESS));
      }
      default: {
        return response(400, status.success(statusCode.BAD_REQUEST, responseMessage.INVALID_REQUEST));
      }
    }
  } catch (error) {
    return response(500, status.fail(statusCode.INTERNAL_ERROR, responseMessage.INTERNAL_SERVER_ERROR));
  }
};

const apiGateWayHandler = async (event: APIGatewayProxyEvent) => {
  console.log('apiGatewayHandler event', JSON.stringify(event));
  if (event.body === null || event.headers.action === undefined) {
    return response(400, status.success(statusCode.BAD_REQUEST, responseMessage.INVALID_REQUEST));
  }

  const { platform, action, transactionId, service } = event.headers;

  try {
    switch (action) {
      case Actions.REGISTER: {
        const dto: RequestRegisterUserDTO = {
          ...JSON.parse(event.body),
          transactionId,
          service,
          platform,
        };
        if (!dtoValidator.toRequestRegisterUserDto(dto)) {
          return response(400, status.success(statusCode.BAD_REQUEST, responseMessage.INVALID_REQUEST));
        }

        await registerUser(dto);
        return response(200, status.success(statusCode.OK, responseMessage.TOKEN_REGISTER_SUCCESS));
      }
      case Actions.CANCEL: {
        const dto: RequestDeleteTokenDTO = {
          ...JSON.parse(event.body),
          transactionId,
          service,
          platform,
        };
        if (!dtoValidator.toRequestDeleteTokenDto(dto)) {
          return response(400, status.success(statusCode.BAD_REQUEST, responseMessage.INVALID_REQUEST));
        }

        await deleteToken(dto);
        return response(200, status.success(statusCode.OK, responseMessage.TOKEN_CANCEL_SUCCESS));
      }
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
      default: {
        return response(400, status.success(statusCode.BAD_REQUEST, responseMessage.INVALID_REQUEST));
      }
    }
  } catch (e) {
    console.error(e);

    return response(500, status.fail(statusCode.INTERNAL_ERROR, responseMessage.INTERNAL_SERVER_ERROR));
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

export const service = async (event: APIGatewayProxyEvent | EventBridgeEvent<string, any> | SNSEvent): Promise<any> => {
  if ('Records' in event) {
    return await snsHandler(event);
  } else if ('detail' in event) {
    return await eventBridgeHandler(event);
  } else {
    return await apiGateWayHandler(event);
  }
};
