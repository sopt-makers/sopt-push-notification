import tokenFactory from './modules/tokenFactory';
import snsFactory from './modules/snsFactory';
import response from './constants/response';
import status from './constants/status';
import statusCode from './constants/statusCode';
import { Actions, RequestBodyDTO, Platform, NotificationType, Services, NotificationStatus } from './types';
import responseMessage from './constants/responseMessage';
import { APIGatewayProxyEvent } from 'aws-lambda';
import logFactory from './modules/logFactory';

const registerUser = async (
  transactionId: string,
  fcmToken: string,
  platform: Platform,
  userId?: string,
): Promise<void> => {
  try {
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

      await tokenFactory.createToken(fcmToken, userId, platform, arn, topicArn);
    } else if (tokenData.Item.pk.S === `u#unknown`) {
      if (tokenData.Item.endpointArn.S === undefined || tokenData.Item.subscriptionArn.S === undefined) {
        throw new Error('endpointArn or topicArn is undefined');
      }

      await tokenFactory.deleteToken(fcmToken);
      await tokenFactory.createToken(
        fcmToken,
        userId,
        platform,
        tokenData.Item.endpointArn.S,
        tokenData.Item.subscriptionArn.S,
      );
    }
  } catch (e) {
    console.error(e);
    throw new Error(`registerUser error: ${e}`);
  }
};

const deleteToken = async (fcmToken: string): Promise<void> => {
  const deletedData = await tokenFactory.deleteToken(fcmToken);

  const arn = deletedData.Attributes?.arn.S;
  const topicArn = deletedData.Attributes?.topicArn.S;

  if (arn === undefined || topicArn === undefined) {
    throw new Error('arn or topicArn is undefined');
  }

  await Promise.all([snsFactory.cancelEndPoint(arn), snsFactory.unSubscribe(topicArn)]);
};

const isEnum = <T extends Record<string, any>>(value: any, enumType: T): value is T => {
  return Object.values(enumType).includes(value);
};

export const service = async (event: APIGatewayProxyEvent): Promise<any> => {
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
        await logFactory.createLog({
          transactionId,
          userIds: userIds,
          fcmToken,
          platform: platform as Platform,
          action,
          notificationType: NotificationType.PUSH,
          orderServiceName: service as Services,
          status: NotificationStatus.START,
        });
        await registerUser(transactionId, fcmToken, platform as Platform, userIds[0]);
        await logFactory.createLog({
          transactionId,
          userIds: userIds,
          fcmToken,
          platform: platform as Platform,
          action,
          notificationType: NotificationType.PUSH,
          orderServiceName: service as Services,
          status: NotificationStatus.SUCCESS,
        });
        return response(200, status.success(statusCode.OK, responseMessage.TOKEN_REGISTER_SUCCESS));
      case Actions.CANCEL:
        await logFactory.createLog({
          transactionId,
          userIds: userIds,
          fcmToken,
          platform: platform as Platform,
          action,
          notificationType: NotificationType.PUSH,
          orderServiceName: service as Services,
          status: NotificationStatus.START,
        });
        await deleteToken(fcmToken);
        await logFactory.createLog({
          transactionId,
          userIds: userIds,
          fcmToken,
          platform: platform as Platform,
          action,
          notificationType: NotificationType.PUSH,
          orderServiceName: service as Services,
          status: NotificationStatus.SUCCESS,
        });

        return response(200, status.success(statusCode.OK, responseMessage.TOKEN_CANCEL_SUCCESS));
      case Actions.SEND:
        return response(200, status.success(statusCode.OK, responseMessage.SEND_SUCCESS));
      default:
        return response(400, status.success(statusCode.BAD_REQUEST, responseMessage.INVALID_REQUEST));
    }
  } catch (e) {
    console.error(e);
    return response(500, status.success(statusCode.INTERNAL_ERROR, responseMessage.INTERNAL_SERVER_ERROR));
  }
};
