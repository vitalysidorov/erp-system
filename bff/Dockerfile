# Этап 1: Чистая сборка внутри Docker
FROM gradle:8.6-jdk17 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
# Принудительно очищаем и собираем свежий JAR
RUN gradle clean bootJar -x test --no-daemon

# Этап 2: Финальный легковесный образ
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar bff-service.jar

RUN useradd -m erpuser
USER erpuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "bff-service.jar"]
