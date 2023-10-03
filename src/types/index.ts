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

enum Category {
  NOTICE = 'NOTICE',
  NEWS = 'NEWS',
  NONE = 'NONE',
}

enum WebHookType {
  SEND = 'SEND',
  SEND_ALL = 'SEND_ALL',
  FAIL = 'FAIL',
}

interface RequestHeaderDTO {
  transactionId: string;
  service: Services;
  platform: Platform;
  action: Actions;
}

interface RequestRegisterUserDTO extends Omit<RequestHeaderDTO, 'action'> {
  deviceToken: string;
  userIds?: string[];
}

interface RequestDeleteTokenDTO extends Omit<RequestHeaderDTO, 'action'> {
  deviceToken: string;
  userIds?: string[];
}

interface RequestSendPushMessageDTO extends Omit<RequestHeaderDTO, 'platform' | 'action'> {
  userIds: string[];
  title: string;
  content: string;
  category: Category;
  deepLink?: string;
  webLink?: string;
}

type RequestSendAllPushMessageDTO = Omit<RequestSendPushMessageDTO, 'userIds'>;

interface PushSuccessMessageDTO {
  userIds?: string[];
  title: string;
  content: string;
  category: Category;
  deepLink?: string;
  webLink?: string;
  service: Services;
  type: WebHookType;
}

interface MessageFactoryDTO {
  topic: PushTopic;
  title: string;
  content: string;
  category: Category;
  deepLink?: string;
  webLink?: string;
}

export {
  Services,
  Actions,
  NotificationType,
  NotificationStatus,
  RequestHeaderDTO,
  Platform,
  Entity,
  RequestSendPushMessageDTO,
  PushSuccessMessageDTO,
  PushTopic,
  Category,
  WebHookType,
  MessageFactoryDTO,
  RequestSendAllPushMessageDTO,
  RequestRegisterUserDTO,
  RequestDeleteTokenDTO,
};
