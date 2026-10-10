package dev.osuna.kanban.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * One shared PostgreSQL container for the whole test run (SPEC 12.1, D5).
 * The static field keeps a single container instance across Spring contexts;
 * Flyway applies V1 once per JVM, {@code ddl-auto=validate} runs against it.
 * Docker Desktop must be running.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerConfig {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18")
            .withDatabaseName("kanban")
            .withUsername("kanban")
            .withPassword("kanban_dev");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }
}
