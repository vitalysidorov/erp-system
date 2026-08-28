package by.vs.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Value("${app.security.jwt.secret-key}")
    private String jwtSecretKey;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html", "/webjars/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()

                        .pathMatchers("/api/v1/b2c/auth/login", "/api/v1/b2c/auth/refresh").permitAll()
                        .pathMatchers("/api/v1/b2c/clients/register").permitAll()

                        .pathMatchers("/api/v1/client/**").authenticated()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        byte[] keyBytes = jwtSecretKey.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
    }
}
