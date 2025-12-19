package com.sopt.push.message;

import static com.sopt.push.common.Constants.APNS;
import static com.sopt.push.common.Constants.DEFAULT;
import static com.sopt.push.common.Constants.GCM;

import com.sopt.push.dto.MessageFactoryDto;
import com.sopt.push.util.JsonUtil;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MessageCreator {

  public static String create(MessageFactoryDto dto) {

    String apns = ApnsMessageBuilder.build(dto);
    String fcm = FcmMessageBuilder.build(dto);

    return switch (dto.topic()) {
      case APNS -> JsonUtil.toJson(Map.of(DEFAULT, dto.content(), APNS, apns));
      case FCM -> JsonUtil.toJson(Map.of(DEFAULT, dto.content(), GCM, fcm));
      case ALL ->
          JsonUtil.toJson(
              Map.of(
                  DEFAULT, dto.content(),
                  APNS, apns,
                  GCM, fcm));
    };
  }
}
