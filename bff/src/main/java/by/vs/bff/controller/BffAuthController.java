package by.vs.bff.controller;

import by.vs.bff.client.ErpClient;
import by.vs.erp.employee.dto.JwtResponse;
import by.vs.erp.employee.dto.LoginRequest;
import by.vs.erp.employee.dto.RefreshRequest;
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
    public Mono<ResponseEntity<JwtResponse>> login(@Valid @RequestBody LoginRequest request) {
        return erpClient.loginInErp(request)
                .map(jwtResponse -> ResponseEntity.ok(jwtResponse));
    }

    @PostMapping("/auth/refresh")
    public Mono<ResponseEntity<JwtResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return erpClient.refreshInErp(request)
                .map(jwtResponse -> ResponseEntity.ok(jwtResponse));
    }

    @PostMapping("/clients/register")
    public Mono<ResponseEntity<Object>> registerClient(@RequestBody Object clientRegisterRequest) {
        return erpClient.registerClientInErp(clientRegisterRequest)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }
}
