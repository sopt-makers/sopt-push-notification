import {
  SNSClient,
  SubscribeCommand,
  CreatePlatformEndpointCommand,
  UnsubscribeCommand,
  DeleteEndpointCommand,
  PublishCommand,
  PublishCommandOutput,
  CreatePlatformEndpointCommandOutput,
  CreatePlatformApplicationCommandInput,
  CreatePlatformEndpointInput,
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

const unSubscribe = async (arn: string): Promise<void> => {
  const command = new UnsubscribeCommand({
    SubscriptionArn: arn,
  });

  const result = await snsClient.send(command);
  if (result.$metadata.httpStatusCode !== 200) {
    console.error('SNS unsubscribe error', result.$metadata);
  }
};

const registerEndPoint = async (
  fcmToken: string,
  platform: string,
  userId: string | undefined,
): Promise<CreatePlatformEndpointCommandOutput> => {
  const platformApplicationArn =
    platform == 'iOS'
      ? (process.env.PLATFORM_APPLICATION_iOS as string)
      : (process.env.PLATFORM_APPLICATION_ANDROID as string);

  const input: CreatePlatformEndpointInput = {
    PlatformApplicationArn: platformApplicationArn,
    Token: fcmToken,
  };

  if (userId) {
    input.CustomUserData = userId;
  }

  const command = new CreatePlatformEndpointCommand(input);

  const endPointData = await snsClient.send(command);

  return endPointData;
};

const cancelEndPoint = async (arn: string): Promise<void> => {
  const command = new DeleteEndpointCommand({
    EndpointArn: arn,
  });

  const result = await snsClient.send(command);
  if (result.$metadata.httpStatusCode !== 200) {
    console.error('SNS delete endpoint error', result.$metadata);
  }
};

const publish = async (arn: string, message: string): Promise<PublishCommandOutput | null> => {
  const command = new PublishCommand({
    TopicArn: arn,
    Message: message,
    MessageStructure: 'json',
  });
  const result = await snsClient.send(command);
  if (result.$metadata.httpStatusCode !== 200) {
    console.error('SNS publish error', result.$metadata);
    return null;
  }
  return result;
};

const snsFactory = {
  subscribe,
  unSubscribe,
  registerEndPoint,
  cancelEndPoint,
  publish,
};

export default snsFactory;
