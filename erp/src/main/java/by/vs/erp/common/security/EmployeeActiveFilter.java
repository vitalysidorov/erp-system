package by.vs.erp.common.security;

import by.vs.erp.employee.repository.EmployeeRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class EmployeeActiveFilter extends OncePerRequestFilter {

    private final EmployeeRepository employeeRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String role = jwt.getClaimAsString("role");
            String email = jwt.getSubject();

            if (role != null && (role.equalsIgnoreCase("EMPLOYEE") || role.equalsIgnoreCase("ADMIN"))) {
                boolean isActive = employeeRepository.findIsActiveByEmail(email).orElse(false);

                if (!isActive) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json; charset=UTF-8");
                    response.getWriter().write("{\"error\": \"Аккаунт сотрудника деактивирован. Доступ заблокирован.\"}");
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
