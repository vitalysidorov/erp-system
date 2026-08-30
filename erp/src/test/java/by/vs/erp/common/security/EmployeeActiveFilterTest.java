package by.vs.erp.common.security;

import by.vs.erp.employee.repository.EmployeeRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class EmployeeActiveFilterTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private EmployeeActiveFilter employeeActiveFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(String email, String role) {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject(email)
                .claim("role", role)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                jwt, jwt.getTokenValue(), List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("Уволенный сотрудник (isActive=false) с ещё действующим токеном получает 403 и не доходит до контроллера")
    void shouldBlockDismissedEmployeeWithStillValidToken() throws Exception {
        setAuthentication("dismissed@erp.by", "EMPLOYEE");
        when(employeeRepository.findIsActiveByEmail("dismissed@erp.by")).thenReturn(Optional.of(false));

        StringWriter responseBody = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        employeeActiveFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json; charset=UTF-8");
        assertTrue(responseBody.toString().contains("деактивирован"));
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("Активный сотрудник с валидным токеном свободно проходит к следующему звену цепочки")
    void shouldAllowActiveEmployeeThrough() throws Exception {
        setAuthentication("active@erp.by", "EMPLOYEE");
        when(employeeRepository.findIsActiveByEmail("active@erp.by")).thenReturn(Optional.of(true));

        employeeActiveFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("Роль ADMIN тоже проверяется на активность (не только EMPLOYEE)")
    void shouldCheckActiveFlagForAdminRoleToo() throws Exception {
        setAuthentication("ex-admin@erp.by", "ADMIN");
        when(employeeRepository.findIsActiveByEmail("ex-admin@erp.by")).thenReturn(Optional.of(false));

        StringWriter responseBody = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        employeeActiveFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("Роль CLIENT не проверяется этим фильтром — проходит без обращения к EmployeeRepository")
    void shouldSkipCheckForClientRole() throws Exception {
        setAuthentication("+375291112233", "CLIENT");

        employeeActiveFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(employeeRepository);
    }

    @Test
    @DisplayName("Отсутствующая запись сотрудника (findIsActiveByEmail пуст) трактуется как неактивный — доступ блокируется")
    void shouldTreatMissingEmployeeAsInactive() throws Exception {
        setAuthentication("ghost@erp.by", "EMPLOYEE");
        when(employeeRepository.findIsActiveByEmail("ghost@erp.by")).thenReturn(Optional.empty());

        StringWriter responseBody = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        employeeActiveFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("Без аутентификации в контексте фильтр не падает и пропускает запрос дальше")
    void shouldPassThroughWhenNoAuthenticationPresent() throws Exception {
        SecurityContextHolder.clearContext();

        employeeActiveFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(employeeRepository);
    }
}