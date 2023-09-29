import {
  Category,
  Platform,
  RequestDeleteTokenDTO,
  RequestRegisterUserDTO,
  RequestSendAllPushMessageDTO,
  RequestSendPushMessageDTO,
  Services,
} from '../types';

const isEnum = <T extends Record<string, any>>(value: any, enumType: T): value is T => {
  return Object.values(enumType).includes(value);
};

const toRequestRegisterUserDto = (dto: unknown): dto is RequestRegisterUserDTO => {
  if (typeof dto !== 'object' || dto === null) {
    return false;
  }

  const { transactionId, service, platform, userIds, deviceToken } = dto as RequestRegisterUserDTO;

  if (isEnum(service, Services) == false) {
    return false;
  }

  if (isEnum(platform, Platform) == false) {
    return false;
  }

  if (!transactionId || typeof transactionId !== 'string') {
    return false;
  }

  if (userIds && !Array.isArray(userIds)) {
    return false;
  }

  if (typeof deviceToken !== 'string') {
    return false;
  }

  return true;
};

const toRequestDeleteTokenDto = (dto: unknown): dto is RequestDeleteTokenDTO => {
  if (typeof dto !== 'object' || dto === null) {
    return false;
  }

  const { transactionId, service, platform, userIds, deviceToken } = dto as RequestDeleteTokenDTO;

  if (isEnum(service, Services) == false) {
    return false;
  }

  if (isEnum(platform, Platform) == false) {
    return false;
  }

  if (!transactionId || typeof transactionId !== 'string') {
    return false;
  }

  if (userIds && !Array.isArray(userIds)) {
    return false;
  }

  if (typeof deviceToken !== 'string') {
    return false;
  }

  return true;
};

const toRequestSendPushMessageDto = (dto: unknown): dto is RequestSendPushMessageDTO => {
  if (typeof dto !== 'object' || dto === null) {
    return false;
  }

  const { transactionId, service, userIds, title, content, category, deepLink, webLink } =
    dto as RequestSendPushMessageDTO;

  if (isEnum(service, Services) == false) {
    return false;
  }

  if (isEnum(category, Category) == false) {
    return false;
  }

  if (!transactionId || typeof transactionId !== 'string') {
    return false;
  }

  if (typeof service !== 'string') {
    return false;
  }

  if (!Array.isArray(userIds)) {
    return false;
  }

  if (typeof title !== 'string') {
    return false;
  }

  if (typeof content !== 'string') {
    return false;
  }

  if (typeof category !== 'string') {
    return false;
  }

  if (deepLink && typeof deepLink !== 'string') {
    return false;
  }

  if (webLink && typeof webLink !== 'string') {
    return false;
  }

  return true;
};

const toRequestSendAllPushMessageDTO = (dto: unknown): dto is RequestSendAllPushMessageDTO => {
  if (typeof dto !== 'object' || dto === null) {
    return false;
  }

  const { transactionId, service, title, content, category, deepLink, webLink } = dto as RequestSendAllPushMessageDTO;

  if (isEnum(service, Services) == false) {
    return false;
  }

  if (isEnum(category, Category) == false) {
    return false;
  }

  if (!transactionId || typeof transactionId !== 'string') {
    return false;
  }

  if (typeof service !== 'string') {
    return false;
  }

  if (typeof title !== 'string') {
    return false;
  }

  if (typeof content !== 'string') {
    return false;
  }

  if (typeof category !== 'string') {
    return false;
  }

  if (deepLink && typeof deepLink !== 'string') {
    return false;
  }

  if (webLink && typeof webLink !== 'string') {
    return false;
  }

  return true;
};

export default {
  toRequestRegisterUserDto,
  toRequestDeleteTokenDto,
  toRequestSendPushMessageDto,
  toRequestSendAllPushMessageDTO,
};
