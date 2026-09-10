# ERP для автосервиса

Модульный монолит на Spring Boot (`erp`) + реактивный BFF-шлюз (`bff`) для управления
автосервисом (СТО): клиенты и автомобили, запись на обслуживание, заказ-наряды, складской
учёт запчастей по партиям (FIFO), финансовая аналитика, сотрудники.

## Технологический стек

| Слой | Технологии |
|---|---|
| Backend (`erp`) | Java 21, Spring Boot 3.3.4, Spring Data JPA, Spring Security (Resource Server, HS256 JWT), MapStruct, Liquibase |
| Backend (`bff`) | Java 17, Spring Boot 3.2.4, Spring WebFlux, Spring Security Reactive, Resilience4j |
| БД | PostgreSQL (CloudNativePG в k8s), раздельные master/replica датасорсы, кастомный `AbstractRoutingDataSource` |
| Кэш | Redis (Sentinel, TLS), Spring Cache abstraction |
| Поиск | Elasticsearch 8.12 (ECK), blue/green переиндексация без простоя |
| Очереди | RabbitMQ — уведомления о дефиците склада (`inventory.stock-deficiency`) |
| Аутентификация | Единый механизм: JWT (HMAC/HS256, jjwt), общий для сотрудников и клиентов |
| Отказоустойчивость | Resilience4j (Circuit Breaker, Rate Limiter) на маршруте BFF → ERP |
| Наблюдаемость | Micrometer + Prometheus, Actuator health-пробы (liveness/readiness) |
| Инфраструктура | Docker, Kubernetes: CNPG (Postgres), ECK (Elastic), Redis (Sentinel), RabbitMQ, nginx-ingress, PodDisruptionBudget, HPA |

## Архитектура

Клиентские запросы всегда идут через `bff`, который проверяет JWT, применяет rate limiting
и circuit breaker перед вызовом `erp`. Внутренние операторы (менеджеры, мастера, админы)
работают с `erp` напрямую через отдельный вход, защищённый на уровне Ingress IP-allowlist'ом. 

### Аутентификация

Единый механизм: собственный JWT-сервис (`AuthService` + `JwtProvider` на jjwt, HMAC-подпись). Refresh-токены хранятся в БД **в виде SHA-256 хеша**.

## Модули (`erp`)

| Модуль | Назначение |
|---|---|
| `employee` | Регистрация/увольнение сотрудников, вход по email, деактивация уволенных (блокировка действующих токенов) |
| `crm` | Клиенты и автомобили |
| `order` | Запись на обслуживание, заказ-наряды, задачи механиков |
| `inventory` | Склад по FIFO-партиям, полнотекстовый поиск (Elasticsearch), автоматическая наценка |
| `finance` | Событийная бухгалтерия: доходы/расходы создаются после `AFTER_COMMIT` основной операции |
| `bff` (отдельный сервис) | Реактивный B2C-шлюз для внешнего клиента |

## Безопасность

- **Авторизация владения**: клиент не может отменить/создать бронь на чужой автомобиль -
  проверка через переиспользуемый `@PreAuthorize`-компонент (`bookingSecurityService`), а
  не полагание на то, что роль CLIENT сама по себе ограничивает доступ.
- **TLS**: ни BFF и ERP, ни ERP и Elasticsearch не доверяют любому сертификату - используется
  реальный truststore. На Ingress включён `ssl-redirect` и обязательная TLS-секция.
- **Rate limiting**: прямой вход в ERP (`/api/v1/auth/`, минуя BFF) лимитирован отдельно на
  уровне Ingress, чтобы не оставался незащищённым обходным путём для credential stuffing.
- **Деактивация сотрудников**: фильтр блокировки уволенных сотрудников зарегистрирован после
  разбора JWT в цепочке Spring Security - действующий токен уволенного сотрудника
  действительно перестаёт работать, а не только при следующем логине.

## Матрица доступа и Ролевая модель (Spring Security)

Реализована строгая проверка полномочий на уровне конечных точек (Endpoint RBAC) и межсервисного взаимодействия:

1. **ROLE_CLIENT (B2C Пользователи):**
  * Самостоятельная открытая регистрация (`/api/v1/clients/register`).
  * Просмотр свободных окон для записи (`/api/vsuggest-slots`).
  * Создание и отмена собственных броней автомобилей.
2. **ROLE_MECHANIC (Автомеханики):**
  * Доступ к персональному списку активных задач в ремзоне (`/api/v1/mechanic/tasks/{id}`).
3. **ROLE_MANAGER (Мастера-приемщики / Операторы):**
  * Полное управление клиентами и автомобилями (CRM).
  * Открытие, ведение, калькуляция и перевод статусов заказ-нарядов (`/api/v1/work-orders`).
  * Прием и оприходование новых партий запчастей на склад по стратегии FIFO (`/api/v1/inventory/batches`).
4. **ROLE_ADMIN (Администраторы):**
  * Регистрация и увольнение сотрудников (`/api/v1/employees/**`).
  * Изменение процентных ставок сотрудников, сброс паролей.
  * Доступ к закрытой финансовой аналитике и отчетам по прибыли (`/api/v1/analytics/report`).
  * Принудительный запуск фоновой переиндексации склада в Elasticsearch.

