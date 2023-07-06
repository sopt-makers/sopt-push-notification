import { MessageFactoryDTO } from '../types';

type APNsMessage = {
  aps: {
    alert: {
      title: string;
      body: string;
    };
  };
  webLink?: string;
  deepLink?: string;
};

type ResponseApnsMessage =
  | {
      default: string;
      APNS: string;
    }
  | {
      default: string;
      APNS_SANDBOX: string;
    };

type ResponseFCMMessage = {
  default: string;
  GCM: string;
};

type FCMMessage = {
  notification: {
    title: string;
    content: string;
  };
  data?: {
    webLink?: string;
    deepLink?: string;
  };
};

type AllTopicMessage = {
  default: string;
  APNS: ResponseApnsMessage;
  APNS_SANDBOX: ResponseApnsMessage;
  GCM: ResponseFCMMessage;
};
const DEFAULT =
  'This is the default message which must be present when publishing a message to a topic. The default message will only be used if a message is not present for one of the notification platforms.';

const apnsMessage = (dto: MessageFactoryDTO): ResponseApnsMessage => {
  const { title, content, webLink, deepLink } = dto;
  const message: APNsMessage = {
    aps: {
      alert: {
        title,
        body: content,
      },
    },
  };

  if (deepLink !== undefined) {
    message.deepLink = deepLink;
  }
  if (webLink !== undefined) {
    message.webLink = webLink;
  }
  if (process.env.STAGE === 'dev') {
    return { default: DEFAULT, APNS_SANDBOX: JSON.stringify(message) };
  }
  return { default: DEFAULT, APNS: JSON.stringify(message) };
};

const fcmMessage = (dto: MessageFactoryDTO): ResponseFCMMessage => {
  const { title, content, webLink, deepLink } = dto;
  const message: FCMMessage = {
    notification: {
      title,
      content,
    },
  };

  if (deepLink !== undefined) {
    message.data = { deepLink };
  }

  if (webLink !== undefined) {
    message.data = { ...(message.data || {}), webLink };
  }
  return { default: DEFAULT, GCM: JSON.stringify(message) };
};

//todo dev prod 나눠서 보내기
const allMessage = (dto: MessageFactoryDTO): AllTopicMessage => {
  return {
    default: DEFAULT,
    APNS: apnsMessage(dto),
    APNS_SANDBOX: apnsMessage(dto),
    GCM: fcmMessage(dto),
  };
};

const createNewMessage = (dto: MessageFactoryDTO): string => {
  if (dto.topic === 'apns') {
    return JSON.stringify(apnsMessage(dto));
  }
  if (dto.topic === 'fcm') {
    return JSON.stringify(fcmMessage(dto));
  }
  if (dto.topic === 'all') {
    return JSON.stringify(allMessage(dto));
  }
  throw new Error('Invalid topic');
};

export default { createNewMessage };
