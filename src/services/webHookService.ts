import { PushSuccessMessageDTO, Services, Category, WebHookType } from '../types';
import axios from 'axios';

interface AppSuccessWebHookDTO {
  id: string;
  title: string;
  content: string;
  category: Category;
  type: WebHookType;
  deepLink?: string;
  webLink?: string;
  userIds?: string[];
}

interface OperationSuccessWebHookDTO {
  id: string;
  title: string;
  content: string;
  category: Category;
  deepLink?: string;
  webLink?: string;
  userIds?: string[];
}

const axiosInstance = axios.create({
  headers: { 'Content-Type': `application/json` },
});

async function appWebHook(appSuccessWebHookDTO: AppSuccessWebHookDTO): Promise<void> {
  try {
    if (process.env.MAKERS_APP_SERVER_URL === undefined) {
      throw new Error('env not defined');
    }

    await axiosInstance.post(process.env.MAKERS_APP_SERVER_URL, JSON.stringify(appSuccessWebHookDTO));
  } catch (e) {
    throw new Error('APP SERVER webhook failed');
  }
}

async function operationWebHook(operationSuccessWebHookDTO: OperationSuccessWebHookDTO): Promise<void> {
  try {
    if (process.env.MAKERS_OPERATION_SERVER_URL === undefined) {
      throw new Error('env not defined');
    }

    await axiosInstance.post(process.env.MAKERS_OPERATION_SERVER_URL, JSON.stringify(operationSuccessWebHookDTO));
  } catch (e) {
    throw new Error('OPERATION SERVER webhook failed');
  }
}

const pushSuccessWebHook = async (dto: PushSuccessMessageDTO): Promise<void> => {
  const { userIds, title, content, category, deepLink, webLink, service, type, id } = dto;

  const appSuccessWebHookDTO: AppSuccessWebHookDTO = {
    id,
    userIds,
    title,
    content,
    category,
    deepLink,
    webLink,
    type,
  };

  switch (service) {
    case Services.APP: {
      await appWebHook(appSuccessWebHookDTO);
      return;
    }
    case Services.OPERATION: {
      await appWebHook(appSuccessWebHookDTO);
      return;
    }
    case Services.CREW: {
      await appWebHook(appSuccessWebHookDTO);
      return;
    }
  }
};

export default { pushSuccessWebHook };
