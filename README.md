# ERP Enterprise Platform & Booking BFF

Высоконагруженная отказоустойчивая система управления ресурсами (ERP) на базе многомодульного монолита Spring Boot и реактивного BFF-сервиса на Spring WebFlux.

## Архитектурные особенности
* **Оркестрация:** Kubernetes (Deployments, HPA, Ingress Nginx, Cert-Manager)
* **База данных:** PostgreSQL 15 с автоматическим failover (CloudNativePG Operator) и CQRS (разделение пулов Read/Write)
* **Кэширование:** Redis Sentinel с шифрованием трафика по TLS
* **Поиск:** Отказоустойчивый кластер ElasticSearch (3 ноды, HTTPS) через ECK Operator
* **Отказоустойчивость приложений:** Circuit Breaker (Resilience4j), Graceful Shutdown

---

## Локальный запуск (Разработка)

### Предварительные требования
* Java 17+
* Docker и Docker Compose

### Быстрый старт инфраструктуры в Docker
Для локальной разработки развернут облегченный стек без Kubernetes. Из корня проекта выполните:
```bash
docker compose up -d
```

### Запуск приложений локально
1. **Запуск ERP-Монолита:**
   ```bash
   cd erp-monolith && ./mvnw spring-boot:run
   ```
2. **Запуск BFF-WebFlux:**
   ```bash
   cd bff-webflux && ./mvnw spring-boot:run
   ```

---

## Деплой в Kubernetes (Production)

Вся инфраструктура и манифесты деплоя находятся в директории `k8s-infra/`.

### Порядок первоначального развертывания кластера:
1. **Установка системных компонентов:**
   ```bash
   kubectl apply -f https://github.com
   kubectl apply -f https://github.com
   ```
2. **Установка операторов баз данных:**
   ```bash
   kubectl apply -f https://githubusercontent.com
   ```
3. **Развертывание окружения:**
   ```bash
   kubectl apply -f k8s-infra/01-infrastructure/
   kubectl apply -f k8s-infra/02-routing-security/
   ```

---

## CI/CD Пайплайн

Автоматизация построена на **GitHub Actions**.
* При пуше в ветку `main` триггеры отслеживают изменения в изолированных папках `erp-monolith/` и `bff-webflux/`.
* Производится сборка Maven -> Сборка Docker образа -> Пуш в Реестр -> Обновление тега в Kubernetes с помощью утилиты `sed` -> Безопасный деплой по стратегии `RollingUpdate`.

### Необходимые Secrets в репозитории GitHub:
* `KUBE_CONFIG` — Строка конфигурации вашего K8s кластера для авторизации агента сборщика.
