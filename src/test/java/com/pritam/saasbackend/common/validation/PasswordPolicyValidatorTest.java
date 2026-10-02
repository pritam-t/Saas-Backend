package com.pritam.saasbackend.common.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyValidatorTest {

    record Holder(@ValidPassword String password) {
    }

    // U+1F600: one character, 4 UTF-8 bytes, 2 Java chars (a surrogate pair).
    private static final String EMOJI = "😀";

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void acceptsMinimumLength() {
        assertThat(validator.validate(new Holder("a".repeat(10)))).isEmpty();
    }

    @Test
    void rejectsBelowMinimumLength() {
        assertThat(validator.validate(new Holder("a".repeat(9)))).hasSize(1);
    }

    @Test
    void acceptsExactly72Bytes() {
        assertThat(validator.validate(new Holder("a".repeat(72)))).isEmpty();
    }

    @Test
    void rejectsMoreThan72AsciiBytes() {
        assertThat(validator.validate(new Holder("a".repeat(73)))).hasSize(1);
    }

    @Test
    void rejectsMultiByteInputOver72BytesThoughUnder72Characters() {
        String password = "é".repeat(40);

        assertThat(password.length()).isLessThan(72);
        assertThat(password.getBytes(StandardCharsets.UTF_8)).hasSize(80);
        assertThat(validator.validate(new Holder(password))).hasSize(1);
    }

    @Test
    void acceptsEighteenFourByteCharactersAt72Bytes() {
        assertThat(validator.validate(new Holder(EMOJI.repeat(18)))).isEmpty();
        assertThat(validator.validate(new Holder(EMOJI.repeat(19)))).hasSize(1);
    }

    @Test
    void countsCharactersNotJavaCharsForTheMinimum() {
        // 9 characters, but 18 Java chars: still too short.
        assertThat(validator.validate(new Holder(EMOJI.repeat(9)))).hasSize(1);
    }

    @Test
    void treatsNullAsValidSoNotNullStaysSeparate() {
        assertThat(validator.validate(new Holder(null))).isEmpty();
    }
}
