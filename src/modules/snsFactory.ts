import {
  SNSClient,
  SubscribeCommand,
  CreatePlatformEndpointCommand,
  UnsubscribeCommand,
  DeleteEndpointCommand,
} from '@aws-sdk/client-sns';

const snsClient = new SNSClient({ region: process.env.AWS_REGION });

const subscribe = async (arn: string) => {
  const command = new SubscribeCommand({
    TopicArn: process.env.ALL_TOPIC_ARN,
    Protocol: 'application',
    Endpoint: arn,
  });

  const topicSubscribeData = await snsClient.send(command);

  return topicSubscribeData;
};

const unSubscribe = (arn: string) => {
  const command = new UnsubscribeCommand({
    SubscriptionArn: arn,
  });

  snsClient.send(command);
};

const registerEndPoint = async (fcmToken: string, platform: string) => {
  const platformApplicationArn =
    platform == 'iOS' ? process.env.PLATFORM_APPLICATION_iOS : process.env.PLATFORM_APPLICATION_ANDROID;

  const command = new CreatePlatformEndpointCommand({
    PlatformApplicationArn: platformApplicationArn,
    Token: fcmToken,
  });

  const endPointData = await snsClient.send(command);

  return endPointData;
};

const cancelEndPoint = (arn: string) => {
  const command = new DeleteEndpointCommand({
    EndpointArn: arn,
  });

  snsClient.send(command);
};

const snsFactory = {
  subscribe,
  unSubscribe,
  registerEndPoint,
  cancelEndPoint,
};

export default snsFactory;
