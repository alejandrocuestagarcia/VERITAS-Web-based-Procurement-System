package com.veritas.backend;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void clearDatabaseTables() {
        if (jdbcTemplate != null) {
            jdbcTemplate.execute("DELETE FROM jira_sync_queue_items");
            jdbcTemplate.execute("DELETE FROM notifications");
            jdbcTemplate.execute("DELETE FROM password_reset_tokens");
            jdbcTemplate.execute("DELETE FROM refresh_tokens");
            jdbcTemplate.execute("DELETE FROM invoices");
            jdbcTemplate.execute("DELETE FROM audit_logs");
            jdbcTemplate.execute("DELETE FROM attachments");
            jdbcTemplate.execute("DELETE FROM quote_line_items");
            jdbcTemplate.execute("DELETE FROM quotes");
            jdbcTemplate.execute("DELETE FROM vendor_evaluations");
            jdbcTemplate.execute("DELETE FROM request_items");
            jdbcTemplate.execute("DELETE FROM requests");
            jdbcTemplate.execute("DELETE FROM transition_rules");
            jdbcTemplate.execute("DELETE FROM workflow_transitions");
            jdbcTemplate.execute("DELETE FROM workflow_steps");
            jdbcTemplate.execute("DELETE FROM workflow_definitions");
            jdbcTemplate.execute("DELETE FROM jira_configs");
            jdbcTemplate.execute("DELETE FROM projects");
            jdbcTemplate.execute("DELETE FROM users");
            jdbcTemplate.execute("DELETE FROM teams");
            jdbcTemplate.execute("UPDATE departments SET budget_id = NULL");
            jdbcTemplate.execute("DELETE FROM internal_budgets");
            jdbcTemplate.execute("DELETE FROM departments");
            jdbcTemplate.execute("DELETE FROM vendors");
            jdbcTemplate.execute("DELETE FROM exchange_rates");
        }
    }
}
