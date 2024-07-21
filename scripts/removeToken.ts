import {
  DeleteEndpointCommand,
  Endpoint,
  ListEndpointsByPlatformApplicationCommand,
  ListSubscriptionsByTopicCommand,
  SNSClient,
  Subscription,
  UnsubscribeCommand,
} from '@aws-sdk/client-sns';
import { DeleteItemCommand, DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { writeFile } from 'fs';
import * as dotenv from 'dotenv';

const envFile = `.env.${process.env.NODE_ENV || 'dev'}`;
dotenv.config({ path: envFile });

const ALL_TOPIC_ARN = process.env.ALL_TOPIC_ARN;
const PLATFORM_APPLICATION_iOS = process.env.PLATFORM_APPLICATION_iOS;
const PLATFORM_APPLICATION_ANDROID = process.env.PLATFORM_APPLICATION_ANDROID;
const DDB_TABLE = process.env.DDB_TABLE;

const credentials = {
  accessKeyId: process.env.ACCESS_KEY_ID || '',
  secretAccessKey: process.env.SECRET_ACCESS || '',
};

type Platform = 'iOS' | 'android';

const getPrimaryKey = (userId = 'unknown', deviceToken: string) => {
  const userInfo = `u#${userId}`;
  const tokenInfo = `d#${deviceToken}`;

  return {
    userInfo,
    tokenInfo,
  };
};

const fetchSubscriptions = async (
  client: SNSClient,
  nextToken?: string,
  collectedSubscriptions: Subscription[] = [],
): Promise<Subscription[]> => {
  const command = new ListSubscriptionsByTopicCommand({
    TopicArn: ALL_TOPIC_ARN,
    NextToken: nextToken,
  });
  const response = await client.send(command);

  if (response.Subscriptions) {
    collectedSubscriptions.push(...response.Subscriptions);
  }

  if (response.NextToken) {
    return fetchSubscriptions(client, response.NextToken, collectedSubscriptions);
  }

  return collectedSubscriptions;
};

/**
 *
 * @param platform
 * @param client
 * @example
 *   {
 *     SubscriptionArn: 'arn:aws:sns:ap-northeast-2:379013966998:SOPT-NOTIFICATION-ALL-DEV:950e036d-ad02-4201-87d8-dc335d597fd4',
 *     Owner: '379013966998',
 *     Protocol: 'application',
 *     Endpoint: 'arn:aws:sns:ap-northeast-2:379013966998:endpoint/APNS/Makers-test-iOS/979d6eb0-d3da-36fb-9a91-da898f0ea85b',
 *     TopicArn: 'arn:aws:sns:ap-northeast-2:379013966998:SOPT-NOTIFICATION-ALL-DEV'
 *   }
 */
const getSubscriptions = async (platform: Platform, client: SNSClient): Promise<Subscription[]> => {
  try {
    const subscriptions = await fetchSubscriptions(client);

    const endpoints = subscriptions.filter((subscription) => subscription.Endpoint !== undefined);

    if (platform === 'iOS') {
      return endpoints.filter((subscriptions) => subscriptions.Endpoint?.includes('iOS'));
    }

    return endpoints.filter((subscriptions) => subscriptions.Endpoint?.includes('Android'));
  } catch (error) {
    console.error('Failed to get platform application ARNs:', error);
    throw new Error('Failed to get platform application ARNs');
  }
};

async function deleteEndpoint(client: SNSClient, endpoints: Endpoint[]) {
  const endpointArns = endpoints.map((endpoint) => endpoint.EndpointArn);
  const commands = endpointArns.map((endpointArn) => new DeleteEndpointCommand({ EndpointArn: endpointArn }));

  for (const command of commands) {
    try {
      await client.send(command);
      console.log('Successfully deleted endpoint:', command.input.EndpointArn);
    } catch (error) {
      console.error('Failed to delete endpoint:', error);
    }
  }
}

async function unSubscriptionAllTopics(client: SNSClient, subscriptions: Subscription[]) {
  const subscriptionArns = subscriptions.map((subscription) => subscription.SubscriptionArn);
  const commands = subscriptionArns.map(
    (subscriptionArn) => new UnsubscribeCommand({ SubscriptionArn: subscriptionArn }),
  );

  for (const command of commands) {
    try {
      await client.send(command);
      console.log('Successfully unsubscribed from all topics:', command.input.SubscriptionArn);
    } catch (error) {
      console.error('Failed to unsubscribe from all topics:', error);
    }
  }
}

/**
 * Endpoint interface
 * {
 *   EndpointArn: 'arn:aws:sns:ap-northeast-2:379013966998:endpoint/APNS/Makers-test-iOS/0440bccb-00ba-3134-9d31-8457845311d3',
 *   Attributes: {
 *     Enabled: 'false',
 *     Token: '28b51ea0d3188ffd0d86e000b983822c7bd8da586ddf258985e490462a78df4b',
 *     CustomUserData: '173'
 *   }
 * }
 */
async function getEndpoints(
  snsClient: SNSClient,
  platform: Platform,
  nextToken?: string,
  collectedEndpoints: Endpoint[] = [],
): Promise<Endpoint[]> {
  const command = new ListEndpointsByPlatformApplicationCommand({
    PlatformApplicationArn: platform === 'iOS' ? PLATFORM_APPLICATION_iOS : PLATFORM_APPLICATION_ANDROID,
    NextToken: nextToken,
  });

  const response = await snsClient.send(command);
  if (response.Endpoints) {
    collectedEndpoints.push(...response.Endpoints);
  }
  if (response.NextToken) {
    return getEndpoints(snsClient, platform, response.NextToken, collectedEndpoints);
  }

  return collectedEndpoints;
}

function saveJsonToFile(jsonData: any, filePath: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const dataString = JSON.stringify(jsonData, null, 2); // Convert JSON to string with pretty print
    writeFile(filePath, dataString, 'utf8', (err: any) => {
      if (err) {
        reject(err);
      } else {
        resolve();
      }
    });
  });
}

