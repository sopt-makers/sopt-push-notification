import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { SNSClient } from '@aws-sdk/client-sns';

const client = new DynamoDBClient({ region: process.env.AWS_REGION });
const snsClient = new SNSClient({ region: process.env.AWS_REGION });

module.exports.service = async (event: any, context: any, callback: any) => {
  callback(null, {
    statusCode: 200,
    body: 'hello push server!',
  });
};
