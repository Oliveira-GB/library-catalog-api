package github.oliveira.gb.librarycatalogapi.api.author;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import github.oliveira.gb.librarycatalogapi.domain.author.AuthorRepository;
import org.junit.jupiter.api.AfterEach;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthorController.
 * Tests HTTP flows, JSON serialization, pagination, and error handling.
 * Uses Testcontainers for database parity.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("Author Controller Integration Tests")
class AuthorControllerIntegrationTest {

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
    private AuthorRepository authorRepository;

    private static final String BASE_API_PATH = "/api/v1/autores";


    @AfterEach
    void tearDown() {
        authorRepository.deleteAll();
    }
    @Nested
    @DisplayName("POST /api/v1/autores - Create Author")
    class CreateAuthorTests {

        @Test
        @DisplayName("Should create author and return HTTP 201 with response body")
        void shouldCreateAuthorAndReturnHttp201WithResponseBody() throws Exception {
            // Arrange
            String requestBody = """
                    {
                        "name": "John Doe",
                        "email": "john.doe@example.com",
                        "biography": "A famous author"
                    }
                    """;

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString(BASE_API_PATH)))
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.name").value("John Doe"))
                    .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                    .andExpect(jsonPath("$.biography").value("A famous author"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        @DisplayName("Should create author without biography")
        void shouldCreateAuthorWithoutBiography() throws Exception {
            // Arrange
            String requestBody = """
                    {
                        "name": "Jane Smith",
                        "email": "jane.smith@example.com"
                    }
                    """;

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Jane Smith"))
                    .andExpect(jsonPath("$.email").value("jane.smith@example.com"))
                    .andExpect(jsonPath("$.biography").doesNotExist());
        }

        @Test
        @DisplayName("Should return HTTP 400 for empty name")
        void shouldReturnHttp400ForEmptyName() throws Exception {
            // Arrange
            String requestBody = """
                    {
                        "name": "",
                        "email": "test@example.com"
                    }
                    """;

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/validation-error"))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());
        }

        @Test
        @DisplayName("Should return HTTP 400 for invalid email format")
        void shouldReturnHttp400ForInvalidEmailFormat() throws Exception {
            // Arrange
            String requestBody = """
                    {
                        "name": "Test Author",
                        "email": "invalid-email-format"
                    }
                    """;

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists());
        }

        @Test
        @DisplayName("Should return HTTP 400 for empty email")
        void shouldReturnHttp400ForEmptyEmail() throws Exception {
            // Arrange
            String requestBody = """
                    {
                        "name": "Test Author",
                        "email": ""
                    }
                    """;

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists());
        }

        @Test
        @DisplayName("Should return HTTP 409 for duplicate email")
        void shouldReturnHttp409ForDuplicateEmail() throws Exception {
            // Arrange - Create first author
            String requestBody = """
                    {
                        "name": "First Author",
                        "email": "unique@example.com"
                    }
                    """;

            mockMvc.perform(post(BASE_API_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated());

            // Act - Try to create author with same email
            String duplicateRequest = """
                    {
                        "name": "Second Author",
                        "email": "unique@example.com"
                    }
                    """;

            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(duplicateRequest));

            // Assert
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/data-conflict"))
                    .andExpect(jsonPath("$.title").value("Data Conflict"))
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("Should return HTTP 400 for name exceeding max length")
        void shouldReturnHttp400ForNameExceedingMaxLength() throws Exception {
            // Arrange
            String longName = "A".repeat(151);
            String requestBody = String.format("""
                    {
                        "name": "%s",
                        "email": "test@example.com"
                    }
                    """, longName);

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());
        }

        @Test
        @DisplayName("Should return HTTP 400 for email exceeding max length")
        void shouldReturnHttp400ForEmailExceedingMaxLength() throws Exception {
            // Arrange
            String longEmail = "a".repeat(140) + "@example.com"; // Exceeds 150 chars
            String requestBody = String.format("""
                    {
                        "name": "Test Author",
                        "email": "%s"
                    }
                    """, longEmail);

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists());
        }

        @Test
        @DisplayName("Should trim whitespace from author name")
        void shouldTrimWhitespaceFromAuthorName() throws Exception {
            // Arrange - Note: email cannot have whitespace due to @Email validation
            String requestBody = """
                    {
                        "name": "  Jane Doe  ",
                        "email": "jane.doe@example.com",
                        "biography": "  A biography  "
                    }
                    """;

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Jane Doe"))
                    .andExpect(jsonPath("$.email").value("jane.doe@example.com"))
                    .andExpect(jsonPath("$.biography").value("  A biography  ")); // Biography is not trimmed
        }
    }

    @Nested
    @DisplayName("GET /api/v1/autores - List Authors")
    class ListAuthorsTests {

        @Test
        @DisplayName("Should return paginated list with HTTP 200")
        void shouldReturnPaginatedListWithHttp200() throws Exception {
            // Arrange - Create some authors first
            createAuthor("Author A", "author.a@example.com");
            createAuthor("Author B", "author.b@example.com");

            // Act
            ResultActions result = mockMvc.perform(get(BASE_API_PATH)
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
        @DisplayName("Should return empty page when no authors exist")
        void shouldReturnEmptyPageWhenNoAuthorsExist() throws Exception {
            // Act - Request page 100 (likely empty)
            ResultActions result = mockMvc.perform(get(BASE_API_PATH)
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
            // Arrange - Create multiple authors
            for (int i = 1; i <= 5; i++) {
                createAuthor("Author " + i, "author" + i + "@example.com");
            }

            // Act
            ResultActions result = mockMvc.perform(get(BASE_API_PATH)
                    .param("page", "0")
                    .param("size", "2"));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.size").value(2));
        }

        @Test
        @DisplayName("Should include all author fields in response")
        void shouldIncludeAllAuthorFieldsInResponse() throws Exception {
            // Arrange
            createAuthorWithBiography("Complete Author", "complete@example.com", "A complete biography");

            // Act
            ResultActions result = mockMvc.perform(get(BASE_API_PATH)
                    .param("page", "0")
                    .param("size", "10"));

            // Assert - Check that at least one author has all fields including biography
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id").exists())
                    .andExpect(jsonPath("$.content[*].name").exists())
                    .andExpect(jsonPath("$.content[*].email").exists())
                    .andExpect(jsonPath("$.content[*].biography", hasItem("A complete biography")))
                    .andExpect(jsonPath("$.content[*].active").exists())
                    .andExpect(jsonPath("$.content[*].createdAt").exists())
                    .andExpect(jsonPath("$.content[*].updatedAt").exists());
        }
    }

    private void createAuthor(String name, String email) throws Exception {
        String requestBody = String.format("""
                {
                    "name": "%s",
                    "email": "%s"
                }
                """, name, email);

        mockMvc.perform(post(BASE_API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());
    }

    private void createAuthorWithBiography(String name, String email, String biography) throws Exception {
        String requestBody = String.format("""
                {
                    "name": "%s",
                    "email": "%s",
                    "biography": "%s"
                }
                """, name, email, biography);

        mockMvc.perform(post(BASE_API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());
    }
}
