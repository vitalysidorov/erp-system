FROM gradle:8.11.1-jdk21-alpine AS builder
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S erpGroup && adduser -S erpUser -G erpGroup
USER erpUser

COPY --from=builder /app/build/libs/*.jar app.jar

ENV JAVA_OPTS="-XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
