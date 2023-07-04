import {
  SNSClient,
  SubscribeCommand,
  CreatePlatformEndpointCommand,
  UnsubscribeCommand,
  DeleteEndpointCommand,
  PublishCommand,
  PublishCommandOutput,
  CreatePlatformEndpointCommandOutput,
  CreatePlatformEndpointInput,
  SubscribeCommandOutput,
} from '@aws-sdk/client-sns';

const snsClient = new SNSClient({ region: process.env.AWS_REGION });

const subscribe = async (arn: string): Promise<SubscribeCommandOutput> => {
  const command = new SubscribeCommand({
    TopicArn: process.env.ALL_TOPIC_ARN,
    Protocol: 'application',
    Endpoint: arn,
  });

  return await snsClient.send(command);
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
  deviceToken: string,
  platform: string,
  userId: string | undefined,
): Promise<CreatePlatformEndpointCommandOutput> => {
  const platformApplicationArn =
    platform == 'iOS'
      ? (process.env.PLATFORM_APPLICATION_iOS as string)
      : (process.env.PLATFORM_APPLICATION_ANDROID as string);

  const input: CreatePlatformEndpointInput = {
    PlatformApplicationArn: platformApplicationArn,
    Token: deviceToken,
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

const publishToTopicArn = async (arn: string, message: string): Promise<PublishCommandOutput | null> => {
  try {
    const command = new PublishCommand({
      TopicArn: arn,
      Message: message,
      MessageStructure: 'json',
    });
    const result = await snsClient.send(command);
    if (result.$metadata.httpStatusCode !== 200) {
      console.error('SNS publish TopicArn error', result);
      return null;
    }
    return result;
  } catch (error) {
    console.error('SNS publish TopicArn error', error);
    return null;
  }
};

const publishToEndpoint = async (arn: string, message: string): Promise<PublishCommandOutput | null> => {
  try {
    const command = new PublishCommand({
      TargetArn: arn,
      Message: message,
      MessageStructure: 'json',
    });
    const result = await snsClient.send(command);
    if (result.$metadata.httpStatusCode !== 200) {
      console.error('SNS endpoint publish error', result);
      return null;
    }
    return result;
  } catch (error) {
    console.error('SNS endpoint publish error', error);
    return null;
  }
};

const snsFactory = {
  subscribe,
  unSubscribe,
  registerEndPoint,
  cancelEndPoint,
  publishToTopicArn,
  publishToEndpoint,
};

export default snsFactory;
