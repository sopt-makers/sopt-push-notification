import { RequestSendAllPushMessageDTO, RequestSendPushMessageDTO } from '../types';

const toRequestSendPushMessageDto = (dto: unknown): dto is RequestSendPushMessageDTO => {
  if (typeof dto !== 'object' || dto === null) {
    return false;
  }

  const { transactionId, service, userIds, title, content, deepLink, webLink } = dto as RequestSendPushMessageDTO;

  if (typeof transactionId !== 'string') {
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

  const { transactionId, service, title, content, deepLink, webLink } = dto as RequestSendAllPushMessageDTO;

  if (typeof transactionId !== 'string') {
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

  if (deepLink && typeof deepLink !== 'string') {
    return false;
  }

  if (webLink && typeof webLink !== 'string') {
    return false;
  }

  return true;
};

export default { toRequestSendPushMessageDto, toRequestSendAllPushMessageDTO };
