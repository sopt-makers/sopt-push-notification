enum Services {
  CREW = 'crew',
  OFFICIAL = 'official',
  OPERATION = 'operation',
  PLAYGROUND = 'playground',
  APP = 'app',
}

enum Actions {
  REGISTER = 'register',
  CANCEL = 'cancel',
  SEND = 'send',
  SEND_ALL = 'sendAll',
}

enum Entity {
  HISTORY = 'history',
  DEVICE_TOKEN = 'deviceToken',
  USER = 'user',
}

enum NotificationType {
  EMAIL = 'email',
  PUSH = 'push',
  SMS = 'sms',
}

enum NotificationStatus {
  START = 'start',
  FAIL = 'fail',
  SUCCESS = 'success',
}

enum Platform {
  iOS = 'iOS',
  Android = 'Android',
  None = '',
}

enum PushTopic {
  Apns = 'apns',
  Fcm = 'fcm',
  All = 'all',
}

interface RequestHeaderDTO {
  transactionId: string;
  service: Services;
  platform: Platform;
  action: Actions;
}

interface RequestBodyDTO {
  fcmToken: string;
  userIds?: string[];
}

interface RequestSendPushMessageDTO extends Omit<RequestHeaderDTO, 'platform' | 'action'> {
  userIds: string[];
  title: string;
  content: string;
  deepLink?: string;
  webLink?: string;
}

interface MessageFactoryDTO {
  topic: PushTopic;
  title: string;
  content: string;
  deepLink?: string;
  webLink?: string;
}

export {
  Services,
  Actions,
  NotificationType,
  NotificationStatus,
  RequestBodyDTO,
  RequestHeaderDTO,
  Platform,
  Entity,
  RequestSendPushMessageDTO,
  PushTopic,
  MessageFactoryDTO,
};
