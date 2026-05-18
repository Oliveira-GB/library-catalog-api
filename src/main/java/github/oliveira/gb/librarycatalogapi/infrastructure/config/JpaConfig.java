package github.oliveira.gb.librarycatalogapi.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Configuration class.
 * Enables JPA Auditing for automatic population of createdAt and updatedAt fields.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}