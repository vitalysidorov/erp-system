package by.vs.bff.controller;

import by.vs.bff.client.ErpClient;
import by.vs.bff.config.SecurityConfig;
import by.vs.bff.dto.ClientBookingRequest;
import by.vs.bff.dto.ClientBookingResponse;
import by.vs.bff.dto.TimeSlotDto;
import by.vs.bff.service.BffBookingService;
import by.vs.bff.util.WithMockJwtClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebFluxTest(BffClientController.class)
@Import(SecurityConfig.class)
class BffClientControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private BffBookingService bffBookingService;

    @MockBean
    private ErpClient erpClient;

    @MockBean
    private ReactiveRedisTemplate<String, TimeSlotDto> reactiveRedisTemplate;

    @MockBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @Test
    @WithMockJwtClient(userId = "user_abc", username = "test_user", role = "CLIENT")
    void createBooking_AuthenticatedClient_Returns200() {
        LocalDateTime start = LocalDateTime.now().withNano(0);
        LocalDateTime end = start.plusHours(1);

        ClientBookingRequest request = new ClientBookingRequest(1L, start, end);
        ClientBookingResponse response = new ClientBookingResponse(77L, 1L, start, end, "CONFIRMED");

        when(bffBookingService.createBookingForClient(anyString(), any(ClientBookingRequest.class)))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/v1/client/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.bookingId").isEqualTo(77)
                .jsonPath("$.status").isEqualTo("CONFIRMED");
    }

    @Test
    @WithMockJwtClient(role = "CLIENT")
    void getSlots_AuthenticatedClient_Returns200AndSlots() {
        TimeSlotDto slot = new TimeSlotDto();
        slot.setSlotStart(LocalDateTime.now().withNano(0));
        slot.setSlotEnd(LocalDateTime.now().plusHours(1).withNano(0));

        when(bffBookingService.getSlotsForClient(anyString(), anyInt()))
                .thenReturn(Mono.just(List.of(slot)));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/client/bookings/slots")
                        .queryParam("date", "2026-09-15")
                        .queryParam("durationHours", 2)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0]").exists();
    }

    @Test
    @WithMockJwtClient(role = "CLIENT")
    void cancelBooking_AuthenticatedClient_Returns200() {
        when(bffBookingService.cancelBookingByClient(anyLong()))
                .thenReturn(Mono.empty());

        webTestClient.patch()
                .uri("/api/v1/client/bookings/100/cancel")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void createBooking_Unauthenticated_Returns401() {
        ClientBookingRequest request = new ClientBookingRequest(1L, LocalDateTime.now(), LocalDateTime.now().plusHours(1));

        webTestClient.post()
                .uri("/api/v1/client/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
