package by.vs.bff.controller;

import by.vs.bff.client.ErpClient;
import by.vs.bff.dto.JwtResponseWithoutRefreshToken;
import by.vs.bff.dto.LoginRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/b2c")
@RequiredArgsConstructor
public class BffAuthController {

    private final ErpClient erpClient;

    @PostMapping("/auth/login")
    public Mono<ResponseEntity<JwtResponseWithoutRefreshToken>> login(@Valid @RequestBody LoginRequest request) {
        return erpClient.loginInErp(request);
    }

    @PostMapping("/auth/refresh")
    public Mono<ResponseEntity<JwtResponseWithoutRefreshToken>> refresh(@CookieValue(name = "refreshToken") String refreshToken) {
        return erpClient.refreshInErp(refreshToken);
    }

    @PostMapping("/auth/logout")
    public Mono<ResponseEntity<Void>> logout() {
        return erpClient.logout();
    }

    @PostMapping("/clients/register")
    public Mono<ResponseEntity<Object>> registerClient(@RequestBody Object clientRegisterRequest) {
        return erpClient.registerClientInErp(clientRegisterRequest)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }
}
