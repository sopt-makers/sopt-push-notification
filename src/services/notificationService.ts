import { Category, Platform, PushTopic } from '../types';
import messageFactory from '../modules/messageFactory';
import snsFactory from '../modules/snsFactory';
import { ResponsePushNotification } from '../types/notifications';
import { PublishCommandOutput } from '@aws-sdk/client-sns';

interface PushMessageDTO {
  title: string;
  content: string;
  category: Category;
  /* 메세지 고유 식별 아이디 입니다. SNS에서 발행하는 Message Id와는 무관함 */
  id: string;
  deepLink?: string;
  webLink?: string;
}

interface PushDTO extends PushMessageDTO {
  endpointArn: string;
}

async function push(arn: string, message: string, sendAll = false): Promise<ResponsePushNotification | null> {
  let result: PublishCommandOutput | null;
  if (sendAll) {
    result = await snsFactory.publishToTopicArn(arn, message);
  } else {
    result = await snsFactory.publishToEndpoint(arn, message);
  }
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
    category: dto.category,
    deepLink: dto.deepLink,
    webLink: dto.webLink,
  });
  const endpointArn = dto.endpointArn;
  return await push(endpointArn, message);
};

const pushFcm = async (dto: PushDTO): Promise<ResponsePushNotification | null> => {
  const message = messageFactory.createNewMessage({
    topic: PushTopic.Fcm,
    title: dto.title,
    content: dto.content,
    category: dto.category,
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
      category: dto.messagePayload.category,
      deepLink: dto.messagePayload.deepLink,
      webLink: dto.messagePayload.webLink,
      id: dto.messagePayload.id,
    });
  }
  if (dto.endpointPayload.platform === Platform.Android) {
    return await pushFcm({
      endpointArn: dto.endpointPayload.endpointArn,
      title: dto.messagePayload.title,
      content: dto.messagePayload.content,
      category: dto.messagePayload.category,
      deepLink: dto.messagePayload.deepLink,
      webLink: dto.messagePayload.webLink,
      id: dto.messagePayload.id,
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
    category: dto.category,
    deepLink: dto.deepLink,
    webLink: dto.webLink,
  });
  const topicArn = process.env.ALL_TOPIC_ARN;
  if (topicArn === undefined) {
    throw new Error('ALL_TOPIC_ARN is not defined');
  }
  return await push(topicArn, message, true);
};

const allTopicPush = async (dto: { messagePayload: PushMessageDTO }): Promise<ResponsePushNotification | null> => {
  return await pushAll(dto.messagePayload);
};

export default { platformPush, allTopicPush };
