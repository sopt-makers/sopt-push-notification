package com.sopt.push.lambda;

import static com.sopt.push.common.Constants.HEADER_ACTION;
import static com.sopt.push.common.Constants.HEADER_PLATFORM;
import static com.sopt.push.common.Constants.HEADER_SERVICE;
import static com.sopt.push.common.Constants.HEADER_TRANSACTION_ID;
import static com.sopt.push.common.Constants.USER_PREFIX;
import static com.sopt.push.common.StatusCode.BAD_REQUEST;
import static com.sopt.push.common.StatusCode.INTERNAL_SERVER_ERROR;
import static com.sopt.push.enums.Platform.fromValue;
import static com.sopt.push.util.ValidationUtil.validate;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.push.common.BusinessException;
import com.sopt.push.common.DeviceTokenException;
import com.sopt.push.common.ErrorMessage;
import com.sopt.push.common.SuccessMessage;
import com.sopt.push.config.AppFactory;
import com.sopt.push.config.ObjectMapperConfig;
import com.sopt.push.domain.DeviceTokenEntity;
import com.sopt.push.dto.*;
import com.sopt.push.enums.Actions;
import com.sopt.push.enums.NotificationStatus;
import com.sopt.push.enums.NotificationType;
import com.sopt.push.enums.Platform;
import com.sopt.push.enums.Services;
import com.sopt.push.service.DeviceTokenService;
import com.sopt.push.service.HistoryService;
import com.sopt.push.service.InvalidEndpointCleaner;
import com.sopt.push.service.SendPushFacade;
import com.sopt.push.service.TokenRegisterFacade;
import com.sopt.push.util.ResponseUtil;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApiGatewayHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DeviceTokenService deviceTokenService;
  private final SendPushFacade sendPushFacade;
  private final InvalidEndpointCleaner invalidEndpointCleaner;
  private final HistoryService historyService;
  private final TokenRegisterFacade tokenRegisterFacade;
  private final ObjectMapper mapper;

  public ApiGatewayHandler() {
    AppFactory factory = AppFactory.getInstance();
    this.deviceTokenService = factory.deviceTokenService();
    this.sendPushFacade = factory.sendPushFacade();
    this.invalidEndpointCleaner = factory.invalidEndpointCleaner();
    this.historyService = factory.historyService();
    this.tokenRegisterFacade = factory.tokenRegisterFacade();
    this.mapper = ObjectMapperConfig.getObjectMapper();
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(
      APIGatewayProxyRequestEvent event, Context context) {

    try {
      ApiGatewayRequestDto request = extractRequest(event);
      Actions action = request.header().action();

      switch (action) {
        case REGISTER -> handleRegister(request);
        case CANCEL -> handleCancel(request);
        case SEND -> handleSend(request);
        case SEND_ALL -> handleSendAll(request);
        default -> throw new BusinessException(ErrorMessage.INVALID_REQUEST);
      }

      SuccessMessage successMessage = getSuccessMessage(action);
      Map<String, Object> responseMap = ResponseUtil.successResponse(successMessage);
      return convertToApiGatewayResponse(responseMap);

    } catch (BusinessException ex) {
      log.error("ApiGateway error: {}", ex.getMessage());
      Map<String, Object> responseMap = ResponseUtil.errorResponse(BAD_REQUEST, ex.getMessage());
      return convertToApiGatewayResponse(responseMap);

    } catch (Exception ex) {
      log.error("ApiGateway error: {}", ex.getMessage(), ex);
      Map<String, Object> responseMap =
          ResponseUtil.errorResponse(
              INTERNAL_SERVER_ERROR, ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
      return convertToApiGatewayResponse(responseMap);
    }
  }

  private ApiGatewayRequestDto extractRequest(APIGatewayProxyRequestEvent event) {
    Map<String, String> headers = event.getHeaders();
    Map<String, Object> body = parseRequestBody(event);
    boolean isInvalidHeader = headers == null || headers.get(HEADER_ACTION) == null;

    if (isInvalidHeader) {
      throw new BusinessException(ErrorMessage.INVALID_REQUEST, "Headers missing or invalid.");
    }

    try {
      String actionStr = headers.get(HEADER_ACTION);
      String platformStr = headers.get(HEADER_PLATFORM);
      String transactionId = headers.get(HEADER_TRANSACTION_ID);
      String serviceStr = headers.get(HEADER_SERVICE);
      Actions action = Actions.fromValue(actionStr);
      Platform platform = fromValue(platformStr);

      if (action == Actions.REGISTER || action == Actions.CANCEL) {
        checkPlatform(platformStr);
      }

      RegisterHeaderDto header =
          new RegisterHeaderDto(transactionId, Services.fromValue(serviceStr), platform, action);

      return new ApiGatewayRequestDto(header, body);
    } catch (Exception e) {
      log.error("Failed to extract request: {}", e.getMessage(), e);
      throw new BusinessException(ErrorMessage.INVALID_REQUEST, e.getMessage());
    }
  }

  private void handleRegister(ApiGatewayRequestDto request) {
    RequestRegisterUserDto body = mapper.convertValue(request.body(), RequestRegisterUserDto.class);
    String transactionId = request.header().transactionId();
    Services service = request.header().service();
    Platform platform = request.header().platform();
    String deviceToken = body.deviceToken();
    Set<String> userIds = body.userIds();
    String userId = (userIds != null && !userIds.isEmpty()) ? userIds.iterator().next() : null;
    RequestRegisterUserDto finalDto =
        new RequestRegisterUserDto(transactionId, service, platform, deviceToken, userIds);

    validate(finalDto);

    try {
      tokenRegisterFacade.register(deviceToken, platform, userId);
      createHistoryLog(
          transactionId,
          userIds,
          deviceToken,
          platform,
          service,
          NotificationStatus.SUCCESS,
          Actions.REGISTER);
    } catch (Exception e) {
      log.error("Failed to register token: {}", e.getMessage(), e);
    }
  }

  private void handleCancel(ApiGatewayRequestDto request) {
    RequestDeleteTokenDto body = mapper.convertValue(request.body(), RequestDeleteTokenDto.class);
    String transactionId = request.header().transactionId();
    Services service = request.header().service();
    Platform platform = request.header().platform();
    String deviceToken = body.deviceToken();
    Set<String> userIds = body.userIds();
    RequestDeleteTokenDto finalDto =
        new RequestDeleteTokenDto(transactionId, service, platform, deviceToken, userIds);

    validate(finalDto);

    try {
      boolean isInvalidUserId = userIds != null && !userIds.isEmpty();
      String userId = isInvalidUserId ? userIds.iterator().next() : null;
      if (userId == null) {
        throw new DeviceTokenException(ErrorMessage.USER_ID_REQUIRED);
      }

      DeviceTokenEntity tokenEntity =
          deviceTokenService.findTokenByDeviceTokenAndUserId(deviceToken, userId);
      if (tokenEntity == null) {
        throw new DeviceTokenException(ErrorMessage.TOKEN_NOT_FOUND);
      }

      String endpointArn = tokenEntity.getEndpointArn();
      String subscriptionArn = tokenEntity.getSubscriptionArn();

      if (endpointArn == null || subscriptionArn == null) {
        throw new DeviceTokenException(ErrorMessage.ARN_UNDEFINED);
      }

      Platform tokenPlatform = fromValue(tokenEntity.getPlatform());
      UserTokenInfoDto userTokenInfo =
          new UserTokenInfoDto(userId, deviceToken, endpointArn, tokenPlatform, subscriptionArn);

      invalidEndpointCleaner.clean(userTokenInfo);
      createHistoryLog(
          transactionId,
          Set.of(userId),
          deviceToken,
          platform,
          service,
          NotificationStatus.SUCCESS,
          Actions.CANCEL);
    } catch (Exception e) {
      log.error("Failed to cancel token: {}", e.getMessage(), e);
    }
  }

  private void handleSend(ApiGatewayRequestDto request) {
    RequestSendPushMessageDto body =
        mapper.convertValue(request.body(), RequestSendPushMessageDto.class);
    RequestSendPushMessageDto finalDto =
        new RequestSendPushMessageDto(
            request.header().transactionId(),
            request.header().service(),
            body.userIds(),
            body.title(),
            body.content(),
            body.category(),
            body.deepLink(),
            body.webLink());

    validate(finalDto);
    sendPushFacade.sendPush(finalDto);
  }

  private void handleSendAll(ApiGatewayRequestDto request) {
    RequestSendAllPushMessageDto body =
        mapper.convertValue(request.body(), RequestSendAllPushMessageDto.class);
    RequestSendAllPushMessageDto finalDto =
        new RequestSendAllPushMessageDto(
            request.header().transactionId(),
            request.header().service(),
            body.title(),
            body.content(),
            body.category(),
            body.deepLink(),
            body.webLink());

    validate(finalDto);
    sendPushFacade.sendPushAll(finalDto);
  }

  private void checkPlatform(String platformStr) {
    boolean isValidPlatform = platformStr == null || platformStr.isBlank();
    if (isValidPlatform) {
      throw new BusinessException(
          ErrorMessage.INVALID_REQUEST, "Platform is required for REGISTER and CANCEL actions");
    }
  }

  private Map<String, Object> parseRequestBody(APIGatewayProxyRequestEvent event) {
    if (event.getBody() == null) {
      throw new BusinessException(ErrorMessage.INVALID_REQUEST, "Request body is missing.");
    }

    try {
      return mapper.readValue(event.getBody(), Map.class);
    } catch (Exception e) {
      throw new BusinessException(ErrorMessage.INVALID_REQUEST, "Failed to parse request body.");
    }
  }

  private SuccessMessage getSuccessMessage(Actions action) {
    return switch (action) {
      case REGISTER -> SuccessMessage.TOKEN_REGISTER_SUCCESS;
      case CANCEL -> SuccessMessage.TOKEN_CANCEL_SUCCESS;
      case SEND, SEND_ALL -> SuccessMessage.SEND_SUCCESS;
    };
  }

  private APIGatewayProxyResponseEvent convertToApiGatewayResponse(
      Map<String, Object> responseMap) {
    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
    response.setStatusCode((Integer) responseMap.get("statusCode"));
    response.setBody((String) responseMap.get("body"));
    return response;
  }

  private void createHistoryLog(
      String transactionId,
      Set<String> userIds,
      String deviceToken,
      Platform platform,
      Services service,
      NotificationStatus status,
      Actions action) {
    CreateHistoryDto createHistoryDto =
        new CreateHistoryDto(
            transactionId,
            null,
            null,
            null,
            null,
            NotificationType.PUSH.getValue(),
            service.getValue(),
            status.getValue(),
            action.getValue(),
            platform != null ? platform.getValue() : null,
            deviceToken,
            null,
            userIds != null
                ? userIds.stream().map(u -> USER_PREFIX + u).collect(Collectors.toSet())
                : Collections.emptySet(),
            null,
            null,
            null,
            null);
    historyService.createLog(createHistoryDto);
  }
}
