package by.vs.erp.common.config;

import by.vs.erp.crm.repository.ClientRepository;
import by.vs.erp.employee.repository.EmployeeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

@Configuration
public class AuthorizationServerConfig {

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(
            EmployeeRepository employeeRepository,
            ClientRepository clientRepository) {
        return context -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                String username = context.getPrincipal().getName();

                // Ищем ID в зависимости от того, кто логинится
                String userId = employeeRepository.findByEmail(username)
                        .map(emp -> emp.getId().toString())
                        .orElseGet(() -> clientRepository.findByEmail(username)
                                .map(cl -> cl.getId().toString())
                                .orElse("UNKNOWN"));

                // Добавляем claims в JWT, как было в вашем JwtFilter
                context.getClaims()
                        .claim("userId", userId)
                        .claim("role", context.getPrincipal().getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""));
            }
        };
    }
}

