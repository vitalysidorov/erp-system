package by.vs.bff.controller;

import by.vs.bff.client.ErpClient;
import by.vs.bff.config.SecurityConfig;
import by.vs.bff.dto.*;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(BffClientController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/mock-jwks"
})
class BffClientControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private BffBookingService bffService;

    @MockBean
    private ErpClient erpClient;

    @MockBean
    private ReactiveRedisTemplate<String, TimeSlotDto> reactiveRedisTemplate;

    @MockBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @Test
    @WithMockJwtClient(subject = "user_abc", role = "CLIENT")
    void createBooking_AuthenticatedClient_Returns200() {
        ClientBookingRequest request = new ClientBookingRequest(
                55L, LocalDateTime.of(2026, 8, 23, 12, 0), LocalDateTime.of(2026, 8, 23, 14, 0)
        );
        ClientBookingResponse response = new ClientBookingResponse(
                777L, 55L, request.startTime(), request.endTime(), "CONFIRMED"
        );

        when(bffService.createBookingForClient(eq("user_abc"), any(ClientBookingRequest.class)))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/v1/client/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.bookingId").isEqualTo(777L)
                .jsonPath("$.status").isEqualTo("CONFIRMED");
    }

    @Test
    void createBooking_Unauthenticated_Returns401() {
        ClientBookingRequest request = new ClientBookingRequest(
                55L, LocalDateTime.now(), LocalDateTime.now().plusHours(1)
        );

        webTestClient.post()
                .uri("/api/v1/client/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
