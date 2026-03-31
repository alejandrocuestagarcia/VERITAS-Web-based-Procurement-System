FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

COPY . .

RUN ./gradlew bootJar --no-daemon && cp build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]