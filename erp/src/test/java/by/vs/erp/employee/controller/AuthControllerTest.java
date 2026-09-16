package by.vs.erp.employee.controller;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.common.security.JwtProvider;
import by.vs.erp.employee.dto.JwtResponse;
import by.vs.erp.employee.dto.LoginRequest;
import by.vs.erp.employee.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("POST /auth/login: Успешный вход")
    void shouldLoginAndReturnJwt() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@erp.by");
        loginRequest.setPassword("adminPassword");

        JwtResponse jwtResponse = new JwtResponse("access-token-string", "refresh-token-string");

        Mockito.when(authService.login(any(LoginRequest.class))).thenReturn(jwtResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-string"))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    @DisplayName("POST /auth/refresh: Успешный рефреш")
    void shouldRefreshTokens() throws Exception {
        String oldRefreshToken = "old-refresh-token";
        Cookie cookie = new Cookie("refreshToken", oldRefreshToken);

        Mockito.when(jwtProvider.validateRefreshToken(oldRefreshToken)).thenReturn(true);

        JwtResponse newJwtResponse = new JwtResponse("new-access-token", "new-refresh-token");
        Mockito.when(authService.refresh(any())).thenReturn(newJwtResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(cookie().value("refreshToken", "new-refresh-token"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

}
