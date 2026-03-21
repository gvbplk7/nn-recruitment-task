FROM gradle:8.14.3-jdk21-alpine AS builder
WORKDIR /workspace

COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
RUN mkdir -p /app/data && chown -R spring:spring /app

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

USER spring:spring
EXPOSE 8080

ENV H2_DB_FILE=/app/data/account-db
VOLUME ["/app/data"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
