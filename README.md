# ERP Platform — Автоматизация СТО (Автосервиса) 

### Архитектурный паттерн:
### Backend for Frontend (BFF) + Основной ERP-модуль

Проект представляет собой распределенную отказоустойчивую платформу для автоматизации процессов автосервиса, разделенную на два ключевых слоя: клиентский шлюз (`bff`) и внутреннее бизнес-ядро (`erp`).

---

## Структура репозитория и модулей

```text
my-erp-platform/
├── bff/                  # Backend For Frontend (WebFlux, Реактивный шлюз)
│   ├── src/main/java     # Логика B2C: авторизация, кеширование, бронирование слотов
│   └── build.gradle      # Spring Boot 3.2.4 (Java 17)
├── erp/                  # Внутреннее ядро СТО (Spring MVC, Синхронное бизнес-ядро)
│   ├── src/main/java     # Модули: CRM, Управление заказами, Склад (Inventory), Финансы
│   ├── src/main/resources# Liquibase-миграции, SQL-скрипты, настройки профилей
│   └── build.gradle      # Spring Boot 3.3.4 (Java 21)
└── k8s-infra/            # Конфигурации развертывания в Kubernetes инфраструктуру
```

---

## Технологический стек

### Слой BFF (Служба B2C-клиентов)
* **Runtime:** Java 17 / Gradle
* **Framework:** Spring Boot 3.2.4 + Spring WebFlux (Асинхронный стек)
* **Безопасность:** Spring Security Реактивный OAuth2 Resource Server (Декодирование и верификация JWT)
* **Резолюция сбоев и лимиты:** Resilience4j (Circuit Breaker для изоляции сбоев ERP + Rate Limiter для защиты от DDoS)
* **Кеширование:** Reactive Redis Template (Кеширование доступных временных слотов для записи на 15 секунд)

### Слой ERP (Внутренние сервисы СТО)
* **Runtime:** Java 21 / Gradle
* **Framework:** Spring Boot 3.3.4 + Spring MVC
* **База данных:** PostgreSQL Cluster (Архитектура Master-Replica с разделением Read/Write транзакций через Spring AOP-аспекты)
* **Миграции данных:** Liquibase (Декларативное управление схемами)
* **Полнотекстовый поиск:** Elasticsearch (Быстрый поиск по складу запчастей: OEM-номер, бренд, наименование) + Database Fallback (резервный поиск через JPA при сбое кластера ES)
* **Событийная модель:** Spring ApplicationEventPublisher (Асинхронная обработка финансовых проводок после закрытия заказ-нарядов)
* **Кеширование:** Redis Cache Manager (Spring Cache для каталога услуг и дашбордов)

---

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

---

## Особенности реализации и Паттерны

* **Балансировка Read/Write БД:** Написан `DataSourceAspect`, который перехватывает аннотации `@Transactional(readOnly = true)` и автоматически перенаправляет SQL-запросы на реплику (`REPLICA`), разгружая основную базу (`MASTER`).
* **FIFO Аллокация Запчастей:** Метод `allocateParts` автоматически списывает детали под заказ-наряды из наиболее старых партий по дате прихода, обеспечивая корректный складской учет.
* **Изоляция сбоев (Resilience):** Если модуль `erp` зависает или недоступен, Circuit Breaker в `bff` мгновенно активирует `fallbackGetSlots()`, возвращая пользователю пустой список вместо падения приложения по таймауту.
* **Архитектурный контроль:** Подключен `ArchUnit` тест (`ArchitectureRulesTest`), проверяющий чистоту слоев: репозитории не знают о контроллерах, соблюдаются единые суффиксы наименований классов.

---

## Локальный запуск и Тестирование

### Предварительные требования
* Наличие установленного Docker и настроенного локального окружения PostgreSQL, Redis, Elasticsearch.

### Запуск интеграционных тестов
Каждый модуль снабжен полной базой интеграционных тестов (используется MockMvc и Spring Security Test для проверки ролей),
необходимо поднять контейнер с тестовой БД и контейнер Elasticsearch для интеграционных тестов:

```bash
# Тестирование бизнес-логики ядра ERP
cd erp
./gradlew test

# Тестирование реактивного шлюза BFF
cd ../bff
./gradlew test
```
---

## Инструкция по развертыванию платформы
Проект подготовлен для enterprise-развертывания с использованием оператора **CloudNativePG (CNPG)** для управления высокодоступным кластером PostgreSQL.

#### 1. Установка оператора CloudNativePG
Перед развертыванием базы данных установите оператор CNPG в ваш кластер:
```bash
kubectl apply -f k8s-infra/01-infrastructure/cnpg-1.30.0.yaml
```
Убедитесь, что подог оператора перешел в статус `Running`:
```bash
kubectl get pods -n cnpg-system
```

#### 2. Создание секретов
Залейте в кластер секреты для баз данных и JWT-токенов (замените значения на свои реальные Base64-строки):
```bash
kubectl create secret generic erp-database-secrets \
  --from-literal=PG_PASSWORD=your_pg_password \
  --from-literal=ELASTIC_PASSWORD=your_elastic_password

kubectl create secret generic erp-jwt-secrets \
  --from-literal=JWT_ACCESS_SECRET=your_access_key \
  --from-literal=JWT_REFRESH_SECRET=your_refresh_key
```

