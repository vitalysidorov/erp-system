package by.vs.bff.util;

import org.springframework.security.test.context.support.WithSecurityContext;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockJwtSecurityContextFactory.class)
public @interface WithMockJwtClient {
    String subject() default "client_12345";
    String role() default "CLIENT";
}
