enum Services {
  CREW = 'crew',
  OFFICIAL = 'official',
  OPERATION = 'operation',
  PLAYGROUND = 'playground',
}

enum Actions {
  REGISTER = 'register',
  CANCEL = 'cancel',
  SEND = 'send',
}

enum Entity {
  HISTORY = 'history',
  DEVICE_TOKEN = 'deviceToken',
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
}

interface RequestHeaderDTO {
  transactionId: string;
  service: Services;
  platform: Platform;
  action: Actions;
}

interface RequestBodyDTO {
  fcmToken: string;
  userIds: string[];
}

export { Services, Actions, NotificationType, NotificationStatus, RequestBodyDTO, RequestHeaderDTO, Platform, Entity };
