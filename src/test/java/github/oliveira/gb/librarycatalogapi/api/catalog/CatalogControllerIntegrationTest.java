package github.oliveira.gb.librarycatalogapi.api.catalog;

import github.oliveira.gb.librarycatalogapi.domain.author.Author;
import github.oliveira.gb.librarycatalogapi.domain.author.AuthorRepository;
import github.oliveira.gb.librarycatalogapi.domain.book.Book;
import github.oliveira.gb.librarycatalogapi.domain.book.BookRepository;
import github.oliveira.gb.librarycatalogapi.domain.book.BookStatus;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CatalogController.
 * Validates catalog search filters, ISBN public lookup, 100-record limit,
 * empty results, and security bypass for the public endpoint.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Catalog Controller Integration Tests")
class CatalogControllerIntegrationTest {

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

    private static final String BASE_API_PATH = "/api/v1/catalogo";
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
    @DisplayName("GET /api/v1/catalogo - Catalog Search")
    class CatalogSearchTests {

        @Test
        @DisplayName("Should return books with no filters")
        void shouldReturnBooksWithNoFilters() throws Exception {
            Category cat = createCategory("Fiction");
            Author auth = createAuthor("John Doe", "john@example.com");
            createBook("Test Book", "978-0-00-000001-0", cat, auth, BookStatus.DISPONIVEL);

            ResultActions result = mockMvc.perform(get(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value("Test Book"))
                    .andExpect(jsonPath("$[0].author").value("John Doe"))
                    .andExpect(jsonPath("$[0].genre").value("Fiction"))
                    .andExpect(jsonPath("$[0].isbn").value("978-0-00-000001-0"))
                    .andExpect(jsonPath("$[0].available").value(true));
        }

        @Test
        @DisplayName("Should filter by title partial match")
        void shouldFilterByTitlePartialMatch() throws Exception {
            Category cat = createCategory("Sci-Fi");
            Author auth = createAuthor("Author", "a@example.com");
            createBook("Matrix", "978-0-00-000002-0", cat, auth, BookStatus.DISPONIVEL);
            createBook("Other Book", "978-0-00-000003-0", cat, auth, BookStatus.DISPONIVEL);

            ResultActions result = mockMvc.perform(get(BASE_API_PATH)
                    .param("titulo", "Mat")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value("Matrix"));
        }

        @Test
        @DisplayName("Should filter by author partial match")
        void shouldFilterByAuthorPartialMatch() throws Exception {
            Category cat = createCategory("Drama");
            Author a1 = createAuthor("Alice Smith", "alice@example.com");
            Author a2 = createAuthor("Bob Jones", "bob@example.com");
            createBook("Book One", "978-0-00-000004-0", cat, a1, BookStatus.DISPONIVEL);
            createBook("Book Two", "978-0-00-000005-0", cat, a2, BookStatus.DISPONIVEL);

            ResultActions result = mockMvc.perform(get(BASE_API_PATH)
                    .param("autor", "Alice")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].author").value("Alice Smith"));
        }

        @Test
        @DisplayName("Should filter by exact genre")
        void shouldFilterByExactGenre() throws Exception {
            Category cat1 = createCategory("Horror");
            Category cat2 = createCategory("Comedy");
            Author auth = createAuthor("Writer", "w@example.com");
            createBook("Scary", "978-0-00-000006-0", cat1, auth, BookStatus.DISPONIVEL);
            createBook("Funny", "978-0-00-000007-0", cat2, auth, BookStatus.DISPONIVEL);

            ResultActions result = mockMvc.perform(get(BASE_API_PATH)
                    .param("genero", "Horror")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].genre").value("Horror"));
        }

        @Test
        @DisplayName("Should filter by availability")
        void shouldFilterByAvailability() throws Exception {
            Category cat = createCategory("Tech");
            Author auth = createAuthor("Dev", "dev@example.com");
            createBook("Available Book", "978-0-00-000008-0", cat, auth, BookStatus.DISPONIVEL);
            createBook("Loaned Book", "978-0-00-000009-0", cat, auth, BookStatus.EMPRESTADO);

            ResultActions result = mockMvc.perform(get(BASE_API_PATH)
                    .param("disponivel", "true")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value("Available Book"))
                    .andExpect(jsonPath("$[0].available").value(true));
        }

        @Test
        @DisplayName("Should concatenate multiple authors via string_agg")
        void shouldConcatenateMultipleAuthorsViaStringAgg() throws Exception {
            Category cat = createCategory("MultiAuthor");
            Author a1 = createAuthor("Alice Smith", "alice@example.com");
            Author a2 = createAuthor("Bob Jones", "bob@example.com");
            Book book = createBook("Multi Book", "978-0-00-000020-0", cat, a1, BookStatus.DISPONIVEL);
            book.getAuthors().add(a2);
            bookRepository.save(book);

            ResultActions result = mockMvc.perform(get(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].author").value("Alice Smith, Bob Jones"));
        }

        @Test
        @DisplayName("Should enforce maximum of 100 records")
        void shouldEnforceMaximumOf100Records() throws Exception {
            Category cat = createCategory("Bulk");
            Author auth = createAuthor("Bulk Author", "bulk@example.com");
            for (int i = 0; i < 101; i++) {
                createBook("Book " + i, "978-0-00-000" + String.format("%03d", i) + "-0", cat, auth, BookStatus.DISPONIVEL);
            }

            ResultActions result = mockMvc.perform(get(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(100));
        }

        @Test
        @DisplayName("Should return empty JSON array when no books match")
        void shouldReturnEmptyArrayWhenNoMatch() throws Exception {
            ResultActions result = mockMvc.perform(get(BASE_API_PATH)
                    .param("titulo", "NonExistent")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/catalogo/livros/{isbn} - Public ISBN Lookup")
    class IsbnLookupTests {

        @Test
        @DisplayName("Should return book metadata without authentication")
        void shouldReturnBookMetadataWithoutAuthentication() throws Exception {
            Category cat = createCategory("Public");
            Author auth = createAuthor("Public Author", "public@example.com");
            createBook("Public Book", "978-0-00-000010-0", cat, auth, BookStatus.DISPONIVEL);

            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/livros/978-0-00-000010-0")
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Public Book"))
                    .andExpect(jsonPath("$.author").value("Public Author"))
                    .andExpect(jsonPath("$.genre").value("Public"))
                    .andExpect(jsonPath("$.isbn").value("978-0-00-000010-0"))
                    .andExpect(jsonPath("$.id").doesNotExist());
        }

        @Test
        @DisplayName("Should return 404 for non-existent ISBN")
        void shouldReturn404ForNonExistentIsbn() throws Exception {
            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/livros/978-0-00-000000-0")
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/resource-not-found"));
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

    private Book createBook(String title, String isbn, Category category, Author author, BookStatus status) {
        Book book = new Book();
        book.setTitle(title);
        book.setIsbn(isbn);
        book.setCategory(category);
        book.getAuthors().add(author);
        book.setStatus(status);
        book.setActive(true);
        return bookRepository.save(book);
    }
}
