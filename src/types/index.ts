enum Services {
  CREW = 'crew',
  OFFICIAL = 'official',
  OPERATION = 'operation',
  PLAYGROUND = 'playground',
}

enum Actions {
  REGISTER = 'register',
  CANCEL = 'cancel',
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

interface RequestDTO {
  fcmToken: string;
  userId: string;
}

type Platform = 'iOS' | 'Android';

export { Services, Actions, NotificationType, NotificationStatus, RequestDTO, Platform };
