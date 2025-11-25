plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.2"
    id("com.diffplug.spotless") version "6.23.3"
    id("checkstyle")
}

group = "com.sopt.push"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // AWS Lambda Java Core
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.3")
    
    // AWS SDK v2
    implementation(platform("software.amazon.awssdk:bom:2.20.162"))
    implementation("software.amazon.awssdk:sns")
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:url-connection-client")
    
    // JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
    
    // Validation
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")
    implementation("org.hibernate.validator:hibernate-validator:8.0.1.Final")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.20")
    
    // Utilities
    implementation("com.fasterxml.uuid:java-uuid-generator:4.2.0")
    
    // Test
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.5.0")
}

// Spotless (코드 포매팅)
spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.23.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// Checkstyle (코드 스타일 검사)
checkstyle {
    toolVersion = "10.12.5"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
}

tasks.named<Checkstyle>("checkstyleMain") {
    setSource("src/main/java")
}

tasks.named<Checkstyle>("checkstyleTest") {
    setSource("src/test/java")
}

// PMD는 현재 사용하지 않음 (필요 시 규칙 파일 추가 후 활성화)

tasks {
    test {
        useJUnitPlatform()
    }
    
    shadowJar {
        archiveBaseName.set("app")
        archiveClassifier.set("")
        archiveVersion.set("")
        
        manifest {
            attributes(mapOf("Main-Class" to "com.sopt.push.lambda.ApiHandler"))
        }
    }
    
    build {
        dependsOn(shadowJar)
    }
    
    // CI에서 포맷 검사도 함께 실행되도록 연결
    named("check") {
        dependsOn("spotlessCheck")
    }
    
    // 포매팅 자동 적용
    register("format") {
        group = "formatting"
        description = "Format code using Spotless"
        dependsOn("spotlessApply")
    }
    
    // 포매팅 검사만 (자동 적용 안 함)
    register("checkFormat") {
        group = "formatting"
        description = "Check code formatting"
        dependsOn("spotlessCheck")
    }
}

