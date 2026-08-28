INSERT INTO usr_employees (email, password_hash, last_name, first_name, role, salary_rate_percent, is_active)
VALUES ('manager@auto.by', '$2a$10$X8OpxmGUpB6A78NsmHjCduQ2w.UjN6u9B2yMvUf3eY7xW1LhVwN0m', 'Иванов', 'Сергей', 'MANAGER', 40.00, true),
       ('master1@auto.by', '$2a$10$X8OpxmGUpB6A78NsmHjCduQ2w.UjN6u9B2yMvUf3eY7xW1LhVwN0m', 'Петров', 'Алексей', 'MASTER', 35.00, true),
       ('master2@auto.by', '$2a$10$X8OpxmGUpB6A78NsmHjCduQ2w.UjN6u9B2yMvUf3eY7xW1LhVwN0m', 'Сидоров', 'Дмитрий', 'MASTER', 35.00, true),
       ('mechanic1@auto.by', '$2a$10$X8OpxmGUpB6A78NsmHjCduQ2w.UjN6u9B2yMvUf3eY7xW1LhVwN0m', 'Козлов', 'Николай', 'MECHANIC', 45.00, true),
       ('mechanic2@auto.by', '$2a$10$X8OpxmGUpB6A78NsmHjCduQ2w.UjN6u9B2yMvUf3eY7xW1LhVwN0m', 'Морозов', 'Владимир', 'MECHANIC', 50.00, true);

INSERT INTO crm_clients (last_name, first_name, phone, password_hash, role, refresh_token)
VALUES ('Васильев', 'Андрей', '+375291112233', '$2a$10$X8OpxmGUpB6A78NsmHjCduQ2w.UjN6u9B2yMvUf3eY7xW1LhVwN0m', 'CLIENT', NULL),
       ('Федоров', 'Виталий', '+375294445566', '$2a$10$X8OpxmGUpB6A78NsmHjCduQ2w.UjN6u9B2yMvUf3eY7xW1LhVwN0m', 'CLIENT', NULL),
       ('Смирнова', 'Елена', '+375297778899', '$2a$10$X8OpxmGUpB6A78NsmHjCduQ2w.UjN6u9B2yMvUf3eY7xW1LhVwN0m', 'CLIENT', NULL);

INSERT INTO inv_parts_catalog (oem_number, name, brand)
VALUES ('50830TA0A01', 'Опора двигателя правая', 'Honda'),
       ('0446502220', 'Колодки тормозные передние', 'Toyota'),
       ('W71294', 'Фильтр масляный', 'Mann'),
       ('5W30-4L', 'Масло моторное 5W30 4л', 'Mobil');

INSERT INTO ord_services_catalog (name, norm_hours, hour_rate_price)
VALUES ('Замена масла в двигателе', 0.50, 60.00),
       ('Замена передних тормозных колодок', 1.20, 65.00),
       ('Диагностика подвески комплексная', 0.80, 50.00),
       ('Капитальный ремонт двигателя', 12.00, 80.00);

INSERT INTO ord_car_classes (brand, price_coefficient)
VALUES ('Toyota', 1.00),
       ('Honda', 1.10),
       ('BMW', 1.40),
       ('Mercedes-Benz', 1.50);

INSERT INTO crm_vehicles (client_id, vin, make, model, plate_number)
VALUES (1, 'JH4CU21609C000123', 'Honda', 'Accord', '7777 AB-7'),
       (2, 'JTDKN3DU001456789', 'Toyota', 'Camry', '1234 KH-7'),
       (3, 'WBA3A510X0F123456', 'BMW', '320i', '5555 OO-5');

INSERT INTO inv_stocks (part_id, quantity, purchase_price, retail_price)
VALUES (1, 3, 120.00, 165.00),
       (2, 15, 35.00, 55.00),
       (3, 40, 8.50, 14.00),
       (4, 25, 45.00, 70.00);

