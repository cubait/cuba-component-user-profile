package it.nexbit.cuba.security.userprofile.restapi.data;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target({ ANNOTATION_TYPE })
@Retention(RUNTIME)
public @interface NavigableConstraint {
}
