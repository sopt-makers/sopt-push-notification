import {
  SubscribeCommand,
  CreatePlatformEndpointCommand,
  UnsubscribeCommand,
  DeleteEndpointCommand,
} from '@aws-sdk/client-sns';

const subscribe = (arn: string) => {
  const command = new SubscribeCommand({
    TopicArn: process.env.ALL_TOPIC_ARN,
    Protocol: 'application',
    Endpoint: arn,
  });

  return command;
};

const unSubscribe = (arn: string) => {
  const command = new UnsubscribeCommand({
    SubscriptionArn: arn,
  });

  return command;
};

const registerEndPoint = (fcmToken: string, platform: string) => {
  const platformApplicationArn =
    platform == 'iOS' ? process.env.PLATFORM_APPLICATION_iOS : process.env.PLATFORM_APPLICATION_ANDROID;

  const command = new CreatePlatformEndpointCommand({
    PlatformApplicationArn: platformApplicationArn,
    Token: fcmToken,
  });

  return command;
};

const cancelEndPoint = (arn: string) => {
  const command = new DeleteEndpointCommand({
    EndpointArn: arn,
  });

  return command;
};

const snsCommands = {
  subscribe,
  unSubscribe,
  registerEndPoint,
  cancelEndPoint,
};

export default snsCommands;
