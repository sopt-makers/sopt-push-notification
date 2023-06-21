import { SubscribeCommand } from '@aws-sdk/client-sns';

const subscribe = (fcmToken: string) => {
  const command = new SubscribeCommand({
    TopicArn: process.env.ALL_TOPIC_ARN,
    Protocol: 'application',
    Endpoint: fcmToken,
  });

  return command;
};

const snsCommands = {
  subscribe,
};

export default snsCommands;
