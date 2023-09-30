import { PushSuccessMessageDTO, Services, Category, Actions } from '../types';
import axios from 'axios';

interface AppSuccessWebHookDTO {
  userIds: string[];
  title: string;
  content: string;
  category: Category;
  deepLink?: string;
  webLink?: string;
  messageIds: string[];
  action: Actions;
}

interface OperationSuccessWebHookDTO {
  userIds: string[];
  title: string;
  content: string;
  category: Category;
  deepLink?: string;
  webLink?: string;
  messageIds: string[];
}

async function appWebHook(appSuccessWebHookDTO: AppSuccessWebHookDTO): Promise<void> {
  try {
    if (process.env.MAKERS_APP_SERVER_URL === undefined) {
      throw new Error('env not defined');
    }

    await axios.post(process.env.MAKERS_APP_SERVER_URL, {
      appSuccessWebHookDTO,
    });
  } catch (e) {
    throw new Error('APP SERVER webhook failed');
  }
}

async function operationWebHook(operationSuccessWebHookDTO: OperationSuccessWebHookDTO): Promise<void> {
  try {
    if (process.env.MAKERS_OPERATION_SERVER_URL === undefined) {
      throw new Error('env not defined');
    }

    await axios.post(process.env.MAKERS_OPERATION_SERVER_URL, {
      operationSuccessWebHookDTO,
    });
  } catch (e) {
    throw new Error('OPERATION SERVER webhook failed');
  }
}

const pushSuccessWebHook = async (dto: PushSuccessMessageDTO): Promise<void> => {
  const { userIds, title, content, category, deepLink, webLink, messageIds, service, action } = dto;

  switch (service) {
    case Services.APP: {
      const appSuccessWebHookDTO: AppSuccessWebHookDTO = {
        userIds: userIds,
        title: title,
        content: content,
        category: category,
        deepLink: deepLink,
        webLink: webLink,
        messageIds: messageIds,
        action: action,
      };

      await appWebHook(appSuccessWebHookDTO);
      return;
    }
    case Services.OPERATION: {
      const operationSuccessWebHookDTO: OperationSuccessWebHookDTO = {
        userIds: userIds,
        title: title,
        content: content,
        category: category,
        deepLink: deepLink,
        webLink: webLink,
        messageIds: messageIds,
      };

      await operationWebHook(operationSuccessWebHookDTO);
      return;
    }
    case Services.CREW: {
      return;
    }
  }
};

export default { pushSuccessWebHook };
