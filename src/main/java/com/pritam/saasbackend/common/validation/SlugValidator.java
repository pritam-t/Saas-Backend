package com.pritam.saasbackend.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;
import java.util.regex.Pattern;

public class SlugValidator implements ConstraintValidator<ValidSlug, String> {

    private static final Pattern SLUG = Pattern.compile("^[a-z][a-z0-9-]{2,40}$");

    /** Names that would clash with routes or read as official (spec 4.1: "reserved slugs rejected"). */
    private static final Set<String> RESERVED = Set.of(
            "admin", "api", "auth", "platform", "public", "system", "www");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return SLUG.matcher(value).matches() && !RESERVED.contains(value);
    }
}
