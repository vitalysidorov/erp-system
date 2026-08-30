package by.vs.erp.employee.service;

import by.vs.erp.common.security.JwtProvider;
import by.vs.erp.common.security.TokenHasher;
import by.vs.erp.crm.repository.ClientRepository;
import by.vs.erp.employee.dto.JwtResponse;
import by.vs.erp.employee.dto.LoginRequest;
import by.vs.erp.employee.dto.RefreshRequest;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private TokenHasher tokenHasher;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Успешный вход: генерация токенов и сохранение хэша refresh токена в БД")
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@erp.by");
        request.setPassword("plainPassword");

        Employee employee = new Employee();
        ReflectionTestUtils.setField(employee, "id", 1L);
        employee.setEmail("user@erp.by");
        employee.setPassword("hashedPassword");
        employee.setRole("MANAGER");
        employee.setIsActive(true);

        when(employeeRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches(request.getPassword(), employee.getPassword())).thenReturn(true);
        when(jwtProvider.generateAccessToken("1", employee.getEmail(), employee.getRole())).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(employee.getEmail())).thenReturn("refresh-token");
        when(tokenHasher.hash("refresh-token")).thenReturn("hashed-refresh-token");

        JwtResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("hashed-refresh-token", employee.getRefreshToken()); // В сущность должен записаться хэш!
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
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Вход отклонен: аккаунт сотрудника был деактивирован (isActive = false)")
    void login_ThrowsBadCredentialsException_WhenEmployeeInactive() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@erp.by");
        request.setPassword("correctPassword");

        Employee employee = new Employee();
        employee.setPassword("hashedPassword");
        employee.setIsActive(false);

        when(employeeRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches(request.getPassword(), employee.getPassword())).thenReturn(true);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Обновление токенов: успешная ротация refresh токена")
    void refresh_Success() {
        String oldRefreshToken = "old-refresh-token";
        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken(oldRefreshToken);

        Employee employee = new Employee();
        ReflectionTestUtils.setField(employee, "id", 1L);
        employee.setEmail("user@erp.by");
        employee.setRole("MECHANIC");
        employee.setIsActive(true);
        employee.setRefreshToken("hashed-old-refresh-token");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user@erp.by");

        when(jwtProvider.validateRefreshToken(oldRefreshToken)).thenReturn(true);
        when(jwtProvider.getRefreshClaims(oldRefreshToken)).thenReturn(claims);
        when(tokenHasher.hash(oldRefreshToken)).thenReturn("hashed-old-refresh-token"); // Входящий хэш
        when(employeeRepository.findByEmail("user@erp.by")).thenReturn(Optional.of(employee));

        when(jwtProvider.generateAccessToken("1", employee.getEmail(), employee.getRole())).thenReturn("new-access");
        when(jwtProvider.generateRefreshToken("user@erp.by")).thenReturn("new-refresh");
        when(tokenHasher.hash("new-refresh")).thenReturn("hashed-new-refresh-token"); // Хэш нового токена

        JwtResponse response = authService.refresh(refreshRequest);

        assertNotNull(response);
        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        assertEquals("hashed-new-refresh-token", employee.getRefreshToken()); // Проверяем перезапись хэша в БД
        verify(employeeRepository, times(1)).save(employee);
    }

    @Test
    @DisplayName("Обновление токенов отклонено: передан невалидный или просроченный токен")
    void refresh_ThrowsBadCredentials_WhenTokenNotValid() {
        String invalidToken = "invalid-token";
        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken(invalidToken);

        when(jwtProvider.validateRefreshToken(invalidToken)).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.refresh(refreshRequest));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Обновление токенов отклонено: токен валиден, но уже был заменен/отозван в БД")
    void refresh_ThrowsBadCredentials_WhenTokenReusedOrRevoked() {
        String token = "stale-token";
        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken(token);

        Employee employee = new Employee();
        employee.setEmail("user@erp.by");
        employee.setIsActive(true);
        employee.setRefreshToken("already-new-hash-in-db"); // В БД токен уже обновился

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user@erp.by");

        when(jwtProvider.validateRefreshToken(token)).thenReturn(true);
        when(jwtProvider.getRefreshClaims(token)).thenReturn(claims);
        when(tokenHasher.hash(token)).thenReturn("hashed-stale-token"); // Хэш старого токена не совпадет с БД
        when(employeeRepository.findByEmail("user@erp.by")).thenReturn(Optional.of(employee));

        assertThrows(BadCredentialsException.class, () -> authService.refresh(refreshRequest));
        verify(employeeRepository, never()).save(any());
    }


    @Test
    @DisplayName("Успешный вход клиента: генерация токенов по номеру телефона и сохранение хэша в БД")
    void login_Client_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("+375291112233");
        request.setPassword("clientPassword");

        by.vs.erp.crm.entity.Client client = new by.vs.erp.crm.entity.Client();
        ReflectionTestUtils.setField(client, "id", 99L);
        client.setPhone("+375291112233");
        client.setPassword("hashedClientPassword");
        client.setRole("CLIENT");

        when(clientRepository.findByPhone(request.getEmail())).thenReturn(Optional.of(client));
        when(passwordEncoder.matches(request.getPassword(), client.getPassword())).thenReturn(true);
        when(jwtProvider.generateAccessToken("99", client.getPhone(), client.getRole())).thenReturn("client-access-token");
        when(jwtProvider.generateRefreshToken(client.getPhone())).thenReturn("client-refresh-token");
        when(tokenHasher.hash("client-refresh-token")).thenReturn("hashed-client-refresh-token");

        JwtResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("client-access-token", response.getAccessToken());
        assertEquals("client-refresh-token", response.getRefreshToken());
        assertEquals("hashed-client-refresh-token", client.getRefreshToken());
        verify(clientRepository, times(1)).save(client);
        verify(employeeRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Вход клиента отклонен: клиент с таким телефоном отсутствует в системе")
    void login_Client_ThrowsUsernameNotFoundException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("88005553535");
        request.setPassword("anyPassword");

        when(clientRepository.findByPhone(anyString())).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authService.login(request));
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Обновление токенов клиента: успешная ротация refresh токена по номеру телефона")
    void refresh_Client_Success() {
        String oldRefreshToken = "client-old-refresh";
        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken(oldRefreshToken);

        by.vs.erp.crm.entity.Client client = new by.vs.erp.crm.entity.Client();
        ReflectionTestUtils.setField(client, "id", 99L);
        client.setPhone("+375291112233");
        client.setRole("CLIENT");
        client.setRefreshToken("hashed-client-old-refresh");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("+375291112233");

        when(jwtProvider.validateRefreshToken(oldRefreshToken)).thenReturn(true);
        when(jwtProvider.getRefreshClaims(oldRefreshToken)).thenReturn(claims);
        when(tokenHasher.hash(oldRefreshToken)).thenReturn("hashed-client-old-refresh");

        when(employeeRepository.findByEmail("+375291112233")).thenReturn(Optional.empty());

        when(clientRepository.findByPhone("+375291112233")).thenReturn(Optional.of(client));
        when(jwtProvider.generateAccessToken("99", client.getPhone(), client.getRole())).thenReturn("client-new-access");
        when(jwtProvider.generateRefreshToken("+375291112233")).thenReturn("client-new-refresh");
        when(tokenHasher.hash("client-new-refresh")).thenReturn("hashed-client-new-refresh");

        JwtResponse response = authService.refresh(refreshRequest);

        assertNotNull(response);
        assertEquals("client-new-access", response.getAccessToken());
        assertEquals("client-new-refresh", response.getRefreshToken());
        assertEquals("hashed-client-new-refresh", client.getRefreshToken());
        verify(clientRepository, times(1)).save(client);
    }

    @Test
    @DisplayName("Обновление токенов клиента отклонено: токен валиден, но уже был заменен/отозван в БД")
    void refresh_Client_ThrowsBadCredentials_WhenTokenRevoked() {
        String token = "client-stale-token";
        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken(token);

        by.vs.erp.crm.entity.Client client = new by.vs.erp.crm.entity.Client();
        client.setPhone("+375291112233");
        client.setRefreshToken("already-changed-in-db-hash");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("+375291112233");

        when(jwtProvider.validateRefreshToken(token)).thenReturn(true);
        when(jwtProvider.getRefreshClaims(token)).thenReturn(claims);
        when(tokenHasher.hash(token)).thenReturn("hashed-stale-token");

        when(employeeRepository.findByEmail("+375291112233")).thenReturn(Optional.empty());
        when(clientRepository.findByPhone("+375291112233")).thenReturn(Optional.of(client));

        assertThrows(BadCredentialsException.class, () -> authService.refresh(refreshRequest));
        verify(clientRepository, never()).save(any());
    }
}
