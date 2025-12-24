package com.sopt.push.lambda;

import static com.sopt.push.common.Constants.TOKEN_PREFIX;
import static com.sopt.push.common.Constants.USER_PREFIX;
import static com.sopt.push.common.StatusCode.BAD_REQUEST;
import static com.sopt.push.common.StatusCode.INTERNAL_SERVER_ERROR;
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
import com.sopt.push.util.ResponseUtil;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ApiGatewayHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DeviceTokenService deviceTokenService;
  private final SendPushFacade sendPushFacade;
  private final InvalidEndpointCleaner invalidEndpointCleaner;
  private final HistoryService historyService;
  private final ObjectMapper mapper;

  public ApiGatewayHandler() {
    AppFactory factory = AppFactory.getInstance();
    this.deviceTokenService = factory.deviceTokenService();
    this.sendPushFacade = factory.sendPushFacade();
    this.invalidEndpointCleaner = factory.invalidEndpointCleaner();
    this.historyService = factory.historyService();
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
      context.getLogger().log("ApiGateway error: " + ex.getMessage());
      Map<String, Object> responseMap = ResponseUtil.errorResponse(BAD_REQUEST, ex.getMessage());
      return convertToApiGatewayResponse(responseMap);

    } catch (Exception ex) {
      context.getLogger().log("ApiGateway error: " + ex.getMessage());
      Map<String, Object> responseMap =
          ResponseUtil.errorResponse(
              INTERNAL_SERVER_ERROR, ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
      return convertToApiGatewayResponse(responseMap);
    }
  }

  private ApiGatewayRequestDto extractRequest(APIGatewayProxyRequestEvent event) {
    Map<String, String> headers = event.getHeaders();

    if (headers == null || headers.get("action") == null) {
      throw new BusinessException(ErrorMessage.INVALID_REQUEST, "Headers missing or invalid.");
    }

    Map<String, Object> body = parseBody(event);

    try {
      String actionStr = headers.get("action");
      String platformStr = headers.get("platform");
      String transactionId = headers.get("transactionId");
      String serviceStr = headers.get("service");

      Actions action = Actions.fromValue(actionStr);
      Platform platform = null;
      if (action == Actions.REGISTER || action == Actions.CANCEL) {
        if (platformStr == null || platformStr.isBlank()) {
          throw new BusinessException(
              ErrorMessage.INVALID_REQUEST, "Platform is required for REGISTER and CANCEL actions");
        }
        platform = Platform.fromValue(platformStr);
      } else if (platformStr != null && !platformStr.isBlank()) {
        platform = Platform.fromValue(platformStr);
      }

      RegisterHeaderDto header =
          new RegisterHeaderDto(transactionId, Services.fromValue(serviceStr), platform, action);

      return new ApiGatewayRequestDto(header, body);
    } catch (IllegalArgumentException e) {
      throw new BusinessException(ErrorMessage.INVALID_REQUEST, e.getMessage());
    }
  }

  private void handleRegister(ApiGatewayRequestDto request) {
    RegisterUserDto body = mapper.convertValue(request.body(), RegisterUserDto.class);
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
      deviceTokenService.registerToken(deviceToken, platform, userId);
      createRegisterLog(
          transactionId, userIds, deviceToken, platform, service, NotificationStatus.SUCCESS);
    } catch (Exception e) {
      throw new DeviceTokenException(ErrorMessage.REGISTER_USER_ERROR, e.getMessage(), e);
    }
  }

  private void handleCancel(ApiGatewayRequestDto request) {
    DeleteTokenDto body = mapper.convertValue(request.body(), DeleteTokenDto.class);
    String transactionId = request.header().transactionId();
    Services service = request.header().service();
    Platform platform = request.header().platform();
    String deviceToken = body.deviceToken();
    Set<String> userIds = body.userIds();
    Set<String> logUserIds = Set.of("NULL");

    RequestDeleteTokenDto finalDto =
        new RequestDeleteTokenDto(transactionId, service, platform, deviceToken, userIds);

    validate(finalDto);

    try {
      String userId = (userIds != null && !userIds.isEmpty()) ? userIds.iterator().next() : null;
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

      String actualUserId = extractUserIdFromSk(tokenEntity.getSk());
      String actualDeviceToken = extractDeviceTokenFromPk(tokenEntity.getPk());
      Platform tokenPlatform = Platform.fromValue(tokenEntity.getPlatform());

      UserTokenInfoDto userTokenInfo =
          new UserTokenInfoDto(
              actualUserId, actualDeviceToken, endpointArn, tokenPlatform, subscriptionArn);

      invalidEndpointCleaner.clean(userTokenInfo);
      createCancelLog(
          transactionId, logUserIds, deviceToken, platform, service, NotificationStatus.SUCCESS);
    } catch (Exception e) {
      throw new DeviceTokenException(ErrorMessage.DELETE_TOKEN_ERROR, e.getMessage(), e);
    }
  }

  private void handleSend(ApiGatewayRequestDto request) {
    SendPushDto body = mapper.convertValue(request.body(), SendPushDto.class);

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
    SendAllPushDto body = mapper.convertValue(request.body(), SendAllPushDto.class);

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

  private Map<String, Object> parseBody(APIGatewayProxyRequestEvent event) {
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

  private String extractUserIdFromSk(String sk) {
    if (sk.startsWith(USER_PREFIX)) {
      return sk.substring(USER_PREFIX.length());
    }
    return sk;
  }

  private String extractDeviceTokenFromPk(String pk) {
    if (pk.startsWith(TOKEN_PREFIX)) {
      return pk.substring(TOKEN_PREFIX.length());
    }
    return pk;
  }

  private void createRegisterLog(
      String transactionId,
      Set<String> userIds,
      String deviceToken,
      Platform platform,
      Services service,
      NotificationStatus status) {
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
            Actions.REGISTER.getValue(),
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

  private void createCancelLog(
      String transactionId,
      Set<String> userIds,
      String deviceToken,
      Platform platform,
      Services service,
      NotificationStatus status) {
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
            Actions.CANCEL.getValue(),
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
