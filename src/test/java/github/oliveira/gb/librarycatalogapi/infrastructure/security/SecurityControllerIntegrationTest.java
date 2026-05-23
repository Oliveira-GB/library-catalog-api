package github.oliveira.gb.librarycatalogapi.infrastructure.security;

import github.oliveira.gb.librarycatalogapi.domain.author.Author;
import github.oliveira.gb.librarycatalogapi.domain.author.AuthorRepository;
import github.oliveira.gb.librarycatalogapi.domain.book.Book;
import github.oliveira.gb.librarycatalogapi.domain.book.BookRepository;
import github.oliveira.gb.librarycatalogapi.domain.category.Category;
import github.oliveira.gb.librarycatalogapi.domain.category.CategoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for security configuration.
 * Validates RFC 7807 error responses for 401/403, public route bypass,
 * and successful authentication with database-backed Basic Auth.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Security Controller Integration Tests")
class SecurityControllerIntegrationTest {

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
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String AUTH_USER = "admin";
    private static final String AUTH_PASS = "admin123";

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM book_authors");
        jdbcTemplate.execute("DELETE FROM books");
        jdbcTemplate.execute("DELETE FROM authors");
        jdbcTemplate.execute("DELETE FROM categories");
    }

    @Nested
    @DisplayName("Public Route - ISBN Lookup")
    class PublicRouteTests {

        @Test
        @DisplayName("Should return 200 OK without authentication on public ISBN route")
        void shouldReturn200OkWithoutAuthenticationOnPublicIsbnRoute() throws Exception {
            Category category = createCategory("Public");
            Author author = createAuthor("Public Author", "public@example.com");
            createBook("Public Book", "978-0-00-000010-0", category, author);

            ResultActions result = mockMvc.perform(get("/api/v1/catalogo/livros/978-0-00-000010-0")
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Public Book"));
        }
    }

    @Nested
    @DisplayName("Protected Routes - Authentication Required")
    class ProtectedRouteTests {

        @Test
        @DisplayName("Should return 401 Unauthorized with RFC 7807 body when no credentials provided")
        void shouldReturn401UnauthorizedWithRfc7807BodyWhenNoCredentialsProvided() throws Exception {
            ResultActions result = mockMvc.perform(post("/api/v1/livros")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "title": "Unauthorized Book",
                                "isbn": "978-0-00-000020-0",
                                "categoryId": 1,
                                "authorIds": [1]
                            }
                            """));

            result.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/unauthorized"))
                    .andExpect(jsonPath("$.title").value("Unauthorized"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value(containsString("Authentication is required")));
        }

        @Test
        @DisplayName("Should return 201 Created with valid Basic Auth credentials")
        void shouldReturn201CreatedWithValidBasicAuthCredentials() throws Exception {
            Category category = createCategory("Secured");
            Author author = createAuthor("Secured Author", "secured@example.com");

            String requestBody = String.format("""
                    {
                        "title": "Secured Book",
                        "isbn": "978-0-13-468599-1",
                        "categoryId": %d,
                        "authorIds": [%d]
                    }
                    """, category.getId(), author.getId());

            ResultActions result = mockMvc.perform(post("/api/v1/livros")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Secured Book"));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized with invalid credentials")
        void shouldReturn401UnauthorizedWithInvalidCredentials() throws Exception {
            ResultActions result = mockMvc.perform(post("/api/v1/livros")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, "wrongpassword"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "title": "Invalid Book",
                                "isbn": "978-0-00-000040-0",
                                "categoryId": 1,
                                "authorIds": [1]
                            }
                            """));

            result.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/unauthorized"))
                    .andExpect(jsonPath("$.title").value("Unauthorized"))
                    .andExpect(jsonPath("$.status").value(401));
        }
    }

    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    private Author createAuthor(String name, String email) {
        Author author = new Author();
        author.setName(name);
        author.setEmail(email);
        return authorRepository.save(author);
    }

    private Book createBook(String title, String isbn, Category category, Author author) {
        Book book = new Book();
        book.setTitle(title);
        book.setIsbn(isbn);
        book.setCategory(category);
        book.getAuthors().add(author);
        book.setActive(true);
        return bookRepository.save(book);
    }
}
