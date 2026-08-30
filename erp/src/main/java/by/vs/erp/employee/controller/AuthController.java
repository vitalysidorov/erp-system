package by.vs.erp.employee.controller;

import by.vs.erp.employee.dto.JwtResponse;
import by.vs.erp.employee.dto.LoginRequest;
import by.vs.erp.employee.dto.RefreshRequest;
import by.vs.erp.employee.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("REST: Запрос на аутентификацию для email/phone: {}", request.getEmail());
        JwtResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        log.info("REST: Запрос на обновление пары JWT токенов");
        JwtResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }
}
