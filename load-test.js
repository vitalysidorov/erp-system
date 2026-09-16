import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 50 },
        { duration: '30s', target: 100 },
        { duration: '20s', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<1500'],
    },
};

const BASE_URL = 'http://localhost:8080/bff/api/v1';

const LOGIN_PAYLOAD = JSON.stringify({
    phone: '+375291112233',
    password: 'password'
});

const HEADERS = { 'Content-Type': 'application/json' };

export default function () {
    // Авторизация. Сервер возвращает accessToken в JSON, а куку refreshToken сохраняет в k6 автоматически
    const loginRes = http.post(`${BASE_URL}/b2c/auth/login`, LOGIN_PAYLOAD, { headers: HEADERS });

    const isLoginOk = check(loginRes, {
        'Login success (200)': (r) => r.status === 200,
    });

    if (!isLoginOk) {
        sleep(1);
        return;
    }

    // Вытаскиваем access-токен из тела ответа
    let accessToken = loginRes.json().accessToken;

    // Этот заголовок нужен для защищенных эндпоинтов (слоты, бронирование)
    let authHeaders = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`
    };

    // Запрос слотов (использует Bearer токен)
    const slotsUrl = `${BASE_URL}/client/bookings/slots?date=2026-09-01&durationHours=2`;
    const slotsRes = http.get(slotsUrl, { headers: authHeaders });

    check(slotsRes, {
        'Get slots status is 200': (r) => r.status === 200,
    });

    sleep(1);

    // Запрос бронирования (использует Bearer токен)
    const bookingPayload = JSON.stringify({
        vehicleId: 1,
        startTime: '2026-09-01T14:00:00',
        endTime: '2026-09-01T16:00:00'
    });

    const bookingRes = http.post(`${BASE_URL}/client/bookings`, bookingPayload, { headers: authHeaders });

    check(bookingRes, {
        'Booking handled': (r) => [200, 409, 429, 503].includes(r.status),
    });

    sleep(2);

    // Тестирование метода REFRESH
    // Кука refreshToken подставится автоматически из памяти k6 (Cookie Jar)
    const refreshRes = http.post(`${BASE_URL}/b2c/auth/refresh`, null, { headers: HEADERS });

    const isRefreshOk = check(refreshRes, {
        'Refresh status is 200': (r) => r.status === 200,
        'New access token present': (r) => r.json().accessToken !== undefined,
    });

    // Если рефреш прошел успешно, обновляем токен для следующих итераций/запросов этого пользователя
    if (isRefreshOk) {
        accessToken = refreshRes.json().accessToken;
        authHeaders['Authorization'] = `Bearer ${accessToken}`;
    }

    sleep(2);
}
