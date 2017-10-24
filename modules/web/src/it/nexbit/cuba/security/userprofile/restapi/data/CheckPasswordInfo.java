package it.nexbit.cuba.security.userprofile.restapi.data;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target({ TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = CheckPasswordInfoValidator.class)
@NavigableConstraint
public @interface CheckPasswordInfo {
    /**
     * Not used.
     * @return empty string
     */
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
