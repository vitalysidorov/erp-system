package by.vs.erp.employee.controller;

import by.vs.erp.common.security.JwtProvider;
import by.vs.erp.employee.dto.JwtResponse;
import by.vs.erp.employee.dto.JwtResponseWithoutRefreshToken;
import by.vs.erp.employee.dto.LoginRequest;
import by.vs.erp.employee.dto.RefreshRequest;
import by.vs.erp.employee.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    @PostMapping("/login")
    public ResponseEntity<JwtResponseWithoutRefreshToken> login(@Valid @RequestBody LoginRequest request,
                                                                HttpServletResponse httpServletResponse) {
        log.info("REST: Запрос на аутентификацию для email/phone: {}", request.getEmail());
        JwtResponse jwtResponse = authService.login(request);

        Cookie refreshCookie = new Cookie("refreshToken", jwtResponse.getRefreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(3 * 24 * 60 * 60);

        httpServletResponse.addCookie(refreshCookie);

        return ResponseEntity.ok(new JwtResponseWithoutRefreshToken(jwtResponse.getAccessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponseWithoutRefreshToken> refresh(@CookieValue(name = "refreshToken") String refreshToken,
                                                                  HttpServletResponse httpServletResponse) {
        log.info("REST: Запрос на обновление пары JWT токенов");

        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        RefreshRequest request = new RefreshRequest(refreshToken);
        JwtResponse response = authService.refresh(request);

        Cookie refreshCookie = new Cookie("refreshToken", response.getRefreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(3 * 24 * 60 * 60);

        httpServletResponse.addCookie(refreshCookie);

        return ResponseEntity.ok(new JwtResponseWithoutRefreshToken(response.getAccessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse httpServletResponse) {
        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);

        httpServletResponse.addCookie(refreshCookie);

        return ResponseEntity.ok().build();
    }
}
