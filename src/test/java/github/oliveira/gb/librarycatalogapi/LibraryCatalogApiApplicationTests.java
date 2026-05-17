package github.oliveira.gb.librarycatalogapi;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for application startup and infrastructure validation.
 * Following TDD approach to verify core infrastructure components.
 *
 * Uses Testcontainers for database isolation with singleton container pattern.
 * Transactional annotation ensures test isolation through automatic rollback.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
@DisplayName("Application Infrastructure Integration Tests")
class LibraryCatalogApiApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
            .withDatabaseName("catalog_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private Flyway flyway;

    @Test
    @DisplayName("Should load application context successfully")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    @DisplayName("Should establish database connection")
    void shouldEstablishDatabaseConnection() throws Exception {
        assertThat(dataSource).isNotNull();
        
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.isValid(5)).isTrue();
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        }
    }

    @Test
    @DisplayName("Should execute JDBC query successfully")
    void shouldExecuteJdbcQuerySuccessfully() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("Should have Flyway bean configured")
    void shouldHaveFlywayBeanConfigured() {
        assertThat(flyway).isNotNull();
    }

    @Test
    @DisplayName("Should create Flyway schema history table")
    void shouldCreateFlywaySchemaHistoryTable() {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'flyway_schema_history'",
            Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should have at least one migration applied")
    void shouldHaveAtLeastOneMigrationApplied() {
        Integer migrationCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
            Integer.class
        );
        assertThat(migrationCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should have baseline migration V1 applied")
    void shouldHaveBaselineMigrationV1Applied() {
        Integer baselineCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND script = 'V1__baseline.sql'",
            Integer.class
        );
        assertThat(baselineCount).isEqualTo(1);
    }
}