package by.vs.bff.service;

import by.vs.bff.client.ErpClient;
import by.vs.bff.dto.*;
import by.vs.bff.exception.SlotAlreadyBookedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ReactiveListOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class BffBookingServiceTest {

    private ErpClient erpClient;
    private BffBookingService bffBookingService;

    private ReactiveRedisTemplate<String, TimeSlotDto> redisTemplate;
    private ReactiveListOperations<String, TimeSlotDto> listOperations;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        erpClient = Mockito.mock(ErpClient.class);

        redisTemplate = Mockito.mock(ReactiveRedisTemplate.class);
        listOperations = Mockito.mock(ReactiveListOperations.class);

        when(redisTemplate.opsForList()).thenReturn(listOperations);

        bffBookingService = new BffBookingService(erpClient, redisTemplate);
    }

    @Test
    void getSlotsForClient_CacheHit_ReturnsCachedData() {
        String date = "2026-08-23";
        int durationHours = 2;
        TimeSlotDto cachedSlot = new TimeSlotDto(LocalDateTime.now(), LocalDateTime.now().plusHours(2));

        when(listOperations.range(anyString(), eq(0L), eq(-1L))).thenReturn(Flux.just(cachedSlot));

        Mono<List<TimeSlotDto>> resultMono = bffBookingService.getSlotsForClient(date, durationHours);

        StepVerifier.create(resultMono)
                .expectNextMatches(slots -> slots.size() == 1 && slots.get(0).getSlotStart().equals(cachedSlot.getSlotStart()))
                .verifyComplete();

        Mockito.verifyNoInteractions(erpClient);
    }

    @Test
    void getSlotsForClient_CacheMiss_FetchesFromErpAndCaches() {
        String date = "2026-08-23";
        int durationHours = 2;
        TimeSlotDto erpSlot = new TimeSlotDto(LocalDateTime.now(), LocalDateTime.now().plusHours(2));

        when(listOperations.range(anyString(), eq(0L), eq(-1L))).thenReturn(Flux.empty());
        when(erpClient.getAvailableSlots(date, durationHours)).thenReturn(Mono.just(List.of(erpSlot)));
        when(listOperations.rightPushAll(anyString(), any(List.class))).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        Mono<List<TimeSlotDto>> resultMono = bffBookingService.getSlotsForClient(date, durationHours);

        StepVerifier.create(resultMono)
                .expectNextMatches(slots -> slots.size() == 1)
                .verifyComplete();

        Mockito.verify(listOperations).rightPushAll(anyString(), any(List.class));
        Mockito.verify(redisTemplate).expire(anyString(), any(Duration.class));
    }

    @Test
    void createBookingForClient_Success() {
        ClientBookingRequest request = new ClientBookingRequest(
                1L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2)
        );
        ClientBookingResponse erpResponse = new ClientBookingResponse(
                100L, 1L, request.startTime(), request.endTime(), "CONFIRMED"
        );

        when(erpClient.createBooking(any(ErpBookingRequest.class))).thenReturn(Mono.just(erpResponse));
        when(redisTemplate.scan()).thenReturn(Flux.empty());

        Mono<ClientBookingResponse> resultMono = bffBookingService.createBookingForClient("client_123", request);

        StepVerifier.create(resultMono)
                .expectNextMatches(response -> response.bookingId().equals(100L))
                .verifyComplete();
    }

    @Test
    void createBookingForClient_SlotAlreadyBooked_ThrowsCustomException() {
        ClientBookingRequest request = new ClientBookingRequest(
                1L, LocalDateTime.now(), LocalDateTime.now().plusHours(2)
        );

        WebClientResponseException conflictException = WebClientResponseException.create(
                409, "Conflict", null, null, null
        );

        when(erpClient.createBooking(any(ErpBookingRequest.class))).thenReturn(Mono.error(conflictException));

        Mono<ClientBookingResponse> resultMono = bffBookingService.createBookingForClient("client_123", request);

        StepVerifier.create(resultMono)
                .expectError(SlotAlreadyBookedException.class)
                .verify();
    }
}
