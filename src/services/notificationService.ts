import { Platform, PushTopic } from '../types';
import messageFactory from '../modules/messageFactory';
import snsFactory from '../modules/snsFactory';
import { ResponsePushNotification } from '../types/notifications';

type pushDTO = {
  endpointArn: string;
  title: string;
  content: string;
  deepLink?: string;
  webLink?: string;
};

async function push(endpointArn: string, message: string): Promise<ResponsePushNotification | null> {
  const result = await snsFactory.publish(endpointArn, message);

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
const pushArn = async (dto: pushDTO) => {
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

const pushFcm = async (dto: pushDTO): Promise<ResponsePushNotification | null> => {
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
  messagePayload: { title: string; content: string; deepLink?: string; webLink?: string };
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

export default { platformPush };
