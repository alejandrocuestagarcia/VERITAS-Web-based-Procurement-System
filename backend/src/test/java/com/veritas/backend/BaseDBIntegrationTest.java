package com.veritas.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all integration tests that require a real PostgreSQL database.
 *
 * Extend this class for any test that interacts with the database directly,
 * such as service tests, repository tests, or full Spring context tests.
 *
 * The PostgreSQL container is started once and shared across all tests in the
 * same test run for performance. Liquibase migrations are applied automatically
 * on startup, ensuring the schema matches production.
 *
 * Each test class should clean up its own data in @BeforeEach to ensure
 * test isolation.
 */
@SpringBootTest
@Testcontainers
public abstract class BaseDBIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
}
