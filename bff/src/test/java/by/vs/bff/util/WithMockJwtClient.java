package by.vs.bff.util;

import org.springframework.security.test.context.support.WithSecurityContext;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockJwtSecurityContextFactory.class)
public @interface WithMockJwtClient {

    String userId() default "user-12345";

    String username() default "test_user";

    String role() default "CLIENT";
}
