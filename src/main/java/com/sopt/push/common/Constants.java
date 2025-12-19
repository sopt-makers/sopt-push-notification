package com.sopt.push.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {

  public static final String DEFAULT = "default";

    public static final String DELIMITER = "#";
  public static final String USER_PREFIX = "u#";
  public static final String TOKEN_PREFIX = "d#";
  public static final String HISTORY_PREFIX = "h#";
    public static final String YEAR_MONTH_FORMAT = "%04d-%02d";

  public static final String USER_ENTITY = "user";
  public static final String DEVICE_TOKEN_ENTITY = "deviceToken";
  public static final String HISTORY_ENTITY = "history";

  public static final String JSON = "json";
  public static final String DETAIL = "detail";

  public static final String GCM = "GCM";
  public static final String APNS = "APNS";

  public static final String DEFAULT_MESSAGE = "";

  public static final String UNKNOWN_USER = "unknown";
    public static final String APPLICATION_PROTOCOL = "application";
}
