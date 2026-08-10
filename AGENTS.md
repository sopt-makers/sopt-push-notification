# Repository Guidelines

## Project Structure & Module Organization

This is a Java 21 AWS Lambda push notification service built with Gradle and SAM. Code lives under `src/main/java/com/sopt/push`:

- `lambda/`: handlers for API Gateway, EventBridge, and SNS.
- `service/`, `repository/`, `client/`: business logic, DynamoDB access, and AWS SDK providers.
- `dto/`, `domain/`, `enums/`, `common/`, `util/`, `config/`: request models, persistence models, shared constants, utilities, and wiring.
- `src/main/resources/logback.xml`: logging configuration.
- `events/`: SAM payloads for API Gateway, SNS, and EventBridge.
- `template.yaml`: SAM infrastructure.
- `config/checkstyle/checkstyle.xml`: style rules.

Add tests under `src/test/java` using the production package layout.

## Build, Test, and Development Commands

- `./gradlew build`: compiles, checks, tests, and builds the Lambda fat jar.
- `./gradlew shadowJar`: creates `build/libs/app.jar`.
- `./gradlew test`: runs JUnit 5 tests.
- `./gradlew check`: runs tests, Checkstyle, and Spotless format checks.
- `./gradlew format`: applies Spotless formatting with Google Java Format.
- `sam build`: builds the SAM application after `build/libs/app.jar` exists.
- `sam local invoke SnsHandlerFunction --event events/sns-event-single.json --env-vars params-dev.json`: invokes SNS locally.
- `./test-sns-handler.sh`: SNS handler local testing helper.

## Coding Style & Naming Conventions

Use Java 21 features conservatively and follow the existing package structure. Spotless applies Google Java Format, removes unused imports, trims whitespace, and requires a final newline. Checkstyle enforces naming, braces, import hygiene, 150-line methods, and 7-parameter methods.

Name DTOs with the existing `*Dto` suffix, domain entities with `*Entity`, Lambda handlers with `*Handler`, services with `*Service` or `*Facade`, and AWS clients with `*ClientProvider`.

## Testing Guidelines

Use JUnit Jupiter and Mockito. Name test classes after the class under test, for example `ApiGatewayHandlerTest`. Prefer focused unit tests for services and utilities, and use `events/*.json` plus SAM for handler-level checks. Run `./gradlew test` before opening a PR; run `./gradlew check` when style may be affected.

## Commit & Pull Request Guidelines

Recent commits use uppercase bracketed types, sometimes with an issue number, such as `[FIX] ...`, `[DOCS] ...`, and `[CHORE/#26] ...`. Keep messages concise and action-oriented.

Pull requests should include a problem summary, approach, test evidence, and any deployment or environment changes. For Lambda behavior changes, mention affected handlers and the sample events or SAM commands used.

## Security & Configuration Tips

Do not commit AWS credentials, real device tokens, production ARNs, or local `params-dev.json` files containing secrets. Keep environment-specific values in AWS/SAM parameters or local ignored files, and use example values in `events/` unless a test explicitly requires real data.
