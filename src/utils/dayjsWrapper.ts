import { Dayjs } from 'dayjs';
import dayjsBase from 'dayjs';
import utc from 'dayjs/plugin/utc';
import timezone from 'dayjs/plugin/timezone';

dayjsBase.extend(utc);
dayjsBase.extend(timezone);

const DEFAULT_TIMEZONE = 'Asia/Seoul';

const dayjs: (...args: Parameters<typeof dayjsBase>) => Dayjs = (...args) => {
  return dayjsBase(...args).tz(DEFAULT_TIMEZONE);
};

export default dayjs;
