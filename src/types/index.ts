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

enum UserStatus {
  UNREGISTERED = '0',
}

interface RequestDTO {
  fcmToken: string;
  userId: string;
}

export { Services, Actions, NotificationType, NotificationStatus, UserStatus };
export { RequestDTO };
