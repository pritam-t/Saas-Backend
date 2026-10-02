package com.pritam.saasbackend.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tenant slug (spec 4.1): {@code ^[a-z][a-z0-9-]{2,40}$} and not reserved.
 * {@code null} is valid; combine with {@code @NotNull} when required.
 */
@Documented
@Constraint(validatedBy = SlugValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSlug {

    String message() default "must be 3-41 characters: a lowercase letter, then lowercase letters, digits or hyphens, and not a reserved name";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
