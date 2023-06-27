import { Platform, PushTopic } from '../types';
import messageFactory from '../modules/messageFactory';
import snsFactory from '../modules/snsFactory';
import { ResponsePushNotification } from '../types/notifications';

interface PushMessageDTO {
  title: string;
  content: string;
  deepLink?: string;
  webLink?: string;
}

interface PushDTO extends PushMessageDTO {
  endpointArn: string;
}

async function push(topicArn: string, message: string): Promise<ResponsePushNotification | null> {
  const result = await snsFactory.publish(topicArn, message);

  if (result === null) {
    return null;
  }

  if (result.MessageId === undefined) {
    return null;
  }

  return {
    messageId: result.MessageId,
  };
}

const pushArn = async (dto: PushDTO) => {
  const message = messageFactory.createNewMessage({
    topic: PushTopic.Apns,
    title: dto.title,
    content: dto.content,
    deepLink: dto.deepLink,
    webLink: dto.webLink,
  });
  const endpointArn = dto.endpointArn;
  return await push(endpointArn, message);
};

const pushFcm = async (dto: PushDTO): Promise<ResponsePushNotification | null> => {
  const message = messageFactory.createNewMessage({
    topic: PushTopic.Apns,
    title: dto.title,
    content: dto.content,
    deepLink: dto.deepLink,
    webLink: dto.webLink,
  });
  const endpointArn = dto.endpointArn;
  return await push(endpointArn, message);
};

const platformPush = async (dto: {
  messagePayload: PushMessageDTO;
  endpointPayload: { endpointArn: string; platform: Platform };
}): Promise<ResponsePushNotification | null> => {
  if (dto.endpointPayload.platform === Platform.iOS) {
    return await pushArn({
      endpointArn: dto.endpointPayload.endpointArn,
      title: dto.messagePayload.title,
      content: dto.messagePayload.content,
      deepLink: dto.messagePayload.deepLink,
      webLink: dto.messagePayload.webLink,
    });
  }
  if (dto.endpointPayload.platform === Platform.Android) {
    return await pushFcm({
      endpointArn: dto.endpointPayload.endpointArn,
      title: dto.messagePayload.title,
      content: dto.messagePayload.content,
      deepLink: dto.messagePayload.deepLink,
      webLink: dto.messagePayload.webLink,
    });
  }
  console.error('platformPush error: platform is not defined', JSON.stringify(dto));
  return null;
};

const pushAll = async (dto: PushMessageDTO): Promise<ResponsePushNotification | null> => {
  const message = messageFactory.createNewMessage({
    topic: PushTopic.All,
    title: dto.title,
    content: dto.content,
    deepLink: dto.deepLink,
    webLink: dto.webLink,
  });
  const topicArn = process.env.ALL_TOPIC_ARN;
  if (topicArn === undefined) {
    throw new Error('ALL_TOPIC_ARN is not defined');
  }
  return await push(topicArn, message);
};

const allTopicPush = async (dto: { messagePayload: PushMessageDTO }): Promise<ResponsePushNotification | null> => {
  return await pushAll(dto.messagePayload);
};

export default { platformPush, allTopicPush };
