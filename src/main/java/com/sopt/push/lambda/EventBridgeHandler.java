package com.sopt.push.lambda;

import static com.sopt.push.common.Constants.DETAIL;
import static com.sopt.push.util.ValidationUtil.validateDto;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.push.common.BusinessException;
import com.sopt.push.common.ErrorMessage;
import com.sopt.push.config.AppFactory;
import com.sopt.push.config.ObjectMapperConfig;
import com.sopt.push.dto.CustomEventDetailDto;
import com.sopt.push.dto.RequestSendAllPushMessageDto;
import com.sopt.push.dto.RequestSendPushMessageDto;
import com.sopt.push.enums.Actions;
import com.sopt.push.service.SendPushFacade;
import com.sopt.push.service.WebHookService;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventBridgeHandler implements RequestHandler<Map<String, Object>, String> {

  private final SendPushFacade sendPushFacade;
  private final WebHookService webHookService;
  private final ObjectMapper mapper;

  public EventBridgeHandler() {
    this(AppFactory.getInstance());
  }

  EventBridgeHandler(AppFactory factory) {
    this.sendPushFacade = factory.sendPushFacade();
    this.webHookService = factory.webHookService();
    this.mapper = ObjectMapperConfig.getObjectMapper();
  }

  @Override
  public String handleRequest(Map<String, Object> event, Context context) {
    try {
      CustomEventDetailDto detail = extractDetail(event);
      Actions action = detail.header().action();

      switch (action) {
        case SEND -> handleSend(detail);
        case SEND_ALL -> handleSendAll(detail);
        default -> throw new BusinessException(ErrorMessage.INVALID_REQUEST);
      }

      return "EventBridge processed";

    } catch (Exception ex) {
      log.error("EventBridge error: {}", ex.getMessage());
      return "EventBridge failed";
    }
  }

  private CustomEventDetailDto extractDetail(Map<String, Object> event) {
    Object detailObject = event.get(DETAIL);
    if (!(detailObject instanceof Map)) {
      throw new BusinessException(ErrorMessage.INVALID_REQUEST, "Event detail missing.");
    }

    return mapper.convertValue(detailObject, CustomEventDetailDto.class);
  }

  private void handleSend(CustomEventDetailDto detail) {
    RequestSendPushMessageDto body =
        mapper.convertValue(detail.body(), RequestSendPushMessageDto.class);

    RequestSendPushMessageDto finalDto =
        new RequestSendPushMessageDto(
            detail.header().transactionId(),
            detail.header().service(),
            body.userIds(),
            body.title(),
            body.content(),
            body.category(),
            body.deepLink(),
            body.webLink());

    validateDto(finalDto);
    sendPushFacade.sendPush(finalDto);

    webHookService.scheduleSuccessWebHook(detail.header().alarmId());
  }

  private void handleSendAll(CustomEventDetailDto detail) {

    RequestSendAllPushMessageDto body =
        mapper.convertValue(detail.body(), RequestSendAllPushMessageDto.class);

    RequestSendAllPushMessageDto finalDto =
        new RequestSendAllPushMessageDto(
            detail.header().transactionId(),
            detail.header().service(),
            body.title(),
            body.content(),
            body.category(),
            body.deepLink(),
            body.webLink());

    validateDto(finalDto);
    sendPushFacade.sendPushAll(finalDto);

    webHookService.scheduleSuccessWebHook(detail.header().alarmId());
  }
}
