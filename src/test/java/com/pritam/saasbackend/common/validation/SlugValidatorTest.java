package com.pritam.saasbackend.common.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SlugValidatorTest {

    record Holder(@ValidSlug String slug) {
    }

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

    @ParameterizedTest
    @ValueSource(strings = {"acme", "abc", "acme-corp-2", "a0-"})
    void acceptsValidSlugs(String slug) {
        assertThat(validator.validate(new Holder(slug))).isEmpty();
    }

    @Test
    void acceptsMaximumLength() {
        assertThat(validator.validate(new Holder("a" + "b".repeat(40)))).isEmpty();
    }

    @Test
    void rejectsOverMaximumLength() {
        assertThat(validator.validate(new Holder("a" + "b".repeat(41)))).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ab", "Acme", "1acme", "-acme", "acme_corp", "acme corp", "acme.corp", "acmé"})
    void rejectsMalformedSlugs(String slug) {
        assertThat(validator.validate(new Holder(slug))).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "api", "auth", "platform", "public", "system", "www"})
    void rejectsReservedSlugs(String slug) {
        assertThat(validator.validate(new Holder(slug))).hasSize(1);
    }

    @Test
    void treatsNullAsValidSoNotNullStaysSeparate() {
        assertThat(validator.validate(new Holder(null))).isEmpty();
    }
}
