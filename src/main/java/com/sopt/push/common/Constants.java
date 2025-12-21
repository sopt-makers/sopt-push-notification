package com.sopt.push.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {

  public static final String DEFAULT = "default";

  public static final String USER_PREFIX = "u#";
  public static final String TOKEN_PREFIX = "d#";
  public static final String HISTORY_PREFIX = "h#";
  public static final String SEPARATOR = "#";

  public static final String USER_ENTITY = "user";
  public static final String DEVICE_TOKEN_ENTITY = "deviceToken";
  public static final String HISTORY_ENTITY = "history";

  public static final String JSON = "json";
  public static final String DETAIL = "detail";

  public static final String GCM = "GCM";
  public static final String APNS = "APNS";

  public static final String TITLE = "title";
  public static final String CATEGORY = "category";
  public static final String ID = "id";
  public static final String SEND_AT = "sendAt";

  public static final String BODY = "body";
  public static final String ALERT = "alert";
  public static final String APS = "aps";

  public static final String CONTENT = "content";
  public static final String WEB_LINK = "webLink";
  public static final String DEEP_LINK = "deepLink";
  public static final String DATA = "data";
  public static final String TOKEN = "Token";

  public static final String DEFAULT_MESSAGE = "";
}
