package com.sopt.push.config;

import static com.sopt.push.common.Constants.HTTP_CLIENT_CONNECT_TIMEOUT_SECONDS;

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
import java.net.http.HttpClient;
import java.time.Duration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.sns.SnsClient;

public class AppFactory {

  private static final AppFactory INSTANCE = new AppFactory();

  private final SendPushFacade sendPushFacade;
  private final WebHookService webHookService;
  private final UserService userService;
  private final HistoryService historyService;
  private final DeviceTokenService deviceTokenService;
  private final NotificationService notificationService;
  private final InvalidEndpointCleaner invalidEndpointCleaner;

  private AppFactory() {

    EnvConfig envConfig = new EnvConfig();
    AwsConfig awsConfig = new AwsConfig(envConfig);
    DynamoDbEnhancedClient dynamoClient = awsConfig.dynamoClient();
    SnsClient snsClient = awsConfig.snsClient();
    String tableName = awsConfig.tableName();

    HttpClient httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(HTTP_CLIENT_CONNECT_TIMEOUT_SECONDS))
            .build();

    UserRepository userRepository = new UserRepository(dynamoClient, tableName);
    HistoryRepository historyRepository = new HistoryRepository(dynamoClient, tableName);
    DeviceTokenRepository tokenRepository = new DeviceTokenRepository(dynamoClient, tableName);

    this.userService = new UserService(userRepository);
    this.historyService = new HistoryService(historyRepository);
    this.deviceTokenService = new DeviceTokenService(tokenRepository);
    this.notificationService = new NotificationService(snsClient, envConfig);
    this.invalidEndpointCleaner =
        new InvalidEndpointCleaner(
            this.userService, this.deviceTokenService, this.notificationService);

    this.webHookService = new WebHookService(httpClient, envConfig);
    this.sendPushFacade =
        new SendPushFacade(
            this.notificationService,
            this.webHookService,
            this.historyService,
            this.userService,
            this.deviceTokenService,
            invalidEndpointCleaner);
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

  public UserService userService() {
    return userService;
  }

  public HistoryService historyService() {
    return historyService;
  }

  public DeviceTokenService deviceTokenService() {
    return deviceTokenService;
  }

  public NotificationService notificationService() {
    return notificationService;
  }

  public InvalidEndpointCleaner invalidEndpointCleaner() {
    return invalidEndpointCleaner;
  }
}
