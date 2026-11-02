# Build Stage
FROM gradle:8.6-jdk21 AS builder
WORKDIR /home/gradle/project

COPY build.gradle settings.gradle ./
COPY src ./src

RUN gradle bootJar --no-daemon

# Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN mkdir -p /data

COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

VOLUME ["/data"]

ENTRYPOINT ["java", "-jar", "app.jar"]