INSERT INTO inv_part_batches (part_id, initial_quantity, available_quantity, purchase_price, received_at)
VALUES (1, 5, 3, 120.00, '2026-08-01 10:00:00'),
       (2, 20, 15, 35.00, '2026-08-05 11:30:00'),
       (3, 50, 40, 8.50, '2026-08-10 09:15:00'),
       (4, 30, 25, 45.00, '2026-08-12 14:00:00');

WITH inserted_employee AS (
    INSERT INTO usr_employees (email, password_hash, last_name, first_name, role, salary_rate_percent, is_active)
        VALUES ('manager1@auto.by', '$2a$10$X8OpxmGvds9878NsmHjCduQ2w.UjN6u9B2yMvUf3eY7xW1LhVwN0m', 'Иванов', 'Иван', 'MANAGER', 43.00, true)
        RETURNING id)
INSERT INTO ord_bookings (created_by, source, vehicle_id, start_time, end_time, status)
SELECT CAST(inserted_employee.id AS VARCHAR), 'MANUAL', '1', '2026-08-21 09:00:00', '2026-08-21 11:00:00', 'CONFIRMED'
FROM inserted_employee;

WITH inserted_employee AS (
    INSERT INTO usr_employees (email, password_hash, last_name, first_name, role, salary_rate_percent, is_active)
        VALUES ('manager2@auto.by', '$2a$10$X8OpxmGvds9878NsmHjCduQ2wqahgsehtesh5MvUf3eY7xW1LhVwN0m', 'Петров', 'Иван', 'MANAGER', 49.00, true)
        RETURNING id)
INSERT INTO ord_bookings (created_by, source, vehicle_id, start_time, end_time, status)
SELECT CAST(inserted_employee.id AS VARCHAR), 'MANUAL', '2', '2026-08-21 12:00:00', '2026-08-21 14:00:00', 'PENDING'
FROM inserted_employee;

INSERT INTO ord_bookings (created_by, source, vehicle_id, start_time, end_time, status)
VALUES ('1', 'BFF_WEB', '3', '2026-08-22 15:00:00', '2026-08-22 16:30:00', 'CONFIRMED');

INSERT INTO ord_work_orders (id, vehicle_id, master_id, status, mileage_in, fuel_level, damages_notes, created_at, closed_at)
VALUES (1, 1, 2, 'CLOSED', 185000, '1/2', 'Царапина на заднем левом крыле', '2026-08-20 09:15:00', '2026-08-20 12:40:00'),
       (2, 2, 2, 'IN_PROGRESS', 92000, '1/4', 'Сколы на капоте', '2026-08-21 08:30:00', NULL),
       (3, 3, 3, 'OPENED', 143000, 'Полный', 'Без видимых кузовных повреждений', '2026-08-21 10:00:00', NULL);

ALTER SEQUENCE ord_work_orders_id_seq RESTART WITH 4;

INSERT INTO ord_order_services (work_order_id, service_id, mechanic_id, quantity, final_price)
VALUES (1, 1, 4, 1, 60.00),
       (1, 3, 4, 1, 50.00),
       (2, 2, 5, 1, 65.00);

INSERT INTO ord_order_parts (work_order_id, part_id, quantity, final_price)
VALUES (1, 3, 1, 14.00),
       (1, 4, 1, 70.00),
       (2, 2, 1, 55.00);

INSERT INTO fin_transactions (work_order_id, type, amount, created_at)
VALUES (1, 'INCOME', 194.00, '2026-08-20 12:40:00');

INSERT INTO ord_order_status_log (work_order_id, from_status, to_status, changed_at, changed_by_id)
VALUES (1, NULL, 'OPENED', '2026-08-20 09:15:00', 2),
       (1, 'OPENED', 'IN_PROGRESS', '2026-08-20 09:40:00', 2),
       (1, 'IN_PROGRESS', 'COMPLETED', '2026-08-20 12:10:00', 2),
       (1, 'COMPLETED', 'CLOSED', '2026-08-20 12:40:00', 2),
       (2, NULL, 'OPENED', '2026-08-21 08:30:00', 2),
       (2, 'OPENED', 'IN_PROGRESS', '2026-08-21 08:45:00', 2),
       (3, NULL, 'OPENED', '2026-08-21 10:00:00', 3);
