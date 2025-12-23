package com.sopt.push.config;

import com.sopt.push.repository.DeviceTokenRepository;
import com.sopt.push.repository.HistoryRepository;
import com.sopt.push.repository.UserRepository;
import com.sopt.push.service.DeviceTokenService;
import com.sopt.push.service.HistoryService;
import com.sopt.push.service.InvalidEndpointCleaner;
import com.sopt.push.service.NotificationService;
import com.sopt.push.service.SendPushFacade;
import com.sopt.push.service.UserService;
import com.sopt.push.service.WebHookService;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.sns.SnsClient;

public class AppFactory {

  private static final AppFactory INSTANCE = new AppFactory();

  private final SendPushFacade sendPushFacade;
  private final WebHookService webHookService;
  private final UserService userService;
  private final DeviceTokenService deviceTokenService;
  private final NotificationService notificationService;
  private final InvalidEndpointCleaner invalidEndpointCleaner;
  private final HistoryService historyService;

  private AppFactory() {

    EnvConfig envConfig = new EnvConfig();
    AwsConfig awsConfig = new AwsConfig(envConfig);
    DynamoDbEnhancedClient dynamoClient = awsConfig.dynamoClient();
    SnsClient snsClient = awsConfig.snsClient();
    String tableName = awsConfig.tableName();

    UserRepository userRepository = new UserRepository(dynamoClient, tableName);
    HistoryRepository historyRepository = new HistoryRepository(dynamoClient, tableName);
    DeviceTokenRepository tokenRepository = new DeviceTokenRepository(dynamoClient, tableName);

    HistoryService historyService = new HistoryService(historyRepository);
    NotificationService notificationService = new NotificationService(snsClient, envConfig);
    SnsFactory snsFactory = new SnsFactory(snsClient, envConfig);
    DeviceTokenService deviceTokenService = new DeviceTokenService(tokenRepository, userRepository, snsFactory);
    UserService userService = new UserService(userRepository);
    InvalidEndpointCleaner invalidEndpointCleaner =
        new InvalidEndpointCleaner(userService, deviceTokenService, notificationService);

    this.webHookService = new WebHookService();
    this.sendPushFacade =
        new SendPushFacade(
            notificationService,
            webHookService,
            historyService,
            userService,
            deviceTokenService,
            invalidEndpointCleaner);
    this.userService = userService;
    this.deviceTokenService = deviceTokenService;
    this.notificationService = notificationService;
    this.invalidEndpointCleaner = invalidEndpointCleaner;
    this.historyService = historyService;
  }

  public static AppFactory getInstance() {
    return INSTANCE;
  }

  public SendPushFacade sendPushFacade() {
    return sendPushFacade;
  }

  public WebHookService webHookService() {
    return webHookService;
  }

  public DeviceTokenService deviceTokenService() {
    return deviceTokenService;
  }

  public UserService userService() {
    return userService;
  }

  public NotificationService notificationService() {
    return notificationService;
  }

  public InvalidEndpointCleaner invalidEndpointCleaner() {
    return invalidEndpointCleaner;
  }

  public HistoryService historyService() {
    return historyService;
  }

}
