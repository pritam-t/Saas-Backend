package com.pritam.saasbackend.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class PasswordPolicyValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_CHARACTERS = 10;

    /** BCrypt only uses the first 72 bytes; longer input would be silently truncated. */
    private static final int MAX_UTF8_BYTES = 72;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        // Code points, not Java chars: an emoji is one character but two chars.
        return value.codePointCount(0, value.length()) >= MIN_CHARACTERS
                && value.getBytes(StandardCharsets.UTF_8).length <= MAX_UTF8_BYTES;
    }
}
