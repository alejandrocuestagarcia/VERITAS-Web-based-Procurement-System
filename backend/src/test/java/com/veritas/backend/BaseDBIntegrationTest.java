package com.veritas.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for all integration tests that require a real PostgreSQL database.
 *
 * Extend this class for any test that interacts with the database directly,
 * such as service tests, repository tests, or full Spring context tests.
 *
 * The PostgreSQL container is started once and shared across all tests in the
 * same test run for performance.
 *
 * Each test class should clean up its own data in @BeforeEach to ensure
 * test isolation.
 */
@SpringBootTest
@TestPropertySource(properties = "app.seeding.enabled=false")
public abstract class BaseDBIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {

        if (System.getenv("CI") == null) {
            POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
            POSTGRES.start();
        } else {
            // In CI, we don't create the container
            POSTGRES = null;
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // Only try to link Postgres properties if the container actually exists
        if (POSTGRES != null && POSTGRES.isRunning()) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        }

        registry.add("spring.mail.username", () -> "test@veritas.local");
        registry.add("spring.mail.password", () -> "test-password");
    }
}
