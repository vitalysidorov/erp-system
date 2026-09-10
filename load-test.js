import http from 'k6/http';
import { check, sleep } from 'k6';

// эмуляция реального наплыва пользователей
export const options = {
    stages: [
        { duration: '30s', target: 20 },  // плавное увеличение до 20 виртуальных пользователей
        { duration: '1m', target: 50 },   // удерживаем 50 активных VUs
        { duration: '30s', target: 100 }, // резко поднимаем до 100 VUs (проверка Resilience4j RateLimiter)
        { duration: '20s', target: 0 },   // плавное отключение пользователей
    ],
    thresholds: {
        http_req_failed: ['rate<0.05'],    // ошибок должно быть менее 5%
        http_req_duration: ['p(95)<1500'], // 95% запросов должны выполняться быстрее 1.5 сек
    },
};

const BASE_URL = 'http://localhost:8080/bff/api/v1'; // Адрес шлюза BFF

// данные для авторизации тестового клиента (из тестовых данных)
const LOGIN_PAYLOAD = JSON.stringify({
    phone: '+375291112233',
    password: 'password'
});

const HEADERS = { 'Content-Type': 'application/json' };

// инициализация сессии для каждого виртуального пользователя
export function setup() {
    // делаем один запрос на авторизацию для получения JWT
    const loginRes = http.post(`${BASE_URL}/b2c/auth/login`, LOGIN_PAYLOAD, { headers: HEADERS });

    const isLoginOk = check(loginRes, {
        'Auth successful (status 200)': (r) => r.status === 200,
        'Token present': (r) => r.json().accessToken !== undefined,
    });

    if (!isLoginOk) {
        fail('Critical Error: Инициализация теста провалена, невозможно получить JWT-токен!');
    }

    return { token: loginRes.json().accessToken };
}

// основные запросы
export default function (data) {
    const authHeaders = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${data.token}`
    };

    // проверка доступных слотов
    // формируем дату в формате ISO, запрашиваем длительность 2 часа
    const slotsUrl = `${BASE_URL}/client/bookings/slots?date=2026-09-01&durationHours=2`;
    const slotsRes = http.get(slotsUrl, { headers: authHeaders });

    check(slotsRes, {
        'Get slots status is 200': (r) => r.status === 200,
        'Redis cache hit/miss returned array': (r) => Array.isArray(r.json()),
    });

    sleep(1);

    // попытка создания бронирования
    const bookingPayload = JSON.stringify({
        vehicleId: 1,
        startTime: '2026-09-01T14:00:00',
        endTime: '2026-09-01T16:00:00'
    });

    const bookingRes = http.post(`${BASE_URL}/client/bookings`, bookingPayload, { headers: authHeaders });

    // при высокой конкуренции часть запросов вернет 200 (успех), а часть — 409/500 (время уже занято/Блокировка СУБД)
    // из-за Resilience4j RateLimiter на пике (100 VUs) также ожидаются ответы 429 Too Many Requests
    check(bookingRes, {
        'Booking handled without crashing': (r) => [200, 409, 429, 503].includes(r.status),
    });

    sleep(2);
}
