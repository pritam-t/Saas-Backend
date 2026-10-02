package com.pritam.saasbackend.db;

import com.pritam.saasbackend.support.IntegrationTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * The context only starts if Flyway migrated the empty container database to head
 * and Hibernate's {@code ddl-auto=validate} accepted every entity mapping.
 * The explicit checks below cover what {@code validate} does not: column lengths,
 * nullability and unique constraints.
 */
@IntegrationTest
class FlywaySchemaIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    @Test
    void shouldMigrateEmptyDatabaseToHead() {

        MigrationInfo[] applied = flyway.info().applied();

        assertThat(applied).isNotEmpty();
        assertThat(applied)
                .allSatisfy(migration ->
                        assertThat(migration.getState()).isEqualTo(MigrationState.SUCCESS));
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void shouldReadMigrationsOnlyFromPublicLocation() {

        assertThat(Arrays.stream(flyway.getConfiguration().getLocations())
                .map(Object::toString))
                .containsExactly("classpath:db/migration/public");
    }

    @Test
    void shouldInheritSchemaSafetySettingsFromMainConfig() {

        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                .isEqualTo("validate");
        assertThat(environment.getProperty("spring.jpa.open-in-view"))
                .isEqualTo("false");
    }

    @Test
    void shouldCreateTenantsTableMatchingTheEntity() {

        List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
                SELECT column_name, data_type, character_maximum_length, is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'tenants'
                ORDER BY ordinal_position
                """);

        assertThat(columns)
                .extracting(
                        column -> column.get("column_name"),
                        column -> column.get("data_type"),
                        column -> column.get("character_maximum_length"),
                        column -> column.get("is_nullable"))
                .containsExactly(
                        tuple("id", "uuid", null, "NO"),
                        tuple("name", "character varying", 100, "NO"),
                        tuple("slug", "character varying", 100, "NO"),
                        tuple("schema_name", "character varying", 100, "NO"),
                        tuple("status", "character varying", 30, "NO"),
                        tuple("created_at", "timestamp with time zone", null, "NO"),
                        tuple("updated_at", "timestamp with time zone", null, "NO"));
    }

    @Test
    void shouldEnforceTenantUniqueConstraintsInTheDatabase() {

        List<String> uniqueConstraints = jdbcTemplate.queryForList("""
                SELECT conname
                FROM pg_constraint
                WHERE conrelid = 'public.tenants'::regclass AND contype = 'u'
                ORDER BY conname
                """, String.class);

        assertThat(uniqueConstraints)
                .containsExactly("uk_tenants_name", "uk_tenants_schema_name", "uk_tenants_slug");
    }
}
