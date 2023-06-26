import { Platform } from './index';

interface UserTokenEntity {
  PK: string;
  SK: string;
  entity: 'user';
  platform: Platform;
  endpointArn: string;
  createdAt: string;
  subscriptionArn: string;
}

interface DeviceTokenEntity {
  PK: string;
  SK: string;
  entity: 'deviceToken';
  platform: Platform;
  endpointArn: string;
  createdAt: string;
  subscriptionArn: string;
}
