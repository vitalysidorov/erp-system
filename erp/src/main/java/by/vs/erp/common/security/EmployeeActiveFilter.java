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

        // Если Spring Security уже успешно распарсил JWT и сохранил его в контексте
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String role = jwt.getClaimAsString("role");
            String email = jwt.getSubject(); // Email сотрудника, который зашит в subject токена

            // Проверяем флаг активности ТОЛЬКО для работников и администраторов
            if (role != null && (role.equalsIgnoreCase("EMPLOYEE") || role.equalsIgnoreCase("ADMIN"))) {
                boolean isActive = employeeRepository.findIsActiveByEmail(email).orElse(false);

                if (!isActive) {
                    // Если сотрудник деактивирован (уволен), принудительно возвращаем 403 JSON
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json; charset=UTF-8");
                    response.getWriter().write("{\"error\": \"Аккаунт сотрудника деактивирован. Доступ заблокирован.\"}");
                    return; // Прерываем выполнение запроса, до контроллера он не дойдет
                }
            }
        }

        // Если это клиент или активный сотрудник — пропускаем запрос дальше к контроллеру
        filterChain.doFilter(request, response);
    }
}
