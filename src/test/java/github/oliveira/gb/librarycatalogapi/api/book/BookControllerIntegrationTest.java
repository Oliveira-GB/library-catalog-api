package github.oliveira.gb.librarycatalogapi.api.book;

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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for BookController.
 * Tests HTTP flows, JSON serialization, pagination, relationship handling, and error handling.
 * Uses Testcontainers for database parity.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Book Controller Integration Tests")
class BookControllerIntegrationTest {

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

    private static final String BASE_API_PATH = "/api/v1/livros";

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM book_authors");
        jdbcTemplate.execute("DELETE FROM books");
        jdbcTemplate.execute("DELETE FROM authors");
        jdbcTemplate.execute("DELETE FROM categories");
    }

    @Nested
    @DisplayName("POST /api/v1/livros - Create Book")
    class CreateBookTests {

        @Test
        @DisplayName("Should create book and return HTTP 201 with response body")
        void shouldCreateBookAndReturnHttp201WithResponseBody() throws Exception {
            // Arrange
            Category category = createCategory("Fiction");
            Author author = createAuthor("John Doe", "john.doe@example.com");

            String requestBody = String.format("""
                    {
                        "title": "Test Book",
                        "isbn": "978-0-13-468599-1",
                        "categoryId": %d,
                        "authorIds": [%d]
                    }
                    """, category.getId(), author.getId());

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString(BASE_API_PATH)))
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.title").value("Test Book"))
                    .andExpect(jsonPath("$.isbn").value("978-0-13-468599-1"))
                    .andExpect(jsonPath("$.status").value("DISPONIVEL"))
                    .andExpect(jsonPath("$.category.id").value(category.getId().intValue()))
                    .andExpect(jsonPath("$.category.name").value("Fiction"))
                    .andExpect(jsonPath("$.authors").isArray())
                    .andExpect(jsonPath("$.authors[0].id").value(author.getId().intValue()))
                    .andExpect(jsonPath("$.authors[0].name").value("John Doe"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        @DisplayName("Should return HTTP 400 for invalid ISBN format")
        void shouldReturnHttp400ForInvalidIsbnFormat() throws Exception {
            // Arrange
            Category category = createCategory("Science");
            Author author = createAuthor("Jane Smith", "jane.smith@example.com");

            String requestBody = String.format("""
                    {
                        "title": "Invalid Book",
                        "isbn": "not-an-isbn",
                        "categoryId": %d,
                        "authorIds": [%d]
                    }
                    """, category.getId(), author.getId());

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/validation-error"))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'isbn')]").exists());
        }

        @Test
        @DisplayName("Should return HTTP 400 for missing title")
        void shouldReturnHttp400ForMissingTitle() throws Exception {
            // Arrange
            Category category = createCategory("History");
            Author author = createAuthor("Bob Brown", "bob.brown@example.com");

            String requestBody = String.format("""
                    {
                        "title": "",
                        "isbn": "978-0-13-468599-1",
                        "categoryId": %d,
                        "authorIds": [%d]
                    }
                    """, category.getId(), author.getId());

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'title')]").exists());
        }

        @Test
        @DisplayName("Should return HTTP 400 for missing categoryId")
        void shouldReturnHttp400ForMissingCategoryId() throws Exception {
            // Arrange
            Author author = createAuthor("Alice Green", "alice.green@example.com");

            String requestBody = String.format("""
                    {
                        "title": "No Category Book",
                        "isbn": "978-0-13-468599-1",
                        "authorIds": [%d]
                    }
                    """, author.getId());

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'categoryId')]").exists());
        }

        @Test
        @DisplayName("Should return HTTP 400 for empty authorIds")
        void shouldReturnHttp400ForEmptyAuthorIds() throws Exception {
            // Arrange
            Category category = createCategory("Poetry");

            String requestBody = String.format("""
                    {
                        "title": "No Authors Book",
                        "isbn": "978-0-13-468599-1",
                        "categoryId": %d,
                        "authorIds": []
                    }
                    """, category.getId());

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'authorIds')]").exists());
        }

        @Test
        @DisplayName("Should return HTTP 404 for non-existent category")
        void shouldReturnHttp404ForNonExistentCategory() throws Exception {
            // Arrange
            Author author = createAuthor("Test Author", "test.author@example.com");

            String requestBody = String.format("""
                    {
                        "title": "Test Book",
                        "isbn": "978-0-13-468599-1",
                        "categoryId": 99999,
                        "authorIds": [%d]
                    }
                    """, author.getId());

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/resource-not-found"));
        }

        @Test
        @DisplayName("Should return HTTP 404 for non-existent author")
        void shouldReturnHttp404ForNonExistentAuthor() throws Exception {
            // Arrange
            Category category = createCategory("Drama");

            String requestBody = String.format("""
                    {
                        "title": "Test Book",
                        "isbn": "978-0-13-468599-1",
                        "categoryId": %d,
                        "authorIds": [99999]
                    }
                    """, category.getId());

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/resource-not-found"));
        }

        @Test
        @DisplayName("Should return HTTP 409 for duplicate ISBN")
        void shouldReturnHttp409ForDuplicateIsbn() throws Exception {
            // Arrange
            Category category = createCategory("Tech");
            Author author1 = createAuthor("First Author", "first@example.com");
            Author author2 = createAuthor("Second Author", "second@example.com");
            String isbn = "978-0-13-468599-1";

            createBook("First Book", isbn, category, author1);

            String requestBody = String.format("""
                    {
                        "title": "Second Book",
                        "isbn": "%s",
                        "categoryId": %d,
                        "authorIds": [%d]
                    }
                    """, isbn, category.getId(), author2.getId());

            // Act
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
        @DisplayName("Should trim whitespace from title and ISBN")
        void shouldTrimWhitespaceFromTitleAndIsbn() throws Exception {
            // Arrange
            Category category = createCategory("Art");
            Author author = createAuthor("Artist", "artist@example.com");

            String requestBody = String.format("""
                    {
                        "title": "  Trimmed Book  ",
                        "isbn": "  978-0-13-468599-1  ",
                        "categoryId": %d,
                        "authorIds": [%d]
                    }
                    """, category.getId(), author.getId());

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Trimmed Book"))
                    .andExpect(jsonPath("$.isbn").value("978-0-13-468599-1"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/livros/{id} - Find Book by ID")
    class FindBookByIdTests {

        @Test
        @DisplayName("Should return book details with HTTP 200")
        void shouldReturnBookDetailsWithHttp200() throws Exception {
            // Arrange
            Category category = createCategory("Philosophy");
            Author author = createAuthor("Philosopher", "phil@example.com");
            Book book = createBook("Philosophy 101", "978-0-201-63361-0", category, author);

            // Act
            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/" + book.getId())
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            );

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(book.getId().intValue()))
                    .andExpect(jsonPath("$.title").value("Philosophy 101"))
                    .andExpect(jsonPath("$.isbn").value("978-0-201-63361-0"))
                    .andExpect(jsonPath("$.status").value("DISPONIVEL"))
                    .andExpect(jsonPath("$.category.id").value(category.getId().intValue()))
                    .andExpect(jsonPath("$.category.name").value("Philosophy"))
                    .andExpect(jsonPath("$.authors").isArray())
                    .andExpect(jsonPath("$.authors[0].name").value("Philosopher"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        @DisplayName("Should return HTTP 404 for non-existent book ID")
        void shouldReturnHttp404ForNonExistentBookId() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/99999")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            );

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/resource-not-found"))
                    .andExpect(jsonPath("$.title").value("Resource Not Found"))
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/livros/isbn/{isbn} - Find Book by ISBN")
    class FindBookByIsbnTests {

        @Test
        @DisplayName("Should return book details with HTTP 200")
        void shouldReturnBookDetailsWithHttp200() throws Exception {
            // Arrange
            Category category = createCategory("Math");
            Author author = createAuthor("Mathematician", "math@example.com");
            Book book = createBook("Calculus I", "978-3-16-148410-0", category, author);

            // Act
            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/isbn/" + book.getIsbn())
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            );

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(book.getId().intValue()))
                    .andExpect(jsonPath("$.title").value("Calculus I"))
                    .andExpect(jsonPath("$.isbn").value("978-3-16-148410-0"))
                    .andExpect(jsonPath("$.category.name").value("Math"))
                    .andExpect(jsonPath("$.authors[0].name").value("Mathematician"));
        }

        @Test
        @DisplayName("Should return HTTP 404 for non-existent ISBN")
        void shouldReturnHttp404ForNonExistentIsbn() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/isbn/978-0-00-000000-0")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            );

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/resource-not-found"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/livros/{id} - Update Book")
    class UpdateBookTests {

        @Test
        @DisplayName("Should update book and return HTTP 200 with updated data")
        void shouldUpdateBookAndReturnHttp200WithUpdatedData() throws Exception {
            // Arrange
            Category category = createCategory("Original Category");
            Author author = createAuthor("Original Author", "original@example.com");
            Book book = createBook("Original Title", "978-0-13-235088-4", category, author);

            Category newCategory = createCategory("New Category");
            Author newAuthor = createAuthor("New Author", "new@example.com");

            String requestBody = String.format("""
                    {
                        "title": "Updated Title",
                        "isbn": "978-0-596-00712-6",
                        "categoryId": %d,
                        "authorIds": [%d]
                    }
                    """, newCategory.getId(), newAuthor.getId());

            // Act
            ResultActions result = mockMvc.perform(put(BASE_API_PATH + "/" + book.getId())
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(book.getId().intValue()))
                    .andExpect(jsonPath("$.title").value("Updated Title"))
                    .andExpect(jsonPath("$.isbn").value("978-0-596-00712-6"))
                    .andExpect(jsonPath("$.category.id").value(newCategory.getId().intValue()))
                    .andExpect(jsonPath("$.category.name").value("New Category"))
                    .andExpect(jsonPath("$.authors[0].id").value(newAuthor.getId().intValue()))
                    .andExpect(jsonPath("$.authors[0].name").value("New Author"));
        }

        @Test
        @DisplayName("Should return HTTP 404 for non-existent book ID")
        void shouldReturnHttp404ForNonExistentBookId() throws Exception {
            // Arrange
            Category category = createCategory("Any");
            Author author = createAuthor("Any", "any@example.com");

            String requestBody = String.format("""
                    {
                        "title": "Title",
                        "isbn": "978-1-4493-2586-2",
                        "categoryId": %d,
                        "authorIds": [%d]
                    }
                    """, category.getId(), author.getId());

            // Act
            ResultActions result = mockMvc.perform(put(BASE_API_PATH + "/99999")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/resource-not-found"));
        }

        @Test
        @DisplayName("Should return HTTP 409 when updating to duplicate ISBN")
        void shouldReturnHttp409WhenUpdatingToDuplicateIsbn() throws Exception {
            // Arrange
            Category category = createCategory("Test Category");
            Author author1 = createAuthor("Author One", "one@example.com");
            Author author2 = createAuthor("Author Two", "two@example.com");
            createBook("Book One", "978-1-4919-5035-7", category, author1);
            Book book2 = createBook("Book Two", "978-0-13-475759-9", category, author2);

            String requestBody = String.format("""
                    {
                        "title": "Updated Book Two",
                        "isbn": "978-1-4919-5035-7",
                        "categoryId": %d,
                        "authorIds": [%d]
                    }
                    """, category.getId(), author2.getId());

            // Act
            ResultActions result = mockMvc.perform(put(BASE_API_PATH + "/" + book2.getId())
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/data-conflict"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/livros/{id} - Deactivate Book")
    class DeactivateBookTests {

        @Test
        @DisplayName("Should deactivate book and return HTTP 204")
        void shouldDeactivateBookAndReturnHttp204() throws Exception {
            // Arrange
            Category category = createCategory("Temp");
            Author author = createAuthor("Temp", "temp@example.com");
            Book book = createBook("Temp Book", "978-0-13-708107-3", category, author);

            // Act
            ResultActions result = mockMvc.perform(delete(BASE_API_PATH + "/" + book.getId())
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            );

            // Assert
            result.andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return HTTP 422 when deactivating loaned book")
        void shouldReturnHttp422WhenDeactivatingLoanedBook() throws Exception {
            // Arrange
            Category category = createCategory("Loaned");
            Author author = createAuthor("Loaned Author", "loaned@example.com");
            Book book = createBook("Loaned Book", "978-0-13-468609-7", category, author);
            book.setStatus(BookStatus.EMPRESTADO);
            bookRepository.save(book);

            // Act
            ResultActions result = mockMvc.perform(delete(BASE_API_PATH + "/" + book.getId())
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            );

            // Assert
            result.andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/loan-validation-failed"))
                    .andExpect(jsonPath("$.title").value("Loan Validation Failed"))
                    .andExpect(jsonPath("$.status").value(422))
                    .andExpect(jsonPath("$.detail").value(containsString("currently loaned")));
        }

        @Test
        @DisplayName("Should return HTTP 404 for non-existent book")
        void shouldReturnHttp404ForNonExistentBook() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(delete(BASE_API_PATH + "/99999")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            );

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/resource-not-found"));
        }

        @Test
        @DisplayName("Should omit deactivated book from list after deletion")
        void shouldOmitDeactivatedBookFromListAfterDeletion() throws Exception {
            // Arrange
            Category category = createCategory("Hide");
            Author author = createAuthor("Hide", "hide@example.com");
            Book book = createBook("Hidden Book", "978-0-13-475822-0", category, author);

            // Verify book is in the list before deletion
            mockMvc.perform(get(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].title", hasItem("Hidden Book")));

            // Act - Delete the book
            mockMvc.perform(delete(BASE_API_PATH + "/" + book.getId())
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            )
                    .andExpect(status().isNoContent());

            // Assert - Verify book is NOT in the list after deletion
            mockMvc.perform(get(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].title", not(hasItem("Hidden Book"))));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/livros - List Books")
    class ListBooksTests {

        @Test
        @DisplayName("Should return paginated list with HTTP 200")
        void shouldReturnPaginatedListWithHttp200() throws Exception {
            // Arrange
            Category category = createCategory("List Category");
            Author author = createAuthor("List Author", "list@example.com");
            createBook("Book A", "978-1-4919-5029-6", category, author);
            createBook("Book B", "978-0-596-51774-9", category, author);

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
        @DisplayName("Should return lightweight summary in list response")
        void shouldReturnLightweightSummaryInListResponse() throws Exception {
            // Arrange
            Category category = createCategory("Lightweight");
            Author author = createAuthor("Light", "light@example.com");
            createBook("Light Book", "978-1-4920-5635-5", category, author);

            // Act
            ResultActions result = mockMvc.perform(get(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .param("page", "0")
                    .param("size", "10"));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").exists())
                    .andExpect(jsonPath("$.content[0].title").exists())
                    .andExpect(jsonPath("$.content[0].isbn").exists())
                    .andExpect(jsonPath("$.content[0].status").exists())
                    .andExpect(jsonPath("$.content[0].categoryName").value("Lightweight"))
                    .andExpect(jsonPath("$.content[0].active").exists())
                    .andExpect(jsonPath("$.content[0].createdAt").exists())
                    .andExpect(jsonPath("$.content[0].updatedAt").exists())
                    .andExpect(jsonPath("$.content[0].category").doesNotExist())
                    .andExpect(jsonPath("$.content[0].authors").doesNotExist());
        }

        @Test
        @DisplayName("Should respect pagination parameters")
        void shouldRespectPaginationParameters() throws Exception {
            // Arrange
            Category category = createCategory("Paginated");
            Author author = createAuthor("Page", "page@example.com");
            createBook("Book 1", "978-1-0981-1873-6", category, author);
            createBook("Book 2", "978-0-13-468599-1", category, author);
            createBook("Book 3", "978-0-201-63361-0", category, author);
            createBook("Book 4", "978-3-16-148410-0", category, author);
            createBook("Book 5", "978-0-13-235088-4", category, author);

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
        @DisplayName("Should return empty page when no books exist")
        void shouldReturnEmptyPageWhenNoBooksExist() throws Exception {
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
        book.setStatus(BookStatus.DISPONIVEL);
        book.setActive(true);
        return bookRepository.save(book);
    }
}
