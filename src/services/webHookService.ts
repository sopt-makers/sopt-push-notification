import { PushSuccessMessageDTO, Services, Category, WebHookType } from '../types';
import axios from 'axios';
import dayjs from 'dayjs';

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
interface OperationScheduleSuccessWebHookDTO {
  sendAt: string;
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

async function operationScheduleSuccessWebHook(alarmId: number, dto: OperationScheduleSuccessWebHookDTO): Promise<void> {
  try {
    if (process.env.MAKERS_OPERATION_SERVER_URL === undefined) {
      throw new Error('env not defined');
    }
    const statusUpdateEndpoint = process.env.MAKERS_OPERATION_SERVER_URL + "/" + alarmId;
    await axiosInstance.patch(statusUpdateEndpoint, JSON.stringify(dto));
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

const scheduleSuccessWebHook = async (alarmId: number): Promise<void> => {
  if (alarmId === undefined) {
    throw new Error('schedule alarm id not defined');
  }

  const sendAt = dayjs().format('YYYY-MM-DD hh:mm');
  const updateStatusWebHookDTO: OperationScheduleSuccessWebHookDTO = {
    sendAt
  };

  await operationScheduleSuccessWebHook(alarmId, updateStatusWebHookDTO);
}

export default { pushSuccessWebHook, scheduleSuccessWebHook };
