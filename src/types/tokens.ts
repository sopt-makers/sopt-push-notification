import { Platform } from './index';

interface UserTokenEntity {
  pk: string;
  sk: string;
  entity: 'user';
  platform: Platform;
  endpointArn: string;
  createdAt: string; // toISOString
  subscriptionArn: string;
}

interface DeviceTokenEntity {
  pk: string;
  sk: string;
  entity: 'deviceToken';
  platform: Platform;
  endpointArn: string;
  createdAt: string; // toISOString
  subscriptionArn: string;
}

export { DeviceTokenEntity, UserTokenEntity };