async function deleteDeviceTokenFromDDB(ddbClient: DynamoDBClient, endpoints: Endpoint[]) {
  const primaryKeys = endpoints
    .map((endpoint) => getPrimaryKey(endpoint.Attributes?.CustomUserData, endpoint.Attributes?.Token || ''))
    .filter((primaryKey) => primaryKey.tokenInfo !== '');

  const userEntityCommands = primaryKeys.map(
    ({ userInfo, tokenInfo }) =>
      new DeleteItemCommand({
        TableName: DDB_TABLE,
        Key: {
          pk: { S: userInfo },
          sk: { S: tokenInfo },
        },
        ReturnValues: 'ALL_OLD',
      }),
  );

  const tokenEntityCommands = primaryKeys.map(
    ({ userInfo, tokenInfo }) =>
      new DeleteItemCommand({
        TableName: DDB_TABLE,
        Key: {
          pk: { S: tokenInfo },
          sk: { S: userInfo },
        },
        ReturnValues: 'ALL_OLD',
      }),
  );

  for (const userEntityCommand of userEntityCommands) {
    try {
      await ddbClient.send(userEntityCommand);
      console.log('Successfully deleted token entity:', userEntityCommand.input.Key);
    } catch (e) {
      console.error(`failed to delete token entity: ${userEntityCommand.input.Key}`, e);
    }
  }

  for (const tokenEntityCommand of tokenEntityCommands) {
    try {
      await ddbClient.send(tokenEntityCommand);
      console.log('Successfully deleted token entity:', tokenEntityCommand.input.Key);
    } catch (e) {
      console.error(`failed to delete token entity: ${tokenEntityCommand.input.Key}`, e);
    }
  }
}

const removeToken = async (snsClient: SNSClient, ddbClient: DynamoDBClient, platform: Platform) => {
  //SoptAllTopic으로부터 전체 구독 정보 가져오기
  const subscriptions = await getSubscriptions(platform, snsClient);
  await saveJsonToFile(subscriptions, 'subscriptions.json');

  //SoptAllTopic으로부터 구독 해지
  await unSubscriptionAllTopics(snsClient, subscriptions);

  const endpoints = await getEndpoints(snsClient, platform);
  await saveJsonToFile(endpoints, 'endpoints.json');

  //PlatFormApplication에 등록된 모든 Endpoint 삭제 및 DB DeviceToken 삭제
  await deleteEndpoint(snsClient, endpoints);
  await deleteDeviceTokenFromDDB(ddbClient, endpoints);
};

void (async () => {
  const snsClient = new SNSClient({
    region: 'ap-northeast-2',
    credentials: credentials,
  });
  const ddbClient = new DynamoDBClient({
    region: 'ap-northeast-2',
    credentials: credentials,
  });

  //플랫폼별 디바이스 토큰을 모두 삭제한다
  await removeToken(snsClient, ddbClient, 'iOS');
})();
