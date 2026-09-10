package by.vs.bff.service;

import by.vs.bff.client.ErpClient;
import by.vs.bff.dto.ClientBookingRequest;
import by.vs.bff.dto.ClientBookingResponse;
import by.vs.bff.dto.ErpBookingRequest;
import by.vs.bff.dto.TimeSlotDto;
import by.vs.bff.exception.SlotAlreadyBookedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveListOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BffBookingServiceTest {

    private ErpClient erpClient;
    private ReactiveRedisTemplate<String, TimeSlotDto> redisTemplate;
    private ReactiveListOperations<String, TimeSlotDto> listOperations;
    private BffBookingService bffBookingService;

    @BeforeEach
    void setUp() {
        erpClient = mock(ErpClient.class);
        redisTemplate = mock(ReactiveRedisTemplate.class);
        listOperations = mock(ReactiveListOperations.class);

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        bffBookingService = new BffBookingService(erpClient, redisTemplate);
    }

    @Test
    void getSlotsForClient_CacheHit_ReturnsCachedData() {
        String dateStr = "2026-08-23";
        int durationHours = 2;
        String cacheKey = "slots::" + dateStr + "::" + durationHours;

        TimeSlotDto cachedSlot = new TimeSlotDto();
        cachedSlot.setSlotStart(LocalDateTime.now());
        cachedSlot.setSlotEnd(LocalDateTime.now().plusHours(2));

        // Имитируем попадание в кэш Redis (Cache HIT)
        when(listOperations.range(cacheKey, 0, -1)).thenReturn(Flux.just(cachedSlot));

        Mono<List<TimeSlotDto>> resultMono = bffBookingService.getSlotsForClient(dateStr, durationHours);

        StepVerifier.create(resultMono)
                .expectNextMatches(slots -> slots.size() == 1 && slots.get(0).equals(cachedSlot))
                .verifyComplete();

        // Проверяем, что до ERP-клиента запрос не дошел
        verifyNoInteractions(erpClient);
    }

    @Test
    void getSlotsForClient_CacheMiss_FetchesFromErpAndCaches() {
        String dateStr = "2026-09-01";
        int durationHours = 2;
        String cacheKey = "slots::" + dateStr + "::" + durationHours;

        TimeSlotDto erpSlot = new TimeSlotDto();
        erpSlot.setSlotStart(LocalDateTime.of(2026, 9, 1, 10, 0));
        erpSlot.setSlotEnd(LocalDateTime.of(2026, 9, 1, 12, 0));

        // Имитируем промах кэша (Cache MISS)
        when(listOperations.range(cacheKey, 0, -1)).thenReturn(Flux.empty());

        // Мокаем успешный ответ от ERP системы
        when(erpClient.getAvailableSlots(any(LocalDateTime.class), eq(durationHours)))
                .thenReturn(Mono.just(List.of(erpSlot)));

        // Мокаем асинхронную запись структуры в Redis после запроса к ERP
        when(listOperations.rightPushAll(eq(cacheKey), any(Collection.class))).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(eq(cacheKey), any(Duration.class))).thenReturn(Mono.just(true));

        Mono<List<TimeSlotDto>> resultMono = bffBookingService.getSlotsForClient(dateStr, durationHours);

        StepVerifier.create(resultMono)
                .expectNextMatches(slots -> slots.size() == 1 && slots.get(0).getSlotStart().getHour() == 10)
                .verifyComplete();

        verify(erpClient, times(1)).getAvailableSlots(any(LocalDateTime.class), eq(durationHours));
        verify(listOperations, times(1)).rightPushAll(eq(cacheKey), any(Collection.class));
    }

    @Test
    void createBookingForClient_Success() {
        String clientId = "client_123";
        ClientBookingRequest request = new ClientBookingRequest(1L, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        ClientBookingResponse erpResponse = new ClientBookingResponse(100L, 1L, request.startTime(), request.endTime(), "CONFIRMED");

        when(erpClient.createBooking(any(ErpBookingRequest.class))).thenReturn(Mono.just(erpResponse));

        Mono<ClientBookingResponse> resultMono = bffBookingService.createBookingForClient(clientId, request);

        StepVerifier.create(resultMono)
                .expectNextMatches(response -> response.bookingId().equals(100L) && "CONFIRMED".equals(response.status()))
                .verifyComplete();
    }

    @Test
    void createBookingForClient_SlotAlreadyBooked_ThrowsCustomException() {
        String clientId = "client_123";
        ClientBookingRequest request = new ClientBookingRequest(1L, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        WebClientResponseException conflictException = WebClientResponseException.create(
                HttpStatus.CONFLICT.value(), "Conflict", null, null, null);

        when(erpClient.createBooking(any(ErpBookingRequest.class))).thenReturn(Mono.error(conflictException));

        Mono<ClientBookingResponse> resultMono = bffBookingService.createBookingForClient(clientId, request);

        StepVerifier.create(resultMono)
                .expectError(SlotAlreadyBookedException.class)
                .verify();
    }
}
