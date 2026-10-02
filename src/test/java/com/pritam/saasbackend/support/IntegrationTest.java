package com.pritam.saasbackend.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Full application context against a Testcontainers PostgreSQL, with the
 * {@code test} profile layered on top of the main configuration, and MockMvc
 * running through the real Spring Security filter chain.
 * <p>
 * The container is shared by every test in a run, so tests must create
 * their own unique data and never rely on rows written by another test.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public @interface IntegrationTest {
}
