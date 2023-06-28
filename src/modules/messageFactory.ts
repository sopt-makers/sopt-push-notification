import { isNil } from 'lodash';
import { MessageFactoryDTO } from '../types';

type APNsMessage = {
  aps: {
    alert: {
      title: string;
      content: string;
    };
  };
  webLink?: string;
  deepLink?: string;
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
  APNS: string;
  APNS_SANDBOX: string;
  GCM: string;
};

const apnsMessage = (dto: MessageFactoryDTO): string => {
  const { title, content, webLink, deepLink } = dto;
  const message: APNsMessage = {
    aps: {
      alert: {
        title,
        content,
      },
    },
  };

  if (!isNil(deepLink)) {
    message.deepLink = deepLink;
  }
  if (!isNil(webLink)) {
    message.webLink = webLink;
  }
  return JSON.stringify(message);
};

const fcmMessage = (dto: MessageFactoryDTO): string => {
  const { title, content, webLink, deepLink } = dto;
  const message: FCMMessage = {
    notification: {
      title,
      content,
    },
  };

  if (!isNil(deepLink)) {
    message.data = { deepLink };
  }

  if (!isNil(webLink)) {
    message.data = { ...(message.data || {}), webLink };
  }
  return JSON.stringify(message);
};

//todo dev prod 나눠서 보내기
const allMessage = (dto: MessageFactoryDTO): string => {
  const message: AllTopicMessage = {
    default: 'hello',
    APNS: JSON.stringify(apnsMessage(dto)),
    APNS_SANDBOX: JSON.stringify(apnsMessage(dto)),
    GCM: JSON.stringify(fcmMessage(dto)),
  };
  return JSON.stringify(message);
};

const createNewMessage = (dto: MessageFactoryDTO): string => {
  if (dto.topic === 'apns') {
    return apnsMessage(dto);
  }
  if (dto.topic === 'fcm') {
    return fcmMessage(dto);
  }
  if (dto.topic === 'all') {
    return allMessage(dto);
  }
  throw new Error('Invalid topic');
};

export default { createNewMessage };