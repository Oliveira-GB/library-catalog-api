package github.oliveira.gb.librarycatalogapi.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for generating interactive API documentation.
 * Defines global metadata and configures the HTTP Basic Authentication
 * security scheme for the Swagger UI "Authorize" button.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Library Catalog Management API",
                version = "1.0.0",
                description = "RESTful transactional core for physical library inventory management. "
                        + "Provides catalog administration, reader identity management, "
                        + "atomic loan/return operations, and parameterized discovery/reporting endpoints."
        )
)
@SecurityScheme(
        name = "basicAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "basic"
)
public class OpenApiConfig {
}
