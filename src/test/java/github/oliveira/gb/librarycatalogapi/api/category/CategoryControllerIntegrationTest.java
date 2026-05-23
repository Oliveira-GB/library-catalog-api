package github.oliveira.gb.librarycatalogapi.api.category;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import github.oliveira.gb.librarycatalogapi.domain.category.CategoryRepository;
import org.junit.jupiter.api.AfterEach;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CategoryController.
 * Tests HTTP flows, JSON serialization, pagination, and error handling.
 * Uses Testcontainers for database parity.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Category Controller Integration Tests")
class CategoryControllerIntegrationTest {

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
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    private static final String AUTH_USER = "admin";
    private static final String AUTH_PASS = "admin123";

    private static final String BASE_API_PATH = "/api/v1/categorias";


    @AfterEach
    void tearDown() {
        categoryRepository.deleteAll();
    }
    @Nested
    @DisplayName("POST /api/v1/categorias - Create Category")
    class CreateCategoryTests {

        @Test
        @DisplayName("Should create category and return HTTP 201 with response body")
        void shouldCreateCategoryAndReturnHttp201WithResponseBody() throws Exception {
            // Arrange
            String requestBody = """
                    {
                        "name": "Fiction"
                    }
                    """;

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString(BASE_API_PATH)))
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.name").value("Fiction"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        @DisplayName("Should return HTTP 400 for empty name")
        void shouldReturnHttp400ForEmptyName() throws Exception {
            // Arrange
            String requestBody = """
                    {
                        "name": ""
                    }
                    """;

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Failed"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());
        }

        @Test
        @DisplayName("Should return HTTP 400 for null name")
        void shouldReturnHttp400ForNullName() throws Exception {
            // Arrange
            String requestBody = """
                    {
                        "name": null
                    }
                    """;

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());
        }

        @Test
        @DisplayName("Should return HTTP 400 for name exceeding max length")
        void shouldReturnHttp400ForNameExceedingMaxLength() throws Exception {
            // Arrange
            String longName = "A".repeat(101);
            String requestBody = String.format("""
                    {
                        "name": "%s"
                    }
                    """, longName);

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());
        }

        @Test
        @DisplayName("Should return HTTP 409 for duplicate category name")
        void shouldReturnHttp409ForDuplicateCategoryName() throws Exception {
            // Arrange - Create first category
            String requestBody = """
                    {
                        "name": "UniqueCategory"
                    }
                    """;

            mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated());

            // Act - Try to create duplicate
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/data-conflict"))
                    .andExpect(jsonPath("$.title").value("Data Conflict"))
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("Should trim whitespace from category name")
        void shouldTrimWhitespaceFromCategoryName() throws Exception {
            // Arrange
            String requestBody = """
                    {
                        "name": "  Science  "
                    }
                    """;

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Science"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/categorias - List Categories")
    class ListCategoriesTests {

        @Test
        @DisplayName("Should return paginated list with HTTP 200")
        void shouldReturnPaginatedListWithHttp200() throws Exception {
            // Arrange - Create some categories first
            createCategory("Category A");
            createCategory("Category B");

            // Act
            ResultActions result = mockMvc.perform(get(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .param("page", "0")
                    .param("size", "10"));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)))
                    .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(2)))
                    .andExpect(jsonPath("$.number").value(0))
                    .andExpect(jsonPath("$.size").value(10));
        }

        @Test
        @DisplayName("Should return empty page when no categories exist")
        void shouldReturnEmptyPageWhenNoCategoriesExist() throws Exception {
            // Act - Request page 100 (likely empty)
            ResultActions result = mockMvc.perform(get(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .param("page", "100")
                    .param("size", "10"));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("Should respect pagination parameters")
        void shouldRespectPaginationParameters() throws Exception {
            // Arrange - Create multiple categories
            for (int i = 1; i <= 5; i++) {
                createCategory("Category " + i);
            }

            // Act
            ResultActions result = mockMvc.perform(get(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .param("page", "0")
                    .param("size", "2"));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.size").value(2));
        }

        @Test
        @DisplayName("Should return RFC 7807 response for all errors")
        void shouldReturnRfc7807ResponseForAllErrors() throws Exception {
            // Act - Send invalid JSON
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{invalid json}"));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").exists())
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.detail").exists());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/categorias/{id} - Deactivate Category")
    class DeactivateCategoryTests {

        @Test
        @DisplayName("Should deactivate category and return HTTP 204")
        void shouldDeactivateCategoryAndReturnHttp204() throws Exception {
            // Arrange - Create a category first
            String categoryName = "CategoryToDelete";
            createCategory(categoryName);

            // Get the created category ID
            Long categoryId = categoryRepository.findAll().stream()
                    .filter(c -> c.getName().equals(categoryName))
                    .findFirst()
                    .map(c -> c.getId())
                    .orElseThrow();

            // Act
            ResultActions result = mockMvc.perform(delete(BASE_API_PATH + "/" + categoryId)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            );

            // Assert
            result.andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return HTTP 404 for non-existent category")
        void shouldReturnHttp404ForNonExistentCategory() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(delete(BASE_API_PATH + "/99999")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            );

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/resource-not-found"))
                    .andExpect(jsonPath("$.title").value("Resource Not Found"))
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Should omit deactivated category from list after deletion")
        void shouldOmitDeactivatedCategoryFromListAfterDeletion() throws Exception {
            // Arrange - Create a category
            String categoryName = "CategoryToBeHidden";
            createCategory(categoryName);

            Long categoryId = categoryRepository.findAll().stream()
                    .filter(c -> c.getName().equals(categoryName))
                    .findFirst()
                    .map(c -> c.getId())
                    .orElseThrow();

            // Verify category is in the list before deletion
            mockMvc.perform(get(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].name", hasItem(categoryName)));

            // Act - Delete the category
            mockMvc.perform(delete(BASE_API_PATH + "/" + categoryId)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            )
                    .andExpect(status().isNoContent());

            // Assert - Verify category is NOT in the list after deletion
            mockMvc.perform(get(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].name", not(hasItem(categoryName))));
        }
    }

    private void createCategory(String name) throws Exception {
        String requestBody = String.format("""
                {
                    "name": "%s"
                }
                """, name);

        mockMvc.perform(post(BASE_API_PATH)
                .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());
    }
}
