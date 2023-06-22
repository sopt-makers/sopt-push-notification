import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { SNSClient } from '@aws-sdk/client-sns';
import tokenCommands from './modules/tokenCommands';
import snsCommands from './modules/snsCommands';
import response from './constants/response';
import status from './constants/status';
import statusCode from './constants/statusCode';
import { Actions, RequestDTO } from './types';
import responseMessage from './constants/responseMessage';

const ddbClient = new DynamoDBClient({ region: process.env.AWS_REGION });
const snsClient = new SNSClient({ region: process.env.AWS_REGION });

export const service = async (event: any): Promise<any> => {
  const eventBody: RequestDTO = JSON.parse(event.body);

  const { platform, action } = event.headers;

  const { fcmToken, userId } = eventBody;

  try {
    switch (action) {
      case Actions.REGISTER:
        const getTokenCommand = tokenCommands.getToken(fcmToken);
        const updateUserIdCommand = tokenCommands.updateUserId(fcmToken, userId);
        const registerEndPointCommand = snsCommands.registerEndPoint(fcmToken, platform);

        const tokenData = await ddbClient.send(getTokenCommand);

        if (!tokenData.Item) {
          const endPointData = await snsClient.send(registerEndPointCommand);

          const arn = endPointData.EndpointArn;

          const subscribeCommand = snsCommands.subscribe(arn!);

          const topicSubscribeData = await snsClient.send(subscribeCommand);

          const topicArn = topicSubscribeData.SubscriptionArn;

          const createTokenCommand = tokenCommands.createToken(fcmToken, userId, arn!, topicArn);

          await ddbClient.send(createTokenCommand);
        } else if (tokenData.Item.userId.S === '') {
          ddbClient.send(updateUserIdCommand);
        }

        return response(200, status.success(statusCode.OK, responseMessage.TOKEN_REGISTER_SUCCESS));

      case Actions.CANCEL:
        const deleteCommand = tokenCommands.deleteToken(fcmToken);
        const deletedData = await ddbClient.send(deleteCommand);

        const arn = deletedData.Attributes!.arn.S;
        const topicArn = deletedData.Attributes!.topicArn.S;

        const cancelEndPointCommand = snsCommands.cancelEndPoint(arn!);
        const unSubscribeCommand = snsCommands.unSubscribe(topicArn!);

        await Promise.all([snsClient.send(cancelEndPointCommand), snsClient.send(unSubscribeCommand)]);

        return response(200, status.success(statusCode.OK, responseMessage.TOKEN_CANCEL_SUCCESS));
      default:
        return response(400, status.success(statusCode.OK, responseMessage.INVALID_REQUEST));
    }
  } catch (e) {
    console.error(e);
    return response(400, status.success(statusCode.INTERNAL_ERROR, responseMessage.INTERNAL_SERVER_ERROR));
  }
};
