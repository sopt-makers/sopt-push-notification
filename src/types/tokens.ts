import { Platform } from './index';

interface UserTokenEntity {
  deviceToken: string;
  userId: string;
  entity: 'user';
  platform: Platform;
  endpointArn: string;
  createdAt: string; // toISOString
  subscriptionArn: string;
}

interface DeviceTokenEntity {
  deviceToken: string;
  userId: string;
  entity: 'deviceToken';
  platform: Platform;
  endpointArn: string;
  createdAt: string; // toISOString
  subscriptionArn: string;
}

export { DeviceTokenEntity, UserTokenEntity };
