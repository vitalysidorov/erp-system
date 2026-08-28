package by.vs.bff.util;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import java.time.Instant;
import java.util.List;

public class WithMockJwtSecurityContextFactory implements WithSecurityContextFactory<WithMockJwtClient> {

    @Override
    public SecurityContext createSecurityContext(WithMockJwtClient annotation) {
        Jwt jwt = Jwt.withTokenValue("mock-bearer-token-value")
                .header("alg", "none")
                .subject(annotation.subject())
                .claim("role", annotation.role())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + annotation.role().toUpperCase());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                jwt,
                jwt.getTokenValue(),
                List.of(authority)
        );

        return new SecurityContextImpl(authentication);
    }
}
