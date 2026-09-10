package by.vs.bff.controller;

import by.vs.bff.client.ErpClient;
import by.vs.bff.config.SecurityConfig;
import by.vs.bff.dto.JwtResponse;
import by.vs.bff.dto.LoginRequest;
import by.vs.bff.dto.RefreshRequest;
import by.vs.bff.dto.TimeSlotDto;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(BffAuthController.class)
@Import(SecurityConfig.class)
class BffAuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ErpClient erpClient;

    @MockBean
    private ReactiveRedisTemplate<String, TimeSlotDto> reactiveRedisTemplate;

    @MockBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @Test
    void login_ValidRequest_Returns200AndTokens() {
        LoginRequest request = new LoginRequest("test@mail.com", "password123");
        JwtResponse erpResponse = new JwtResponse("access-token", "refresh-token");

        when(erpClient.loginInErp(any(LoginRequest.class))).thenReturn(Mono.just(erpResponse));

        webTestClient.post()
                .uri("/api/v1/b2c/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isEqualTo("access-token")
                .jsonPath("$.refreshToken").isEqualTo("refresh-token");
    }

    @Test
    void login_InvalidRequest_Returns400BadRequest() {
        // Ошибка валидации: пустой email и пароль
        LoginRequest request = new LoginRequest("", "");

        webTestClient.post()
                .uri("/api/v1/b2c/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void refresh_ValidRequest_Returns200() {
        RefreshRequest request = new RefreshRequest("refresh-token-value");
        JwtResponse erpResponse = new JwtResponse("new-access-token", "new-refresh-token");

        when(erpClient.refreshInErp(any(RefreshRequest.class))).thenReturn(Mono.just(erpResponse));

        webTestClient.post()
                .uri("/api/v1/b2c/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isEqualTo("new-access-token");
    }

    @Test
    void registerClient_Success_Returns201Created() {
        Map<String, String> registerRequest = Map.of(
                "name", "Ivan",
                "phone", "+375291112233"
        );
        Map<String, Object> erpResponse = Map.of("id", 123, "status", "REGISTERED");

        when(erpClient.registerClientInErp(any())).thenReturn(Mono.just(erpResponse));

        webTestClient.post()
                .uri("/api/v1/b2c/clients/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isCreated() // Проверка ResponseEntity.status(HttpStatus.CREATED)
                .expectBody()
                .jsonPath("$.status").isEqualTo("REGISTERED");
    }
}