#### 3. Развертывание кластера баз данных и очередей
Последовательно примените манифесты конфигурации инфраструктуры:
```bash
# Развертывание кластера PostgreSQL (Master + Replica)
kubectl apply -f k8s-infra/02-database/postgres-cluster.yaml

# Развертывание отказоустойчивого Redis Sentinel
kubectl apply -f k8s-infra/03-cache/redis-sentinel.yaml

# Развертывание кластера Elasticsearch
kubectl apply -f k8s-infra/04-search/elasticsearch-cluster.yaml
```

#### 4. Деплой приложений (BFF и Core ERP)
После того как базы данных сообщат о готовности (`kubectl get cluster`), разверните бизнес-логику:
```bash
kubectl apply -f k8s-infra/05-apps/erp-core-deployment.yaml
kubectl apply -f k8s-infra/05-apps/bff-gateway-deployment.yaml
```

---

## Мониторинг кластера (Prometheus)

Вся платформа подготовлена к продакшен-мониторингу: в модулях `bff` и `erp` эндпоинты сборщика метрик Micrometer открыты наружу, а метрики автоматически тегируются именем приложения (`application: bff` / `application: erp`).

В среде Kubernetes сбор метрик осуществляется внутри приватной сети кластера (CoreDNS) напрямую с подов, минуя NGINX Ingress.

### Быстрый запуск Prometheus в Kubernetes

#### 1. Создание пространства имен
Выделите изолированный нэймспейс под задачи мониторинга:
```bash
kubectl create namespace monitoring
```

#### 2. Развертывание конфигурации (ConfigMap)
Создайте файл `prometheus-config.yaml` со следующей конфигурацией сбора метрик (учитывает базовый префикс `/bff` для шлюза):

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  namespace: monitoring
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 5s

    scrape_configs:
      - job_name: 'bff-gateway'
        metrics_path: '/bff/actuator/prometheus'
        static_configs:
          - targets: ['bff.default.svc.cluster.local:8080'] 

      - job_name: 'erp-core'
        metrics_path: '/actuator/prometheus'
        static_configs:
          - targets: ['erp.default.svc.cluster.local:8085'] 

      - job_name: 'prometheus'
        static_configs:
          - targets: ['localhost:9090']
```
Примените его в кластере:
```bash
kubectl apply -f prometheus-config.yaml
```

#### 3. Настройка прав доступа (RBAC)
Дайте серверу Prometheus права на сканирование эндпоинтов подов кластера. Создайте и примените `prometheus-rbac.yaml`:

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: prometheus
  namespace: monitoring
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: prometheus
rules:
- apiGroups: [""]
  resources: ["nodes", "nodes/proxy", "services", "endpoints", "pods"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["extensions", "networking.k8s.io"]
  resources: ["ingresses"]
  verbs: ["get", "list", "watch"]
- nonResourceURLs: ["/metrics"]
  verbs: ["get"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: prometheus
subjects:
- kind: ServiceAccount
  name: prometheus
  namespace: monitoring
roleRef:
  kind: ClusterRole
  name: prometheus
  apiGroup: rbac.authorization.k8s.io
```
```bash
kubectl apply -f prometheus-rbac.yaml
```

#### 4. Развертывание сервера Prometheus
Создайте и примените манифест запуска сервиса `prometheus-deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: prometheus
  namespace: monitoring
spec:
  replicas: 1
  selector:
    matchLabels:
      app: prometheus
  template:
    metadata:
      labels:
        app: prometheus
    spec:
      serviceAccountName: prometheus
      containers:
        - name: prometheus
          image: prom/prometheus:v2.53.0
          args:
            - "--config.file=/etc/prometheus/prometheus.yml"
            - "--storage.tsdb.path=/prometheus"
            - "--web.enable-lifecycle"
          ports:
            - containerPort: 9090
              name: web
          volumeMounts:
            - name: config-volume
              mountPath: /etc/prometheus
            - name: storage-volume
              mountPath: /prometheus
      volumes:
        - name: config-volume
          configMap:
            name: prometheus-config
        - name: storage-volume
          emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: prometheus-service
  namespace: monitoring
spec:
  selector:
    app: prometheus
  ports:
    - protocol: TCP
      port: 9090
      targetPort: 9090
  type: ClusterIP
```
```bash
kubectl apply -f prometheus-deployment.yaml
```

#### 5. Доступ к веб-панели (Web UI)
Для безопасного подключения к панели управления без настройки внешних доменов выполните проброс портов:
```bash
kubectl port-forward svc/prometheus-service 9090:9090 -n monitoring
```
После этого откройте в браузере **`http://localhost:9090`** и перейдите в меню *Status -> Targets* для проверки сборщиков.

### Полезные PromQL запросы для проверки:
* Общее количество HTTP-запросов к ядру ERP: `http_server_requests_seconds_count{application="erp"}`
* Мониторинг пула соединений БД (HikariCP): `hikaricp_connections_active{application="erp"}`
* Счётчик загрузки процессора JVM: `system_cpu_usage{application="bff"}`
