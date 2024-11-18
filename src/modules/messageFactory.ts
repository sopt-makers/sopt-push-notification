import { Category, MessageFactoryDTO } from '../types';
import dayjs from 'dayjs';

type APNsMessage = {
  aps: {
    alert: {
      title: string;
      body: string;
    };
  };
  category: Category;
  id: string;
  webLink?: string;
  deepLink?: string;
  sendAt: string;
};

type ResponseApnsMessage = {
  default: string;
  APNS: string;
};

type ResponseFCMMessage = {
  default: string;
  GCM: string;
};

type FCMMessage = {
  data: {
    title: string;
    content: string;
    category: Category;
    id: string;
    webLink?: string;
    deepLink?: string;
    sendAt: string;
  };
};

type AllTopicMessage = {
  default: string;
  APNS: ResponseApnsMessage;
  GCM: ResponseFCMMessage;
};
const DEFAULT =
  'This is the default message which must be present when publishing a message to a topic. The default message will only be used if a message is not present for one of the notification platforms.';

const apnsMessage = (dto: MessageFactoryDTO): ResponseApnsMessage => {
  const { title, content, category, webLink, deepLink, id } = dto;
  const now = dayjs();
  const sendAt = now.format("YYYY-MM-DD HH:mm:ss");
  const message: APNsMessage = {
    aps: {
      alert: {
        title,
        body: content,
      },

    },
    category,
    id,
    sendAt,
  };

  if (deepLink !== undefined) {
    message.deepLink = deepLink;
  }
  if (webLink !== undefined) {
    message.webLink = webLink;
  }

  return { default: DEFAULT, APNS: JSON.stringify(message) };
};

const fcmMessage = (dto: MessageFactoryDTO): ResponseFCMMessage => {
  const { title, content, category, webLink, deepLink, id } = dto;
  const now = dayjs();
  const sendAt = now.format("YYYY-MM-DD HH:mm:ss");
  const message: FCMMessage = {
    data: {
      id,
      title,
      content,
      category,
      sendAt,
    },
  };

  if (deepLink !== undefined) {
    message.data.deepLink = deepLink;
  }

  if (webLink !== undefined) {
    message.data.webLink = webLink;
  }
  return { default: DEFAULT, GCM: JSON.stringify(message) };
};

const allMessage = (dto: MessageFactoryDTO): AllTopicMessage => {
  return {
    default: DEFAULT,
    APNS: apnsMessage(dto),
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
