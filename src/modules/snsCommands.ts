import { SubscribeCommand } from '@aws-sdk/client-sns';

const subscribe = (arn: string) => {
  const command = new SubscribeCommand({
    TopicArn: process.env.ALL_TOPIC_ARN,
    Protocol: 'application',
    Endpoint: arn,
  });

  return command;
};

const snsCommands = {
  subscribe,
};

export default snsCommands;
