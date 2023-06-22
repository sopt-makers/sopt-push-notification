import { DynamoDBClient, PutItemCommand, GetItemCommand, UpdateItemCommand, DeleteItemCommand } from '@aws-sdk/client-dynamodb';
import { SNSClient } from '@aws-sdk/client-sns';
import tokenCommands from './modules/tokenCommands';
import snsCommands from './modules/snsCommands';
import response from './constants/response';
import status from './constants/status';
import statusCode from './constants/statusCode';
import { Actions, UserStatus, RequestDTO } from './types';
import responseMessage from './constants/responseMessage';

const client = new DynamoDBClient({ region: process.env.AWS_REGION });
const snsClient = new SNSClient({ region: process.env.AWS_REGION });

export const service = async (event: any): Promise<any> => {
  const eventBody: RequestDTO = JSON.parse(event.body);

  const { fcmToken, userId } = eventBody;

  try {
    switch (event.headers.action) {
      case Actions.REGISTER:
        const getCommand = tokenCommands.getToken(fcmToken, userId);
        const createCommand = tokenCommands.createToken(fcmToken, userId);
        const subscribeCommand = snsCommands.subscribe(fcmToken);

        const data = await client.send(getCommand);

        if (!data.Item) {
          if (userId != UserStatus.UNREGISTERED) {
            const getUnRegisterCommand = tokenCommands.getToken(fcmToken, UserStatus.UNREGISTERED);
            const unRegisteredData = await client.send(getUnRegisterCommand);

            if (unRegisteredData.Item) {
              const deleteUnRegisteredCommand = tokenCommands.deleteToken(fcmToken, UserStatus.UNREGISTERED);
              await client.send(deleteUnRegisteredCommand);
            }
          }

          await snsClient.send(subscribeCommand);

          await client.send(createCommand);
        } else {
          return response(400, status.fail(statusCode.BAD_REQUEST, responseMessage.DUPLICATED_TOKEN));
        }

        return response(200, status.success(statusCode.OK, responseMessage.TOKEN_REGISTER_SUCCESS));

      case Actions.CANCEL:
        const deleteCommand = tokenCommands.deleteToken(fcmToken, userId);
        const result = await client.send(deleteCommand);

        if (!result.Attributes) {
          return response(200, status.success(statusCode.OK, responseMessage.TOKEN_NOT_EXIST));
        }

        return response(200, status.success(statusCode.OK, responseMessage.TOKEN_CANCEL_SUCCESS));
      default:
        return response(400, status.success(statusCode.OK, responseMessage.INVALID_REQUEST));
    }
  } catch (e) {
    console.error(e);
    return response(500, responseMessage.INTERNAL_SERVER_ERROR);
  }
};
