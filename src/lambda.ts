import { DynamoDBClient, PutItemCommand, GetItemCommand, UpdateItemCommand, DeleteItemCommand } from '@aws-sdk/client-dynamodb';
import { SNSClient } from '@aws-sdk/client-sns';
import tokenCommand from './modules/tokenCommands';

const client = new DynamoDBClient({ region: process.env.AWS_REGION });
const snsClient = new SNSClient({ region: process.env.AWS_REGION });

export const service = async (event: any, context: any, callback: any): Promise<any> => {
  const data = JSON.parse(event.body);

  const { fcmToken, userId } = data;

  let body;

  try {
    switch (event.headers.action) {
      case 'register':
        const getCommand = tokenCommand.getToken(fcmToken, userId);
        const createCommand = tokenCommand.createToken(fcmToken, userId);

        const data = await client.send(getCommand);

        if (!data.Item) {
          await client.send(createCommand);
        }

        body = 'register success';

        break;
      case 'cancel':
        const deleteCommand = tokenCommand.deleteToken(fcmToken, userId);
        await client.send(deleteCommand);

        body = 'cancel success';
        break;
      default:
        throw new Error(`Unsupported action: "${event.headers.action}"`);
    }
  } catch (e) {
    callback(e);
  } finally {
    callback(null, {
      statusCode: 200,
      body: body,
    });
  }
};
