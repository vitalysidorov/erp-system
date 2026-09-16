package by.vs.bff.controller;

import by.vs.bff.client.ErpClient;
import by.vs.bff.config.SecurityConfig;
import by.vs.bff.dto.JwtResponseWithoutRefreshToken;
import by.vs.bff.dto.LoginRequest;
import by.vs.bff.dto.TimeSlotDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
        JwtResponseWithoutRefreshToken erpResponse = new JwtResponseWithoutRefreshToken("access-token");
        String mockCookieHeader = "refreshToken=new-refresh-token-value; Path=/; HttpOnly; Secure; Max-Age=259200";

        when(erpClient.loginInErp(any(LoginRequest.class))).thenReturn(Mono.just(
                ResponseEntity.ok()
                        .header("Set-Cookie", mockCookieHeader)
                        .body(erpResponse)
        ));

        webTestClient.post()
                .uri("/api/v1/b2c/auth/login")
                .cookie("refreshToken", "old-refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isEqualTo("access-token");
    }

    @Test
    void login_InvalidRequest_Returns400BadRequest() {
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
        var erpResponse = new JwtResponseWithoutRefreshToken("new-access-token");

        String mockCookieHeader = "refreshToken=new-refresh-token-value; Path=/; HttpOnly; Secure; Max-Age=259200";

        when(erpClient.refreshInErp(any())).thenReturn(Mono.just(
                ResponseEntity.ok()
                        .header("Set-Cookie", mockCookieHeader)
                        .body(erpResponse)
        ));

        webTestClient.post()
                .uri("/api/v1/b2c/auth/refresh")
                .cookie("refreshToken", "old-refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
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
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.status").isEqualTo("REGISTERED");
    }
}
