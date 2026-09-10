package by.vs.bff.util;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.time.Instant;
import java.util.List;

public class WithMockJwtSecurityContextFactory implements WithSecurityContextFactory<WithMockJwtClient> {

    @Override
    public SecurityContext createSecurityContext(WithMockJwtClient annotation) {
        Jwt jwt = Jwt.withTokenValue("mock-bearer-token-value")
                .header("alg", "none")
                .subject(annotation.username())
                .claim("userId", annotation.userId())
                .claim("role", annotation.role())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + annotation.role().toUpperCase());

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt,
                List.of(authority),
                annotation.username()
        );

        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(authentication);
        return context;
    }
}
