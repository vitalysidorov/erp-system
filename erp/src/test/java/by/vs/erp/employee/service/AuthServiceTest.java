package by.vs.erp.employee.service;

import by.vs.erp.common.security.JwtProvider;
import by.vs.erp.employee.dto.JwtResponse;
import by.vs.erp.employee.dto.LoginRequest;
import by.vs.erp.employee.entity.Employee;
import by.vs.erp.employee.repository.EmployeeRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Успешный вход: генерация токенов и сохранение refresh токена в БД")
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@erp.by");
        request.setPassword("plainPassword");

        Employee employee = new Employee();
        employee.setEmail("user@erp.by");
        employee.setPassword("hashedPassword");
        employee.setRole("MANAGER");
        employee.setIsActive(true);

        when(employeeRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches(request.getPassword(), employee.getPassword())).thenReturn(true);
        when(jwtProvider.generateAccessToken(employee.getEmail(), employee.getRole())).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(employee.getEmail())).thenReturn("refresh-token");

        JwtResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("refresh-token", employee.getRefreshToken()); // Проверяем, что токен записался в сущность
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    @DisplayName("Вход отклонен: сотрудник с таким email отсутствует в системе")
    void login_ThrowsUsernameNotFoundException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("absent@erp.by");

        when(employeeRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authService.login(request));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Вход отклонен: введен некорректный пароль")
    void login_ThrowsBadCredentialsException_WhenPasswordInvalid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@erp.by");
        request.setPassword("wrongPassword");

        Employee employee = new Employee();
        employee.setPassword("hashedPassword");

        when(employeeRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches(request.getPassword(), employee.getPassword())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Вход отклонен: аккаунт сотрудника был деактивирован (isActive = false)")
    void login_ThrowsBadCredentialsException_WhenEmployeeInactive() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@erp.by");
        request.setPassword("correctPassword");

        Employee employee = new Employee();
        employee.setPassword("hashedPassword");
        employee.setIsActive(false); // Деактивирован

        when(employeeRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches(request.getPassword(), employee.getPassword())).thenReturn(true);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Обновление токенов: успешная ротация refresh токена")
    void refresh_Success() {
        String oldRefreshToken = "old-refresh-token";
        Employee employee = new Employee();
        employee.setEmail("user@erp.by");
        employee.setRole("MECHANIC");
        employee.setRefreshToken(oldRefreshToken);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user@erp.by");

        when(jwtProvider.validateRefreshToken(oldRefreshToken)).thenReturn(true);
        when(jwtProvider.getRefreshClaims(oldRefreshToken)).thenReturn(claims);
        when(employeeRepository.findByEmail("user@erp.by")).thenReturn(Optional.of(employee));
        when(jwtProvider.generateAccessToken("user@erp.by", "MECHANIC")).thenReturn("new-access");
        when(jwtProvider.generateRefreshToken("user@erp.by")).thenReturn("new-refresh");

        JwtResponse response = authService.refresh(oldRefreshToken);

        assertNotNull(response);
        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        assertEquals("new-refresh", employee.getRefreshToken()); // Токен обновился
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    @DisplayName("Обновление токенов отклонено: передан невалидный или просроченный токен")
    void refresh_ThrowsBadCredentials_WhenTokenNotValid() {
        String invalidToken = "invalid-token";
        when(jwtProvider.validateRefreshToken(invalidToken)).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.refresh(invalidToken));
    }

    @Test
    @DisplayName("Обновление токенов отклонено: токен валиден, но уже был заменен/отозван в БД")
    void refresh_ThrowsBadCredentials_WhenTokenReusedOrRevoked() {
        String token = "stale-token";
        Employee employee = new Employee();
        employee.setRefreshToken("already-new-token-in-db"); // В базе уже лежит другой токен

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user@erp.by");

        when(jwtProvider.validateRefreshToken(token)).thenReturn(true);
        when(jwtProvider.getRefreshClaims(token)).thenReturn(claims);
        when(employeeRepository.findByEmail("user@erp.by")).thenReturn(Optional.of(employee));

        assertThrows(BadCredentialsException.class, () -> authService.refresh(token));
    }
}
