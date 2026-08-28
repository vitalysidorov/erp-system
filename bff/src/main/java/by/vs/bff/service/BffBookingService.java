package by.vs.bff.service;

import by.vs.bff.client.ErpClient;
import by.vs.bff.dto.*;
import by.vs.bff.exception.ErpUnavailableException;
import by.vs.bff.exception.SlotAlreadyBookedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BffBookingService {

    private final ErpClient erpClient;
    private final ReactiveRedisTemplate<String, TimeSlotDto> redisTemplate;

    @RateLimiter(name = "b2cRateLimiter")
    @CircuitBreaker(name = "erpBookingService", fallbackMethod = "fallbackGetSlots")
    public Mono<List<TimeSlotDto>> getSlotsForClient(String dateStr, int durationHours) {
        String cacheKey = String.format("slots::%s::%d", dateStr, durationHours);

        return redisTemplate.opsForList().range(cacheKey, 0, -1)
                .collectList()
                .flatMap(cachedSlots -> {
                    if (!cachedSlots.isEmpty()) {
                        log.info("Кэш HIT для ключа: {}", cacheKey);
                        return Mono.just(cachedSlots);
                    }

                    log.info("Кэш MISS для ключа: {}. Запрашиваем ERP...", cacheKey);

                    // Конвертируем строку ("yyyy-MM-dd") в LocalDateTime (на 00:00:00) для ERP
                    LocalDateTime requestDateTime = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();

                    return erpClient.getAvailableSlots(requestDateTime, durationHours)
                            .flatMap(erpSlots -> {
                                if (erpSlots.isEmpty()) {
                                    return Mono.just(erpSlots);
                                }
                                return redisTemplate.opsForList().rightPushAll(cacheKey, erpSlots)
                                        .then(redisTemplate.expire(cacheKey, java.time.Duration.ofSeconds(15)))
                                        .then(Mono.just(erpSlots));
                            });
                });
    }

    public Mono<ClientBookingResponse> createBookingForClient(String clientId, ClientBookingRequest clientRequest) {
        ErpBookingRequest erpRequest = new ErpBookingRequest(
                clientRequest.vehicleId(),
                clientRequest.startTime(),
                clientRequest.endTime(),
                clientId,
                "BFF_WEB"
        );

        return erpClient.createBooking(erpRequest)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.warn("ERP вернула ошибку при создании брони. Статус: {}, Тело: {}",
                            ex.getStatusCode(), ex.getResponseBodyAsString());

                    if (ex.getStatusCode().is4xxClientError()) {
                        return Mono.error(new SlotAlreadyBookedException(
                                "Выбранное время уже занято. Пожалуйста, выберите другой временной слот."
                        ));
                    }

                    return Mono.error(new ErpUnavailableException(
                            "Сервер бронирования временно перегружен. Попробуйте повторить запрос позже."
                    ));
                })
                .timeout(java.time.Duration.ofSeconds(4))
                .onErrorResume(java.util.concurrent.TimeoutException.class, ex -> {
                    log.error("Таймаут ожидания ответа от ERP системы.");
                    return Mono.error(new ErpUnavailableException(
                            "Время ожидания ответа истекло. Проверьте статус брони в личном кабинете."
                    ));
                });
    }

    public Mono<Void> cancelBookingByClient(Long bookingId) {
        return erpClient.cancelBooking(bookingId);
    }

    private Mono<List<TimeSlotDto>> fallbackGetSlots(String date, int durationHours, Throwable exception) {
        log.error("Сработал Fallback для получения слотов. Причина: {}", exception.getMessage());
        return Mono.just(Collections.emptyList());
    }
}