## Запуск тестов локально

Интеграционные тесты (`BaseIntegrationTest` и наследники) ожидают реально запущенные
Postgres и Elasticsearch на портах, заданных в `erp/src/test/resources/application.yaml`.
RabbitMQ для тестов не обязателен — слушатель дефицита склада отключён в тестовом профиле
(`rabbitmq.listener.auto-start: false`), но брокер поднят в compose-файле для тех тестов,
которые всё же собирают полный контекст с `RabbitTemplate`.

```bash
# Поднять зависимости для тестов
docker compose -f docker-compose.test.yml up -d

# Дождаться healthy-статуса сервисов, затем:
cd erp
export TEST_DB_PASSWORD=postgres
export JWT_ACCESS_SECRET=test-access-secret-change-me-0123456789
export JWT_REFRESH_SECRET=test-refresh-secret-change-me-0123456789
export ELASTIC_PASSWORD=changeme
./gradlew test

# Остановить зависимости после тестов
docker compose -f docker-compose.test.yml down -v
```

## Деплой в Kubernetes

Манифесты в `k8s-infra/` применяются по порядку каталогов:

```bash
kubectl apply -f k8s-infra/01-infrastructure/   # CNPG-оператор, Postgres-кластер, Elastic, Redis, RabbitMQ
kubectl apply -f k8s-infra/02-infrastructure/   # Ingress (публичный auth-ingress лимитирован отдельно от остального публичного трафика)
kubectl apply -f k8s-infra/03-infrastructure/   # Deployments erp/bff, Service, PodDisruptionBudget, HPA
```

Перед первым деплоем нужно создать секреты (Redis-пароль, JWT-секреты, пароли БД/Elastic,
TLS-сертификаты для Ingress) через внешний секрет-менеджер.

## Структура репозитория

```
erp/                    — основной бэкенд, модульный монолит по доменам
bff/                    — реактивный Backend-for-Frontend для B2C-клиента
k8s-infra/              — манифесты Kubernetes (инфраструктура, Ingress, Deployments)
docker-compose.test.yml — окружение для интеграционных тестов (Postgres, Elasticsearch, RabbitMQ)
```

## Планы по дальнейшим доработкам

**Внедрение мультитенантности. План реализации:** 

Сейчас приложение рассчитано на использование в рамках одной СТО, это необходимо исправить.

Я планирую оставить общую схему и добавить дискриминатор branch_id:

У Stock уникальность сейчас part_id UNIQUE - станет составной (part_id, branch_id), потому что один и тот же артикул на разных складах - это разные остатки.

*JwtProvider.generateAccessToken* получит дополнительный параметр *branchId* (для ADMIN - null, что означает "без ограничения по филиалу"). Фильтр (по аналогии с EmployeeActiveFilter, который уже в цепочке) кладёт *branchId* в *BranchContextHolder* на время запроса и чистит после.

Точки бизнес-логики, которые сломаются без правки:

SmartBookingService.findAvailableSlots - рабочие часы 09:00–21:00 захардкожены в коде. С филиалами это должно браться из Branch.workHoursStart/End, иначе у филиала с другим графиком слоты будут считаться неверно.

BookingRepository.findOverlappingBookings - сейчас глобальный запрос по всем броням без учёта филиала. Без фильтра по branch_id бронь в одном городе будет "конфликтовать" по времени с бронью в другом — ложные отказы в записи.

WorkOrderService.allocateParts (FIFO-списание партий) - findAvailableBatches(partId) должен стать findAvailableBatches(partId, branchId), иначе списание в одном филиале съедает физический остаток другого.

PartBatchService.receiveNewBatch и StockRepository.findByPartId - поставка приходит в конкретный филиал; findByPartId заменить на findByPartIdAndBranchId.

StockIndexDocument (Elasticsearch) - нужно поле branchId, иначе полнотекстовый поиск по складу будет показывать менеджеру остатки чужой точки.

WorkOrderService.createWorkOrder - сейчас при первом визите клиент/авто создаются "с нуля" без привязки к филиалу; сам WorkOrder получает branch_id явно из контекста текущего сотрудника.

А дальнейшие изменения я буду рассматривать на практике.

**Внесение предоплаты клиентом. Необходимо реализовать:**

Идемпотентность вебхуков: платежная система может прислать один и тот же вебхук несколько раз. Необходимо проверять по базе данных, не обработан ли уже этот платеж.

Безопасность: при отправке запроса на создание платежа в шлюз всегда нужно передавать уникальный ключ идемпотентности, чтобы из-за сбоя сети у пользователя не списались деньги дважды.

Логирование: логировать все входящие сырые JSON-тела от вебхуков платежных систем. Если возникнет спорная ситуация по оплате, логи - единственный аргумент.

**Добавить уведомление пльзователей за час/сутки до брони с помощью SMS, дописать логику уведомления менеджеров по почте.**

## Текущая схема базы данных: