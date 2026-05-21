package github.oliveira.gb.librarycatalogapi.api.loan;

import github.oliveira.gb.librarycatalogapi.domain.book.Book;
import github.oliveira.gb.librarycatalogapi.domain.book.BookRepository;
import github.oliveira.gb.librarycatalogapi.domain.book.BookStatus;
import github.oliveira.gb.librarycatalogapi.domain.category.Category;
import github.oliveira.gb.librarycatalogapi.domain.category.CategoryRepository;
import github.oliveira.gb.librarycatalogapi.domain.loan.LoanRepository;
import github.oliveira.gb.librarycatalogapi.domain.reader.Reader;
import github.oliveira.gb.librarycatalogapi.domain.reader.ReaderRepository;
import org.junit.jupiter.api.AfterEach;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for LoanController.
 * Validates full HTTP flow, JSON serialization, RFC 7807 error responses,
 * transactional rollback, and database constraints using Testcontainers.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("Loan Controller Integration Tests")
class LoanControllerIntegrationTest {

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
    private ReaderRepository readerRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LoanRepository loanRepository;

    private static final String BASE_API_PATH = "/api/v1/emprestimos";

    @AfterEach
    void tearDown() {
        // Explicit cleanup is required because MockMvc does not propagate the test transaction.
        // Deletion order must respect foreign key constraints (loan_items refs books and loans).
        loanRepository.deleteAll(); // cascade deletes loan_items
        bookRepository.deleteAll();
        categoryRepository.deleteAll();
        readerRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /api/v1/emprestimos - Create Loan")
    class CreateLoanTests {

        @Test
        @DisplayName("Should create loan and return HTTP 201 with response body")
        void shouldCreateLoanAndReturnHttp201() throws Exception {
            // Arrange
            Reader reader = createTestReader("test@example.com", "123.456.789-00");
            Book book = createTestBook("978-0-00-000001-0", "Test Book 1");

            String requestBody = String.format("""
                    {
                        "readerId": %d,
                        "bookIds": [%d]
                    }
                    """, reader.getId(), book.getId());

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString(BASE_API_PATH)))
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.readerId").value(reader.getId().intValue()))
                    .andExpect(jsonPath("$.status").value("ATIVO"))
                    .andExpect(jsonPath("$.bookIds[0]").value(book.getId().intValue()))
                    .andExpect(jsonPath("$.dueDate").exists())
                    .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("Should return HTTP 422 when batch size exceeds 5 books")
        void shouldReturnHttp422WhenBatchSizeExceeded() throws Exception {
            Reader reader = createTestReader("batch@example.com", "123.456.789-01");
            StringBuilder bookIds = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                Book b = createTestBook("978-0-00-00000" + i + "-0", "Book " + i);
                if (i > 0) bookIds.append(", ");
                bookIds.append(b.getId());
            }

            String requestBody = String.format("""
                    {
                        "readerId": %d,
                        "bookIds": [%s]
                    }
                    """, reader.getId(), bookIds);

            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            result.andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/loan-validation-failed"))
                    .andExpect(jsonPath("$.title").value("Loan Validation Failed"))
                    .andExpect(jsonPath("$.detail").exists());
        }

        @Test
        @DisplayName("Should return HTTP 422 when batch contains duplicate titles")
        void shouldReturnHttp422WhenDuplicateTitlesInBatch() throws Exception {
            Reader reader = createTestReader("dup@example.com", "123.456.789-02");
            Book book = createTestBook("978-0-00-000010-0", "Duplicate Book");

            String requestBody = String.format("""
                    {
                        "readerId": %d,
                        "bookIds": [%d, %d]
                    }
                    """, reader.getId(), book.getId(), book.getId());

            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            result.andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.title").value("Loan Validation Failed"))
                    .andExpect(jsonPath("$.detail").value(containsString("duplicate titles")));
        }

        @Test
        @DisplayName("Should return HTTP 422 when book is unavailable (EMPRESTADO)")
        void shouldReturnHttp422WhenBookUnavailable() throws Exception {
            Reader reader = createTestReader("unavail@example.com", "123.456.789-03");
            Book book = createTestBook("978-0-00-000020-0", "Unavailable Book");
            book.setStatus(BookStatus.EMPRESTADO);
            bookRepository.save(book);

            String requestBody = String.format("""
                    {
                        "readerId": %d,
                        "bookIds": [%d]
                    }
                    """, reader.getId(), book.getId());

            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            result.andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.detail").value(containsString("not available for loan")));
        }

        @Test
        @DisplayName("Should return HTTP 404 when reader does not exist")
        void shouldReturnHttp404WhenReaderNotFound() throws Exception {
            String requestBody = """
                    {
                        "readerId": 99999,
                        "bookIds": [10]
                    }
                    """;

            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));
        }

        @Test
        @DisplayName("Should return HTTP 400 for empty bookIds list")
        void shouldReturnHttp400ForEmptyBookIds() throws Exception {
            Reader reader = createTestReader("empty@example.com", "123.456.789-04");

            String requestBody = String.format("""
                    {
                        "readerId": %d,
                        "bookIds": []
                    }
                    """, reader.getId());

            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'bookIds')]").exists());
        }

        @Test
        @DisplayName("Should return HTTP 400 when readerId is null")
        void shouldReturnHttp400WhenReaderIdNull() throws Exception {
            String requestBody = """
                    {
                        "bookIds": [10]
                    }
                    """;

            ResultActions result = mockMvc.perform(post(BASE_API_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody));

            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'readerId')]").exists());
        }

        @Test
        @DisplayName("Should serialize concurrent loan requests for same reader and book")
        void shouldSerializeConcurrentRequests() throws Exception {
            Reader reader = createTestReader("concurrent@example.com", "123.456.789-99");
            Book book = createTestBook("978-0-00-000099-0", "Concurrent Book");

            String requestBody = String.format("""
                    {
                        "readerId": %d,
                        "bookIds": [%d]
                    }
                    """, reader.getId(), book.getId());

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch latch = new CountDownLatch(1);
            List<Future<Integer>> futures = new ArrayList<>();

            for (int i = 0; i < 2; i++) {
                futures.add(executor.submit(() -> {
                    latch.await();
                    var response = mockMvc.perform(post(BASE_API_PATH)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody))
                            .andReturn()
                            .getResponse();
                    return response.getStatus();
                }));
            }

            latch.countDown();

            int status1 = futures.get(0).get();
            int status2 = futures.get(1).get();
            executor.shutdown();

            List<Integer> statuses = List.of(status1, status2);
            assertThat(statuses).containsExactlyInAnyOrder(201, 422);
        }
    }

    private Reader createTestReader(String email, String cpf) {
        Reader reader = new Reader();
        reader.setName("Test Reader");
        reader.setEmail(email);
        reader.setCpf(cpf);
        return readerRepository.save(reader);
    }

    private Book createTestBook(String isbn, String title) {
        Category category = new Category();
        category.setName("Test Category " + isbn);
        Category savedCategory = categoryRepository.save(category);

        Book book = new Book();
        book.setTitle(title);
        book.setIsbn(isbn);
        book.setCategory(savedCategory);
        return bookRepository.save(book);
    }
}
