package by.vs.bff.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;
import java.util.List;

public class ReactiveJwtAuthenticationConverter implements Converter<Jwt, Mono<UsernamePasswordAuthenticationToken>> {

    @Override
    public Mono<UsernamePasswordAuthenticationToken> convert(Jwt jwt) {
        String role = jwt.getClaimAsString("role");
        if (role == null) {
            role = "USER";
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.toUpperCase());

        // в качестве credentials передаем текст токена.
        String tokenValue = jwt.getTokenValue();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                jwt,
                tokenValue,
                List.of(authority)
        );

        return Mono.just(auth);
    }
}
