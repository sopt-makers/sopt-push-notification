import tokenFactory from './modules/tokenFactory';
import snsFactory from './modules/snsFactory';
import response from './constants/response';
import status from './constants/status';
import statusCode from './constants/statusCode';
import { Actions, RequestDTO, Platform } from './types';
import responseMessage from './constants/responseMessage';

export const service = async (event: any): Promise<any> => {
  const eventBody: RequestDTO = JSON.parse(event.body);

  const platform: Platform = event.headers.platform;
  const action: Actions = event.headers.action;

  const { fcmToken, userId } = eventBody;

  try {
    switch (action) {
      case Actions.REGISTER:
        const tokenData = await tokenFactory.getToken(fcmToken);

        if (!tokenData.Item) {
          const endPointData = await snsFactory.registerEndPoint(fcmToken, platform);

          const arn = endPointData.EndpointArn;

          const topicData = await snsFactory.subscribe(arn!);

          const topicArn = topicData.SubscriptionArn;

          await tokenFactory.createToken(fcmToken, userId, arn!, topicArn!);
        } else if (tokenData.Item.userId.S === '') {
          tokenFactory.updateUserId(fcmToken, userId);
        }

        return response(200, status.success(statusCode.OK, responseMessage.TOKEN_REGISTER_SUCCESS));

      case Actions.CANCEL:
        const deletedData = await tokenFactory.deleteToken(fcmToken);

        const arn = deletedData.Attributes!.arn.S;
        const topicArn = deletedData.Attributes!.topicArn.S;

        await Promise.all([snsFactory.cancelEndPoint(arn!), snsFactory.unSubscribe(topicArn!)]);

        return response(200, status.success(statusCode.OK, responseMessage.TOKEN_CANCEL_SUCCESS));
      default:
        return response(400, status.success(statusCode.OK, responseMessage.INVALID_REQUEST));
    }
  } catch (e) {
    console.error(e);
    return response(400, status.success(statusCode.INTERNAL_ERROR, responseMessage.INTERNAL_SERVER_ERROR));
  }
};
